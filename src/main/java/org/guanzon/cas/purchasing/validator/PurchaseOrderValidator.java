package org.guanzon.cas.purchasing.validator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.MatrixAuthManager;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.inv.InvTransCons;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.json.simple.JSONObject;

/**
 * Base validator for Purchase Order. Holds every Purchase Order business rule.
 *
 * <p>
 * The rules here are the General Inventory rules. Each industry validator
 * (PurchaseOrder_MC, _MP, _SP, _Appliance) extends this class and adds its own
 * rules through the protected hooks.</p>
 *
 * <p>
 * How it is organized:</p>
 * <ul>
 * <li><b>Status procedures</b> ({@link #validate()}): fixed order of checks per
 * status. A subclass cannot skip or reorder them.</li>
 * <li><b>Common rules</b> ({@code commonXxxRules}, final): apply to every
 * industry.</li>
 * <li><b>Industry rules</b> ({@code industryXxxRules}): empty by default;
 * override in the industry class.</li>
 * <li><b>Entry rules</b> (public {@code validateXxx} methods): checks the
 * controller calls while the user is editing (discounts, advance payment,
 * duplicate items, source lines, ...). They return a result and never change
 * the transaction; the controller decides what to reset.</li>
 * <li><b>Settings</b> ({@code getXxx}/{@code isXxx}): override to tune a rule
 * for one industry without rewriting it.</li>
 * </ul>
 *
 * <p>
 * Every result has a "result" key: "success" or "error" (with "message").</p>
 */
public abstract class PurchaseOrderValidator implements GValidator {

    protected static final String SOURCE_CD = InvTransCons.PURCHASE_ORDER;
    protected static final String APPROVAL_MATRIX = "PURCHASE ORDER MATRIX";
    protected static final LocalDate NULL_DATE = LocalDate.of(1900, 1, 1);

    protected GRiderCAS poGrider;
    protected String psTranStat;
    protected Model_PO_Master poMaster;
    protected final ArrayList<Model_PO_Detail> paDetail = new ArrayList<>();
    protected final ArrayList<Model_PO_Detail> paDetailRemoved = new ArrayList<>();
    protected final ArrayList<Object> paOthers = new ArrayList<>();
    protected int pnActiveAttachments = 0;
    protected MatrixAuthManager poMatrix;

    // =====================================================================
    // Wiring (GValidator + context supplied by the controller)
    // =====================================================================
    @Override
    public void setApplicationDriver(Object applicationDriver) {
        poGrider = (GRiderCAS) applicationDriver;
    }

    @Override
    public void setTransactionStatus(String transactionStatus) {
        psTranStat = transactionStatus;
    }

    @Override
    public void setMaster(Object value) {
        poMaster = (Model_PO_Master) value;
    }

    @Override
    public void setDetail(ArrayList<Object> value) {
        copyDetails(value, paDetail);
    }

    @Override
    public void setOthers(ArrayList<Object> value) {
        paOthers.clear();
        if (value != null) {
            paOthers.addAll(value);
        }
    }

    /** Detail rows removed from the PO during editing. */
    public void setRemovedDetail(ArrayList<Object> value) {
        copyDetails(value, paDetailRemoved);
    }

    /** Number of attachments that were not removed. */
    public void setActiveAttachmentCount(int count) {
        pnActiveAttachments = count;
    }

    private static void copyDetails(List<Object> faSource, List<Model_PO_Detail> faTarget) {
        faTarget.clear();
        if (faSource == null) {
            return;
        }
        for (Object loItem : faSource) {
            faTarget.add((Model_PO_Detail) loItem);
        }
    }

