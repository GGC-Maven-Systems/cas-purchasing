package ph.com.guanzongroup.cas.purchasing.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.ActionAuthManager;
import org.guanzon.appdriver.agent.MatrixAuthChecker;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.agent.systables.TransactionAttachment;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.inv.InvTransCons;
import org.guanzon.cas.inv.InventoryTransaction;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderStaticData;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.purchasing.status.POStatus;
import ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrderValidator;

/**
 * Abstract base controller for Purchase Order.
 *
 * <p>
 * Handles only the main procedures shared by every industry:</p>
 * <ul>
 * <li>{@link #InitTransaction()}</li>
 * <li>{@link #NewTransaction()}</li>
 * <li>{@link #OpenTransaction(String)}</li>
 * <li>{@link #UpdateTransaction()}</li>
 * <li>{@link #SaveTransaction()}</li>
 * <li>{@link #ConfirmTransaction(String)}</li>
 * <li>{@link #ApproveTransaction(String)}</li>
 * <li>{@link #CancelTransaction(String)}</li>
 * <li>{@link #PostTransaction(String)}</li>
 * <li>{@link #VoidTransaction(String)}</li>
 * <li>{@link #ReturnTransaction(String)}</li>
 * <li>{@link #POCancelTransaction()}, {@link #RevertStatus()}: status writes
 * inside a parent's DB transaction</li>
 * </ul>
 *
 * <p>
 * It also holds what every industry shares: the detail grid (AddDetail,
 * RemoveDetail, ...), discounts and totals (setDiscountRate, computeNetTotal,
 * ...), attachments (addAttachment, loadAttachments, ...), the audit trail
 * (getApprover, getEntryBy, getConfirmedBy, ...), the save rules
 * {@link #checkConfirmedEditApproval()} and {@link #checkReturnedEdit()} (run
 * by willSave), and the searches (SearchBranch, SearchSupplier, SearchBarcode,
 * ...) with the {@link #Search()} helper behind
 * them.</p>
 *
 * <p>
 * Every procedure is overridable. An industry controller extends this class
 * (PurchaseOrderGeneral, PurchaseOrderMC, PurchaseOrderMP, PurchaseOrderSP,
 * PurchaseOrderAppliance) and either overrides a whole procedure, or plugs
 * into it through the protected hooks:</p>
 * <ul>
 * <li>{@link #createValidator()} (required): the industry's rules.</li>
 * <li>{@link #initSQL()} (required): browse SQL.</li>
 * <li>{@link #prepareIndustrySave()}, {@link #saveIndustryRecords()}: the
 * industry's own save steps ({@link #willSave()} and {@link #saveOthers()}
 * already run the shared ones).</li>
 * <li>{@link #resetOthers()}: clear the industry's own objects on New /
 * Open.</li>
 * <li>{@link #initOthers()}: extra objects created by InitTransaction.</li>
 * <li>{@link #prepareStatusChange(String)}: work before a status-change
 * transaction starts.</li>
 * <li>{@link #processStatusChange(String, String)}: work inside the
 * status-change transaction, before the new status is written.</li>
 * <li>{@link #postInventoryLedger(String)}: inventory ledger on approve /
 * cancel.</li>
 * <li>{@link #prepareRules(PurchaseOrderValidator)}: extra context for the
 * validator.</li>
 * </ul>
 *
 * <p>
 * Business rules live in the validators (see {@link PurchaseOrderValidator}).
 * Dialogs go through a {@link UserPrompt} ({@link #setUserPrompt}), so the
 * controller also runs without a screen.</p>
 *
 * <p>
 * Results: every method builds its own result object. The inherited
 * {@code poJSON} field is only set when a public method returns (through
 * {@link #result(JSONObject)}), for code that still reads it; never read it to
 * get the result of another call.</p>
 */
public abstract class PurchaseOrder extends Transaction {

    private UserPrompt poPrompt;
    /** Detail rows removed from the PO while editing. */
    protected List<Model> paDetailRemoved = new ArrayList<>();
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    // =====================================================================
    // User interface
    // =====================================================================
    /** Dialogs used by this controller (default: JavaFX dialogs). */
    public void setUserPrompt(UserPrompt foPrompt) {
        poPrompt = foPrompt;
    }

