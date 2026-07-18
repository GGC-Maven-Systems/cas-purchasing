/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.controller;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javax.script.ScriptException;
import javax.sql.rowset.CachedRowSet;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.swing.JRViewerToolbar;
import net.sf.jasperreports.view.JasperViewer;
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
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.client.Client;
import org.guanzon.cas.client.account.AP_Client_Master;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.inv.InvTransCons;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.Brand;
import org.guanzon.cas.parameter.Category;
import org.guanzon.cas.parameter.CategoryLevel2;
import org.guanzon.cas.parameter.Company;
import org.guanzon.cas.parameter.Department;
import org.guanzon.cas.parameter.Industry;
import org.guanzon.cas.parameter.ModelVariant;
import org.guanzon.cas.parameter.Term;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.guanzon.cas.purchasing.model.Model_PO_Quotation_Request_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Quotation_Request_Master;
import org.guanzon.cas.purchasing.model.Model_PO_Quotation_Request_Supplier;
import org.guanzon.cas.purchasing.services.QuotationControllers;
import org.guanzon.cas.purchasing.services.QuotationModels;
import org.guanzon.cas.purchasing.status.POQuotationRequestStatus;
import org.guanzon.cas.purchasing.validator.POQuotationRequestValidatorFactory;
import org.json.simple.JSONArray;
import ph.com.guanzongroup.cas.cashflow.DisbursementVoucher;
import ph.com.guanzongroup.cas.cashflow.model.Model_Disbursement_Master;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.DisbursementStatic;
import ph.com.guanzongroup.cas.cashflow.utility.CustomCommonUtil;

/**
 *
 * @author Arsiela
 */
public class POQuotationRequest extends Transaction {
    private String psIndustryId = "";
    private String psCompanyId = "";
    private String psCategorCd = "";
    
    private String psSearchCompany, psSearchIndustry, psSearchCategory, psSearchBranch, psSearchDestination, psSearchDepartment, psSearchSupplier;
    
    List<Model_PO_Quotation_Request_Master> paMasterList;
    List<Model> paDetailRemoved;
    List<Model> paSuppliersRemoved;
    List<Model_PO_Quotation_Request_Supplier> paSuppliers;
    
    public JSONObject InitTransaction() {
        SOURCE_CODE = InvTransCons.PURCHASE_ORDER_QUOTATION_REQUEST;

        poMaster = new QuotationModels(poGRider).POQuotationRequestMaster();
        poDetail = new QuotationModels(poGRider).POQuotationRequestDetails();

        paMasterList = new ArrayList<>();
        paDetail = new ArrayList<>();
        paDetailRemoved = new ArrayList<>();
        paSuppliers = new ArrayList<>();
        paSuppliersRemoved = new ArrayList<>();

        return initialize();
    }

    public JSONObject NewTransaction()
            throws CloneNotSupportedException, SQLException, GuanzonException {
        if(!pbWthParent){
            //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
            poJSON = seekApproval();
            if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                return poJSON;
            }
        }
        
        return newTransaction();
    }

    public JSONObject SaveTransaction()
            throws SQLException,
            GuanzonException,
            CloneNotSupportedException {
        return saveTransaction();
    }

    public JSONObject OpenTransaction(String transactionNo)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        if(!pbWthParent){
            //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
            poJSON = seekApproval();
            if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                return poJSON;
            }
        }
        
        //Clear data
        resetMaster();
        resetOthers();
        Detail().clear();
        return openTransaction(transactionNo);
    }

    public JSONObject UpdateTransaction() throws SQLException, GuanzonException {
        if(!pbWthParent){
            //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
            poJSON = seekApproval();
            if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                return poJSON;
            }
        }
        
        return updateTransaction();
    }
    
    /**
     * Seek Approval method 
     * @return JSON
     * @throws SQLException
     * @throws GuanzonException 
     */
    public JSONObject seekApproval() 
            throws SQLException, SQLException, GuanzonException{
        poJSON = new JSONObject();
        //Moved only the script for seeking of approval - Arsiela 10-15-2025 - 14:11:01
            
        //load authorization manager that evaluates current users authority for this process
        ActionAuthManager loAuth = new ActionAuthManager(poGRider, "cas-purchasing");
        poJSON = loAuth.isAuthorized();

        //check if currenty user is authorized
        if(!((String)poJSON.get("result")).equalsIgnoreCase("true")){
            //show process needs authorization
            ShowMessageFX.Warning((String)poJSON.get("warning"), "Authorization Required", null);
            //get authorization from authoried personnel
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if("error".equals((String)poJSON.get("result"))){
                return poJSON;
            }

            //check if approving officer is authorized
            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
            int lnUserLevl = Integer.parseInt(poJSON.get("nUserLevl").toString());
            poJSON = loAuth.isAuthorized(lsUserIDxx, lnUserLevl);

            //if approving is not authorized then do not continue process
            if(!((String)poJSON.get("result")).equalsIgnoreCase("true")){
                ShowMessageFX.Warning((String)poJSON.get("warning"), "Authorization Required", null);
                 poJSON.put("result", "error");
                 poJSON.put("message", "User is not an authorized approving officer..");
                 return poJSON;
            }
            setApproving(lsUserIDxx);
        }  
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    public JSONObject ConfirmTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = POQuotationRequestStatus.CONFIRMED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        MatrixAuthChecker check = null; 
        
        if(!pbWthParent){
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            //Check if there is a authorization request
            if(loMatrix != null){
                //initialized MatrixAuthChecker object
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if(!check.isAuthOkay()){
                    //check if authorization request allows system approval
                    if(!check.isAllowSys()){
                        //extract the JSONObject from JSONArray
                        JSONObject loJson = (JSONObject)loMatrix.get(0);

                        //check if current user is authorized to approved this transaction
                        poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                        
                        //If not authorized/request system approval
                        if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if("error".equals((String)poJSON.get("result"))){
                                return poJSON;
                            }

                            //check if approving officer is authorized
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                            //user is not authorized
                            if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                                return poJSON;
                            }
                            setApproving(lsUserIDxx);
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if(!check.isAuthOkay()){
                        //check  the user level again then if he/she allow to approve
                        poGRider.beginTrans("UPDATE STATUS", "Confirm Transaction", SOURCE_CODE, Master().getTransactionNo());

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();
                        
                        poJSON.put("result", "matrix");
                        return poJSON;
                    }
                }
            }
            //there are no authorization event request
            else{
                //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
                poJSON = seekApproval();
                if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                    return poJSON;
                }
            }
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if(check != null){
            check.postAuth();
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction confirmed successfully.");
        return poJSON;
    }
    
    public JSONObject ApproveTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = POQuotationRequestStatus.APPROVED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        MatrixAuthChecker check = null; 
        
        if(!pbWthParent){
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            //Check if there is a authorization request
            if(loMatrix != null){
                //initialized MatrixAuthChecker object
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if(!check.isAuthOkay()){
                    //check if authorization request allows system approval
                    if(!check.isAllowSys()){
                        //extract the JSONObject from JSONArray
                        JSONObject loJson = (JSONObject)loMatrix.get(0);

                        //check if current user is authorized to approved this transaction
                        poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                        
                        //If not authorized/request system approval
                        if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if("error".equals((String)poJSON.get("result"))){
                                return poJSON;
                            }

                            //check if approving officer is authorized
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                            //user is not authorized
                            if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                                return poJSON;
                            }
                            setApproving(lsUserIDxx);
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if(!check.isAuthOkay()){
                        //check  the user level again then if he/she allow to approve
                        poGRider.beginTrans("UPDATE STATUS", "Approve Transaction", SOURCE_CODE, Master().getTransactionNo());

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();
                        
                        poJSON.put("result", "matrix");
                        return poJSON;
                    }
                }
            }
            //there are no authorization event request
            else{
                //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
                poJSON = seekApproval();
                if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                    return poJSON;
                }
            }
        }

        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if(check != null){
            check.postAuth();
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction approved successfully.");
        return poJSON;
    }
    
    public JSONObject PostTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = POQuotationRequestStatus.POSTED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        MatrixAuthChecker check = null; 

        if(!pbWthParent){
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            //Check if there is a authorization request
            if(loMatrix != null){
                //initialized MatrixAuthChecker object
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if(!check.isAuthOkay()){
                    //check if authorization request allows system approval
                    if(!check.isAllowSys()){
                        //extract the JSONObject from JSONArray
                        JSONObject loJson = (JSONObject)loMatrix.get(0);

                        //check if current user is authorized to approved this transaction
                        poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                        
                        //If not authorized/request system approval
                        if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if("error".equals((String)poJSON.get("result"))){
                                return poJSON;
                            }

                            //check if approving officer is authorized
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                            //user is not authorized
                            if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                                return poJSON;
                            }
                            setApproving(lsUserIDxx);
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if(!check.isAuthOkay()){
                        //check  the user level again then if he/she allow to approve
                        poGRider.beginTrans("UPDATE STATUS", "Post Transaction", SOURCE_CODE, Master().getTransactionNo());

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();
                        
                        poJSON.put("result", "matrix");
                        return poJSON;
                    }
                }
            }
            //there are no authorization event request
            else{
                //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
                poJSON = seekApproval();
                if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                    return poJSON;
                }
            }
        }
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if(check != null){
            check.postAuth();
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction posted successfully.");

        return poJSON;
    }
    
    public JSONObject VoidTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = POQuotationRequestStatus.VOID;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        MatrixAuthChecker check = null; 
        
        if(!pbWthParent){
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            //Check if there is a authorization request
            if(loMatrix != null){
                //initialized MatrixAuthChecker object
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if(!check.isAuthOkay()){
                    //check if authorization request allows system approval
                    if(!check.isAllowSys()){
                        //extract the JSONObject from JSONArray
                        JSONObject loJson = (JSONObject)loMatrix.get(0);

                        //check if current user is authorized to approved this transaction
                        poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                        
                        //If not authorized/request system approval
                        if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if("error".equals((String)poJSON.get("result"))){
                                return poJSON;
                            }

                            //check if approving officer is authorized
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                            //user is not authorized
                            if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                                return poJSON;
                            }
                            setApproving(lsUserIDxx);
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if(!check.isAuthOkay()){
                        //check  the user level again then if he/she allow to approve
                        poGRider.beginTrans("UPDATE STATUS", "Void Transaction", SOURCE_CODE, Master().getTransactionNo());

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();
                        
                        poJSON.put("result", "matrix");
                        return poJSON;
                    }
                }
            }
            //there are no authorization event request
            else{
                //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
                poJSON = seekApproval();
                if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                    return poJSON;
                }
            }
        }
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if(check != null){
            check.postAuth();
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction voided successfully.");
        return poJSON;
    }
    
    public JSONObject CancelTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = POQuotationRequestStatus.CANCELLED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        MatrixAuthChecker check = null; 
        
        if(!pbWthParent){
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            //Check if there is a authorization request
            if(loMatrix != null){
                //initialized MatrixAuthChecker object
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if(!check.isAuthOkay()){
                    //check if authorization request allows system approval
                    if(!check.isAllowSys()){
                        //extract the JSONObject from JSONArray
                        JSONObject loJson = (JSONObject)loMatrix.get(0);

                        //check if current user is authorized to approved this transaction
                        poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                        
                        //If not authorized/request system approval
                        if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if("error".equals((String)poJSON.get("result"))){
                                return poJSON;
                            }

                            //check if approving officer is authorized
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                            //user is not authorized
                            if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                                return poJSON;
                            }
                            setApproving(lsUserIDxx);
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if(!check.isAuthOkay()){
                        //check  the user level again then if he/she allow to approve
                        poGRider.beginTrans("UPDATE STATUS", "Cancel Transaction", SOURCE_CODE, Master().getTransactionNo());

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();
                        
                        poJSON.put("result", "matrix");
                        return poJSON;
                    }
                }
            }
            //there are no authorization event request
            else{
                //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
                poJSON = seekApproval();
                if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                    return poJSON;
                }
            }
        }
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if(check != null){
            check.postAuth();
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction cancelled successfully.");
        return poJSON;
    }
    
    public JSONObject DisApprovedTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = POQuotationRequestStatus.CANCELLED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        MatrixAuthChecker check = null; 
        
        if(!pbWthParent){
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            //Check if there is a authorization request
            if(loMatrix != null){
                //initialized MatrixAuthChecker object
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if(!check.isAuthOkay()){
                    //check if authorization request allows system approval
                    if(!check.isAllowSys()){
                        //extract the JSONObject from JSONArray
                        JSONObject loJson = (JSONObject)loMatrix.get(0);

                        //check if current user is authorized to approved this transaction
                        poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                        
                        //If not authorized/request system approval
                        if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if("error".equals((String)poJSON.get("result"))){
                                return poJSON;
                            }

                            //check if approving officer is authorized
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                            //user is not authorized
                            if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                                return poJSON;
                            }
                            setApproving(lsUserIDxx);
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if(!check.isAuthOkay()){
                        //check  the user level again then if he/she allow to approve
                        poGRider.beginTrans("UPDATE STATUS", "Disapprove Transaction", SOURCE_CODE, Master().getTransactionNo());

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();
                        
                        poJSON.put("result", "matrix");
                        return poJSON;
                    }
                }
            }
            //there are no authorization event request
            else{
                //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
                poJSON = seekApproval();
                if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                    return poJSON;
                }
            }
        }
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if(check != null){
            check.postAuth();
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction dis-approved successfully.");
        return poJSON;
    }
    
    public JSONObject ReturnTransaction(String remarks)
            throws ParseException,
            SQLException,
            GuanzonException,
            CloneNotSupportedException {
        poJSON = new JSONObject();
        String lsStatus = POQuotationRequestStatus.RETURNED;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if(!pbWthParent){
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            //Check if there is a authorization request
            if(loMatrix != null){
                //initialized MatrixAuthChecker object
                MatrixAuthChecker check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if(!check.isAuthOkay()){
                    //check if authorization request allows system approval
                    if(!check.isAllowSys()){
                        //extract the JSONObject from JSONArray
                        JSONObject loJson = (JSONObject)loMatrix.get(0);

                        //check if current user is authorized to approved this transaction
                        poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                        
                        //If not authorized/request system approval
                        if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if("error".equals((String)poJSON.get("result"))){
                                return poJSON;
                            }

                            //check if approving officer is authorized
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                            //user is not authorized
                            if(!"success".equalsIgnoreCase((String)poJSON.get("result"))){
                                return poJSON;
                            }
                            setApproving(lsUserIDxx);
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if(!check.isAuthOkay()){
                        //check  the user level again then if he/she allow to approve
                        poGRider.beginTrans("UPDATE STATUS", "Return Transaction", SOURCE_CODE, Master().getTransactionNo());

                        System.out.println("Status: " + lsStatus);
                        lsStatus = Character.toString((char)(64 + Integer.parseInt(lsStatus)));
                        System.out.println("Status: " + lsStatus);
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();
                        
                        poJSON.put("result", "success");
                        return poJSON;
                    }
                }
            }
            //there are no authorization event request
            else{
                //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
                poJSON = seekApproval();
                if("error".equalsIgnoreCase((String)poJSON.get("result"))){
                    return poJSON;
                }
            }
        }
        
        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, false);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction returned successfully.");
        return poJSON;
    }
    
    /*Search References*/
    public JSONObject searchTransaction()
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat != null) {
            if (psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                    lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                }
                lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
            } else {
                lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
            }
        }

        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                                            + " AND a.sCategrCd = " + SQLUtil.toSQL(psCategorCd)
                                            + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode()));
        
        if (lsTransStat != null && !"".equals(lsTransStat)) {
            lsSQL = lsSQL + lsTransStat;
        }

        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction Date»Transaction No»Branch»Department»Category",
                "dTransact»sTransNox»Branch»Department»Category2", 
                "a.dTransact»a.sTransNox»c.sBranchNm»d.sDeptName»f.sDescript", 
                0);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }
    
    public JSONObject searchTransaction(String fsBranch, String fsDepartment, String fsCateogry, String fsTransactionDate,String fsTransactionNo)
            throws CloneNotSupportedException,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat != null) {
            if (psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                    lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                }
                lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
            } else {
                lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
            }
        }
        
        String lsBranch = fsBranch != null && !"".equals(fsBranch) 
                                                    ? " AND c.sBranchNm LIKE " + SQLUtil.toSQL("%"+fsBranch)
                                                    : "";
        
        String lsDepartment = fsDepartment != null && !"".equals(fsDepartment) 
                                                    ? " AND d.sDeptName LIKE " + SQLUtil.toSQL("%"+fsDepartment)
                                                    : "";
        
        String lsCategory = fsCateogry != null && !"".equals(fsCateogry) 
                                                    ? " AND f.sDescript LIKE " + SQLUtil.toSQL("%"+fsCateogry)
                                                    : "";
        
        String lsTransactionDate = fsTransactionDate != null && !"".equals(fsTransactionDate) && !"1900-01-01".equals(fsTransactionDate) 
                                                    ? " AND a.dTransact = " + SQLUtil.toSQL(fsTransactionDate)
                                                    : "";
        
        String lsTransactionNo = fsTransactionNo != null && !"".equals(fsTransactionNo) 
                                                    ? " AND a.sTransNox LIKE " + SQLUtil.toSQL("%"+fsTransactionNo)
                                                    : "";

        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, 
                   " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                 + " AND a.sCategrCd = " + SQLUtil.toSQL(psCategorCd)
                 + " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode())
                 + lsBranch
                 + lsDepartment
                 + lsCategory
                 + lsTransactionDate
                 + lsTransactionNo
                );
        
        if (lsTransStat != null && !"".equals(lsTransStat)) {
            lsSQL = lsSQL + lsTransStat;
        }

        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction Date»Transaction No»Branch»Department»Category",
                "dTransact»sTransNox»Branch»Department»Category2", 
                "a.dTransact»a.sTransNox»c.sBranchNm»d.sDeptName»f.sDescript", 
                1);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }
    
    public JSONObject SearchDestination(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setDestination(object.getModel().getBranchCode());
        }
        return poJSON;
    }
     
    public JSONObject SearchBranch(String value, boolean byCode, boolean isSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            if(isSearch){
                setSearchBranch(object.getModel().getBranchName());
            } else {
                Master().setBranchCode(object.getModel().getBranchCode());
            }
        }

        return poJSON;
    }
    
    public JSONObject SearchDepartment(String value, boolean byCode, boolean isSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Department object = new ParamControllers(poGRider, logwrapr).Department();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            if(isSearch){
                setSearchDepartment(object.getModel().getDescription());
            } else {
                Master().setDepartmentId(object.getModel().getDepartmentId());
            }
        }
        return poJSON;
    }
    
    public JSONObject SearchCategory(String value, boolean byCode, boolean isSearch) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        
