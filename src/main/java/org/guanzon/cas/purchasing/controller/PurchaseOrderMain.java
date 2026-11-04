package org.guanzon.cas.purchasing.controller;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javax.sql.rowset.CachedRowSet;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.xml.JRXmlWriter;
import net.sf.jasperreports.export.*;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.swing.JRViewerToolbar;
import net.sf.jasperreports.view.JasperViewer;
import org.apache.commons.codec.binary.Base64;
import org.guanzon.appdriver.agent.ActionAuthManager;
import org.guanzon.appdriver.agent.MatrixAuthChecker;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.agent.systables.SysTableContollers;
import org.guanzon.appdriver.agent.systables.TransactionAttachment;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscReplUtil;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.base.WebFile;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.appdriver.token.RequestAccess;
import org.guanzon.cas.client.account.AP_Client_Master;
import org.guanzon.cas.client.model.Model_Client_Master;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.client.services.ClientModels;
import org.guanzon.cas.inv.InvTransCons;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.InventoryTransaction;
//import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.inv.warehouse.StockRequest;
import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Master;
import org.guanzon.cas.inv.warehouse.services.InvWarehouseControllers;
import org.guanzon.cas.inv.warehouse.status.StockRequestStatus;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.Brand;
import org.guanzon.cas.parameter.Category;
import org.guanzon.cas.parameter.Company;
import org.guanzon.cas.parameter.Department;
import org.guanzon.cas.parameter.Industry;
import org.guanzon.cas.parameter.Project;
import org.guanzon.cas.parameter.Term;
import org.guanzon.cas.parameter.model.Model_Project;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.parameter.services.ParamModels;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderControllers;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderStaticData;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.guanzon.cas.purchasing.utility.CustomJasperViewerReports;
import org.guanzon.cas.purchasing.validator.PurchaseOrderValidator;
import org.guanzon.cas.purchasing.validator.PurchaseOrderValidatorFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.cashflow.DisbursementVoucher;
import ph.com.guanzongroup.cas.cashflow.Payee;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import org.guanzon.cas.purchasing.services.QuotationControllers;
import org.guanzon.cas.purchasing.status.POQuotationStatus;
import org.json.simple.parser.JSONParser;
import ph.com.guanzongroup.cas.cashflow.utility.CustomCommonUtil;

/**
 * Main Purchase Order controller: every procedure shared by all industries.
 *
 * <p>Each industry extends this class (PurchaseOrderGeneral, PurchaseOrderMC,
 * PurchaseOrderMP, PurchaseOrderSP, PurchaseOrderAppliance), supplies its
 * validator through {@link #createValidator()}, and adds industry procedures
 * through the protected hooks. Business rules live in the validators.</p>
 */
public abstract class PurchaseOrderMain extends Transaction {

    List<Model_Inv_Stock_Request_Master> paStockRequest;
    List<Model_PO_Master> paPOMaster;
    List<StockRequest> poStockRequest;
    List<POQuotation> poPOQuotation;
    List<POQuotation> poPOQuotationStatus;
    List<POQuotation> poPOQuotationRemovedStatus;
    List<Model> paDetailRemoved;
    CashflowControllers poPaymentRequest;
    String PayeeID;
    String dfrom;
    String dthru;
    List<TransactionAttachment> paAttachments;

    public JSONObject InitTransaction() {
        SOURCE_CODE = InvTransCons.PURCHASE_ORDER;
        poMaster = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
        poDetail = new PurchaseOrderModels(poGRider).PurchaseOrderDetails();
        paDetail = new ArrayList<>();
        poStockRequest = new ArrayList<>();
        poPOQuotation = new ArrayList<>();
        poPOQuotationStatus = new ArrayList<>();
        poPOQuotationRemovedStatus = new ArrayList<>();

        return initialize();
    }

    // =====================================================================
    // Shared helpers (refactor 2026-11-04)
    // =====================================================================

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

    /**
     * Shared authorization gate for Confirm / Approve / Post / Cancel / Void.
     *
     * Returns:
     *  - "success": caller may proceed. foCheck[0] holds the MatrixAuthChecker
     *    to postAuth() after the status change (null when no matrix applies).
     *  - "matrix": an authorization request was filed and the transaction was
     *    tagged with the pending status letter (64 + status code).
     *  - "error": validation or authorization failed.
     */
    private JSONObject authorizeStatus(String fsStatus, String fsPendingRemarks, String fsEvent, MatrixAuthChecker[] foCheck)
            throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        foCheck[0] = null;

        if (pbWthParent) {
            return successJSON();
        }