    // =====================================================================
    // Status procedures (fixed for all industries)
    // =====================================================================
    @Override
    public final JSONObject validate() {
        if (poGrider == null || poMaster == null || psTranStat == null) {
            return error("Validator is not initialized.");
        }

        //validate status change request
        JSONObject loJSON = StatusChangeValidator.validatePOStatChange(poMaster, psTranStat);
        if (!isSuccess(loJSON)) {
            return loJSON;
        }

        try {
            switch (psTranStat) {
                case PurchaseOrderStatus.OPEN:
                    return runRules(this::commonNewRules, this::industryNewRules);
                case PurchaseOrderStatus.CONFIRMED:
                    return runRules(this::commonConfirmedRules, this::industryConfirmedRules);
                case PurchaseOrderStatus.APPROVED:
                    return validateApproved();
                case PurchaseOrderStatus.POSTED:
                    return runRules(this::commonPostedRules, this::industryPostedRules);
                case PurchaseOrderStatus.CANCELLED:
                    return runRules(this::commonCancelledRules, this::industryCancelledRules);
                case PurchaseOrderStatus.VOID:
                    return runRules(this::commonVoidRules, this::industryVoidRules);
                case PurchaseOrderStatus.RETURNED:
                    return runRules(this::commonReturnedRules, this::industryReturnedRules);
                default:
                    return success();
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            //never return null: the controller reads the "result" key
            return error(ex.getMessage());
        }
    }

    /** One group of rules. */
    @FunctionalInterface
    protected interface RuleStep {
        JSONObject run() throws SQLException, GuanzonException;
    }

    /** Common rules first; industry rules run only when the common rules pass. */
    private JSONObject runRules(RuleStep foCommon, RuleStep foIndustry) throws SQLException, GuanzonException {
        JSONObject loJSON = foCommon.run();
        if (!isSuccess(loJSON)) {
            return loJSON;
        }
        loJSON = foIndustry.run();
        return loJSON != null ? loJSON : success();
    }

    /**
     * Approval: common + industry rules first, then build the authorization
     * matrix request (if any) and let the matrix manager process it.
     */
    private JSONObject validateApproved() throws SQLException, GuanzonException {
        JSONObject loJSON = commonApprovedRules();
        if (!isSuccess(loJSON)) {
            return loJSON;
        }
        loJSON = industryApprovedRules();
        if (!isSuccess(loJSON)) {
            return loJSON;
        }

        poMatrix = new MatrixAuthManager(poGrider, SOURCE_CD, poMaster.getTransactionNo());

        QuotationVariance loVariance = loadQuotationVariance();
        if (isApprovalAuthRequired(loVariance)) {
            String lsAuthCode = poMatrix.getAuthType(getApprovalMatrixName(), poMaster.getTranTotal().toString(), "");
            poMatrix.addAuthRequest(lsAuthCode, "", "", buildAuthRemarks());
        }

        //industry may add its own authorization requests
        addIndustryAuthRequests(poMatrix, loVariance);

        if (!poMatrix.hasAuthRequest()) {
            return success();
        }

        loJSON = poMatrix.processAuth();
        return loJSON != null ? loJSON : error("Unable to process authorization request.");
    }

    // =====================================================================
    // Common status rules (General Inventory) - apply to every industry
    // =====================================================================
    protected final JSONObject commonNewRules() throws SQLException {
        if (isEmpty(poMaster.getSupplierID())) {
            return error("Invalid Supplier.");
        }

        LocalDate ldTransact = toLocalDate(poMaster.getTransactionDate());
        if (ldTransact == null || NULL_DATE.equals(ldTransact)) {
            return error("Invalid Transaction Date.");
        }

        LocalDate ldServer = toLocalDate(poGrider.getServerDate());
        if (ldTransact.isAfter(ldServer)) {
            return error("Future transaction dates are not allowed.");
        }

        int lnMaxYears = getMaxBackdateYears();
        if (ldTransact.isBefore(ldServer.minusYears(lnMaxYears))) {
            return error("Backdated transactions beyond " + lnMaxYears + " year(s) are not allowed.");
        }

        if (isEmpty(poMaster.getDestinationID())) {
            return error("Destination is not set.");
        }

        LocalDate ldExpected = toLocalDate(poMaster.getExpectedDate());
        if (ldExpected == null) {
            return error("Invalid Expected Delivery Date.");
        }
        if (NULL_DATE.equals(ldExpected)) {
            return error("Invalid Expected Delivery Transaction Date.");
        }
        if (ldExpected.isBefore(ldTransact)) {
            return error("The expected date cannot be earlier than the transaction date.");
        }

        if (isEmpty(poMaster.getTermCode())) {
            return error("Invalid Term.");
        }

        if (poMaster.getWithAdvPaym()) {
            if (num(poMaster.getDownPaymentRatesPercentage()) <= 0.0000
                    && num(poMaster.getDownPaymentRatesAmount()) <= 0.0000) {
                return error("You selected advance rate, but no Adv Rate and Adv Amount.");
            }
        }

        if (poMaster.getEditMode() == EditMode.ADDNEW
                && isReferenceRequiredForBackdate()
                && ldTransact.isBefore(ldServer)
                && isEmpty(poMaster.getReference())) {
            return error("A reference number is required for backdated transactions.");
        }

        return success();
    }

    protected final JSONObject commonConfirmedRules() throws SQLException {
        //Require Attachment if transaction total is greater than 100,000.0000 Arsiela 08-07-2026 04:44PM Requested by sir Mac
        if (num(poMaster.getTranTotal()) > getAttachmentRequiredAmount() && pnActiveAttachments <= 0) {
            return error("Transaction attachment cannot be empty.");
        }
        return success();
    }

    /** Add rules here that every industry must pass before approval. */
    protected final JSONObject commonApprovedRules() throws SQLException {
        return success();
    }

    protected final JSONObject commonPostedRules() throws SQLException {
        return success();
    }

    protected final JSONObject commonCancelledRules() throws SQLException {
        return success();
    }

    protected final JSONObject commonVoidRules() throws SQLException {
        return success();
    }

    protected final JSONObject commonReturnedRules() throws SQLException {
        if (PurchaseOrderStatus.RETURNED.equals(poMaster.getTransactionStatus())) {
            return error("Transaction was already returned.");
        }
        return success();
    }

    // =====================================================================
    // Industry status rules (override in the industry class)
    // =====================================================================
    protected JSONObject industryNewRules() throws SQLException, GuanzonException {
        return success();
    }

    protected JSONObject industryConfirmedRules() throws SQLException, GuanzonException {
        return success();
    }

    protected JSONObject industryApprovedRules() throws SQLException, GuanzonException {
        return success();
    }

    protected JSONObject industryPostedRules() throws SQLException, GuanzonException {
        return success();
    }

    protected JSONObject industryCancelledRules() throws SQLException, GuanzonException {
        return success();
    }

    protected JSONObject industryVoidRules() throws SQLException, GuanzonException {
        return success();
    }

    protected JSONObject industryReturnedRules() throws SQLException, GuanzonException {
        return success();
    }

    /**
     * Extra authorization requests for approval (e.g. a separate matrix for an
     * industry). Default: none.
     */
    protected void addIndustryAuthRequests(MatrixAuthManager foMatrix, QuotationVariance foVariance)
            throws SQLException, GuanzonException {
    }

    // =====================================================================
    // Entry rules: source lines (stock request / PO quotation)
    // =====================================================================
    /**
     * Checks every detail line against its source document for the status set
     * with setTransactionStatus(). Called by the controller before it updates
     * the stock requests and quotations.
     */
    public JSONObject validateSourceLines() throws SQLException, GuanzonException, CloneNotSupportedException {
        for (Model_PO_Detail loDetail : paDetail) {
            if (isEmpty(loDetail.getSouceNo())) {
                continue;
            }

            JSONObject loJSON;
            if (PurchaseOrderStatus.SourceCode.STOCKREQUEST.equals(loDetail.getSouceCode())) {
                loJSON = validateStockRequestLine(loDetail);
            } else if (PurchaseOrderStatus.SourceCode.POQUOTATION.equals(loDetail.getSouceCode())) {
                loJSON = validateQuotationNotOrdered(loDetail.getSouceNo());
            } else {
                loJSON = success();
            }

            if (!isSuccess(loJSON)) {
                return loJSON;
            }
        }

        //industry may add more source-line checks
        JSONObject loJSON = industrySourceLineRules();
        return loJSON != null ? loJSON : success();
    }

    /** Industry source-line checks. Default: none. */
    protected JSONObject industrySourceLineRules() throws SQLException, GuanzonException {
        return success();
    }

    protected JSONObject validateStockRequestLine(Model_PO_Detail foDetail)
            throws SQLException, GuanzonException, CloneNotSupportedException {
        double lnApproved = foDetail.InvStockRequestDetail().getApproved();

        //remaining = nApproved - (nCancelld + nIssueQty + nOrderQty)
        if (PurchaseOrderStatus.CONFIRMED.equals(psTranStat)
                || PurchaseOrderStatus.APPROVED.equals(psTranStat)) {
            double lnRemaining = lnApproved
                    - (foDetail.InvStockRequestDetail().getCancelled()
                    + foDetail.InvStockRequestDetail().getIssued()
                    + foDetail.InvStockRequestDetail().getPurchase());
            if (lnRemaining == 0.00) {
                return error("Discrepancy: Stock requests related to this order number have already been processed.");
            }
        }

        if (num(foDetail.getQuantity()) > lnApproved) {
            return error("Discrepancy: Order Quantity cannot greater than request quantity, please enter valid order quantity.");
        }

        if (PurchaseOrderStatus.RETURNED.equals(poMaster.getTransactionStatus()) && lnApproved == 0) {
            return error("All stock requests related to this order number have already been processed.");
        }

        return success();
    }

    /**
     * A PO Quotation may feed only one confirmed/approved Purchase Order.
     * Returns success when no other PO uses the quotation.
     */
    public JSONObject validateQuotationNotOrdered(String fsQuotationNo) throws SQLException {
        String lsSQL = "SELECT b.sSourceNo"
                + " FROM PO_Master a"
                + " LEFT JOIN PO_Detail b ON a.sTransNox = b.sTransNox"
                + " WHERE b.sSourceNo = " + SQLUtil.toSQL(fsQuotationNo)
                + " AND b.sSourceCd = " + SQLUtil.toSQL(PurchaseOrderStatus.SourceCode.POQUOTATION)
                + " AND ( a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.CONFIRMED)
                + "     OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.APPROVED) + " )"
                + " AND a.sTransNox <> " + SQLUtil.toSQL(poMaster.getTransactionNo())
                + " LIMIT 1";

        if (hasRow(lsSQL)) {
            return error("Selected PO Quotation already have an existing Purchase Order.");
        }
        JSONObject loJSON = success();
        loJSON.put("message", "No Record Found.");
        return loJSON;
    }

