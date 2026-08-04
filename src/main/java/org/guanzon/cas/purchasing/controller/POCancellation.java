package org.guanzon.cas.purchasing.controller;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.ActionAuthManager;
import org.guanzon.appdriver.agent.MatrixAuthChecker;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.inv.InvTransCons;
import org.guanzon.cas.inv.InventoryTransaction;
import org.guanzon.cas.purchasing.controller.PurchaseOrder;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderControllers;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.json.simple.JSONObject;
import org.guanzon.cas.purchasing.status.POCancellationRecords;
import org.guanzon.cas.purchasing.status.POCancellationStatus;
import org.guanzon.cas.purchasing.model.Model_PO_Cancellation_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Cancellation_Master;
import org.guanzon.cas.purchasing.services.POModels;
import org.guanzon.cas.purchasing.validator.POCancellationValidatorFactory;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;

public class POCancellation extends Transaction {

    private String psIndustryCode = "";
    private String psCompanyID = "";
    private String psCategorCD = "";
    private String psApprovalUser = "";
    private List<Model> paMaster;
    private List<Model> paPurchaseOrder;

    public void setIndustryID(String industryId) {
        psIndustryCode = industryId;
    }

    public void setCompanyID(String companyId) {
        psCompanyID = companyId;
    }

    public void setCategoryID(String categoryId) {
        psCategorCD = categoryId;
    }

    public Model_PO_Cancellation_Master getMaster() {
        return (Model_PO_Cancellation_Master) poMaster;
    }

    @SuppressWarnings("unchecked")
    public List<Model_PO_Cancellation_Master> getMasterList() {
        return (List<Model_PO_Cancellation_Master>) (List<?>) paMaster;
    }

    public Model_PO_Cancellation_Master getMaster(int masterRow) {
        return (Model_PO_Cancellation_Master) paMaster.get(masterRow);

    }

    @SuppressWarnings("unchecked")
    public List<Model_PO_Cancellation_Detail> getDetailList() {
        return (List<Model_PO_Cancellation_Detail>) (List<?>) paDetail;
    }

    public Model_PO_Cancellation_Detail getDetail(int entryNo) {
        if (getMaster().getTransactionNo().isEmpty() || entryNo <= 0) {
            return null;
        }

//        //autoadd detail if empty
//        Model_PO_Cancellation_Detail lastDetail = (Model_PO_Cancellation_Detail) paDetail.get(paDetail.size() - 1);
//        String stockID = lastDetail.getStockId();
//        if (stockID != null && !stockID.trim().isEmpty()) {
//            Model_PO_Cancellation_Detail newDetail = new POModels(poGRider).POCancellationDetail();
//            newDetail.newRecord();
//            newDetail.setTransactionNo(getMaster().getTransactionNo());
//            newDetail.setEntryNo(paDetail.size() + 1);
//            paDetail.add(newDetail);
//        }
        Model_PO_Cancellation_Detail loDetail;

        //find the detail record
        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr++) {
            loDetail = (Model_PO_Cancellation_Detail) paDetail.get(lnCtr);

            if (loDetail.getEntryNo() == entryNo) {
                return loDetail;
            }
        }

        loDetail = new POModels(poGRider).POCancellationDetail();
        loDetail.newRecord();
        loDetail.setTransactionNo(getMaster().getTransactionNo());
        loDetail.setEntryNo(entryNo);
        paDetail.add(loDetail);

