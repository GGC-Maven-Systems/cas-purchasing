package ph.com.guanzongroup.cas.purchasing.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.ActionAuthManager;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.cas.inv.InvTransCons;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.json.simple.JSONObject;

public class PurchaseOrderx extends Transaction{
    JSONObject poJSON;

    public JSONObject InitTransaction() {
        SOURCE_CODE = InvTransCons.PURCHASE_ORDER;
        
        poMaster = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
        poDetail = new PurchaseOrderModels(poGRider).PurchaseOrderDetails();
        
        paDetail = new ArrayList<>();
        return initialize();
    }

    public JSONObject NewTransaction() throws CloneNotSupportedException, SQLException, GuanzonException {
        if (!pbWthParent) {
            poJSON = isUserAccessAllowed();
            if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        return newTransaction();
    }

    public JSONObject OpenTransaction(String transactionNo) throws SQLException, GuanzonException, CloneNotSupportedException {
         if (!pbWthParent) {            
            poJSON = isUserAccessAllowed();
            if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        return openTransaction(transactionNo);
    }

    public JSONObject UpdateTransaction() throws SQLException, GuanzonException {
        if (!pbWthParent) {
            poJSON = isUserAccessAllowed();
            if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        return updateTransaction();
    }

    public JSONObject SaveTransaction() throws CloneNotSupportedException, SQLException, GuanzonException {
        return saveTransaction();
    }

    public JSONObject ApproveTransaction(String remarks) {
        return errorJSON("This function is not supported...");
    }

    public JSONObject CancelTransaction(String remarks) {
        return errorJSON("This function is not supported...");
    }

    public JSONObject ConfirmTransaction(String remarks) {
        return errorJSON("This function is not supported...");
    }

    public JSONObject PostTransaction(String remarks) {
        return errorJSON("This function is not supported...");
    }

    public JSONObject AddDetail() throws CloneNotSupportedException {
        if (Detail(getDetailCount() - 1).getStockID().isEmpty()) {
            return errorJSON("Last row has empty item.");
        }
        return addDetail();
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
        
    private JSONObject isUserAccessAllowed() throws SQLException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        
        //load authorization manager that evaluates current users authority for this process
        ActionAuthManager loAuth = new ActionAuthManager(poGRider, "cas-purchasing");
        poJSON = loAuth.isAuthorized();

        //check if currenty user is authorized
        if (!((String) poJSON.get("result")).equalsIgnoreCase("true")) {
            //show process needs authorization
            ShowMessageFX.Warning((String) poJSON.get("warning"), "Authorization Required", null);
        
            //get authorization from authoried personnel
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //check if approving officer is authorized
            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
            int lnUserLevl = Integer.parseInt(poJSON.get("nUserLevl").toString());
            poJSON = loAuth.isAuthorized(lsUserIDxx, lnUserLevl);

            //if approving is not authorized then do not continue process
            if (!((String) poJSON.get("result")).equalsIgnoreCase("true")) {
                ShowMessageFX.Warning((String) poJSON.get("warning"), "Authorization Required", null);
                poJSON.put("result", "error");
                poJSON.put("message", "User is not an authorized approving officer..");
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }
    
    /** Unit of work executed inside a DB transaction. */
    @FunctionalInterface
    private interface TxWork {
        JSONObject run() throws Exception;
    }

    private static JSONObject successJSON() {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "success");
        return loJSON;
    }

    private static JSONObject successJSON(String fsMessage) {
        JSONObject loJSON = successJSON();
        loJSON.put("message", fsMessage);
        return loJSON;
    }

    private static JSONObject errorJSON(String fsMessage) {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "error");
        loJSON.put("message", fsMessage);
        return loJSON;
    }

    private void safeRollback() {
        try {
            poGRider.rollbackTrans();
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Rollback failed", ex);
        }
    }

    /**
     * Runs work inside begin/commit. Any non-success result or exception rolls
     * back. When fbOwnTrans is false (called with a parent that owns the
     * transaction) nothing is begun, committed, or rolled back here.
     */
    private JSONObject inTransaction(String fsEvent, boolean fbOwnTrans, TxWork foWork) {
        boolean lbStarted = false;
        try {
            if (fbOwnTrans) {
                poGRider.beginTrans("UPDATE STATUS", fsEvent, SOURCE_CODE, Master().getTransactionNo());
                lbStarted = true;
            }

            JSONObject loJSON = foWork.run();
            if (loJSON == null || !"success".equals((String) loJSON.get("result"))) {
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
}