//        CategoryLevel2 object = new ParamControllers(poGRider, logwrapr).CategoryLevel2();
//        object.setRecordStatus(RecordStatus.ACTIVE);
//
//        poJSON = object.searchRecord(value, byCode);
//        if ("success".equals((String) poJSON.get("result"))) {
//            if(isSearch){
//                setSearchCategory(object.getModel().getDescription());
//            } else {
//                System.out.println("Category ID: " + object.getModel().getCategoryId());
//                System.out.println("Description " + object.getModel().getDescription());
//                Master().setCategoryLevel2(object.getModel().getCategoryId());
//            }
//        }
        
        
        CategoryLevel2 object = new ParamControllers(poGRider, logwrapr).CategoryLevel2();
        String lsSQL = MiscUtil.addCondition(object.getSQ_Browse(), "cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                                            + " AND (sIndstCdx = "+SQLUtil.toSQL(Master().getIndustryId())+" OR ISNULL(sIndstCdx))"); //+ SQLUtil.toSQL(Master().getIndustryId()));
        
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                value,
                "Category ID»Description",
                "sCategrCd»sDescript",
                "sCategrCd»sDescript",
                byCode ? 0 : 1);

        if (poJSON != null) {
            poJSON = object.getModel().openRecord((String) poJSON.get("sCategrCd"));
            if ("success".equals((String) poJSON.get("result"))) {
                if(isSearch){
                    setSearchCategory(object.getModel().getDescription());
                } else {
                    System.out.println("Category2 ID: " + object.getModel().getCategoryId());
                    System.out.println("Description " + object.getModel().getDescription());
                    Master().setCategoryLevel2(object.getModel().getCategoryId());
                }
            }
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
        }
        
        return poJSON;
    }
    
    public JSONObject SearchBrand(String value, boolean byCode, int row)
            throws ExceptionInInitializerError,
            SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        
        if(Master().getCategoryLevel2()== null || "".equals(Master().getCategoryLevel2())){
            poJSON.put("result", "error");
            poJSON.put("message", "Category is not set.");
            return poJSON;
        }
        
        Brand object = new ParamControllers(poGRider, logwrapr).Brand();
        object.setRecordStatus(RecordStatus.ACTIVE);