    // =====================================================================
    // Entry rules: detail grid
    // =====================================================================
    /** A new row may be added only when the last row has an item. */
    public JSONObject validateCanAddDetail() {
        if (!paDetail.isEmpty() && isEmpty(paDetail.get(paDetail.size() - 1).getStockID())) {
            return error("Last row has empty item.");
        }
        return success();
    }

    /**
     * The same item may appear only once among lines that were not pulled
     * from a source document.
     *
     * @param fnRow row being edited (skipped)
     * @param fsStockId item being placed on the row
     * @param fsItemLabel text shown in the message, e.g. "Barcode: XYZ"
     */
    public JSONObject validateUniqueItem(int fnRow, String fsStockId, String fsItemLabel) {
        for (int lnRow = 0; lnRow < paDetail.size(); lnRow++) {
            if (lnRow == fnRow) {
                continue;
            }
            Model_PO_Detail loDetail = paDetail.get(lnRow);
            if (isEmpty(loDetail.getSouceNo()) && Objects.equals(fsStockId, loDetail.getStockID())) {
                JSONObject loJSON = error(fsItemLabel + " already exist in table at row " + (lnRow + 1) + ".");
                loJSON.put("tableRow", lnRow);
                return loJSON;
            }
        }
        return success();
    }

    /**
     * Quantity check over the item lines. Same result shape as the old
     * isDetailHasZeroQty(): "tableRow" and, on error, "warning" ("true" when
     * every line is zero).
     */
    public JSONObject validateQuantities() {
        int lnZeroRow = -1;
        boolean lbHasNonZero = false;
        boolean lbHasZero = false;
        int lnLastRow = paDetail.size() - 1;

        for (int lnRow = 0; lnRow <= lnLastRow; lnRow++) {
            Model_PO_Detail loDetail = paDetail.get(lnRow);
            if (isEmpty(loDetail.getStockID())) {
                continue;
            }
            if (num(loDetail.getQuantity()) == 0.00) {
                lbHasZero = true;
                if (lnZeroRow == -1) {
                    lnZeroRow = lnRow;
                }
            } else {
                lbHasNonZero = true;
            }
        }

        JSONObject loJSON;
        if (!lbHasNonZero && lbHasZero) {
            loJSON = error("All items have zero quantity. Please enter a valid quantity.");
            loJSON.put("tableRow", lnZeroRow);
            loJSON.put("warning", "true");
        } else if (lbHasZero) {
            loJSON = error("Some items have zero quantity. Please review.");
            loJSON.put("tableRow", lnZeroRow);
            loJSON.put("warning", "false");
        } else {
            loJSON = success();
            loJSON.put("message", "All items have valid quantities.");
            loJSON.put("tableRow", lnLastRow);
        }
        return loJSON;
    }

