/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.inv.model.Model_Inv_Serial;
import org.guanzon.cas.inv.model.Model_Inv_Serial_Registration;
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.services.InvModels;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela 04-28-2025
 */
public class Model_POReturn_Detail extends Model{
    
    Number psReceiveQty = 1;
    
    //reference objects
    //All reference fields below are intentionally NOT constructed in initialize() - see their
    //accessors, which build them lazily on first access so opening this record never touches
    //those tables.
    Model_Inventory poInventory;
    Model_Inv_Serial poInvSerial;
    Model_Inv_Serial_Registration poInvSerialRegistration;
    Model_PO_Master poPurchaseOrder;
    
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());
            
            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            
            //assign default values
            poEntity.updateObject("dModified", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("nQuantity", 0.00);
            poEntity.updateObject("nReceived", 0.00);
            poEntity.updateObject("nUnitPrce", 0.0000);
            poEntity.updateObject("nFreightx", 0.00);
            poEntity.updateObject("cReversex", "+");
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sTransNox";
            ID2 = "nEntryNox";

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }
    
    public JSONObject setTransactionNo(String transactionNo){
        return setValue("sTransNox", transactionNo);
    }
    
    public String getTransactionNo(){
        return (String) getValue("sTransNox");
    }
    
    public JSONObject setEntryNo(int entryNo){
        return setValue("nEntryNox", entryNo);
    }
    
    public int getEntryNo(){
        return (int) getValue("nEntryNox");
    }
    
    public JSONObject setStockId(String stockID){
        return setValue("sStockIDx", stockID);
    }
    
    public String getStockId(){
        return (String) getValue("sStockIDx");
    }
    
    public JSONObject setUnitType(String unitType){
        return setValue("cUnitType", unitType);
    }
    
    public String getUnitType(){
        return (String) getValue("cUnitType");
    }
    
    public JSONObject setSerialId(String serialId){
        return setValue("sSerialID", serialId);
    }
    
    public String getSerialId(){
        return (String) getValue("sSerialID");
    }
    
    public JSONObject setQuantity(Number quantity){
        return setValue("nQuantity", quantity);
    }
    
    public Number getQuantity(){
        if(getValue("nQuantity") == null || "".equals(getValue("nQuantity"))){
            return 0.00;
        } 
        return (Number) getValue("nQuantity");
    }
    
    public JSONObject setReceivedQty(Number receivedQuantity){
        return setValue("nReceived", receivedQuantity);
    }
    
    public Number getReceivedQty(){
        if(getValue("nReceived") == null || "".equals(getValue("nReceived"))){
            return 0.00;
        } 
        return (Number) getValue("nReceived");
    }
    
    public JSONObject setUnitPrce(Number unitPrce){
        return setValue("nUnitPrce", unitPrce);
    }
    
    public Number getUnitPrce(){
        if(getValue("nUnitPrce") == null || "".equals(getValue("nUnitPrce"))){
            return 0.0000;
        } 
        return (Number) getValue("nUnitPrce");
    }
    
    public JSONObject setFreight(Number freight){
        return setValue("nFreightx", freight);
    }
    
    public Number getFreight(){
        if(getValue("nFreightx") == null || "".equals(getValue("nFreightx"))){
            return 0.00;
        } 
        return (Number) getValue("nFreightx");
    }
    
    public JSONObject setSourceNo(String sourceNo){
        return setValue("sSourceNo", sourceNo);
    }
    
    public String getSourceNo(){
        return (String) getValue("sSourceNo");
    }
    
    public JSONObject setBatchNo(String batchNo){
        return setValue("sBatchNox", batchNo);
    }
    
    public String getBatchNo(){
        return (String) getValue("sBatchNox");
    }
    
    public JSONObject isVatable(boolean isVatable){
        return setValue("cWithVATx", isVatable ? "1" : "0");
    } 
    
    public boolean isVatable(){
        return ((String) getValue("cWithVATx")).equals("1");
    }
    
    public JSONObject isReverse(boolean isReverse) {
        return setValue("cReversex", isReverse ? "+" : "-");
    }

    public boolean isReverse() {
        return ((String) getValue("cReversex")).equals("+");
    }
    
    public JSONObject setModifiedDate(Date modifiedDate){
        return setValue("dModified", modifiedDate);
    }
    
    public Date getModifiedDate(){
        return (Date) getValue("dModified");
    }
    
    @Override
    public String getNextCode() {
        return "";
    }
    
    //reference object models
    public Model_Inventory Inventory() throws SQLException, GuanzonException {
        if (poInventory == null) {
            poInventory = new InvModels(poGRider).Inventory();
        }
        
        String id = (String) (getValue("sStockIDx") == null ? "" : getValue("sStockIDx"));

        if (!"".equals(id)) {
            if (poInventory.getEditMode() == EditMode.READY
                    && poInventory.getStockId().equals(id)) {
                return poInventory;
            } else {
                poJSON = poInventory.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    return poInventory;
                } else {
                    poInventory.initialize();
                    return poInventory;
                }
            }
        } else {
            poInventory.initialize();
            return poInventory;
        }
    }
    
    public Model_Inv_Serial InventorySerial() throws SQLException, GuanzonException {
        if (poInvSerial == null) {
            poInvSerial = new InvModels(poGRider).InventorySerial();
        }
        
        String id = (String) (getValue("sSerialID") == null ? "" : getValue("sSerialID"));

        if (!"".equals(id)) {
            if (poInvSerial.getEditMode() == EditMode.READY
                    && poInvSerial.getSerialId().equals(id)) {
                return poInvSerial;
            } else {
                poJSON = poInvSerial.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    return poInvSerial;
                } else {
                    poInvSerial.initialize();
                    return poInvSerial;
                }
            }
        } else {
            poInvSerial.initialize();
            return poInvSerial;
        }
    }
    
    public Model_Inv_Serial_Registration InventorySerialRegistration() throws SQLException, GuanzonException {
        if (poInvSerialRegistration == null) {
            poInvSerialRegistration = new InvModels(poGRider).InventorySerialRegistration();
        }
        
        String id = (String) (getValue("sSerialID") == null ? "" : getValue("sSerialID"));

        if (!"".equals(id)) {
            if (poInvSerialRegistration.getEditMode() == EditMode.READY
                    && poInvSerialRegistration.getSerialId().equals(id)) {
                return poInvSerialRegistration;
            } else {
                poJSON = poInvSerialRegistration.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    return poInvSerialRegistration;
                } else {
                    poInvSerialRegistration.initialize();
                    return poInvSerialRegistration;
                }
            }
        } else {
            poInvSerialRegistration.initialize();
            return poInvSerialRegistration;
        }
    }
    
    public Model_PO_Master PurchaseOrderMaster() throws SQLException, GuanzonException {
            if (poPurchaseOrder == null) {
                poPurchaseOrder = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
            }
            
            String id = (String) (getValue("sSourceNo") == null ? "" : getValue("sSourceNo"));

            if (!"".equals(id)) {
                if (poPurchaseOrder.getEditMode() == EditMode.READY
                        && poPurchaseOrder.getTransactionNo().equals(id)) {
                    return poPurchaseOrder;
                } else {
                    poJSON = poPurchaseOrder.openRecord(id);

                    if ("success".equals((String) poJSON.get("result"))) {
                        return poPurchaseOrder;
                    } else {
                        poPurchaseOrder.initialize();
                        return poPurchaseOrder;
                    }
                }
            } else {
                poPurchaseOrder.initialize();
                return poPurchaseOrder;
            }
    }
    
    
    public JSONObject openRecord(String transactionNo, String stockId) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsSQL = MiscUtil.makeSelect(this);
        lsSQL = MiscUtil.addCondition(lsSQL, " sTransNox = " + SQLUtil.toSQL(transactionNo) 
                                        + " AND sStockIDx = " + SQLUtil.toSQL(stockId));
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (loRS.next()) {
                for (int lnCtr = 1; lnCtr <= loRS.getMetaData().getColumnCount(); lnCtr++){
                    setValue(lnCtr, loRS.getObject(lnCtr)); 
                }
                MiscUtil.close(loRS);
                pnEditMode = EditMode.READY;
                poJSON = new JSONObject();
                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
            } else {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "No record to load.");
            } 
        } catch (SQLException e) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        } 
        return poJSON;
    }
    
    //end reference object models
}