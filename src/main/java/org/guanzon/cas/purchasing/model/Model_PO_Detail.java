package org.guanzon.cas.purchasing.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.inv.model.Model_Inv_Master;
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.services.InvModels;
import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Detail;
import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Master;
import org.guanzon.cas.inv.warehouse.services.InvWarehouseModels;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Brand;
import org.guanzon.cas.parameter.model.Model_Category;
import org.guanzon.cas.parameter.model.Model_Color;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.model.Model_Inv_Type;
import org.guanzon.cas.parameter.model.Model_Measure;
import org.guanzon.cas.parameter.model.Model_Model;
import org.guanzon.cas.parameter.model.Model_Term;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import org.guanzon.cas.purchasing.services.QuotationModels;

public class Model_PO_Detail extends Model {

    //reference objects
    //All reference fields below are intentionally NOT constructed in initialize() - see their
    //accessors, which build them lazily on first access so opening this record never touches
    //those tables.
    Model_Branch poBranch;
    Model_Industry poIndustry;
    Model_Company poCompany;
    Model_Term poTerm;
    Model_Brand poBrand;
    Model_Model poModel;
    Model_Color poColor;
    Model_Category poCategory;
    Model_Inv_Type poInv_Type;
    Model_Measure poMeasure;
    Model_Inv_Stock_Request_Master poInvStockMaster;
    Model_Inv_Stock_Request_Detail poInvStockDetail;
    Model_Inventory poInventory;
    Model_Inv_Master poInventoryMaster;
    Model_PO_Quotation_Master poPOQuotationMaster;
    Model_PO_Quotation_Detail poPOQuotationDetail;
    private String lsBrand ;
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            //assign default values
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("nCancelld", 0.00);
            poEntity.updateObject("nUnitPrce", 0.0000);
            poEntity.updateObject("nOldPrice", 0.0000);
            poEntity.updateObject("nQtyOnHnd", 0.00);
            poEntity.updateObject("nRecOrder", 0.00);
            poEntity.updateObject("nQuantity", 0.00);
            poEntity.updateObject("nReceived", 0.00);
            poEntity.updateObject("dModified", SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));

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

    private static String xsDateShort(Date fdValue) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }

    @Override
    public String getNextCode() {
        return "";
    }

    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    public JSONObject setEntryNo(Number entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public Number getEntryNo() {
        return (Number) getValue("nEntryNox");
    }

    public JSONObject setStockID(String stockID) {
        return setValue("sStockIDx", stockID);
    }

    public String getStockID() {
        return (String) getValue("sStockIDx");
    }

    public JSONObject setDescription(String description) {
        return setValue("sDescript", description);
    }

    public String getDescription() {
        return (String) getValue("sDescript");
    }

    public JSONObject setOldPrice(Number oldPrice) {
        return setValue("nOldPrice", oldPrice);
    }

    public Number getOldPrice() {
        return (Number) getValue("nOldPrice");
    }

    public JSONObject setUnitPrice(Number unitPrice) {
        return setValue("nUnitPrce", unitPrice);
    }

    public Number getUnitPrice() {
        return (Number) getValue("nUnitPrce");
    }

    public JSONObject setQuantityOnHand(Number quantityOnHand) {
        return setValue("nQtyOnHnd", quantityOnHand);
    }

    public Number getQuantityOnHand() {
        return (Number) getValue("nQtyOnHnd");
    }

    public JSONObject setRecordOrder(Number recordOrder) {
        return setValue("nRecOrder", recordOrder);
    }

    public Number getRecordOrder() {
        return (Number) getValue("nRecOrder");
    }

    public JSONObject setQuantity(Number quantity) {
        return setValue("nQuantity", quantity);
    }

    public Number getQuantity() {
        return (Number) getValue("nQuantity");
    }

    public JSONObject setReceivedQuantity(Number receivedQuantity) {
        return setValue("nReceived", receivedQuantity);
    }

    public Number getReceivedQuantity() {
        return (Number) getValue("nReceived");
    }

    public JSONObject setCancelledQuantity(Number cancelledQuantity) {
        return setValue("nCancelld", cancelledQuantity);
    }

    public Number getCancelledQuantity() {
        return (Number) getValue("nCancelld");
    }

    public JSONObject setSouceCode(String sourceCode) {
        return setValue("sSourceCd", sourceCode);
    }

    public String getSouceCode() {
        return (String) getValue("sSourceCd");
    }

    public JSONObject setSouceNo(String sourceNo) {
        return setValue("sSourceNo", sourceNo);
    }

    public String getSouceNo() {
        return (String) getValue("sSourceNo");
    }

    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

//    public JSONObject setBrandId(String brandId) {
//        return poBrand.setBrandId(brandId);
//    }
//
//    public String getBrandId() {
//        return poBrand.getBrandId();
//    }


    public String setBrandId(String brandID) {
        return lsBrand = brandID;
    }

    public String getBrandId() {
        return lsBrand;
    }

    public JSONObject setSourceEntryNo(Number entryNo) {
        return setValue("nSrEtryNo", entryNo);
    }

    public Number getSourceEntryNo() {
        return (Number) getValue("nSrEtryNo");
    }

    //reference object models
    public Model_Branch Branch() throws GuanzonException, SQLException {
        if (poBranch == null) {
            poBranch = new ParamModels(poGRider).Branch();
        }
        
        String id = (String) (getValue("sBranchCd") == null ? "" : getValue("sBranchCd"));

        if (!"".equals(id)) {
            if (poBranch.getEditMode() == EditMode.READY
                    && poBranch.getBranchCode().equals(id)) {
                return poBranch;
            } else {
                if (ReferenceCache.tryLoad("Branch", id, poBranch)) {
                    return poBranch;
                }

                poJSON = poBranch.openRecord(id);
                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Branch", id, poBranch);
                    return poBranch;
                } else {
                    poBranch.initialize();
                    return poBranch;
                }
            }
        } else {
            poBranch.initialize();
            return poBranch;
        }
    }

    public Model_Industry Industry() throws GuanzonException, SQLException {
        if (poIndustry == null) {
            poIndustry = new ParamModels(poGRider).Industry();
        }
        
        String id = (String) (getValue("sIndstCdx") == null ? "" : getValue("sIndstCdx"));

        if (!"".equals(id)) {
            if (poIndustry.getEditMode() == EditMode.READY
                    && poIndustry.getIndustryId().equals(id)) {
                return poIndustry;
            } else {
                if (ReferenceCache.tryLoad("Industry", id, poIndustry)) {
                    return poIndustry;
                }

                poJSON = poIndustry.openRecord(id);
                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Industry", id, poIndustry);
                    return poIndustry;
                } else {
                    poIndustry.initialize();
                    return poIndustry;
                }
            }
        } else {
            poIndustry.initialize();
            return poIndustry;
        }
    }

    public Model_Category Category() throws GuanzonException, SQLException {
        if (poCategory == null) {
            poCategory = new ParamModels(poGRider).Category();
        }
        
        String id = (String) (getValue("sCategrCd") == null ? "" : getValue("sCategrCd"));

        if (!"".equals(id)) {
            if (poCategory.getEditMode() == EditMode.READY
                    && poCategory.getCategoryId().equals(id)) {
                return poCategory;
            } else {
                if (ReferenceCache.tryLoad("Category", id, poCategory)) {
                    return poCategory;
                }

                poJSON = poCategory.openRecord(id);
                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Category", id, poCategory);
                    return poCategory;
                } else {
                    poCategory.initialize();
                    return poCategory;
                }
            }
        } else {
            poCategory.initialize();
            return poCategory;
        }
    }

    public Model_Company Company() throws GuanzonException, SQLException {
        if (poCompany == null) {
            poCompany = new ParamModels(poGRider).Company();
        }
        
        String id = (String) (getValue("sCompanyID") == null ? "" : getValue("sCompanyID"));

        if (!"".equals(id)) {
            if (poCompany.getEditMode() == EditMode.READY
                    && poCompany.getCompanyId().equals(id)) {
                return poCompany;
            } else {
                if (ReferenceCache.tryLoad("Company", id, poCompany)) {
                    return poCompany;
                }

                poJSON = poCompany.openRecord(id);
                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Company", id, poCompany);
                    return poCompany;
                } else {
                    poCompany.initialize();
                    return poCompany;
                }
            }
        } else {
            poCompany.initialize();
            return poCompany;
        }
    }

    public Model_Term Term() throws GuanzonException, SQLException {
        if (poTerm == null) {
            poTerm = new ParamModels(poGRider).Term();
        }
        
        String id = (String) (getValue("sTermCode") == null ? "" : getValue("sTermCode"));

        if (!"".equals(id)) {
            if (poTerm.getEditMode() == EditMode.READY
                    && poTerm.getTermId().equals(id)) {
                return poTerm;
            } else {
                if (ReferenceCache.tryLoad("Term", id, poTerm)) {
                    return poTerm;
                }

                poJSON = poTerm.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Term", id, poTerm);
                    return poTerm;
                } else {
                    poTerm.initialize();
                    return poTerm;
                }
            }
        } else {
            poTerm.initialize();
            return poTerm;
        }
    }

    public Model_Inventory Inventory() throws GuanzonException, SQLException {
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

    public Model_Inv_Master InventoryMaster() throws GuanzonException, SQLException {
        if (poInventoryMaster == null) {
            poInventoryMaster = new InvModels(poGRider).InventoryMaster();
        }
        
        String id = (String) (getValue("sStockIDx") == null ? "" : getValue("sStockIDx"));

        if (!"".equals(id)) {
            if (poInventoryMaster.getEditMode() == EditMode.READY
                    && poInventoryMaster.getStockId().equals(id)) {
                return poInventoryMaster;
            } else {
                poJSON = poInventoryMaster.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    return poInventoryMaster;
                } else {
                    poInventoryMaster.initialize();
                    return poInventoryMaster;
                }
            }
        } else {
            poInventoryMaster.initialize();
            return poInventoryMaster;
        }
    }

    public Model_Brand Brand() throws SQLException, GuanzonException {
        if (poBrand == null) {
            poBrand = new ParamModels(poGRider).Brand();
        }

        if (!"".equals(lsBrand)) {
            if (this.poBrand.getEditMode() == 1 && this.poBrand
                    .getBrandId().equals(lsBrand)) {
                return this.poBrand;
            }

            if (ReferenceCache.tryLoad("Brand", lsBrand, poBrand)) {
                return poBrand;
            }

            this.poJSON = this.poBrand.openRecord(this.getBrandId());
            if ("success".equals(this.poJSON.get("result"))) {
                ReferenceCache.store("Brand", lsBrand, poBrand);
                return this.poBrand;
            }
            this.poBrand.initialize();
            return this.poBrand;
        }
        this.poBrand.initialize();
        return this.poBrand;
    }