    /** Adding fnLineAmount to the current net total must stay below the cap. */
    public JSONObject validateNetTotalWithLine(double fnLineAmount) {
        double lnNetTotal = round4(num(poMaster.getNetTotal()) + fnLineAmount);
        if (lnNetTotal >= getMaxNetTotal()) {
            return error("The net total exceeds the maximum allowed amount. Please reduce the value and try again.");
        }
        return success();
    }

    // =====================================================================
    // Entry rules: discounts and advance payment
    // =====================================================================
    public JSONObject validateDiscountRate(double fnRate) {
        if (num(poMaster.getTranTotal()) == 0.0000) {
            return error("You're not allowed to enter discount rate, no detail amount entered.");
        }
        if (fnRate < 0.0000 || fnRate > 100.0000) {
            return error("Invalid Discount Rate.  Must be between 0.00 and 100.00");
        }
        return success();
    }

    public JSONObject validateDiscountAmount(double fnAmount) {
        if (fnAmount >= getMaxDiscountAmount()) {
            return error("Discount amount must not exceed " + String.format("%,.2f", getMaxDiscountAmount()) + ".");
        }
        double lnTotal = num(poMaster.getTranTotal());
        double lnAfterRate = round4(lnTotal - ((num(poMaster.getDiscount()) / 100) * lnTotal));
        if (lnAfterRate == 0.0000) {
            return error("You're not allowed to enter discount amount, no amount entered.");
        }
        if (fnAmount < 0.0000 || fnAmount > lnAfterRate) {
            return error("Invalid Discount Amount");
        }
        return success();
    }

