package org.guanzon.cas.purchasing.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.cas.client.account.AP_Client_Master;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.inv.InvTransCons;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.Brand;
import org.guanzon.cas.parameter.CategoryLevel2;
import org.guanzon.cas.parameter.Company;
import org.guanzon.cas.parameter.Department;
import org.guanzon.cas.parameter.ModelVariant;
import org.guanzon.cas.parameter.Term;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.purchasing.model.Model_PO_Quotation_Request_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Quotation_Request_Master;
import org.guanzon.cas.purchasing.services.QuotationModels;
import org.json.simple.JSONObject;

public class POQuotationRequestx extends Transaction {
    JSONObject poJSON;

    public JSONObject InitTransaction() {
         SOURCE_CODE = InvTransCons.PURCHASE_ORDER_QUOTATION_REQUEST;

        poMaster = new QuotationModels(poGRider).POQuotationRequestMaster();
        poDetail = new QuotationModels(poGRider).POQuotationRequestDetails();
        paDetail = new ArrayList<>();

        return initialize();
    }

    public JSONObject NewTransaction() {
        return null;
    }

    public JSONObject OpenTransaction() {
        return null;
    }

    public JSONObject UpdateTransaction() {
        return null;
    }

    public JSONObject SaveTransaction() {
        return null;
    }

    public JSONObject ConfirmTransaction(String remarks) {
        return null;
    }

    public JSONObject ApproveTransaction(String remarks) {
        return null;
    }

    public JSONObject PostTransaction(String remarks) {
        return null;
    }

    public JSONObject VoidTransaction(String remarks) {
        return null;
    }

    public JSONObject CancelTransaction(String remarks) {
        return null;
    }

    public JSONObject DisapproveTransaction(String remarks) {
        return null;
    }

    public JSONObject ReturnTransaction(String remarks) {
        return null;
    }

    public JSONObject SearchTransaction() {
        return null;
    }

    public JSONObject SearchTransaction(String branch, String department, String category, String tranDate,String transNo) {
        return null;
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
     
    public JSONObject SearchBranch(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            Master().setBranchCode(object.getModel().getBranchCode());
        }

        return poJSON;
    }
    
    public JSONObject SearchDepartment(String value, boolean byCode, boolean isSearch) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Department object = new ParamControllers(poGRider, logwrapr).Department();
        object.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setDepartmentId(object.getModel().getDepartmentId());
        }

        return poJSON;
    }
    
    public JSONObject SearchCategory(String value, boolean byCode) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        
        CategoryLevel2 object = new ParamControllers(poGRider, logwrapr).CategoryLevel2();

        String lsSQL = MiscUtil.addCondition(object.getSQ_Browse(), 
                                                "cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE) +
                                                    " AND (sIndstCdx = "+SQLUtil.toSQL(Master().getIndustryId())+" OR ISNULL(sIndstCdx))");
        
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
                    Master().setCategoryLevel2(object.getModel().getCategoryId());
            }
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
        }
        
        return poJSON;
    }
    
    public JSONObject SearchBrand(String value, boolean byCode, int row) throws ExceptionInInitializerError, SQLException, GuanzonException {
        poJSON = new JSONObject();
        
        if(Master().getCategoryLevel2()== null || "".equals(Master().getCategoryLevel2())){
            poJSON.put("result", "error");
            poJSON.put("message", "Category is not set.");
            return poJSON;
        }
        
        Brand object = new ParamControllers(poGRider, logwrapr).Brand();
        object.setRecordStatus(RecordStatus.ACTIVE);

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

    @Override
    public Model_PO_Quotation_Request_Master Master() {
        return (Model_PO_Quotation_Request_Master) poMaster;
    }

    @Override
    public Model_PO_Quotation_Request_Detail Detail(int row) {
        return (Model_PO_Quotation_Request_Detail) paDetail.get(row);
    }

    public List<Model> Detail() {
        return paDetail;
    }
    
    @Override
    public int getDetailCount() {
        return paDetail.size();
    }
    
}