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
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.inv.model.Model_Inv_Serial;
import org.guanzon.cas.inv.model.Model_Inv_Serial_Ledger;
import org.guanzon.cas.inv.model.Model_Inv_Serial_Registration;
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.services.InvModels;
import org.guanzon.cas.parameter.model.Model_Inv_Location;
import org.guanzon.cas.parameter.services.ParamModels;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingModels;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela
 */
public class Model_POR_Serial extends Model {
    
    //reference objects
    //All reference fields below are intentionally NOT constructed in initialize() - see their
    //accessors, which build them lazily on first access so opening this record never touches
    //those tables.
    Model_Inventory poInventory;
    Model_Inv_Location poLocation;  
    Model_Inv_Serial poInvSerial; 
    Model_Inv_Serial_Registration poInvSerialRegistration; 
    Model_Inv_Serial_Ledger poInvSerialLedger;    
    Model_POR_Detail poPorDetail;
    
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());
            
            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            
            //assign default values
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("dModified", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sTransNox";
            ID2 = "nEntryNox";
            ID3 = "sSerialID";

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }
    
    public JSONObject setTransactionNo(String transactionNo){
        return setValue("sTransNox", transactionNo);
    }
    
    public String getTransactionNo  (){
        return (String) getValue("sTransNox");
    }
    
    public JSONObject setEntryNo(int entryNo){
        return setValue("nEntryNox", entryNo);
    }
    
    public int getEntryNo(){
        return (int) getValue("nEntryNox");
    }
    
    public JSONObject setStockId(String stockId){
        return setValue("sStockIDx", stockId);
    }
    
    public String getStockId(){
        return (String) getValue("sStockIDx");
    }
    
    public JSONObject setSerialId(String serialId){
        return setValue("sSerialID", serialId);
    }
    
    public String getSerialId(){
        return (String) getValue("sSerialID");
    }
    
    public JSONObject setLocationId(String locationId){
        return setValue("sLocatnID", locationId);
    }
    
    public String getLocationId(){
        return (String) getValue("sLocatnID");
    }
    
    public JSONObject setModifiedDate(Date modifiedDate){
        return setValue("dModified", modifiedDate);
    }
    
    public Date getModifiedDate(){
        return (Date) getValue("dModified");
    }
    
    public JSONObject setSerial01(String serialNumber) {
        if (poInvSerial == null) {
            poInvSerial = new InvModels(poGRider).InventorySerial();
        }
        
        return poInvSerial.setSerial01(serialNumber);
    }

    public String getSerial01() {
        if (poInvSerial == null) {
            poInvSerial = new InvModels(poGRider).InventorySerial();
        }
        
        if(poInvSerial.getSerial01() == null){
            return "";
        }
        
        return poInvSerial.getSerial01();
    }

    public JSONObject setSerial02(String serialNumber) {
        if (poInvSerial == null) {
            poInvSerial = new InvModels(poGRider).InventorySerial();
        }
        
        return poInvSerial.setSerial02(serialNumber);
    }

    public String getSerial02() {
        if (poInvSerial == null) {
            poInvSerial = new InvModels(poGRider).InventorySerial();
        }
        
        if(poInvSerial.getSerial02() == null){
            return "";
        }
        
        return poInvSerial.getSerial02();
    }

    public JSONObject setConductionStickerNo(String conductionStickerNo) {
        if (poInvSerialRegistration == null) {
            poInvSerialRegistration = new InvModels(poGRider).InventorySerialRegistration();
        }
        
        return poInvSerialRegistration.setConductionStickerNo(conductionStickerNo);
    }

    public String getConductionStickerNo() {
        if (poInvSerialRegistration == null) {
            poInvSerialRegistration = new InvModels(poGRider).InventorySerialRegistration();
        }
        
        if(poInvSerialRegistration.getConductionStickerNo() == null){
            return "";
        } 
        
        return poInvSerialRegistration.getConductionStickerNo();
        
    }

    public JSONObject setPlateNo(String plateNo) {
        if (poInvSerialRegistration == null) {
            poInvSerialRegistration = new InvModels(poGRider).InventorySerialRegistration();
        }
        
        return poInvSerialRegistration.setPlateNoP(plateNo);
    }

    public String getPlateNo() {
        if (poInvSerialRegistration == null) {
            poInvSerialRegistration = new InvModels(poGRider).InventorySerialRegistration();
        }
        
        if(poInvSerialRegistration.getPlateNoP() == null){
            return "";
        } 
        
        return poInvSerialRegistration.getPlateNoP();
    }
    
    @Override
    public String getNextCode() {
        return "";
//        return MiscUtil.getNextCode(this.getTable(), ID, true, poGRider.getGConnection().getConnection(), poGRider.getBranchCode());
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
    
    public Model_Inv_Location Location() throws SQLException, GuanzonException {
        if (poLocation == null) {
            poLocation = new ParamModels(poGRider).InventoryLocation();
        }

        String id = (String) (getValue("sLocatnID") == null ? "" : getValue("sLocatnID"));
        
        if (!"".equals(id)) {
            if (poLocation.getEditMode() == EditMode.READY
                    && poLocation.getLocationId().equals(id)) {
                return poLocation;
            } else {
                if (ReferenceCache.tryLoad("Inv_Location", id, poLocation)) {
                    return poLocation;
                }

                poJSON = poLocation.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Inv_Location", id, poLocation);
                    return poLocation;
                } else {
                    poLocation.initialize();
                    return poLocation;
                }
            }
        } else {
            poLocation.initialize();
            return poLocation;
        }
    }
    
    public Model_Inv_Serial InventorySerial() throws SQLException, GuanzonException {
        if (poInvSerial == null) {
            poInvSerial = new InvModels(poGRider).InventorySerial();
        }

        String id = (String) (getValue("sSerialID") == null ? "" : getValue("sSerialID"));
        
        if (!"".equals(id)) {
            if (poInvSerial.getEditMode() == EditMode.READY
                    && poInvSerial.getStockId().equals(id)) {
                return poInvSerial;
            } else {
                poJSON = poInvSerial.openRecord(id);

                if ("success".equals(id)) {
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
    
    public Model_Inv_Serial_Ledger InventorySerialLedger() throws SQLException, GuanzonException {
        if (poInvSerialLedger == null) {
            poInvSerialLedger = new InvModels(poGRider).InventorySerialLedger();
        }

        String id = (String) (getValue("sSerialID") == null ? "" : getValue("sSerialID"));
        
        if (!"".equals(id)) {
            if (poInvSerialLedger.getEditMode() == EditMode.READY
                    && poInvSerialLedger.getSerialId().equals(id)) {
                return poInvSerialLedger;
            } else {
                poJSON = poInvSerialLedger.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    return poInvSerialLedger;
                } else {
                    poInvSerialLedger.initialize();
                    return poInvSerialLedger;
                }
            }
        } else {
            poInvSerialLedger.initialize();
            return poInvSerialLedger;
        }
    }
    
    public JSONObject openRecord(String transactionNo, String serialId) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsSQL = MiscUtil.makeSelect(this);
        lsSQL = MiscUtil.addCondition(lsSQL, "sTransNox = " + SQLUtil.toSQL(transactionNo) 
                                        + " AND sSerialID = " + SQLUtil.toSQL(serialId));
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
    
    public Model_POR_Detail PurchaseOrderReceivingDetails() throws SQLException, GuanzonException {
        if (poPorDetail == null) {
            poPorDetail = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingDetails();
        }

        if (!"".equals((String) getValue("sStockIDx"))) {
            if (poPorDetail.getEditMode() == EditMode.READY
                    && (poPorDetail.getEntryNo() == (int) getValue("nEntryNox"))) {
                return poPorDetail;
            } else {
                poJSON = poPorDetail.openRecord((String) getValue("sTransNox"), (int) getValue("nEntryNox"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poPorDetail;
                } else {
                    poPorDetail.initialize();
                    return poPorDetail;
                }
            }
        } else {
            poPorDetail.initialize();
            return poPorDetail;
        }
    }
    
    //end reference object models
}