//    public Model_Brand Brand() throws GuanzonException, SQLException {
//        System.out.println("brandID" + getBrandId());
//        if (!"".equals(getBrandId())) {
////            if (poBrand.getEditMode() == EditMode.READY
////                    && poBrand.getBrandId().equals(getBrandId())) {
////                return poBrand;
////            } else {
//            poJSON = poBrand.openRecord(getBrandId());
//            if ("success".equals((String) poJSON.get("result"))) {
//                return poBrand;
//            } else {
//                poBrand.initialize();
//                return poBrand;
//            }
////            }
//        } else {
//            poBrand.initialize();
//            return poBrand;
//        }
//    }

//    public Model_Brand Brand() throws GuanzonException, SQLException {
//        if (!"".equals((String) getValue("sBrandIDx"))) {
//            if (poBrand.getEditMode() == EditMode.READY
//                    && poBrand.getBrandId().equals((String) getValue("sBrandIDx"))) {
//                return poBrand;
//            } else {
//                poJSON = poBrand.openRecord((String) getValue("sBrandIDx"));
//
//                if ("success".equals((String) poJSON.get("result"))) {
//                    return poBrand;
//                } else {
//                    poBrand.initialize();
//                    return poBrand;
//                }
//            }
//        } else {
//            poBrand.initialize();
//            return poBrand;
//        }
//    }
    public Model_Color Color() throws GuanzonException, SQLException {
        if (poColor == null) {
            poColor = new ParamModels(poGRider).Color();
        }

        String id = (String) (getValue("sColorIDx") == null ? "" : getValue("sColorIDx"));
        
        if (!"".equals(id)) {
            if (poColor.getEditMode() == EditMode.READY
                    && poColor.getColorId().equals(id)) {
                return poColor;
            } else {
                if (ReferenceCache.tryLoad("Color", id, poColor)) {
                    return poColor;
                }

                poJSON = poColor.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Color", id, poColor);
                    return poColor;
                } else {
                    poColor.initialize();
                    return poColor;
                }
            }
        } else {
            poColor.initialize();
            return poColor;
        }
    }

    public Model_Inv_Type InventoryType() throws GuanzonException, SQLException {
        if (poInv_Type == null) {
            poInv_Type = new ParamModels(poGRider).InventoryType();
        }
        
        String id = (String) (getValue("sInvTypCd") == null ? "" : getValue("sInvTypCd"));

        if (!"".equals(id)) {
            if (poInv_Type.getEditMode() == EditMode.READY
                    && poInv_Type.getInventoryTypeId().equals(id)) {
                return poInv_Type;
            } else {
                if (ReferenceCache.tryLoad("Inv_Type", id, poInv_Type)) {
                    return poInv_Type;
                }

                poJSON = poInv_Type.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Inv_Type", id, poInv_Type);
                    return poInv_Type;
                } else {
                    poInv_Type.initialize();
                    return poInv_Type;
                }
            }
        } else {
            poInv_Type.initialize();
            return poInv_Type;
        }
    }

    public Model_Measure Measure() throws GuanzonException, SQLException {
        if (poMeasure == null) {
            poMeasure = new ParamModels(poGRider).Measurement();
        }
        
        String id = (String) (getValue("sMeasurID") == null ? "" : getValue("sMeasurID"));

        if (!"".equals(id)) {
            if (poMeasure.getEditMode() == EditMode.READY
                    && poMeasure.getMeasureId().equals(id)) {
                return poMeasure;
            } else {
                if (ReferenceCache.tryLoad("Measure", id, poMeasure)) {
                    return poMeasure;
                }

                poJSON = poMeasure.openRecord(id);
                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Measure", id, poMeasure);
                    return poMeasure;
                } else {
                    poMeasure.initialize();
                    return poMeasure;
                }
            }
        } else {
            poMeasure.initialize();
            return poMeasure;
        }
    }

    public Model_Model Model() throws GuanzonException, SQLException {
        if (poModel == null) {
            poModel = new ParamModels(poGRider).Model();
        }
        
        String id = (String) (getValue("sModelIDx") == null ? "" : getValue("sModelIDx"));

        if (!"".equals(id)) {
            if (poModel.getEditMode() == EditMode.READY
                    && poModel.getModelId().equals(id)) {
                return poModel;
            } else {
                if (ReferenceCache.tryLoad("Model", id, poModel)) {
                    return poModel;
                }

                poJSON = poModel.openRecord(id);
                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Model", id, poModel);
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

    public Model_Inv_Stock_Request_Master InvStockRequestMaster() throws GuanzonException, SQLException {
        if (poInvStockMaster == null) {
            poInvStockMaster = new InvWarehouseModels(poGRider).InventoryStockRequestMaster();
        }
        
        String id = (String) (getValue("sTransNox") == null ? "" : getValue("sTransNox"));

        if (!"".equals(id)) {
            if (poInvStockMaster.getEditMode() == EditMode.READY
                    && poInvStockMaster.getTransactionNo().equals(id)) {
                return poInvStockMaster;
            } else {
                poJSON = poInvStockMaster.openRecord(id);
                if ("success".equals((String) poJSON.get("result"))) {
                    return poInvStockMaster;
                } else {
                    poInvStockMaster.initialize();
                    return poInvStockMaster;
                }
            }
        } else {
            poInvStockMaster.initialize();
            return poInvStockMaster;
        }
    }

    public Model_Inv_Stock_Request_Detail InvStockRequestDetail() throws GuanzonException, SQLException {
        if (poInvStockDetail == null) {
            poInvStockDetail = new InvWarehouseModels(poGRider).InventoryStockRequestDetail();
        }

        String id = (String) (getValue("sSourceNo") == null ? "" : getValue("sSourceNo"));
        
        if (!"".equals(id)) {
            if (poInvStockDetail.getEditMode() == EditMode.READY
                    && poInvStockDetail.getTransactionNo().equals(id)) {
                return poInvStockDetail;
            } else {
                poJSON = poInvStockDetail.openRecordByReference(id, getValue("sStockIDx"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poInvStockDetail;
                } else {
                    poInvStockMaster.initialize();
                    return poInvStockDetail;
                }
            }
        } else {
            poInvStockDetail.initialize();
            return poInvStockDetail;
        }
    }

    public Model_PO_Quotation_Master POQuotationMaster() throws GuanzonException, SQLException {
        if (poPOQuotationMaster == null) {
            poPOQuotationMaster = new QuotationModels(poGRider).POQuotationMaster();
        }
        
        String id = (String) (getValue("sSourceNo") == null ? "" : getValue("sSourceNo"));

        if (!"".equals(id)) {
            if (poPOQuotationMaster.getEditMode() == EditMode.READY
                    && poPOQuotationMaster.getTransactionNo().equals(id)) {
                return poPOQuotationMaster;
            } else {
                poJSON = poPOQuotationMaster.openRecord(id);
                if ("success".equals((String) poJSON.get("result"))) {
                    return poPOQuotationMaster;
                } else {
                    poPOQuotationMaster.initialize();
                    return poPOQuotationMaster;
                }
            }
        } else {
            poPOQuotationMaster.initialize();
            return poPOQuotationMaster;
        }
    }

    public Model_PO_Quotation_Detail POQuotationDetail() throws GuanzonException, SQLException {
        if (poPOQuotationDetail == null) {
            poPOQuotationDetail = new QuotationModels(poGRider).POQuotationDetails();
        }
        
        String id = (String) (getValue("sSourceNo") == null ? "" : getValue("sSourceNo"));

        if (!"".equals(id)) {
            if (poPOQuotationDetail.getEditMode() == EditMode.READY
                    && poPOQuotationDetail.getTransactionNo().equals(id)) {
                return poPOQuotationDetail;
            } else {
                poJSON = poPOQuotationDetail.openRecord(id, (String) getValue("sStockIDx"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poPOQuotationDetail;
                } else {
                    poPOQuotationDetail.initialize();
                    return poPOQuotationDetail;
                }
            }
        } else {
            poPOQuotationDetail.initialize();
            return poPOQuotationDetail;
        }
    }

    public JSONObject openRecordByReference(String Id1, Object Id2) throws SQLException, GuanzonException {
        poJSON = new JSONObject();

        String lsSQL = MiscUtil.makeSelect(this);

        //replace the condition based on the primary key column of the record
        lsSQL = MiscUtil.addCondition(lsSQL, "sTransNox = " + SQLUtil.toSQL(Id1)
                + " AND sStockIDx = " + SQLUtil.toSQL(Id2));

        ResultSet loRS = poGRider.executeQuery(lsSQL);

        try {
            if (loRS.next()) {
                for (int lnCtr = 1; lnCtr <= loRS.getMetaData().getColumnCount(); lnCtr++) {
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
//            logError(getCurrentMethodName() + "»" + e.getMessage());
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }

        return poJSON;
    }
}
