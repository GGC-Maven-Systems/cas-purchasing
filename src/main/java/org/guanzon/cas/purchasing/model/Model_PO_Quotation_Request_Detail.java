/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.model;

import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.services.InvModels;
import org.guanzon.cas.parameter.model.Model_Brand;
import org.guanzon.cas.parameter.model.Model_Model;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela
 */
public class Model_PO_Quotation_Request_Detail extends Model {
    
    String psBrandId = "";
    String psModelId = "";
    
    //reference objects
    //poBrand/poModel/poInventory are intentionally NOT constructed in initialize() - see their
    //accessors below, which build them lazily on first access so opening this record never
    //touches those tables. (poModelVariant/poColor/poInvMaster/poCategory were previously
    //eager-constructed here too but had no accessor anywhere in this class - removed as dead
    //fields.)
    Model_Brand poBrand;
    Model_Model poModel;
    Model_Inventory poInventory;

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
    
    public JSONObject setUnitPrice(Double unitPrice) {
        return setValue("nUnitPrce", unitPrice);
    }

    public Double getUnitPrice() {
        if (getValue("nUnitPrce") == null || "".equals(getValue("nUnitPrce"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nUnitPrce").toString());
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
       if(psBrandId == null){
           return "";
       }
        return psBrandId;
    }
    
    public void setModelId(String modelId){
        psModelId = modelId;
    }
    
    public String getModelId(){
       if(psModelId == null){
           return "";
       }
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
                    && getBrandId().equals(poBrand.getBrandId())) {
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

        if (!"".equals(getModelId())) {
            if (poModel.getEditMode() == EditMode.READY
                    && getModelId().equals(poModel.getModelId())) {
                return poModel;
            } else {
                if (ReferenceCache.tryLoad("Model", getModelId(), poModel)) {
                    return poModel;
                }

                poJSON = poModel.openRecord(getModelId());
                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Model", getModelId(), poModel);
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
    
    //end reference object models
}
