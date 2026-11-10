package ph.com.guanzongroup.cas.purchasing.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.ActionAuthManager;
import org.guanzon.appdriver.agent.MatrixAuthChecker;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.inv.InvTransCons;
import org.guanzon.cas.inv.InventoryTransaction;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import ph.com.guanzongroup.cas.purchasing.status.POStatus;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrderValidator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

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
 * </ul>
 *
 * <p>
 * Every procedure is overridable. An industry controller extends this class
 * (PurchaseOrderGeneral, PurchaseOrderMC, PurchaseOrderMP, PurchaseOrderSP,
 * PurchaseOrderAppliance) and either overrides a whole procedure, or plugs
 * into it through the protected hooks:</p>
 * <ul>
 * <li>{@link #createValidator()} (required): the industry's rules.</li>
 * <li>{@link #initSQL()}, {@link #willSave()}, {@link #saveOthers()}
 * (required): browse SQL and save steps.</li>
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
public abstract class PurchaseOrderMain extends Transaction {

    private UserPrompt poPrompt;

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

        return result(newTransaction());
    }

    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        if (!pbWthParent) {
            JSONObject loJSON = seekApproval();
            if (!isSuccess(loJSON)) {
                return result(loJSON);
            }
        }

        return result(openTransaction(transactionNo));
    }

    public JSONObject UpdateTransaction() throws SQLException, GuanzonException {
        if (!pbWthParent) {
            JSONObject loJSON = seekApproval();
            if (!isSuccess(loJSON)) {
                return result(loJSON);
            }
        }

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

    /** Extra context for the validator (removed details, attachments, ...). Default: none. */
    protected void prepareRules(PurchaseOrderValidator foRules) {
    }

    // =====================================================================
    // Framework callbacks
    // =====================================================================
    /** Browse SQL used by the industry's search (sets SQL_BROWSE). */
    @Override
    public abstract void initSQL();

    /** Preparation and system assignments before saving. */
    @Override
    public abstract JSONObject willSave() throws SQLException, CloneNotSupportedException, GuanzonException;

    /** Other records saved together with the PO. */
    @Override
    public abstract JSONObject saveOthers();

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

    @Override
    public void saveComplete() {
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
    // Shared helpers
    // =====================================================================
    /** Validator loaded with the master, details and industry context for the given status. */
    protected PurchaseOrderValidator rules(String fsStatus) {
        PurchaseOrderValidator loRules = createValidator();
        loRules.setApplicationDriver(poGRider);
        loRules.setTransactionStatus(fsStatus);
        loRules.setMaster(Master());
        loRules.setDetail(new ArrayList<Object>(paDetail));
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

    protected static boolean isSuccess(JSONObject foJSON) {
        return foJSON != null && "success".equals((String) foJSON.get("result"));
    }
}
