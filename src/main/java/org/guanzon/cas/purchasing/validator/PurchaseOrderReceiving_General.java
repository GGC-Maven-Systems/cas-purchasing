/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.validator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.MatrixAuthManager;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.inv.InvTransCons;
import org.guanzon.cas.purchasing.model.Model_POR_Detail;
import org.guanzon.cas.purchasing.model.Model_POR_Master;
import org.guanzon.cas.purchasing.status.PurchaseOrderReceivingStatus;
import org.guanzon.cas.purchasing.status.PurchaseOrderReturnStatus;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela 03-12-2025
 */
public class PurchaseOrderReceiving_General implements GValidator{
    MatrixAuthManager poMatrix;
    GRiderCAS poGRider;
    String psTranStat;
    JSONObject poJSON;
    
    Model_POR_Master poMaster;
    ArrayList<Model_POR_Detail> paDetail;

    String SOURCE_CD = InvTransCons.PURCHASE_RECEIVING;
    
    @Override
    public void setApplicationDriver(Object applicationDriver) {
        poGRider = (GRiderCAS) applicationDriver;
    }

    @Override
    public void setTransactionStatus(String transactionStatus) {
        psTranStat = transactionStatus;
    }

    @Override
    public void setMaster(Object value) {
        poMaster = (Model_POR_Master) value;
    }

    @Override
    public void setDetail(ArrayList<Object> value) {
        paDetail.clear();
        for(int lnCtr = 0; lnCtr <= value.size() - 1; lnCtr++){
            paDetail.add((Model_POR_Detail) value.get(lnCtr));
        }
    }