        return loDetail;
    }

    @SuppressWarnings("unchecked")
    public List<Model_PO_Master> getPurchaseOrderList() {
        return (List<Model_PO_Master>) (List<?>) paPurchaseOrder;
    }

    /**
     * Seek Approval method
     *
     * @return JSON
     * @throws SQLException
     * @throws GuanzonException
     */
    public JSONObject seekApproval()
            throws SQLException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        //Moved only the script for seeking of approval - Arsiela 10-15-2025 - 14:11:01

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
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject initTransaction() throws GuanzonException, SQLException {
        SOURCE_CODE = InvTransCons.PURCHASE_ORDER_CANCELLATION;

        poMaster = new POModels(poGRider).POCancellationMaster();
        poDetail = new POModels(poGRider).POCancellationDetail();
        paMaster = new ArrayList<Model>();
        paDetail = new ArrayList<Model>();
        paPurchaseOrder = new ArrayList<Model>();
        initSQL();

        return super.initialize();
    }

    @Override
    public void initSQL() {
        SQL_BROWSE = "SELECT"
                + " a.sTransNox"
                + ", a.dTransact"
                + ", a.dTransact"
                + ", b.sCompnyNm"
                + ", c.sReferNox"
                + " FROM PO_Cancellation_Master a "
                + "     LEFT JOIN Client_Master b ON a.sSupplier = b.sClientID"
                + "     LEFT JOIN PO_Master c ON a.sSourceNo = c.sTransNox";
    }

    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        if (!pbWthParent) {
            //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
            poJSON = seekApproval();
            if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        return openTransaction(transactionNo);
    }

    public JSONObject NewTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        if (!pbWthParent) {
            //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
            poJSON = seekApproval();
            if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        poJSON = new JSONObject();
        poJSON = newTransaction();
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        getMaster().setIndustryId(psIndustryCode);
        getMaster().setCompanyID(psCompanyID);
        getMaster().setCategory(psCategorCD);
        getMaster().setBranchCd(poGRider.getBranchCode());
        return poJSON;
    }

    public JSONObject SaveTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {

        poJSON = super.saveTransaction();

        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        openTransaction(getMaster().getTransactionNo());
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction saved Successfully.");
        return poJSON;
    }

    public JSONObject UpdateTransaction() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        if (POCancellationStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already confirmed.");
            return poJSON;
        }

        if (POCancellationStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already cancelled.");
            return poJSON;
        }

        if (!pbWthParent) {
            //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
            poJSON = seekApproval();
            if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        return updateTransaction();
    }

    @Override
    protected JSONObject willSave() throws SQLException, GuanzonException {
        poJSON = new JSONObject();

        poJSON = isEntryOkay(POCancellationStatus.OPEN);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        int lnDetailCount = 0;
        double lnTotalAmount = 0;

        // Loop backward to safely remove
        for (int lnCtr = paDetail.size() - 1; lnCtr >= 0; lnCtr--) {
            Model_PO_Cancellation_Detail loDetail = (Model_PO_Cancellation_Detail) paDetail.get(lnCtr);

            if (loDetail == null
                    || loDetail.getStockId() == null || loDetail.getStockId().isEmpty()
                    || loDetail.getQuantity() <= 0) {
                paDetail.remove(lnCtr);
                continue;
            }

        }

        // Assign values after cleaning
        for (int lnCtr = 0; lnCtr < paDetail.size(); lnCtr++) {
            Model_PO_Cancellation_Detail loDetail = (Model_PO_Cancellation_Detail) paDetail.get(lnCtr);
            loDetail.setTransactionNo(getMaster().getTransactionNo());
            loDetail.setEntryNo(lnCtr + 1);
            lnTotalAmount += loDetail.getUnitPrice() * loDetail.getQuantity();
        }

        getMaster().setEntryNo(lnDetailCount);
        getMaster().setTransactionTotal(lnTotalAmount);
        if (getEditMode() == EditMode.ADDNEW) {
            getMaster().setEntryId(poGRider.Encrypt(poGRider.getUserID()));
            getMaster().setEntryDate(poGRider.getServerDate());
        }

        pdModified = poGRider.getServerDate();

        poJSON.put("result", "success");
        return poJSON;

    }

    @Override
    protected JSONObject isEntryOkay(String status) {
        psApprovalUser = "";

        poJSON = new JSONObject();
        GValidator loValidator = POCancellationValidatorFactory.make(getMaster().getIndustryId());

        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(poMaster);
        ArrayList laDetailList = new ArrayList<>(getDetailList());
        loValidator.setDetail(laDetailList);

        poJSON = loValidator.validate();
        if (poJSON.containsKey("isRequiredApproval") && Boolean.TRUE.equals(poJSON.get("isRequiredApproval"))) {
            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
                poJSON = ShowDialogFX.getUserApproval(poGRider);
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                } else {
                    if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "User is not an authorized approving officer.");
                        return poJSON;
                    }
                    psApprovalUser = poJSON.get("sUserIDxx") != null
                            ? poJSON.get("sUserIDxx").toString()
                            : poGRider.getUserID();
                }
            } else {
                psApprovalUser = poGRider.getUserID();
            }
        }
        return poJSON;
    }

    public JSONObject CloseTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        String lsStatus = POCancellationStatus.CONFIRMED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        MatrixAuthChecker check = null;

        if (!pbWthParent) {
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            //Check if there is a authorization request
            if (loMatrix != null) {
                //initialized MatrixAuthChecker object
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, getMaster().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if (!check.isAuthOkay()) {
                    //check if authorization request allows system approval
                    if (!check.isAllowSys()) {
                        //extract the JSONObject from JSONArray
                        JSONObject loJson = (JSONObject) loMatrix.get(0);

                        //check if current user is authorized to approved this transaction
                        poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());

                        //If not authorized/request system approval
                        if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if ("error".equals((String) poJSON.get("result"))) {
                                return poJSON;
                            }

                            //check if approving officer is authorized
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                            //user is not authorized
                            if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
                                return poJSON;
                            }
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if (!check.isAuthOkay()) {
                        //check  the user level again then if he/she allow to approve
                        poGRider.beginTrans("UPDATE STATUS", "Close Transaction", SOURCE_CODE, getMaster().getTransactionNo());

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char) (64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "Close Transaction", lsStatus, false, true);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();

                        poJSON.put("result", "matrix");
                        return poJSON;
                    }
                }
            } //there are no authorization event request
            else {
                //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
                poJSON = seekApproval();
                if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                    return poJSON;
                }
            }
        }

        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, getMaster().getTransactionNo());
        try {
            paPOMaster = new ArrayList<>();
            for (int lnCtr = 0; lnCtr < paDetail.size(); lnCtr++) {
                Model_PO_Cancellation_Detail loDetail = (Model_PO_Cancellation_Detail) paDetail.get(lnCtr);

                if (loDetail.getOrderNo() != null) {
                    if (!loDetail.getOrderNo().isEmpty()) {
                        if (loDetail.getStockId() != null) {
                            if (!loDetail.getStockId().isEmpty()) {
                                //Arsiela 08-04-2026
                                if(paPOMaster.size() <= 0 || paPOMaster.isEmpty()){
                                    paPOMaster.add(loDetail.getOrderNo());
                                } else {
                                    if(!paPOMaster.contains(loDetail.getOrderNo())){
                                        paPOMaster.add(loDetail.getOrderNo());
                                    }
                                }
                                
                                poJSON = new JSONObject();
                                poJSON = TagPODetail(lnCtr);

                                if (!"success".equals((String) poJSON.get("result"))) {
                                    poGRider.rollbackTrans();
                                    return poJSON;
                                }
                            }
                        }
                    }
                }
            }
            
            //Arsiela 08-04-2026
            poJSON = updatePurchaseOrder(POCancellationStatus.CONFIRMED);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            //kalyptus - 2025.10.10 09:31am
            //Update the inventory for this Cancelled Purchase
            InventoryTransaction loTrans = new InventoryTransaction(poGRider);
            loTrans.PurchaseOrder_Cancellation((String) poMaster.getValue("sTransNox"), (Date) poMaster.getValue("dTransact"), false);

            for (Model loDetail : paDetail) {
                Model_PO_Cancellation_Detail detail = (Model_PO_Cancellation_Detail) loDetail;
                loTrans.addDetail((String) poMaster.getValue("sIndstCdx"), detail.getStockId(), "0", detail.getQuantity(), 0, detail.getUnitPrice());
            }
            loTrans.saveTransaction();

            poJSON = statusChange(poMaster.getTable(),
                    (String) poMaster.getValue("sTransNox"),
                    "ConfirmTransaction",
                    POCancellationStatus.CONFIRMED,
                    false, true);
            if ("error".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            if (check != null) {
                check.postAuth();
            }
        } catch (SQLException | GuanzonException | CloneNotSupportedException | ParseException ex) {
            Logger.getLogger(PurchaseOrder.class.getName()).log(Level.SEVERE, null, ex);

            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            poGRider.rollbackTrans();
            return poJSON;

        }
        poGRider.commitTrans();

        openTransaction(getMaster().getTransactionNo());
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction confirmed successfully.");

        return poJSON;
    }
    
    private List<String> paPOMaster = new ArrayList<>(); //Arsiela 08-04-2026
    private JSONObject updatePurchaseOrder(String fsStatus) throws CloneNotSupportedException, SQLException, GuanzonException, ParseException{
        //Check PO Cancellation Detail: Added Purchase Order cancel when all PO Detail was cancelled; Arsiela 08-04-2026
        boolean lbIsAllCancelled = true;
        for(int lnCtr = 0; lnCtr < paPOMaster.size(); lnCtr++){
            if(paPOMaster.get(lnCtr) != null && !"".equals(paPOMaster.get(lnCtr))){
                PurchaseOrder loObject = new PurchaseOrderControllers(poGRider, logwrapr).PurchaseOrder();
                loObject.InitTransaction();
                poJSON = loObject.OpenTransaction(paPOMaster.get(lnCtr));
                if (!"success".equals((String) poJSON.get("result"))) {
                    poGRider.rollbackTrans();
                    return poJSON;
                }
                
                if(POCancellationStatus.CONFIRMED.equals(fsStatus)){
                    for(int lnRow = 0; lnRow < loObject.getDetailCount();lnRow++ ){ //Check Purchase Order Detail
                        boolean lbBreak = false;
                        boolean lbExist = false;
                        for (int lnCnt = 0; lnCnt < paDetail.size(); lnCnt++) { //Check PO Cancellation Detail
                            Model_PO_Cancellation_Detail loDetail = (Model_PO_Cancellation_Detail) paDetail.get(lnCnt);
                            if (loDetail.getOrderNo() != null) {
                                if (!loDetail.getOrderNo().isEmpty() && loDetail.getOrderNo().equals(paPOMaster.get(lnCtr))) {
                                    if (loDetail.getStockId() != null) {
                                        if (!loDetail.getStockId().isEmpty() && loDetail.getStockId().equals(loObject.Detail(lnRow).getStockID())) {
                                            lbExist = true;
                                            double ldblCancelQty = loObject.Detail(lnRow).getCancelledQuantity().doubleValue(); //loDetail.getQuantity() + 
                                            lbIsAllCancelled = (ldblCancelQty == loObject.Detail(lnRow).getQuantity().doubleValue());
                                            if(!lbIsAllCancelled){
                                                lbBreak = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if(!lbBreak){
                            break;
                        }

                        if(!lbExist){
                            lbIsAllCancelled = (loObject.Detail(lnRow).getCancelledQuantity().doubleValue() == loObject.Detail(lnRow).getQuantity().doubleValue());
                            if(!lbIsAllCancelled){
                                break;
                            }
                        }
                    }

                    if(lbIsAllCancelled){
                        poJSON = loObject.POCancelTransaction();
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }
                    }
                } else if(POCancellationStatus.CANCELLED.equals(fsStatus)){
                    poJSON = loObject.RevertStatus();
                    if (!"success".equals((String) poJSON.get("result"))) {
                        poGRider.rollbackTrans();
                        return poJSON;
                    }
                }
            }
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }
    
    public JSONObject TagPODetail(int EntryNo) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        Model_PO_Cancellation_Detail loDetail = (Model_PO_Cancellation_Detail) paDetail.get(EntryNo);
        Model_PO_Detail laPurchaseOrder = loDetail.PurchaseOrderDetail();
        if (laPurchaseOrder.getEditMode() == EditMode.READY) {
            laPurchaseOrder.updateRecord();
            laPurchaseOrder.setCancelledQuantity(loDetail.getQuantity());
            laPurchaseOrder.setModifiedDate(poGRider.getServerDate());
            poJSON = laPurchaseOrder.saveRecord();

            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject UnTagPODetail(int EntryNo) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        Model_PO_Cancellation_Detail loDetail = (Model_PO_Cancellation_Detail) paDetail.get(EntryNo);
        Model_PO_Detail laPurchaseOrder = loDetail.PurchaseOrderDetail();
        if (laPurchaseOrder.getEditMode() == EditMode.READY) {
            laPurchaseOrder.updateRecord();
            laPurchaseOrder.setCancelledQuantity(-loDetail.getQuantity());
            laPurchaseOrder.setModifiedDate(poGRider.getServerDate());
            poJSON = laPurchaseOrder.saveRecord();

            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject PostTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        String lsStatus = POCancellationStatus.POSTED;

        if (getEditMode() != EditMode.UPDATE
                && getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid Edit Mode.");
            return poJSON;
        }

        MatrixAuthChecker check = null;

        if (!pbWthParent) {
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            //Check if there is a authorization request
            if (loMatrix != null) {
                //initialized MatrixAuthChecker object
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, getMaster().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if (!check.isAuthOkay()) {
                    //check if authorization request allows system approval
                    if (!check.isAllowSys()) {
                        //extract the JSONObject from JSONArray
                        JSONObject loJson = (JSONObject) loMatrix.get(0);

                        //check if current user is authorized to approved this transaction
                        poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());

                        //If not authorized/request system approval
                        if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if ("error".equals((String) poJSON.get("result"))) {
                                return poJSON;
                            }

                            //check if approving officer is authorized
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                            //user is not authorized
                            if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
                                return poJSON;
                            }
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if (!check.isAuthOkay()) {
                        //check  the user level again then if he/she allow to approve
                        poGRider.beginTrans("UPDATE STATUS", "Post Transaction", SOURCE_CODE, getMaster().getTransactionNo());

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char) (64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "Post Transaction", lsStatus, false, true);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();

                        poJSON.put("result", "matrix");
                        return poJSON;
                    }
                }
            } //there are no authorization event request
            else {
                //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
                poJSON = seekApproval();
                if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                    return poJSON;
                }
            }
        }

        poMaster.setValue("sReceived", poGRider.Encrypt(poGRider.getUserID()));
        if (!psApprovalUser.isEmpty()) {
            poMaster.setValue("sApproved", poGRider.Encrypt(psApprovalUser));
        }

        poGRider.beginTrans("UPDATE STATUS", "PostTransaction", SOURCE_CODE, getMaster().getTransactionNo());

        poJSON = SaveTransaction();
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = statusChange(poMaster.getTable(),
                (String) poMaster.getValue("sTransNox"),
                "PostTransaction",
                POCancellationStatus.POSTED,
                false, true);
        if ("error".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        if (check != null) {
            check.postAuth();
        }

        poGRider.commitTrans();

        openTransaction(getMaster().getTransactionNo());
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction posted successfully.");

        return poJSON;
    }

    public JSONObject CancelTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = POCancellationStatus.CANCELLED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid Edit Mode");
            return poJSON;
        }

        MatrixAuthChecker check = null;

        if (!pbWthParent) {
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            //Check if there is a authorization request
            if (loMatrix != null) {
                //initialized MatrixAuthChecker object
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, getMaster().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if (!check.isAuthOkay()) {
                    //check if authorization request allows system approval
                    if (!check.isAllowSys()) {
                        //extract the JSONObject from JSONArray
                        JSONObject loJson = (JSONObject) loMatrix.get(0);

                        //check if current user is authorized to approved this transaction
                        poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());

                        //If not authorized/request system approval
                        if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if ("error".equals((String) poJSON.get("result"))) {
                                return poJSON;
                            }

                            //check if approving officer is authorized
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                            //user is not authorized
                            if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
                                return poJSON;
                            }
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if (!check.isAuthOkay()) {
                        //check  the user level again then if he/she allow to approve
                        poGRider.beginTrans("UPDATE STATUS", "Cancel Transaction", SOURCE_CODE, getMaster().getTransactionNo());

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char) (64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "Cancel Transaction", lsStatus, false, true);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();

                        poJSON.put("result", "matrix");
                        return poJSON;
                    }
                }
            } //there are no authorization event request
            else {
                //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
                poJSON = seekApproval();
                if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                    return poJSON;
                }
            }
        }

        poGRider.beginTrans("UPDATE STATUS", "CancelTransaction", SOURCE_CODE, getMaster().getTransactionNo());
        
        //Added by Arsiela 08-04-2026
        paPOMaster = new ArrayList<>();
        for (int lnCtr = 0; lnCtr < paDetail.size(); lnCtr++) {
            Model_PO_Cancellation_Detail loDetail = (Model_PO_Cancellation_Detail) paDetail.get(lnCtr);

            if (loDetail.getOrderNo() != null) {
                if (!loDetail.getOrderNo().isEmpty()) {
                    if (loDetail.getStockId() != null) {
                        if (!loDetail.getStockId().isEmpty()) {
                            if(paPOMaster.size() <= 0 || paPOMaster.isEmpty()){
                                paPOMaster.add(loDetail.getOrderNo());
                            } else {
                                if(!paPOMaster.contains(loDetail.getOrderNo())){
                                    paPOMaster.add(loDetail.getOrderNo());
                                }
                            }
                        }
                    }
                }
            }
        }

        try {
            //Arsiela 08-04-2026
            poJSON = updatePurchaseOrder(POCancellationStatus.CANCELLED);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
        } catch (ParseException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            poGRider.rollbackTrans();
            return poJSON;
        }

        //kalyptus - 2025.10.10 09:31am
        //Update the inventory for this Cancelled Purchase
        InventoryTransaction loTrans = new InventoryTransaction(poGRider);
        loTrans.PurchaseOrder_Cancellation((String) poMaster.getValue("sTransNox"), (Date) poMaster.getValue("dTransact"), true);

        for (Model loDetail : paDetail) {
            Model_PO_Cancellation_Detail detail = (Model_PO_Cancellation_Detail) loDetail;
            loTrans.addDetail((String) poMaster.getValue("sIndstCdx"), detail.getStockId(), (String) poMaster.getValue("sTransNox"), detail.getQuantity(), 0, detail.getUnitPrice());
        }
        loTrans.saveTransaction();

        poJSON = statusChange(poMaster.getTable(),
                (String) poMaster.getValue("sTransNox"),
                "CancelTransaction",
                POCancellationStatus.CANCELLED,
                false, true);
        if ("error".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        if (check != null) {
            check.postAuth();
        }

        poGRider.commitTrans();

        openTransaction(getMaster().getTransactionNo());
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction cancelled successfully.");

        return poJSON;
    }

    public JSONObject VoidTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        String lsStatus = POCancellationStatus.VOID;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid Edit Mode.");
            return poJSON;
        }

        MatrixAuthChecker check = null;

        if (!pbWthParent) {
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            //Check if there is a authorization request
            if (loMatrix != null) {
                //initialized MatrixAuthChecker object
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, getMaster().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if (!check.isAuthOkay()) {
                    //check if authorization request allows system approval
                    if (!check.isAllowSys()) {
                        //extract the JSONObject from JSONArray
                        JSONObject loJson = (JSONObject) loMatrix.get(0);

                        //check if current user is authorized to approved this transaction
                        poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());

                        //If not authorized/request system approval
                        if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if ("error".equals((String) poJSON.get("result"))) {
                                return poJSON;
                            }

                            //check if approving officer is authorized
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                            //user is not authorized
                            if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
                                return poJSON;
                            }
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if (!check.isAuthOkay()) {
                        //check  the user level again then if he/she allow to approve
                        poGRider.beginTrans("UPDATE STATUS", "Void Transaction", SOURCE_CODE, getMaster().getTransactionNo());

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char) (64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "Void Transaction", lsStatus, false, true);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();

                        poJSON.put("result", "matrix");
                        return poJSON;
                    }
                }
            } //there are no authorization event request
            else {
                //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
                poJSON = seekApproval();
                if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                    return poJSON;
                }
            }
        }