    protected UserPrompt prompt() {
        if (poPrompt == null) {
            poPrompt = new FxUserPrompt();
        }
        return poPrompt;
    }

    // =====================================================================
    // Procedures
    // =====================================================================
    /** Creates the master/detail models, then the industry's own objects (initOthers). */
    public JSONObject InitTransaction() {
        SOURCE_CODE = InvTransCons.PURCHASE_ORDER;
        poMaster = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
        poDetail = new PurchaseOrderModels(poGRider).PurchaseOrderDetails();
        paDetail = new ArrayList<>();
        paDetailRemoved = new ArrayList<>();

        initOthers();

        return initialize();
    }

    public JSONObject NewTransaction() throws CloneNotSupportedException, SQLException, GuanzonException {
        if (!pbWthParent) {
            JSONObject loJSON = seekApproval();
            if (!isSuccess(loJSON)) {
                return result(loJSON);
            }
        }

        clearEditState();
        return result(newTransaction());
    }

    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        if (!pbWthParent) {
            JSONObject loJSON = seekApproval();
            if (!isSuccess(loJSON)) {
                return result(loJSON);
            }
        }

        clearEditState();
        return result(openTransaction(transactionNo));
    }

    public JSONObject UpdateTransaction() throws SQLException, GuanzonException {
        if (!pbWthParent) {
            JSONObject loJSON = seekApproval();
            if (!isSuccess(loJSON)) {
                return result(loJSON);
            }
        }

        paDetailRemoved.clear();
        return result(updateTransaction());
    }

    /** Saves through the framework: willSave, save (validator), saveOthers, saveComplete. */
    public JSONObject SaveTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        return result(saveTransaction());
    }

    public JSONObject ConfirmTransaction(String remarks) throws ParseException, SQLException, CloneNotSupportedException, GuanzonException {
        return result(changeStatus(PurchaseOrderStatus.CONFIRMED, remarks, remarks,
                "ConfirmTransaction", "Transaction confirmed successfully.", true));
    }

    /**
     * Approves the transaction. When the authorization matrix files a pending
     * request, the result is "success" with a "request submitted" message (the
     * UI treats it as done), as before.
     */
    public JSONObject ApproveTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        JSONObject loJSON = changeStatus(PurchaseOrderStatus.APPROVED, remarks, "Approval matrix",
                "Approve Transaction", "Transaction approved successfully.", true);

        if ("matrix".equals((String) loJSON.get("result"))) {
            loJSON = successJSON("Transaction approving request submitted successfully.");
        }
        return result(loJSON);
    }

    public JSONObject CancelTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        return result(changeStatus(PurchaseOrderStatus.CANCELLED, remarks, remarks,
                "Cancel Transaction", "Transaction cancelled successfully.", true));
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
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
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
    // Status-change template (used by the procedures above; industries may
    // reuse it for their own status procedures, e.g. Post or Void)
    // =====================================================================
    /**
     * Shared flow for a status change:
     * <ol>
     * <li>a transaction must be loaded;</li>
     * <li>validator + authorization ({@link #authorizeStatus});</li>
     * <li>{@link #prepareStatusChange(String)} (before the DB transaction);</li>
     * <li>inside one DB transaction: {@link #processStatusChange},
     * {@link #postInventoryLedger}, write the new status, post the
     * authorization.</li>
     * </ol>
     *
     * @param fsStatus new status
     * @param fsRemarks remarks saved with the status change
     * @param fsPendingRemarks remarks saved when the matrix files a pending request
     * @param fsEvent event name for the DB transaction log
     * @param fsSuccessMessage message on success
     * @param fbOwnTrans false when a parent already owns the DB transaction
     * @return "success", "error", or "matrix" (a pending authorization request was filed)
     */
    protected JSONObject changeStatus(String fsStatus, String fsRemarks, String fsPendingRemarks,
            String fsEvent, String fsSuccessMessage, boolean fbOwnTrans)
            throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        if (getEditMode() != EditMode.READY) {
            return errorJSON("No transaction was loaded.");
        }

        final MatrixAuthChecker[] loCheck = new MatrixAuthChecker[1];
        JSONObject loJSON = authorizeStatus(fsStatus, fsPendingRemarks, fsEvent, loCheck);
        if (!isSuccess(loJSON)) {
            return loJSON; //error or "matrix"
        }

        loJSON = prepareStatusChange(fsStatus);
        if (!isSuccess(loJSON)) {
            return loJSON;
        }

        return inTransaction(fsEvent, fbOwnTrans, () -> {
            JSONObject loStep = processStatusChange(fsStatus, fsRemarks);
            if (!isSuccess(loStep)) {
                return loStep;
            }

            loStep = postInventoryLedger(fsStatus);
            if (!isSuccess(loStep)) {
                return loStep;
            }

            loStep = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), fsRemarks, fsStatus, false, true);
            if (!isSuccess(loStep)) {
                return loStep;
            }

            if (loCheck[0] != null) {
                loCheck[0].postAuth();
            }
            return successJSON(fsSuccessMessage);
        });
    }

    // =====================================================================
    // Hooks (override in the industry controller)
    // =====================================================================
    /** Validator for this controller's industry. Must extend {@link PurchaseOrderValidator}. */
    protected abstract PurchaseOrderValidator createValidator();

    /** Extra objects created by InitTransaction. Default: none. */
    protected void initOthers() {
    }

    /**
     * Work before the status-change DB transaction starts (e.g. prepare the
     * source documents). A non-success result stops the change. Default: none.
     */
    protected JSONObject prepareStatusChange(String fsStatus)
            throws SQLException, GuanzonException, CloneNotSupportedException {
        return successJSON();
    }

    /**
     * Work inside the status-change DB transaction, before the new status is
     * written (e.g. save the source documents). A non-success result rolls the
     * whole change back. Default: none.
     */
    protected JSONObject processStatusChange(String fsStatus, String fsRemarks) throws Exception {
        return successJSON();
    }

    /**
     * Inventory ledger, inside the status-change DB transaction.
     * <ul>
     * <li>APPROVED: records the order quantities.</li>
     * <li>CANCELLED of an APPROVED PO: reverses that entry.</li>
     * </ul>
     * Override to change or skip it for an industry.
     */
    protected JSONObject postInventoryLedger(String fsStatus) throws Exception {
        boolean lbReverse;
        if (PurchaseOrderStatus.APPROVED.equals(fsStatus)) {
            lbReverse = false;
        } else if (PurchaseOrderStatus.CANCELLED.equals(fsStatus)
                && PurchaseOrderStatus.APPROVED.equalsIgnoreCase((String) poMaster.getValue("cTranStat"))) {
            lbReverse = true;
        } else {
            return successJSON();
        }

        InventoryTransaction loTrans = new InventoryTransaction(poGRider, psBranchCode);
        loTrans.PurchaseOrder((String) poMaster.getValue("sTransNox"), (Date) poMaster.getValue("dTransact"), lbReverse);
        for (Model loDetail : paDetail) {
            Model_PO_Detail detail = (Model_PO_Detail) loDetail;
            loTrans.addDetail((String) poMaster.getValue("sIndstCdx"), detail.getStockID(), "0", 0,
                    detail.getQuantity().doubleValue(), detail.getUnitPrice().doubleValue());
        }
        loTrans.saveTransaction();
        return successJSON();
    }

    /**
     * Hook: clear the industry's own objects when a PO is created or opened, so
     * nothing from the previous PO carries over. Default: nothing.
     */
    protected void resetOthers() {
    }

    /** Extra context for the validator. Default: none; removed details and the attachment count are always passed. */
    protected void prepareRules(PurchaseOrderValidator foRules) {
    }

    // =====================================================================
    // Framework callbacks
    // =====================================================================
    /** Browse SQL used by the industry's search (sets SQL_BROWSE). */
    @Override
    public abstract void initSQL();

    /**
     * Before saving, in this order: prepare the rows (transaction no., drop
     * empty rows, numbering); the shared save rules
     * ({@link #checkConfirmedEditApproval()}, {@link #checkReturnedEdit()});
     * the industry's own preparation ({@link #prepareIndustrySave()}); the
     * attachments (link to this PO, rename duplicates, upload unsent files).
     */
    @Override
    public JSONObject willSave() throws SQLException, CloneNotSupportedException, GuanzonException {
        prepareDetailsForSave();

        JSONObject loJSON = checkConfirmedEditApproval();
        if (!isSuccess(loJSON)) {
            return loJSON;
        }

        loJSON = checkReturnedEdit();
        if (!isSuccess(loJSON)) {
            return loJSON;
        }

        loJSON = prepareIndustrySave();
        if (!isSuccess(loJSON)) {
            return loJSON;
        }

        return Attachments().prepareForSave(Master().getTransactionNo(), Master().getBranchCode());
    }

    /**
     * Records saved together with the PO, inside its DB transaction: the
     * attachments, then the industry's own records
     * ({@link #saveIndustryRecords()}).
     */
    @Override
    public JSONObject saveOthers() {
        try {
            JSONObject loJSON = Attachments().save();
            if (!isSuccess(loJSON)) {
                return loJSON;
            }
            return saveIndustryRecords();
        } catch (CloneNotSupportedException | SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            return errorJSON(ex.getMessage());
        }
    }

    /**
     * Industry hook: the industry's own preparation before saving, after the
     * shared save rules. Default: nothing.
     */
    protected JSONObject prepareIndustrySave() throws SQLException, CloneNotSupportedException, GuanzonException {
        return successJSON();
    }

    /**
     * Industry hook: the industry's own records saved with the PO, inside its
     * DB transaction, after the attachments. Default: nothing.
     */
    protected JSONObject saveIndustryRecords() throws SQLException, CloneNotSupportedException, GuanzonException {
        return successJSON();
    }

    @Override
    public JSONObject initFields() {
        return successJSON();
    }

    /** Entry validation before saving: validator rules for OPEN. */
    @Override
    protected JSONObject save() throws CloneNotSupportedException, SQLException, GuanzonException {
        return isEntryOkay(PurchaseOrderStatus.OPEN);
    }

    @Override
    protected JSONObject isEntryOkay(String status) throws CloneNotSupportedException {
        return rules(status).validate();
    }

    /** Clears what belongs to the previous PO: removed rows, attachments, then resetOthers(). */
    private void clearEditState() {
        paDetailRemoved.clear();
        Attachments().reset();
        resetOthers();
    }

    @Override
    public void saveComplete() {
        //the removed rows are gone from the database now
        paDetailRemoved.clear();
    }

    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }

    @Override
    public Model_PO_Master Master() {
        return (Model_PO_Master) poMaster;
    }

    @Override
    public Model_PO_Detail Detail(int row) {
        return (Model_PO_Detail) paDetail.get(row);
    }

    // =====================================================================
    // Detail grid (shared by all industries; rules come from the validator)
    // =====================================================================
    /** Adds a row when the last row has an item. */
    public JSONObject AddDetail() throws CloneNotSupportedException {
        JSONObject loJSON = rules().validateCanAddDetail();
        if (!isSuccess(loJSON)) {
            return result(loJSON);
        }
        return result(addDetail());
    }

    /**
     * Removes a detail row while adding or editing the PO.
     *
     * <p>
     * A row that is already saved goes to the removed list
     * ({@link #DetailRemove(int)}, {@link #getDetailRemovedCount()}), so the
     * validator and the source documents (stock requests, quotations) can
     * release what it held. On save the remaining rows are renumbered and the
     * framework deletes the leftover saved rows from PO_Detail.</p>
     *
     * <p>
     * Leaves one empty row when the last row is removed, so the grid always
     * has a row to type in.</p>
     */
    public JSONObject RemoveDetail(int row) throws CloneNotSupportedException {
        if (getEditMode() != EditMode.ADDNEW && getEditMode() != EditMode.UPDATE) {
            return result(errorJSON("Invalid edit mode detected."));
        }
        if (row < 0 || row >= getDetailCount()) {
            return result(errorJSON("Invalid row number."));
        }

        Model loItem = Detail().get(row);
        if (loItem.getEditMode() != EditMode.ADDNEW) {
            paDetailRemoved.add(loItem);
        }
        Detail().remove(row);

        if (getDetailCount() == 0) {
            JSONObject loJSON = addDetail();
            if (!isSuccess(loJSON)) {
                return result(loJSON);
            }
        }
        return result(successJSON());
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
    // Discounts and totals.
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

    /** Rule for an amount entry (see applyAmount). */
    @FunctionalInterface
    protected interface AmountRule {

        JSONObject check(double fnValue);
    }

    /**
     * Parses the entry, checks its rule, sets it, and checks the resulting net
     * total. On any failure the field is set back to 0.
     */
    protected JSONObject applyAmount(String fsValue, AmountRule foRule, Consumer<Double> foSetter) {
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
        BigDecimal ldTotal = money(Master().getTranTotal());
        BigDecimal ldRateDiscount = ldTotal.multiply(money(Master().getDiscount()))
                .divide(HUNDRED, 4, RoundingMode.HALF_UP);
        BigDecimal ldNet = ldTotal.subtract(ldRateDiscount)
                .subtract(money(Master().getAdditionalDiscount()));
        Master().setNetTotal(ldNet.doubleValue());
    }

    /** Net total plus this line must stay below the limit; otherwise the line quantity goes back to 0. */
    public JSONObject netTotalChecker(int pnRow) {
        BigDecimal ldLine = money(Detail(pnRow).getQuantity())
                .multiply(money(Detail(pnRow).getUnitPrice()));
        JSONObject loJSON = rules().validateNetTotalWithLine(ldLine.doubleValue());
        if (!isSuccess(loJSON)) {
            Detail(pnRow).setQuantity(0);
            computeNetTotal();
        }
        return result(loJSON);
    }

    // =====================================================================
    // Save rules shared by all industries (run by willSave)
    // =====================================================================
    /**
     * Save rule for a CONFIRMED PO being edited: an encoder needs an approving
     * officer. Success for any other case. Run by willSave.
     */
    protected JSONObject checkConfirmedEditApproval() throws SQLException, GuanzonException {
        if (PurchaseOrderStatus.CONFIRMED.equals(Master().getTransactionStatus()) && pnEditMode == EditMode.UPDATE
                && poGRider.getUserLevel() <= UserRight.ENCODER) {
            JSONObject loJSON = prompt().getUserApproval(poGRider);
            if (!isSuccess(loJSON)) {
                return loJSON;
            }
            setApproving((String) loJSON.get("sUserIDxx"));
        }
        return successJSON();
    }

    /**
     * Save rule for a RETURNED PO: it must have at least one change.
     * Compares with the saved record, read straight from the database.
     * Success for any other status. Run by willSave.
     */
    protected JSONObject checkReturnedEdit() throws SQLException, GuanzonException, CloneNotSupportedException {
        if (!PurchaseOrderStatus.RETURNED.equals(Master().getTransactionStatus())) {
            return successJSON();
        }

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

    // =====================================================================
    // Attachments (shared by all industries; saved by willSave / saveOthers)
    // =====================================================================
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

    private PurchaseOrderAttachments poAttachments;

    protected PurchaseOrderAttachments createAttachments() {
        return new PurchaseOrderAttachments(poGRider, getSourceCode());
    }

    public PurchaseOrderAttachments Attachments() {
        if (poAttachments == null) {
            poAttachments = createAttachments();
        }
        return poAttachments;
    }

    // =====================================================================
    // Audit trail (shared by all industries; the history screens that use
    // it stay in the industry controller)
    // =====================================================================
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

    private PurchaseOrderAudit poAudit;

    protected PurchaseOrderAudit createAudit() {
        return new PurchaseOrderAudit(poGRider, getSourceCode());
    }

    public PurchaseOrderAudit Audit() {
        if (poAudit == null) {
            poAudit = createAudit();
        }
        return poAudit;
    }

    // =====================================================================
    // Lookups
    // =====================================================================
    /** Inventory type of the PO's industry + category; null when not found. */
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

    // =====================================================================
    // Searches shared by all industries
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

    public JSONObject SearchBarcodeDescription(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws SQLException, GuanzonException, CloneNotSupportedException {
        return result(Search().inventory(value, byCode, row, hasNoSupplier, true, RecordStatus.ACTIVE, "Barcode: ", false));
    }

    /** Includes inactive models (record statuses 1, 2, 3) and copies the brand. */
    public JSONObject SearchModel(String value, boolean byCode, int row, boolean hasNoSupplier) throws SQLException, GuanzonException {
        return result(Search().inventory(value, byCode, row, hasNoSupplier, true, "123", "Barcode: ", true));
    }

    public JSONObject SearchSupplier(String value, boolean byCode) throws SQLException, GuanzonException {
        return result(Search().supplier(value, byCode));
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

    // =====================================================================
    // Search helper: override createSearch() to give
    // an industry its own version
    // =====================================================================
    private PurchaseOrderSearch poSearch;

    protected PurchaseOrderSearch createSearch() {
        return new PurchaseOrderSearch(this,
                () -> new ParamControllers(poGRider, logwrapr),
                () -> new InvControllers(poGRider, logwrapr),
                () -> new ClientControllers(poGRider, logwrapr));
    }

    public PurchaseOrderSearch Search() {
        if (poSearch == null) {
            poSearch = createSearch();
        }
        return poSearch;
    }

    // =====================================================================
    // Shared helpers
    // =====================================================================
    /** Validator loaded with the master, details and industry context for the given status. */
    protected PurchaseOrderValidator rules(String fsStatus) {
        PurchaseOrderValidator loRules = createValidator();
        loRules.setApplicationDriver(poGRider);
        loRules.setTransactionStatus(fsStatus);
        loRules.setMaster(Master());
        loRules.setDetail(new ArrayList<Object>(paDetail));
        loRules.setRemovedDetail(new ArrayList<Object>(paDetailRemoved));
        loRules.setActiveAttachmentCount(Attachments().getActiveCount());
        prepareRules(loRules);
        return loRules;
    }

    /** Validator for entry checks while editing (uses the current status). */
    protected PurchaseOrderValidator rules() {
        return rules(Master().getTransactionStatus());
    }

    /**
     * Basic save preparation any industry can call from willSave: assigns the
     * transaction number to a new PO, drops detail rows with no item or no
     * quantity, and numbers the remaining rows.
     */
    protected void prepareDetailsForSave() throws SQLException, GuanzonException, CloneNotSupportedException {
        if (Master().getEditMode() == EditMode.ADDNEW) {
            Master().setTransactionNo(Master().getNextCode());
        }

        Iterator<Model> loIterator = Detail().iterator();
        while (loIterator.hasNext()) {
            Model loItem = loIterator.next();
            Object loQuantity = loItem.getValue("nQuantity");
            Object loStockId = loItem.getValue("sStockIDx");
            if (loQuantity == null || loStockId == null
                    || ((String) loStockId).isEmpty()
                    || ((Number) loQuantity).doubleValue() <= 0.00) {
                loIterator.remove();
            }
        }

        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
            Detail(lnCtr).setModifiedDate(poGRider.getServerDate());
        }
    }

    /**
     * Checks the current user's authority for the purchasing module and, if
     * needed, asks an approving officer.
     */
    public JSONObject seekApproval() throws SQLException, GuanzonException {
        //load authorization manager that evaluates current users authority for this process
        ActionAuthManager loAuth = new ActionAuthManager(poGRider, "cas-purchasing");
        JSONObject loJSON = loAuth.isAuthorized();
        if ("true".equalsIgnoreCase((String) loJSON.get("result"))) {
            return successJSON();
        }

        //get authorization from an approving officer
        prompt().warning((String) loJSON.get("warning"), "Authorization Required");
        loJSON = prompt().getUserApproval(poGRider);
        if (!isSuccess(loJSON)) {
            return loJSON;
        }

        //the approving officer must be authorized too
        String lsUserId = loJSON.get("sUserIDxx").toString();
        int lnUserLevel = Integer.parseInt(loJSON.get("nUserLevl").toString());
        loJSON = loAuth.isAuthorized(lsUserId, lnUserLevel);
        if (!"true".equalsIgnoreCase((String) loJSON.get("result"))) {
            prompt().warning((String) loJSON.get("warning"), "Authorization Required");
            return errorJSON("User is not an authorized approving officer..");
        }
        return successJSON();
    }

    /**
     * Authorization gate for a status change.
     *
     * Returns:
     *  - "success": caller may proceed. foCheck[0] holds the MatrixAuthChecker
     *    to postAuth() after the status change (null when no matrix applies).
     *  - "matrix": an authorization request was filed and the transaction was
     *    tagged with the pending status letter (64 + status code).
     *  - "error": validation or authorization failed.
     */
    protected JSONObject authorizeStatus(String fsStatus, String fsPendingRemarks, String fsEvent, MatrixAuthChecker[] foCheck)
            throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        foCheck[0] = null;

        if (pbWthParent) {
            return successJSON();
        }

        //validator
        JSONObject loJSON = isEntryOkay(fsStatus);
        if (!isSuccess(loJSON)) {
            return loJSON;
        }

        //get the matrix return from isEntryOkay
        JSONArray loMatrix = (JSONArray) loJSON.get("matrix");

        //there are no authorization event request
        if (loMatrix == null) {
            return seekApproval();
        }

        MatrixAuthChecker loCheck = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
        loJSON = loCheck.loadAuth();
        if (!isSuccess(loJSON)) {
            return loJSON;
        }
        foCheck[0] = loCheck;

        //already approved by all authorizing personnel
        if (loCheck.isAuthOkay()) {
            return successJSON();
        }

        //authorization request allows system approval
        if (loCheck.isAllowSys()) {
            String lsAuthType = (String) ((JSONObject) loMatrix.get(0)).get("sAuthType");

            //check if current user is authorized to approve this transaction
            loJSON = loCheck.authTrans(lsAuthType, poGRider.getUserID());
            if (!"success".equalsIgnoreCase((String) loJSON.get("result"))) {
                //request approval from an approving officer
                loJSON = prompt().getUserApproval(poGRider);
                if (!isSuccess(loJSON)) {
                    return loJSON;
                }

                //authorize the APPROVING OFFICER, not the current user
                String lsApproverID = (String) loJSON.get("sUserIDxx");
                loJSON = loCheck.authTrans(lsAuthType, lsApproverID);
                if (!"success".equalsIgnoreCase((String) loJSON.get("result"))) {
                    return loJSON;
                }
            }
        }

        if (loCheck.isAuthOkay()) {
            return successJSON();
        }

        //still pending: tag the transaction with the pending status letter
        final String lsPending = POStatus.of(fsStatus).getPendingCode();
        loJSON = inTransaction(fsEvent, true, () -> statusChange(poMaster.getTable(),
                (String) poMaster.getValue("sTransNox"), fsPendingRemarks, lsPending, false, true));

        if (isSuccess(loJSON)) {
            loJSON.put("result", "matrix");
        }
        return loJSON;
    }

    /** Unit of work executed inside a DB transaction. */
    @FunctionalInterface
    protected interface TxWork {

        JSONObject run() throws Exception;
    }

    /**
     * Runs work inside begin/commit. Any non-success result or exception rolls
     * back. When fbOwnTrans is false (a parent owns the transaction) nothing is
     * begun, committed, or rolled back here.
     */
    protected JSONObject inTransaction(String fsEvent, boolean fbOwnTrans, TxWork foWork) {
        boolean lbStarted = false;
        try {
            if (fbOwnTrans) {
                poGRider.beginTrans("UPDATE STATUS", fsEvent, SOURCE_CODE, Master().getTransactionNo());
                lbStarted = true;
            }

            JSONObject loJSON = foWork.run();
            if (!isSuccess(loJSON)) {
                if (lbStarted) {
                    safeRollback();
                }
                return loJSON != null ? loJSON : errorJSON("Unknown error during " + fsEvent + ".");
            }

            if (lbStarted) {
                poGRider.commitTrans();
            }
            return loJSON;
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            if (lbStarted) {
                safeRollback();
            }
            return errorJSON(MiscUtil.getException(ex));
        }
    }

    protected void safeRollback() {
        try {
            poGRider.rollbackTrans();
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Rollback failed", ex);
        }
    }

    /** Sets the inherited poJSON (for code that still reads it) and returns the same result. */
    protected JSONObject result(JSONObject foResult) {
        poJSON = foResult;
        return foResult;
    }

    protected static JSONObject successJSON() {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "success");
        return loJSON;
    }

    protected static JSONObject successJSON(String fsMessage) {
        JSONObject loJSON = successJSON();
        loJSON.put("message", fsMessage);
        return loJSON;
    }

    protected static JSONObject errorJSON(String fsMessage) {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "error");
        loJSON.put("message", fsMessage);
        return loJSON;
    }

    /** Number as BigDecimal for money math; 0 when null. */
    protected static BigDecimal money(Number fnValue) {
        if (fnValue == null) {
            return BigDecimal.ZERO;
        }
        if (fnValue instanceof BigDecimal) {
            return (BigDecimal) fnValue;
        }
        return BigDecimal.valueOf(fnValue.doubleValue());
    }

    protected static boolean isSuccess(JSONObject foJSON) {
        return foJSON != null && "success".equals((String) foJSON.get("result"));
    }
}