    @Override
    public void setOthers(ArrayList<Object> value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public JSONObject validate() {
        //validate status change request
        JSONObject loJson = StatusChangeValidator.validatePORcvdStatChange(poMaster, psTranStat);
        if (!"success".equals((String) loJson.get("result"))) {
            return loJson;
        }
        
        try {
            switch (psTranStat){
                case PurchaseOrderReceivingStatus.OPEN:
                    return validateNew();
                case PurchaseOrderReceivingStatus.CONFIRMED:
                    return validateConfirmed();
                case PurchaseOrderReceivingStatus.POSTED:
                    return validatePosted();
                case PurchaseOrderReceivingStatus.PAID:
                    return validatePaid();
                case PurchaseOrderReceivingStatus.CANCELLED:
                    return validateCancelled();
                case PurchaseOrderReceivingStatus.VOID:
                    return validateVoid();
                case PurchaseOrderReceivingStatus.RETURNED:
                    return validateReturned();
                default:
                    poJSON = new JSONObject();
                    poJSON.put("result", "success");
            }
        } catch (SQLException ex) {
            Logger.getLogger(PurchaseOrderReceiving_General.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GuanzonException ex) {
            Logger.getLogger(PurchaseOrderReceiving_General.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return poJSON;
    }
    
    private JSONObject validateNew() throws SQLException{
        poJSON = new JSONObject();
        Date loTransactionDate = poMaster.getTransactionDate();
        Date loReferenceDate = poMaster.getReferenceDate();
        LocalDate serverDate = strToDate(xsDateShort(poGRider.getServerDate()));
        LocalDate oneYearAgo = serverDate.minusYears(1);
        
        if (loTransactionDate == null) {
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }

        if ("1900-01-01".equals(xsDateShort(loTransactionDate))) {
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }
        LocalDate transactionDate = strToDate(xsDateShort(poMaster.getTransactionDate()));
        if (transactionDate.isAfter(serverDate)) {
            poJSON.put("message", "Future transaction dates are not allowed.");
            return poJSON;
        }
        // Backdated beyond 1 year validation
        if (transactionDate.isBefore(oneYearAgo)) {
            poJSON.put("message", "Backdated transactions beyond 1 year are not allowed.");
            return poJSON;
        }
//        if (poMaster.getIndustryId() == null) {
//            poJSON.put("message", "Industry is not set.");
//            return poJSON;
//        }
        if (poMaster.getCompanyId() == null || poMaster.getCompanyId().isEmpty()) {
            poJSON.put("message", "Company is not set.");
            return poJSON;
        }
        if (poMaster.getDepartmentId()== null || poMaster.getDepartmentId().isEmpty()) {
            poJSON.put("message", "Department is not set.");
            return poJSON;
        }
        if (poMaster.getCategoryCode()== null || poMaster.getCategoryCode().isEmpty()) {
            poJSON.put("message", "Category is not set.");
            return poJSON;
        }
        //Do not validate supplier: PO Receiving is allowed to save without supplier according to sir mac
        //Need to validate supplier : based on meeting 04142025
        if (poMaster.getSupplierId() == null || poMaster.getSupplierId().isEmpty()) {
            poJSON.put("message", "Supplier is not set.");
            return poJSON;
        }
//        if (poMaster.getTruckingId()== null || poMaster.getTruckingId().isEmpty()) {
//            poJSON.put("message", "Trucking is not set.");
//            return poJSON;
//        }
        if (loReferenceDate == null) {
            poJSON.put("message", "Invalid Reference Date.");
            return poJSON;
        }
        if ("1900-01-01".equals(xsDateShort(loReferenceDate))) {
            poJSON.put("message", "Invalid Reference Date.");
            return poJSON;
        }
        LocalDate referenceDate = strToDate(xsDateShort(poMaster.getReferenceDate()));
        if (referenceDate.isAfter(serverDate)) {
            poJSON.put("message", "Future reference dates are not allowed.");
            return poJSON;
        }
        if (referenceDate.isAfter(transactionDate)) {
            poJSON.put("message", "Reference date cannot be later than the receiving date.");
            return poJSON;
        }
        // Backdated beyond 1 year validation
        if (referenceDate.isBefore(oneYearAgo)) {
            poJSON.put("message", "Backdated reference dates beyond 1 year are not allowed.");
            return poJSON;
        }
        if (poMaster.getReferenceNo()== null || poMaster.getReferenceNo().isEmpty()) {
            poJSON.put("message", "Reference is not set.");
            return poJSON;
        }
        if (poMaster.getTermCode() == null || poMaster.getTermCode().isEmpty()) {
            poJSON.put("message", "Invalid Term.");
            return poJSON;
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateConfirmed() throws SQLException{
        poJSON = new JSONObject();
        
        if(!poMaster.getTransactionStatus().equalsIgnoreCase(PurchaseOrderReceivingStatus.OPEN)){
            poJSON.put("message", "Invalid Transaction Status.");
            return poJSON;
        }
        
        Date loTransactionDate = poMaster.getTransactionDate();
        Date loReferenceDate = poMaster.getReferenceDate();
        LocalDate serverDate = strToDate(xsDateShort(poGRider.getServerDate()));
        LocalDate oneYearAgo = serverDate.minusYears(1);
        
        if (loTransactionDate == null) {
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }

        if ("1900-01-01".equals(xsDateShort(loTransactionDate))) {
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }
        LocalDate transactionDate = strToDate(xsDateShort(poMaster.getTransactionDate()));
        if (transactionDate.isAfter(serverDate)) {
            poJSON.put("message", "Future transaction dates are not allowed.");
            return poJSON;
        }
        // Backdated beyond 1 year validation
        if (transactionDate.isBefore(oneYearAgo)) {
            poJSON.put("message", "Backdated transactions beyond 1 year are not allowed.");
            return poJSON;
        }
//        if (poMaster.getIndustryId() == null) {
//            poJSON.put("message", "Industry is not set.");
//            return poJSON;
//        }
        if (poMaster.getCompanyId() == null || poMaster.getCompanyId().isEmpty()) {
            poJSON.put("message", "Company is not set.");
            return poJSON;
        }
        if (poMaster.getDepartmentId()== null || poMaster.getDepartmentId().isEmpty()) {
            poJSON.put("message", "Department is not set.");
            return poJSON;
        }
        if (poMaster.getCategoryCode()== null || poMaster.getCategoryCode().isEmpty()) {
            poJSON.put("message", "Category is not set.");
            return poJSON;
        }
        //Do not validate supplier: PO Receiving is allowed to save without supplier according to sir mac
        //Need to validate supplier : based on meeting 04142025
        if (poMaster.getSupplierId() == null || poMaster.getSupplierId().isEmpty()) {
            poJSON.put("message", "Supplier is not set.");
            return poJSON;
        }
//        if (poMaster.getTruckingId()== null || poMaster.getTruckingId().isEmpty()) {
//            poJSON.put("message", "Trucking is not set.");
//            return poJSON;
//        }
        if (loReferenceDate == null) {
            poJSON.put("message", "Invalid Reference Date.");
            return poJSON;
        }
        if ("1900-01-01".equals(xsDateShort(loReferenceDate))) {
            poJSON.put("message", "Invalid Reference Date.");
            return poJSON;
        }
        LocalDate referenceDate = strToDate(xsDateShort(poMaster.getTransactionDate()));
        if (referenceDate.isAfter(serverDate)) {
            poJSON.put("message", "Future reference dates are not allowed.");
            return poJSON;
        }
        // Backdated beyond 1 year validation
        if (referenceDate.isBefore(oneYearAgo)) {
            poJSON.put("message", "Backdated reference dates beyond 1 year are not allowed.");
            return poJSON;
        }
        if (poMaster.getReferenceNo()== null || poMaster.getReferenceNo().isEmpty()) {
            poJSON.put("message", "Reference is not set.");
            return poJSON;
        }
        if (poMaster.getTermCode() == null || poMaster.getTermCode().isEmpty()) {
            poJSON.put("message", "Invalid Term.");
            return poJSON;
        }

        poMatrix = new MatrixAuthManager(poGRider, SOURCE_CD, poMaster.getTransactionNo());
        
        //Get the difference of purchase order receiving and purchase order
        String lsSQL = "SELECT " +
                            "  COALESCE(a.sTransNox, b.sTransNox) AS sTransNox" +
                            ", COALESCE(c.sStockIDx, d.sStockIDx) AS sStockIDx" +
                            ", ((IFNULL(b.nTranTotl, 0) - IFNULL(a.nTranTotl, 0)) / NULLIF(b.nTranTotl, 0) * 100) AS nTotlDiff" +
                            ", ((IFNULL(d.nQuantity, 0) - IFNULL(c.nQuantity, 0)) / NULLIF(d.nQuantity, 0) * 100) AS nQtyxDiff" +
                            ", ((IFNULL(d.nUnitPrce, 0) - IFNULL(c.nUnitPrce, 0)) / NULLIF(d.nUnitPrce, 0) * 100) AS nPrceDiff" +
                            ", c.sOrderNox" +
                            ", b.nTranTotl nPTotlAmt" +
                            ", a.nTranTotl nRTotlAmt" +
                            ", d.nQuantity nPOQtyxxx" +
                            ", c.nQuantity nRcQtyxxx" +
                            ", d.nUnitPrce nPUntPrce" +
                            ", c.nUnitPrce nRUntPrce" +
                      " FROM PO_Receiving_Master a" +
                            " LEFT JOIN PO_Receiving_Detail c ON a.sTransNox = c.sTransNox" +
                            " LEFT JOIN PO_Detail d ON c.sOrderNox = d.sTransNox AND c.sStockIDx = d.sStockIDx " +
                            " LEFT JOIN PO_Master b ON d.sTransNox = b.sTransNox" +
                      " WHERE a.sTransNox = " + SQLUtil.toSQL(poMaster.getTransactionNo()) +
                      " UNION" +
                      " SELECT " +
                            "  COALESCE(a.sTransNox, b.sTransNox) AS sTransNox" +
                            ", COALESCE(c.sStockIDx, d.sStockIDx) AS sStockIDx" +
                            ", ((IFNULL(b.nTranTotl, 0) - IFNULL(a.nTranTotl, 0)) / NULLIF(b.nTranTotl, 0) * 100) AS nTotlDiff" +
                            ", ((IFNULL(d.nQuantity, 0) - IFNULL(c.nQuantity, 0)) / NULLIF(d.nQuantity, 0) * 100) AS nQtyxDiff" +
                            ", ((IFNULL(d.nUnitPrce, 0) - IFNULL(c.nUnitPrce, 0)) / NULLIF(d.nUnitPrce, 0) * 100) AS nPrceDiff" +
                            ", c.sOrderNox" +
                            ", b.nTranTotl nPTotlAmt" +
                            ", a.nTranTotl nRTotlAmt" +
                            ", d.nQuantity nPOQtyxxx" +
                            ", c.nQuantity nRcQtyxxx" +
                            ", d.nUnitPrce nPUntPrce" +
                            ", c.nUnitPrce nRUntPrce" +
                      " FROM PO_Receiving_Master a" +
                            " RIGHT JOIN PO_Receiving_Detail c ON a.sTransNox = c.sTransNox" +
                            " RIGHT JOIN PO_Detail d ON c.sOrderNox = d.sTransNox AND c.sStockIDx = d.sStockIDx " +
                            " RIGHT JOIN PO_Master b ON d.sTransNox = b.sTransNox" +
                      " WHERE a.sTransNox = " + SQLUtil.toSQL(poMaster.getTransactionNo());
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        //set to 100% meaning no purchase order
        double lnTotlDiff = 100;
        double lnQtyxDiff = 100;
        double lnPrceDiff = 100;
        
        double lnPTotlAmt = 0;
        double lnRTotlAmt = 0;
        
        int lnRecCtrxx = 0;
        int lnNullCtrx = 0;
        
        //Check for the overall difference between the purchase delivery and purchase order...
        String lsRemarks;
        while(loRS.next()){
            //Get the total purchase receiving total amount
            if(lnRTotlAmt == 0){
                lnRTotlAmt = loRS.getDouble("nRTotlAmt");
            }

            //Count the number of records and the number of records with no order no
            lnRecCtrxx++;
            if(loRS.getString("sOrderNox").isEmpty()){
                lnNullCtrx++;
                continue;
            }
            
            //set the purchase order total, purchase receiving total, and their difference if not yet set
            if(lnTotlDiff != 100){
                lnTotlDiff = loRS.getDouble("nTotlDiff");
                lnPTotlAmt = loRS.getDouble("nPTotlAmt");
            }  
            
            //A difference of positive value means that purchase order quantity is higher than purchase delivery quantity
            //A difference of negative value means that purchase order quantity is lower than purchase delivery quantity
            //A difference of zero(0) means the quantity is the same...
            if(loRS.getDouble("nQtyxDiff") < lnQtyxDiff){
                lnQtyxDiff = loRS.getDouble("nQtyxDiff");
            }
            
            //A difference of positive value means that purchase order price is higher than purchase delivery price
            //A difference of negative value means that purchase order price is lower than purchase delivery price
            //A difference of zero(0) means the price is the same...
            if(loRS.getDouble("nPrceDiff") < lnPrceDiff){
                lnPrceDiff = loRS.getDouble("nPrceDiff");
            }
        }

        //Is the purchase delivery  without a purchase order or is there an item delivered but not ordered
        if(lnRecCtrxx == lnNullCtrx || lnNullCtrx != 0){
            lsRemarks = poMaster.getBranchCode()  
                + "/" + poMaster.getTransactionNo() 
                + ";" + SQLUtil.dateFormat(poMaster.getTransactionDate(), "yyyy-MM-dd") 
                + ";" + poMaster.getTransactionTotal().toString() 
                + ";" + poMaster.getRemarks();
            
            String lsAuthCode = poMatrix.getAuthType("PURCHASE ORDER MATRIX", String.valueOf(poMaster.getTransactionTotal().toString()), "");
            poMatrix.addAuthRequest(lsAuthCode, "", "", lsRemarks);
        }
        else{
            //check if price of delivered items are higher than the purchase order
            if(lnPrceDiff < 0){
                lsRemarks = poMaster.getBranchCode()
                    + "/" + poMaster.getTransactionNo() 
                    + ";" + SQLUtil.dateFormat(poMaster.getTransactionDate(), "yyyy-MM-dd") 
                    + "; Discrepancy in Unit Price: " + String.valueOf(Math.abs(lnPrceDiff)) 
                    + ";" + poMaster.getRemarks();

                String lsAuthCode = poMatrix.getAuthType("PURCHASE PRICE DISCREPANCY", String.valueOf(Math.abs(lnPrceDiff)), "");
                if(!lsAuthCode.isEmpty()){
                    poMatrix.addAuthRequest(lsAuthCode, "", "", lsRemarks);
                }
            }
            
            //check if quantity of delivered items are higher than the purchase order
            if(lnQtyxDiff < 0){
                lsRemarks = poMaster.getBranchCode()
                    + "/" + poMaster.getTransactionNo() 
                    + ";" + SQLUtil.dateFormat(poMaster.getTransactionDate(), "yyyy-MM-dd") 
                    + "; Discrepancy in Quantity Delivered: " + String.valueOf(Math.abs(lnQtyxDiff)) 
                    + ";" + poMaster.getRemarks();

                String lsAuthCode = poMatrix.getAuthType("PURCHASE QUANTITY DISCREPANCY", String.valueOf(Math.abs(lnQtyxDiff)), "");
                if(!lsAuthCode.isEmpty()){
                    poMatrix.addAuthRequest(lsAuthCode, "", "", lsRemarks);
                }
            }
            
            //chekc if quantity or price from delivery is higher the purchase order
            if(lnPrceDiff < 0 || lnQtyxDiff < 0 ){
                String lsAuthCod1 = poMatrix.getAuthType("PURCHASE ORDER MATRIX", String.valueOf(lnRTotlAmt), "");
                String lsAuthCod2 = poMatrix.getAuthType("PURCHASE ORDER MATRIX", String.valueOf(lnPTotlAmt), "");

                //If authcode of purchase order is different from supposedly authcode of purchase delivery then create authcode
                if(!lsAuthCod1.equalsIgnoreCase(lsAuthCod2)){
                    lsRemarks = poMaster.getBranchCode()  
                        + "/" + poMaster.getTransactionNo() 
                        + ";" + SQLUtil.dateFormat(poMaster.getTransactionDate(), "yyyy-MM-dd") 
                        + ";" + poMaster.getTransactionTotal().toString() 
                        + ";" + poMaster.getRemarks();

                    poMatrix.addAuthRequest(lsAuthCod1, "", "", lsRemarks);
                }
            }
        }
        
        if(poMatrix.hasAuthRequest()){
            try {
                poJSON = poMatrix.processAuth();
            } catch (GuanzonException ex) {
                poJSON.put("result", "error");
                poJSON.put("message", ex.getMessage());
                return poJSON;
            }
            return poJSON;
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateApproved(){
        poJSON = new JSONObject();
    
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateProcessed(){
        poJSON = new JSONObject();
                
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validatePosted() throws SQLException, GuanzonException{
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validatePaid(){
        poJSON = new JSONObject();
                
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateCancelled() throws SQLException{
        poJSON = new JSONObject();
        
        //TODO
//        poJSON = checkPOReturn();
//        if("error".equals((String) poJSON.get("result"))){
//            poJSON.put("message", "Found existing Purchase Order Return <" + (String) poJSON.get("sTransNox") + ">. Cancellation of transaction aborted.");
//            return poJSON;
//        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateVoid() throws SQLException{
        poJSON = new JSONObject();
        
        //TODO
//        poJSON = checkPOReturn();
//        if("error".equals((String) poJSON.get("result"))){
//            poJSON.put("message", "Found existing Purchase Order Return <" + (String) poJSON.get("sTransNox") + ">. Voiding of transaction aborted.");
//            return poJSON;
//        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject validateReturned() throws SQLException{
        poJSON = new JSONObject();
        
            //TODO
//        poJSON = checkPOReturn();
//        if("error".equals((String) poJSON.get("result"))){
//            poJSON.put("message", "Found existing Purchase Order Return <" + (String) poJSON.get("sTransNox") + ">. Returning of transaction aborted.");
//            return poJSON;
//        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    //check existing PO Return
    private JSONObject checkPOReturn() throws SQLException{
        poJSON = new JSONObject();
        String lsPOReturn = "";
        String lsSQL = " SELECT "
                + " a.sTransNox "
                + " FROM po_return_master a ";
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sSourceNo = " + SQLUtil.toSQL(poMaster.getTransactionNo())
                + " AND ( a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReturnStatus.CONFIRMED)
                + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReturnStatus.PAID)
                + " OR a.cTranStat = " + SQLUtil.toSQL(PurchaseOrderReturnStatus.POSTED)
                + " ) ");
        
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0) {
                while (loRS.next()) {
                    if(lsPOReturn.isEmpty()){
                        lsPOReturn = loRS.getString("sTransNox");
                    } else {
                        lsPOReturn = lsPOReturn + ", " +loRS.getString("sTransNox");
                    }
                }
                poJSON.put("result", "error");
                poJSON.put("sTransNox", lsPOReturn);
                return poJSON;
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
        }
        
        poJSON.put("result", "success");
        return poJSON; 
    }
    
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
    
}
