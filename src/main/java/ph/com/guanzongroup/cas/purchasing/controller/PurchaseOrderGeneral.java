package ph.com.guanzongroup.cas.purchasing.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.rowset.CachedRowSet;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.systables.TransactionAttachment;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Master;
import org.guanzon.cas.inv.warehouse.services.InvWarehouseControllers;
import org.guanzon.cas.parameter.model.Model_Project;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.parameter.services.ParamModels;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.guanzon.cas.purchasing.services.QuotationControllers;
import ph.com.guanzongroup.cas.purchasing.status.POStatus;
import org.guanzon.cas.purchasing.status.PurchaseOrderStaticData;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrderValidator;
import ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrder_General;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;

/**
 * Purchase Order controller for General Inventory.
 *
 * <p>
 * {@link PurchaseOrderMain} runs the main procedures
 * (Init/New/Open/Update/Save/Confirm/Approve/Cancel); this class plugs the
 * General Inventory work into them through the hooks and adds the rest:
 * Post/Void/Return, lists, discounts, status history, reports and the
 * engineering project title.</p>
 *
 * <p>
 * The work itself is in helper classes that any industry controller can reuse
 * (override the matching create... method to change one):</p>
 * <ul>
 * <li>{@link PurchaseOrderSourceDocuments}: stock requests and PO quotations</li>
 * <li>{@link PurchaseOrderAdvancePayment}: payment request for the advance</li>
 * <li>{@link PurchaseOrderSearch}: lookups that fill the PO</li>
 * <li>{@link PurchaseOrderAttachments}, {@link PurchaseOrderAudit},
 * {@link PurchaseOrderPrinter}</li>
 * </ul>
 *
 * <p>
 * Rules are in {@link PurchaseOrder_General}. The public methods are the same
 * as before, so the screens do not change.</p>
 */
public class PurchaseOrderGeneral extends PurchaseOrderMain {

    private static final Logger LOG = Logger.getLogger(PurchaseOrderGeneral.class.getName());
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** Purchase orders loaded by getPurchaseOrder / getConfirmedPurchaseOrder. */
    private List<Model_PO_Master> paPOMaster = new ArrayList<>();
    /** Detail rows removed from the PO while editing. */
    private List<Model> paDetailRemoved = new ArrayList<>();
    /** Date range of the last report query, shown on the printed report. */
    private String psReportDateFrom;
    private String psReportDateThru;

    private PurchaseOrderSourceDocuments poSourceDocuments;
    private PurchaseOrderAdvancePayment poAdvancePayment;
    private PurchaseOrderSearch poSearch;
    private PurchaseOrderAttachments poAttachments;
    private PurchaseOrderAudit poAudit;
    private PurchaseOrderPrinter poPrinter;

    /** Engineering project referenced by the PO. */
    private Model_Project poProject;
    /** Set when a Cancel/Void prepared the project; saved inside the transaction. */
    private boolean pbProjectPrepared = false;

    // =====================================================================
    // Main hooks
    // =====================================================================
    @Override
    protected PurchaseOrderValidator createValidator() {
        return new PurchaseOrder_General();
    }

    /** General Inventory validator (gives access to the project title rule). */
    protected PurchaseOrder_General generalRules(String fsStatus) {
        return (PurchaseOrder_General) rules(fsStatus);
    }

    @Override
    protected void initOthers() {
        SourceDocuments().reset();
        paDetailRemoved = new ArrayList<>();
    }

    /** Removed detail rows and the attachment count are needed by the rules. */
    @Override
    protected void prepareRules(PurchaseOrderValidator foRules) {
        foRules.setRemovedDetail(new ArrayList<Object>(paDetailRemoved));
        foRules.setActiveAttachmentCount(Attachments().getActiveCount());
    }

    /**
     * Before the status-change transaction: check and load the source
     * documents; build the payment request (APPROVED); untag the engineering
     * project when no other PO uses it (CANCELLED / VOID).
     */
    @Override
    protected JSONObject prepareStatusChange(String fsStatus)
            throws SQLException, GuanzonException, CloneNotSupportedException {
        pbProjectPrepared = false;

        JSONObject loJSON = SourceDocuments().prepare(fsStatus, paDetailRemoved);
        if (!isSuccess(loJSON)) {
            return loJSON;
        }

        if (PurchaseOrderStatus.APPROVED.equals(fsStatus)) {
            loJSON = AdvancePayment().prepare();
            if (!isSuccess(loJSON)) {
                return loJSON;
            }
        }

        if ((PurchaseOrderStatus.CANCELLED.equals(fsStatus) || PurchaseOrderStatus.VOID.equals(fsStatus))
                && generalRules(fsStatus).isProjectTitleUpdateRequired(fsStatus, true)) {
            loJSON = setProjectTitle(fsStatus);
            if (!isSuccess(loJSON)) {
                return loJSON;
            }
            pbProjectPrepared = true;
        }
        return successJSON();
    }

    /**
     * Inside the status-change transaction: save the source documents, the
     * payment request (APPROVED) and the project (CANCELLED / VOID); the PO
     * quotations follow the new status.
     */
    @Override
    protected JSONObject processStatusChange(String fsStatus, String fsRemarks) throws Exception {
        JSONObject loJSON = SourceDocuments().save();
        if (!isSuccess(loJSON)) {
            return loJSON;
        }

        if (PurchaseOrderStatus.APPROVED.equals(fsStatus)) {
            loJSON = AdvancePayment().save();
            if (!isSuccess(loJSON)) {
                return loJSON;
            }
        }

        if (pbProjectPrepared) {
            pbProjectPrepared = false;
            loJSON = saveProjectTitle();
            if (!isSuccess(loJSON)) {
                return loJSON;
            }
        }

        switch (fsStatus) {
            case PurchaseOrderStatus.CONFIRMED:
            case PurchaseOrderStatus.CANCELLED:
            case PurchaseOrderStatus.VOID:
            case PurchaseOrderStatus.RETURNED:
                loJSON = SourceDocuments().updateQuotationStatus(fsStatus);
                if (!isSuccess(loJSON)) {
                    return loJSON;
                }
                break;
            default:
                break;
        }
        return successJSON();
    }