//        poJSON = object.searchRecord(value, byCode, Master().getIndustryId());
        poJSON = object.searchRecord(value, byCode,"09"); //Fix industry code "09" for GENERAL Only
        if ("success".equals((String) poJSON.get("result"))) {
            if (!object.getModel().getBrandId().equals(Detail(row).getBrandId())) {
                Detail(row).setModelId("");
                Detail(row).setStockId("");
                Detail(row).setDescription("");
            }

            Detail(row).setBrandId(object.getModel().getBrandId());
        }
        return poJSON;
    }

    public JSONObject SearchModel(String value, boolean byCode, int row)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);

        if(Detail(row).getBrandId() == null || "".equals(Detail(row).getBrandId())){
            poJSON.put("result", "error");
            poJSON.put("message", "Brand is not set.");
            return poJSON;
        }
        
        ModelVariant object = new ParamControllers(poGRider, logwrapr).ModelVariant();
        object.setRecordStatus(RecordStatus.ACTIVE);
        System.out.println("Brand ID : "  + Detail(row).getBrandId());
        poJSON = object.searchRecordByModel(value, byCode, Detail(row).getBrandId());
        poJSON.put("row", row);
        if ("success".equals((String) poJSON.get("result"))) {
            if (!object.getModel().getModelId().equals(Detail(row).getModelId())) {
                Detail(row).setStockId("");
                Detail(row).setDescription("");
            }
            
            Detail(row).setModelId(object.getModel().getModelId());
        }

        return poJSON;
    }
    
    public JSONObject SearchInventory(String value, boolean byCode, int row) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        
        if(Master().getCategoryLevel2()== null || "".equals(Master().getCategoryLevel2())){
            poJSON.put("result", "error");
            poJSON.put("message", "Category is not set.");
            return poJSON;
        }
        
        String lsBrand = Detail(row).getBrandId() != null && !"".equals(Detail(row).getBrandId()) 
                                                    ? " AND a.sBrandIDx = " + SQLUtil.toSQL(Detail(row).getBrandId())
                                                    : "";
        String lsModel = Detail(row).getModelId()!= null && !"".equals(Detail(row).getModelId()) 
                                                    ? " AND a.sModelIDx = " + SQLUtil.toSQL(Detail(row).getModelId())
                                                    : "";
        String lsCategory2 = Master().getCategoryLevel2() != null && !"".equals(Master().getCategoryLevel2()) 
                                                    ? " AND a.sCategCd2 = " + SQLUtil.toSQL(Master().getCategoryLevel2())
                                                    : "";
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);
        String lsSQL = MiscUtil.addCondition(object.getSQ_Browse(), 
                                             //" a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                                            " a.sCategCd1 = " + SQLUtil.toSQL(Master().getCategoryCode())
                                            + lsCategory2
                                            + lsBrand
                                            + lsModel
                                            );
        lsSQL = lsSQL + " GROUP BY a.sStockIDx ";
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                value,
                "Barcode»Description»Brand»Model»Variant»UOM",
                "sBarCodex»sDescript»xBrandNme»xModelNme»xVrntName»xMeasurNm",
                "a.sBarCodex»TRIM(a.sDescript)»IFNULL(b.sDescript, '')»IFNULL(c.sDescript, '')»IFNULL(f.sDescript, '')»IFNULL(e.sDescript, '')",
                byCode ? 0 : 1);

        if (poJSON != null) {
            poJSON = object.getModel().openRecord((String) poJSON.get("sStockIDx"));
            if ("success".equals((String) poJSON.get("result"))) {
                JSONObject loJSON = checkExistingDetail(row,
                        object.getModel().getStockId(),
                        object.getModel().getDescription().trim(), 
                        true
                        );
                if ("error".equals((String) loJSON.get("result"))) {
                    if((boolean) loJSON.get("reverse")){
                        return loJSON;
                    } else {
                        row = (int) loJSON.get("row");
                        Detail(row).isReverse(true);
                    }
                }

                Detail(row).setStockId(object.getModel().getStockId());
                Detail(row).setDescription(object.getModel().getDescription().trim());
                Detail(row).setBrandId(object.getModel().getBrandId());
                Detail(row).setModelId(object.getModel().getModelId());
                   
                if(object.getModel().getSellingPrice() != null){
                    Detail(row).setUnitPrice(object.getModel().getSellingPrice().doubleValue());
                } else {
                    Detail(row).setUnitPrice(0.0000);
                }
            } 
            
            System.out.println("Barcode : " + Detail(row).Inventory().getBarCode());
            System.out.println("Description : " + Detail(row).Inventory().getDescription());
            System.out.println("Category : " + Master().Category2().getDescription());
            System.out.println("Brand : " + Detail(row).Brand().getDescription());
            System.out.println("Model : " + Detail(row).Model().getDescription());
            
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
        }
        
        if ("error".equals((String) poJSON.get("result"))) {
            if(!"".equals(value)){
                poJSON = checkExistingDetail(row,
                        "",
                        value,
                        false
                        );
                if ("error".equals((String) poJSON.get("result"))) {
                    if((boolean) poJSON.get("reverse")){
                        return poJSON;
                    } else {
                        row = (int) poJSON.get("row");
                        Detail(row).isReverse(true);
                    }
                }
            }

            Detail(row).setStockId("");
            Detail(row).setBrandId("");
            Detail(row).setModelId("");
            Detail(row).setDescription(value);
            Detail(row).setUnitPrice(0.0000);
        }
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        poJSON.put("row", row);
        return poJSON;
    }
    
    public JSONObject SearchSupplier(String value, boolean byCode, int row) throws SQLException, GuanzonException {
        
        AP_Client_Master object = new ClientControllers(poGRider, logwrapr).APClientMaster();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            JSONObject loJSON = checkExistingSupplier(row,
                    object.getModel().getClientId(),
                    POQuotationRequestSupplierList(row).getCompanyId()
                    );
            if ("error".equals((String) loJSON.get("result"))) {
                if((boolean) loJSON.get("reverse")){
                    return loJSON;
                } else {
                    //reset the current row
                    if( POQuotationRequestSupplierList(row).getEditMode() == EditMode.ADDNEW){
                        POQuotationRequestSupplierList(row).setSupplierId("");
                        POQuotationRequestSupplierList(row).setAddressId("");
                        POQuotationRequestSupplierList(row).setContactId("");
                        POQuotationRequestSupplierList(row).setCompanyId("");
                    }
                    
                    //reverse into true the result row
                    row = (int) loJSON.get("row");
                    POQuotationRequestSupplierList(row).isReverse(true);
                }
            }
            
            POQuotationRequestSupplierList(row).setSupplierId(object.getModel().getClientId());
            POQuotationRequestSupplierList(row).setAddressId(object.getModel().ClientAddress().getAddressId());
            POQuotationRequestSupplierList(row).setContactId(object.getModel().ClientInstitutionContact().getContactPId());
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            poJSON.put("row", row);
        }
        System.out.println("Supplier : " + POQuotationRequestSupplierList(row).Supplier().getCompanyName());
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        poJSON.put("row", row);
        return poJSON;
    }

    public JSONObject SearchTerm(String value, boolean byCode, int row) throws ExceptionInInitializerError, SQLException, GuanzonException {
        poJSON = new JSONObject();
        
        if(POQuotationRequestSupplierList(row).getSupplierId() == null || "".equals(POQuotationRequestSupplierList(row).getSupplierId())){
            poJSON.put("result", "error");
            poJSON.put("message", "Supplier cannot be empty.");
            return poJSON;
        }
        
        Term object = new ParamControllers(poGRider, logwrapr).Term();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            POQuotationRequestSupplierList(row).setTerm(object.getModel().getTermId());
        }

        System.out.println("Term : " + POQuotationRequestSupplierList(row).Term().getTermValue());
        return poJSON;
    }
    
    public JSONObject SearchCompany(String value, boolean byCode, int row) throws ExceptionInInitializerError, SQLException, GuanzonException {
        poJSON = new JSONObject();
        
        if(POQuotationRequestSupplierList(row).getSupplierId() == null || "".equals(POQuotationRequestSupplierList(row).getSupplierId())){
            poJSON.put("result", "error");
            poJSON.put("message", "Supplier cannot be empty.");
            return poJSON;
        }
        
        Company object = new ParamControllers(poGRider, logwrapr).Company();
        object.setRecordStatus(RecordStatus.ACTIVE);
        
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            poJSON = checkExistingSupplier(row,
                    POQuotationRequestSupplierList(row).getSupplierId(),
                    object.getModel().getCompanyId()
                    );
            if ("error".equals((String) poJSON.get("result"))) {
                if((boolean) poJSON.get("reverse")){
                    return poJSON;
                } else {
                    //reset the current row
                    if( POQuotationRequestSupplierList(row).getEditMode() == EditMode.ADDNEW){
                        POQuotationRequestSupplierList(row).setSupplierId("");
                        POQuotationRequestSupplierList(row).setAddressId("");
                        POQuotationRequestSupplierList(row).setContactId("");
                        POQuotationRequestSupplierList(row).setCompanyId("");
                    }
                    
                    //reverse into true the result row
                    row = (int) poJSON.get("row");
                    POQuotationRequestSupplierList(row).isReverse(true);
                }
            }
            POQuotationRequestSupplierList(row).setCompanyId(object.getModel().getCompanyId());
        }
        System.out.println("Company ID : " + POQuotationRequestSupplierList(row).Company().getCompanyId());
        System.out.println("Company : " + POQuotationRequestSupplierList(row).Company().getCompanyName());
        
        poJSON.put("result", "success");
        poJSON.put("message", "success");
        poJSON.put("row", row);
        return poJSON;
    }
    
    /*Validate*/
    public JSONObject checkExistingDetail(int row, String stockId, String description, boolean isSearch){
        JSONObject loJSON = new JSONObject();
        loJSON.put("row", row);
        if(stockId == null){
            stockId = "";
        }
        if(description == null){
            description = "";
        }
        int lnRow = 0;
        for (int lnCtr = 0; lnCtr <= getDetailCount()- 1; lnCtr++) {
            if(Detail(lnCtr).isReverse()){
                lnRow++;
            }
            if (lnCtr != row) {
                //Check Existing Stock ID and Description
                if(!"".equals(stockId) || !"".equals(description)){
                    if((stockId.equals(Detail(lnCtr).getStockId())  && isSearch )
                            || (description.equals(Detail(lnCtr).getDescription()))){
                        if(Detail(lnCtr).isReverse()){
                            loJSON.put("result", "error");
                            loJSON.put("message", "Item Description already exists in the transaction detail at row "+lnRow+".");
                            loJSON.put("reverse", true);
                            loJSON.put("row", lnCtr);
                            return loJSON;
                        } else {
                            loJSON.put("result", "error");
                            loJSON.put("reverse", false);
                            loJSON.put("row", lnCtr);
                            return loJSON;
                        }
                    }
                }    
            }
        }
        
        loJSON.put("result", "success");
        loJSON.put("message", "success");
        return loJSON;
    }
    
    private JSONObject checkExistingSupplier(int row, String supplierId, String compnay){
        JSONObject loJSON = new JSONObject();
        int lnRow = 0;
        for(int lnCtr = 0; lnCtr <= getPOQuotationRequestSupplierCount()- 1; lnCtr++){
            if(POQuotationRequestSupplierList(lnCtr).isReverse()){
                lnRow++;
            }
            if (lnCtr != row) {
                System.out.println("Row : " + lnCtr);
                System.out.println("Supplier : " + POQuotationRequestSupplierList(lnCtr).getSupplierId());
                System.out.println("Company : " + POQuotationRequestSupplierList(lnCtr).getCompanyId());
                
                if (POQuotationRequestSupplierList(lnCtr).getSupplierId().equals(supplierId)
                        && POQuotationRequestSupplierList(lnCtr).getCompanyId().equals(compnay)
                        ) {
                    if(POQuotationRequestSupplierList(lnCtr).isReverse()){
                        loJSON.put("result", "error");
                        loJSON.put("reverse", true);
                        loJSON.put("message", "Supplier with the same company already exists in the table at row "+lnRow+".");
                        loJSON.put("row", lnCtr);
                        return loJSON;
                    } else {
                        loJSON.put("result", "error");
                        loJSON.put("reverse", false);
                        loJSON.put("row", lnCtr);
                        return loJSON;
                    }
                } 
            }
        }
        
        loJSON.put("result", "success");
        loJSON.put("message", "success");
        loJSON.put("row", row);
        return loJSON;
    }
    
    public boolean checkExistStockInventory(){
        for(int lnCtr = 0; lnCtr <= getDetailCount()-1;lnCtr++){
            if(Detail(lnCtr).isReverse()){
                if(Detail(lnCtr).getStockId() != null && !"".equals(Detail(lnCtr).getStockId())){
                    return true;
                }
            }
        }
        return false;
    }
    
    /*Load*/
    public JSONObject loadPOQuotationRequestList(String fsBranch, String fsDepartment, String fsCateogry, Date fdTransactionDate, String fsTransactionNo, boolean isApproval) {
        try {
            
            String lsBranch = fsBranch != null && !"".equals(fsBranch) 
                                                        ? " AND c.sBranchNm LIKE " + SQLUtil.toSQL("%"+fsBranch)
                                                        : "";

            String lsDepartment = fsDepartment != null && !"".equals(fsDepartment) 
                                                        ? " AND d.sDeptName LIKE " + SQLUtil.toSQL("%"+fsDepartment)
                                                        : "";

            String lsCategory = fsCateogry != null && !"".equals(fsCateogry) 
                                                        ? " AND f.sDescript LIKE " + SQLUtil.toSQL("%"+fsCateogry)
                                                        : "";

            String lsTransactionDate = fdTransactionDate != null && !"".equals(fdTransactionDate) && !"1900-01-01".equals(xsDateShort(fdTransactionDate)) 
                                                        ? " AND a.dTransact = " + SQLUtil.toSQL(xsDateShort(fdTransactionDate))
                                                        : "";

            String lsTransactionNo = fsTransactionNo != null && !"".equals(fsTransactionNo) 
                                                        ? " AND a.sTransNox LIKE " + SQLUtil.toSQL("%"+fsTransactionNo)
                                                        : "";
            
            String lsApproval = "";
            if(isApproval){
                lsApproval = " AND a.sTransNox NOT IN (SELECT q.sSourceNo FROM PO_Quotation_Master q WHERE q.sSourceNo = a.sTransNox ) ";
            }

            String lsTransStat = "";
            if (psTranStat != null) {
                if (psTranStat.length() > 1) {
                    for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                        lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                    }
                    lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
                } else {
                    lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
                }
            }

            initSQL();
            String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                    + " AND a.sCategrCd = " + SQLUtil.toSQL(psCategorCd)
                    + lsBranch
                    + lsDepartment
                    + lsCategory
                    + lsTransactionDate
                    + lsTransactionNo
            );
            
            if (lsApproval != null && !"".equals(lsApproval)) {
                lsSQL = lsSQL + lsApproval;
            }
            
            if (lsTransStat != null && !"".equals(lsTransStat)) {
                lsSQL = lsSQL + lsTransStat;
            }

            lsSQL = lsSQL + " ORDER BY a.dTransact DESC ";

            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            poJSON = new JSONObject();

            int lnctr = 0;

            if (MiscUtil.RecordCount(loRS) >= 0) {
                paMasterList = new ArrayList<>();
                while (loRS.next()) {
                    // Print the result set
                    System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                    System.out.println("dTransact: " + loRS.getDate("dTransact"));
                    System.out.println("------------------------------------------------------------------------------");

                    paMasterList.add(POQuotationRequestMaster());
                    paMasterList.get(paMasterList.size() - 1).openRecord(loRS.getString("sTransNox"));
                    lnctr++;
                }

                System.out.println("Records found: " + lnctr);
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
            } else {
                paMasterList = new ArrayList<>();
                paMasterList.add(POQuotationRequestMaster());
                poJSON.put("result", "error");
                poJSON.put("continue", true);
                poJSON.put("message", "No record found.");
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } catch (GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }
        return poJSON;
    }
    
    public JSONObject loadPOQuotationRequestSupplierList()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        paSuppliers = new ArrayList<>();

        List loList = getPOQuotationRequestSupplier();
        for (int lnCtr = 0; lnCtr <= loList.size() - 1; lnCtr++) {
            paSuppliers.add(POQuotationRequestSupplier());
            poJSON = paSuppliers.get(getPOQuotationRequestSupplierCount()- 1).openRecord((String) loList.get(lnCtr), lnCtr+1);
            if ("success".equals((String) poJSON.get("result"))) {
                if(Master().getEditMode() == EditMode.UPDATE){
                   poJSON = paSuppliers.get(getPOQuotationRequestSupplierCount() - 1).updateRecord();
                }
            }
            
        }
        return poJSON;
    }
    
    
    public void ReloadDetail() throws CloneNotSupportedException{
        String lsBrandId = "";
        String lsModelId = "";
        int lnCtr = getDetailCount() - 1;
        while (lnCtr >= 0) {
//            System.out.println("Brand : " + Detail(lnCtr).getBrandId());
//            System.out.println("Model : " + Detail(lnCtr).getModelId());
//            System.out.println("Description : " + Detail(lnCtr).getDescription());
            if ((Detail(lnCtr).getStockId() == null || "".equals(Detail(lnCtr).getStockId()))
                    && (Detail(lnCtr).getDescription()== null || "".equals(Detail(lnCtr).getDescription()))) {
                
                if (Detail(lnCtr).getBrandId() != null
                    && !"".equals(Detail(lnCtr).getBrandId())) {
                    lsBrandId = Detail(lnCtr).getBrandId();
                }
                
                if (Detail(lnCtr).getModelId()!= null
                    && !"".equals(Detail(lnCtr).getModelId())) {
                    lsModelId = Detail(lnCtr).getModelId();
                }
                
                if(Detail(lnCtr).getEditMode() == EditMode.ADDNEW){
//                    System.out.println("Delete Detail : " + lnCtr);
                    deleteDetail(lnCtr); 
                    //Detail().remove(lnCtr);
                }
//                else if (Detail(lnCtr).getEditMode() == EditMode.UPDATE) {
//                    Detail(lnCtr).isReverse(false);
//                    removeDetail(Detail(lnCtr));
//                }
            }
            lnCtr--;
        }

        if ((getDetailCount() - 1) >= 0) {
            if (
                !Detail(getDetailCount() - 1).isReverse() &&
                (Detail(getDetailCount() - 1).getStockId() != null
                    && !"".equals(Detail(getDetailCount() - 1).getStockId()))
                || (Detail(getDetailCount() - 1).getDescription()!= null
                    && !"".equals(Detail(getDetailCount() - 1).getDescription()))) {
                AddDetail();
            }
        }

        if ((getDetailCount() - 1) < 0) {
            AddDetail();
        }
        
        //Set brand Id to last row
        if (!lsBrandId.isEmpty()) {
            Detail(getDetailCount() - 1).setBrandId(lsBrandId);
        }
        if (!lsModelId.isEmpty()) {
            Detail(getDetailCount() - 1).setModelId(lsModelId);
        }
    }
    
    public void ReloadSupplier() 
            throws CloneNotSupportedException, 
            SQLException, 
            GuanzonException{     
        
        if(getEditMode() == EditMode.ADDNEW || getEditMode() == EditMode.UPDATE){
        } else {
            return;
        }
        int lnRow = getPOQuotationRequestSupplierCount() - 1;
        while (lnRow >= 0) {
            if ((paSuppliers.get(lnRow).getSupplierId() == null || "".equals(paSuppliers.get(lnRow).getSupplierId()))
                    && (paSuppliers.get(lnRow).getCompanyId() == null || "".equals(paSuppliers.get(lnRow).getCompanyId()))
                    ) {
                
                if(paSuppliers.get(lnRow).getEditMode() == EditMode.ADDNEW){
                    paSuppliers.remove(lnRow);
                } else {
                    paSuppliers.get(lnRow).isReverse(false);
                }
            }
            lnRow--;
        }

        if ((getPOQuotationRequestSupplierCount()- 1) >= 0) {
            if (paSuppliers.get(getPOQuotationRequestSupplierCount() - 1).getSupplierId()!= null && !"".equals(paSuppliers.get(getPOQuotationRequestSupplierCount() - 1).getSupplierId())
                && (paSuppliers.get(getPOQuotationRequestSupplierCount() - 1).getCompanyId() != null && !"".equals(paSuppliers.get(getPOQuotationRequestSupplierCount() - 1).getCompanyId()))
                    ) {
                AddPOQuotationRequestSupplier();
            }
        }

        if ((getPOQuotationRequestSupplierCount() - 1) < 0) {
            AddPOQuotationRequestSupplier();
        }
    }
    
    private Model_PO_Quotation_Request_Master POQuotationRequestMaster() {
        return new QuotationModels(poGRider).POQuotationRequestMaster();
    }
    
    private Model_PO_Quotation_Request_Supplier POQuotationRequestSupplier() {
        return new QuotationModels(poGRider).POQuotationRequestSupplier();
    }

    @Override
    public Model_PO_Quotation_Request_Master Master() {
        return (Model_PO_Quotation_Request_Master) poMaster;
    }

    public Model_PO_Quotation_Request_Detail getDetail() {
        return (Model_PO_Quotation_Request_Detail) poDetail;
    }
    
    public List<Model_PO_Quotation_Request_Supplier> POQuotationRequestSupplierList() {
        return paSuppliers;
    }
    
    public Model_PO_Quotation_Request_Master POQuotationRequestList(int row) {
        return (Model_PO_Quotation_Request_Master) paMasterList.get(row);
    }

    @Override
    public Model_PO_Quotation_Request_Detail Detail(int row) {
        return (Model_PO_Quotation_Request_Detail) paDetail.get(row);
    }
    
    public Model_PO_Quotation_Request_Supplier POQuotationRequestSupplierList(int row) {
        return (Model_PO_Quotation_Request_Supplier) paSuppliers.get(row);
    }

    private Model_PO_Quotation_Request_Detail DetailRemoved(int row) {
        return (Model_PO_Quotation_Request_Detail) paDetailRemoved.get(row);
    }
    
    @Override
    public int getDetailCount() {
        if (paDetail == null) {
            paDetail = new ArrayList<>();
        }

        return paDetail.size();
    }
    
    public int getPOQuotationRequestCount() {
        if (paMasterList == null) {
            paMasterList = new ArrayList<>();
        }

        return paMasterList.size();
    }
    
    public int getPOQuotationRequestSupplierCount() {
        if (paSuppliers == null) {
            paSuppliers = new ArrayList<>();
        }

        return paSuppliers.size();
    }
    
    private int getDetailRemovedCount() {
        if (paDetailRemoved == null) {
            paDetailRemoved = new ArrayList<>();
        }

        return paDetailRemoved.size();
    }

    private List getPOQuotationRequestSupplier() throws SQLException, GuanzonException {
        String lsSQL = MiscUtil.makeSelect(POQuotationRequestSupplier());
        lsSQL = MiscUtil.addCondition(lsSQL, " sTransNox = " + SQLUtil.toSQL(Master().getTransactionNo()));
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        List<String> loList = new ArrayList();
        while (loRS.next()) {
             loList.add(loRS.getString("sTransNox")); 
        }
        return loList;
    }
    
    public JSONObject AddDetail()
            throws CloneNotSupportedException {
        poJSON = new JSONObject();

        if (getDetailCount() > 0) {
            if (Detail(getDetailCount() - 1).getStockId()!= null || Detail(getDetailCount() - 1).getDescription() != null) {
                if (Detail(getDetailCount() - 1).getStockId().isEmpty() && Detail(getDetailCount() - 1).getDescription().isEmpty()) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Last row has empty item.");
                    return poJSON;
                }
            }
        }

        return addDetail();
    }
    
    public JSONObject AddPOQuotationRequestSupplier()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        
        if (paSuppliers == null) {
            paSuppliers = new ArrayList<>();
        }

        if (paSuppliers.isEmpty()) {
            paSuppliers.add(POQuotationRequestSupplier());
            poJSON = paSuppliers.get(getPOQuotationRequestSupplierCount()- 1).newRecord();
        } else {
            if (paSuppliers.get(paSuppliers.size() - 1).getSupplierId() != null && !"".equals(paSuppliers.get(paSuppliers.size() - 1).getSupplierId())) {
                paSuppliers.add(POQuotationRequestSupplier());
                poJSON = paSuppliers.get(getPOQuotationRequestSupplierCount()- 1).newRecord();
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to add supplier.");
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        return poJSON;

    }
    
    public JSONObject removeDetails() {
        poJSON = new JSONObject();
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            if (item.getEditMode() == EditMode.ADDNEW) {
                detail.remove();
            } else {
                paDetailRemoved.add(item);
                item.setValue("cReversex", POQuotationRequestStatus.Reverse.EXCLUDE);
            }
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    public JSONObject removeWithInvDetails() {
        poJSON = new JSONObject();
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            //Only remove the detail with stock id to not be conflict in category level 2 of transaction vs inventory information
            if(item.getValue("sStockIDx") != null && !"".equals(item.getValue("sStockIDx"))){
                if (item.getEditMode() == EditMode.ADDNEW) {
                    detail.remove();
                } else {
                    paDetailRemoved.add(item);
                    item.setValue("cReversex", POQuotationRequestStatus.Reverse.EXCLUDE);
                }
            }
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }

    private void removeDetail(Model_PO_Quotation_Request_Detail item) {
        if (paDetailRemoved == null) {
            paDetailRemoved = new ArrayList<>();
        }
        
        paDetailRemoved.add(item);
    }

    public void resetMaster() {
        poMaster = new QuotationModels(poGRider).POQuotationRequestMaster();
    }
    
    public void resetOthers() {
        paSuppliers = new ArrayList<>();
        paDetailRemoved = new ArrayList<>();
        paSuppliersRemoved = new ArrayList<>();
        setSearchBranch("");
        setSearchCategory("");
        setSearchDepartment("");
    }
    
    /*Convert Date to String*/
    private static String xsDateShort(Date fdValue) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }

    private LocalDate strToDate(String val) {
        DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(val, date_formatter);
        return localDate;
    }
    
    public void setIndustryId(String industryId) { psIndustryId = industryId; }
    public void setCompanyId(String companyId) { psCompanyId = companyId; }
    public void setCategoryId(String categoryId) { psCategorCd = categoryId; }

    public void setSearchCompany(String searchCompany){ psSearchCompany = searchCompany; }
    public void setSearchIndustry(String searchIndustry){ psSearchIndustry = searchIndustry; }
    public void setSearchCategory(String searchCategory) { psSearchCategory = searchCategory; }
    public void setSearchBranch(String searchBranch) { psSearchBranch = searchBranch; }
    public void setSearchDestination(String searchDestination) { psSearchDestination = searchDestination; }
    public void setSearchDepartment(String searchDepartment) { psSearchDepartment = searchDepartment; }
    public void setSearchSupplier(String searchSupplier) { psSearchSupplier = searchSupplier; }
    
    public String getSearchCompany() { return psSearchCompany; }
    public String getSearchIndustry() { return psSearchIndustry; }
    public String getSearchCategory() { return psSearchCategory; }
    public String getSearchBranch() { return psSearchBranch; }
    public String getSearchDestination() { return psSearchDestination; }
    public String getSearchDepartment() { return psSearchDepartment; }
    public String getSearchSupplier() { return psSearchSupplier; }

    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }
    
    @Override
    public JSONObject willSave()
            throws SQLException,
            GuanzonException,
            CloneNotSupportedException {
        /*Put system validations and other assignments here*/
        poJSON = new JSONObject();
        
        //if current status is Return check changes
        if(POQuotationRequestStatus.RETURNED.equals(Master().getTransactionStatus())){
            boolean lbUpdated = false;
            POQuotationRequest loRecord = new QuotationControllers(poGRider, null).POQuotationRequest();
            loRecord.InitTransaction();
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            loRecord.OpenTransaction(Master().getTransactionNo());
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            
            lbUpdated = loRecord.getDetailCount() == (getDetailCount() - 1);
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getReferenceNo().equals(Master().getReferenceNo());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getCategoryLevel2().equals(Master().getCategoryLevel2());
            }
            if (lbUpdated) {
                lbUpdated = Objects.equals(loRecord.Master().getExpectedPurchaseDate(), Master().getExpectedPurchaseDate());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getDestination().equals(Master().getDestination());
            }
            if (lbUpdated) {
                lbUpdated = loRecord.Master().getRemarks().equals(Master().getRemarks());
            }

            if (lbUpdated) {
                for (int lnCtr = 0; lnCtr <= loRecord.getDetailCount() - 1; lnCtr++) {
                    lbUpdated = loRecord.Detail(lnCtr).getStockId().equals(Detail(lnCtr).getStockId());
                    if (lbUpdated) {
                        lbUpdated = loRecord.Detail(lnCtr).getQuantity().doubleValue() == Detail(lnCtr).getQuantity().doubleValue();
                    } 
                    if (lbUpdated) {
                        lbUpdated = loRecord.Detail(lnCtr).getDescription().equals(Detail(lnCtr).getDescription());
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
        
        }
        
        switch(Master().getTransactionStatus()){
            case POQuotationRequestStatus.CONFIRMED:
            case POQuotationRequestStatus.RETURNED:
                //seek for approval
                poJSON =  seekApproval();
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
            break;
        }
        
        if (paDetailRemoved == null) {
            paDetailRemoved = new ArrayList<>();
        }
        
        if(Master().getEditMode() == EditMode.ADDNEW){
            System.out.println("Will Save : " + Master().getNextCode());
            Master().setTransactionNo(Master().getNextCode());
            Master().setPrepared(poGRider.getUserID());
        }
        
        Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
        Master().setModifiedDate(poGRider.getServerDate());
        Master().setPreparedDate(poGRider.getServerDate()); //Re-updated prepared date when user edited the transaction conflict in null when updating transaction
       
        //Check detail
        boolean lbWillDelete = true;
        for(int lnCtr = 0; lnCtr <= getDetailCount()-1; lnCtr++){
            if ((Detail(lnCtr).getQuantity() > 0.00) && Detail(lnCtr).isReverse() ) {
                lbWillDelete = false;
            }
        }
        
        if(lbWillDelete){
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction quantity to be save.");
            return poJSON;
        }
        
        //Check Supplier detail
        int lnRow = 0;
        for(int lnCtr = 0; lnCtr <= getPOQuotationRequestSupplierCount()-1; lnCtr++){
            if(POQuotationRequestSupplierList(lnCtr).isReverse()){
                lnRow++;
                if (POQuotationRequestSupplierList(lnCtr).getSupplierId() != null 
                        && !"".equals(POQuotationRequestSupplierList(lnCtr).getSupplierId())) {
                    
                    if(POQuotationRequestSupplierList(lnCtr).getTerm()== null 
                        || "".equals(POQuotationRequestSupplierList(lnCtr).getTerm())){
                        poJSON.put("result", "error");
                        poJSON.put("message", "Supplier term at row "+lnRow+" cannot be empty.");
                        return poJSON;
                    }
                    
                    if(POQuotationRequestSupplierList(lnCtr).getCompanyId() == null 
                        || "".equals(POQuotationRequestSupplierList(lnCtr).getCompanyId())){
                        poJSON.put("result", "error");
                        poJSON.put("message", "Supplier company at row "+lnRow+" cannot be empty.");
                        return poJSON;
                    }
                }
            }
        }
        
        String lsQuantity = "0.00";
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            if(item.getValue("nQuantity") != null && !"".equals(item.getValue("nQuantity"))){
                lsQuantity = item.getValue("nQuantity").toString();
            }
            if (((item.getValue("sDescript") == null || "".equals(item.getValue("sDescript")))
                  &&  (item.getValue("sStockIDx") == null || "".equals(item.getValue("sStockIDx"))))
                  || (Double.valueOf(lsQuantity) <= 0.00)) {

                if (item.getEditMode() == EditMode.ADDNEW) {
                    detail.remove();
                } else {
                    paDetailRemoved.add(item);
                    item.setValue("cReversex", POQuotationRequestStatus.Reverse.EXCLUDE);
                }

            }
        }
        
        //remove supplier without details
        Iterator<Model_PO_Quotation_Request_Supplier> others = paSuppliers.iterator();
        while (others.hasNext()) {
            Model_PO_Quotation_Request_Supplier item = others.next();
            if (item.getSupplierId() == null || "".equals(item.getSupplierId())){
                others.remove();
            }
        }

        //Validate detail after removing all zero qty and empty stock Id
        if (getDetailCount() <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction detail to be save.");
            return poJSON;
        }

        //assign other info on detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr+1);
        }
        
        for (int lnCtr = 0; lnCtr <= getPOQuotationRequestSupplierCount()- 1; lnCtr++) {
            POQuotationRequestSupplierList(lnCtr).setTransactionNo(Master().getTransactionNo());
            POQuotationRequestSupplierList(lnCtr).setEntryNo(lnCtr+1);
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    @Override
    public JSONObject save() {
        /*Put saving business rules here*/
        return isEntryOkay(POQuotationRequestStatus.OPEN);
    }
    
    @Override
    public void saveComplete() {
        /*This procedure was called when saving was complete*/
        System.out.println("Transaction saved successfully.");
    }
    
    @Override
    public JSONObject saveOthers() {
        /*Only modify this if there are other tables to modify except the master and detail tables*/
        poJSON = new JSONObject();
        try {
            //Save PO Quotation Request Supplier
            for (int lnCtr = 0; lnCtr <= getPOQuotationRequestSupplierCount()- 1; lnCtr++) {
                if (paSuppliers.get(lnCtr).getEditMode() == EditMode.ADDNEW || paSuppliers.get(lnCtr).getEditMode() == EditMode.UPDATE) {
                    paSuppliers.get(lnCtr).setModifiedDate(poGRider.getServerDate());
                    poJSON = paSuppliers.get(lnCtr).saveRecord();
                    if ("error".equals((String) poJSON.get("result"))) {
                        System.out.println("Save PO Quotation Request Supplier " + (String) poJSON.get("message"));
                        poJSON.put("result", "error");
                        poJSON.put("message", (String) poJSON.get("message"));
                        return poJSON;
                    }
                }
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        poJSON.put("result", "success");
        return poJSON;
    }
    

    @Override
    public JSONObject initFields() {
        try {
            /*Put initial model values here*/
            poJSON = new JSONObject();
            System.out.println("Dept ID : " + poGRider.getDepartment());
            System.out.println("Current User : " + poGRider.getUserID());
            
            Master().setBranchCode(poGRider.getBranchCode());
            Master().setIndustryId(psIndustryId);
            Master().setCategoryCode(psCategorCd);
            Master().setTransactionDate(poGRider.getServerDate());
            Master().setTransactionStatus(POQuotationRequestStatus.OPEN);
            
            //TODO Hardcode Default value is for sir jeff
            Master().setDepartmentId("033"); // Autoset Department of procurement
            Master().setDestination("M001"); //Autoset Destination of sir jeff
            
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }

        poJSON.put("result", "success");
        return poJSON;
    }
    
    @Override
    protected JSONObject isEntryOkay(String status) {
        GValidator loValidator = POQuotationRequestValidatorFactory.make(Master().getIndustryId());

        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(poMaster);
//        loValidator.setDetail(paDetail);

        poJSON = loValidator.validate();

        return poJSON;
    }

    @Override
    public void initSQL() {
        SQL_BROWSE =  " SELECT         "
                    + "    a.sTransNox "
                    + "  , a.sIndstCdx "
                    + "  , a.sBranchCd "
                    + "  , a.sDeptIDxx "
                    + "  , a.sCategrCd "
                    + "  , a.dTransact "
                    + "  , a.sCategCd2 "
                    + "  , a.sDestinat "
                    + "  , a.sReferNox "
                    + "  , a.cTranStat "
                    + "  , b.sDescript AS Industry   "
                    + "  , c.sBranchNm AS Branch     "
                    + "  , d.sDeptName AS Department "
                    + "  , e.sDescript AS Category   "
                    + "  , f.sDescript AS Category2  "
                    + " FROM PO_Quotation_Request_Master a "
                    + " LEFT JOIN Industry b ON b.sIndstCdx = a.sIndstCdx        "
                    + " LEFT JOIN Branch c ON c.sBranchCd = a.sBranchCd          "
                    + " LEFT JOIN Department d ON d.sDeptIDxx = a.sDeptIDxx      "
                    + " LEFT JOIN Category e ON e.sCategrCd = a.sCategrCd        "
                    + " LEFT JOIN Category_Level2 f ON f.sCategrCd = a.sCategCd2 " ;
        
    }
    
//    public List<String> getApprover() throws SQLException{
//        List<String> lsList = new ArrayList<String>();
//            String lsSQL =   " SELECT "
//                        + "     a.sSourceCD "
//                        + " ,	a.sSourceNo "
//                        + " ,	a.cTranStat "
//                        + " ,	d.sCompnyNm "
//                        + " ,	c.dApproved "
//                        + " ,   CONCAT(c.sEmployID, ' - ',d.sCompnyNm,' ',c.dApproved) AS sApprover "
//                        + " FROM Transaction_Authorization_Master a  "
//                        + " ,	Transaction_Authorization_Detail b     "
//                        + " ,	Transaction_Authorization_Recipient c  "
//                        + " LEFT JOIN Client_Master d ON c.sEmployID = d.sClientID "
//                        + " WHERE a.sTransNox = b.sSourceNo AND b.sSourceNo = c.sSourceNo    "
//                        + " AND a.sSourceCD = " + SQLUtil.toSQL(getSourceCode())
//                        + " AND a.sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo())
//                        + " AND a.cTranStat = '2' "
//                        + " AND c.cTranStat = '1' ";
//            System.out.println("Executing SQL: " + lsSQL);
//            ResultSet loRS = poGRider.executeQuery(lsSQL);
//            if (MiscUtil.RecordCount(loRS) >= 0) {
//                while (loRS.next()) {
//                    lsList.add(loRS.getString("sApprover"));
//                }
//                MiscUtil.close(loRS);
//            }
//            
//        return lsList;
//    }
    
    public JSONObject getApprover() throws SQLException{
        JSONObject loJSON = new JSONObject();
        loJSON.put("sModified", "");
        loJSON.put("dModified", "");
        String lsSQL =   " SELECT "
                    + "     a.sModified"
                    + " ,   a.dModified"
                    + " FROM Transaction_Status_History a  "
                    + " WHERE a.sTableNme = " + SQLUtil.toSQL(Master().getTable())
                    + " AND a.sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo())
                    + " AND a.cRefrStat = " + SQLUtil.toSQL(POQuotationRequestStatus.APPROVED)
                    + " AND a.cTranStat = '1' ";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (MiscUtil.RecordCount(loRS) >= 0) {
            while (loRS.next()) {
                // Get the LocalDateTime from your result set
                LocalDateTime dModified = loRS.getObject("dModified", LocalDateTime.class);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
                loJSON.put("sModified", loRS.getString("sModified"));
                loJSON.put("dModified", dModified.format(formatter));
                loJSON.put("result", "success");
                return loJSON;
            }
            MiscUtil.close(loRS);
        }
        
        loJSON.put("result", "error");
        return loJSON;
    }
    
    public String getPreparedDate() throws SQLException, GuanzonException {
        String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(Master()), " sTransNox =  " + SQLUtil.toSQL(Master().getTransactionNo())) ;
        System.out.println("SQL " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
          if (MiscUtil.RecordCount(loRS) > 0L) {
            if (loRS.next()) {
                // Get the LocalDateTime from your result set
                LocalDateTime dModified = loRS.getObject("dPrepared", LocalDateTime.class);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
                return dModified.format(formatter);
            } 
          }
          MiscUtil.close(loRS);
        } catch (SQLException e) {
          poJSON.put("result", "error");
          poJSON.put("message", e.getMessage());
        } 
        return "";
    }
    
    /*EXPORT FILE*/
    private JSONObject sentEmail(){
        poJSON = new JSONObject();
        
        for(int lnCtr=0; lnCtr <= getDetailCount()-1;lnCtr++){
            //Call sent email method
//            poJSON = sentEmail(lnCtr);
//            if ("success".equals((String) poJSON.get("result"))) {
                //if success sending of email call exportFile
                poJSON = exportFile(lnCtr);
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }   
//            }    
            
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    public JSONObject exportFile(){
        poJSON = new JSONObject();
        
        if(getPOQuotationRequestSupplierCount() <= 0){
            poJSON.put("result", "error");
            poJSON.put("message", "No supplier to be export.");
            return poJSON;
        }
        //retreiving using column index
        System.out.println("-----------------------------------EXPORT-------------------------------------");
        for (int lnCtr = 0; lnCtr <= getPOQuotationRequestSupplierCount() - 1; lnCtr++) {
                if(POQuotationRequestSupplierList(lnCtr).isReverse()){
                System.out.println("Row No ->> " + lnCtr);
                poJSON = exportFile(lnCtr);
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }   
            }
        }
        System.out.println("------------------------------------------------------------------------------");
        
        poJSON.put("result", "success");
        poJSON.put("message", "Export complete. \n\nCheck folder located in\""+System.getProperty("sys.default.path.temp")+"\"Export folder.");
        return poJSON;
    }
    public JSONObject exportFile(int POQuotationRequestSupplierRow){
        poJSON = new JSONObject();
        String lsExportPath = System.getProperty("sys.default.path.temp") + "/Export";
        String lsReportPath = System.getProperty("sys.default.path.config") + "/Reports/POQuotationRequest.jrxml";
        String lsWaterMarkPath = System.getProperty("sys.default.path.config") + "/Reports//images/approved.png";
        try {
            
            File folder = new File(lsExportPath);
            if (!folder.exists()) {
                if (folder.mkdirs()) {
                    // Folder successfully created
                    System.out.println("Folder created at: " + folder.getAbsolutePath());
                } else {
                    // Failed to create folder
                    poJSON.put("result", "error");
                    poJSON.put("message", "Failed to create folder. \n\nEnsure the application has write permissions to \"" + System.getProperty("sys.default.path.temp") + "\"Export");
                    return poJSON;
                }
            }
            
            // 1. Prepare parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("sTransNox", Master().getTransactionNo());
            parameters.put("dTransDte", new java.sql.Date(Master().getTransactionDate().getTime()));
            if(Master().getExpectedPurchaseDate() != null){
                parameters.put("dExpPrchseDte", new java.text.SimpleDateFormat("MMMM dd, yyyy").format((java.util.Date) Master().getExpectedPurchaseDate()));
            } else {
                parameters.put("dExpPrchseDte", "");
            }
            parameters.put("sRemarks", Master().getRemarks());
            parameters.put("sSupplierNm", POQuotationRequestSupplierList(POQuotationRequestSupplierRow).Supplier().getCompanyName()); 
            parameters.put("sSupplierAddress", POQuotationRequestSupplierList(POQuotationRequestSupplierRow).Address().getAddress()); 
            parameters.put("sContactNo", POQuotationRequestSupplierList(POQuotationRequestSupplierRow).Contact().getMobileNo()); 
            if(Master().Department().getDescription() != null && !"".equals(Master().Department().getDescription())){
                parameters.put("sDestination", Master().Destination().getBranchName() + " - " + Master().Department().getDescription());
            } else {
                parameters.put("sDestination", Master().Destination().getBranchName());
            }
            parameters.put("sBranchNm", Master().Branch().getBranchName());
            parameters.put("sAddressx", Master().Branch().getAddress());
            parameters.put("sCompnyNm", POQuotationRequestSupplierList(POQuotationRequestSupplierRow).Company().getCompanyName());
            parameters.put("dDatexxx", new java.sql.Date(poGRider.getServerDate().getTime()));
            parameters.put("sCompany", POQuotationRequestSupplierList(POQuotationRequestSupplierRow).Company().getCompanyName());
            parameters.put("sPreparedBy","");
            parameters.put("sApprovedBy","");
            
            String lsPreparedBy = "";
            if(Master().getPrepared().length() > 10){
                lsPreparedBy = getSysUser(poGRider.Decrypt(Master().getPrepared())); 
            } else {
                lsPreparedBy = getSysUser(Master().getPrepared()); 
            }
            
            // Get the LocalDateTime from your result set
            System.out.println("PREPARED DATE : " + Master().getPreparedDate());//Always returning NULL 
            System.out.println("PREPARED DATE MANUAL QUERY: " + getPreparedDate()); 
            parameters.put("sPreparedBy", "Prepared by : "+ lsPreparedBy + " " + getPreparedDate()); 
            
            if(Master().getTransactionStatus().equals(POQuotationRequestStatus.APPROVED)){
                //Update value when approved
                JSONObject loJSON = getApprover();
                if("success".equals((String) loJSON.get("result"))){
                    String lsApprover = (String) loJSON.get("sModified");
                    if(lsApprover != null && !"".equals(lsApprover)){
                        if(lsApprover.length() > 10){
                            lsApprover = getSysUser(poGRider.Decrypt(lsApprover)); 
                        } else {
                            lsApprover = getSysUser(lsApprover); 
                        }
                        parameters.put("sApprovedBy","Approved by: " + lsApprover  + " " + String.valueOf((String) loJSON.get("dModified"))); 
                    }
                }
            }
            
            parameters.put("watermarkImagePath", lsWaterMarkPath);
            List<OrderDetail> orderDetails = new ArrayList<>();
            
            int lnRow = 1;
            String lsDescription = "";
            String lsBrand = "";
            String lsModel = "";
            String lsMeasure = "";
            String lsColor = "";
            for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
                if(Detail(lnCtr).isReverse()){
                    
                    lsDescription = Detail(lnCtr).getDescription();
                    
                    if(Detail(lnCtr).getStockId() != null && !"".equals(Detail(lnCtr).getStockId())){
                        lsBrand = Detail(lnCtr).Inventory().Brand().getDescription();
                        lsModel = Detail(lnCtr).Inventory().Model().getDescription();
                        lsMeasure = Detail(lnCtr).Inventory().Measure().getDescription();
                        lsColor = Detail(lnCtr).Inventory().Color().getDescription();
                    } 
                        
                    if(lsBrand == null || "".equals(lsBrand)){ 
                        lsBrand = ""; 
                    } else {
                        lsBrand = lsBrand.toUpperCase();
                    }  
                    if(lsModel == null || "".equals(lsModel)){ 
                        lsModel = ""; 
                    } else {
                        lsModel = lsModel.toUpperCase();
                    }  
                    if(lsMeasure == null || "".equals(lsMeasure)){ 
                        lsMeasure = ""; 
                    } else {
                        lsMeasure = lsMeasure.toUpperCase();
                    }  
                    if(lsColor == null || "".equals(lsColor)){ 
                        lsColor = ""; 
                    } else {
                        lsColor = lsColor.toUpperCase();
                    }
                    
                    orderDetails.add(new OrderDetail(lnRow, lsDescription, lsBrand,lsModel,lsMeasure,lsColor,Detail(lnCtr).getQuantity()));
                    lnRow++;
                }
                lsDescription = "";lsBrand = "";lsModel = "";lsMeasure = "";lsColor = "";
            }
            
            File file = new File(lsReportPath);
            if (file.exists()) {
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Jasper file does not exist. \n\nEnsure the file is located in \""+System.getProperty("sys.default.path.config")+"\"Reports");
                return poJSON;
            }

            // 3. Create data source
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(orderDetails);

            // 4. Compile and fill report
            JasperReport jasperReport = JasperCompileManager.compileReport(lsReportPath);
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport,
                    parameters,
                    dataSource
            );
            
            String lsExportFile = lsExportPath + "/" + Master().getTransactionNo() + " - "+  POQuotationRequestSupplierList(POQuotationRequestSupplierRow).Company().getCompanyName() + " " + POQuotationRequestSupplierList(POQuotationRequestSupplierRow).Supplier().getCompanyName() + ".pdf";
            JasperExportManager.exportReportToPdfFile(jasperPrint, lsExportFile);
            
        } catch (JRException e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(e));
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
        }
        
        return poJSON;
    }
    
    public static class OrderDetail {

        private Integer nRowNo;
        private String sDescription;
        private String sBrand;
        private String sModel;
        private String sMeasure;
        private String sColor;
        private double nOrder;

        public OrderDetail(Integer rowNo, String description, String brand, String model, String measure, String color, double order) {
            this.nRowNo = rowNo;
            this.sDescription = description;
            this.sBrand = brand;
            this.sModel = model;
            this.sMeasure = measure;
            this.sColor = color;
            this.nOrder = order;
        }

        public Integer getnRowNo() {
            return nRowNo;
        }

        public String getsDescription() {
            return sDescription;
        }

        public String getsBrand() {
            return sBrand;
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

        public double getnOrder() {
            return nOrder;
        }
    }
    
    public String getStatus(String fsStatus){
        switch(fsStatus){
            case POQuotationRequestStatus.OPEN:
                return  "Open";
            case POQuotationRequestStatus.CONFIRMED:
                return  "Confirmed";
            case POQuotationRequestStatus.POSTED:
                return  "Posted";
            case POQuotationRequestStatus.CANCELLED:
                return  "Cancelled";
            case POQuotationRequestStatus.VOID:
                return  "Void";
            case POQuotationRequestStatus.APPROVED:
                return  "Approved";
            case POQuotationRequestStatus.RETURNED:
                return  "Returned";
            default:
            case "":
                return "Unknown";
        }
    }
    
    public void ShowStatusHistory() throws SQLException, GuanzonException, Exception{
        CachedRowSet crs = getStatusHistory();
        
        crs.beforeFirst();
        
        while(crs.next()){
            switch (crs.getString("cRefrStat")){
                case "":
                    crs.updateString("cRefrStat", "-");
                    break;
                case POQuotationRequestStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case POQuotationRequestStatus.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case POQuotationRequestStatus.POSTED:
                    crs.updateString("cRefrStat", "POSTED");
                    break;
                case POQuotationRequestStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case POQuotationRequestStatus.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;
                case POQuotationRequestStatus.APPROVED:
                    crs.updateString("cRefrStat", "APPROVED");
                    break;
                case POQuotationRequestStatus.RETURNED:
                    crs.updateString("cRefrStat", "RETURNED");
                    break;
                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);
                    
                    switch (stat){
                        case POQuotationRequestStatus.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case POQuotationRequestStatus.CONFIRMED:
                            crs.updateString("cRefrStat", "CONFIRMED");
                            break;
                        case POQuotationRequestStatus.POSTED:
                            crs.updateString("cRefrStat", "POSTED");
                            break;
                        case POQuotationRequestStatus.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case POQuotationRequestStatus.VOID:
                            crs.updateString("cRefrStat", "VOID");
                            break;
                        case POQuotationRequestStatus.APPROVED:
                            crs.updateString("cRefrStat", "APPROVED");
                            break;
                        case POQuotationRequestStatus.RETURNED:
                            crs.updateString("cRefrStat", "RETURNED");
                            break;
                    }
            }
            crs.updateRow(); 
        }
        
        JSONObject loJSON  = getEntryBy();
        String entryBy = "";
        String entryDate = "";
        
        if ("success".equals((String) loJSON.get("result"))){
            entryBy = (String) loJSON.get("sCompnyNm");
            entryDate = (String) loJSON.get("sEntryDte");
        }
        
        showStatusHistoryUI("Purchase Order Quotation Request", (String) poMaster.getValue("sTransNox"), entryBy, entryDate, crs);
    }
    
    public JSONObject getEntryBy() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsEntry = "";
        String lsEntryDate = "";
        String lsSQL =  " SELECT b.sModified, b.dModified " 
                        + " FROM PO_Quotation_Request_Master a "
                        + " LEFT JOIN xxxAuditLogMaster b ON b.sSourceNo = a.sTransNox AND b.sEventNme LIKE 'ADD%NEW' AND b.sRemarksx = " + SQLUtil.toSQL(Master().getTable());
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox =  " + SQLUtil.toSQL(Master().getTransactionNo())) ;
        System.out.println("Execute SQL : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
          if (MiscUtil.RecordCount(loRS) > 0L) {
            if (loRS.next()) {
                if(loRS.getString("sModified") != null && !"".equals(loRS.getString("sModified"))){
                    if(loRS.getString("sModified").length() > 10){
                        lsEntry = getSysUser(poGRider.Decrypt(loRS.getString("sModified"))); 
                    } else {
                        lsEntry = getSysUser(loRS.getString("sModified")); 
                    }
                    // Get the LocalDateTime from your result set
                    LocalDateTime dModified = loRS.getObject("dModified", LocalDateTime.class);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
                    lsEntryDate =  dModified.format(formatter);
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
    
    public String getSysUser(String fsId) throws SQLException, GuanzonException {
        String lsEntry = "";
        String lsSQL =   " SELECT b.sCompnyNm from xxxSysUser a " 
                       + " LEFT JOIN Client_Master b ON b.sClientID = a.sEmployNo ";
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sUserIDxx =  " + SQLUtil.toSQL(fsId)) ;
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
    
    /******05-04-2026 Arsiela********/
    /*******************************PURCHASE ORDER QUOTATION REQUEST REPORT**************************************/
    
    public JSONObject SearchDestination(String value, boolean byCode, boolean isSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            if(isSearch){
                setSearchDestination(object.getModel().getBranchName());
            } else {
                Master().setDestination(object.getModel().getBranchCode());
            }
        }

        return poJSON;
    }
    
    public JSONObject SearchCategory(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Category object = new ParamControllers(poGRider, logwrapr).Category();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            setSearchCategory(object.getModel().getDescription());
        }
        return poJSON;
    }
    
    public JSONObject SearchCompany(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Company object = new ParamControllers(poGRider, logwrapr).Company();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            setSearchCompany(object.getModel().getCompanyName());
        }
        return poJSON;
    }
    
    public JSONObject SearchIndustry(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Industry object = new ParamControllers(poGRider, logwrapr).Industry();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            setSearchIndustry(object.getModel().getDescription());
        }
        return poJSON;
    }
    
    public JSONObject SearchSupplier(String value, boolean byCode) throws SQLException, GuanzonException {
        AP_Client_Master object = new ClientControllers(poGRider, logwrapr).APClientMaster();
        object.setRecordStatus(RecordStatus.ACTIVE);
        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            setSearchSupplier(object.getModel().Client().getCompanyName());
        }
        
        return poJSON;
    }
    
    //If main office or general allow to view all poq request 
    public void initializeFilter() throws SQLException, GuanzonException{
        Master().setIndustryId(psIndustryId);
        Master().setCategoryCode(psCategorCd);
        setSearchCompany(getCompanyName(psCompanyId));
        setSearchIndustry(Master().Industry().getDescription());
        setSearchCategory(Master().Category().getDescription());
        setSearchBranch(poGRider.getBranchName());
    }
    
    public String getCompanyName(String fsCompanyId) throws SQLException, GuanzonException{
        Company loObject = new ParamControllers(poGRider,logwrapr).Company();
        loObject.openRecord(fsCompanyId);
        if(loObject.getModel().getCompanyName() != null && !"".equals(loObject.getModel().getCompanyName())){
            return loObject.getModel().getCompanyName();
        }
        return "";
    }
    
    private List<TransactionDetail> paReport = new ArrayList<>();
    public JSONObject loadReport(boolean isSummary, String fsDateFrom, String fsDateTo, String fsStatus) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        paReport = new ArrayList<>();
        String lsFilterAll = "";
        if (psSearchCompany != null && !"".equals(psSearchCompany)) {
            lsFilterAll += " AND e.sCompnyNm LIKE " + SQLUtil.toSQL("%"+psSearchCompany+"%") ;
        }
        if (psSearchIndustry != null && !"".equals(psSearchIndustry)) {
            lsFilterAll += " AND d.sDescript LIKE " + SQLUtil.toSQL("%"+psSearchIndustry+"%") ;
        }
        if (psSearchCategory != null && !"".equals(psSearchCategory)) {
            lsFilterAll += " AND j.sDescript LIKE " + SQLUtil.toSQL("%"+psSearchCategory+"%") ;
        }
        if (psSearchBranch != null && !"".equals(psSearchBranch)) {
            lsFilterAll += " AND g.sBranchNm LIKE " + SQLUtil.toSQL("%"+psSearchBranch+"%") ;
        }
        if (psSearchDestination != null && !"".equals(psSearchDestination)) {
            lsFilterAll += " AND h.sBranchNm LIKE " + SQLUtil.toSQL("%"+psSearchDestination+"%") ;
        }
        if (psSearchDepartment != null && !"".equals(psSearchDepartment)) {
            lsFilterAll += " AND f.sDeptName LIKE " + SQLUtil.toSQL("%"+psSearchDepartment+"%") ;
        }
        if (psSearchSupplier != null && !"".equals(psSearchSupplier)) {
            lsFilterAll += " AND i.sCompnyNm LIKE " + SQLUtil.toSQL("%"+psSearchSupplier+"%") ;
        }
        
        switch(fsStatus){
            case  "OPEN":
                fsStatus = POQuotationRequestStatus.OPEN;
                break;
            case  "CONFIRMED":
                fsStatus = POQuotationRequestStatus.CONFIRMED;
                break;
            case  "APPROVED":
                fsStatus = POQuotationRequestStatus.APPROVED;
                break;
            case  "POSTED":
                fsStatus = POQuotationRequestStatus.POSTED;
                break;
            case  "RETURNED":
                fsStatus = POQuotationRequestStatus.RETURNED;
                break;
            case  "VOID":
                fsStatus = POQuotationRequestStatus.VOID;
                break;
            case  "CANCELLED":
                fsStatus = POQuotationRequestStatus.CANCELLED;
                break;
            default:
            case  "ALL":
                fsStatus = "%";
                break;
        }
        
        String lsSQL = MiscUtil.addCondition(getSQ_Report(isSummary),
                " dTransact BETWEEN " + SQLUtil.toSQL(fsDateFrom)
                + " AND " + SQLUtil.toSQL(fsDateTo)
                + " AND a.cTranStat LIKE " + SQLUtil.toSQL(fsStatus)
                
            );
        
        if (!"".equals(lsFilterAll)) {
            lsSQL = lsSQL + " " + lsFilterAll; 
        }
        
        if(isSummary){
            //CHECK if main office or general allow to view all poq request 
            if (!psIndustryId.equals(System.getProperty("sys.main.industry")) && !psIndustryId.equals(System.getProperty("sys.general.industry"))) { 
                lsSQL = lsSQL + " GROUP BY a.sTransNox, c.sSupplier " ;
            } else {
                lsSQL = lsSQL + " GROUP BY a.sTransNox, c.sSupplier, c.sCompnyID " ;
            }
        }
        lsSQL = lsSQL + " ORDER BY a.dTransact, a.sTransNox, c.sSupplier, c.sCompnyID, b.nEntryNox ASC ";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (MiscUtil.RecordCount(loRS) <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No record found.");
            return poJSON;
        }
        
        // Get metadata
        ResultSetMetaData metaData = loRS.getMetaData();
        // Get number of columns
        int columnCount = metaData.getColumnCount();
        int lnctr = 0;
        JSONArray dataArray = new JSONArray();
        while (loRS.next()) {
            JSONObject record = new JSONObject(); 
            
            for (int lnRow = 1; lnRow <= columnCount; lnRow++) {
                int columnType = metaData.getColumnType(lnRow);
                Object value;
                switch (columnType) {
                    case java.sql.Types.VARCHAR:
                    case java.sql.Types.CHAR:
                        value = loRS.getString(lnRow);
                        break;
                    case java.sql.Types.INTEGER:
                        value = loRS.getInt(lnRow);
                        break;
                    case java.sql.Types.DOUBLE:
                    case java.sql.Types.FLOAT:
                    case java.sql.Types.DECIMAL:
                        value = loRS.getDouble(lnRow);
                        break;
                    case java.sql.Types.DATE:
                        value = loRS.getDate(lnRow);
                        break;
                    case java.sql.Types.TIMESTAMP:
                        value = loRS.getTimestamp(lnRow);
                        break;
                    default:
                        value = loRS.getObject(lnRow); // fallback
                }
                if("xTranStat".equals(metaData.getColumnLabel(lnRow))){
                    record.put(metaData.getColumnLabel(lnRow), getStatus(String.valueOf(value)));
                } else {
                    record.put(metaData.getColumnLabel(lnRow), value);
                }
//                System.out.println(metaData.getColumnLabel(lnRow) + " : " + value);
            }
            dataArray.add(record);
            
            paReport.add(new TransactionDetail(
                    (lnctr+1)
                    , loRS.getString("sTransNox")
                    , loRS.getDate("dTransact")
                    , loRS.getString("sReferNox")
                    , loRS.getString("xBranchNm")
                    , loRS.getString("xDestintn")
                    , loRS.getString("xDeptName")
                    , loRS.getString("xCompanyx")
                    , loRS.getString("xCategLv2")
                    , loRS.getString("xSupplier")
                    , loRS.getString("xTermCode")
                    , loRS.getInt("xEntryNox")
                    , loRS.getString("xDetDescx")
                    , loRS.getDouble("xUnitPrce")
                    , loRS.getDouble("xQuantity")
                    , loRS.getDouble("xDetTotal")
                    , loRS.getDouble("xTranTotal")
                    , getStatus(loRS.getString("xTranStat"))
            ));
            
//            System.out.println("---------------------------------------------");
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
        return poJSON;
    }
    
    public List getReport(){
        return paReport;
    }
    
    private String getSQ_Report(boolean isSummary){
        return " SELECT  " +
                "  IFNULL(a.sTransNox,'') AS sTransNox " + // Transaction No
                ", a.dTransact AS dTransact " + // Date
                ", IFNULL(a.sReferNox,'') AS sReferNox " + // Reference No
                ", a.dExpPurch AS dExpPurch " + 
                //            ", " + getStatus("a.cTranStat") + " AS xTranStat " + // Transaction Status
                ", IFNULL(a.cTranStat,'') AS xTranStat " +
                ", IFNULL(d.sDescript,'') as xIndustry " + // Industry
                ", IFNULL(e.sCompnyNm,'') AS xCompanyx " + // Company
                ", IFNULL(g.sBranchNm,'') AS xBranchNm " + // Branch
                ", IFNULL(h.sBranchNm,'') AS xDestintn " + // Destination
                ", IFNULL(f.sDeptName,'') AS xDeptName " + // Department
                ", IFNULL(i.sCompnyNm,'') AS xSupplier " + // Supplier
                ", IFNULL(v.sDescript,'') AS xTermCode " + // Term
                ", IFNULL(j.sDescript,'') AS xCategory " +
                ", IFNULL(k.sDescript,'') AS xCategLv2 " + // Category
                ", IFNULL(b.sDescript,'') AS xPQRDtail " +
                ", IFNULL(l.sDescript,'') AS xInvDescr " +
                ", IFNULL(m.sDescript,'') AS xInvCateg " +
                ", IFNULL(n.sDescript,'') as xInvCate2 " + 
                ", IFNULL(o.sDescript,'') AS xInvCate3 " +
                ", IFNULL(p.sDescript,'') AS xInvCate4 " +
                ", IFNULL(q.sDescript,'') AS xInvBrand " +
                ", IFNULL(r.sDescript,'') AS xInvModel " +
                ", IFNULL(s.sDescript,'') AS xInvMdlVt " +
                ", IFNULL(t.sDescript,'') AS xInvColor " +
                ", IFNULL(u.sDescript,'') AS xInvMeasr " +
                ", b.nEntryNox AS xEntryNox " + // Detail Row
                ", b.nQuantity AS xQuantity " + // Quantity
                ", b.nUnitPrce AS xUnitPrce " + // Cost
                ", TRIM(CONCAT_WS(' ',IFNULL(q.sDescript,''),IFNULL(b.sDescript,''),IFNULL(u.sDescript,''))) AS xDetDescx " + // Item
                ", (b.nUnitPrce * b.nQuantity) AS xDetTotal " + // Total
                (isSummary ? ", SUM(b.nUnitPrce * b.nQuantity) " : ", (b.nUnitPrce * b.nQuantity) ") + " AS xTranTotal " + // Transaction Total
            "FROM PO_Quotation_Request_Master a " +
            "LEFT JOIN PO_Quotation_Request_Detail b ON b.sTransNox = a.sTransNox AND b.cReversex = " + SQLUtil.toSQL(POQuotationRequestStatus.Reverse.INCLUDE) +
            "LEFT JOIN PO_Quotation_Request_Supplier c ON c.sTransNox = a.sTransNox AND c.cReversex = " + SQLUtil.toSQL(POQuotationRequestStatus.Reverse.INCLUDE) +
            "LEFT JOIN Industry d ON d.sIndstCdx = a.sIndstCdx " +
            "LEFT JOIN Company e ON e.sCompnyID = c.sCompnyID " +
            "LEFT JOIN Department f ON f.sDeptIDxx = a.sDeptIDxx " +
            "LEFT JOIN Branch g ON g.sBranchCd = a.sBranchCd " +
            "LEFT JOIN Branch h ON h.sBranchCd = a.sDestinat " +
            "LEFT JOIN Client_Master i ON i.sClientID = c.sSupplier " +
            "LEFT JOIN Category j On j.sCategrCd = a.sCategrCd " +
            "LEFT JOIN Category_Level2 k ON k.sCategrCd = a.sCategCd2 " +
            "LEft JOIN Inventory l ON l.sStockIDx = b.sStockIDx " +
            "LEFT JOIN Category m ON m.sCategrCd = l.sCategCd1 " +
            "Left Join Category_Level2 n ON n.sCategrCd = l.sCategCd2 " +
            "LEFT JOIN Category_Level3 o ON o.sCategrCd = l.sCategCd3 " +
            "LEFT JOIN Category_Level4 p ON p.sCategrCd = l.sCategCd4 " +
            "LEFT JOIN Brand q ON q.sBrandIDx = l.sBrandIDx " +
            "LEFT JOIN Model r ON r.sModelIDx = l.sModelIDx " +
            "LEFT JOIN Model_Variant s ON s.sModelIDx = l.sModelIDx " +
            "LEFT JOIN Color t ON t.sColorIDx = l.sColorIDx " +
            "LEFT JOIN Measure u ON u.sMeasurID = l.sMeasurID " +
            "LEFT JOIN Term v ON v.sTermCode = c.sTermCode " ;
    }
    
    //Print Report
    public JSONObject printReport(boolean isSummary, String fsDateFrom, String fsDateTo)
            throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();    
        JasperReport jasperReport = null;      
        pbShowed = false; 
        pbIsPrinted = false;
        DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // adjust if needed
        DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy");

        LocalDate dateFrom = LocalDate.parse(fsDateFrom, inputFormat);
        LocalDate dateTo = LocalDate.parse(fsDateTo, inputFormat);

        String formattedFrom = dateFrom.format(outputFormat);
        String formattedTo = dateTo.format(outputFormat);
        
        if(paReport == null || paReport.size() <= 0 ){
            poJSON.put("result", "error");
            poJSON.put("message",  "No data loaded.");
            return poJSON;
        }
        
        try {
            String jrxmlPath = System.getProperty("sys.default.path.config") + "/Reports/POQuotationRequestSummary.jrxml";
            if(!isSummary){
                jrxmlPath = System.getProperty("sys.default.path.config") + "/Reports/POQuotationRequestDetail.jrxml";
            }
            jasperReport = JasperCompileManager.compileReport(jrxmlPath);
            
            Model_PO_Quotation_Request_Master loMaster = new QuotationModels(poGRider).POQuotationRequestMaster();
            Model_PO_Quotation_Request_Supplier loSupplier = new QuotationModels(poGRider).POQuotationRequestSupplier();
            loMaster.setIndustryId(psIndustryId);
            loMaster.setCategoryCode(psCategorCd);
            loSupplier.setCompanyId(psCompanyId);
            
            Map<String, Object> params = new HashMap<>();
            params.put("sCompanyName", loSupplier.Company().getCompanyName());
            params.put("sCompanyAddress", loSupplier.Company().getCompanyAddress()); //new java.sql.Date(Master().getTransactionDate().getTime())
            params.put("sDateRange", "From " + formattedFrom + " - " + formattedTo);
            
            JasperPrint currentPrint = JasperFillManager.fillReport(
                    jasperReport,
                    params,
                    new JRBeanCollectionDataSource(paReport)
            );
            
            
            showViewerAndWait(currentPrint);
        } catch (JRException | SQLException | GuanzonException  ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction print aborted!");
        } 

        return poJSON;
    }
    
    private void showViewerAndWait(JasperPrint print) {
        // create viewer on Swing thread
        final CustomJasperViewer viewer = new CustomJasperViewer(print);
        viewer.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        final CountDownLatch latch = new CountDownLatch(1);
        viewer.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                latch.countDown();             // release when user closes
                proceedAfterViewerClosed();
            }
        });

        javax.swing.SwingUtilities.invokeLater(() -> viewer.setVisible(true));

        try {
            latch.await();                     // 🔴 blocks here until viewer closes
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); // keep interruption status
        }
    }
    
    private boolean pbShowed = false;
    private void proceedAfterViewerClosed() {
        Platform.runLater(() -> {
            System.out.println("SHOWED!!!!!!!!!!!");

            if (pbShowed) {
                return;
            } else {
                pbShowed = true;
            }

            if ("error".equals((String) poJSON.get("result"))) {
                ShowMessageFX.Warning(null, "Computerized Accounting System",
                    (String) poJSON.get("message"));
            } else {
                if (pbIsPrinted) {
                    ShowMessageFX.Information(null, "Computerized Accounting System",
                        "Report Printed/Saved Successfully");
                } else {
                    ShowMessageFX.Warning(null, "Computerized Accounting System",
                        "Printing was canceled by the user.");
                }
            }
        });
    }
    
    private boolean pbIsPrinted = false;
    public class CustomJasperViewer extends JasperViewer {

        public CustomJasperViewer(final JasperPrint jasperPrint) {
            super(jasperPrint, false);
            customizePrintButton(jasperPrint);
        }

        /* ---- toolbar patch ------------------------------------------ */
        private JSONObject customizePrintButton(final JasperPrint jasperPrint) {

            try {
                JRViewer viewer = findJRViewer(this);
                if (viewer == null) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "JRViewer not found!");
                    return poJSON;
                }
                for (int i = 0; i < viewer.getComponentCount(); i++) {
                    if (viewer.getComponent(i) instanceof JRViewerToolbar) {

                        JRViewerToolbar toolbar = (JRViewerToolbar) viewer.getComponent(i);

                        for (int j = 0; j < toolbar.getComponentCount(); j++) {
                            if (toolbar.getComponent(j) instanceof JButton) {

                                final JButton button = (JButton) toolbar.getComponent(j);
                                switch(button.getToolTipText()){
                                    case "Print":
                                        /* remove existing handlers */
                                        ActionListener[] old = button.getActionListeners();
                                        for (int k = 0; k < old.length; k++) {
                                            button.removeActionListener(old[k]);
                                        }

                                        /* add our own (anonymous inner‑class, not lambda) */
                                        button.addActionListener(new ActionListener() {
                                            @Override
                                            public void actionPerformed(ActionEvent e) {
                                                try {
                                                    pbIsPrinted = JasperPrintManager.printReport(jasperPrint, true);
                                                    if (pbIsPrinted) {
                                                        poJSON.put("result", "success");
                                                        poJSON.put("message",  "Transaction Printed Successfully.");
                                                        CustomJasperViewer.this.dispose();
                                                    } else {
                                                        poJSON.put("result", "error");
                                                        poJSON.put("message",  "Printing was canceled by the user.");
                                                    }
                                                } catch (JRException ex) {
                                                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
                                                }
                                            }
                                        });      
                                        break;
                                    case "Save":
                                        // remove existing handlers
                                        ActionListener[] oldSave = button.getActionListeners();
                                        for (int k = 0; k < oldSave.length; k++) {
                                            button.removeActionListener(oldSave[k]);
                                        }

                                        button.addActionListener(new ActionListener() {
                                            @Override
                                            public void actionPerformed(ActionEvent e) {
                                                try {
                                                    JFileChooser fileChooser = new JFileChooser();
                                                    fileChooser.setDialogTitle("Save Report");

                                                    // File filters
                                                    FileNameExtensionFilter pdfFilter = new FileNameExtensionFilter("PDF Files", "pdf");
                                                    FileNameExtensionFilter xlsFilter = new FileNameExtensionFilter("Excel Files", "xlsx");
                                                    FileNameExtensionFilter docxFilter = new FileNameExtensionFilter("Word Files", "docx");

                                                    fileChooser.addChoosableFileFilter(pdfFilter);
                                                    fileChooser.addChoosableFileFilter(xlsFilter);
                                                    fileChooser.addChoosableFileFilter(docxFilter);
                                                    fileChooser.setFileFilter(pdfFilter); // default

                                                    int userSelection = fileChooser.showSaveDialog(null);

                                                    if (userSelection == JFileChooser.APPROVE_OPTION) {
                                                        File file = fileChooser.getSelectedFile();
                                                        String path = file.getAbsolutePath();

                                                        javax.swing.filechooser.FileFilter selectedFilter = fileChooser.getFileFilter();

                                                        if (selectedFilter == pdfFilter) {
                                                            if (!path.endsWith(".pdf")) path += ".pdf";
                                                            JasperExportManager.exportReportToPdfFile(jasperPrint, path);

                                                        } else if (selectedFilter == xlsFilter) {
                                                            if (!path.endsWith(".xlsx")) path += ".xlsx";

                                                            JRXlsxExporter exporter = new JRXlsxExporter();
                                                            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                                                            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(path));

                                                            SimpleXlsxReportConfiguration config = new SimpleXlsxReportConfiguration();
                                                            config.setDetectCellType(true);
                                                            config.setCollapseRowSpan(false);

                                                            exporter.setConfiguration(config);
                                                            exporter.exportReport();

                                                        } else if (selectedFilter == docxFilter) {
                                                            if (!path.endsWith(".docx")) path += ".docx";

                                                            JRDocxExporter exporter = new JRDocxExporter();
                                                            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                                                            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(path));
                                                            exporter.exportReport();
                                                        }
                                                        pbIsPrinted = true;
                                                        poJSON.put("result", "success");
                                                        poJSON.put("message", "Report saved successfully.");
                                                        CustomJasperViewer.this.dispose();

                                                    } else {
                                                        pbIsPrinted = false;
                                                        poJSON.put("result", "error");
                                                        poJSON.put("message", "Saving was canceled by the user.");
                                                    }

                                                } catch (Exception ex) {
                                                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                                                }
                                            }
                                        });
                                        break;
                                    default:
                                    // Disable all other buttons
                                    button.setEnabled(false);
                                }
                            }
                        }
                        toolbar.revalidate();
                        toolbar.repaint();
                    }
                }
            } catch (Exception e) {
                System.out.println("Error customizing print button: " + e.getMessage());
                poJSON.put("result", "error");
                poJSON.put("message", "Error customizing print button: " + e.getMessage());
            }
            
            return poJSON;
        }
        
        private JRViewer findJRViewer(Component parent) {
            if (parent instanceof JRViewer) {
                return (JRViewer) parent;
            }
            if (parent instanceof Container) {
                Component[] comps = ((Container) parent).getComponents();
                for (int i = 0; i < comps.length; i++) {
                    JRViewer v = findJRViewer(comps[i]);
                    if (v != null) {
                        return v;
                    }
                }
            }
            return null;
        }
    }
    
    public static class TransactionDetail {
        private final Integer nRowNo;
        private final Integer nRow;
        private final String sTransNo;
        private final Date dTransDate;
        private final String sRefNo;
        private final String sBranch;
        private final String sDestination;
        private final String sDepartment;
        private final String sCompany;
        private final String sCategory;
        private final String sSupplier;
        private final String sTerm;
        private final String sItem;
        private final String sStatus;
        private final Double nCost;
        private final Double nQty;
        private final Double nTotal;
        private final Double nTransTotal;

        public TransactionDetail(Integer nRowNo,
                                 String sTransNo,
                                 Date dTransDate,
                                 String sRefNo,
                                 String sBranch,
                                 String sDestination,
                                 String sDepartment,
                                 String sCompany,
                                 String sCategory,
                                 String sSupplier,
                                 String sTerm,
                                 Integer nRow,
                                 String sItem,
                                 Double nCost,
                                 Double nQty,
                                 Double nTotal,
                                 Double nTransTotal,
                                 String sStatus) {

            this.nRowNo = nRowNo;
            this.sTransNo = sTransNo;
            this.dTransDate = dTransDate;
            this.sRefNo = sRefNo;
            this.sBranch = sBranch;
            this.sDestination = sDestination;
            this.sDepartment = sDepartment;
            this.sCompany = sCompany;
            this.sCategory = sCategory;
            this.sSupplier = sSupplier;
            this.sTerm = sTerm;
            this.sStatus = sStatus;
            this.nRow = nRow;
            this.sItem = sItem;
            this.nCost = nCost;
            this.nQty = nQty;
            this.nTotal = nTotal;
            this.nTransTotal = nTransTotal;
        }

        public Integer getnRowNo() { return nRowNo; }
        public Integer getnRow() { return nRow; }
        public String getsTransNo() { return sTransNo; }
        public Date getdTransDate() { return dTransDate; }
        public String getsRefNo() { return sRefNo; }
        public String getsBranch() { return sBranch; }
        public String getsDestination() { return sDestination; }
        public String getsDepartment() { return sDepartment; }
        public String getsCompany() { return sCompany; }
        public String getsCategory() { return sCategory; }
        public String getsSupplier() { return sSupplier; }
        public String getsTerm() { return sTerm; }
        public String getsItem() { return sItem; }
        public Double getnCost() { return nCost; }
        public Double getnQty() { return nQty; }
        public Double getnTotal() { return nTotal; }
        public Double getnTransTotal() { return nTransTotal; }
        public String getsStatus() { return sStatus; }
    }
}