    public JSONObject validateAdvancePaymentRate(double fnRate) {
        double lnAfterDiscounts = getAmountAfterDiscounts();
        if (lnAfterDiscounts <= 0.0000) {
            return error("Invalid Advance Payment Rate, the total transaction amount is 0.0000");
        }
        if (fnRate < 0.0000 || fnRate > 100.0000) {
            return error("Invalid Advance Payment Rate. Must be between 0.0000 and 100.0000");
        }
        //Added validation for total downpayment Arsiela 07-15-2026
        double lnTotalAdvance = round4(((lnAfterDiscounts / 100) * fnRate) + num(poMaster.getDownPaymentRatesAmount()));
        if (lnTotalAdvance > lnAfterDiscounts) {
            return error("Invalid Downpayment Total.");
        }
        return success();
    }

    public JSONObject validateAdvancePaymentAmount(double fnAmount) {
        double lnAfterDiscounts = getAmountAfterDiscounts();
        if (lnAfterDiscounts <= 0.0000) {
            return error("Invalid Advance Payment Amount, the total transaction amount is 0.0000");
        }
        if (fnAmount < 0.0000 || fnAmount > lnAfterDiscounts) {
            return error("Invalid Advance Payment Amount");
        }
        //Added validation for total downpayment Arsiela 07-15-2026
        double lnTotalAdvance = round4(((lnAfterDiscounts / 100) * num(poMaster.getDownPaymentRatesPercentage())) + fnAmount);
        if (lnTotalAdvance > lnAfterDiscounts) {
            return error("Invalid Downpayment Total.");
        }
        return success();
    }

    /** Net total after a discount or advance change must not be negative. */
    public JSONObject validateNetTotal() {
        if (num(poMaster.getNetTotal()) < 0.0000) {
            return error("Invalid Total Transaction Amount");
        }
        return success();
    }

    /** Transaction total less discount rate and discount amount, 4 decimals. */
    protected double getAmountAfterDiscounts() {
        double lnTotal = num(poMaster.getTranTotal());
        return round4(lnTotal - (((lnTotal / 100) * num(poMaster.getDiscount())) + num(poMaster.getAdditionalDiscount())));
    }