        //validator
        JSONObject loJSON = isEntryOkay(fsStatus);
        if (!"success".equals((String) loJSON.get("result"))) {
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
        if (!"success".equals((String) loJSON.get("result"))) {
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
                loJSON = ShowDialogFX.getUserApproval(poGRider);
                if ("error".equals((String) loJSON.get("result"))) {
                    return loJSON;
                }

                //FIX: authorize the APPROVING OFFICER, not the current user
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
        final String lsPending = Character.toString((char) (64 + Integer.parseInt(fsStatus)));
        loJSON = inTransaction(fsEvent, true, () -> statusChange(poMaster.getTable(),
                (String) poMaster.getValue("sTransNox"), fsPendingRemarks, lsPending, false, true));

        if ("success".equals((String) loJSON.get("result"))) {
            loJSON.put("result", "matrix");
        }
        return loJSON;
    }

    // =====================================================================
    // Rules: every business rule lives in the validator
    // =====================================================================

    /**
     * Validator for this controller's industry. Every Purchase Order validator
     * extends {@link PurchaseOrderValidator}.
     */
    protected abstract PurchaseOrderValidator createValidator();

    /** Validator loaded with the current master, details and context for the given status. */
    protected PurchaseOrderValidator rules(String fsStatus) {
        PurchaseOrderValidator loRules = createValidator();
        loRules.setApplicationDriver(poGRider);
        loRules.setTransactionStatus(fsStatus);
        loRules.setMaster(Master());
        loRules.setDetail(new ArrayList<Object>(paDetail));
        getDetailRemovedCount(); //initializes the removed list
        loRules.setRemovedDetail(new ArrayList<Object>(paDetailRemoved));
        loRules.setActiveAttachmentCount(getActiveAttachmentCount());
        return loRules;
    }

    /** Validator for entry checks while editing (uses the current status). */
    protected PurchaseOrderValidator rules() {
        return rules(Master().getTransactionStatus());
    }

    // =====================================================================
    // Industry hooks (override in PurchaseOrderGeneral, PurchaseOrderMC, ...)
    // Defaults do nothing extra.
    // =====================================================================

    /** Extra preparation before saving (inside willSave). Default: none. */
    protected JSONObject industryWillSave() throws SQLException, GuanzonException, CloneNotSupportedException {
        return successJSON();
    }

    /** Extra records to save with the PO (inside saveOthers). Default: none. */
    protected JSONObject industrySaveOthers() throws SQLException, GuanzonException, CloneNotSupportedException {
        return successJSON();
    }

    /**
     * Extra preparation after the source documents are prepared and before the
     * status-change transaction starts (same statuses as below). A non-success
     * result stops the change. Default: none.
     */
    protected JSONObject industryPrepareStatusChange(String fsStatus) throws SQLException, GuanzonException, CloneNotSupportedException {
        return successJSON();
    }

    /**
     * Extra work inside the status-change transaction, before the new status is
     * written (fsStatus is CONFIRMED, APPROVED, POSTED, CANCELLED, VOID or
     * RETURNED). A non-success result rolls the whole change back. Default: none.
     */
    protected JSONObject industryStatusChange(String fsStatus) throws Exception {
        return successJSON();
    }

    /** Whether approval creates a Payment Request for the advance payment. */
    protected boolean isAdvancePaymentRequestRequired() {
        return Master().getWithAdvPaym()
                && (Master().getDownPaymentRatesAmount().doubleValue() > PurchaseOrderStaticData.default_value_double
                || Master().getDownPaymentRatesPercentage().doubleValue() > PurchaseOrderStaticData.default_value_double);
    }

    /** JRXML used by printTransaction for the given template type; null if unknown. */
    protected String getPrintTemplatePath(String jasperType) {
        String lsPath = System.getProperty("sys.default.path.config") + "/reports/";
        switch (jasperType) {
            case PurchaseOrderStaticData.Printing_CAR_MC_MPUnit_Appliance:
                return lsPath + "PurchaseOrderCarMcMPUnitAppliance.jrxml";
            case PurchaseOrderStaticData.Printing_CARSp_MCSp_General:
                return lsPath + "PurchaseOrderCARSpMCSpGeneral.jrxml";
            case PurchaseOrderStaticData.Printing_Pedritos:
                return lsPath + "PurchaseOrderPedritos.jrxml";
            default:
                return null;
        }
    }

    /** Item description printed on the PO for one detail row. */
    protected String buildPrintDescription(int fnRow, String jasperType) throws SQLException, GuanzonException {
        String lsDescription = safeString(Detail(fnRow).Inventory().getDescription());
        switch (jasperType) {
            case PurchaseOrderStaticData.Printing_CARSp_MCSp_General:
            case PurchaseOrderStaticData.Printing_Pedritos:
                String lsFullDesc = safeString(Detail(fnRow).Inventory().Brand().getDescription());
                lsFullDesc = joinText(lsFullDesc, safeString(Detail(fnRow).Inventory().Model().getDescription()));
                return joinText(lsFullDesc, lsDescription);
            default:
                return lsDescription;
        }
    }

    private static String joinText(String fsLeft, String fsRight) {
        if (fsLeft.isEmpty()) {
            return fsRight;
        }
        return fsRight.isEmpty() ? fsLeft : fsLeft + " " + fsRight;
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

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public JSONObject initFields() {
        /*Put initial model values here*/
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }

    private Model_PO_Master POMasterList() {
        return new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
    }
    private Model_PO_Detail PODetailList() {
        return new PurchaseOrderModels(poGRider).PurchaseOrderDetails();
    }

    public Model_PO_Master POMaster(int row) {
        return (Model_PO_Master) paPOMaster.get(row);
    }

    public int getPOMasterCount() {
        return this.paPOMaster.size();
    }

    private StockRequest StockRequest() {
        return new InvWarehouseControllers(poGRider, logwrapr).StockRequest();
    }

    private POQuotation POQuotation() {
        return new QuotationControllers(poGRider, logwrapr).POQuotation();
    }

    public Model_Inv_Stock_Request_Master InvStockRequestMaster(int row) {
        return (Model_Inv_Stock_Request_Master) paStockRequest.get(row);
    }

    public int getInvStockRequestCount() {
        return paStockRequest == null ? 0 : this.paStockRequest.size();
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

    public int getDetailRemovedCount() {
        if (paDetailRemoved == null) {
            paDetailRemoved = new ArrayList<>();
        }

        return paDetailRemoved.size();
    }

    public Model_PO_Detail DetailRemove(int row) {
        return (Model_PO_Detail) paDetailRemoved.get(row);
    }

    public JSONObject AddDetail() throws CloneNotSupportedException {
        JSONObject loJSON = rules().validateCanAddDetail();
        if (!PurchaseOrderValidator.isSuccess(loJSON)) {
            return loJSON;
        }
        return addDetail();
    }

    public String getStatusValue() {
        String lsStatus = Master().getConvertedTransactionStatus();
        switch (lsStatus) {
            case PurchaseOrderStatus.CONFIRMED:
                lsStatus = "CONFIRMED";
                break;
            case PurchaseOrderStatus.APPROVED:
                lsStatus = "APPROVED";
                break;
            case PurchaseOrderStatus.RETURNED:
                lsStatus = "RETURNED";
                break;
            case PurchaseOrderStatus.CANCELLED:
                lsStatus = "CANCELLED";
                break;
            case PurchaseOrderStatus.VOID:
                lsStatus = "VOIDED";
                break;
            case PurchaseOrderStatus.PROCESSED:
                lsStatus = "PROCESSED";
                break;
            case PurchaseOrderStatus.POSTED:
                lsStatus = "POSTED";
                break;
            case PurchaseOrderStatus.OPEN:
                lsStatus = "OPEN";
                break;
            default:
                lsStatus = "UNKNOWN";
                break;
        }

        if ("ABCDEFGHIJ".contains(Master().getTransactionStatus())) {
            lsStatus = lsStatus + "+";
        }

        return lsStatus;
    }

    public JSONObject NewTransaction() throws CloneNotSupportedException, SQLException, GuanzonException {
        if (!pbWthParent) {
            //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
            poJSON = seekApproval();
            if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        return newTransaction();
    }

    public JSONObject SaveTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        return saveTransaction();
    }

    @Override
    public JSONObject willSave() throws SQLException, CloneNotSupportedException, GuanzonException {
        /* Put system validations and other assignments here */
        poJSON = new JSONObject();

        if (Master().getEditMode() == EditMode.ADDNEW) {
            Master().setTransactionNo(Master().getNextCode());
        }

        if (Master().getTransactionStatus().equals(PurchaseOrderStatus.CONFIRMED) && pnEditMode == EditMode.UPDATE) {
            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
                poJSON = ShowDialogFX.getUserApproval(poGRider);
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                setApproving((String) poJSON.get("sUserIDxx"));
            }
        }
//
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            Object quantityObj = item.getValue("nQuantity");
            Object stockIDObj = item.getValue("sStockIDx");

            // Check if the values are not null
            if (quantityObj != null && stockIDObj != null) {
                double quantity = ((Number) quantityObj).doubleValue();
                String stockID = (String) stockIDObj;

                // Remove only items with empty stock ID or zero quantity
                if (stockID.isEmpty() || quantity <= 0.00) {
                    detail.remove();
                }
            } else {
                // Handle the case where the values are null
                detail.remove();
            }
        }
        if (PurchaseOrderStatus.RETURNED.equals(Master().getTransactionStatus())) {
            PurchaseOrderMain loRecord = new PurchaseOrderControllers(poGRider, null).PurchaseOrder(Master().getIndustryID());
            loRecord.InitTransaction();
            //FIX: open as child so OpenTransaction() does not prompt for approval mid-save
            loRecord.setWithParent(true);
            loRecord.OpenTransaction(Master().getTransactionNo());

            List<Model_PO_Detail> laSavedDetail = new ArrayList<>();
            for (int lnCtr = 0; lnCtr <= loRecord.getDetailCount() - 1; lnCtr++) {
                laSavedDetail.add(loRecord.Detail(lnCtr));
            }

            //A returned PO must have at least one change (rule in the validator)
            poJSON = rules().validateReturnedEdit(loRecord.Master(), laSavedDetail);
            if (!PurchaseOrderValidator.isSuccess(poJSON)) {
                return poJSON;
            }
            //enable this settrans if saving of return stat will change the status to the last status
//            Master().setTransactionStatus(PurchaseOrderStatus.OPEN); //If edited update trasaction status into open
        }

        //Allow the user to edit details but seek an approval from the approving officer
        if (PurchaseOrderStatus.CONFIRMED.equals(Master().getTransactionStatus())) {
            poJSON = setValueToOthers(Master().getTransactionStatus());
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        poJSON = industryWillSave();
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        
        
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr + 1);
            Detail(lnCtr).setModifiedDate(poGRider.getServerDate());
        }
        
        for (int lnCtr = 0; lnCtr <= getTransactionAttachmentCount()- 1; lnCtr++) {
            TransactionAttachmentList(lnCtr).getModel().setSourceNo(Master().getTransactionNo());
            TransactionAttachmentList(lnCtr).getModel().setSourceCode(getSourceCode());
            TransactionAttachmentList(lnCtr).getModel().setBranchCode(Master().getBranchCode());
            TransactionAttachmentList(lnCtr).getModel().setImagePath(System.getProperty("sys.default.path.temp.attachments"));
            
            String lsOriginalFileName = TransactionAttachmentList(lnCtr).getModel().getFileName();
            //Check existing file name in database
            if(EditMode.ADDNEW == TransactionAttachmentList(lnCtr).getModel().getEditMode()){
                int lnCopies = 0;
                String fsFilePath = TransactionAttachmentList(lnCtr).getModel().getImagePath() + "/" + TransactionAttachmentList(lnCtr).getModel().getFileName();
                String lsNewFileName = TransactionAttachmentList(lnCtr).getModel().getFileName();
                while ("error".equals((String)checkExistingFileName(lsNewFileName).get("result"))) {
                    lnCopies++;
                    //Rename the file
                    int dotIndex = TransactionAttachmentList(lnCtr).getModel().getFileName().lastIndexOf(".");
                    if (dotIndex == -1) {
                        lsNewFileName = TransactionAttachmentList(lnCtr).getModel().getFileName() +"_"+lnCopies;
                    } else {
                        lsNewFileName = TransactionAttachmentList(lnCtr).getModel().getFileName().substring(0, dotIndex) +"_"+ lnCopies +TransactionAttachmentList(lnCtr).getModel().getFileName().substring(dotIndex);
                    }
                }

                if(lnCopies > 0){
                    Path source = Paths.get(fsFilePath);
                    try {
                        // Copy file into the target directory with a new name
                        Path target = Paths.get(System.getProperty("sys.default.path.temp.attachments")).resolve(lsNewFileName);
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        //check if file is existing
                        int lnChecker = 0;
                        File file = new File(TransactionAttachmentList(lnCtr).getModel().getImagePath() + "/" + lsNewFileName);
                        while(!file.exists() && lnChecker < 5){
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);  
                            System.out.println("Re-Copying... " + lnChecker);
                            lnChecker++;
                        }
                        TransactionAttachmentList(lnCtr).getModel().setFileName(lsNewFileName);
                        System.out.println("File copied successfully as " + lsNewFileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
            //Upload Attachment when send status is 0
            try {
                if("0".equals(TransactionAttachmentList(lnCtr).getModel().getSendStatus())){
                    poJSON = uploadCASAttachments(poGRider, System.getProperty("sys.default.access.token"), lnCtr,lsOriginalFileName);
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            }
            
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
     * Upload Attachment
     * @param instance
     * @param access 
     * @param fnRow 
     * @return  
     * @throws java.lang.Exception 
     */
    public JSONObject uploadCASAttachments(GRiderCAS instance, String access, int fnRow, String fsOriginalFileName) throws Exception{       
        poJSON = new JSONObject();
        System.out.println("Uploading... : fsOriginalFileName : " + fsOriginalFileName);
        System.out.println("New File Name... : " + paAttachments.get(fnRow).getModel().getFileName());
        String hash;
        String lsFile = paAttachments.get(fnRow).getModel().getFileName();
        
        //check if new file is existing
        File file = new File(paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile);
        if(!file.exists()){
            //check if original file is existing
            lsFile = fsOriginalFileName;
            file = new File(paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile);
            if(!file.exists()){
                poJSON.put("result", "error");
                poJSON.put("message", "Cannot locate file in " + paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile
                                        + ".\nContact system administrator for assistance.");
                return poJSON;  
            }
        }

        //check if file hash is not empty
        hash = paAttachments.get(fnRow).getModel().getMD5Hash();
        if(paAttachments.get(fnRow).getModel().getMD5Hash() == null || "".equals(paAttachments.get(fnRow).getModel().getMD5Hash())){
            hash = MiscReplUtil.md5Hash(paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile);
        }

        JSONObject result = WebFile.UploadFile(getAccessToken(access)
                                , "0032"
                                , ""
                                , paAttachments.get(fnRow).getModel().getFileName()
                                , instance.getBranchCode()
                                , hash
                                , encodeFileToBase64Binary(file)
                                , paAttachments.get(fnRow).getModel().getSourceCode()
                                , paAttachments.get(fnRow).getModel().getSourceNo()
                                , "");

        if("error".equalsIgnoreCase((String) result.get("result"))){
            System.out.println("Upload Error : " + result.toJSONString());
            System.out.println("Upload Error : " + paAttachments.get(fnRow).getModel().getFileName());
            poJSON.put("result", "error");
            poJSON.put("message", "System error while uploading file "+ paAttachments.get(fnRow).getModel().getFileName()
                                    + ".\nContact system administrator for assistance.");
            return poJSON;
        }
        paAttachments.get(fnRow).getModel().setMD5Hash(hash);
        paAttachments.get(fnRow).getModel().setSendStatus("1");
        System.out.println("Upload Success : " + paAttachments.get(fnRow).getModel().getFileName());
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private static String encodeFileToBase64Binary(File file) throws Exception{
         FileInputStream fileInputStreamReader = new FileInputStream(file);
         byte[] bytes = new byte[(int)file.length()];
         fileInputStreamReader.read(bytes);
         return new String(Base64.encodeBase64(bytes), "UTF-8");
    } 
    
    private static JSONObject token = null;
    private static String getAccessToken(String access){
        try {
            JSONParser oParser = new JSONParser();
            if(token == null){
                token = (JSONObject)oParser.parse(new FileReader(access));
            }
            
            Calendar current_date = Calendar.getInstance();
            current_date.add(Calendar.MINUTE, -25);
            Calendar date_created = Calendar.getInstance();
            date_created.setTime(SQLUtil.toDate((String) token.get("created") , SQLUtil.FORMAT_TIMESTAMP));
            
            //Check if token is still valid within the time frame
            //Request new access token if not in the current period range
            if(current_date.after(date_created)){
                String[] xargs = new String[] {(String) token.get("parent"), access};
                RequestAccess.main(xargs);
                token = (JSONObject)oParser.parse(new FileReader(access));
            }
            
            return (String)token.get("access_key");
        } catch (IOException ex) {
            return null;
        } catch (ParseException ex) {
            return null;
        }
    }
    
    public JSONObject checkExistingFileName(String fsFileName) throws SQLException, GuanzonException{
        poJSON = new JSONObject();
        
        String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(TransactionAttachment().getModel()), 
                                                                    " sFileName = " + SQLUtil.toSQL(fsFileName)
                                                                    );
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0) {
                if(loRS.next()){
                    if(loRS.getString("sFileName") != null && !"".equals(loRS.getString("sFileName"))){
                        poJSON.put("result", "error");
                        poJSON.put("message", "File name already exist in database.\nTry changing the file name to upload.");
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
        }
        return poJSON;
    }

    @Override
    protected JSONObject save() throws CloneNotSupportedException, SQLException, GuanzonException {
        return isEntryOkay(PurchaseOrderStatus.OPEN);
    }

    @Override
    protected JSONObject isEntryOkay(String status) throws CloneNotSupportedException {
        poJSON = rules(status).validate();
        return poJSON;
    }

    @Override
    public JSONObject saveOthers() {
        JSONObject poJSON = new JSONObject();
        try {
            //Save Attachments
            for (int lnCtr = 0; lnCtr <= getTransactionAttachmentCount() - 1; lnCtr++) {
                if (paAttachments.get(lnCtr).getEditMode() == EditMode.ADDNEW || paAttachments.get(lnCtr).getEditMode() == EditMode.UPDATE) {
                    paAttachments.get(lnCtr).getModel().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
                    paAttachments.get(lnCtr).getModel().setModifiedDate(poGRider.getServerDate());
                    paAttachments.get(lnCtr).setWithParentClass(true);
                    System.out.println("CHECK ATTACHMENT RECORD STAT : " + paAttachments.get(lnCtr).getModel().getRecordStatus());
                    poJSON = paAttachments.get(lnCtr).saveRecord();
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                }
            }
            poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            poJSON = industrySaveOthers();
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

        } catch (CloneNotSupportedException | SQLException | GuanzonException ex) {
            Logger.getLogger(PurchaseOrderMain.class.getName()).log(Level.SEVERE, null, ex);
            poJSON.put("result", "error");
            poJSON.put("message", ex.getMessage());
            return poJSON;
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public void saveComplete() {
        /*This procedure was called when saving was complete*/
        System.out.println("Transaction saved successfully.");
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

    public JSONObject UpdateTransaction() throws SQLException, GuanzonException {
        if (!pbWthParent) {
            //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
            poJSON = seekApproval();
            if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                return poJSON;
            }
        }

        return updateTransaction();
    }

    // =====================================================================
    // Status changes
    // =====================================================================

    public JSONObject ConfirmTransaction(String remarks) throws ParseException, SQLException, CloneNotSupportedException, GuanzonException {
        poJSON = new JSONObject();

        if (Master().getTransactionStatus().equals(PurchaseOrderStatus.OPEN)) {
            final String lsStatus = PurchaseOrderStatus.CONFIRMED;

            if (getEditMode() != EditMode.READY) {
                return errorJSON("No transacton was loaded.");
            }

            //Attachment requirement and other confirm rules run in the validator (authorizeStatus)
            final MatrixAuthChecker[] loCheck = new MatrixAuthChecker[1];
            poJSON = authorizeStatus(lsStatus, remarks, "ConfirmTransaction", loCheck);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON; //error or "matrix"
            }

            poJSON = setValueToOthers(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //industry preparation before the transaction starts
            poJSON = industryPrepareStatusChange(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            poJSON = inTransaction("ConfirmTransaction", true, () -> {
                JSONObject loJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
                if (!"success".equals((String) loJSON.get("result"))) {
                    return loJSON;
                }

                //Update Transaction Status of PO Quotation
                loJSON = updatePOQuotationStatus(lsStatus);
                if ("error".equals((String) loJSON.get("result"))) {
                    System.out.println("PO Quotation Saving " + (String) loJSON.get("message"));
                    return loJSON;
                }

                //industry procedure, inside the same transaction
                loJSON = industryStatusChange(lsStatus);
                if (!"success".equals((String) loJSON.get("result"))) {
                    return loJSON;
                }
                
                loJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
                if (!"success".equals((String) loJSON.get("result"))) {
                    return loJSON;
                }

                if (loCheck[0] != null) {
                    loCheck[0].postAuth();
                }
                return successJSON("Transaction confirmed successfully.");
            });
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            Master().setTransactionStatus(PurchaseOrderStatus.CONFIRMED);
        }

        //mac 2026.02.27
        poJSON = ApproveTransaction("Auto-approve after confirmation.");
        return poJSON;
    }

    public JSONObject ApproveTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        final String lsStatus = PurchaseOrderStatus.APPROVED;

        if (getEditMode() != EditMode.READY) {
            return errorJSON("No transacton was loaded.");
        }

        final MatrixAuthChecker[] loCheck = new MatrixAuthChecker[1];
        poJSON = authorizeStatus(lsStatus, "Approval matrix", "Approve Transaction", loCheck);
        if ("matrix".equals((String) poJSON.get("result"))) {
            //Kept as "success" for the UI, as before; the message now says it is a pending request
            return successJSON("Transaction approving request submitted successfully.");
        }
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //industry preparation before the transaction starts
        poJSON = industryPrepareStatusChange(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //Added validation to generate PRF only if the PO Status is APPROVED. Arsiela 03-10-2026 11:11am
        poJSON = generatePRF(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = inTransaction("Approve Transaction", true, () -> {
            JSONObject loJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
            if (!"success".equals((String) loJSON.get("result"))) {
                return loJSON;
            }

            //Added validation to generate PRF only if the PO Status is APPROVED. Arsiela 03-10-2026 11:11am
            loJSON = savePRF(lsStatus);
            if (!"success".equals((String) loJSON.get("result"))) {
                return loJSON;
            }

            //kalyptus - 2025.10.08 02:36pm
            //save to inventory ledger
            InventoryTransaction loTrans = new InventoryTransaction(poGRider, psBranchCode);
            loTrans.PurchaseOrder((String) poMaster.getValue("sTransNox"), (Date) poMaster.getValue("dTransact"), false);
            for (Model loDetail : paDetail) {
                Model_PO_Detail detail = (Model_PO_Detail) loDetail;
                loTrans.addDetail((String) poMaster.getValue("sIndstCdx"), detail.getStockID(), "0", 0, detail.getQuantity().doubleValue(), detail.getUnitPrice().doubleValue());
            }
            loTrans.saveTransaction();

            //industry procedure, inside the same transaction
            loJSON = industryStatusChange(lsStatus);
            if (!"success".equals((String) loJSON.get("result"))) {
                return loJSON;
            }
            
            loJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
            if (!"success".equals((String) loJSON.get("result"))) {
                return loJSON;
            }

            if (loCheck[0] != null) {
                loCheck[0].postAuth();
            }
            return successJSON("Transaction approved successfully.");
        });
        return poJSON;
    }

    public JSONObject PostTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        final String lsStatus = PurchaseOrderStatus.POSTED;

        if (getEditMode() != EditMode.READY) {
            return errorJSON("No transacton was loaded.");
        }

        final MatrixAuthChecker[] loCheck = new MatrixAuthChecker[1];
        poJSON = authorizeStatus(lsStatus, remarks, "Post Transaction", loCheck);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON; //error or "matrix"
        }

        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //industry preparation before the transaction starts
        poJSON = industryPrepareStatusChange(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //FIX: when called with a parent, the parent owns the transaction (no begin/commit/rollback here)
        poJSON = inTransaction("Post Transaction", !pbWthParent, () -> {
            JSONObject loJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
            if (!"success".equals((String) loJSON.get("result"))) {
                return loJSON;
            }

            //industry procedure, inside the same transaction
            loJSON = industryStatusChange(lsStatus);
            if (!"success".equals((String) loJSON.get("result"))) {
                return loJSON;
            }
            
            loJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
            if (!"success".equals((String) loJSON.get("result"))) {
                return loJSON;
            }

            if (loCheck[0] != null) {
                loCheck[0].postAuth();
            }
            return successJSON("Transaction posted successfully.");
        });
        return poJSON;
    }

    public JSONObject CancelTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        final String lsStatus = PurchaseOrderStatus.CANCELLED;

        if (getEditMode() != EditMode.READY) {
            return errorJSON("No transacton was loaded.");
        }

        final MatrixAuthChecker[] loCheck = new MatrixAuthChecker[1];
        poJSON = authorizeStatus(lsStatus, remarks, "Cancel Transaction", loCheck);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON; //error or "matrix"
        }

        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //industry preparation before the transaction starts
        poJSON = industryPrepareStatusChange(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        final boolean lbWasApproved = PurchaseOrderStatus.APPROVED.equalsIgnoreCase((String) poMaster.getValue("cTranStat"));

        poJSON = inTransaction("Cancel Transaction", true, () -> {
            //kalyptus-2025.10.08 02:52pm
            //reverse the inventory ledger entry made on approval
            if (lbWasApproved) {
                //FIX: same branch and industry source as ApproveTransaction
                InventoryTransaction loTrans = new InventoryTransaction(poGRider, psBranchCode);
                loTrans.PurchaseOrder((String) poMaster.getValue("sTransNox"), (Date) poMaster.getValue("dTransact"), true);
                for (Model loDetail : paDetail) {
                    Model_PO_Detail detail = (Model_PO_Detail) loDetail;
                    loTrans.addDetail((String) poMaster.getValue("sIndstCdx"), detail.getStockID(), "0", 0, detail.getQuantity().doubleValue(), detail.getUnitPrice().doubleValue());
                }
                loTrans.saveTransaction();
            }

            JSONObject loJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
            if (!"success".equals((String) loJSON.get("result"))) {
                return loJSON;
            }

            //Update Transaction Status of PO Quotation
            loJSON = updatePOQuotationStatus(lsStatus);
            if ("error".equals((String) loJSON.get("result"))) {
                System.out.println("PO Quotation Saving " + (String) loJSON.get("message"));
                return loJSON;
            }

            //industry procedure, inside the same transaction
            loJSON = industryStatusChange(lsStatus);
            if (!"success".equals((String) loJSON.get("result"))) {
                return loJSON;
            }
            
            loJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
            if (!"success".equals((String) loJSON.get("result"))) {
                return loJSON;
            }

            if (loCheck[0] != null) {
                loCheck[0].postAuth();
            }
            return successJSON("Transaction cancelled successfully.");
        });
        return poJSON;
    }

    //Arsiela 08-04-2026
    //Called inside a parent's transaction: no rollback here, the parent handles it.
    public JSONObject POCancelTransaction() throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.CANCELLED;
        
        try {
            poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "CancelTransaction", lsStatus, false, true);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            
            poJSON = successJSON("Transaction cancelled successfully.");
        } catch (GuanzonException | SQLException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON = errorJSON(MiscUtil.getException(ex));
        }
        return poJSON;
    }
    
    //Called inside a parent's transaction: no rollback here, the parent handles it.
    public JSONObject RevertStatus() throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        
        String lsStatus = getPrevStatus();
        System.out.println("PREVIOUS STATUS : " + lsStatus);
        if(lsStatus != null && !"".equals(lsStatus)){
            try {
                poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "RevertStatus", lsStatus, false, true);
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                poJSON = successJSON("Transaction status revert successfully.");
            } catch (GuanzonException | SQLException | CloneNotSupportedException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
                poJSON = errorJSON(MiscUtil.getException(ex));
            }
        }
        return poJSON;
    }
    
    //Check Previous Status
    public String getPrevStatus() throws SQLException{
        String lsSQL = " SELECT cRefrStat FROM Transaction_Status_History WHERE sSourceNo = "+SQLUtil.toSQL(Master().getTransactionNo())
                        + " AND sTableNme = "+SQLUtil.toSQL(Master().getTable())
                        + " AND cRefrStat != "+SQLUtil.toSQL(Master().getTransactionStatus()) //Not equal to current status
                        + " ORDER BY sTransNox DESC LIMIT 1  ";
        System.out.println("SQL : " + lsSQL);
        ResultSet loRS = this.poGRider.executeQuery(lsSQL);
        String lsStatus = "";
        if(loRS != null){
            if(loRS.next()){
                lsStatus = loRS.getString("cRefrStat");
            }
            MiscUtil.close(loRS);
        }
        return lsStatus;
    }

    public JSONObject VoidTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        final String lsStatus = PurchaseOrderStatus.VOID;

        if (getEditMode() != EditMode.READY) {
            return errorJSON("No transacton was loaded.");
        }

        final MatrixAuthChecker[] loCheck = new MatrixAuthChecker[1];
        poJSON = authorizeStatus(lsStatus, remarks, "Void Transaction", loCheck);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON; //error or "matrix"
        }

        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //industry preparation before the transaction starts
        poJSON = industryPrepareStatusChange(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = inTransaction("Void Transaction", true, () -> {
            JSONObject loJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
            if (!"success".equals((String) loJSON.get("result"))) {
                return loJSON;
            }

            //Update Transaction Status of PO Quotation
            loJSON = updatePOQuotationStatus(lsStatus);
            if ("error".equals((String) loJSON.get("result"))) {
                System.out.println("PO Quotation Saving " + (String) loJSON.get("message"));
                return loJSON;
            }

            //industry procedure, inside the same transaction
            loJSON = industryStatusChange(lsStatus);
            if (!"success".equals((String) loJSON.get("result"))) {
                return loJSON;
            }
            
            loJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
            if (!"success".equals((String) loJSON.get("result"))) {
                return loJSON;
            }

            if (loCheck[0] != null) {
                loCheck[0].postAuth();
            }
            return successJSON("Transaction voided successfully.");
        });
        return poJSON;
    }

    public JSONObject ReturnTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        final String lsStatus = PurchaseOrderStatus.RETURNED;

        if (getEditMode() != EditMode.READY) {
            return errorJSON("No transacton was loaded.");
        }

        //validator (includes the "already returned" rule)
        poJSON = isEntryOkay(PurchaseOrderStatus.RETURNED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            } else {
                if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                    return errorJSON("User is not an authorized approving officer..");
                }
                setApproving((String) poJSON.get("sUserIDxx"));
            }
        }
        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //industry preparation before the transaction starts
        poJSON = industryPrepareStatusChange(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = inTransaction("Return Transaction", true, () -> {
            //industry procedure, inside the same transaction
            JSONObject loJSON = industryStatusChange(lsStatus);
            if (!"success".equals((String) loJSON.get("result"))) {
                return loJSON;
            }
            
            loJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false, true);
            if (!"success".equals((String) loJSON.get("result"))) {
                return loJSON;
            }

            loJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
            if (!"success".equals((String) loJSON.get("result"))) {
                return loJSON;
            }

            //Update Transaction Status of PO Quotation
            loJSON = updatePOQuotationStatus(lsStatus);
            if ("error".equals((String) loJSON.get("result"))) {
                System.out.println("PO Quotation Saving " + (String) loJSON.get("message"));
                return loJSON;
            }

            return successJSON("Transaction returned successfully.");
        });
        return poJSON;
    }

    public JSONObject isDetailHasZeroQty() {
        //rule in the validator; same result shape as before ("tableRow", "warning")
        poJSON = rules().validateQuantities();
        return poJSON;
    }

    private JSONObject setValueToOthers(String status)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poStockRequest = new ArrayList<>();
        poPOQuotation = new ArrayList<>();
        poPOQuotationStatus = new ArrayList<>();
        poPOQuotationRemovedStatus = new ArrayList<>();
        int lnCtr;

        //Source-line rules (stock request quantities, quotation already ordered) live in the validator
        poJSON = rules(status).validateSourceLines();
        if (!PurchaseOrderValidator.isSuccess(poJSON)) {
            return poJSON;
        }

        //Update the stock requests and quotations this PO came from
        for (lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            String lsSourceNo = Detail(lnCtr).getSouceNo();
            if (lsSourceNo == null || lsSourceNo.isEmpty()) {
                continue;
            }

            if (PurchaseOrderStatus.SourceCode.STOCKREQUEST.equals(Detail(lnCtr).getSouceCode())) {
                updateInvStockRequest(status, lsSourceNo, Detail(lnCtr).getStockID(), Detail(lnCtr).getQuantity().doubleValue());
            } else if (PurchaseOrderStatus.SourceCode.POQUOTATION.equals(Detail(lnCtr).getSouceCode())) {
                updatePOQuotation(status, lsSourceNo, Detail(lnCtr).getStockID(), Detail(lnCtr).getQuantity().doubleValue(), false);
            }
        }
        //Update stock request removed in purchase order
        for (lnCtr = 0; lnCtr <= getDetailRemovedCount() - 1; lnCtr++) {
            //FIX: read the source code from the REMOVED row (was Detail(lnCtr))
            switch (DetailRemove(lnCtr).getSouceCode()) {
                case PurchaseOrderStatus.SourceCode.STOCKREQUEST:
                    updateInvStockRequest(status, DetailRemove(lnCtr).getSouceNo(), DetailRemove(lnCtr).getStockID(), DetailRemove(lnCtr).getQuantity().doubleValue());
                    break;
                case PurchaseOrderStatus.SourceCode.POQUOTATION:
                    updatePOQuotation(status, DetailRemove(lnCtr).getSouceNo(), DetailRemove(lnCtr).getStockID(), DetailRemove(lnCtr).getQuantity().doubleValue(), true);
                    break;
            }
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    //TODO
    private void updatePOQuotation(String status, String orderNo, String stockId, double quantity, boolean isRemoved)
            throws GuanzonException,
            SQLException,
            CloneNotSupportedException {
        int lnRow, lnList;
        boolean lbExist = false;
        //2.check if order no is already exist in purchase order array list
        for (lnRow = 0; lnRow <= poPOQuotation.size() - 1; lnRow++) {
            if (poPOQuotation.get(lnRow).Master().getTransactionNo() != null) {
                if (orderNo.equals(poPOQuotation.get(lnRow).Master().getTransactionNo())) {
                    lbExist = true;
                    break;
                }
            }
        }

        if (!lbExist) {
            //Populate updating of status
            if (isRemoved) {
                poPOQuotationRemovedStatus.add(POQuotation());
                poPOQuotationRemovedStatus.get(poPOQuotationRemovedStatus.size() - 1).InitTransaction();
                poPOQuotationRemovedStatus.get(poPOQuotationRemovedStatus.size() - 1).OpenTransaction(orderNo);
            } else {
                poPOQuotationStatus.add(POQuotation());
                poPOQuotationStatus.get(poPOQuotationStatus.size() - 1).InitTransaction();
                poPOQuotationStatus.get(poPOQuotationStatus.size() - 1).OpenTransaction(orderNo);
            }

            poPOQuotation.add(POQuotation());
            poPOQuotation.get(poPOQuotation.size() - 1).InitTransaction();
            poPOQuotation.get(poPOQuotation.size() - 1).OpenTransaction(orderNo);
            //Update PO Quotation
            poPOQuotation.get(poPOQuotation.size() - 1).UpdateTransaction();
            lnList = poPOQuotation.size() - 1;
        } else {
            //if already exist, get the row no of purchase order
            lnList = lnRow;
        }
        //TODO UPDATING OF DETAILS (see version history for the commented-out detail update loop)
    }

    private void updateInvStockRequest(String status, String orderNo, String stockId, double quantity)
            throws GuanzonException,
            SQLException,
            CloneNotSupportedException {
        int lnRow, lnList;
        double lnRecQty = 0.00;
        boolean lbExist = false;
        //2.check if order no is already exist in purchase order array list
        for (lnRow = 0; lnRow <= poStockRequest.size() - 1; lnRow++) {
            if (poStockRequest.get(lnRow).Master().getTransactionNo() != null) {
                if (orderNo.equals(poStockRequest.get(lnRow).Master().getTransactionNo())) {
                    lbExist = true;
                    break;
                }
            }
        }

        if (!lbExist) {
            poStockRequest.add(StockRequest());
            poStockRequest.get(poStockRequest.size() - 1).InitTransaction();
            poStockRequest.get(poStockRequest.size() - 1).OpenTransaction(orderNo);
            poStockRequest.get(poStockRequest.size() - 1).UpdateTransaction();
            lnList = poStockRequest.size() - 1;
        } else {
            //if already exist, get the row no of purchase order
            lnList = lnRow;
        }

        for (lnRow = 0; lnRow <= poStockRequest.get(lnList).getDetailCount() - 1; lnRow++) {
            if (stockId.equals(poStockRequest.get(lnList).Detail(lnRow).getStockId())) {

                switch (status) {
                    case PurchaseOrderStatus.CONFIRMED:
                        lnRecQty = getRequestQty(orderNo, stockId, true, poStockRequest.get(lnList).getSourceCode());
                        lnRecQty = lnRecQty + quantity;
                        break;
                    case PurchaseOrderStatus.APPROVED:
                        lnRecQty = poStockRequest.get(lnList).Detail(lnRow).getPurchase();
                        poStockRequest.get(lnList).Master().setProcessed(true);
                        poStockRequest.get(lnList).Master().setModifiedDate(poGRider.getServerDate());
                        poStockRequest.get(lnList).Master().setModifyingId(poGRider.getUserID());
                        break;
                    case PurchaseOrderStatus.VOID:
                    case PurchaseOrderStatus.RETURNED:
                        lnRecQty = getRequestQty(orderNo, stockId, false, poStockRequest.get(lnList).getSourceCode());
                        lnRecQty = lnRecQty - quantity;
                        break;
                }
                if (lnRecQty < 0) {
                    lnRecQty = 0;
                }
                poStockRequest.get(lnList).Detail(lnRow).setPurchase(lnRecQty);
                poStockRequest.get(lnList).Detail(lnRow).setModifiedDate(poGRider.getServerDate());
                break;
            }
        }
    }

    private JSONObject generatePRF(String fsStatus)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        //Added validation to generate PRF only if the PO Status is APPROVED. Arsiela 03-10-2026 11:28am
        if (!PurchaseOrderStatus.APPROVED.equals(fsStatus)) {
            poJSON.put("result", "success");
            return poJSON;
        }

        try {
            System.out.println("poJSON = generatePRF()");
            //Added validation for rate - Arsiela 07-15-2026 (now in isAdvancePaymentRequestRequired)
            if (isAdvancePaymentRequestRequired()) {
                poPaymentRequest = new CashflowControllers(poGRider, null);

                poPaymentRequest.PaymentRequest().InitTransaction();
                poPaymentRequest.PaymentRequest().NewTransaction();

                SearchPayee(Master().getSupplierID());

                poPaymentRequest.PaymentRequest().Master().setTransactionDate(Master().getTransactionDate());
                poPaymentRequest.PaymentRequest().Master().setBranchCode(Master().getDestinationID());
                if (poGRider.isMainOffice() || poGRider.isWarehouse()) {
                    poPaymentRequest.PaymentRequest().Master().setDepartmentID(poGRider.getDepartment());
                }

                //Disable previous computation and replace by the computation below - Arsiela 07-15-2026
                Double downPaymentFixedAmount = Master().getDownPaymentRatesAmount().doubleValue();
                Double downPaymentPercentage = Master().getDownPaymentRatesPercentage().doubleValue();
                Double downPaymentPercentageAmount = (Master().getNetTotal().doubleValue() / 100) * downPaymentPercentage;
                Double totalAdv = downPaymentPercentageAmount + downPaymentFixedAmount;

                poPaymentRequest.PaymentRequest().Master().setRemarks(Master().getRemarks());
                poPaymentRequest.PaymentRequest().Master().setIndustryID(Master().getIndustryID());
                poPaymentRequest.PaymentRequest().Master().setCompanyID(Master().getCompanyID());
                poPaymentRequest.PaymentRequest().Master().setSourceCode(SOURCE_CODE);
                poPaymentRequest.PaymentRequest().Master().setSourceNo(Master().getTransactionNo());
                poPaymentRequest.PaymentRequest().Master().setPayeeID(PayeeID); //Master().getSupplierID()
                poPaymentRequest.PaymentRequest().Master().setEntryNo(1);
                poPaymentRequest.PaymentRequest().Master().setSeriesNo(poPaymentRequest.PaymentRequest().getSeriesNoByBranch());
                poPaymentRequest.PaymentRequest().Master().setTranTotal(totalAdv);
                poPaymentRequest.PaymentRequest().Master().setNetTotal(totalAdv);
                poPaymentRequest.PaymentRequest().Master().setTransactionStatus(PurchaseOrderStatus.CONFIRMED);

                poPaymentRequest.PaymentRequest().Detail(0).setEntryNo((int) 1);
                poPaymentRequest.PaymentRequest().Detail(0).setParticularID(PurchaseOrderStaticData.default_empty_string);
                poPaymentRequest.PaymentRequest().Detail(0).setAmount(totalAdv);
                poPaymentRequest.PaymentRequest().Detail(0).setDiscount(PurchaseOrderStaticData.default_value_double);
                poPaymentRequest.PaymentRequest().Detail(0).setAddDiscount(PurchaseOrderStaticData.default_value_double);
                poPaymentRequest.PaymentRequest().Detail(0).setVatable(PurchaseOrderStaticData.default_value_string);
                poPaymentRequest.PaymentRequest().Detail(0).setWithHoldingTax(PurchaseOrderStaticData.default_value_double);
                poPaymentRequest.PaymentRequest().Detail(0).setPRFRemarks(Master().getRemarks()); //Autoset PO Remarks to PRF - Arsiela 07-15-2026
                poPaymentRequest.PaymentRequest().Detail(0).isReverse(true); //Added by Arsiela 07-15-2026
                poPaymentRequest.PaymentRequest().AddDetail();
            }

        } catch (Exception e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    //FIX: returns double; quantities are decimal and getInt() truncated them
    private double getRequestQty(String orderNo, String stockId, boolean isAdd, String sourceCode) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        double lnRecQty = 0.0000;
        String lsSQL = "SELECT b.nQuantity AS nQuantity "
                + "FROM PO_Master a "
                + "LEFT JOIN PO_Detail b ON b.sTransNox = a.sTransNox ";

        lsSQL = MiscUtil.addCondition(lsSQL, " b.sSourceNo = " + SQLUtil.toSQL(orderNo)
                + " AND b.sStockIDx = " + SQLUtil.toSQL(stockId)
                + " AND ( a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.CONFIRMED)
                + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.APPROVED)
                + " ) "
                + " AND b.sSourceCd = " + SQLUtil.toSQL(sourceCode)
        );
        if (isAdd) {
            lsSQL = lsSQL + " AND a.sTransNox <> " + SQLUtil.toSQL(Master().getTransactionNo());
        }

        System.out.println("executeQuery: >>>> " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) >= 0) {
                while (loRS.next()) {
                    lnRecQty = lnRecQty + loRS.getDouble("nQuantity");
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
            lnRecQty = 0;
        }
        return lnRecQty;
    }

    private JSONObject savePRF(String fsStatus)
            throws CloneNotSupportedException {
        poJSON = new JSONObject();

        //Added validation to generate PRF only if the PO Status is APPROVED. Arsiela 03-10-2026 11:28am
        if (!PurchaseOrderStatus.APPROVED.equals(fsStatus)) {
            poJSON.put("result", "success");
            return poJSON;
        }

        try {
            //Added validation for rate - Arsiela 07-15-2026 (now in isAdvancePaymentRequestRequired)
            if (isAdvancePaymentRequestRequired()) {
                
                //Populate PO attachment to PRF - Arsiela 07-06-2026
                poPaymentRequest.PaymentRequest().loadPOAttachment(Master().getTransactionNo());

                poPaymentRequest.PaymentRequest().setWithParent(true);
                poJSON = poPaymentRequest.PaymentRequest().SaveTransaction();
                if ("error".equals((String) poJSON.get("result"))) {
                    poJSON.put("result", "error");
                    return poJSON;

                }
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(PurchaseOrderMain.class
                    .getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    private JSONObject saveUpdates(String status)
            throws CloneNotSupportedException {
        poJSON = new JSONObject();
        int lnCtr;
        try {
            for (lnCtr = 0; lnCtr <= poStockRequest.size() - 1; lnCtr++) {
                if (PurchaseOrderStatus.APPROVED.equals(status)) {
                    poStockRequest.get(lnCtr).Master().setProcessed(true);
                }
                poStockRequest.get(lnCtr).Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
                poStockRequest.get(lnCtr).Master().setModifiedDate(poGRider.getServerDate());
                poStockRequest.get(lnCtr).setWithParent(true);
                poJSON = poStockRequest.get(lnCtr).SaveTransaction();
                if ("error".equals((String) poJSON.get("result"))) {
                    System.out.println("Stock Request Saving " + (String) poJSON.get("message"));
                    return poJSON;

                }
            }

            //TODO Save Ordered Quantity
            for (lnCtr = 0; lnCtr <= poPOQuotation.size() - 1; lnCtr++) {
                poPOQuotation.get(lnCtr).Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
                poPOQuotation.get(lnCtr).Master().setModifiedDate(poGRider.getServerDate());
                poPOQuotation.get(lnCtr).setWithParent(true);
                poJSON = poPOQuotation.get(lnCtr).SaveTransaction();
                if ("error".equals((String) poJSON.get("result"))) {
                    System.out.println("PO Quotation Saving " + (String) poJSON.get("message"));
                    return poJSON;
                }
            }

        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(PurchaseOrderMain.class
                    .getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    private JSONObject updatePOQuotationStatus(String fsStatus) throws CloneNotSupportedException, SQLException, GuanzonException, ParseException {
        for (int lnCtr = 0; lnCtr <= poPOQuotationStatus.size() - 1; lnCtr++) {
            switch (fsStatus) {
                case PurchaseOrderStatus.CONFIRMED:
                    poPOQuotationStatus.get(lnCtr).setWithParent(true);
                    poJSON = poPOQuotationStatus.get(lnCtr).PostTransaction("");
                    if ("error".equals((String) poJSON.get("result"))) {
                        System.out.println("PO Quotation Posting " + (String) poJSON.get("message"));
                        return poJSON;
                    }
                    break;
                case PurchaseOrderStatus.RETURNED:
                case PurchaseOrderStatus.VOID:
                case PurchaseOrderStatus.CANCELLED:
                    poJSON = rules(fsStatus).validateQuotationNotOrdered(poPOQuotationStatus.get(lnCtr).Master().getTransactionNo());
                    if ("success".equals((String) poJSON.get("result"))) {
                        poPOQuotationStatus.get(lnCtr).setWithParent(true);
                        poJSON = poPOQuotationStatus.get(lnCtr).ApproveTransaction("");
                        if ("error".equals((String) poJSON.get("result"))) {
                            System.out.println("PO Quotation Approving " + (String) poJSON.get("message"));
                            return poJSON;
                        }
                    }
                    break;
            }
        }

        for (int lnCtr = 0; lnCtr <= poPOQuotationRemovedStatus.size() - 1; lnCtr++) {
            poJSON = rules(fsStatus).validateQuotationNotOrdered(poPOQuotationRemovedStatus.get(lnCtr).Master().getTransactionNo());
            if ("success".equals((String) poJSON.get("result"))) {
                poPOQuotationRemovedStatus.get(lnCtr).setWithParent(true);
                poJSON = poPOQuotationRemovedStatus.get(lnCtr).ApproveTransaction("");
                if ("error".equals((String) poJSON.get("result"))) {
                    System.out.println("PO Quotation Approving " + (String) poJSON.get("message"));
                    return poJSON;
                }
            }
        }

        poJSON = successJSON();
        return poJSON;
    }

    @Override
    public void initSQL() {
        SQL_BROWSE = "SELECT "
                + "  a.sTransNox,"
                + "  a.dTransact,"
                + "  b.sDescript,"
                + "  c.sCompnyNm,"
                + "  e.sCompnyNm, "
                + "  f.sDescript,"
                + "  a.sBranchCd,"
                + "  a.sReferNox"
                + " FROM PO_Master a "
                + " LEFT JOIN Industry b ON a.sIndstCdx = b.sIndstCdx "
                + " LEFT JOIN Company c ON c.sCompnyID = a.sCompnyID "
                + " LEFT JOIN Client_Master e ON a.sSupplier = e.sClientID"
                + " , Category f ";
    }

    public JSONObject SearchTransaction(String fsValue, String fsSupplierID, String fsReferID,String fsTransNo, int fnSort) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsTransStat = " AND ( a.cTranStat IN (" + lsTransStat.substring(2) + ")"
                    + " OR (ASCII(a.cTranStat) - 64) IN (" + lsTransStat.substring(2) + ") )";
        } else {
            lsTransStat = " AND ( a.cTranStat = " + SQLUtil.toSQL(psTranStat)
                    + " OR (ASCII(a.cTranStat) - 64) = " + SQLUtil.toSQL(psTranStat) + " ) ";
        }

        initSQL();
        String lsFilterCondition = String.join(" AND ", "a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()),
                " a.sCategrCd = f.sCategrCd ",
                " a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID()),
                " a.sSupplier LIKE " + SQLUtil.toSQL("%" + fsSupplierID),
                " a.sCategrCd LIKE " + SQLUtil.toSQL("%" + Master().getCategoryCode()),
                " a.sReferNox LIKE " + SQLUtil.toSQL("%" + fsReferID),
                " a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsTransNo));

        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, lsFilterCondition);
        if (!psTranStat.isEmpty()) {
            lsSQL = lsSQL + lsTransStat;
        }
        //FIX: was || (always true unless user is both main office AND warehouse)
        if (!poGRider.isMainOffice() && !poGRider.isWarehouse()) {
            lsSQL = lsSQL + " AND a.sBranchCd LIKE " + SQLUtil.toSQL(poGRider.getBranchCode());
        }

        lsSQL = lsSQL + " GROUP BY a.sTransNox";
        System.out.println("SQL EXECUTED: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                fsValue,
                "Transaction Date»Transaction No»Reference No»Company»Supplier",
                "dTransact»sTransNox»a.sReferNox»c.sCompnyNm»e.sCompnyNm",
                "dTransact»sTransNox»a.sReferNox»e.sCompnyNm",
                fnSort);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            return errorJSON("No record loaded.");
        }
    }
    

    /*Search Master References*/
    public JSONObject SearchBranch(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            Master().setBranchCode(object.getModel().getBranchCode());
        }

        return poJSON;
    }

    public JSONObject SearchIndustry(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Industry object = new ParamControllers(poGRider, logwrapr).Industry();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setIndustryID(object.getModel().getIndustryId());
        }

        return poJSON;
    }
    
    public JSONObject SearchCategory(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Category object = new ParamControllers(poGRider, logwrapr).Category();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setCategoryCode(object.getModel().getCategoryId());
        }

        return poJSON;
    }

    public JSONObject SearchTerm(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Term object = new ParamControllers(poGRider, logwrapr).Term();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setTermCode(object.getModel().getTermId());
        }

        return poJSON;
    }

    /**
     * Shared inventory lookup for the SearchBarcode* / SearchModel methods.
     *
     * @param fbUseIndustry  filter by the PO's industry (false for the *General variants)
     * @param fsRecordStatus inventory record status filter
     * @param fsDupLabel     prefix for the duplicate-row message ("Model: " / "Barcode: ")
     * @param fbSetBrand     also copy the brand from the selected item (SearchModel)
     */
    private JSONObject searchInventory(String value, boolean byCode, int row, boolean hasNoSupplier,
            boolean fbUseIndustry, String fsRecordStatus, String fsDupLabel, boolean fbSetBrand)
            throws SQLException, GuanzonException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(fsRecordStatus);

        String supplier = Master().getSupplierID().isEmpty() ? null : Master().getSupplierID();
        String brand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String industry = (!fbUseIndustry || Master().getIndustryID().isEmpty()) ? null : Master().getIndustryID();
        String category = Master().getCategoryCode();

        poJSON = object.searchRecord(
                value,
                byCode,
                hasNoSupplier ? supplier : null,
                brand,
                industry,
                category
        );

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        String lsStockId = object.getModel().getStockId();

        //An item may appear only once among manual lines (rule in the validator)
        JSONObject loCheck = rules().validateUniqueItem(row, lsStockId, fsDupLabel + object.getModel().getDescription());
        if (!PurchaseOrderValidator.isSuccess(loCheck)) {
            poJSON = loCheck;
            return poJSON;
        }

        if (fbSetBrand) {
            Detail(row).setBrandId(object.getModel().getBrandId());
        }
        Detail(row).setStockID(lsStockId);
        Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
        Detail(row).setOldPrice(object.getModel().getCost().doubleValue());
        return poJSON;
    }

