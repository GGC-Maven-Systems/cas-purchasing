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
import org.guanzon.cas.parameter.model.Model_Color;
import org.guanzon.cas.parameter.model.Model_Model;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela
 */
public class Model_PO_Quotation_Detail extends Model {
    
    String psBrandId = "";
    String psModelId = "";
    
    //reference objects
    //All reference fields below are intentionally NOT constructed in initialize() - see their
    //accessors, which build them lazily on first access so opening this record never touches
    //those tables. (poModelVariant/poInvMaster/poCategory were previously eager-constructed
    //here too but had no accessor anywhere in this class - removed as dead fields.)
    Model_Brand poBrand;
    Model_Model poModel;
    Model_Color poColor;
    Model_Inventory poInventory;
    Model_Inventory poReplaced;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            //assign default values
            poEntity.updateNull("dModified");
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("nQuantity", 0);
            poEntity.updateObject("nUnitPrce", 0.0000);
            poEntity.updateObject("nDiscRate", 0.00);
            poEntity.updateObject("nDiscAmtx", 0.0000);
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

    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    public JSONObject setEntryNo(int entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public int getEntryNo() {
        if (getValue("nEntryNox") == null || "".equals(getValue("nEntryNox"))) {
            return 0;
        }
        return (int) getValue("nEntryNox");
    }

    public JSONObject setQuantity(Double quantity) {
        return setValue("nQuantity", quantity);
    }

    public Double getQuantity() {
        if (getValue("nQuantity") == null || "".equals(getValue("nQuantity"))) {
            return 0.00;
        }
        return Double.valueOf(getValue("nQuantity").toString());
    }

    public JSONObject setStockId(String stockId) {
        return setValue("sStockIDx", stockId);
    }

    public String getStockId() {
        return (String) getValue("sStockIDx");
    }

    public JSONObject setDescription(String description) {
        return setValue("sDescript", description);
    }

    public String getDescription() {
        return (String) getValue("sDescript");
    }

    public JSONObject setReplaceId(String replaceId) {
        return setValue("sReplacID", replaceId);
    }

    public String getReplaceId() {
        return (String) getValue("sReplacID");
    }

    public JSONObject setReplaceDescription(String ReplaceDescription) {
        return setValue("sReplacDs", ReplaceDescription);
    }

    public String getReplaceDescription() {
        return (String) getValue("sReplacDs");
    }
    
    public JSONObject setUnitPrice(Double unitPrice) {
        return setValue("nUnitPrce", unitPrice);
    }

    public Double getUnitPrice() {
        if (getValue("nUnitPrce") == null || "".equals(getValue("nUnitPrce"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nUnitPrce").toString());
    }
    
    public JSONObject setDiscountRate(Double discountRate) {
        return setValue("nDiscRate", discountRate);
    }

    public Double getDiscountRate() {
        if (getValue("nDiscRate") == null || "".equals(getValue("nDiscRate"))) {
            return 0.00;
        }
        return Double.valueOf(getValue("nDiscRate").toString());
    }
    
    public JSONObject setDiscountAmount(Double discountAmount) {
        return setValue("nDiscAmtx", discountAmount);
    }

    public Double getDiscountAmount() {
        if (getValue("nDiscAmtx") == null || "".equals(getValue("nDiscAmtx"))) {
            return 0.00;
        }
        return Double.valueOf(getValue("nDiscAmtx").toString());
    }
    
    public JSONObject isReverse(boolean isReverse) {
        return setValue("cReversex", isReverse ? "+" : "-");
    }

    public boolean isReverse() {
        return ((String) getValue("cReversex")).equals("+");
    }

    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }
    
    public void setBrandId(String brandId){
        psBrandId = brandId;
    }
    
    public String getBrandId(){
        return psBrandId;
    }
    
    public void setModelId(String modelId){
        psModelId = modelId;
    }
    
    public String getModelId(){
        return psModelId;
    }

    @Override
    public String getNextCode() {
        return "";
    }

    //reference object models
    public Model_Brand Brand() throws GuanzonException, SQLException {
        if (poBrand == null) {
            poBrand = new ParamModels(poGRider).Brand();
        }

        if (!"".equals((String) getValue("sStockIDx")) && (String) getValue("sStockIDx") != null) {
            psBrandId = Inventory().getBrandId();
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

    public Model_Model Model() throws SQLException, GuanzonException {
        if (poModel == null) {
            poModel = new ParamModels(poGRider).Model();
        }

        if (!"".equals((String) getValue("sStockIDx")) && (String) getValue("sStockIDx") != null) {
            psModelId = Inventory().getModelId();
            setModelId(Inventory().getModelId());
        }

        if (!"".equals(getBrandId())) {
            if (poModel.getEditMode() == EditMode.READY
                    && poModel.getBrandId().equals(getBrandId())) {
                return poModel;
            } else {
                if (ReferenceCache.tryLoad("Model", getBrandId(), poModel)) {
                    return poModel;
                }

                poJSON = poModel.openRecord(getBrandId());
                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Model", getBrandId(), poModel);
                    return poModel;
                } else {
                    poModel.initialize();
                    return poModel;
                }
            }
        } else {
            poModel.initialize();
            return poModel;
        }
    }

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
    
    public Model_Inventory ReplacedInventory() throws SQLException, GuanzonException {
        if (poReplaced == null) {
            poReplaced = new InvModels(poGRider).Inventory();
        }

        if (!"".equals((String) getValue("sReplacID"))) {
            if (poReplaced.getEditMode() == EditMode.READY
                    && poReplaced.getStockId().equals((String) getValue("sReplacID"))) {
                return poReplaced;
            } else {
                poJSON = poReplaced.openRecord((String) getValue("sReplacID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    return poReplaced;
                } else {
                    poReplaced.initialize();
                    return poReplaced;
                }
            }
        } else {
            poReplaced.initialize();
            return poReplaced;
        }
    }
    
    public JSONObject openRecord(String transactionNo, String stockId) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsSQL = MiscUtil.makeSelect(this);
        lsSQL = MiscUtil.addCondition(lsSQL, " sTransNox = " + SQLUtil.toSQL(transactionNo) 
                                        + " AND ( sStockIDx = " + SQLUtil.toSQL(stockId)
                                        + " OR sReplacID = " + SQLUtil.toSQL(stockId)
                                        + " )"
                                        );
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