    // =====================================================================
    // Status procedures
    // =====================================================================
    /**
     * Confirms an OPEN PO, then approves it automatically. A PO that is no
     * longer OPEN goes straight to approval.
     */
    @Override
    public JSONObject ConfirmTransaction(String remarks) throws ParseException, SQLException, CloneNotSupportedException, GuanzonException {
        if (PurchaseOrderStatus.OPEN.equals(Master().getTransactionStatus())) {
            JSONObject loJSON = super.ConfirmTransaction(remarks);
            if (!isSuccess(loJSON)) {
                return result(loJSON); //error or "matrix"
            }
            Master().setTransactionStatus(PurchaseOrderStatus.CONFIRMED);
        }
        return result(ApproveTransaction("Auto-approve after confirmation."));
    }

    public JSONObject PostTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        //when called with a parent, the parent owns the DB transaction
        return result(changeStatus(PurchaseOrderStatus.POSTED, remarks, remarks,
                "Post Transaction", "Transaction posted successfully.", !pbWthParent));
    }

    public JSONObject VoidTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        return result(changeStatus(PurchaseOrderStatus.VOID, remarks, remarks,
                "Void Transaction", "Transaction voided successfully.", true));
    }

    /**
     * Returns the PO. Uses the validator and an approving officer (for
     * encoders) instead of the authorization matrix.
     */
    public JSONObject ReturnTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        final String lsStatus = PurchaseOrderStatus.RETURNED;

        if (getEditMode() != EditMode.READY) {
            return result(errorJSON("No transaction was loaded."));
        }

        JSONObject loJSON = isEntryOkay(lsStatus);
        if (!isSuccess(loJSON)) {
            return result(loJSON);
        }

        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            loJSON = prompt().getUserApproval(poGRider);
            if (!isSuccess(loJSON)) {
                return result(loJSON);
            }
            if (Integer.parseInt(loJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                return result(errorJSON("User is not an authorized approving officer.."));
            }
            setApproving((String) loJSON.get("sUserIDxx"));
        }

        loJSON = prepareStatusChange(lsStatus);
        if (!isSuccess(loJSON)) {
            return result(loJSON);
        }

        return result(inTransaction("Return Transaction", true, () -> {
            JSONObject loStep = processStatusChange(lsStatus, remarks);
            if (!isSuccess(loStep)) {
                return loStep;
            }
            loStep = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
            if (!isSuccess(loStep)) {
                return loStep;
            }
            return successJSON("Transaction returned successfully.");
        }));
    }

    /** Cancels inside a parent's DB transaction (the parent rolls back on error). */
    public JSONObject POCancelTransaction() throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        return result(writeStatus(PurchaseOrderStatus.CANCELLED, "CancelTransaction", "Transaction cancelled successfully."));
    }

    /** Puts back the previous status, inside a parent's DB transaction. */
    public JSONObject RevertStatus() throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        String lsStatus = getPrevStatus();
        if (lsStatus == null || lsStatus.isEmpty()) {
            return result(successJSON());
        }
        return result(writeStatus(lsStatus, "RevertStatus", "Transaction status reverted successfully."));
    }

    private JSONObject writeStatus(String fsStatus, String fsRemarks, String fsSuccessMessage) {
        try {
            JSONObject loJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), fsRemarks, fsStatus, false, true);
            return isSuccess(loJSON) ? successJSON(fsSuccessMessage) : loJSON;
        } catch (GuanzonException | SQLException | CloneNotSupportedException ex) {
            LOG.log(Level.SEVERE, MiscUtil.getException(ex), ex);
            return errorJSON(MiscUtil.getException(ex));
        }
    }

    /** Most recent status before the current one; empty when none. */
    public String getPrevStatus() throws SQLException {
        String lsSQL = PurchaseOrderQueries.previousStatus(Master().getTransactionNo(), Master().getTable(), Master().getTransactionStatus());
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (loRS == null) {
            return "";
        }
        try {
            return loRS.next() ? loRS.getString("cRefrStat") : "";
        } finally {
            MiscUtil.close(loRS);
        }
    }

    // =====================================================================
    // Saving
    // =====================================================================
    @Override
    public JSONObject willSave() throws SQLException, CloneNotSupportedException, GuanzonException {
        //transaction no., drop empty rows, number the rows
        prepareDetailsForSave();

        String lsStatus = Master().getTransactionStatus();

        //an encoder editing a confirmed PO needs an approving officer
        if (PurchaseOrderStatus.CONFIRMED.equals(lsStatus) && pnEditMode == EditMode.UPDATE
                && poGRider.getUserLevel() <= UserRight.ENCODER) {
            JSONObject loJSON = prompt().getUserApproval(poGRider);
            if (!isSuccess(loJSON)) {
                return loJSON;
            }
            setApproving((String) loJSON.get("sUserIDxx"));
        }

        //a returned PO must have at least one change
        if (PurchaseOrderStatus.RETURNED.equals(lsStatus)) {
            JSONObject loJSON = checkReturnedEdit();
            if (!isSuccess(loJSON)) {
                return loJSON;
            }
        }

        //editing a confirmed PO updates its source documents
        if (PurchaseOrderStatus.CONFIRMED.equals(lsStatus)) {
            JSONObject loJSON = SourceDocuments().prepare(lsStatus, paDetailRemoved);
            if (!isSuccess(loJSON)) {
                return loJSON;
            }
        }

        //engineering project title
        if (generalRules(lsStatus).isProjectTitleUpdateRequired(lsStatus, false)) {
            JSONObject loJSON = setProjectTitle(lsStatus);
            if (!isSuccess(loJSON)) {
                return loJSON;
            }
        }

        //attachments: link to this PO, rename duplicates, upload unsent files
        return Attachments().prepareForSave(Master().getTransactionNo(), Master().getBranchCode());
    }

    /** Compares with the saved record, read straight from the database. */
    private JSONObject checkReturnedEdit() throws SQLException, GuanzonException, CloneNotSupportedException {
        Model_PO_Master loSaved = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
        JSONObject loJSON = loSaved.openRecord(Master().getTransactionNo());
        if (!isSuccess(loJSON)) {
            return loJSON;
        }

        List<PurchaseOrderValidator.SavedLine> laLines = new ArrayList<>();
        ResultSet loRS = poGRider.executeQuery(PurchaseOrderQueries.savedLines(Master().getTransactionNo()));
        if (loRS == null) {
            return errorJSON("Query execution failed.");
        }
        try {
            while (loRS.next()) {
                laLines.add(new PurchaseOrderValidator.SavedLine(loRS.getString("sStockIDx"), loRS.getDouble("nQuantity")));
            }
        } finally {
            MiscUtil.close(loRS);
        }
        return rules().validateReturnedEditLines(loSaved, laLines);
    }

    @Override
    public JSONObject saveOthers() {
        try {
            JSONObject loJSON = Attachments().save();
            if (!isSuccess(loJSON)) {
                return loJSON;
            }

            loJSON = SourceDocuments().save();
            if (!isSuccess(loJSON)) {
                poGRider.rollbackTrans();
                return loJSON;
            }

            //the engineering project is tagged when a new PO is saved
            String lsStatus = Master().getTransactionStatus();
            if (Master().getEditMode() == EditMode.ADDNEW
                    && generalRules(lsStatus).isProjectTitleUpdateRequired(lsStatus, false)) {
                loJSON = saveProjectTitle();
                if (!isSuccess(loJSON)) {
                    poGRider.rollbackTrans();
                    return loJSON;
                }
            }
            return successJSON();
        } catch (CloneNotSupportedException | SQLException | GuanzonException ex) {
            LOG.log(Level.SEVERE, null, ex);
            return errorJSON(ex.getMessage());
        }
    }

    @Override
    public void initSQL() {
        SQL_BROWSE = PurchaseOrderQueries.browse();
    }

    // =====================================================================
    // Detail grid
    // =====================================================================
    /** Adds a row when the last row has an item. */
    public JSONObject AddDetail() throws CloneNotSupportedException {
        JSONObject loJSON = rules().validateCanAddDetail();
        if (!isSuccess(loJSON)) {
            return result(loJSON);
        }
        return result(addDetail());
    }

    public int getDetailRemovedCount() {
        return paDetailRemoved.size();
    }

    public Model_PO_Detail DetailRemove(int row) {
        return (Model_PO_Detail) paDetailRemoved.get(row);
    }

    /** Quantity check: "tableRow" of the first zero line, "warning" "true" when all are zero. */
    public JSONObject isDetailHasZeroQty() {
        return result(rules().validateQuantities());
    }

    /** Status label for the screen (e.g. "APPROVED", "APPROVED+" while pending). */
    public String getStatusValue() {
        return POStatus.screenLabel(Master().getTransactionStatus());
    }

    // =====================================================================
    // Lists
    // =====================================================================
    /** Browse dialog; opens the picked PO. */
    public JSONObject SearchTransaction(String fsValue, String fsSupplierID, String fsReferID, String fsTransNo, int fnSort)
            throws CloneNotSupportedException, SQLException, GuanzonException {
        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, String.join(" AND ",
                "a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()),
                "a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID()),
                "a.sSupplier LIKE " + SQLUtil.toSQL("%" + fsSupplierID),
                "a.sCategrCd LIKE " + SQLUtil.toSQL("%" + Master().getCategoryCode()),
                "a.sReferNox LIKE " + SQLUtil.toSQL("%" + fsReferID),
                "a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsTransNo)));
        lsSQL += andIfAny(PurchaseOrderQueries.statusOrPending("a.cTranStat", psTranStat, psTranStat));
        lsSQL += branchFilter("a.sBranchCd");
        lsSQL += " GROUP BY a.sTransNox";
        LOG.log(Level.FINE, lsSQL);

        JSONObject loPicked = prompt().browse(poGRider, lsSQL, fsValue,
                "Transaction Date»Transaction No»Reference No»Company»Supplier",
                "dTransact»sTransNox»a.sReferNox»c.sCompnyNm»e.sCompnyNm",
                "dTransact»sTransNox»a.sReferNox»e.sCompnyNm",
                fnSort);
        if (loPicked == null) {
            return result(errorJSON("No record loaded."));
        }
        return OpenTransaction((String) loPicked.get("sTransNox"));
    }

    /** POs with the statuses in psTranStat (and their pending letters). */
    public JSONObject getPurchaseOrder(String fsSupplierID, String fsReferID, String fsTransactionNo) throws SQLException, GuanzonException {
        return result(loadPurchaseOrders(fsSupplierID, fsReferID, psTranStat));
    }

    /** POs with the statuses in psTranStat, plus POs with approval pending. */
    public JSONObject getConfirmedPurchaseOrder(String fsSupplierID, String fsReferID) throws SQLException, GuanzonException {
        return result(loadPurchaseOrders(fsSupplierID, fsReferID, PurchaseOrderStatus.APPROVED));
    }

    private JSONObject loadPurchaseOrders(String fsSupplierID, String fsReferID, String fsPendingCodes) throws SQLException, GuanzonException {
        String lsSQL = MiscUtil.addCondition(PurchaseOrderQueries.purchaseOrderList(), String.join(" AND ",
                "a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()),
                "a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID()),
                "a.sSupplier LIKE " + SQLUtil.toSQL("%" + fsSupplierID),
                "a.sCategrCd = " + SQLUtil.toSQL(Master().getCategoryCode()),
                "a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsReferID)));
        lsSQL += andIfAny(PurchaseOrderQueries.statusOrPending("a.cTranStat", psTranStat, fsPendingCodes));
        lsSQL += branchFilter("a.sBranchCd");
        lsSQL += " ORDER BY a.dTransact, e.sCompnyNm, a.sTransNox ASC";
        LOG.log(Level.FINE, lsSQL);

        paPOMaster = new ArrayList<>();
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (loRS == null) {
            return errorJSON("Query execution failed.");
        }
        try {
            while (loRS.next()) {
                Model_PO_Master loMaster = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
                loMaster.openRecord(loRS.getString("sTransNox"));
                paPOMaster.add(loMaster);
            }
        } finally {
            MiscUtil.close(loRS);
        }
        return successJSON("Record loaded successfully.");
    }

    public Model_PO_Master POMaster(int row) {
        return paPOMaster.get(row);
    }

    public int getPOMasterCount() {
        return paPOMaster.size();
    }

    /** @deprecated never filled; kept so existing screens still compile. */
    @Deprecated
    public Model_Inv_Stock_Request_Master InvStockRequestMaster(int row) {
        throw new IndexOutOfBoundsException("No stock request masters are loaded.");
    }

    /** @deprecated never filled; always 0. */
    @Deprecated
    public int getInvStockRequestCount() {
        return 0;
    }

    public String getInventoryTypeCode() throws SQLException {
        ResultSet loRS = poGRider.executeQuery(PurchaseOrderQueries.inventoryType(Master().getIndustryID(), Master().getCategoryCode()));
        if (loRS == null) {
            return null;
        }
        try {
            return loRS.next() ? loRS.getString("sInvTypCd") : null;
        } finally {
            MiscUtil.close(loRS);
        }
    }

    /** " AND " + condition, or empty when there is no condition. */
    private static String andIfAny(String fsCondition) {
        return fsCondition.isEmpty() ? "" : " AND " + fsCondition;
    }

    /** Branches other than main office / warehouse see their own records only. */
    private String branchFilter(String fsColumn) {
        if (poGRider.isMainOffice() || poGRider.isWarehouse()) {
            return "";
        }
        return " AND " + fsColumn + " = " + SQLUtil.toSQL(poGRider.getBranchCode());
    }

    // =====================================================================
    // Source documents (stock requests, PO quotations)
    // =====================================================================
    public JSONObject getApprovedStockRequests() throws SQLException, GuanzonException {
        return result(SourceDocuments().getAvailableSources());
    }

    public JSONObject addStockRequestOrdersToPODetail(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        return result(SourceDocuments().addStockRequestLines(transactionNo));
    }

    public JSONObject addPOQuotationToPODetail(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        return result(SourceDocuments().addQuotationLines(transactionNo));
    }

    // =====================================================================
    // Searches (same methods as before)
    // =====================================================================
    public JSONObject SearchBranch(String value, boolean byCode) throws SQLException, GuanzonException {
        return result(Search().branch(value, byCode));
    }

    public JSONObject SearchIndustry(String value, boolean byCode) throws SQLException, GuanzonException {
        return result(Search().industry(value, byCode));
    }

    public JSONObject SearchCategory(String value, boolean byCode) throws SQLException, GuanzonException {
        return result(Search().category(value, byCode));
    }

    public JSONObject SearchTerm(String value, boolean byCode) throws SQLException, GuanzonException {
        return result(Search().term(value, byCode));
    }

    public JSONObject SearchBarcode(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws SQLException, GuanzonException, CloneNotSupportedException {
        return result(Search().inventory(value, byCode, row, hasNoSupplier, true, RecordStatus.ACTIVE, "Model: ", false));
    }

    public JSONObject SearchBarcodeGeneral(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws SQLException, GuanzonException, CloneNotSupportedException {
        return result(Search().inventory(value, byCode, row, hasNoSupplier, false, RecordStatus.ACTIVE, "Model: ", false));
    }

    public JSONObject SearchBarcodeDescription(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws SQLException, GuanzonException, CloneNotSupportedException {
        return result(Search().inventory(value, byCode, row, hasNoSupplier, true, RecordStatus.ACTIVE, "Barcode: ", false));
    }

    public JSONObject SearchBarcodeDescriptionGeneral(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws SQLException, GuanzonException, CloneNotSupportedException {
        return result(Search().inventory(value, byCode, row, hasNoSupplier, false, RecordStatus.ACTIVE, "Barcode: ", false));
    }

    /** Includes inactive models (record statuses 1, 2, 3) and copies the brand. */
    public JSONObject SearchModel(String value, boolean byCode, int row, boolean hasNoSupplier) throws SQLException, GuanzonException {
        return result(Search().inventory(value, byCode, row, hasNoSupplier, true, "123", "Barcode: ", true));
    }

    public JSONObject SearchSupplier(String value, boolean byCode) throws SQLException, GuanzonException {
        return result(Search().supplier(value, byCode));
    }

    public JSONObject SearchProject(String value) throws SQLException, GuanzonException {
        return result(Search().project(value));
    }

    public JSONObject SearchPayee(String value) throws SQLException, GuanzonException {
        return result(AdvancePayment().searchPayee(value));
    }

    public JSONObject SearchCompany(String value, boolean byCode) throws SQLException, GuanzonException {
        return result(Search().company(value, byCode));
    }

    public JSONObject SearchDestination(String value, boolean byCode) throws SQLException, GuanzonException {
        return result(Search().destination(value, byCode));
    }

    public JSONObject SearchDepartment(String value, boolean byCode) throws SQLException, GuanzonException {
        return result(Search().department(value, byCode));
    }

    public JSONObject SearchBrand(String value, boolean byCode, int row) throws SQLException, GuanzonException {
        return result(Search().brand(value, byCode, row));
    }

    public JSONObject SearchCategoryReports(String value, boolean byCode) throws SQLException, GuanzonException {
        return result(Search().categoryForReport(value, byCode));
    }

    public JSONObject SearchDestinationReports(String value, boolean byCode) throws SQLException, GuanzonException {
        return result(Search().destinationForReport(value, byCode));
    }

    public JSONObject SearchBranchReports(String value, boolean byCode) throws SQLException, GuanzonException {
        return result(Search().branchForReport(value, byCode));
    }

    public JSONObject SearchSupplierReports(String value, boolean byCode) throws SQLException, GuanzonException {
        return result(Search().supplierForReport(value, byCode));
    }

    // =====================================================================
    // Discounts, advance payment and totals.
    // Rules live in the validator; on a failed rule the field is reset to 0.
    // =====================================================================
    public JSONObject setDiscountRate(String fsValue) {
        return applyAmount(fsValue, lnValue -> rules().validateDiscountRate(lnValue),
                lnValue -> Master().setDiscount(lnValue));
    }

    public JSONObject setDiscountAmount(String fsValue) {
        return applyAmount(fsValue, lnValue -> rules().validateDiscountAmount(lnValue),
                lnValue -> Master().setAdditionalDiscount(lnValue));
    }

    public JSONObject setAdvancePaymentRate(String fsValue) {
        return applyAmount(fsValue, lnValue -> rules().validateAdvancePaymentRate(lnValue),
                lnValue -> Master().setDownPaymentRatesPercentage(lnValue));
    }

    public JSONObject setAdvancePaymentAmount(String fsValue) {
        return applyAmount(fsValue, lnValue -> rules().validateAdvancePaymentAmount(lnValue),
                lnValue -> Master().setDownPaymentRatesAmount(lnValue));
    }

    @FunctionalInterface
    private interface AmountRule {

        JSONObject check(double fnValue);
    }

    /**
     * Parses the entry, checks its rule, sets it, and checks the resulting net
     * total. On any failure the field is set back to 0.
     */
    private JSONObject applyAmount(String fsValue, AmountRule foRule, Consumer<Double> foSetter) {
        double lnValue;
        try {
            lnValue = fsValue == null || fsValue.trim().isEmpty()
                    ? 0.0
                    : new BigDecimal(fsValue.replace(",", "").trim()).doubleValue();
        } catch (NumberFormatException ex) {
            return result(resetAmount(foSetter, errorJSON("Invalid amount: " + fsValue)));
        }

        JSONObject loJSON = foRule.check(lnValue);
        if (!isSuccess(loJSON)) {
            return result(resetAmount(foSetter, loJSON));
        }

        foSetter.accept(lnValue);
        computeNetTotal();

        loJSON = rules().validateNetTotal();
        if (!isSuccess(loJSON)) {
            return result(resetAmount(foSetter, loJSON));
        }
        return result(loJSON);
    }

    private JSONObject resetAmount(Consumer<Double> foSetter, JSONObject foError) {
        foSetter.accept(PurchaseOrderStaticData.default_value_double);
        computeNetTotal();
        return foError;
    }

    /** Net total = total - total x discount% - discount amount (4 decimals). */
    public void computeNetTotal() {
        BigDecimal ldTotal = PurchaseOrderAdvancePayment.money(Master().getTranTotal());
        BigDecimal ldRateDiscount = ldTotal.multiply(PurchaseOrderAdvancePayment.money(Master().getDiscount()))
                .divide(HUNDRED, 4, RoundingMode.HALF_UP);
        BigDecimal ldNet = ldTotal.subtract(ldRateDiscount)
                .subtract(PurchaseOrderAdvancePayment.money(Master().getAdditionalDiscount()));
        Master().setNetTotal(ldNet.doubleValue());
    }

    /** Net total plus this line must stay below the limit; otherwise the line quantity goes back to 0. */
    public JSONObject netTotalChecker(int pnRow) {
        BigDecimal ldLine = PurchaseOrderAdvancePayment.money(Detail(pnRow).getQuantity())
                .multiply(PurchaseOrderAdvancePayment.money(Detail(pnRow).getUnitPrice()));
        JSONObject loJSON = rules().validateNetTotalWithLine(ldLine.doubleValue());
        if (!isSuccess(loJSON)) {
            Detail(pnRow).setQuantity(0);
            computeNetTotal();
        }
        return result(loJSON);
    }

    // =====================================================================
    // History
    // =====================================================================
    public void ShowApprovalHistory() throws Exception {
        CachedRowSet loHistory = getApprovalHistory();

        JSONObject loEntry = MiscUtil.getEntryBy(poGRider, poMaster.getTable(), (String) poMaster.getValue("sTransNox"));
        String lsEntryBy = isSuccess(loEntry) ? (String) loEntry.get("sCompnyNm") : "";
        String lsEntryDate = isSuccess(loEntry) ? (String) loEntry.get("sEntryDte") : "";

        showApprovalHistoryUI("Purchase Order", (String) poMaster.getValue("sTransNox"), lsEntryBy, lsEntryDate,
                Double.parseDouble(poMaster.getValue("nTranTotl").toString()), loHistory);
    }

    public void ShowStatusHistory() throws Exception {
        CachedRowSet loHistory = getStatusHistory();
        loHistory.beforeFirst();
        while (loHistory.next()) {
            loHistory.updateString("cRefrStat", POStatus.historyLabel(loHistory.getString("cRefrStat")));
            loHistory.updateRow();
        }

        JSONObject loEntry = getEntryBy();
        String lsEntryBy = isSuccess(loEntry) ? (String) loEntry.get("sCompnyNm") : "";
        String lsEntryDate = isSuccess(loEntry) ? (String) loEntry.get("sEntryDte") : "";

        showStatusHistoryUI("Purchase Order", (String) poMaster.getValue("sTransNox"), lsEntryBy, lsEntryDate, loHistory);
    }

    // =====================================================================
    // Reports
    // =====================================================================
    /**
     * Summary report rows (one per PO) for the statuses in psTranStat.
     * Empty filters are ignored. Encoders see their own branch only.
     */
    public JSONObject RetrieveSummaryReports(LocalDate dateFrom, LocalDate dateThru,
            String branch, String destination, String supplier, String category) throws SQLException, GuanzonException {
        List<String> laFilter = reportFilters("a", "a.sTransNox", "a.sDestinat", dateFrom, dateThru, branch, destination, supplier, category);
        String lsSQL = PurchaseOrderQueries.summaryReport()
                + " WHERE " + String.join(" AND ", laFilter)
                + " ORDER BY a.dTransact, a.sTransNox ASC";

        return result(readReport(lsSQL, loRS -> {
            JSONObject loRow = new JSONObject();
            loRow.put("Supplier", loRS.getString("Supplier"));
            loRow.put("Destination", loRS.getString("Destination"));
            loRow.put("Branch", loRS.getString("Branch"));
            loRow.put("sTransNox", loRS.getString("sTransNox"));
            loRow.put("sReferNox", loRS.getString("sReferNox"));
            loRow.put("dTransact", loRS.getDate("dTransact"));
            loRow.put("Category", loRS.getString("Category"));
            loRow.put("Term", loRS.getString("Term"));
            loRow.put("cTranStat", POStatus.label(loRS.getString("cTranStat")));
            loRow.put("Total", loRS.getDouble("nTranTotl"));
            return loRow;
        }));
    }

    /**
     * Detailed report rows (one per PO line) for the statuses in psTranStat.
     * Empty filters are ignored. Encoders see their own branch only.
     */
    public JSONObject RetrieveSummaryDetailedReports(LocalDate dateFrom, LocalDate dateThru,
            String branch, String destination, String supplier, String category) throws SQLException, GuanzonException {
        List<String> laFilter = reportFilters("b", "a.sTransNox", "b.sDestinat", dateFrom, dateThru, branch, destination, supplier, category);
        String lsSQL = PurchaseOrderQueries.detailedReport()
                + " WHERE " + String.join(" AND ", laFilter)
                + " ORDER BY b.dTransact, a.sTransNox, a.nEntryNox ASC";

        return result(readReport(lsSQL, loRS -> {
            JSONObject loRow = new JSONObject();
            loRow.put("Supplier", loRS.getString("Supplier"));
            loRow.put("Destination", loRS.getString("Destination"));
            loRow.put("sTransNox", loRS.getString("sTransNox"));
            loRow.put("dTransact", loRS.getDate("dTransact"));
            loRow.put("sBarCodex", loRS.getString("sBarCodex"));
            loRow.put("Description", loRS.getString("Description"));
            loRow.put("Brand", loRS.getString("Brand"));
            loRow.put("ModelCode", loRS.getString("ModelCode"));
            loRow.put("ModelName", loRS.getString("ModelName"));
            loRow.put("Color", loRS.getString("Color"));
            loRow.put("Quantity", loRS.getDouble("nQuantity"));
            loRow.put("nReceived", loRS.getDouble("nReceived"));
            loRow.put("nCancelld", loRS.getDouble("nCancelld"));
            loRow.put("UnitPrice", loRS.getDouble("nUnitPrce"));
            loRow.put("Total", loRS.getDouble("Total"));
            return loRow;
        }));
    }

    /** @deprecated misspelled; use {@link #RetrieveSummaryReports}. The first parameter was never used. */
    @Deprecated
    public JSONObject RetriveSummaryReports(Boolean issummarized, LocalDate dateFrom, LocalDate dateThru,
            String Branch, String Destination, String Supplier, String Category) throws SQLException, GuanzonException {
        return RetrieveSummaryReports(dateFrom, dateThru, Branch, Destination, Supplier, Category);
    }

    /** @deprecated misspelled; use {@link #RetrieveSummaryDetailedReports}. The first parameter was never used. */
    @Deprecated
    public JSONObject RetriveSummaryDetailedReports(Boolean issummarized, LocalDate dateFrom, LocalDate dateThru,
            String Branch, String Destination, String Supplier, String Category) throws SQLException, GuanzonException {
        return RetrieveSummaryDetailedReports(dateFrom, dateThru, Branch, Destination, Supplier, Category);
    }

    /**
     * Filters shared by both reports. Remembers the date range for the printed
     * report header.
     *
     * @param fsMaster alias of PO_Master in the report query
     */
    private List<String> reportFilters(String fsMaster, String fsTransNoColumn, String fsDestinationColumn,
            LocalDate fdFrom, LocalDate fdThru, String fsBranch, String fsDestination, String fsSupplier, String fsCategory) {
        List<String> laFilter = new ArrayList<>();
        laFilter.add(fsMaster + ".sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID()));
        laFilter.add(fsMaster + ".sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()));

        psReportDateFrom = fdFrom == null ? "" : fdFrom.toString();
        psReportDateThru = fdThru == null ? "" : fdThru.toString();
        if (fdFrom != null && fdThru != null) {
            laFilter.add(fsMaster + ".dTransact BETWEEN " + SQLUtil.toSQL(java.sql.Date.valueOf(fdFrom))
                    + " AND " + SQLUtil.toSQL(java.sql.Date.valueOf(fdThru)));
        }
        if (fsBranch != null && !fsBranch.trim().isEmpty()) {
            laFilter.add(fsMaster + ".sBranchCd = " + SQLUtil.toSQL(fsBranch));
        }
        if (fsDestination != null && !fsDestination.trim().isEmpty()) {
            laFilter.add(fsDestinationColumn + " = " + SQLUtil.toSQL(fsDestination));
        }
        if (fsSupplier != null && !fsSupplier.trim().isEmpty()) {
            laFilter.add(fsMaster + ".sSupplier = " + SQLUtil.toSQL(fsSupplier));
        }
        if (fsCategory != null && !fsCategory.trim().isEmpty()) {
            laFilter.add(fsMaster + ".sCategrCd = " + SQLUtil.toSQL(fsCategory));
        }
        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            laFilter.add(fsTransNoColumn + " LIKE " + SQLUtil.toSQL(poGRider.getBranchCode() + "%"));
        }
        String lsStatus = PurchaseOrderQueries.statusIn(fsMaster + ".cTranStat", psTranStat);
        if (!lsStatus.isEmpty()) {
            laFilter.add(lsStatus);
        }
        return laFilter;
    }

    @FunctionalInterface
    private interface RowReader {

        JSONObject read(ResultSet foRS) throws SQLException;
    }

    /** Runs a report query: "data" holds the rows; "error" with empty data when there are none. */
    private JSONObject readReport(String fsSQL, RowReader foReader) throws SQLException {
        LOG.log(Level.FINE, fsSQL);
        ResultSet loRS = poGRider.executeQuery(fsSQL);
        if (loRS == null) {
            return errorJSON("Query execution failed.");
        }

        JSONArray laData = new JSONArray();
        try {
            while (loRS.next()) {
                laData.add(foReader.read(loRS));
            }
        } finally {
            MiscUtil.close(loRS);
        }

        JSONObject loJSON = laData.isEmpty() ? errorJSON("No records found.") : successJSON("Record(s) loaded successfully.");
        loJSON.put("data", laData);
        return loJSON;
    }

    // =====================================================================
    // Helpers: override a create... method to give an industry its own version
    // =====================================================================
    protected PurchaseOrderSourceDocuments createSourceDocuments() {
        return new PurchaseOrderSourceDocuments(this, poGRider,
                () -> new InvWarehouseControllers(poGRider, logwrapr),
                () -> new QuotationControllers(poGRider, logwrapr),
                this::AddDetail);
    }

    protected PurchaseOrderAdvancePayment createAdvancePayment() {
        return new PurchaseOrderAdvancePayment(this, poGRider, () -> new CashflowControllers(poGRider, logwrapr));
    }

    protected PurchaseOrderSearch createSearch() {
        return new PurchaseOrderSearch(this,
                () -> new ParamControllers(poGRider, logwrapr),
                () -> new InvControllers(poGRider, logwrapr),
                () -> new ClientControllers(poGRider, logwrapr));
    }

    protected PurchaseOrderAttachments createAttachments() {
        return new PurchaseOrderAttachments(poGRider, getSourceCode());
    }

    protected PurchaseOrderAudit createAudit() {
        return new PurchaseOrderAudit(poGRider, getSourceCode());
    }

    protected PurchaseOrderPrinter createPrinter() {
        return new PurchaseOrderPrinter(this, poGRider, Audit());
    }

    public PurchaseOrderSourceDocuments SourceDocuments() {
        if (poSourceDocuments == null) {
            poSourceDocuments = createSourceDocuments();
        }
        return poSourceDocuments;
    }

    public PurchaseOrderAdvancePayment AdvancePayment() {
        if (poAdvancePayment == null) {
            poAdvancePayment = createAdvancePayment();
        }
        return poAdvancePayment;
    }

    public PurchaseOrderSearch Search() {
        if (poSearch == null) {
            poSearch = createSearch();
        }
        return poSearch;
    }

    public PurchaseOrderAttachments Attachments() {
        if (poAttachments == null) {
            poAttachments = createAttachments();
        }
        return poAttachments;
    }

    public PurchaseOrderAudit Audit() {
        if (poAudit == null) {
            poAudit = createAudit();
        }
        return poAudit;
    }

    public PurchaseOrderPrinter Printer() {
        if (poPrinter == null) {
            poPrinter = createPrinter();
        }
        return poPrinter;
    }

    // ---- Attachments (same methods as before) ----
    public void resetattachment() {
        Attachments().reset();
    }

    public TransactionAttachment TransactionAttachmentList(int row) {
        return Attachments().get(row);
    }

    public int getTransactionAttachmentCount() {
        return Attachments().getCount();
    }

    public JSONObject addAttachment() throws SQLException, GuanzonException {
        return result(Attachments().add());
    }

    public int addAttachment(String fFileName) throws SQLException, GuanzonException {
        return Attachments().add(fFileName, Master().getTransactionNo());
    }

    public JSONObject removeAttachment(int fnRow) throws GuanzonException, SQLException {
        return result(Attachments().remove(fnRow));
    }

    public void copyFile(String fsPath) {
        result(Attachments().copyToTemp(fsPath));
    }

    public JSONObject loadAttachments() throws SQLException, GuanzonException {
        return result(Attachments().load(Master().getTransactionNo(), Master().getEditMode() == EditMode.UPDATE));
    }

    /** @deprecated use {@code Attachments().upload(row, originalFileName)}; instance and access are not needed. */
    @Deprecated
    public JSONObject uploadCASAttachments(GRiderCAS instance, String access, int fnRow, String fsOriginalFileName) throws Exception {
        return result(Attachments().upload(fnRow, fsOriginalFileName));
    }

    /** Error when the file name is already used by a saved attachment. */
    public JSONObject checkExistingFileName(String fsFileName) throws SQLException, GuanzonException {
        if (Attachments().isFileNameUsed(fsFileName)) {
            return result(errorJSON("File name already exist in database.\nTry changing the file name to upload."));
        }
        return result(successJSON());
    }

    // ---- Audit trail ----
    public List<String> getApprover() throws SQLException {
        return Audit().getApprovers(Master().getTransactionNo());
    }

    public String getSysUser(String fsId) throws SQLException, GuanzonException {
        return Audit().getSysUserName(fsId);
    }

    public JSONObject getEntryBy() throws SQLException, GuanzonException {
        return result(Audit().getEntryBy(Master().getTable(), Master().getTransactionNo()));
    }

    public JSONObject getConfirmedBy() throws SQLException, GuanzonException {
        return result(Audit().getConfirmedBy(Master().getTransactionNo()));
    }

    // ---- Printing ----
    public JSONObject printTransaction(String jasperType) {
        return result(Printer().printTransaction(jasperType));
    }

    /** Prints the summary/detailed report for the date range of the last Retrieve...Reports call. */
    public JSONObject printReports(Boolean isSummarized, JSONArray reportData) {
        return result(Printer().printReports(Boolean.TRUE.equals(isSummarized), reportData, psReportDateFrom, psReportDateThru));
    }

    /** Same as {@link #printReports(Boolean, JSONArray)}. */
    public JSONObject PrintSummaryReports(Boolean isSummarized, JSONArray reportData) {
        return printReports(isSummarized, reportData);
    }

    public static String formatDateToText(String fsDate) {
        return PurchaseOrderPrinter.formatDateToText(fsDate);
    }

    // =====================================================================
    // Engineering project title (General Inventory only)
    // =====================================================================
    private Model_Project Project() {
        if (poProject == null) {
            poProject = new ParamModels(poGRider).Project();
        }
        return poProject;
    }

    /** Opens the referenced project and tags (OPEN) or untags (CANCELLED/VOID) it. */
    private JSONObject setProjectTitle(String fsStatus) throws SQLException, GuanzonException, CloneNotSupportedException {
        Model_Project loProject = Project();
        loProject.initialize();

        JSONObject loJSON = loProject.openRecord(Master().getReference());
        if ("error".equalsIgnoreCase((String) loJSON.get("result"))) {
            return loJSON;
        }
        loJSON = loProject.updateRecord();
        if ("error".equalsIgnoreCase((String) loJSON.get("result"))) {
            return loJSON;
        }

        switch (fsStatus) {
            case PurchaseOrderStatus.OPEN:
                loJSON = loProject.isUsed(true);
                break;
            case PurchaseOrderStatus.CANCELLED:
            case PurchaseOrderStatus.VOID:
                loJSON = loProject.isUsed(false);
                break;
            default:
                break;
        }
        loProject.setModifiedDate(poGRider.getServerDate());
        loProject.setModifyingId(poGRider.getUserID());

        if ("error".equalsIgnoreCase((String) loJSON.get("result"))) {
            return loJSON;
        }
        return successJSON();
    }

    private JSONObject saveProjectTitle() throws SQLException, GuanzonException, CloneNotSupportedException {
        JSONObject loJSON = Project().saveRecord();
        if ("error".equalsIgnoreCase((String) loJSON.get("result"))) {
            return loJSON;
        }
        return successJSON();
    }

    /**
     * Whether a project is used by more than one PO.
     *
     * @return "result" ("error" when used by more than one PO), "message", "count"
     */
    public JSONObject CheckProjectUsage(String fsProjectCode) throws SQLException, GuanzonException {
        int lnCount = generalRules(Master().getTransactionStatus()).getProjectUsageCount(fsProjectCode);
        JSONObject loJSON = lnCount > 1
                ? errorJSON("Project is already used in multiple transactions.")
                : successJSON("No usage found. You can proceed.");
        loJSON.put("count", lnCount);
        return result(loJSON);
    }
}