    public JSONObject SearchBarcode(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException, NullPointerException {
        return searchInventory(value, byCode, row, hasNoSupplier, true, RecordStatus.ACTIVE, "Model: ", false);
    }

    public JSONObject SearchBarcodeGeneral(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException, NullPointerException {
        return searchInventory(value, byCode, row, hasNoSupplier, false, RecordStatus.ACTIVE, "Model: ", false);
    }

    public JSONObject SearchBarcodeDescription(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException, NullPointerException {
        return searchInventory(value, byCode, row, hasNoSupplier, true, RecordStatus.ACTIVE, "Barcode: ", false);
    }

    public JSONObject SearchBarcodeDescriptionGeneral(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException, NullPointerException {
        return searchInventory(value, byCode, row, hasNoSupplier, false, RecordStatus.ACTIVE, "Barcode: ", false);
    }

    public JSONObject SearchModel(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws SQLException, GuanzonException, NullPointerException {
        return searchInventory(value, byCode, row, hasNoSupplier, true, "123", "Barcode: ", true);
    }

    public JSONObject SearchSupplier(String value, boolean byCode) throws SQLException, GuanzonException {

        AP_Client_Master object = new ClientControllers(poGRider, logwrapr).APClientMaster();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            Master().setSupplierID(object.getModel().getClientId());
            Master().setAddressID(object.getModel().ClientAddress().getAddressId());
            Master().setContactID(object.getModel().ClientInstitutionContact().getContactPId());
            if (object.getModel().getTermId() != null && !"".equals(object.getModel().getTermId())) {
                Master().setTermCode(object.getModel().getTermId());
            }
        }

        return poJSON;
    }

    public JSONObject SearchProject(String value) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Project object = new ParamControllers(poGRider, logwrapr).Project();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, true);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setReference(object.getModel().getProjectID());
        }

        return poJSON;
    }

    public JSONObject SearchPayee(String value) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Payee object = new CashflowControllers(poGRider, logwrapr).Payee();
        object.setRecordStatus("1");

        poJSON = object.searchPayee(value);

        if ("success".equals((String) poJSON.get("result"))) {
            PayeeID = (String) poJSON.get("sPayeeIDx");
        }

        return poJSON;
    }