//        //validator
//        poJSON = isEntryOkay(POCancellationStatus.VOID);
//        if ("error".equals((String) poJSON.get("result"))) {
//            return poJSON;
//        }
        poGRider.beginTrans("UPDATE STATUS", "VoidTransaction", SOURCE_CODE, getMaster().getTransactionNo());

        for (int lnCtr = 0; lnCtr < paDetail.size(); lnCtr++) {
            Model_PO_Cancellation_Detail loDetail = (Model_PO_Cancellation_Detail) paDetail.get(lnCtr);

            if (loDetail.getOrderNo() != null) {
                if (!loDetail.getOrderNo().isEmpty()) {
                    if (loDetail.getStockId() != null) {
                        if (!loDetail.getStockId().isEmpty()) {
                            poJSON = new JSONObject();
                            poJSON = UnTagPODetail(lnCtr);

                            if (!"success".equals((String) poJSON.get("result"))) {
                                poGRider.rollbackTrans();
                                return poJSON;
                            }
                        }
                    }
                }
            }
        }

        poJSON = statusChange(poMaster.getTable(),
                (String) poMaster.getValue("sTransNox"),
                "VoidTransaction",
                POCancellationStatus.VOID,
                false, true);
        if ("error".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        if (check != null) {
            check.postAuth();
        }

        poGRider.commitTrans();

        openTransaction(getMaster().getTransactionNo());
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction voided successfully.");

        return poJSON;
    }

    public JSONObject searchTransaction(String value, boolean byCode, boolean byExact) {
        try {
            String lsSQL = SQL_BROWSE;

            lsSQL = MiscUtil.addCondition(lsSQL, "LEFT(a.sTransNox,4) = " + SQLUtil.toSQL(poGRider.getBranchCode()));

            String lsCondition = "";
            if (psTranStat != null) {
                if (this.psTranStat.length() > 1) {
                    for (int lnCtr = 0; lnCtr <= this.psTranStat.length() - 1; lnCtr++) {
                        lsCondition = lsCondition + ", " + SQLUtil.toSQL(Character.toString(this.psTranStat.charAt(lnCtr)));
                    }
                    lsCondition = "a.cTranStat IN (" + lsCondition.substring(2) + ")";
                } else {
                    lsCondition = "a.cTranStat = " + SQLUtil.toSQL(this.psTranStat);
                }
                lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
            }

            if (!psIndustryCode.isEmpty()) {
                lsSQL = MiscUtil.addCondition(lsSQL, "a.sIndstCdx = " + SQLUtil.toSQL(psIndustryCode));
            }

            if (!psCategorCD.isEmpty()) {
                lsSQL = MiscUtil.addCondition(lsSQL, "a.sCategrCd = " + SQLUtil.toSQL(psCategorCD));
            }
            System.out.println("Search Query is = " + lsSQL);
            poJSON = ShowDialogFX.Search(poGRider,
                    lsSQL,
                    value,
                    "Transaction No»Reference No»Supplier»Date",
                    "sTransNox»sReferNox»sCompnyNm»dTransact",
                    "a.sTransNox»c.sReferNox»b.sCompnyNm»a.dTransact",
                    byExact ? (byCode ? 0 : 1) : 2);

            if (poJSON != null) {
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                return openTransaction((String) poJSON.get("sTransNox"));

            } else {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "No record loaded.");
                return poJSON;

            }
        } catch (CloneNotSupportedException | SQLException | GuanzonException ex) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded. " + ex.getMessage());
            return poJSON;
        }
    }

    public JSONObject searchDetailByPO(int row, String value, boolean byCode) throws SQLException, GuanzonException, CloneNotSupportedException {
        Model_PO_Detail loBrowse = new PurchaseOrderModels(poGRider).PurchaseOrderDetails();
        loBrowse.initialize();
        String lsSQL = POCancellationRecords.PurchaseOrder();
        if (getMaster().getSourceNo() == null || getMaster().getSourceNo().isEmpty()) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No Reference loaded.");
            return poJSON;
        }

        if (!psIndustryCode.isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sIndstCdx = " + SQLUtil.toSQL(psIndustryCode));
        }

        if (!psCategorCD.isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sCategrCd = " + SQLUtil.toSQL(psCategorCD));
        }

        if (getMaster().getSourceNo() == null || getMaster().getSourceNo().isEmpty()) {
            poJSON = new JSONObject();
            poJSON = searchTransactionOrder("", true, true);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            return poJSON;
        }

        if (!getMaster().getSourceNo().isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox = " + SQLUtil.toSQL(getMaster().getSourceNo()));
        }

        lsSQL = MiscUtil.addCondition(lsSQL, " a.cTranstat IN ( " + SQLUtil.toSQL(PurchaseOrderStatus.APPROVED) + " ," + SQLUtil.toSQL(PurchaseOrderStatus.POSTED) + ")");
        lsSQL = MiscUtil.addCondition(lsSQL, "(b.nReceived + b.nCancelld) < b.nQuantity  ");
        lsSQL = MiscUtil.addCondition(lsSQL, " a.cTranstat IN ( " + SQLUtil.toSQL(PurchaseOrderStatus.APPROVED) + " ," + SQLUtil.toSQL(PurchaseOrderStatus.POSTED) + ")");

        poJSON = new JSONObject();
        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                value,
                "Stock ID»Barcode»Description»Brand»Model»Color",
                "a.sStockIDx»sBarCodex»xDescript»xBrandNme»xModelNme»xColorNme",
                "a.sStockIDx»sBarCodex»d.sDescript»e.sDescript»f.sDescript»g.sDescript",
                byCode ? 1 : 2);

        if (poJSON != null) {
            poJSON = loBrowse.openRecord((String) this.poJSON.get("sTransNox"));
            System.out.println("result " + (String) poJSON.get("result"));

            if ("success".equals((String) poJSON.get("result"))) {
                for (int lnExisting = 0; lnExisting <= paDetail.size() - 1; lnExisting++) {
                    Model_PO_Cancellation_Detail loExisting = (Model_PO_Cancellation_Detail) paDetail.get(lnExisting);
                    if (loExisting.getStockId() != null) {
                        if (loExisting.getStockId().equals(loBrowse.getStockID())) {
                            poJSON = new JSONObject();
                            poJSON.put("result", "error");
                            poJSON.put("message", "Selected Inventory is already exist!");
                            return poJSON;
                        }
                    }
                }

                this.poJSON = new JSONObject();
                this.poJSON.put("result", "success");
                getDetail(row).setOrderNo(loBrowse.getTransactionNo());
                getDetail(row).setStockId(loBrowse.getStockID());
                return poJSON;
            }

        }
        this.poJSON = new JSONObject();
        this.poJSON.put("result", "error");
        this.poJSON.put("message", "No record loaded.");
        return this.poJSON;

    }

    public JSONObject searchTransactionOrder(String value, boolean byCode, boolean byExact) throws SQLException, GuanzonException, CloneNotSupportedException {
        Model_PO_Master loBrowse = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();

        String lsSQL = POCancellationRecords.PurchaseOrder();
        if (!psIndustryCode.isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sIndstCdx = " + SQLUtil.toSQL(psIndustryCode));
        }

        if (!psCategorCD.isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sCategrCd = " + SQLUtil.toSQL(psCategorCD));
        }

        lsSQL = MiscUtil.addCondition(lsSQL, " a.cTranstat IN ( " + SQLUtil.toSQL(PurchaseOrderStatus.APPROVED) + " ," + SQLUtil.toSQL(PurchaseOrderStatus.POSTED) + ")");
        lsSQL = MiscUtil.addCondition(lsSQL, "(b.nReceived + b.nCancelld) < b.nQuantity  ");

        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                value,
                "Transaction No»Refernce No»Destination",
                "sTransNox»sReferNox»xDestinat",
                "a.sTransNox»a.sReferNox»c.sBranchNm",
                byExact ? (byCode ? 0 : 1) : 2);

        if (poJSON != null) {
            poJSON = loBrowse.openRecord((String) this.poJSON.get("sBranchCd"));
            System.out.println("result " + (String) poJSON.get("result"));

            if ("success".equals((String) poJSON.get("result"))) {
                getMaster().setSourceNo(loBrowse.getTransactionNo());
                replaceDetail(loBrowse.getTransactionNo());
                this.poJSON = new JSONObject();
                this.poJSON.put("result", "success");
                return poJSON;
            }

        }
        this.poJSON = new JSONObject();
        this.poJSON.put("result", "error");
        this.poJSON.put("message", "No record loaded.");
        return this.poJSON;

    }

    public JSONObject loadPurchaseOrderList(String fsColumn, String fsValue)
            throws SQLException, GuanzonException, CloneNotSupportedException {

//        if (getMaster().getIndustryId() == null
//                || getMaster().getIndustryId().isEmpty()) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "No industry is set.");
//            return poJSON;
//        }
        paPurchaseOrder.clear();
        initSQL();
        String lsSQL = POCancellationRecords.PurchaseOrder();

        if (!psIndustryCode.isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sIndstCdx = " + SQLUtil.toSQL(psIndustryCode));
        }
        if (!psCategorCD.isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sCategrCd = " + SQLUtil.toSQL(psCategorCD));
        }

        lsSQL = MiscUtil.addCondition(lsSQL, " a.cTranstat IN ( " + SQLUtil.toSQL(PurchaseOrderStatus.APPROVED) + " ," + SQLUtil.toSQL(PurchaseOrderStatus.POSTED) + ")");
        lsSQL = MiscUtil.addCondition(lsSQL, "(b.nReceived + b.nCancelld) < b.nQuantity  ");
        if (!fsColumn.isEmpty() && !fsValue.isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, fsColumn + " LIKE" + SQLUtil.toSQL(fsValue));
        }

        System.out.println("Load Transaction list query is " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        if (MiscUtil.RecordCount(loRS)
                <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No record found.");
            return poJSON;
        }
        Set<String> processedTrans = new HashSet<>();

        while (loRS.next()) {
            String transNo = loRS.getString("sTransNox");

            // Skip if we already processed this transaction number
            if (processedTrans.contains(transNo)) {
                continue;
            }

            Model_PO_Master loObject
                    = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();

            poJSON = loObject.openRecord(transNo);

            if ("success".equals((String) poJSON.get("result"))) {
                paPurchaseOrder.add((Model) loObject);

                // Mark this transaction as processed
                processedTrans.add(transNo);
            } else {
                return poJSON;
            }
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }

    private PurchaseOrder getPurchaseOrder(String transactionNo)
            throws GuanzonException, SQLException, CloneNotSupportedException {
        PurchaseOrder loSubClass = new PurchaseOrderControllers(poGRider, null).PurchaseOrder();
        loSubClass.InitTransaction();
        loSubClass.OpenTransaction(transactionNo);

        if ("error".equals((String) poJSON.get("result"))) {
            return null;
        }

        return loSubClass;
    }

    public JSONObject replaceDetail(String fsPOTransaction)
            throws GuanzonException, CloneNotSupportedException, SQLException {
        poJSON = new JSONObject();
        Model_PO_Cancellation_Detail loDetail;
        //check if PO already in Detail
        for (int lnDetail = 0; lnDetail <= paDetail.size() - 1; lnDetail++) {
            loDetail = (Model_PO_Cancellation_Detail) paDetail.get(lnDetail);
            if (loDetail.getOrderNo() != null) {
                if (loDetail.getOrderNo().equals(fsPOTransaction)) {
                    poJSON.put("result", "success");
                    poJSON.put("message", "Purchase Order is Already added! ");
                    return poJSON;
                }

            }

        }
        paDetail.clear();
        loDetail = new POModels(poGRider).POCancellationDetail();
        loDetail.newRecord();
        loDetail.setTransactionNo(getMaster().getTransactionNo());
        loDetail.setEntryNo(1);
        paDetail.add(loDetail);
        //clone detail to po cancellation
        PurchaseOrder loPurchase = getPurchaseOrder(fsPOTransaction);

        if (loPurchase != null) {
            for (int lnCtr = 0; lnCtr < loPurchase.getDetailCount(); lnCtr++) {
                loDetail = getDetail(lnCtr + 1);
                //clone only unserved
                if (Double.valueOf(String.valueOf(loPurchase.Detail(lnCtr).getQuantity()))
                        > Double.valueOf(String.valueOf(loPurchase.Detail(lnCtr).getCancelledQuantity())) + Double.valueOf(String.valueOf(loPurchase.Detail(lnCtr).getReceivedQuantity()))) {

                    loDetail.setOrderNo(loPurchase.Master().getTransactionNo());

                    loDetail.setStockId(loPurchase.Detail(lnCtr).getStockID());
                    loDetail.setUnitPrice(Double.valueOf(String.valueOf(loPurchase.Detail(lnCtr).getUnitPrice())));
                }
            }
            getMaster().setSourceNo(loPurchase.Master().getTransactionNo());
            getMaster().setSupplierID(loPurchase.Master().getSupplierID());
        } else {
            poJSON.put("result", "error");
            poJSON.put("message", "Unable to Retrieve Detail");
            return poJSON;
        }
        poJSON.put("result", "success");
//        poJSON.put("message", "Detail added successfully.");
        return poJSON;
    }

    public JSONObject replaceDetail(int fnPOTransaction)
            throws CloneNotSupportedException, SQLException, GuanzonException {
        Model_PO_Master loPurchase = (Model_PO_Master) paPurchaseOrder.get(fnPOTransaction);
        return replaceDetail(loPurchase.getTransactionNo());
    }

    public JSONObject retrieveDetail()
            throws GuanzonException, CloneNotSupportedException, SQLException {
        poJSON = new JSONObject();

        // clone detail from Purchase Order
        PurchaseOrder loPurchase = getPurchaseOrder(getMaster().getSourceNo());
        if (loPurchase != null) {

            for (int lnCtr = 0; lnCtr < loPurchase.getDetailCount(); lnCtr++) {
                //unserved only
                if (Double.valueOf(String.valueOf(loPurchase.Detail(lnCtr).getQuantity()))
                        > Double.valueOf(String.valueOf(loPurchase.Detail(lnCtr).getCancelledQuantity())) + Double.valueOf(String.valueOf(loPurchase.Detail(lnCtr).getReceivedQuantity()))) {
                    String lsStockId = loPurchase.Detail(lnCtr).getStockID();
                    boolean lbExists = false;

                    // 🔎 check if already exists in current transfer
                    for (int lnRowDetail = 1; lnRowDetail <= getDetailCount(); lnRowDetail++) {
                        Model_PO_Cancellation_Detail loExistDetail = getDetail(lnRowDetail);
                        if (loExistDetail.getStockId() != null && loExistDetail.getStockId().equals(lsStockId)) {
                            lbExists = true;
                            break;
                        }
                    }

                    if (!lbExists) {
                        // 🆕 add new detail
                        int lnNewRow = getDetailCount() + 1;

                        Model_PO_Cancellation_Detail loNewDetail = getDetail(lnNewRow);
                        if (loNewDetail.getStockId() != null) {
                            if (!loNewDetail.getStockId().isEmpty()) {

                                lnNewRow = lnNewRow + 1;
                            }
                        }

                        loNewDetail.setOrderNo(loPurchase.Master().getTransactionNo());
                        loNewDetail.setStockId(lsStockId);
                        loNewDetail.setUnitPrice(((Number) loPurchase.Detail(lnCtr).Inventory().getCost()).doubleValue());
                    }
                }
            }
        } else {
            poJSON.put("result", "error");
//            poJSON.put("message", "Unable to Retrieve Detail");
            return poJSON;
        }

        poJSON.put(
                "result", "success");
        // poJSON.put("message", "Detail added successfully.");
        return poJSON;
    }

    public JSONObject loadTransactionListConfirmation(String value, String column)
            throws SQLException, GuanzonException, CloneNotSupportedException {

        paMaster.clear();
        initSQL();
        String lsSQL = SQL_BROWSE;

        if (value != null && !value.isEmpty()) {
            //sTransNox/dTransact/dSchedule
            lsSQL = MiscUtil.addCondition(lsSQL, column + " LIKE " + SQLUtil.toSQL(value + "%"));
        }
        String lsCondition = "";
        if (psTranStat != null) {
            if (this.psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= this.psTranStat.length() - 1; lnCtr++) {
                    lsCondition = lsCondition + ", " + SQLUtil.toSQL(Character.toString(this.psTranStat.charAt(lnCtr)));
                }
                lsCondition = "a.cTranStat IN (" + lsCondition.substring(2) + ")";
            } else {
                lsCondition = "a.cTranStat = " + SQLUtil.toSQL(this.psTranStat);
            }
            lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        }

        if (!psIndustryCode.isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sIndstCdx = " + SQLUtil.toSQL(psIndustryCode));
        }

        if (!psCategorCD.isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sCategrCd = " + SQLUtil.toSQL(psCategorCD));
        }

        lsSQL = MiscUtil.addCondition(lsSQL, "LEFT(a.sTransNox,4) =" + SQLUtil.toSQL(poGRider.getBranchCode()));
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        System.out.println("Load Transaction list query is " + lsSQL);

        if (MiscUtil.RecordCount(loRS)
                <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No record found.");
            return poJSON;
        }

        while (loRS.next()) {
            Model_PO_Cancellation_Master loObject = new POModels(poGRider).POCancellationMaster();
            poJSON = loObject.openRecord(loRS.getString("sTransNox"));

            if ("success".equals((String) poJSON.get("result"))) {
                paMaster.add((Model) loObject);
            } else {
                return poJSON;
            }
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }

}