    // =====================================================================
    // Entry rules: returned PO edit
    // =====================================================================
    /**
     * Saving a RETURNED PO requires at least one change compared with the
     * saved record.
     */
    public JSONObject validateReturnedEdit(Model_PO_Master foSaved, List<Model_PO_Detail> faSavedDetail) {
        boolean lbSame = faSavedDetail.size() == paDetail.size()
                && Objects.equals(foSaved.getTransactionDate(), poMaster.getTransactionDate())
                && Objects.equals(foSaved.getSupplierID(), poMaster.getSupplierID())
                && Objects.equals(foSaved.getDestinationID(), poMaster.getDestinationID())
                && Objects.equals(foSaved.getTermCode(), poMaster.getTermCode())
                && num(foSaved.getDiscount()) == num(poMaster.getDiscount())
                && num(foSaved.getAdditionalDiscount()) == num(poMaster.getAdditionalDiscount())
                && num(foSaved.getTranTotal()) == num(poMaster.getTranTotal())
                && Objects.equals(foSaved.getRemarks(), poMaster.getRemarks());

        if (lbSame && foSaved.getWithAdvPaym()) {
            lbSame = num(foSaved.getDownPaymentRatesPercentage()) == num(poMaster.getDownPaymentRatesPercentage())
                    && num(foSaved.getDownPaymentRatesAmount()) == num(poMaster.getDownPaymentRatesAmount());
        }

        for (int lnRow = 0; lbSame && lnRow < faSavedDetail.size(); lnRow++) {
            lbSame = Objects.equals(faSavedDetail.get(lnRow).getStockID(), paDetail.get(lnRow).getStockID())
                    && num(faSavedDetail.get(lnRow).getQuantity()) == num(paDetail.get(lnRow).getQuantity());
        }

        return lbSame ? error("No update has been made.") : success();
    }

    // =====================================================================
    // Settings (override to tune a rule for one industry)
    // =====================================================================
    /** How far back a transaction date may go. */
    protected int getMaxBackdateYears() {
        return 1;
    }

    /** Whether a new backdated PO must carry a reference number. */
    protected boolean isReferenceRequiredForBackdate() {
        return true;
    }

    /** Above this total, confirmation requires at least one attachment. */
    protected double getAttachmentRequiredAmount() {
        return 100000.0000;
    }

    /** Discount amount must be below this. */
    protected double getMaxDiscountAmount() {
        return 1000000.0000;
    }

    /** Net total must be below this. */
    protected double getMaxNetTotal() {
        return 100000000.0000;
    }

    /** Allowed absolute % difference between PO and quotation quantity. */
    protected double getQuantityTolerancePct() {
        return 0.0000;
    }

    /** Allowed absolute % difference between PO and quotation unit price. */
    protected double getPriceTolerancePct() {
        return 0.0000;
    }

    /** Matrix name used to look up the authorization type. */
    protected String getApprovalMatrixName() {
        return APPROVAL_MATRIX;
    }

    /**
     * Default rule: authorization is required when any line has no matching
     * quotation line, or when quantity or price differs from the quotation by
     * more than the tolerance.
     */
    protected boolean isApprovalAuthRequired(QuotationVariance foVariance) {
        if (!foVariance.isFullyQuoted()) {
            return true;
        }
        return foVariance.nMaxQtyDiffPct > getQuantityTolerancePct()
                || foVariance.nMaxPriceDiffPct > getPriceTolerancePct();
    }

    /** Remarks attached to the authorization request. */
    protected String buildAuthRemarks() {
        return poMaster.getBranchCode()
                + "/" + poMaster.getTransactionNo()
                + ";" + SQLUtil.dateFormat(poMaster.getTransactionDate(), "yyyy-MM-dd")
                + ";" + poMaster.getTranTotal().toString()
                + ";" + Objects.toString(poMaster.getRemarks(), "");
    }

    // =====================================================================
    // PO vs quotation comparison
    // =====================================================================
    /** Summary of how the PO lines differ from their source quotation. */
    protected static class QuotationVariance {

        /** Number of PO lines checked. */
        public int nRecords;
        /** Lines with no matching quotation line. */
        public int nUnquoted;
        /** Largest absolute % quantity difference among quoted lines. */
        public double nMaxQtyDiffPct;
        /** Largest absolute % unit price difference among quoted lines. */
        public double nMaxPriceDiffPct;
        /** % difference between quotation total and PO total (first quoted line). */
        public double nTotalDiffPct;

