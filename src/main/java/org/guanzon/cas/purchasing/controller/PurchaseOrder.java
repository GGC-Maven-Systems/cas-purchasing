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

public class PurchaseOrder extends Transaction {

    List<Model_Inv_Stock_Request_Master> paStockRequest;
    List<Model_PO_Master> paPOMaster;
    List<StockRequest> poStockRequest;
    List<POQuotation> poPOQuotation;
    List<POQuotation> poPOQuotationStatus;
    List<POQuotation> poPOQuotationRemovedStatus;
    List<Model> paDetailRemoved;
    CashflowControllers poPaymentRequest;
    Model_Project poProject;
    String PayeeID;
    String dfrom;
    String dthru;
    String allowedDepartment = System.getProperty("allowed.department");
    private boolean pbApproval = false;
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
        poProject = new ParamModels(poGRider).Project();

        return initialize();
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
        System.out.println(poGRider.getUserID());
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
        return this.paStockRequest.size();
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
        JSONObject loJSON = new JSONObject();
        if (Detail(getDetailCount() - 1).getStockID().isEmpty()) {
            loJSON = new JSONObject();
            loJSON.put("result", "error");
            loJSON.put("message", "Last row has empty item.");
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

    private JSONObject updateProjectTitle(String fsProjectCode, String fsStatus)
            throws GuanzonException, SQLException, CloneNotSupportedException {

        poProject.initialize();

        poJSON = poProject.openRecord(fsProjectCode);
        if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
            return poJSON;
        }
        poJSON = poProject.updateRecord();
        if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
            return poJSON;
        }

        switch (fsStatus) {
            case PurchaseOrderStatus.OPEN:
                poJSON = poProject.isUsed(true);
                break;
            case PurchaseOrderStatus.CANCELLED:
                poJSON = poProject.isUsed(false);
                break;
            case PurchaseOrderStatus.VOID:
                poJSON = poProject.isUsed(false);
                break;
        }
        poProject.setModifiedDate(poGRider.getServerDate());
        poProject.setModifyingId(poGRider.getUserID());
        
