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
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.services.InvModels;
import org.guanzon.cas.parameter.model.Model_Brand;
import org.guanzon.cas.parameter.services.ParamModels;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.guanzon.cas.purchasing.services.PurchaseOrderReturnModels;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela 03-12-2025
 */
public class Model_POR_Detail extends Model{
    
    String psBrandId = "";
    
    //reference objects
    //poBrand/poInventory/poPurchaseOrder/poPurchaseOrderReturn/poPurchaseOrderReturnDetail are
    //intentionally NOT constructed in initialize() - see their accessors below, which build them
    //lazily on first access so opening this record never touches those tables.
    Model_Brand poBrand;
    Model_Inventory poInventory;
    Model_PO_Master poPurchaseOrder;
    Model_POReturn_Master poPurchaseOrderReturn;
    Model_POReturn_Detail poPurchaseOrderReturnDetail;
    
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());
            
            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            
            //assign default values
            poEntity.updateObject("dExpiryDt", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("dModified", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("cSerialze", "0");
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("nWHCountx", 0.00);
            poEntity.updateObject("nQuantity", 0.00);
            poEntity.updateObject("nOrderQty", 0.00);
            poEntity.updateObject("nUnitPrce", 0.0000);
            poEntity.updateObject("nFreightx", 0.00);
            poEntity.updateObject("nDiscount", 0.00);
            poEntity.updateObject("nAddDiscx", 0.0000);
            poEntity.updateObject("cReversex", "+");
//            poEntity.updateObject("nTranTotl", 0.00);
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
    
    public String getTransactionNo  (){
        return (String) getValue("sTransNox");
    }
    
    public JSONObject setEntryNo(int entryNo){
        return setValue("nEntryNox", entryNo);
    }
    
    public int getEntryNo(){
        return (int) getValue("nEntryNox");
    }
    
    public JSONObject setOrderNo(String orderNo){
        return setValue("sOrderNox", orderNo);
    }
    
    public String getOrderNo(){
        return (String) getValue("sOrderNox");
    }
    
    public JSONObject setStockId(String stockID){
        return setValue("sStockIDx", stockID);
    }
    
    public String getStockId(){
        return (String) getValue("sStockIDx");
    }
    
    public JSONObject setReplaceId(String replaceID){
        return setValue("sReplacID", replaceID);
    }
    
    public String getReplaceId(){
        return (String) getValue("sReplacID");
    }
    
    public JSONObject setUnitType(String unitType){
        return setValue("cUnitType", unitType);
    }
    
    public String getUnitType(){
        return (String) getValue("cUnitType");
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
    
    public JSONObject setTotal(Number total){
        return setValue("nTranTotl", total);
    }
    
    public Number getTotal(){
        if(getValue("nTranTotl") == null || "".equals(getValue("nTranTotl"))){
            return 0.0000;
        } 
        return (Number) getValue("nTranTotl");
    }
    
    public JSONObject setExpiryDate(Date expiryDate){
        return setValue("dExpiryDt", expiryDate);
    }
    
    public Date getExpiryDate(){
        return (Date) getValue("dExpiryDt");
    }
    
    public JSONObject setWhCount(Number whCount){
        return setValue("nWHCountx", whCount);
    }
    
    public Number getWhCount(){
        if(getValue("nWHCountx") == null || "".equals(getValue("nWHCountx"))){
            return 0.00;
        } 
        return (Number) getValue("nWHCountx");
    }
    
    public JSONObject setOrderQty(Number orderQty){
        return setValue("nOrderQty", orderQty);
    }
    
    public Number getOrderQty(){
        if(getValue("nOrderQty") == null || "".equals(getValue("nOrderQty"))){
            return 0.00;
        } 
        return (Number) getValue("nOrderQty");
    }
    
    public JSONObject setDiscountRate(Number discountRate){
        return setValue("nDiscount", discountRate);
    }
    
    public Number getDiscountRate(){
        if(getValue("nDiscount") == null || "".equals(getValue("nDiscount"))){
            return 0.00;
        } 
        return (Number) getValue("nDiscount");
    }
    
    public JSONObject setDiscountAmount(Number discountAmount){
        return setValue("nAddDiscx", discountAmount);
    }
    
    public Number getDiscountAmount(){
        if(getValue("nAddDiscx") == null || "".equals(getValue("nAddDiscx"))){
            return 0.0000;
        } 
        return (Number) getValue("nAddDiscx");
    }
    
    public JSONObject isVatable(boolean isVatable){
        return setValue("cWithVATx", isVatable ? "1" : "0");
    } 
    
    public boolean isVatable(){
        return ((String) getValue("cWithVATx")).equals("1");
    }
    
    public JSONObject isSerialized(boolean isSerialize){
        return setValue("cSerialze", isSerialize ? "1" : "0");
    } 
    
    public boolean isSerialized(){
        return ((String) getValue("cSerialze")).equals("1");
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
    
    public void setBrandId(String brandId){
        psBrandId = brandId;
    }
    
    public String getBrandId(){
        return psBrandId;
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

        if (!"".equals((String) getValue("sStockIDx"))) {
            if (poInventory.getEditMode() == EditMode.READY
                    && poInventory.getStockId().equals((String) getValue("sStockIDx"))) {
                return poInventory;
            } else {
                poJSON = poInventory.openRecord((String) getValue("sStockIDx"));

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

    public Model_Inventory Supersede() throws SQLException, GuanzonException {
        if (poInventory == null) {
            poInventory = new InvModels(poGRider).Inventory();
        }

        if (!"".equals((String) getValue("sReplacID"))) {
            if (poInventory.getEditMode() == EditMode.READY
                    && poInventory.getStockId().equals((String) getValue("sReplacID"))) {
                return poInventory;
            } else {
                poJSON = poInventory.openRecord((String) getValue("sReplacID"));

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
    
    public Model_Brand Brand() throws GuanzonException, SQLException {
        if (poBrand == null) {
            poBrand = new ParamModels(poGRider).Brand();
        }

        if (!"".equals((String) getValue("sStockIDx")) && (String) getValue("sStockIDx") != null) {
            setBrandId(Inventory().getBrandId());
        }

        if (!"".equals(getBrandId())) {
            if (poBrand.getEditMode() == EditMode.READY
                    && poBrand.getBrandId().equals(getBrandId())) {
                return poBrand;
            } else {
                if (ReferenceCache.tryLoad("Brand", getBrandId(), poBrand)) {
                    return poBrand;
                }

                poJSON = poBrand.openRecord(getBrandId());
                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Brand", getBrandId(), poBrand);
                    return poBrand;
                } else {
                    poBrand.initialize();
                    return poBrand;
                }
            }
        } else {
            poBrand.initialize();
            return poBrand;
        }
    }

    public Model_PO_Master PurchaseOrderMaster() throws SQLException, GuanzonException {
            if (poPurchaseOrder == null) {
                poPurchaseOrder = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
            }

            if (!"".equals((String) getValue("sOrderNox"))) {
                if (poPurchaseOrder.getEditMode() == EditMode.READY
                        && poPurchaseOrder.getTransactionNo().equals((String) getValue("sOrderNox"))) {
                    return poPurchaseOrder;
                } else {
                    poJSON = poPurchaseOrder.openRecord((String) getValue("sOrderNox"));

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
    
    public Model_POReturn_Master PurchaseOrderReturnMaster() throws SQLException, GuanzonException {
        if (poPurchaseOrderReturn == null) {
            poPurchaseOrderReturn = new PurchaseOrderReturnModels(poGRider).PurchaseOrderReturnMaster();
        }

        if (!"".equals((String) getValue("sOrderNox"))) {
            if (poPurchaseOrderReturn.getEditMode() == EditMode.READY
                    && poPurchaseOrderReturn.getTransactionNo().equals((String) getValue("sOrderNox"))) {
                return poPurchaseOrderReturn;
            } else {
                poJSON = poPurchaseOrderReturn.openRecord((String) getValue("sOrderNox"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poPurchaseOrderReturn;
                } else {
                    poPurchaseOrderReturn.initialize();
                    return poPurchaseOrderReturn;
                }
            }
        } else {
            poPurchaseOrderReturn.initialize();
            return poPurchaseOrderReturn;
        }
    }
    
    public Model_POReturn_Detail PurchaseOrderReturnDetail() throws SQLException, GuanzonException {
        if (poPurchaseOrderReturnDetail == null) {
            poPurchaseOrderReturnDetail = new PurchaseOrderReturnModels(poGRider).PurchaseOrderReturnDetails();
        }

        if (!"".equals((String) getValue("sOrderNox"))) {
            if (poPurchaseOrderReturnDetail.getEditMode() == EditMode.READY
                    && poPurchaseOrderReturnDetail.getTransactionNo().equals((String) getValue("sOrderNox"))
                    && poPurchaseOrderReturnDetail.getTransactionNo().equals((String) getValue("sStockIDx"))) {
                return poPurchaseOrderReturnDetail;
            } else {
                poJSON = poPurchaseOrderReturnDetail.openRecord((String) getValue("sOrderNox"),(String) getValue("sStockIDx"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poPurchaseOrderReturnDetail;
                } else {
                    poPurchaseOrderReturnDetail.initialize();
                    return poPurchaseOrderReturnDetail;
                }
            }
        } else {
            poPurchaseOrderReturnDetail.initialize();
            return poPurchaseOrderReturnDetail;
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
    
    public JSONObject openRecord(String transactionNo, String orderNo, String stockId) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsSQL = MiscUtil.makeSelect(this);
        lsSQL = MiscUtil.addCondition(lsSQL, " sTransNox = " + SQLUtil.toSQL(transactionNo) 
                                        + " AND sOrderNox = " + SQLUtil.toSQL(orderNo)
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