        public boolean isFullyQuoted() {
            return nRecords > 0 && nUnquoted == 0;
        }
    }

    protected final QuotationVariance loadQuotationVariance() throws SQLException {
        String lsSQL = "SELECT"
                + "  c.sStockIDx"
                + ", d.sTransNox sQuoteNox"
                + ", ((IFNULL(b.nTranTotl, 0) - IFNULL(a.nTranTotl, 0)) / NULLIF(b.nTranTotl, 0) * 100) AS nTotlDiff"
                + ", ((IFNULL(d.nQuantity, 0) - IFNULL(c.nQuantity, 0)) / NULLIF(d.nQuantity, 0) * 100) AS nQtyxDiff"
                + ", ((IFNULL(d.nUnitPrce, 0) - IFNULL(c.nUnitPrce, 0)) / NULLIF(d.nUnitPrce, 0) * 100) AS nPrceDiff"
                + " FROM PO_Master a"
                + " LEFT JOIN PO_Detail c ON a.sTransNox = c.sTransNox"
                + " LEFT JOIN PO_Quotation_Detail d ON c.sSourceNo = d.sTransNox"
                + "     AND c.sStockIDx = d.sStockIDx"
                + "     AND c.sSourceCd = " + SQLUtil.toSQL(PurchaseOrderStatus.SourceCode.POQUOTATION)
                + " LEFT JOIN PO_Quotation_Master b ON d.sTransNox = b.sTransNox"
                + " WHERE a.sTransNox = " + SQLUtil.toSQL(poMaster.getTransactionNo());

        ResultSet loRS = poGrider.executeQuery(lsSQL);
        if (loRS == null) {
            throw new SQLException("Unable to load quotation comparison for " + poMaster.getTransactionNo() + ".");
        }

        QuotationVariance loVariance = new QuotationVariance();
        boolean lbTotalSet = false;
        try {
            while (loRS.next()) {
                loVariance.nRecords++;

                if (loRS.getString("sQuoteNox") == null) {
                    loVariance.nUnquoted++;
                    continue;
                }

                if (!lbTotalSet) {
                    loVariance.nTotalDiffPct = loRS.getDouble("nTotlDiff");
                    lbTotalSet = true;
                }

                //absolute value: any difference, higher or lower, counts
                loVariance.nMaxQtyDiffPct = Math.max(loVariance.nMaxQtyDiffPct, Math.abs(loRS.getDouble("nQtyxDiff")));
                loVariance.nMaxPriceDiffPct = Math.max(loVariance.nMaxPriceDiffPct, Math.abs(loRS.getDouble("nPrceDiff")));
            }
        } finally {
            loRS.close();
        }
        return loVariance;
    }

    // =====================================================================
    // Helpers
    // =====================================================================
    protected static JSONObject success() {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "success");
        return loJSON;
    }

    protected static JSONObject error(String fsMessage) {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "error");
        loJSON.put("message", fsMessage);
        return loJSON;
    }

    public static boolean isSuccess(JSONObject foJSON) {
        return foJSON != null && "success".equals((String) foJSON.get("result"));
    }

    protected static boolean isEmpty(String fsValue) {
        return fsValue == null || fsValue.trim().isEmpty();
    }

    protected static double num(Number fnValue) {
        return fnValue == null ? 0.0000 : fnValue.doubleValue();
    }

    /** Rounds to 4 decimals to avoid recurring decimal digits in comparisons. */
    protected static double round4(double fnValue) {
        return BigDecimal.valueOf(fnValue).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    protected static LocalDate toLocalDate(Date fdValue) {
        return fdValue == null ? null : new java.sql.Date(fdValue.getTime()).toLocalDate();
    }

    protected boolean hasRow(String fsSQL) throws SQLException {
        ResultSet loRS = poGrider.executeQuery(fsSQL);
        if (loRS == null) {
            throw new SQLException("Query execution failed.");
        }
        try {
            return loRS.next();
        } finally {
            loRS.close();
        }
    }

    /** Details passed through setDetail(). */
    protected ArrayList<Model_PO_Detail> getDetails() {
        return paDetail;
    }

    /** Removed details passed through setRemovedDetail(). */
    protected ArrayList<Model_PO_Detail> getRemovedDetails() {
        return paDetailRemoved;
    }
}