        if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;

    }

    private JSONObject setProjectTitle(String fsStatus)
            throws CloneNotSupportedException, SQLException, GuanzonException {

        poJSON = new JSONObject();

        poJSON = updateProjectTitle(Master().getReference(), fsStatus);
        if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    private JSONObject saveProjectTitle(String fsStatus)
            throws CloneNotSupportedException, SQLException, GuanzonException {

        poJSON = new JSONObject();
        poJSON = poProject.saveRecord();
        if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public JSONObject willSave() throws SQLException, CloneNotSupportedException, GuanzonException {
        /* Put system validations and other assignments here */
        poJSON = new JSONObject();
        boolean lbUpdated = false;

        if (Master().getEditMode() == EditMode.ADDNEW) {
            System.out.println("Will Save : " + Master().getNextCode());
            Master().setTransactionNo(Master().getNextCode());

        }

        if (poGRider.getDepartment().equals(allowedDepartment)) {
            if (Master().getTransactionStatus().equals(PurchaseOrderStatus.OPEN)
                    || Master().getTransactionStatus().equals(PurchaseOrderStatus.CANCELLED)
                    || Master().getTransactionStatus().equals(PurchaseOrderStatus.VOID)) {
                if(Master().getReference()!=null && !Master().getReference().isEmpty()){
                    poJSON = setProjectTitle(Master().getTransactionStatus());
                    if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                }
            }
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
            PurchaseOrder loRecord = new PurchaseOrderControllers(poGRider, null).PurchaseOrder();
            loRecord.InitTransaction();
            loRecord.OpenTransaction(Master().getTransactionNo());

            lbUpdated = loRecord.getDetailCount() == getDetailCount();
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getTransactionDate().equals(Master().getTransactionDate());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getSupplierID().equals(Master().getSupplierID());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getDestinationID().equals(Master().getDestinationID());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getTermCode().equals(Master().getTermCode());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getDiscount().doubleValue() == Master().getDiscount().doubleValue();
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getAdditionalDiscount().doubleValue() == Master().getAdditionalDiscount().doubleValue();
            }

            if (loRecord.Master().getWithAdvPaym() == true) {
                if (lbUpdated) {
                    lbUpdated = loRecord.Master().getDownPaymentRatesPercentage().doubleValue() == Master().getDownPaymentRatesPercentage().doubleValue();
                }
            }
            if (loRecord.Master().getWithAdvPaym() == true) {
                if (lbUpdated) {
                    lbUpdated = loRecord.Master().getDownPaymentRatesAmount().doubleValue() == Master().getDownPaymentRatesAmount().doubleValue();
                }
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getTranTotal().doubleValue() == Master().getTranTotal().doubleValue();
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getRemarks().equals(Master().getRemarks());
            }
            if (lbUpdated) {
                for (int lnCtr = 0; lnCtr <= loRecord.getDetailCount() - 1; lnCtr++) {
                    lbUpdated = loRecord.Detail(lnCtr).getStockID().equals(Detail(lnCtr).getStockID());
                    if (lbUpdated) {
                        lbUpdated = loRecord.Detail(lnCtr).getQuantity().equals(Detail(lnCtr).getQuantity());
                    }

                    if (!lbUpdated) {
                        break;
                    }
                }
            }

            if (lbUpdated) {
                poJSON.put("result", "error");
                poJSON.put("message", "No update has been made.");
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
        GValidator loValidator = PurchaseOrderValidatorFactory.make(Master().getIndustryID());

        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(Master());

        poJSON = loValidator.validate();
        return poJSON;
    }

    @Override
    public JSONObject saveOthers() {
        JSONObject poJSON = new JSONObject();
        try {
            if (Master().getEditMode() == EditMode.ADDNEW) {
                if (poGRider.getDepartment().equals(allowedDepartment)) {
                    if (Master().getTransactionStatus().equals(PurchaseOrderStatus.OPEN)
                    || Master().getTransactionStatus().equals(PurchaseOrderStatus.CANCELLED)
                    || Master().getTransactionStatus().equals(PurchaseOrderStatus.VOID)) {
                        if (Master().getReference() != null && !Master().getReference().isEmpty()) {
                            poJSON = saveProjectTitle(Master().getTransactionStatus());
                            if (!"success".equals((String) poJSON.get("result"))) {
                                poGRider.rollbackTrans();
                                return poJSON;
                            }
                        }
                    }
                }
            }
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

        } catch (CloneNotSupportedException | SQLException | GuanzonException ex) {
            Logger.getLogger(PurchaseOrder.class.getName()).log(Level.SEVERE, null, ex);
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

    public JSONObject ConfirmTransaction(String remarks) throws ParseException, SQLException, CloneNotSupportedException, GuanzonException {
        poJSON = new JSONObject();

        //mac 2026.02.27
        //  call approve transaction upon confirmation on class and deactivate the approval UI
        //check the status of the transaction
        //kapag OPEN, idaan mo muna sa confirmation, pag hindi naman idaan mo sa approval
        if (Master().getTransactionStatus().equals(PurchaseOrderStatus.OPEN)) {
            String lsStatus = PurchaseOrderStatus.CONFIRMED;

            boolean lbConfirm = true;

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
                    check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
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
                            //check  the user level again then if he/she allow to confirm
                            poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());

                            lsStatus = Character.toString((char) (64 + Integer.parseInt(lsStatus)));
                            poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
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

            poJSON = setValueToOthers(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //check  the user level again then if he/she allow to approve
            poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());

            poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            //Update Transaction Status of PO Quotation
            poJSON = updatePOQuotationStatus(lsStatus);
            if ("error".equals((String) poJSON.get("result"))) {
                System.out.println("PO Quotation Saving " + (String) poJSON.get("message"));
                return poJSON;
            }

            poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            if (check != null) {
                check.postAuth();
            }

            poGRider.commitTrans();

            //mac 2026.02.27
            Master().setTransactionStatus(PurchaseOrderStatus.CONFIRMED);
        }

        //mac 2026.02.27
        poJSON = ApproveTransaction("Auto-approve after confirmation.");

//        poJSON = new JSONObject();
//        poJSON.put("result", "success");
//
//        if (lbConfirm) {
//            poJSON.put("message", "Transaction confirmed successfully.");
//        } else {
//            poJSON.put("message", "Transaction confirmation request submitted successfully.");
//        }
        return poJSON;
    }

    public JSONObject ApproveTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.APPROVED;
        boolean lbApprove = true;

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
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if (!check.isAuthOkay()) {
                    //check if authorization request allows system approval
                    if (check.isAllowSys()) {
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
                        poGRider.beginTrans("UPDATE STATUS", "Approve Transaction", SOURCE_CODE, Master().getTransactionNo());

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char) (64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "Approval matrix", lsStatus, !lbApprove, true);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();

                        //mac 2026.03.07
                        poJSON = new JSONObject();
                        poJSON.put("result", "success");

                        if (lbApprove) {
                            poJSON.put("message", "Transaction approved successfully.");
                        } else {
                            poJSON.put("message", "Transaction approving request submitted successfully.");
                        }
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

        System.out.println("poJSON = setValueToOthers(lsStatus)");
        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //Added validation to generate PRF only if the PO Status is APPROVED. Arsiela 03-10-2026 11:11am
        poJSON = generatePRF(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //check  the user level again then if he/she allow to approve
        poGRider.beginTrans("UPDATE STATUS", "Approve Transaction", SOURCE_CODE, Master().getTransactionNo());
        try {
            System.out.println("poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED)");
            poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            //Added validation to generate PRF only if the PO Status is APPROVED. Arsiela 03-10-2026 11:11am
            poJSON = savePRF(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            //kalyptus - 2025.10.08 02:36pm
            //save to inventory ledger
            //InventoryTransaction loTrans = new InventoryTransaction(poGRider);
            InventoryTransaction loTrans = new InventoryTransaction(poGRider, psBranchCode);
            loTrans.PurchaseOrder((String) poMaster.getValue("sTransNox"), (Date) poMaster.getValue("dTransact"), false);

            for (Model loDetail : paDetail) {
                Model_PO_Detail detail = (Model_PO_Detail) loDetail;
                loTrans.addDetail((String) poMaster.getValue("sIndstCdx"), detail.getStockID(), "0", 0, detail.getQuantity().doubleValue(), detail.getUnitPrice().doubleValue());
            }
            loTrans.saveTransaction();

            System.out.println("poJSON = statusChange(");
            poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbApprove, true);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            if (check != null) {
                check.postAuth();
            }

            poGRider.commitTrans();

            poJSON = new JSONObject();
            poJSON.put("result", "success");

            if (lbApprove) {
                poJSON.put("message", "Transaction approved successfully.");
            } else {
                poJSON.put("message", "Transaction approving request submitted successfully.");
            }

        } catch (GuanzonException | SQLException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poGRider.rollbackTrans();
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }
        return poJSON;
    }

    public JSONObject PostTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.POSTED;
        boolean lbPost = true;

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
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
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
                        //check  the user level again then if he/she allow to confirm
                        poGRider.beginTrans("UPDATE STATUS", "Post Transaction", SOURCE_CODE, Master().getTransactionNo());

                        lsStatus = Character.toString((char) (64 + Integer.parseInt(lsStatus)));
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbPost, true);
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

        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        //check  the user level again then if he/she allow to approve
        if (!pbWthParent) {
            poGRider.beginTrans("UPDATE STATUS", "Post Transaction", SOURCE_CODE, Master().getTransactionNo());
        }
        poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbPost, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        if (check != null) {
            check.postAuth();
        }

        if (!pbWthParent) {
            poGRider.commitTrans();
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbPost) {
            poJSON.put("message", "Transaction posted successfully.");
        } else {
            poJSON.put("message", "Transaction posting request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject CancelTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.CANCELLED;
        boolean lnCancel = true;
        int ProjectUsageCount = 0;
        
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
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
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
                        poGRider.beginTrans("UPDATE STATUS", "Cancel Transaction", SOURCE_CODE, Master().getTransactionNo());

                        lsStatus = Character.toString((char) (64 + Integer.parseInt(lsStatus)));
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lnCancel, true);
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

        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        /*use to tag or untag project title this code is intended for engineering dept only
        *added by: TEEJEI DE CELIS
        *date: 04/09/2026
         */
        if (poGRider.getDepartment().equals(allowedDepartment)) {
            poJSON = CheckProjectUsage(Master().getReference());
            ProjectUsageCount = Integer.parseInt(poJSON.get("count").toString());

            if (ProjectUsageCount <= 1) {
                poJSON = setProjectTitle(lsStatus);
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
            }
        }
        //check  the user level again then if he/she allow to approve
        poGRider.beginTrans("UPDATE STATUS", "Cancel Transaction", SOURCE_CODE, Master().getTransactionNo());
        try {
            //kalyptus-2025.10.08 02:52pm
            //save to inventory ledger
            if (PurchaseOrderStatus.APPROVED.equalsIgnoreCase((String) poMaster.getValue("cTranStat"))) {
                InventoryTransaction loTrans = new InventoryTransaction(poGRider);
                loTrans.PurchaseOrder((String) poMaster.getValue("sTransNox"), (Date) poMaster.getValue("dTransact"), true);

                for (Model loDetail : paDetail) {
                    Model_PO_Detail detail = (Model_PO_Detail) loDetail;
                    loTrans.addDetail((String) detail.getValue("sIndstCdx"), detail.getStockID(), "0", 0, detail.getQuantity().doubleValue(), detail.getUnitPrice().doubleValue());
                }
                loTrans.saveTransaction();
            }

            poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
            /*use to tag or untag project title this code is intended for engineering dept only
        *added by: TEEJEI DE CELIS
        *date: 04/09/2026
             */
            if (poGRider.getDepartment().equals(allowedDepartment)) {
                if (ProjectUsageCount <= 1) {
                    poJSON = saveProjectTitle(lsStatus);
                    if (!"success".equals((String) poJSON.get("result"))) {
                        poGRider.rollbackTrans();
                        return poJSON;
                    }
                }
            }

            //Update Transaction Status of PO Quotation
            poJSON = updatePOQuotationStatus(lsStatus);
            if ("error".equals((String) poJSON.get("result"))) {
                System.out.println("PO Quotation Saving " + (String) poJSON.get("message"));
                return poJSON;
            }

            poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lnCancel, true);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            if (check != null) {
                check.postAuth();
            }

            poGRider.commitTrans();
            poJSON = new JSONObject();
            poJSON.put("result", "success");

            if (lnCancel) {
                poJSON.put("message", "Transaction cancelled successfully.");
            } else {
                poJSON.put("message", "Transaction cancellation request submitted successfully.");
            }

        } catch (GuanzonException | SQLException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poGRider.rollbackTrans();
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }
        return poJSON;
    }

    public JSONObject VoidTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.VOID;
        boolean lbVoid = true;
        int ProjectUsageCount = 0;
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
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
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
                        poGRider.beginTrans("UPDATE STATUS", "Void Transaction", SOURCE_CODE, Master().getTransactionNo());

                        lsStatus = Character.toString((char) (64 + Integer.parseInt(lsStatus)));
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbVoid, true);
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

        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        /*use to tag or untag project title this code is intended for engineering dept only
        *added by: TEEJEI DE CELIS
        *date: 04/09/2026
         */
        if (poGRider.getDepartment().equals(allowedDepartment)) {
            poJSON = CheckProjectUsage(Master().getReference());
            ProjectUsageCount = Integer.parseInt(poJSON.get("count").toString());

            if (ProjectUsageCount <= 1) {
                poJSON = setProjectTitle(lsStatus);
                if (!"success".equals((String) poJSON.get("result"))) {
                    poGRider.rollbackTrans();
                    return poJSON;
                }
            }
        }
        //check  the user level again then if he/she allow to approve
        poGRider.beginTrans("UPDATE STATUS", "Void Transaction", SOURCE_CODE, Master().getTransactionNo());

        poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        /*use to tag or untag project title this code is intended for engineering dept only
        *added by: TEEJEI DE CELIS
        *date: 04/09/2026
         */
        if (poGRider.getDepartment().equals(allowedDepartment)) {
            if (ProjectUsageCount <= 1) {
                poJSON = saveProjectTitle(lsStatus);
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
            }
        }
        //Update Transaction Status of PO Quotation
        poJSON = updatePOQuotationStatus(lsStatus);
        if ("error".equals((String) poJSON.get("result"))) {
            System.out.println("PO Quotation Saving " + (String) poJSON.get("message"));
            return poJSON;
        }

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbVoid, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        if (check != null) {
            check.postAuth();
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbVoid) {
            poJSON.put("message", "Transaction voided successfully.");
        } else {
            poJSON.put("message", "Transaction voiding request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject ReturnTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = PurchaseOrderStatus.RETURNED;
        boolean lbReturn = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already returned.");
            return poJSON;
        }

        //validator
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
                    poJSON.put("result", "error");
                    poJSON.put("message", "User is not an authorized approving officer..");
                    return poJSON;
                }
                setApproving((String) poJSON.get("sUserIDxx"));
            }
        }
        poJSON = setValueToOthers(lsStatus);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        //check  the user level again then if he/she allow to approve
        poGRider.beginTrans("UPDATE STATUS", "Return Transaction", SOURCE_CODE, Master().getTransactionNo());

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbReturn, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        //Update Transaction Status of PO Quotation
        poJSON = updatePOQuotationStatus(lsStatus);
        if ("error".equals((String) poJSON.get("result"))) {
            System.out.println("PO Quotation Saving " + (String) poJSON.get("message"));
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbReturn) {
            poJSON.put("message", "Transaction returned successfully.");
        } else {
            poJSON.put("message", "Transaction return request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject isDetailHasZeroQty() {
        poJSON = new JSONObject();
        int zeroQtyRow = -1;
        boolean hasNonZeroQty = false;
        boolean hasZeroQty = false;
        int lastRow = getDetailCount() - 1;

        for (int lnRow = 0; lnRow <= lastRow; lnRow++) {
            double quantity = Detail(lnRow).getQuantity().doubleValue();
            String stockID = (String) Detail(lnRow).getValue("sStockIDx");

            if (!stockID.isEmpty()) {
                if (quantity == 0.00) {
                    hasZeroQty = true;
                    if (zeroQtyRow == -1) {
                        zeroQtyRow = lnRow;
                    }
                } else {
                    hasNonZeroQty = true;
                }
            }
        }

        if (!hasNonZeroQty && hasZeroQty) {
            poJSON.put("result", "error");
            poJSON.put("message", "All items have zero quantity. Please enter a valid quantity.");
            poJSON.put("tableRow", zeroQtyRow);
            poJSON.put("warning", "true");
        } else if (hasZeroQty) {
            poJSON.put("result", "error");
            poJSON.put("message", "Some items have zero quantity. Please review.");
            poJSON.put("tableRow", zeroQtyRow);
            poJSON.put("warning", "false");
        } else {
            poJSON.put("result", "success");
            poJSON.put("message", "All items have valid quantities.");
            poJSON.put("tableRow", lastRow);
        }

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

        //Update Purchase Order exist in PO Receiving Detail
        for (lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            System.out.println("----------------------PURCHASE ORDER RECEIVING DETAIL---------------------- ");
            System.out.println("TransNo : " + (lnCtr + 1) + " : " + Detail(lnCtr).getTransactionNo());
            System.out.println("OrderNo : " + (lnCtr + 1) + " : " + Detail(lnCtr).getSouceNo());
            System.out.println("StockId : " + (lnCtr + 1) + " : " + Detail(lnCtr).getStockID());
            System.out.println("------------------------------------------------------------------ ");
            if (Detail(lnCtr).getSouceNo() != null && !"".equals(Detail(lnCtr).getSouceNo())) {
                double totalRequest = 0.00;
                switch (Detail(lnCtr).getSouceCode()) {
                    case PurchaseOrderStatus.SourceCode.STOCKREQUEST:
                        totalRequest = (Detail(lnCtr).InvStockRequestDetail().getApproved());
                        //                        - (Detail(lnCtr).InvStockRequestDetail().getIssued()
                        //                        + Detail(lnCtr).InvStockRequestDetail().getPurchase()));

                        //1. Check discrepancy if the order quantity is not greater than request
                        if (!Detail(lnCtr).getSouceNo().isEmpty()) {
                            //                    remaining = nApproved - (nCancelld + nIssueQty + nOrderQty)

                            if (status.equals(PurchaseOrderStatus.CONFIRMED)
                                    || status.equals(PurchaseOrderStatus.APPROVED)) {
                                double remaining = 0.0000;
                                remaining = (Detail(lnCtr).InvStockRequestDetail().getApproved()
                                        - (Detail(lnCtr).InvStockRequestDetail().getCancelled()
                                        + Detail(lnCtr).InvStockRequestDetail().getIssued()
                                        + Detail(lnCtr).InvStockRequestDetail().getPurchase()));
                                if (remaining == 0.00) {
                                    poJSON.put("result", "error");
                                    poJSON.put("message", "Discrepancy: Stock requests related to this order number have already been processed.");
                                    return poJSON;
                                }
                            }

                            if (Detail(lnCtr).getQuantity().doubleValue() > totalRequest) {
                                poJSON.put("result", "error");
                                poJSON.put("message", "Discrepancy: Order Quantity cannot greater than request quantity, please enter valid order quantity.");
                                return poJSON;
                            }
                        }

                        //1. Check for discrepancy
                        if (Detail(lnCtr).getQuantity().doubleValue() != Detail(lnCtr).InvStockRequestDetail().getQuantity()) {
                            System.out.println("Require Approval");
                            pbApproval = true;
                        }

                        if (Master().getTransactionStatus().equals(PurchaseOrderStatus.RETURNED)) {

                            if (totalRequest == 0) {
                                poJSON.put("result", "error");
                                poJSON.put("message", "All stock requests related to this order number have already been processed.");
                                return poJSON;
                            }
                        }

                        updateInvStockRequest(status, Detail(lnCtr).getSouceNo(), Detail(lnCtr).getStockID(), Detail(lnCtr).getQuantity().doubleValue());
                        break;
                    case PurchaseOrderStatus.SourceCode.POQUOTATION:
                        //TODO Validate
                        poJSON = checkExistingPO(Detail(lnCtr).getSouceNo(), Detail(lnCtr).getSouceCode());
                        if ("error".equals((String) poJSON.get("result"))) {
                            return poJSON;
                        }
                        //TODO UPDATING OF DETAILS
//                        totalRequest = (Detail(lnCtr).POQuotationDetail().getQuantity());
                        //1. Check discrepancy if the order quantity is not greater than request
//                        if (!Detail(lnCtr).getSouceNo().isEmpty()) {
//        //                    remaining = nApproved - (nCancelld + nIssueQty + nOrderQty)
//
//                           if (status.equals(PurchaseOrderStatus.CONFIRMED) ||
//                                   status.equals(PurchaseOrderStatus.APPROVED)) {
//                                double remaining = 0.0000;
//                                    remaining = (Detail(lnCtr).InvStockRequestDetail().getApproved() -
//                                            (Detail(lnCtr).InvStockRequestDetail().getCancelled() + 
//                                            Detail(lnCtr).InvStockRequestDetail().getIssued() + 
//                                            Detail(lnCtr).InvStockRequestDetail().getPurchase()));
//                                if (remaining == 0.00) {
//                                    poJSON.put("result", "error");
//                                    poJSON.put("message", "Discrepancy: Stock requests related to this order number have already been processed.");
//                                    return poJSON;
//                                }
//                            }
//
//                            if (Detail(lnCtr).getQuantity().doubleValue() > totalRequest) {
//                                poJSON.put("result", "error");
//                                poJSON.put("message", "Discrepancy: Order Quantity cannot greater than request quantity, please enter valid order quantity.");
//                                return poJSON;
//                            }
//                        }
//
//                        //1. Check for discrepancy
//                        if (Detail(lnCtr).getQuantity().doubleValue() != Detail(lnCtr).InvStockRequestDetail().getQuantity()) {
//                            System.out.println("Require Approval");
//                            pbApproval = true;
//                        }
//
//                        if (Master().getTransactionStatus().equals(PurchaseOrderStatus.RETURNED)) {
//                            if (totalRequest == 0) {
//                                poJSON.put("result", "error");
//                                poJSON.put("message", "All stock requests related to this order number have already been processed.");
//                                return poJSON;
//                            }
//                        }

                        updatePOQuotation(status, Detail(lnCtr).getSouceNo(), Detail(lnCtr).getStockID(), Detail(lnCtr).getQuantity().doubleValue(), false);
                        break;
                }

            } else {
                //Require approve for all po receiving without po
                System.out.println("Require Approval");
                pbApproval = true;
            }
        }
        //Update stock request removed in purchase order
        for (lnCtr = 0; lnCtr <= getDetailRemovedCount() - 1; lnCtr++) {
            switch (Detail(lnCtr).getSouceCode()) {
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
        double lnRecQty = 0.00;
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
        //TODO UPDATING OF DETAILS
//        for (lnRow = 0; lnRow <= poPOQuotation.get(lnList).getDetailCount() - 1; lnRow++) {
//            if (stockId.equals(poPOQuotation.get(lnList).Detail(lnRow).getStockId())) {
//
//                switch (status) {
//                    case PurchaseOrderStatus.CONFIRMED:
////                        poPOQuotation.get(lnList).Master().setTransactionStatus(POQuotationStatus.POSTED);
//                        lnRecQty = getRequestQty(orderNo, stockId, true,poPOQuotation.get(lnList).getSourceCode());
//                        lnRecQty = lnRecQty + quantity;
//                        break;
//                    case PurchaseOrderStatus.APPROVED:
//                        lnRecQty = poPOQuotation.get(lnList).Detail(lnRow).getQuantity();
////                        poPOQuotation.get(lnList).Master().setTransactionStatus(POQuotationStatus.POSTED);
//                        poPOQuotation.get(lnList).Master().setModifiedDate(poGRider.getServerDate());
//                        poPOQuotation.get(lnList).Master().setModifyingId(poGRider.getUserID());
//                        break;
//                    case PurchaseOrderStatus.VOID:
//                    case PurchaseOrderStatus.RETURNED:
////                        poPOQuotation.get(lnList).Master().setTransactionStatus(POQuotationStatus.APPROVED); //TODO
//                        lnRecQty = getRequestQty(orderNo, stockId, false,poPOQuotation.get(lnList).getSourceCode());
//                        lnRecQty = lnRecQty - quantity;
//                        break;
//                }
//                if (lnRecQty < 0) {
//                    lnRecQty = 0;
//                }
////                poPOQuotation.get(lnList).Detail(lnRow).setPurchase(lnRecQty);
//                poPOQuotation.get(lnList).Detail(lnRow).setModifiedDate(poGRider.getServerDate());
//                break;
//            }
//        }
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
            if (Master().getWithAdvPaym() 
                    && (Master().getDownPaymentRatesAmount().doubleValue() > PurchaseOrderStaticData.default_value_double 
                    || Master().getDownPaymentRatesPercentage().doubleValue() > PurchaseOrderStaticData.default_value_double) //Added validation for rate - Arsiela 07-15-2026 ; Original programmer Dave & TJ forgot to update based on the previous instruction as per sir TJ
                ) {
                poPaymentRequest = new CashflowControllers(poGRider, null);

                poPaymentRequest.PaymentRequest().InitTransaction();
                poPaymentRequest.PaymentRequest().NewTransaction();

                SearchPayee(Master().getSupplierID());

                poPaymentRequest.PaymentRequest().Master().setTransactionDate(Master().getTransactionDate());
                poPaymentRequest.PaymentRequest().Master().setBranchCode(Master().getDestinationID());
                if (poGRider.isMainOffice() || poGRider.isWarehouse()) {
                    poPaymentRequest.PaymentRequest().Master().setDepartmentID(poGRider.getDepartment());
                }

//                double transactionTotal = Master().getTranTotal().doubleValue();
//                double discountRate = Master().getDiscount().doubleValue();
//                double additionalDiscount = Master().getAdditionalDiscount().doubleValue();
//                double downPaymentPercentage = Master().getDownPaymentRatesPercentage().doubleValue();
//                double downPaymentFixedAmount = Master().getDownPaymentRatesAmount().doubleValue();
//
//                double discountAmount = transactionTotal * discountRate;
//                double netTotal = transactionTotal - discountAmount;
//
//                netTotal -= additionalDiscount;
//
//                double downPaymentPercentageAmount = netTotal * downPaymentPercentage;
//                netTotal -= downPaymentPercentageAmount;
//                netTotal -= downPaymentFixedAmount;
//
//                double totalAdv = downPaymentPercentageAmount + downPaymentFixedAmount;

                //Disable computation above and replace by the computation below - Arsiela 07-15-2026
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

    private int getRequestQty(String orderNo, String stockId, boolean isAdd, String sourceCode) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        int lnRecQty = 0;
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
                    lnRecQty = lnRecQty + loRS.getInt("nQuantity");
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
            if (Master().getWithAdvPaym() 
                    && (Master().getDownPaymentRatesAmount().doubleValue() > PurchaseOrderStaticData.default_value_double 
                    || Master().getDownPaymentRatesPercentage().doubleValue() > PurchaseOrderStaticData.default_value_double) //Added validation for rate - Arsiela 07-15-2026 ; Original programmer Dave & TJ forgot to update based on the previous instruction as per sir TJ
                ) {
                
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
            Logger.getLogger(PurchaseOrder.class
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
            Logger.getLogger(PurchaseOrder.class
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
                    poJSON = checkExistingPO(poPOQuotationStatus.get(lnCtr).Master().getTransactionNo(), PurchaseOrderStatus.SourceCode.POQUOTATION);
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
            poJSON = checkExistingPO(poPOQuotationRemovedStatus.get(lnCtr).Master().getTransactionNo(), PurchaseOrderStatus.SourceCode.POQUOTATION);
            if ("success".equals((String) poJSON.get("result"))) {
                poPOQuotationRemovedStatus.get(lnCtr).setWithParent(true);
                poJSON = poPOQuotationRemovedStatus.get(lnCtr).ApproveTransaction("");
                if ("error".equals((String) poJSON.get("result"))) {
                    System.out.println("PO Quotation Approving " + (String) poJSON.get("message"));
                    return poJSON;
                }
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    private JSONObject checkExistingPO(String fsSourceNo, String fsSourceCode) throws SQLException {
        String detailQuery = " SELECT b.sSourceNo, b.sSourceCd "
                + " FROM PO_Master a "
                + " LEFT JOIN PO_Detail b ON a.sTransNox = b.sTransNox ";

        String lsFilterCondition = String.join(" AND ",
                " b.sSourceNo = " + SQLUtil.toSQL(fsSourceNo),
                " b.sSourceCd = " + SQLUtil.toSQL(fsSourceCode),
                " ( a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.CONFIRMED)
                + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderStatus.APPROVED) + " ) ",
                " a.sTransNox <> " + SQLUtil.toSQL(Master().getTransactionNo())
        );

        detailQuery = MiscUtil.addCondition(detailQuery, lsFilterCondition);
        System.out.println("Executing SQL: " + detailQuery);
        ResultSet loRS = poGRider.executeQuery(detailQuery);
        if (MiscUtil.RecordCount(loRS) >= 0) {
            if (loRS.next()) {
                poJSON.put("result", "error");
                poJSON.put("message", "Selected PO Quotation already have an existing Purchase Order.");
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        poJSON.put("message", "No Record Found."); //Allow to set / update
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
        String lsBranch = "";
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
        if (!poGRider.isMainOffice() || !poGRider.isWarehouse()) {
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
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
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

    public JSONObject SearchBarcode(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException, NullPointerException {

        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

        String supplier = Master().getSupplierID().isEmpty() ? null : Master().getSupplierID();
        String brand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String industry = Master().getIndustryID().isEmpty() ? null : Master().getIndustryID();
        String category = Master().getCategoryCode();

        poJSON = object.searchRecord(
                value,
                byCode,
                hasNoSupplier ? supplier : null,
                brand,
                industry,
                category
        );

        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Detail(lnRow).getSouceNo().equals("") || Detail(lnRow).getSouceNo() == null)
                            && (Detail(lnRow).getStockID().equals(object.getModel().getStockId()))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Model: " + object.getModel().getDescription() + " already exist in table at row " + (lnRow + 1) + ".");
                        poJSON.put("tableRow", lnRow);
                        return poJSON;
                    }
                }
            }
            Detail(row).setStockID(object.getModel().getStockId());
            Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
            Detail(row).setOldPrice(object.getModel().getCost().doubleValue());
        }
        return poJSON;
    }

    public JSONObject SearchBarcodeGeneral(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException, NullPointerException {

        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

        String supplier = Master().getSupplierID().isEmpty() ? null : Master().getSupplierID();
        String brand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String industry = Master().getIndustryID().isEmpty() ? null : Master().getIndustryID();
        String category = Master().getCategoryCode();

        poJSON = object.searchRecord(
                value,
                byCode,
                hasNoSupplier ? supplier : null,
                brand,
                null,
                category
        );

        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Detail(lnRow).getSouceNo().equals("") || Detail(lnRow).getSouceNo() == null)
                            && (Detail(lnRow).getStockID().equals(object.getModel().getStockId()))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Model: " + object.getModel().getDescription() + " already exist in table at row " + (lnRow + 1) + ".");
                        poJSON.put("tableRow", lnRow);
                        return poJSON;
                    }
                }
            }
            Detail(row).setStockID(object.getModel().getStockId());
            Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
            Detail(row).setOldPrice(object.getModel().getCost().doubleValue());
//            Detail(row).isSerialized(object.getModel().isSerialized());
//            Detail(row).setCategoryCode(object.getModel().getCategoryFirstLevelId());
        }
        return poJSON;
    }

    public JSONObject SearchBarcodeDescription(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException, NullPointerException {

        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

        String supplier = Master().getSupplierID().isEmpty() ? null : Master().getSupplierID();
        String brand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String industry = Master().getIndustryID().isEmpty() ? null : Master().getIndustryID();
        String category = Master().getCategoryCode();

        poJSON = object.searchRecord(
                value,
                byCode,
                hasNoSupplier ? supplier : null,
                brand,
                industry,
                category
        );

        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Detail(lnRow).getSouceNo().equals("") || Detail(lnRow).getSouceNo() == null)
                            && (Detail(lnRow).getStockID().equals(object.getModel().getStockId()))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Barcode: " + object.getModel().getDescription() + " already exist in table at row " + (lnRow + 1) + ".");
                        poJSON.put("tableRow", lnRow);
                        return poJSON;
                    }
                }
            }
            Detail(row).setStockID(object.getModel().getStockId());
            Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
            Detail(row).setOldPrice(object.getModel().getCost().doubleValue());
        }
        return poJSON;
    }

    public JSONObject SearchBarcodeDescriptionGeneral(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException, NullPointerException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

        String supplier = Master().getSupplierID().isEmpty() ? null : Master().getSupplierID();
        String brand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String industry = Master().getIndustryID().isEmpty() ? null : Master().getIndustryID();
        String category = Master().getCategoryCode();

        poJSON = object.searchRecord(
                value,
                byCode,
                hasNoSupplier ? supplier : null,
                brand,
                null,
                category
        );

        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Detail(lnRow).getSouceNo().equals("") || Detail(lnRow).getSouceNo() == null)
                            && (Detail(lnRow).getStockID().equals(object.getModel().getStockId()))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Barcode: " + object.getModel().getDescription() + " already exist in table at row " + (lnRow + 1) + ".");
                        poJSON.put("tableRow", lnRow);
                        return poJSON;
                    }
                }
            }
            Detail(row).setStockID(object.getModel().getStockId());
            Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
            Detail(row).setOldPrice(object.getModel().getCost().doubleValue());
        }
        return poJSON;
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
//            Master().setPayeeID(object.getModel().getPayeeID());
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
            Detail(row).setBrandId(brand.getModel().getBrandId());
        }

        return poJSON;
    }

    public JSONObject SearchModel(String value, boolean byCode, int row, boolean hasNoSupplier)
            throws SQLException, GuanzonException, NullPointerException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus("123");
        
        String supplier = Master().getSupplierID().isEmpty() ? null : Master().getSupplierID();
        String brand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String industry = Master().getIndustryID().isEmpty() ? null : Master().getIndustryID();
        String category = Master().getCategoryCode();

        poJSON = object.searchRecord(
                value,
                byCode,
                hasNoSupplier ? supplier : null,
                brand,
                industry,
                category
        );

        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Detail(lnRow).getSouceNo().equals("") || Detail(lnRow).getSouceNo() == null)
                            && (Detail(lnRow).getStockID().equals(object.getModel().getStockId()))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Barcode: " + object.getModel().getDescription() + " already exist in table at row " + (lnRow + 1) + ".");
                        poJSON.put("tableRow", lnRow);
                        return poJSON;
                    }
                }
            }
            Detail(row).setStockID(object.getModel().getStockId());
            Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
            Detail(row).setOldPrice(object.getModel().getCost().doubleValue());
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
        if (!poGRider.isMainOffice() || !poGRider.isWarehouse()) {
            lsSQL = lsSQL + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode());
        }
        lsSQL = lsSQL
                + " GROUP BY a.sTransNox "
                + " HAVING SUM(b.nApproved - (b.nIssueQty + b.nOrderQty)) > 0 ";
        //For General Only