    public JSONObject SearchCompany(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Company object = new ParamControllers(poGRider, logwrapr).Company();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setCompanyID(object.getModel().getCompanyId());
        }
        return poJSON;
    }

    public JSONObject SearchDestination(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setDestinationID(object.getModel().getBranchCode());
        }
        return poJSON;
    }
    public JSONObject SearchDepartment(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Department object = new ParamControllers(poGRider, logwrapr).Department();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setDestinationID(object.getModel().getDepartmentId());
        }
        return poJSON;
    }

    public JSONObject SearchBrand(String value, boolean byCode, int row) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Brand brand = new ParamControllers(poGRider, logwrapr).Brand();
        brand.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = brand.searchRecord(value, byCode, Master().getIndustryID());
        if ("success".equals((String) poJSON.get("result"))) {
            Detail(row).setBrandId((String) poJSON.get("sBrandIDx"));
        }

        return poJSON;
    }

    private String getInvStockRequest_SQL() {
        return "SELECT"
                + "  a.sTransNox AS sTransNox,"
                + "  e.sBranchNm AS sBranchNm,"
                + "  a.sBranchCd,"
                + "  a.cTranStat,"
                + "  a.dTransact AS dTransact,"
                + "  a.sReferNox,"
                + "  a.cTranStat,"
                + "  a.sIndstCdx,"
                + "  COUNT(DISTINCT b.sStockIDx) AS total_details,"
                + "  SUM(b.nApproved - (b.nIssueQty + b.nOrderQty)) AS total_request,"
                + " '" + PurchaseOrderStatus.SourceCode.STOCKREQUEST + "' AS request_type"
                + " FROM Inv_Stock_Request_Master a"
                + " LEFT JOIN Inv_Stock_Request_Detail b ON a.sTransNox = b.sTransNox"
                + " LEFT JOIN Inventory c ON b.sStockIDx = c.sStockIDx"
                + " LEFT JOIN Branch e ON a.sBranchCd = e.sBranchCd"
                + " LEFT JOIN Industry f ON a.sIndstCdx = f.sIndstCdx"
                + " LEFT JOIN Inv_Supplier g ON g.sStockIDx = c.sStockIDx"
                + " LEFT JOIN Category h ON c.sCategCd1 = h.sCategrCd";
    }

    private String getPOQuotation_SQL() {
        return "SELECT"
                + "  a.sTransNox AS sTransNox,"
                + "  e.sBranchNm AS sBranchNm,"
                + "  a.sBranchCd,"
                + "  a.cTranStat,"
                + "  a.dTransact AS dTransact,"
                + "  a.sReferNox,"
                + "  a.cTranStat,"
                + "  a.sIndstCdx,"
                + "  COUNT(DISTINCT b.sStockIDx) AS total_details,"
                + "  SUM(b.nQuantity) AS total_request,"
                + "  '" + PurchaseOrderStatus.SourceCode.POQUOTATION + "' AS request_type"
                + " FROM PO_Quotation_Master a"
                + " LEFT JOIN PO_Quotation_Detail b ON a.sTransNox = b.sTransNox"
                + " LEFT JOIN Inventory c ON b.sStockIDx = c.sStockIDx"
                + " LEFT JOIN Branch e ON a.sBranchCd = e.sBranchCd"
                + " LEFT JOIN Industry f ON a.sIndstCdx = f.sIndstCdx"
                + " LEFT JOIN Inv_Supplier g ON g.sStockIDx = c.sStockIDx"
                + " LEFT JOIN Category h ON c.sCategCd1 = h.sCategrCd";
    }

    public JSONObject getApprovedStockRequests() throws SQLException, GuanzonException {
        String lsSQL = getInvStockRequest_SQL();
        String lsFilterCondition = String.join(" AND ", " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()),
                " e.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID()),
                " (g.sSupplier = " + SQLUtil.toSQL(Master().getSupplierID()) + " OR g.sSupplier IS NULL )",
                " b.nApproved > 0 ",
                " a.cProcessd = " + SQLUtil.toSQL(Logical.NO),
                " b.nApproved <> (b.nIssueQty + b.nOrderQty) ",
                " c.sCategCd1 = " + SQLUtil.toSQL(Master().getCategoryCode()),
                " a.cTranStat = " + SQLUtil.toSQL(StockRequestStatus.CONFIRMED));
        lsSQL = MiscUtil.addCondition(lsSQL, lsFilterCondition);
        //FIX: was ||
        if (!poGRider.isMainOffice() && !poGRider.isWarehouse()) {
            lsSQL = lsSQL + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode());
        }
        lsSQL = lsSQL
                + " GROUP BY a.sTransNox "
                + " HAVING SUM(b.nApproved - (b.nIssueQty + b.nOrderQty)) > 0 ";
        //For General Only
        lsSQL = lsSQL + " UNION " + getPOQuotation_SQL();
        lsFilterCondition = String.join(" AND ", " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()),
                " a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID()),
                " a.sSupplier = " + SQLUtil.toSQL(Master().getSupplierID()),
                " a.sCategrCd = " + SQLUtil.toSQL(Master().getCategoryCode()),
                " a.cTranStat = " + SQLUtil.toSQL(POQuotationStatus.APPROVED));
        lsSQL = lsSQL + " WHERE " + lsFilterCondition;
        //Do not include branch according to ma'am she 10082025
        lsSQL = lsSQL + " GROUP BY a.sTransNox ";

        lsSQL = lsSQL + " ORDER BY dTransact, sBranchNm, sTransNox ASC";//sort data into ascending according to ma'am she 05-28-2026 : Arsiela - 05-28-2026 03:43 PM

        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        JSONArray dataArray = new JSONArray();
        JSONObject loJSON = new JSONObject();

        if (loRS == null) {
            loJSON.put("result", "error");
            loJSON.put("message", "Query execution failed.");
            return loJSON;
        }

        try {
            int lnctr = 0;

            if (MiscUtil.RecordCount(loRS) >= 0) {
                while (loRS.next()) {
                    JSONObject request = new JSONObject();
                    request.put("sTransNox", loRS.getString("sTransNox"));
                    request.put("sBranchCd", loRS.getString("sBranchCd"));
                    request.put("dTransact", loRS.getDate("dTransact"));
                    request.put("sReferNox", loRS.getString("sReferNox"));
                    request.put("cTranStat", loRS.getString("cTranStat"));
                    request.put("sBranchNm", loRS.getString("sBranchNm"));
                    request.put("total_details", loRS.getInt("total_details"));
                    request.put("request_type", loRS.getString("request_type"));

                    dataArray.add(request);
                    lnctr++;
                }
                loJSON.put("data", dataArray);
                loJSON.put("result", "success");
                loJSON.put("message", "Record loaded successfully.");
            } else {
                loJSON.put("result", "error");
                loJSON.put("data", new JSONArray());
                loJSON.put("message", "No records found.");
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            loJSON.put("result", "error");
            loJSON.put("message", e.getMessage());
        }

        return loJSON;
    }

    public JSONObject addStockRequestOrdersToPODetail(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        InvWarehouseControllers loTrans = new InvWarehouseControllers(poGRider, logwrapr);
        poJSON = loTrans.StockRequest().InitTransaction();

        if (!"success".equals(poJSON.get("result"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "No records found.");
            return poJSON;
        }

        poJSON = loTrans.StockRequest().OpenTransaction(transactionNo);
        if (!"success".equals(poJSON.get("result"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "No records found.");
            return poJSON;
        }

        if (areAllStockRequestDetailsInPODetail(loTrans)) {
            poJSON.put("result", "error");
            poJSON.put("message", "All stock request details are already in purchase order detail.");
            return poJSON;
        }
        String detailQuery = "SELECT b.sStockIDx "
                + "FROM Inv_Stock_Request_Master a "
                + "LEFT JOIN Branch e ON a.sBranchCd = e.sBranchCd "
                + "LEFT JOIN Inv_Stock_Request_Detail b ON a.sTransNox = b.sTransNox "
                + "LEFT JOIN Inventory c ON b.sStockIDx = c.sStockIDx "
                + "LEFT JOIN Inv_Supplier g ON g.sStockIDx = c.sStockIDx "
                + "LEFT JOIN Category h ON h.sCategrCd = c.sCategCd1";

        String lsFilterCondition = String.join(" AND ",
                " a.sIndstCdx LIKE " + SQLUtil.toSQL("%" + Master().getIndustryID()),
                " e.sCompnyID LIKE " + SQLUtil.toSQL("%" + Master().getCompanyID()),
                " h.sCategrCd LIKE " + SQLUtil.toSQL("%" + Master().getCategoryCode()),
                " g.sSupplier LIKE " + SQLUtil.toSQL("%" + Master().getSupplierID()),
                " b.nApproved > 0",
                " a.cTranStat = " + SQLUtil.toSQL(Logical.YES));

        detailQuery = MiscUtil.addCondition(detailQuery, lsFilterCondition);
        System.out.println("Executing SQL: " + detailQuery);
        // Fetch valid stock IDs
        Set<String> validStockIds = new HashSet<>();
        try (ResultSet rsDetail = poGRider.executeQuery(detailQuery)) {
            if (rsDetail == null) {
                poJSON.put("result", "error");
                poJSON.put("message", "Query execution failed.");
                return poJSON;
            }
            while (rsDetail.next()) {
                validStockIds.add(rsDetail.getString("sStockIDx"));
            }
        }

        boolean allProcessed = true;

        for (int lnCtr = 0; lnCtr < loTrans.StockRequest().getDetailCount(); lnCtr++) {
            if (!validStockIds.contains(loTrans.StockRequest().Detail(lnCtr).getStockId())) {
                continue; // Skip details that do not match the master conditions
            }

            // If at least one stock is not fully processed, set flag to false
            if (loTrans.StockRequest().Detail(lnCtr).getApproved()
                    != loTrans.StockRequest().Detail(lnCtr).getIssued()
                    + loTrans.StockRequest().Detail(lnCtr).getPurchase()) {
                allProcessed = false;
            }

            boolean exists = false;
            for (int lnRow = 0; lnRow < getDetailCount(); lnRow++) {
                if (Detail(lnRow).getSouceNo().equals(loTrans.StockRequest().Detail(lnCtr).getTransactionNo())
                        && Detail(lnRow).getStockID().equals(loTrans.StockRequest().Detail(lnCtr).getStockId())) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                double remainingStock = loTrans.StockRequest().Detail(lnCtr).getApproved()
                        - (loTrans.StockRequest().Detail(lnCtr).getIssued() + loTrans.StockRequest().Detail(lnCtr).getPurchase());
                if (remainingStock > 0) {
                    AddDetail();
                    int lnLastIndex = getDetailCount() - 1;
                    Detail(lnLastIndex).setSouceNo(loTrans.StockRequest().Detail(lnCtr).getTransactionNo());
                    Detail(lnLastIndex).setTransactionNo(loTrans.StockRequest().Detail(lnCtr).getTransactionNo());
                    Detail(lnLastIndex).setEntryNo(loTrans.StockRequest().Detail(lnCtr).getEntryNumber());
                    Detail(lnLastIndex).setStockID(loTrans.StockRequest().Detail(lnCtr).getStockId());
                    Detail(lnLastIndex).setRecordOrder(0);
                    Detail(lnLastIndex).setUnitPrice(loTrans.StockRequest().Detail(lnCtr).Inventory().getCost().doubleValue());
                    Detail(lnLastIndex).setQuantity(0);
                    Detail(lnLastIndex).setSouceCode(SOURCE_CODE);
                }
            }
        }

        // Only check `allProcessed` after the loop
        if (allProcessed) {
            poJSON.put("result", "error");
            poJSON.put("message", "All records are already processed!");
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("message", "Record loaded successfully.");
        return poJSON;
    }

    private boolean areAllStockRequestDetailsInPODetail(InvWarehouseControllers loTrans) {
        for (int lnCtr = 0; lnCtr < loTrans.StockRequest().getDetailCount(); lnCtr++) {
            boolean found = false;

            for (int lnRow = 0; lnRow < getDetailCount(); lnRow++) {
                if (Detail(lnRow).getSouceNo().equals(loTrans.StockRequest().Detail(lnCtr).getTransactionNo())
                        && Detail(lnRow).getStockID().equals(loTrans.StockRequest().Detail(lnCtr).getStockId()
                        )) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }
        return true;
    }

    public JSONObject addPOQuotationToPODetail(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        QuotationControllers loTrans = new QuotationControllers(poGRider, logwrapr);
        poJSON = loTrans.POQuotation().InitTransaction();

        if (!"success".equals(poJSON.get("result"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "No records found.");
            return poJSON;
        }

        poJSON = loTrans.POQuotation().OpenTransaction(transactionNo);
        if (!"success".equals(poJSON.get("result"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "No records found.");
            return poJSON;
        }

        if (areAllQuotationControllerIsInPODetail(loTrans)) {
            poJSON.put("result", "error");
            poJSON.put("message", "All PO Quotation details are already in purchase order detail.");
            return poJSON;
        }

        //A quotation may feed only one PO (rule in the validator)
        poJSON = rules().validateQuotationNotOrdered(transactionNo);
        if ("error".equals(poJSON.get("result"))) {
            return poJSON;
        }

        double ldblRemainingDiscount = 0.0000;
        double ldblDiscountAmt = 0.0000;
        double ldblDiscountRateAmt = 0.0000;
        double ldblVATAmount = 0.0000;
        if (loTrans.POQuotation().Master().getVatAmount() > 0.0000) {
            ldblVATAmount = loTrans.POQuotation().Master().getVatAmount() / loTrans.POQuotation().getDetailCount();
        }
        if (loTrans.POQuotation().Master().getAdditionalDiscountAmount() > 0.0000) {
            ldblDiscountAmt = loTrans.POQuotation().Master().getAdditionalDiscountAmount() / loTrans.POQuotation().getDetailCount();
        }
        if (loTrans.POQuotation().Master().getDiscountRate() > 0) {
            ldblDiscountRateAmt = loTrans.POQuotation().Master().getGrossAmount() * (loTrans.POQuotation().Master().getDiscountRate() / 100);
            ldblDiscountRateAmt = ldblDiscountRateAmt / loTrans.POQuotation().getDetailCount();
        }
        for (int lnCtr = 0; lnCtr < loTrans.POQuotation().getDetailCount(); lnCtr++) {
            boolean exists = false;
            for (int lnRow = 0; lnRow < getDetailCount(); lnRow++) {
                if (loTrans.POQuotation().Detail(lnCtr).getReplaceId() != null && !"".equals(loTrans.POQuotation().Detail(lnCtr).getReplaceId())) {
                    if (Detail(lnRow).getSouceNo().equals(loTrans.POQuotation().Detail(lnCtr).getTransactionNo())
                            && Detail(lnRow).getStockID().equals(loTrans.POQuotation().Detail(lnCtr).getReplaceId())) {
                        exists = true;
                        break;
                    }
                } else {
                    if (Detail(lnRow).getSouceNo().equals(loTrans.POQuotation().Detail(lnCtr).getTransactionNo())
                            && Detail(lnRow).getStockID().equals(loTrans.POQuotation().Detail(lnCtr).getStockId())) {
                        exists = true;
                        break;
                    }
                }
            }
            double ldblMasterDiscount = 0.0000;
            double ldblUnitPrice = 0.0000;
            if (!exists) {
                int lnLastIndex = getDetailCount() - 1;
                Detail(lnLastIndex).setSouceNo(loTrans.POQuotation().Detail(lnCtr).getTransactionNo());
                Detail(lnLastIndex).setTransactionNo(loTrans.POQuotation().Detail(lnCtr).getTransactionNo());
                Detail(lnLastIndex).setRecordOrder(loTrans.POQuotation().Detail(lnCtr).getQuantity());
                Detail(lnLastIndex).setUnitPrice(loTrans.POQuotation().Detail(lnCtr).getUnitPrice());
                Detail(lnLastIndex).setQuantity(loTrans.POQuotation().Detail(lnCtr).getQuantity());
                Detail(lnLastIndex).setSouceCode(loTrans.POQuotation().getSourceCode());
                if (loTrans.POQuotation().Detail(lnCtr).getReplaceId() != null && !"".equals(loTrans.POQuotation().Detail(lnCtr).getReplaceId())) {
                    Detail(lnLastIndex).setStockID(loTrans.POQuotation().Detail(lnCtr).getReplaceId());
                } else {
                    Detail(lnLastIndex).setStockID(loTrans.POQuotation().Detail(lnCtr).getStockId());
                }

                //subtract detail discount per item
                double ldblDetailDiscountRate = 0.0000;
                if (loTrans.POQuotation().Detail(lnCtr).getDiscountRate() > 0.00) {
                    ldblDetailDiscountRate = loTrans.POQuotation().Detail(lnCtr).getUnitPrice() * (loTrans.POQuotation().Detail(lnCtr).getDiscountRate() / 100);
                }
                ldblUnitPrice = loTrans.POQuotation().Detail(lnCtr).getUnitPrice() - (ldblDetailDiscountRate + loTrans.POQuotation().Detail(lnCtr).getDiscountAmount());
                if (loTrans.POQuotation().Master().isVatable()) {
                    ldblUnitPrice = ldblUnitPrice + (ldblVATAmount / loTrans.POQuotation().Detail(lnCtr).getQuantity());
                }
                //subtract master discount per item
                ldblMasterDiscount = (ldblDiscountAmt + ldblDiscountRateAmt + ldblRemainingDiscount) / loTrans.POQuotation().Detail(lnCtr).getQuantity();
                if (ldblMasterDiscount > ldblUnitPrice) {
                    ldblRemainingDiscount = (ldblDiscountAmt + ldblDiscountRateAmt + ldblRemainingDiscount) - (ldblUnitPrice * loTrans.POQuotation().Detail(lnCtr).getQuantity());
                    ldblUnitPrice = 0.0000;
                } else {
                    ldblRemainingDiscount = 0.0000;
                    ldblUnitPrice = ldblUnitPrice - ldblMasterDiscount;
                }

                Detail(lnLastIndex).setUnitPrice(ldblUnitPrice);
                AddDetail();
            }

            //check if last counter; disribute remaining discount amount to quotation detail with unit price
            if (lnCtr == loTrans.POQuotation().getDetailCount() - 1) {
                if (ldblRemainingDiscount > 0.0000) {
                    for (int lnRow = 0; lnRow < getDetailCount(); lnRow++) {
                        if (Detail(lnRow).getSouceNo().equals(loTrans.POQuotation().Master().getTransactionNo())
                                && Detail(lnRow).getSouceCode().equals(loTrans.POQuotation().getSourceCode())
                                && Detail(lnRow).getUnitPrice().doubleValue() > 0.0000) {
                            ldblUnitPrice = Detail(lnRow).getUnitPrice().doubleValue();
                            ldblMasterDiscount = ldblRemainingDiscount / Detail(lnRow).getRecordOrder().doubleValue();
                            if (ldblMasterDiscount > ldblUnitPrice) {
                                ldblRemainingDiscount = ldblRemainingDiscount - (ldblUnitPrice * Detail(lnRow).getRecordOrder().doubleValue());
                                ldblUnitPrice = 0.0000;
                            } else {
                                ldblRemainingDiscount = 0.0000;
                                ldblUnitPrice = ldblUnitPrice - ldblMasterDiscount;
                            }

                            Detail(lnRow).setUnitPrice(ldblUnitPrice);
                            if (ldblRemainingDiscount <= 0.0000) {
                                break;
                            }
                        }
                    }
                }
            }
        }

        poJSON.put("result", "success");
        poJSON.put("message", "Record loaded successfully.");
        return poJSON;
    }

    private boolean areAllQuotationControllerIsInPODetail(QuotationControllers loTrans) {
        for (int lnCtr = 0; lnCtr < loTrans.POQuotation().getDetailCount(); lnCtr++) {
            boolean found = false;

            for (int lnRow = 0; lnRow < getDetailCount(); lnRow++) {
                if (loTrans.POQuotation().Detail(lnCtr).getReplaceId() != null && !"".equals(loTrans.POQuotation().Detail(lnCtr).getReplaceId())) {
                    if (Detail(lnRow).getSouceNo().equals(loTrans.POQuotation().Detail(lnCtr).getTransactionNo())
                            && Detail(lnRow).getStockID().equals(loTrans.POQuotation().Detail(lnCtr).getReplaceId()
                            )) {
                        found = true;
                        break;
                    }
                } else {
                    if (Detail(lnRow).getSouceNo().equals(loTrans.POQuotation().Detail(lnCtr).getTransactionNo())
                            && Detail(lnRow).getStockID().equals(loTrans.POQuotation().Detail(lnCtr).getStockId()
                            )) {
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                return false;
            }
        }
        return true;
    }

    public JSONObject getPurchaseOrder(String fsSupplierID, String fsReferID,String fsTransactionNo) throws SQLException, GuanzonException {
        JSONObject loJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsTransStat = " AND ( a.cTranStat IN (" + lsTransStat.substring(2) + ")"
                    + " OR (ASCII(a.cTranStat) - 64) IN (" + lsTransStat.substring(2) + ") )";
        } else {
            lsTransStat = " AND ( a.cTranStat = " + SQLUtil.toSQL(psTranStat)
                    + " OR (ASCII(a.cTranStat) - 64) = " + SQLUtil.toSQL(psTranStat) + " ) ";
        }

        String lsSQL = " SELECT "
                + "  a.sTransNox,"
                + "  c.sBranchNm,"
                + "  a.sBranchCd,"
                + "  a.dTransact,"
                + "  a.sTransNox,"
                + "  a.cTranStat, "
                + "  e.sCompnyNm "
                + " FROM PO_Master a "
                + " LEFT JOIN PO_Detail b ON b.sTransNox = a.sTransNox"
                + " LEFT JOIN Branch c ON a.sBranchCd = c.sBranchCd"
                + " LEFT JOIN Industry d ON a.sIndstCdx = d.sIndstCdx"
                + " LEFT JOIN Client_Master e ON e.sClientID = a.sSupplier ";
        String lsFilterCondition = String.join(" AND ",
                " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()),
                " a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID()),
                " a.sSupplier LIKE " + SQLUtil.toSQL("%" + fsSupplierID),
                " a.sCategrCd = " + SQLUtil.toSQL(Master().getCategoryCode()),
                " a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsReferID));
        lsSQL = MiscUtil.addCondition(lsSQL, lsFilterCondition);
        if (!psTranStat.isEmpty()) {
            lsSQL = lsSQL + lsTransStat;
        }
        //FIX: was ||
        if (!poGRider.isMainOffice() && !poGRider.isWarehouse()) {
            lsSQL = lsSQL + " AND a.sBranchCd LIKE " + SQLUtil.toSQL(poGRider.getBranchCode());
        }
        lsSQL = lsSQL + " GROUP BY  a.sTransNox"
                + " ORDER BY a.dTransact,e.sCompnyNm,a.sTransNox ASC"; //sort data into ascending according to ma'am she 05-28-2026 : Arsiela - 05-28-2026 03:43 PM
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        int lnCtr = 0;
        if (MiscUtil.RecordCount(loRS) >= 0) {
            paPOMaster = new ArrayList<>();
            while (loRS.next()) {
                paPOMaster.add(POMasterList());
                paPOMaster.get(paPOMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
                lnCtr++;
            }
            System.out.println("Records found: " + lnCtr);
            loJSON.put("result", "success");
            loJSON.put("message", "Record loaded successfully.");
        } else {
            paPOMaster = new ArrayList<>();
            paPOMaster.add(POMasterList());
            loJSON.put("result", "error");
            loJSON.put("continue", true);
            loJSON.put("message", "No record found .");
        }
        MiscUtil.close(loRS);
        return loJSON;
    }

    public JSONObject getConfirmedPurchaseOrder(String fsSupplierID, String fsReferID) throws SQLException, GuanzonException {
        JSONObject loJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsTransStat = " AND ( a.cTranStat IN (" + lsTransStat.substring(2) + ")"
                    + " OR (ASCII(a.cTranStat) - 64) = " + SQLUtil.toSQL(PurchaseOrderStatus.APPROVED) + " ) ";
        } else {
            lsTransStat = " AND ( a.cTranStat = " + SQLUtil.toSQL(psTranStat)
                    + " OR (ASCII(a.cTranStat) - 64) = " + SQLUtil.toSQL(PurchaseOrderStatus.APPROVED) + " ) ";
        }

        String lsSQL = " SELECT "
                + "  a.sTransNox,"
                + "  c.sBranchNm,"
                + "  a.sBranchCd,"
                + "  a.dTransact,"
                + "  a.sTransNox,"
                + "  a.cTranStat, "
                + "  e.sCompnyNm "
                + " FROM PO_Master a "
                + " LEFT JOIN PO_Detail b ON b.sTransNox = a.sTransNox"
                + " LEFT JOIN Branch c ON a.sBranchCd = c.sBranchCd"
                + " LEFT JOIN Industry d ON a.sIndstCdx = d.sIndstCdx"
                + " LEFT JOIN Client_Master e ON e.sClientID = a.sSupplier ";
        String lsFilterCondition = String.join(" AND ",
                " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()),
                " a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID()),
                " a.sSupplier LIKE " + SQLUtil.toSQL("%" + fsSupplierID),
                " a.sCategrCd = " + SQLUtil.toSQL(Master().getCategoryCode()),
                " a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsReferID));
        lsSQL = MiscUtil.addCondition(lsSQL, lsFilterCondition);
        if (!psTranStat.isEmpty()) {
            lsSQL = lsSQL + lsTransStat;
        }
        //FIX: was ||
        if (!poGRider.isMainOffice() && !poGRider.isWarehouse()) {
            lsSQL = lsSQL + " AND a.sBranchCd LIKE " + SQLUtil.toSQL(poGRider.getBranchCode());
        }
        lsSQL = lsSQL + " GROUP BY  a.sTransNox"
                + " ORDER BY a.dTransact,e.sCompnyNm,a.sTransNox ASC"; //sort data into ascending according to ma'am she 05-28-2026 : Arsiela - 05-28-2026 03:43 PM
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        int lnCtr = 0;
        if (MiscUtil.RecordCount(loRS) >= 0) {
            paPOMaster = new ArrayList<>();
            while (loRS.next()) {
                paPOMaster.add(POMasterList());
                paPOMaster.get(paPOMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
                lnCtr++;
            }
            System.out.println("Records found: " + lnCtr);
            loJSON.put("result", "success");
            loJSON.put("message", "Record loaded successfully.");
        } else {
            paPOMaster = new ArrayList<>();
            paPOMaster.add(POMasterList());
            loJSON.put("result", "error");
            loJSON.put("continue", true);
            loJSON.put("message", "No record found .");
        }
        MiscUtil.close(loRS);
        return loJSON;
    }

    public String getInventoryTypeCode() throws SQLException {
        String lsSQL = "SELECT a.sInvTypCd FROM Category a"
                + " LEFT JOIN Inv_Type b ON a.sInvTypCd = b.sInvTypCd";
        //FIX: the AND clause was inside the quoted industry value
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID())
                + " AND a.sCategrCd = " + SQLUtil.toSQL(Master().getCategoryCode()));

        ResultSet loRS = poGRider.executeQuery(lsSQL);
        String inventoryTypeCode = null;

        if (loRS.next()) {
            inventoryTypeCode = loRS.getString("sInvTypCd");
        }

        MiscUtil.close(loRS);
        return inventoryTypeCode;
    }

    // =====================================================================
    // Discounts, advance payment and totals.
    // Rules live in the validator; on a failed rule the field is reset to 0.
    // =====================================================================

    private static double parseAmount(String fsValue) {
        if (fsValue == null || fsValue.trim().isEmpty()) {
            return PurchaseOrderStaticData.default_value_double;
        }
        return Double.parseDouble(fsValue.replace(",", "").trim());
    }

    /** Applies a value after its rule passed, then checks the resulting net total. */
    private JSONObject applyAmount(JSONObject foRule, Runnable foApply, Runnable foReset) {
        if (!PurchaseOrderValidator.isSuccess(foRule)) {
            foReset.run();
            computeNetTotal();
            poJSON = foRule;
            return poJSON;
        }

        foApply.run();
        computeNetTotal();

        poJSON = rules().validateNetTotal();
        if (!PurchaseOrderValidator.isSuccess(poJSON)) {
            foReset.run();
            computeNetTotal();
        }
        return poJSON;
    }

    public JSONObject setDiscountRate(String fsValue) {
        double lnRate = parseAmount(fsValue);
        return applyAmount(rules().validateDiscountRate(lnRate),
                () -> Master().setDiscount(lnRate),
                () -> Master().setDiscount(PurchaseOrderStaticData.default_value_double));
    }

    public JSONObject setDiscountAmount(String fsValue) {
        double lnAmount = parseAmount(fsValue);
        return applyAmount(rules().validateDiscountAmount(lnAmount),
                () -> Master().setAdditionalDiscount(lnAmount),
                () -> Master().setAdditionalDiscount(PurchaseOrderStaticData.default_value_double));
    }

    public JSONObject setAdvancePaymentRate(String fsValue) {
        double lnRate = parseAmount(fsValue);
        return applyAmount(rules().validateAdvancePaymentRate(lnRate),
                () -> Master().setDownPaymentRatesPercentage(lnRate),
                () -> Master().setDownPaymentRatesPercentage(PurchaseOrderStaticData.default_value_double));
    }

    public JSONObject setAdvancePaymentAmount(String fsValue) {
        double lnAmount = parseAmount(fsValue);
        return applyAmount(rules().validateAdvancePaymentAmount(lnAmount),
                () -> Master().setDownPaymentRatesAmount(lnAmount),
                () -> Master().setDownPaymentRatesAmount(PurchaseOrderStaticData.default_value_double));
    }

    public void computeNetTotal() {
        double totalAmount = Master().getTranTotal().doubleValue(); // Total transaction amount
        double discountRate = Master().getDiscount().doubleValue() / 100;
        double discountRateWithTotalAmount = totalAmount * discountRate;
        double additionalDiscountAmount = Master().getAdditionalDiscount().doubleValue();

        // Amount after all discounts
        double totalDiscount = (discountRateWithTotalAmount + additionalDiscountAmount);

        double netTotal = totalAmount - totalDiscount;
        Master().setNetTotal(netTotal);

    }

    public JSONObject netTotalChecker(int pnRow) {
        double lnLineAmount = Detail(pnRow).getQuantity().doubleValue() * Detail(pnRow).getUnitPrice().doubleValue();
        poJSON = rules().validateNetTotalWithLine(lnLineAmount);
        if (!PurchaseOrderValidator.isSuccess(poJSON)) {
            Detail(pnRow).setQuantity(0);
            computeNetTotal();
        }
        return poJSON;
    }

    public List<String> getApprover() throws SQLException {
        List<String> lsList = new ArrayList<String>();
        String lsSQL = " SELECT "
                + "     a.sSourceCD "
                + " ,	a.sSourceNo "
                + " ,	a.cTranStat "
                + " ,	d.sCompnyNm "
                + " ,	c.dApproved "
                + " ,   CONCAT(c.sEmployID, ' - ',d.sCompnyNm,' ',c.dApproved) AS sApprover "
                + " FROM Transaction_Authorization_Master a  "
                + " ,	Transaction_Authorization_Detail b     "
                + " ,	Transaction_Authorization_Recipient c  "
                + " LEFT JOIN Client_Master d ON c.sEmployID = d.sClientID "
                + " WHERE a.sTransNox = b.sSourceNo AND b.sTransNox = c.sSourceNo    "
                + " AND a.sSourceCD = " + SQLUtil.toSQL(getSourceCode())
                + " AND a.sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo())
                + " AND a.cTranStat = '2' "
                + " AND c.cTranStat = '1' ";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (MiscUtil.RecordCount(loRS) >= 0) {
            while (loRS.next()) {
                lsList.add(loRS.getString("sApprover"));
            }
            MiscUtil.close(loRS);
        }

        return lsList;
    }

    public String getSysUser(String fsId) throws SQLException, GuanzonException {
        String lsEntry = "";
        String lsSQL = " SELECT b.sCompnyNm from xxxSysUser a "
                + " LEFT JOIN Client_Master b ON b.sClientID = a.sEmployNo ";
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sUserIDxx =  " + SQLUtil.toSQL(fsId));
        System.out.println("SQL " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0L) {
                if (loRS.next()) {
                    lsEntry = loRS.getString("sCompnyNm");
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
        return lsEntry;
    }

    public JSONObject getEntryBy() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsEntry = "";
        String lsEntryDate = "";
        String lsSQL = " SELECT b.sModified, b.dModified "
                + " FROM PO_Master a "
                + " LEFT JOIN xxxAuditLogMaster b ON b.sSourceNo = a.sTransNox AND b.sEventNme LIKE 'ADD%NEW' AND b.sRemarksx = " + SQLUtil.toSQL(Master().getTable());
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox =  " + SQLUtil.toSQL(Master().getTransactionNo()));
        System.out.println("Execute SQL : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0L) {
                if (loRS.next()) {
                    if (loRS.getString("sModified") != null && !"".equals(loRS.getString("sModified"))) {
                        if (loRS.getString("sModified").length() > 10) {
                            lsEntry = getSysUser(poGRider.Decrypt(loRS.getString("sModified")));
                        } else {
                            lsEntry = getSysUser(loRS.getString("sModified"));
                        }
                        // Get the LocalDateTime from your result set
                        LocalDateTime dModified = loRS.getObject("dModified", LocalDateTime.class);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
                        lsEntryDate = dModified.format(formatter);
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            return errorJSON(e.getMessage());
        }

        poJSON = successJSON();
        poJSON.put("sCompnyNm", lsEntry);
        poJSON.put("sEntryDte", lsEntryDate);
        return poJSON;
    }

    public JSONObject getConfirmedBy() throws SQLException, GuanzonException {
        String lsConfirm = "";
        String lsDate = "";
        String lsSQL = "SELECT b.sModified,b.dModified FROM PO_Master a "
                + " LEFT JOIN Transaction_Status_History b ON b.sSourceNo = a.sTransNox AND b.sTableNme = 'PO_Master' "
                + " AND ( b.cRefrStat = " + SQLUtil.toSQL(PurchaseOrderStatus.CONFIRMED)
                + " OR (ASCII(b.cRefrStat) - 64)  = " + SQLUtil.toSQL(PurchaseOrderStatus.CONFIRMED) + " )";
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox = " + SQLUtil.toSQL(Master().getTransactionNo()));
        System.out.println("Execute SQL : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0L) {
                if (loRS.next()) {
                    if (loRS.getString("sModified") != null && !"".equals(loRS.getString("sModified"))) {
                        if (loRS.getString("sModified").length() > 10) {
                            lsConfirm = getSysUser(poGRider.Decrypt(loRS.getString("sModified")));
                        } else {
                            lsConfirm = getSysUser(loRS.getString("sModified"));
                        }
                        // Get the LocalDateTime from your result set
                        LocalDateTime dModified = loRS.getObject("dModified", LocalDateTime.class);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
                        lsDate = dModified.format(formatter);
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            return errorJSON(e.getMessage());
        }

        poJSON = successJSON();
        poJSON.put("sConfirmed", lsConfirm);
        poJSON.put("sConfrmDte", lsDate);
        return poJSON;
    }

    public void ShowApprovalHistory() throws SQLException, GuanzonException, Exception {
        CachedRowSet crs = getApprovalHistory();

        JSONObject loJSON = MiscUtil.getEntryBy(poGRider, poMaster.getTable(), (String) poMaster.getValue("sTransNox"));

        String entryBy = "";
        String entryDate = "";

        if ("success".equals((String) loJSON.get("result"))) {
            entryBy = (String) loJSON.get("sCompnyNm");
            entryDate = (String) loJSON.get("sEntryDte");
        }

        showApprovalHistoryUI("Purchase Order", (String) poMaster.getValue("sTransNox"), entryBy, entryDate, Double.parseDouble(poMaster.getValue("nTranTotl").toString()), crs);
    }

    public void ShowStatusHistory() throws SQLException, GuanzonException, Exception {
        CachedRowSet crs = getStatusHistory();

        crs.beforeFirst();

        while (crs.next()) {
            switch (crs.getString("cRefrStat")) {
                case "":
                    crs.updateString("cRefrStat", "-");
                    break;
                case PurchaseOrderStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case PurchaseOrderStatus.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case PurchaseOrderStatus.PROCESSED:
                    crs.updateString("cRefrStat", "PROCESSED");
                    break;
                case PurchaseOrderStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case PurchaseOrderStatus.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;
                case PurchaseOrderStatus.APPROVED:
                    crs.updateString("cRefrStat", "APPROVED");
                    break;
                case PurchaseOrderStatus.POSTED:
                    crs.updateString("cRefrStat", "POSTED");
                    break;
                case PurchaseOrderStatus.RETURNED:
                    crs.updateString("cRefrStat", "RETURNED");
                    break;
                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);

                    switch (stat) {
                        case PurchaseOrderStatus.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case PurchaseOrderStatus.CONFIRMED:
                            crs.updateString("cRefrStat", "CONFIRMED");
                            break;
                        case PurchaseOrderStatus.PROCESSED:
                            crs.updateString("cRefrStat", "PROCESSED");
                            break;
                        case PurchaseOrderStatus.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case PurchaseOrderStatus.VOID:
                            crs.updateString("cRefrStat", "VOID");
                            break;
                        case PurchaseOrderStatus.APPROVED:
                            crs.updateString("cRefrStat", "APRVL REQUESTD");
                            break;
                        case PurchaseOrderStatus.POSTED:
                            crs.updateString("cRefrStat", "POSTED");
                            break;
                        case PurchaseOrderStatus.RETURNED:
                            crs.updateString("cRefrStat", "RETURNED");
                            break;
                    }
            }
            crs.updateRow();
        }

        JSONObject loJSON = getEntryBy();
        String entryBy = "";
        String entryDate = "";

        if ("success".equals((String) loJSON.get("result"))) {
            entryBy = (String) loJSON.get("sCompnyNm");
            entryDate = (String) loJSON.get("sEntryDte");
        }

        showStatusHistoryUI("Purchase Order", (String) poMaster.getValue("sTransNox"), entryBy, entryDate, crs);
    }

    public JSONObject printTransaction(String jasperType) {
        poJSON = new JSONObject();
        String watermarkPath = System.getProperty("sys.default.path.config") + "/reports/images/" + "draft.png"; //set draft as default
        try {

            String lsCompanyAddress = "";
            if (Master().Company().getCompanyAddress() != null && !"".equals(Master().Company().getCompanyAddress())) {
                lsCompanyAddress = Master().Company().getCompanyAddress().trim();
            }
            if (Master().Company().TownCity().getDescription() != null && !"".equals(Master().Company().TownCity().getDescription())) {
                lsCompanyAddress = lsCompanyAddress + " " + Master().Company().TownCity().getDescription().trim();
            }
            if (Master().Company().TownCity().Province().getDescription() != null && !"".equals(Master().Company().TownCity().Province().getDescription())) {
                lsCompanyAddress = lsCompanyAddress + ", " + Master().Company().TownCity().Province().getDescription().trim();
            }

            //Branch / Destination
            String lsDestinationAddress = "";
            if (Master().Branch().getAddress() != null && !"".equals(Master().Branch().getAddress())) {
                lsDestinationAddress = Master().Branch().getAddress().trim();
            }
            if (Master().Branch().TownCity().getDescription() != null && !"".equals(Master().Branch().TownCity().getDescription())) {
                lsDestinationAddress = lsDestinationAddress + " " + Master().Branch().TownCity().getDescription().trim();
            }
            if (Master().Branch().TownCity().Province().getDescription() != null && !"".equals(Master().Branch().TownCity().Province().getDescription())) {
                lsDestinationAddress = lsDestinationAddress + ", " + Master().Branch().TownCity().Province().getDescription().trim();
            }

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("sCompnyNm", "");
            parameters.put("sConfirmed", "");

            JSONObject loJSONEntry = getEntryBy();
            if ("error".equals((String) loJSONEntry.get("result"))) {
                return loJSONEntry;
            }

            if ((String) loJSONEntry.get("sCompnyNm") != null && !"".equals((String) loJSONEntry.get("sCompnyNm"))) {
                parameters.put("sCompnyNm", "Prepared by: " + (String) loJSONEntry.get("sCompnyNm") + " " + String.valueOf((String) loJSONEntry.get("sEntryDte")));
            }

            JSONObject loJSONConfirm = getConfirmedBy();
            if ("error".equals((String) loJSONConfirm.get("result"))) {
                return loJSONConfirm;
            } else {
                if ((String) loJSONConfirm.get("sConfirmed") != null && !"".equals((String) loJSONConfirm.get("sConfirmed"))) {
                    parameters.put("sConfirmed", "Confirmed by: " + (String) loJSONConfirm.get("sConfirmed") + " " + String.valueOf((String) loJSONConfirm.get("sConfrmDte")));
                }
            }

            parameters.put("sBranchNm", poGRider.getBranchName());
            parameters.put("sAddressx", lsCompanyAddress);
            parameters.put("sTransNox", Master().getTransactionNo());
            parameters.put("sDestination", Master().Branch().getBranchName());
            parameters.put("sCompany", Master().Company().getCompanyName());
            parameters.put("sSupplier", Master().Supplier().getCompanyName());
            parameters.put("sTerm", Master().Term().getDescription());
            parameters.put("sDestinationAddress", lsDestinationAddress);
            //set default value
            parameters.put("sApprval1", "");
            parameters.put("sApprval2", "");
            parameters.put("sApprval3", "");
            //Update value when approved
            if (Master().getTransactionStatus().equals(PurchaseOrderStatus.APPROVED)) {
                List<String> lsList = getApprover();
                for (int lnCtr = 0; lnCtr <= lsList.size() - 1; lnCtr++) {
                    parameters.put("sApprval" + (lnCtr + 1), lsList.get(lnCtr));
                }
            }

            parameters.put("sRemarks", Master().getRemarks());
            parameters.put("dTransDte", new java.sql.Date(Master().getTransactionDate().getTime()));
            parameters.put("dDatexxx", new java.sql.Date(poGRider.getServerDate().getTime()));
            parameters.put("nTtlAmntx", Master().getTranTotal().doubleValue());
            parameters.put("nDiscRate", Master().getDiscount().doubleValue());
            parameters.put("nDiscAmnt", Master().getAdditionalDiscount().doubleValue());
            parameters.put("nAdvAmntx", Master().getDownPaymentRatesAmount().doubleValue());
            parameters.put("nAdvRatex", Master().getDownPaymentRatesPercentage().doubleValue());
            parameters.put("nNetAmntx", Master().getNetTotal().doubleValue());

            switch (Master().getTransactionStatus()) {
                case PurchaseOrderStatus.POSTED:
                case PurchaseOrderStatus.APPROVED:
                    if ("1".equals(Master().getPrint())) {
                        watermarkPath = System.getProperty("sys.default.path.config") + "/reports/images/" + "approvedreprint.png";
                    } else {
                        watermarkPath = System.getProperty("sys.default.path.config") + "/reports/images/" + "approved.png";
                    }
                    break;
            }
            parameters.put("watermarkImagePath", watermarkPath);
            List<OrderDetail> orderDetails = new ArrayList<>();

            double lnTotal = 0.0000;
            String lsFullDesc = "";
            for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
                lnTotal = Detail(lnCtr).getUnitPrice().doubleValue() * Detail(lnCtr).getQuantity().doubleValue();
                //description per industry/template (overridable)
                lsFullDesc = buildPrintDescription(lnCtr, jasperType);

                orderDetails.add(new OrderDetail(lnCtr + 1, // Line No.
                        safeString(Detail(lnCtr).getSouceNo()), // Source No
                        safeString(Detail(lnCtr).Inventory().getBarCode()), // Barcode
                        safeString(Detail(lnCtr).Inventory().Brand().getDescription()), // Brand
                        safeString(Detail(lnCtr).Inventory().Variant().getDescription()) + " "
                        + safeString(Detail(lnCtr).Inventory().Variant().getYearModel()), // Variant + Year Model
                        safeString(Detail(lnCtr).Inventory().Model().getDescription()), // Model
                        safeString(Detail(lnCtr).Inventory().Measure().getDescription()),// Unit of Measure
                        safeString(Detail(lnCtr).Inventory().Color().getDescription()), // Color
                        lsFullDesc, // Item Description
                        Detail(lnCtr).getUnitPrice() != null
                        ? Detail(lnCtr).getUnitPrice().doubleValue() : 0.00, // Unit Price
                        Detail(lnCtr).getQuantity() != null
                        ? Detail(lnCtr).getQuantity().doubleValue() : 0.00, // Quantity
                        lnTotal // Line Total
                ));
            }

            // 3. Create data source
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(orderDetails);

            // 4. Compile and fill report
            //template per industry (overridable)
            String jrxmlPath = getPrintTemplatePath(jasperType);
            if (jrxmlPath == null) {
                return errorJSON("Unknown print template: " + jasperType);
            }

            JasperReport jasperReport;

            jasperReport = JasperCompileManager.compileReport(jrxmlPath);

            JasperPrint jasperPrint;
            jasperPrint = JasperFillManager.fillReport(
                    jasperReport,
                    parameters,
                    dataSource
            );

            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                CustomJasperViewer viewer = new CustomJasperViewer(jasperPrint);
                viewer.setVisible(true);
            } else {
                //mac 2026.02.21
                //export pdf file
                JasperExportManager.exportReportToPdfFile(jasperPrint, System.getProperty("sys.default.path.config") + "/temp/" + Master().getTransactionNo() + ".pdf");
            }

            poJSON.put("result", "success");
        } catch (JRException | SQLException | GuanzonException ex) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction print aborted!");
            Logger
                    .getLogger(PurchaseOrderMain.class
                            .getName()).log(Level.SEVERE, null, ex);
        }
        return poJSON;

    }

    private String safeString(Object value) {
        return value == null ? "" : value.toString();
    }

    public static class OrderDetail {

        private Integer nRowNo;
        private String sOrderNo;
        private String sBarcode;
        private String sBrandName;
        private String sVariant;
        private String sModel;
        private String sMeasure;
        private String sColor;
        private String sDescription;
        private double nUprice;
        private double nOrder;
        private double nTotal;
        private double nDiscRate;
        private double nDiscAmt;
        private double nAdvRate;
        private double nAdvAmt;
        private double nNetAmt;

        public OrderDetail(Integer rowNo, String orderNo, String barcode, String BrandName, String Variant, String Model, String Measure, String Color, String description,
                double uprice, double order, double nTotal) {
            this.nRowNo = rowNo;
            this.sOrderNo = orderNo;
            this.sBarcode = barcode;
            this.sBrandName = BrandName;
            this.sVariant = Variant;
            this.sModel = Model;
            this.sMeasure = Measure;
            this.sColor = Color;
            this.sDescription = description;
            this.nUprice = uprice;
            this.nOrder = order;
            this.nTotal = nTotal;
        }

        public Integer getnRowNo() {
            return nRowNo;
        }

        public String getsOrderNo() {
            return sOrderNo;
        }

        public String getsBarcode() {
            return sBarcode;
        }

        public String getsBrandName() {
            return sBrandName;
        }

        public String getsVariant() {
            return sVariant;
        }

        public String getsModel() {
            return sModel;
        }

        public String getsMeasure() {
            return sMeasure;
        }

        public String getsColor() {
            return sColor;
        }

        public String getsDescription() {
            return sDescription;
        }

        public double getnUprice() {
            return nUprice;
        }

        public double getnOrder() {
            return nOrder;
        }

        public double getnTotal() {
            return nTotal;
        }

        public double getnDiscRate() {
            return nDiscRate;
        }

        public double getnDiscAmt() {
            return nDiscAmt;
        }

        public double getnAdvRate() {
            return nAdvRate;
        }

        public double getnAdvAmt() {
            return nAdvAmt;
        }

        public double getnNetAmt() {
            return nNetAmt;
        }

    }

    public class CustomJasperViewer extends JasperViewer {

        public CustomJasperViewer(JasperPrint jasperPrint) {
            super(jasperPrint, false);
            customizePrintButton(jasperPrint);
        }

        private void customizePrintButton(JasperPrint jasperPrint) {
            poJSON = new JSONObject();
            try {
                JRViewer viewer = findJRViewer(this);
                if (viewer == null) {
                    System.out.println("JRViewer not found!");
                    return;
                }

                for (int i = 0; i < viewer.getComponentCount(); i++) {
                    if (viewer.getComponent(i) instanceof JRViewerToolbar) {
                        JRViewerToolbar toolbar = (JRViewerToolbar) viewer.getComponent(i);

                        for (int j = 0; j < toolbar.getComponentCount(); j++) {
                            if (toolbar.getComponent(j) instanceof JButton) {
                                JButton button = (JButton) toolbar.getComponent(j);

                                if ("Print".equals(button.getToolTipText())) {
                                    for (ActionListener al : button.getActionListeners()) {
                                        button.removeActionListener(al);
                                    }
                                    button.addActionListener(e -> {
                                        try {
                                            boolean isPrinted = JasperPrintManager.printReport(jasperPrint, true);
                                            if (isPrinted) {
                                                PrintTransaction(true);
                                            } else {
                                                Platform.runLater(() -> {
                                                    ShowMessageFX.Warning("Printing was canceled by the user.", "Print Purchase Order", null);
                                                    SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());

                                                });
                                            }
                                        } catch (JRException ex) {
                                            Platform.runLater(() -> {
                                                ShowMessageFX.Warning("Print Failed: " + ex.getMessage(), "Computerized Accounting System", null);
                                                SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                                            });
                                        } catch (SQLException | GuanzonException | CloneNotSupportedException ex) {
                                            Logger.getLogger(PurchaseOrderMain.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    });
                                }
                            }
                        }

                        // Force UI refresh after hiding the button
                        toolbar.revalidate();
                        toolbar.repaint();
                    }
                }
            } catch (Exception e) {
                System.out.println("Error customizing print button: " + e.getMessage());
            }
        }

        private void PrintTransaction(boolean fbIsPrinted) throws SQLException, CloneNotSupportedException, GuanzonException {
            poJSON = new JSONObject();
            if (fbIsPrinted) {
                if (((String) poMaster.getValue("cTranStat")).equals(PurchaseOrderStatus.APPROVED)) {
                    poJSON = OpenTransaction((String) poMaster.getValue("sTransNox"));
                    if ("error".equals((String) poJSON.get("result"))) {
                        Platform.runLater(() -> {
                            ShowMessageFX.Warning((String) poJSON.get("message"), "Print Purchase Order", null);
                            SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                        });
                        fbIsPrinted = false;
                    }
                    poJSON = UpdateTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        Platform.runLater(() -> {
                            ShowMessageFX.Warning((String) poJSON.get("message"), "Print Purchase Order", null);
                            SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                        });
                        fbIsPrinted = false;
                    }

                    poMaster.setValue("dModified", poGRider.getServerDate());
                    poMaster.setValue("sModified", poGRider.getUserID());
                    poMaster.setValue("cPrintxxx", Logical.YES);

                    poJSON = SaveTransaction();
                    if ("error".equals((String) poJSON.get("result"))) {
                        Platform.runLater(() -> {
                            ShowMessageFX.Warning((String) poJSON.get("message"), "Print Purchase Order", null);
                            SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                        });
                        fbIsPrinted = false;
                    }
                }
            }

            if (fbIsPrinted) {
                Platform.runLater(() -> {
                    ShowMessageFX.Information("Transaction printed successfully.", "Print Purchase Order", null);
                    SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                });
            } else {
                Platform.runLater(() -> {
                    ShowMessageFX.Information("Transaction printed aborted.", "Print Purchase Order", null);
                    SwingUtilities.invokeLater(() -> CustomJasperViewer.this.toFront());
                });
            }
        }

        private JRViewer findJRViewer(Component parent) {
            if (parent instanceof JRViewer) {
                return (JRViewer) parent;
            }
            if (parent instanceof Container) {
                Component[] components = ((Container) parent).getComponents();
                for (Component component : components) {
                    JRViewer viewer = findJRViewer(component);
                    if (viewer != null) {
                        return viewer;
                    }
                }
            }
            return null;
        }
    }

    public JSONObject RetriveSummaryReports(Boolean issummarized,
            LocalDate dateFrom,
            LocalDate dateThru,
            String Branch,
            String Destination,
            String Supplier,
            String Category)
            throws SQLException, GuanzonException {

        poJSON = new JSONObject();
        String lsTransStat = "";
        dfrom = String.valueOf(dateFrom);
        dthru = String.valueOf(dateThru);
        try {

            String lsSQL = "SELECT DISTINCT "
                    + "  g.sCompnyNm AS Supplier, "
                    + "  c.sBranchNm AS Destination, "
                    + "  d.sBranchNm AS Branch, "
                    + "  a.sTransNox, "
                    + "  a.sReferNox, "
                    + "  a.dTransact, "
                    + "  e.sDescript AS Category, "
                    + "  f.sDescript AS Term, "
                    + "  a.nTranTotl, "
                    + "  a.cTranStat "
                    + " FROM PO_Master a "
                    + " LEFT JOIN AP_Client_Master b ON a.sSupplier = b.sClientID "
                    + " LEFT JOIN Branch c ON a.sDestinat = c.sBranchCd "
                    + " LEFT JOIN Branch d ON a.sBranchCd = d.sBranchCd "
                    + " LEFT JOIN Category e ON a.sCategrCd = e.sCategrCd "
                    + " LEFT JOIN Term f ON a.sTermCode = f.sTermCode "
                    + " LEFT JOIN Client_Master g ON b.sClientID = g.sClientID ";

            // -------------------------------
            // FILTERS
            // -------------------------------
            List<String> lsFilter = new ArrayList<>();

            if (dateFrom != null && dateThru != null) {
                lsFilter.add("a.dTransact BETWEEN "
                        + SQLUtil.toSQL(java.sql.Date.valueOf(dateFrom))
                        + " AND "
                        + SQLUtil.toSQL(java.sql.Date.valueOf(dateThru)));
                dfrom = String.valueOf(dateFrom);
                dthru = String.valueOf(dateThru);
            }
            
            if (Branch != null && !Branch.trim().isEmpty()) {
                lsFilter.add("a.sBranchCd = " + SQLUtil.toSQL(Branch));
            }

            if (Destination != null && !Destination.trim().isEmpty()) {
                lsFilter.add("a.sDestinat = " + SQLUtil.toSQL(Destination));
            }

            if (Supplier != null && !Supplier.trim().isEmpty()) {
                lsFilter.add("a.sSupplier = " + SQLUtil.toSQL(Supplier));
            }

            if (Category != null && !Category.trim().isEmpty()) {
                lsFilter.add("a.sCategrCd = " + SQLUtil.toSQL(Category));
            }
            
            lsFilter.add("a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID())
                    + " AND a.sIndstCdx = " +  SQLUtil.toSQL(Master().getIndustryID()));
            if(poGRider.getUserLevel()<=UserRight.ENCODER){
                lsFilter.add("a.sTransNox LIKE " + SQLUtil.toSQL(poGRider.getBranchCode() + "%"));
            }

            if (psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                    lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                }
                lsFilter.add( " a.cTranStat IN (" + lsTransStat.substring(2) + ")");
            } else {
                lsFilter.add( " a.cTranStat = " + SQLUtil.toSQL(psTranStat));
            }
            if (!lsFilter.isEmpty()) {
                lsSQL += " WHERE " + String.join(" AND ", lsFilter);
            }

            lsSQL += " ORDER BY a.dTransact,a.sTransNox ASC";

            System.out.println("Executing SQL: " + lsSQL);

            ResultSet loRS = poGRider.executeQuery(lsSQL);

            if (loRS == null) {
                return errorJSON("Query execution failed.");
            }

            int lnctr = 0;
            JSONArray dataArray = new JSONArray();

            while (loRS.next()) {

                JSONObject record = new JSONObject();

                record.put("Supplier", loRS.getString("Supplier"));
                record.put("Destination", loRS.getString("Destination"));
                record.put("Branch", loRS.getString("Branch"));
                record.put("sTransNox", loRS.getString("sTransNox"));
                record.put("sReferNox", loRS.getString("sReferNox"));
                record.put("dTransact", loRS.getDate("dTransact"));
                record.put("Category", loRS.getString("Category"));
                record.put("Term", loRS.getString("Term"));

                String tranStat = loRS.getString("cTranStat");
                switch (tranStat) {
                    case "0":
                        record.put("cTranStat", "OPEN");
                        break;
                    case "1":
                        record.put("cTranStat", "CONFIRMED");
                        break;
                    case "2":
                        record.put("cTranStat", "PROCESSED");
                        break;
                    case "3":
                        record.put("cTranStat", "CANCELLED");
                        break;
                    case "4":
                        record.put("cTranStat", "VOID");
                        break;
                    case "5":
                        record.put("cTranStat", "APPROVED");
                        break;
                    case "6":
                        record.put("cTranStat", "POSTED");
                        break;
                    case "9":
                        record.put("cTranStat", "RETURNED");
                        break;
                    case "E":
                        record.put("cTranStat", "APPROVED+");
                        break;

                    default:
                        record.put("cTranStat", tranStat);
                }
                record.put("Total", loRS.getDouble("nTranTotl"));

                dataArray.add(record);
                lnctr++;
            }

            MiscUtil.close(loRS);

            if (lnctr > 0) {
                poJSON.put("result", "success");
                poJSON.put("message", "Record(s) loaded successfully.");
                poJSON.put("data", dataArray);
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "No records found.");
                poJSON.put("data", new JSONArray());
            }

        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }

        return poJSON;
    }
    
    public JSONObject RetriveSummaryDetailedReports(Boolean issummarized,
            LocalDate dateFrom,
            LocalDate dateThru,
            String Branch,
            String Destination,
            String Supplier,
            String Category)
            throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        try {

            String lsSQL = "SELECT DISTINCT "
                    + "  h.sCompnyNm AS Supplier, "
                    + "  d.sBranchNm AS Destination, "
                    + "  b.sTransNox, "
                    + "  b.dTransact, "
                    + "  j.sDescript AS Brand, "
                    + "  k.sModelCde AS ModelCode, "
                    + "  k.sDescript AS ModelName, "
                    + "  l.sDescript AS Color, "
                    + "  a.nQuantity, "
                    + "  a.nReceived, "
                    + "  a.nCancelld, "
                    + "  i.nUnitPrce, "
                    + "  i.sBarCodex, "
                    + "  i.sDescript AS Description, "
                    + "  ROUND(a.nQuantity * i.nUnitPrce, 4) AS Total "
                    + " FROM PO_Detail a "
                    + " LEFT JOIN PO_Master b ON a.sTransNox = b.sTransNox "
                    + " LEFT JOIN AP_Client_Master c ON b.sSupplier = c.sClientID "
                    + " LEFT JOIN Branch d ON b.sDestinat = d.sBranchCd "
                    + " LEFT JOIN Branch e ON b.sBranchCd = e.sBranchCd "
                    + " LEFT JOIN Category f ON b.sCategrCd = f.sCategrCd "
                    + " LEFT JOIN Term g ON b.sTermCode = g.sTermCode "
                    + " LEFT JOIN Client_Master h ON c.sClientID = h.sClientID "
                    + " LEFT JOIN Inventory i ON a.sStockIDx = i.sStockIDx "
                    + " LEFT JOIN Brand j ON i.sBrandIDx = j.sBrandIDx "
                    + " LEFT JOIN Model k ON i.sModelIDx = k.sModelIDx "
                    + " LEFT JOIN Color l ON i.sColorIDx = l.sColorIDx ";
            
            // -------------------------------
            // FILTERS
            // -------------------------------
            List<String> lsFilter = new ArrayList<>();

            if (dateFrom != null && dateThru != null) {
                lsFilter.add("b.dTransact BETWEEN "
                        + SQLUtil.toSQL(java.sql.Date.valueOf(dateFrom))
                        + " AND "
                        + SQLUtil.toSQL(java.sql.Date.valueOf(dateThru)));
                dfrom = String.valueOf(dateFrom);
                dthru = String.valueOf(dateThru);
            }
            if (Branch != null && !Branch.trim().isEmpty()) {
                lsFilter.add("b.sBranchCd = " + SQLUtil.toSQL(Branch));
            }

            if (Destination != null && !Destination.trim().isEmpty()) {
                lsFilter.add("d.sBranchCd = " + SQLUtil.toSQL(Destination));
            }

            if (Supplier != null && !Supplier.trim().isEmpty()) {
                lsFilter.add("b.sSupplier = " + SQLUtil.toSQL(Supplier));
            }

            if (Category != null && !Category.trim().isEmpty()) {
                lsFilter.add("b.sCategrCd = " + SQLUtil.toSQL(Category));
            }
            
            lsFilter.add("b.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID()) 
                    + " AND b.sIndstCdx = " +  SQLUtil.toSQL(Master().getIndustryID()));
            if(poGRider.getUserLevel()<=UserRight.ENCODER){
                lsFilter.add("a.sTransNox LIKE " + SQLUtil.toSQL(poGRider.getBranchCode() + "%"));
            }
            if (psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                    lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                }
                lsFilter.add( " b.cTranStat IN (" + lsTransStat.substring(2) + ")");
            } else {
                lsFilter.add( " b.cTranStat = " + SQLUtil.toSQL(psTranStat));
            }
            
            if (!lsFilter.isEmpty()) {
                lsSQL += " WHERE " + String.join(" AND ", lsFilter);
            }

            lsSQL += " ORDER BY b.dTransact, a.sTransNox, a.nEntryNox ASC";

            System.out.println("Executing SQL: " + lsSQL);

            ResultSet loRS = poGRider.executeQuery(lsSQL);

            if (loRS == null) {
                return errorJSON("Query execution failed.");
            }

            int lnctr = 0;
            JSONArray dataArray = new JSONArray();

            while (loRS.next()) {

                JSONObject record = new JSONObject();

                record.put("Supplier", loRS.getString("Supplier"));
                record.put("Destination", loRS.getString("Destination"));
                record.put("sTransNox", loRS.getString("sTransNox"));
                record.put("dTransact", loRS.getDate("dTransact"));
                record.put("sBarCodex", loRS.getString("sBarCodex"));
                record.put("Description", loRS.getString("Description"));
                record.put("Brand", loRS.getString("Brand"));
                record.put("ModelCode", loRS.getString("ModelCode"));
                record.put("ModelName", loRS.getString("ModelName"));
                record.put("Color", loRS.getString("Color"));
                record.put("Quantity", loRS.getDouble("nQuantity"));
                record.put("nReceived", loRS.getDouble("nReceived"));
                record.put("nCancelld", loRS.getDouble("nCancelld"));
                record.put("UnitPrice", loRS.getDouble("nUnitPrce"));
                record.put("Total", loRS.getDouble("Total"));

                dataArray.add(record);
                lnctr++;
            }

            MiscUtil.close(loRS);

            if (lnctr > 0) {
                poJSON.put("result", "success");
                poJSON.put("message", "Record(s) loaded successfully.");
                poJSON.put("data", dataArray);
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "No records found.");
                poJSON.put("data", new JSONArray());
            }

        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }

        return poJSON;
    } 

    private String safeStrings(Object value) {
        return value == null ? "" : value.toString();
    }
    

   public static class Reports {

    private Integer nRowNo;
    private String sSupplier;
    private String sDestination;
    private String sPONo;
    private java.sql.Date dTransact;
    private String sBarCodex;
    private String sDescript;
    private String sBrand;
    private String sModelCode;
    private String sModelName;
    private String sColor;
    private double nQty;
    private double nReceived;
    private double nCancelld;
    private double nUnitPrice;
    private double nTotal;
    private String sBranch;
    private String sCategory;
    private String sTerm;
    private String sReferNo;
    private String cTranStat;

    public Reports(Integer nrowNo, String sSupplier, String sDestination, String sPONo, java.sql.Date dTransact,
                   String sBarCodex,String sDescript, String sBrand, String sModelCode, String sModelName, String sColor,
                   double nQty, double nReceived, double nCancelld, double nUnitPrice, double nTotal) {

        this.nRowNo = nrowNo;
        this.sSupplier = sSupplier;
        this.sDestination = sDestination;
        this.sPONo = sPONo;
        this.dTransact = dTransact;
        this.sBarCodex = sBarCodex;
        this.sDescript = sDescript;
        this.sBrand = sBrand;
        this.sModelCode = sModelCode;
        this.sModelName = sModelName;
        this.sColor = sColor;
        this.nQty = nQty;
        this.nReceived = nReceived;
        this.nCancelld = nCancelld;
        this.nUnitPrice = nUnitPrice;
        this.nTotal = nTotal;
    }
    public Reports(Integer nrowNo, String sSupplier, String sDestination, String sBranch,String sPONo, String sReferNo, java.sql.Date dTransact,
                   String sCategory, String sTerm,String cTranStat, double nTotal) {

        this.nRowNo = nrowNo;
        this.sSupplier = sSupplier;
        this.sDestination = sDestination;
        this.sBranch = sBranch;
        this.sPONo = sPONo;
        this.sReferNo = sReferNo;
        this.dTransact = dTransact;
        this.sCategory = sCategory;
        this.sTerm = sTerm;
        this.cTranStat = cTranStat;
        this.nTotal = nTotal;
    }

    public Integer getnRowNo() { return nRowNo; }

    public String getsSupplier() { return sSupplier; }
    public String getsDestination() { return sDestination; }
    public String getsPONo() { return sPONo; }
    public java.sql.Date getdTransact() { return dTransact; }
    public String getsBarCodex() { return sBarCodex; }
    public String getsDescript() { return sDescript; }
    public String getsBrand() { return sBrand; }
    public String getsModelCode() { return sModelCode; }
    public String getsModelName() { return sModelName; }
    public String getsColor() { return sColor; }

    public double getnQty() { return nQty; }
    public double getnReceived() { return nReceived; }
    public double getnCancelld() { return nCancelld; }
    public double getnUnitPrice() { return nUnitPrice; }
    public double getnTotal() { return nTotal; }
    
    public String getsBranch() { return sBranch; }
    public String getsCategory() { return sCategory; }
    public String getsTerm() { return sTerm; }
    public String getsReferNo() { return sReferNo; }
    public String getcTranStat() { return cTranStat; }
}

    
    public JSONObject PrintSummaryReports(Boolean isSummarized, JSONArray reportData) {
        poJSON = new JSONObject();

        try {

            Map<String, Object> parameters = new HashMap<>();

            List<Reports> reportList = new ArrayList<>();

            for (int i = 0; i < reportData.size(); i++) {

                JSONObject obj = (JSONObject) reportData.get(i);

                if (isSummarized) {
                    reportList.add(new Reports(i + 1,
                            safeStrings(obj.get("Supplier")),
                            safeStrings(obj.get("Destination")),
                            safeStrings(obj.get("Branch")),
                            safeStrings(obj.get("sTransNox")),
                            safeStrings(obj.get("sReferNox")),
                            (java.sql.Date) obj.get("dTransact"),
                            safeStrings(obj.get("Category")),
                            safeStrings(obj.get("Term")),
                            safeStrings(obj.get("cTranStat")),
                            parseDouble(obj.get("Total"))));

                } else {
                    reportList.add(new Reports(
                            i + 1,
                            safeStrings(obj.get("Supplier")),
                            safeStrings(obj.get("Destination")),
                            safeStrings(obj.get("sTransNox")),
                            (java.sql.Date) obj.get("dTransact"),
                            safeStrings(obj.get("sBarCodex")),
                            safeStrings(obj.get("Description")),
                            safeStrings(obj.get("Brand")),
                            safeStrings(obj.get("ModelCode")),
                            safeStrings(obj.get("ModelName")),
                            safeStrings(obj.get("Color")),
                            parseDouble(obj.get("Quantity")),
                            parseDouble(obj.get("nReceived")),
                            parseDouble(obj.get("nCancelld")),
                            parseDouble(obj.get("UnitPrice")), //FIX: key was "UnitPrce", so unit price printed as 0
                            parseDouble(obj.get("Total"))
                    ));
                }
            }

            JRBeanCollectionDataSource dataSource
                    = new JRBeanCollectionDataSource(reportList);

            String jrxmlPath;

            if (isSummarized) {
                jrxmlPath = System.getProperty("sys.default.path.config")
                        + "/reports/PurchaseOrderSummary.jrxml";
            } else {
                jrxmlPath = System.getProperty("sys.default.path.config")
                        + "/reports/PurchaseOrderSummaryDetail.jrxml";
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlPath);

            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport,
                    parameters,
                    dataSource
            );

            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                CustomJasperViewerReports.show(
                        jasperPrint,"Purchase Order Reports");
            } else {
                JasperExportManager.exportReportToPdfFile(
                        jasperPrint,
                        System.getProperty("sys.default.path.config")
                        + "/temp/" + Master().getTransactionNo() + ".pdf"
                );
            }

            //FIX: success only when nothing failed (was set after the catch, overwriting errors)
            poJSON.put("result", "success");
        } catch (Exception ex) {
            Logger.getLogger(PurchaseOrderMain.class.getName()).log(Level.SEVERE, null, ex);
            poJSON.put("result", "error");
            poJSON.put("message", ex.getMessage());
        }
        return poJSON;
    }

    private double parseDouble(Object val) {
        try {
            return val == null ? 0.0000 : Double.parseDouble(val.toString());
        } catch (Exception e) {
            return 0.0000;
        }
    }

    
    public static String formatDateToText(String dfrom) {

        if (dfrom == null || dfrom.isEmpty()) {
            return "";
        }

        DateTimeFormatter inputFormatter
                = DateTimeFormatter.ofPattern("yyyy-M-d");

        LocalDate date = LocalDate.parse(dfrom, inputFormatter);

        DateTimeFormatter outputFormatter
                = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

        return date.format(outputFormatter);
    }
    
    public JSONObject printReports(Boolean isSummarized, JSONArray reportData) {
        poJSON = new JSONObject();

        try {

            String lsCompanyAddress = "";
            if (Master().Company().getCompanyAddress() != null && !"".equals(Master().Company().getCompanyAddress())) {
                lsCompanyAddress = Master().Company().getCompanyAddress().trim();
            }
            if (Master().Company().TownCity().getDescription() != null && !"".equals(Master().Company().TownCity().getDescription())) {
                lsCompanyAddress = lsCompanyAddress + " " + Master().Company().TownCity().getDescription().trim();
            }
            if (Master().Company().TownCity().Province().getDescription() != null && !"".equals(Master().Company().TownCity().Province().getDescription())) {
                lsCompanyAddress = lsCompanyAddress + ", " + Master().Company().TownCity().Province().getDescription().trim();
            }

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("sLogCompny", poGRider.getCompnyId());
            parameters.put("sAddressx", lsCompanyAddress);
            
            parameters.put("sCompany", Master().Company().getCompanyName());
            parameters.put("sIndustry", Master().Industry().getDescription());
            parameters.put("sBranch", Master().Branchx().getBranchName());
            parameters.put("sDateRange", "FROM " + formatDateToText(dfrom) + " TO " + formatDateToText(dthru));
           
            List<Reports> reportList = new ArrayList<>();

            for (int i = 0; i < reportData.size(); i++) {

                JSONObject obj = (JSONObject) reportData.get(i);
                if (isSummarized) {
                    reportList.add(new Reports(i + 1,
                            safeStrings(obj.get("Supplier")),
                            safeStrings(obj.get("Destination")),
                            safeStrings(obj.get("Branch")),
                            safeStrings(obj.get("sTransNox")),
                            safeStrings(obj.get("sReferNox")),
                            (java.sql.Date) obj.get("dTransact"),
                            safeStrings(obj.get("Category")),
                            safeStrings(obj.get("Term")),
                            safeStrings(obj.get("cTranStat")),
                            parseDouble(obj.get("Total"))));

                } else {
                    reportList.add(new Reports(
                            i + 1,
                            safeStrings(obj.get("Supplier")),
                            safeStrings(obj.get("Destination")),
                            safeStrings(obj.get("sTransNox")),
                            (java.sql.Date) obj.get("dTransact"),
                            safeStrings(obj.get("sBarCodex")),
                            safeStrings(obj.get("Description")),
                            safeStrings(obj.get("Brand")),
                            safeStrings(obj.get("ModelCode")),
                            safeStrings(obj.get("ModelName")),
                            safeStrings(obj.get("Color")),
                            parseDouble(obj.get("Quantity")),
                            parseDouble(obj.get("nReceived")),
                            parseDouble(obj.get("nCancelld")),
                            parseDouble(obj.get("UnitPrice")),
                            parseDouble(obj.get("Total"))
                    ));
                }
            }

            JRBeanCollectionDataSource dataSource
                    = new JRBeanCollectionDataSource(reportList);

            // 4. Compile and fill report
            String jrxmlPath;

            if (isSummarized) {
                jrxmlPath = System.getProperty("sys.default.path.config")
                        + "/reports/PurchaseOrderSummary.jrxml";
            } else {
                jrxmlPath = System.getProperty("sys.default.path.config")
                        + "/reports/PurchaseOrderSummaryDetail.jrxml";
            }

            JasperReport jasperReport;

            jasperReport = JasperCompileManager.compileReport(jrxmlPath);

            JasperPrint jasperPrint;
            jasperPrint = JasperFillManager.fillReport(
                    jasperReport,
                    parameters,
                    dataSource
            );

            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                CustomJasperViewerReports.show(
                        jasperPrint,
                        "Purchase Order Reports");
            } else {
                //mac 2026.02.21
                //export pdf file
                JasperExportManager.exportReportToPdfFile(jasperPrint, System.getProperty("sys.default.path.config") + "/temp/" + Master().getTransactionNo() + ".pdf");
            }

            poJSON.put("result", "success");
        } catch (JRException | SQLException | GuanzonException ex) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction print aborted!");
            Logger
                    .getLogger(PurchaseOrderMain.class
                            .getName()).log(Level.SEVERE, null, ex);
        }
        return poJSON;

    }
    
    // this method is for attachment
    //added by teejei
    //May 11, 2026
    
    public void resetattachment() {
        paAttachments = new ArrayList<>();
    }
    
    private TransactionAttachment TransactionAttachment() throws SQLException, GuanzonException {
        return new SysTableContollers(poGRider, null).TransactionAttachment();
    }

    private List<TransactionAttachment> TransactionAttachmentList() {
        return paAttachments;
    }

    public TransactionAttachment TransactionAttachmentList(int row) {
        return (TransactionAttachment) paAttachments.get(row);
    }

    public int getTransactionAttachmentCount() {
        if (paAttachments == null) {
            paAttachments = new ArrayList<>();
        }

        return paAttachments.size();
    }

    /** Attachments not flagged as removed (inactive). */
    private int getActiveAttachmentCount() {
        int lnCount = 0;
        for (int lnCtr = 0; lnCtr < getTransactionAttachmentCount(); lnCtr++) {
            if (!RecordStatus.INACTIVE.equals(paAttachments.get(lnCtr).getModel().getRecordStatus())) {
                lnCount++;
            }
        }
        return lnCount;
    }

    public JSONObject addAttachment()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        getTransactionAttachmentCount(); //ensures list is initialized

        if (!paAttachments.isEmpty()) {
            String lsLastTransNo = paAttachments.get(paAttachments.size() - 1).getModel().getTransactionNo();
            if (lsLastTransNo == null || lsLastTransNo.isEmpty()) {
                return errorJSON("Unable to add transaction attachment.");
            }
        }

        //FIX: every new attachment is initialized with newRecord() (was only the first one)
        paAttachments.add(TransactionAttachment());
        poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).newRecord();
        if (!"success".equals((String) poJSON.get("result"))) {
            paAttachments.remove(getTransactionAttachmentCount() - 1);
            return poJSON;
        }

        poJSON = successJSON();
        return poJSON;
    }
    
    public JSONObject removeAttachment(int fnRow) throws GuanzonException, SQLException{
        poJSON = new JSONObject();
        if(getTransactionAttachmentCount() <= 0){
            return errorJSON("No transaction attachment to be removed.");
        }
        if (fnRow < 0 || fnRow >= getTransactionAttachmentCount()) {
            return errorJSON("Invalid attachment row.");
        }
        
        if(paAttachments.get(fnRow).getEditMode() == EditMode.ADDNEW){
            paAttachments.remove(fnRow);
        } else {
            paAttachments.get(fnRow).getModel().setRecordStatus(RecordStatus.INACTIVE);
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    public int addAttachment(String fFileName) throws SQLException, GuanzonException{
        for(int lnCtr = 0;lnCtr <= getTransactionAttachmentCount() - 1;lnCtr++){
            if(fFileName.equals(paAttachments.get(lnCtr).getModel().getFileName())
                && RecordStatus.INACTIVE.equals(paAttachments.get(lnCtr).getModel().getRecordStatus())){
                paAttachments.get(lnCtr).getModel().setRecordStatus(RecordStatus.ACTIVE);
                return lnCtr;
            }
        }
        
        addAttachment();
        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setFileName(fFileName);
        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setSourceNo(Master().getTransactionNo());
        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setRecordStatus(RecordStatus.ACTIVE);
        return getTransactionAttachmentCount() - 1;
    }
    
    public void copyFile(String fsPath){
        Path source = Paths.get(fsPath);
        Path targetDir = Paths.get(System.getProperty("sys.default.path.temp.attachments"));

        try {
            // Ensure target directory exists
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // Copy file into the target directory
            Files.copy(source, targetDir.resolve(source.getFileName()),
                       StandardCopyOption.REPLACE_EXISTING);

            System.out.println("File copied successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject loadAttachments()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        paAttachments = new ArrayList<>();

        TransactionAttachment loAttachment = new SysTableContollers(poGRider, null).TransactionAttachment();
        List loList = loAttachment.getAttachments(SOURCE_CODE, Master().getTransactionNo());
        for (int lnCtr = 0; lnCtr <= loList.size() - 1; lnCtr++) {
            paAttachments.add(TransactionAttachment());
            poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).openRecord((String) loList.get(lnCtr));
            if ("success".equals((String) poJSON.get("result"))) {
                if(Master().getEditMode() == EditMode.UPDATE){
                   poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).updateRecord();
                }
            }
            
            //Download Attachments
            poJSON = WebFile.DownloadFile(WebFile.getAccessToken(System.getProperty("sys.default.access.token"))
                    , "0032" //Constant
                    , "" //Empty
                    , paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getFileName()
                    , SOURCE_CODE
                    , paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceNo()
                    , "");
            if ("success".equals((String) poJSON.get("result"))) {
                
                poJSON = (JSONObject) poJSON.get("payload");
                if(WebFile.Base64ToFile((String) poJSON.get("data")
                        , (String) poJSON.get("hash")
                        , System.getProperty("sys.default.path.temp.attachments") + "/"
                        , (String) poJSON.get("filename"))){
                    System.out.println("File downloaded succesfully.");
                } else {
                    poJSON = (JSONObject) poJSON.get("error");
                    poJSON.put("result", "error");
                    System.out.println("ERROR WebFile.DownloadFile: " + poJSON.get("message"));
                }
                
            } else {
                System.out.println("poJSON error WebFile.DownloadFile: " + poJSON.toJSONString());
            }
        }
        return poJSON;
    }

    public JSONObject SearchCategoryReports(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Category object = new ParamControllers(poGRider, logwrapr).Category();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;

        }
        poJSON.put("category",object.getModel().getDescription());
        poJSON.put("categoryID",object.getModel().getCategoryId());
        poJSON.put("result", "success");
        return poJSON;
    }
    public JSONObject SearchDestinationReports(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        //FIX: condition was inverted (returned early on success, filled data on error)
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        poJSON.put("destination",object.getModel().getBranchName());
        poJSON.put("destinationID",object.getModel().getBranchCode());
        poJSON.put("result", "success");
        return poJSON;
    }
    public JSONObject SearchBranchReports(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        poJSON.put("branch",object.getModel().getBranchName());
        poJSON.put("branchID",object.getModel().getBranchCode());
        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject SearchSupplierReports(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        AP_Client_Master object = new ClientControllers(poGRider, logwrapr).APClientMaster();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        poJSON.put("supplier",object.getModel().Client().getCompanyName());
        poJSON.put("supplierID",object.getModel().Client().getClientId());
        poJSON.put("result", "success");
        return poJSON;
    }
}