//        if(Master().getIndustryID() == null || "".equals(Master().getIndustryID())){
        lsSQL = lsSQL + " UNION " + getPOQuotation_SQL();
        lsFilterCondition = String.join(" AND ", " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()),
                " a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID()),
                " a.sSupplier = " + SQLUtil.toSQL(Master().getSupplierID()),
                " a.sCategrCd = " + SQLUtil.toSQL(Master().getCategoryCode()),
                " a.cTranStat = " + SQLUtil.toSQL(POQuotationStatus.APPROVED));
        lsSQL = lsSQL + " WHERE " + lsFilterCondition;
        //Do not include branch according to ma'am she 10082025
//            if (!poGRider.isMainOffice() || !poGRider.isWarehouse()) {
//                lsSQL = lsSQL + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode());
//            }
        lsSQL = lsSQL + " GROUP BY a.sTransNox ";
//        }

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

        // ✅ Only check `allProcessed` **after** the loop
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

        poJSON = checkExistingPO(transactionNo, loTrans.POQuotation().getSourceCode());
        if ("error".equals(poJSON.get("result"))) {
            return poJSON;
        }

//        boolean allProcessed = true;
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

        // ✅ Only check `allProcessed` **after** the loop
//        if (allProcessed) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "All records are already processed!");
//            return poJSON;
//        }
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
        if (!poGRider.isMainOffice() || !poGRider.isWarehouse()) {
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
                // Print the result set
                System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                System.out.println("dTransact: " + loRS.getDate("dTransact"));
                System.out.println("------------------------------------------------------------------------------");

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
        if (!poGRider.isMainOffice() || !poGRider.isWarehouse()) {
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
                // Print the result set
                System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                System.out.println("dTransact: " + loRS.getDate("dTransact"));
                System.out.println("------------------------------------------------------------------------------");

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
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()
                + " AND a.sCategrCd = " + SQLUtil.toSQL(Master().getCategoryCode())));

        ResultSet loRS = poGRider.executeQuery(lsSQL);
        String inventoryTypeCode = null;

        if (loRS.next()) {
            inventoryTypeCode = loRS.getString("sInvTypCd");
        }

        MiscUtil.close(loRS);
        return inventoryTypeCode;
    }

    public JSONObject setDiscountRate(String fsValue) {
        poJSON = new JSONObject();
        if (fsValue == null || fsValue.isEmpty()) {
            fsValue = "0.0000";
        }
        double lnTotalAmount = (double) Master().getTranTotal();
        if (lnTotalAmount == 0.0000) {
            poJSON.put("message", "You're not allowed to enter discount rate, no detail amount entered.");
            poJSON.put("result", "error");
            Master().setDiscount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }
        double lnDiscountRate = Double.parseDouble(fsValue);
        if (lnDiscountRate < PurchaseOrderStaticData.default_value_double || lnDiscountRate > 100.0000) {
            poJSON.put("message", "Invalid Discount Rate.  Must be between 0.00 and 100.00");
            poJSON.put("result", "error");
            Master().setDiscount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }

        Master().setDiscount(lnDiscountRate);
        computeNetTotal();
        double lnNetTotal = Master().getNetTotal().doubleValue();
        if (lnNetTotal < PurchaseOrderStaticData.default_value_double) {
            poJSON.put("message", "Invalid Total Transaction Amount");
            poJSON.put("result", "error");
            Master().setAdditionalDiscount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject setDiscountAmount(String fsValue) {
        poJSON = new JSONObject();
        if (fsValue == null || fsValue.isEmpty()) {
            fsValue = "0.0000";
        }
        if (Double.parseDouble(fsValue) >= 1000000.0000) {
            poJSON.put("message", "Discount amount must not exceed 1 Million.");
            poJSON.put("result", "error");
            Master().setAdditionalDiscount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }
        double lnTotalAmount = Master().getTranTotal().doubleValue() - ((Master().getDiscount().doubleValue() / 100) * Master().getTranTotal().doubleValue());
        if (lnTotalAmount == PurchaseOrderStaticData.default_value_double) {
            poJSON.put("message", "You're not allowed to enter discount amount, no amount entered.");
            poJSON.put("result", "error");
            Master().setAdditionalDiscount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }
        double lnDiscountAmount = Double.parseDouble(fsValue.replace(",", ""));
        if (lnDiscountAmount < 0.0 || lnDiscountAmount > lnTotalAmount) {
            poJSON.put("message", "Invalid Discount Amount");
            poJSON.put("result", "error");
            Master().setAdditionalDiscount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }

        Master().setAdditionalDiscount(lnDiscountAmount);
        computeNetTotal();
        double lnNetTotal = Master().getNetTotal().doubleValue();
        if (lnNetTotal < PurchaseOrderStaticData.default_value_double) {
            poJSON.put("message", "Invalid Total Transaction Amount");
            poJSON.put("result", "error");
            Master().setAdditionalDiscount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject setAdvancePaymentRate(String fsValue) {
        poJSON = new JSONObject();
        if (fsValue == null || fsValue.isEmpty()) {
            fsValue = "0.00";
        }
        double amountAfterDiscounts = Master().getTranTotal().doubleValue() - (((Master().getTranTotal().doubleValue() / 100) * Master().getDiscount().doubleValue()) + Master().getAdditionalDiscount().doubleValue());
        if (amountAfterDiscounts <= PurchaseOrderStaticData.default_value_double) {
            poJSON.put("message", "Invalid Advance Payment Rate, the total transaction amount is 0.0000");
            poJSON.put("result", "error");
            Master().setDownPaymentRatesPercentage(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }

        double lnAdvanceRate = Double.parseDouble(fsValue);
        if (lnAdvanceRate < PurchaseOrderStaticData.default_value_double || lnAdvanceRate > 100.0000) {
            poJSON.put("message", "Invalid Advance Payment Rate. Must be between 0.0000 and 100.0000");
            poJSON.put("result", "error");
            Master().setDownPaymentRatesPercentage(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }
        
        //Added validation for total downpayment Arsiela 07-15-2026
        double ldblTotalAdvPayment = ((amountAfterDiscounts/100) * lnAdvanceRate) + Master().getDownPaymentRatesAmount().doubleValue();
        if(ldblTotalAdvPayment > amountAfterDiscounts){
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid Downpayment Total.");
            Master().setDownPaymentRatesPercentage(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }

        Master().setDownPaymentRatesPercentage(lnAdvanceRate);
        computeNetTotal();
        double lnNetTotal = Master().getNetTotal().doubleValue();
        if (lnNetTotal < PurchaseOrderStaticData.default_value_double) {
            poJSON.put("message", "Invalid Total Transaction Amount");
            poJSON.put("result", "error");
            Master().setDownPaymentRatesPercentage(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject setAdvancePaymentAmount(String fsValue) {
        poJSON = new JSONObject();
        if (fsValue == null || fsValue.isEmpty()) {
            fsValue = "0.0000";
        }
//        double totalAmountAfterDiscount = Master().getTranTotal().doubleValue() - ((Master().getTranTotal().doubleValue() * Master().getDiscount().doubleValue()) + Master().getAdditionalDiscount().doubleValue());
//        double totalAdvRateAmount = totalAmountAfterDiscount * Master().getDownPaymentRatesPercentage().doubleValue();
//        double totalAmountDiscountWithRate = totalAmountAfterDiscount - totalAdvRateAmount;

        //Replace by the computation above Arsiela 07-15-2026
        double amountAfterDiscounts = Master().getTranTotal().doubleValue() - (((Master().getTranTotal().doubleValue() / 100) * Master().getDiscount().doubleValue()) + Master().getAdditionalDiscount().doubleValue());
        
        if (amountAfterDiscounts <= PurchaseOrderStaticData.default_value_double) {
            poJSON.put("message", "Invalid Advance Payment Amount, the total transaction amount is 0.0000");
            poJSON.put("result", "error");
            Master().setDownPaymentRatesPercentage(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }

        double lnAdvanceAmount = Double.parseDouble(fsValue.replace(",", ""));
        if (lnAdvanceAmount < PurchaseOrderStaticData.default_value_double || lnAdvanceAmount > amountAfterDiscounts) {
            poJSON.put("message", "Invalid Advance Payment Amount");
            poJSON.put("result", "error");
            Master().setDownPaymentRatesAmount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }
        
        double ldblTotalAdvPayment = ((amountAfterDiscounts/100) * Master().getDownPaymentRatesPercentage().doubleValue()) + lnAdvanceAmount ; //Added validation for total downpayment Arsiela 07-15-2026
        if(ldblTotalAdvPayment > amountAfterDiscounts){
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid Downpayment Total.");
            Master().setDownPaymentRatesAmount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }

        Master().setDownPaymentRatesAmount(lnAdvanceAmount);
        computeNetTotal();
        double lnNetTotal = Master().getNetTotal().doubleValue();
        if (lnNetTotal < PurchaseOrderStaticData.default_value_double) {
            poJSON.put("message", "Invalid Total Transaction Amount");
            poJSON.put("result", "error");
            Master().setDownPaymentRatesAmount(PurchaseOrderStaticData.default_value_double);
            computeNetTotal();
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    public void computeNetTotal() {
        double totalAmount = Master().getTranTotal().doubleValue(); // Total transaction amount
        double discountRate = Master().getDiscount().doubleValue() / 100;
        System.out.printf("Discount: %.2f%%\n", discountRate);
        double discountRateWithTotalAmount = totalAmount * discountRate;
        double additionalDiscountAmount = Master().getAdditionalDiscount().doubleValue();

        // Amount after all discounts
        double totalDiscount = (discountRateWithTotalAmount + additionalDiscountAmount);

//        // Compute down payment from rate
//        double downPaymentRate = Master().getDownPaymentRatesPercentage().doubleValue(); // e.g., 0.20 for 20%
//        double downPaymentAmountFromRate = totalDiscount * downPaymentRate;
//
//        // Subtract down payment amount (fixed)
//        double downPaymentAmount = Master().getDownPaymentRatesAmount().doubleValue();
//        double totalDownPayment = downPaymentAmountFromRate + downPaymentAmount;
        // Final net total
//        double netTotal = totalAmount - totalDiscount - totalDownPayment;
//
        double netTotal = totalAmount - totalDiscount;
        Master().setNetTotal(netTotal);

    }

    public JSONObject netTotalChecker(int pnRow) {
        poJSON = new JSONObject();
        double NetTotl = Master().getNetTotal().doubleValue() + (Detail(pnRow).getQuantity().doubleValue() * Detail(pnRow).getUnitPrice().doubleValue());
        if (NetTotl >= 100000000.0000) {
            poJSON.put("result", "error");
            poJSON.put("message", "The net total exceeds the maximum allowed amount. Please reduce the value and try again.");
            Detail(pnRow).setQuantity(0);
            computeNetTotal();
            return poJSON;
        }
        poJSON.put("result", "success");
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
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
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
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
            return poJSON;
        }

        poJSON.put("result", "success");
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
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
            return poJSON;
        }

        poJSON.put("result", "success");
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

            System.out.println("Company Address : " + Master().Company().getCompanyAddress());
            System.out.println("Company Town : " + Master().Company().TownCity().getDescription());
            System.out.println("Company Province " + Master().Company().TownCity().Province().getDescription());
            System.out.println("Branch Address : " + Master().Branch().getAddress());
            System.out.println("Branch Town : " + Master().Branch().TownCity().getDescription());
            System.out.println("Branch Province " + Master().Branch().TownCity().Province().getDescription());

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
//            parameters.put("sCompnyNm", "Prepared by: "+ poGRider.getLogName()+ " " + poGRider.getServerDate()); //poGRider.getClientName()
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
                System.out.println("------------------------------------------------------");
                System.out.println("Row : " + (lnCtr + 1));
                System.out.println("Barcode : " + safeString(Detail(lnCtr).Inventory().getBarCode()));
                System.out.println("Brand : " + safeString(Detail(lnCtr).Inventory().Brand().getDescription()));
                System.out.println("Model : " + safeString(Detail(lnCtr).Inventory().Model().getDescription()));
                System.out.println("Variant : " + safeString(Detail(lnCtr).Inventory().Variant().getDescription()));
                System.out.println("Year : " + safeString(Detail(lnCtr).Inventory().Variant().getYearModel()));
                System.out.println("Measure : " + safeString(Detail(lnCtr).Inventory().Measure().getDescription()));
                System.out.println("Color : " + safeString(Detail(lnCtr).Inventory().Color().getDescription()));
                System.out.println("Description : " + safeString(Detail(lnCtr).Inventory().getDescription()));
                System.out.println("------------------------------------------------------");

                lnTotal = Detail(lnCtr).getUnitPrice().doubleValue() * Detail(lnCtr).getQuantity().doubleValue();
                lsFullDesc = safeString(Detail(lnCtr).Inventory().getDescription());
                switch (jasperType) {
                    case PurchaseOrderStaticData.Printing_CARSp_MCSp_General:
                    case PurchaseOrderStaticData.Printing_Pedritos:
                        lsFullDesc = safeString(Detail(lnCtr).Inventory().Brand().getDescription());
                        if (lsFullDesc.isEmpty()) {
                            lsFullDesc = safeString(Detail(lnCtr).Inventory().Model().getDescription());
                        } else {
                            lsFullDesc = lsFullDesc + " " + safeString(Detail(lnCtr).Inventory().Model().getDescription());
                        }

                        if (lsFullDesc.isEmpty()) {
                            lsFullDesc = safeString(Detail(lnCtr).Inventory().getDescription());
                        } else {
                            lsFullDesc = lsFullDesc + " " + safeString(Detail(lnCtr).Inventory().getDescription());
                        }
                        break;
                    default:
                        throw new AssertionError();
                }

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
            String jrxmlPath = "";
            switch (jasperType) {
                case PurchaseOrderStaticData.Printing_CAR_MC_MPUnit_Appliance:
                    jrxmlPath = System.getProperty("sys.default.path.config") + "/reports/" + "PurchaseOrderCarMcMPUnitAppliance.jrxml"; //TODO
                    break;
                case PurchaseOrderStaticData.Printing_CARSp_MCSp_General:
                    jrxmlPath = System.getProperty("sys.default.path.config") + "/reports/" + "PurchaseOrderCARSpMCSpGeneral.jrxml"; //TODO PurchaseOrderPedritos
                    break;
                case PurchaseOrderStaticData.Printing_Pedritos:
                    jrxmlPath = System.getProperty("sys.default.path.config") + "/reports/" + "PurchaseOrderPedritos.jrxml"; //TODO PurchaseOrderPedritos
                    break;
                default:
                    throw new AssertionError();
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
                    .getLogger(PurchaseOrder.class
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

                                //if ever na kailangan e hide si button save
//                                if (button.getToolTipText() != null) {
//                                    if (button.getToolTipText().equals("Save")) {
//                                        button.setEnabled(false);  // Disable instead of hiding
//                                        button.setVisible(false);  // Hide it completely
//                                    }
//                                }
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
                                            Logger.getLogger(PurchaseOrder.class.getName()).log(Level.SEVERE, null, ex);
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

    /**
     * Checks whether a project has already been used in transactions.
     *
     * <p>
     * This method queries the database to determine how many times the project
     * identified by <code>fsProjectCode</code> appears in the
     * <code>po_master</code> table, joined with the <code>project</code> table.
     * The result is returned as a {@link JSONObject} containing the usage count
     * and a status message.</p>
     *
     * <p>
     * Behavior:</p>
     * <ul>
     * <li>If the project is used in more than one transaction (count &gt; 1),
     * the result will indicate an "error" with a message stating that the
     * project is already used.</li>
     * <li>If the project is used in zero or one transaction (count &le; 1), the
     * result will indicate "success" with a message that no duplicate usage is
     * found.</li>
     * </ul>
     *
     * <p>
     * The returned {@link JSONObject} has the following structure:</p>
     * <pre>
     * {
     *     "result": "success" | "error",
     *     "message": "Description of the result",
     *     "count": n  // Number of times the project is used
     * }
     * </pre>
     *
     * @param fsProjectCode the unique project code to check for usage
     * @return a {@link JSONObject} containing the result ("success" or
     * "error"), a descriptive message, and the usage count
     * @throws SQLException if a database access error occurs or the SQL query
     * fails
     * @throws GuanzonException if a custom business logic exception occurs
     * during processing
     *
     * <p>
     * Example usage:</p>
     * <pre>
     * JSONObject usage = CheckProjectUsage("GK0126000003");
     * int count = Integer.parseInt(usage.get("count").toString());
     * if ("error".equals(usage.get("result"))) {
     *     // handle error
     * } else {
     *     // proceed
     * }
     * </pre>
     *
     * <p>
     * Notes:</p>
     * <ul>
     * <li>This method uses a COUNT(*) query for performance instead of fetching
     * all rows.</li>
     * <li>The join with the <code>project</code> table ensures the project
     * exists and allows for potential future filtering based on project
     * attributes.</li>
     * <li>Logging is done via <code>System.out.println</code> for debugging SQL
     * execution.</li>
     * </ul>
     *
     * @author TEEJEI DE CELIS
     * @date 2026-04-09
     */
    public JSONObject CheckProjectUsage(String fsProjectCode) throws SQLException, GuanzonException {
        poJSON = new JSONObject();

        String lsSQL = "SELECT COUNT(*) AS nCount "
                + "FROM PO_Master a "
                + "LEFT JOIN Project b ON a.sReferNox = b.sProjCode "
                + "WHERE a.sReferNox = " + SQLUtil.toSQL(fsProjectCode);

        System.out.println("EXECUTING SQL: " + lsSQL);
        ResultSet rs = poGRider.executeQuery(lsSQL);

        if (rs.next()) {
            int lnCount = rs.getInt("nCount");

            if (lnCount > 1) {
                poJSON.put("result", "error");
                poJSON.put("message", "Project is already used in multiple transactions.");
                poJSON.put("count", lnCount);
            } else {
                poJSON.put("result", "success");
                poJSON.put("message", "No usage found. You can proceed.");
                poJSON.put("count", lnCount);
            }
        }

        rs.close();
        return poJSON;
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
                poJSON.put("result", "error");
                poJSON.put("message", "Query execution failed.");
                return poJSON;
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
                poJSON.put("result", "error");
                poJSON.put("message", "Query execution failed.");
                return poJSON;
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

//            parameters.put("sIndustry", Master().Industry().getDescription());
//            parameters.put("sCompany", Master().Company().getCompanyName());
//            parameters.put("sBranch", Master().Branchx().getDescription());

            List<Reports> reportList = new ArrayList<>();

            for (int i = 0; i < reportData.size(); i++) {

                JSONObject obj = (JSONObject) reportData.get(i);

                if (isSummarized) {
                    System.out.println("reportData : " +  obj);
                    
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
                     System.out.println("DETAIL : " + i+ 1 +  obj);
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
                   parseDouble(obj.get("UnitPrce")),
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

            

        } catch (Exception ex) {
            poJSON.put("result", "error");
            poJSON.put("message", ex.getMessage());
        }
        poJSON.put("result", "success");
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

            System.out.println("Company Address : " + Master().Company().getCompanyAddress());
            System.out.println("Company Town : " + Master().Company().TownCity().getDescription());
            System.out.println("Company Province " + Master().Company().TownCity().Province().getDescription());
            System.out.println("Branch Address : " + Master().Branch().getAddress());
            System.out.println("Branch Town : " + Master().Branch().TownCity().getDescription());
            System.out.println("Branch Province " + Master().Branch().TownCity().Province().getDescription());

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
                    System.out.println("reportData : " +  obj);
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
                    System.out.println("reportData : " +  obj);
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
                    .getLogger(PurchaseOrder.class
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

    public JSONObject addAttachment()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        if (paAttachments.isEmpty()) {
            paAttachments.add(TransactionAttachment());
            poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).newRecord();
        } else {
            if (!paAttachments.get(paAttachments.size() - 1).getModel().getTransactionNo().isEmpty()) {
                paAttachments.add(TransactionAttachment());
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to add transaction attachment.");
                return poJSON;
            }
        }
        poJSON.put("result", "success");
        return poJSON;
    }
    
    public JSONObject removeAttachment(int fnRow) throws GuanzonException, SQLException{
        poJSON = new JSONObject();
        if(getTransactionAttachmentCount() <= 0){
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction attachment to be removed.");
            return poJSON;
        }
        
        if(paAttachments.get(fnRow).getEditMode() == EditMode.ADDNEW){
            paAttachments.remove(fnRow);
            System.out.println("Attachment :"+ fnRow+" Removed");
        } else {
            paAttachments.get(fnRow).getModel().setRecordStatus(RecordStatus.INACTIVE);
            System.out.println("Attachment :"+ fnRow+" Deactivate");
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    public int addAttachment(String fFileName) throws SQLException, GuanzonException{
        for(int lnCtr = 0;lnCtr <= getTransactionAttachmentCount() - 1;lnCtr++){
            if(fFileName.equals(paAttachments.get(lnCtr).getModel().getFileName())
                && RecordStatus.INACTIVE.equals(paAttachments.get(lnCtr).getModel().getRecordStatus())){
                paAttachments.get(lnCtr).getModel().setRecordStatus(RecordStatus.ACTIVE);
                System.out.println("Attachment :"+ lnCtr+" Activate");
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
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getTransactionNo());
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceNo());
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceCode());
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getFileName());
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
                    System.out.println("poJSON success: " +  poJSON.toJSONString());
                    System.out.println("File downloaded succesfully.");
                } else {
                    poJSON = (JSONObject) poJSON.get("error");
                    poJSON.put("result", "error");
                    System.out.println("ERROR WebFile.DownloadFile: " + poJSON.get("message"));
                    System.out.println("poJSON error WebFile.DownloadFile: " + poJSON.toJSONString());
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
        if ("success".equals((String) poJSON.get("result"))) {
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
