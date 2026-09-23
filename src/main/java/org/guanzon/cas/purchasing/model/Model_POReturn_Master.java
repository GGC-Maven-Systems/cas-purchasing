/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.model;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.cas.client.model.Model_Client_Address;
import org.guanzon.cas.client.model.Model_Client_Institution_Contact;
import org.guanzon.cas.client.model.Model_Client_Master;
import org.guanzon.cas.client.services.ClientModels;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Category;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.services.ParamModels;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderReturnStatus;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela 04-28-2025
 */
public class Model_POReturn_Master extends Model {
        
    //reference objects
    //All reference fields below are intentionally NOT constructed in initialize() - see their
    //accessors, which build them lazily on first access so opening this record never touches
    //those tables.
    Model_Branch poBranch;
    Model_Industry poIndustry;
    Model_Category poCategory;
    Model_Company poCompany;
    Model_Client_Master poSupplier;
    Model_Client_Address poSupplierAdress;
    Model_Client_Institution_Contact poSupplierContactPerson;
    Model_POR_Master poPurchaseOrderReceiving;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());
            
            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            
            //assign default values
            poEntity.updateObject("dTransact", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("dModified", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("nDiscount", 0.00);
            poEntity.updateObject("nAddDiscx", 0.0000);
            poEntity.updateObject("nTranTotl", 0.0000);
            poEntity.updateObject("nVATRatex", 0.00);
            poEntity.updateObject("nTWithHld", 0.00);
            poEntity.updateObject("nAmtPaidx", 0.0000);
            poEntity.updateObject("nFreightx", 0.00);
            poEntity.updateString("cPrintxxx", Logical.NO);
            poEntity.updateString("cProcessd", Logical.NO);
            poEntity.updateString("cTranStat", PurchaseOrderReturnStatus.OPEN);
            
            poEntity.updateObject("nVATSales", 0.0000);
            poEntity.updateObject("nVATAmtxx", 0.0000);
            poEntity.updateObject("nZroVATSl", 0.0000);
            poEntity.updateObject("nVATExmpt", 0.0000);
            
            //end - assign default values

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = "sTransNox";

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
    
    public JSONObject setBranchCode(String branchCode){
        return setValue("sBranchCd", branchCode);
    }
    
    public String getBranchCode(){
        return (String) getValue("sBranchCd");
    }
    
    public JSONObject setIndustryId(String industryID){
        return setValue("sIndstCdx", industryID);
    }
    
    public String getIndustryId(){
        return (String) getValue("sIndstCdx");
    }
    
    public JSONObject setTransactionDate(Date transactionDate){
        return setValue("dTransact", transactionDate);
    }
    
    public Date getTransactionDate(){
        return (Date) getValue("dTransact");
    }
    
    public JSONObject setCompanyId(String companyID){
        return setValue("sCompnyID", companyID);
    }
    
    public String getCompanyId(){
        return (String) getValue("sCompnyID");
    }
    
    public JSONObject setSupplierId(String supplierID){
        return setValue("sSupplier", supplierID);
    }
    
    public String getSupplierId(){
        return (String) getValue("sSupplier");
    }
    
    public JSONObject setAddressId(String addressID){
        return setValue("sAddrssID", addressID);
    }
    
    public String getAddressId(){
        return (String) getValue("sAddrssID");
    }
    
    public JSONObject setContactId(String contactID){
        return setValue("sContctID", contactID);
    }
    
    public String getContactId(){
        return (String) getValue("sContctID");
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
    
    public JSONObject setDiscount(Number discount){
        return setValue("nAddDiscx", discount);
    }
    
    public Number getDiscount(){
        if(getValue("nAddDiscx") == null || "".equals(getValue("nAddDiscx"))){
            return 0.0000;
        } 
        return (Number) getValue("nAddDiscx");
    }
    
    public JSONObject setTransactionTotal(Number transactionTotal){
        return setValue("nTranTotl", transactionTotal);
    }
    
    public Number getTransactionTotal(){
        if(getValue("nTranTotl") == null || "".equals(getValue("nTranTotl"))){
            return 0.0000;
        } 
        return (Number) getValue("nTranTotl");
    }
    
    public JSONObject setVatRate(Number vatRate){
        return setValue("nVATRatex", vatRate);
    }
    
    public Number getVatRate(){
        return (Number) getValue("nVATRatex");
    }
    
    public JSONObject setVatSales(Number vatSales) {
        return setValue("nVATSales", vatSales);
    }

    public Number getVatSales() {
        if(getValue("nVATSales") == null || "".equals(getValue("nVATSales"))){
            return 0.00;
        } 
        return (Number) getValue("nVATSales");
    }
    
    public JSONObject setVatAmount(Number vatAmount) {
        return setValue("nVATAmtxx", vatAmount);
    }

    public Number getVatAmount() {
        if(getValue("nVATAmtxx") == null || "".equals(getValue("nVATAmtxx"))){
            return 0.00;
        } 
        return (Number) getValue("nVATAmtxx");
    }
    
    public JSONObject setZeroVatSales(Number zeroVatSales) {
        return setValue("nZroVATSl", zeroVatSales);
    }

    public Number getZeroVatSales() {
        if(getValue("nZroVATSl") == null || "".equals(getValue("nZroVATSl"))){
            return 0.00;
        } 
        return (Number) getValue("nZroVATSl");
    }
    
    public JSONObject setVatExemptSales(Number vatExemptSales) {
        return setValue("nVATExmpt", vatExemptSales);
    }

    public Number getVatExemptSales() {
        if(getValue("nVATExmpt") == null || "".equals(getValue("nVATExmpt"))){
            return 0.00;
        } 
        return (Number) getValue("nVATExmpt");
    }
    
    public JSONObject setWithHoldingTax(Number withHoldingTax){
        return setValue("nTWithHld", withHoldingTax);
    }
    
    public Number getWithHoldingTax(){
        return (Number) getValue("nTWithHld");
    }
    
    public JSONObject setAmountPaid(Number amountPaid){
        return setValue("nAmtPaidx", amountPaid);
    }
    
    public Number getAmountPaid(){
        if(getValue("nAmtPaidx") == null || "".equals(getValue("nAmtPaidx"))){
            return 0.0000;
        } 
        return (Number) getValue("nAmtPaidx");
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
    
    public JSONObject setRemarks(String remarks){
        return setValue("sRemarksx", remarks);
    }
    
    public String getRemarks(){
        return (String) getValue("sRemarksx");
    }
    
    public JSONObject setSourceNo(String sourceNo){
        return setValue("sSourceNo", sourceNo);
    }
    
    public String getSourceNo(){
        return (String) getValue("sSourceNo");
    }
    
    public JSONObject setSourceCode(String sourceCode){
        return setValue("sSourceCd", sourceCode);
    }
    
    public String getSourceCode(){
        return (String) getValue("sSourceCd");
    }
    
    public JSONObject setPrint(String print){
        return setValue("cPrintxxx", print);
    }
    
    public String getPrint(){
        return (String) getValue("cPrintxxx");
    }
    
    public JSONObject setEntryNo(int entryNo){
        return setValue("nEntryNox", entryNo);
    }
    
    public int getEntryNo(){
        return (int) getValue("nEntryNox");
    }
    
    public JSONObject isProcessed(boolean isProcessed){
        return setValue("cProcessd", isProcessed ? "1" : "0");
    }
    
    public boolean isProcessed(){
        return ((String) getValue("cProcessd")).equals("1");
    }
    
    public JSONObject isWithHoldingTax(boolean isProcessed){
        return setValue("cTWithHld", isProcessed ? "1" : "0");
    }
    
    public boolean isWithHoldingTax(){
        return ((String) getValue("cTWithHld")).equals("1");
    }

    public JSONObject isVatTaxable(boolean isVatable){
        return setValue("cVATaxabl", isVatable ? "1" : "0");
    } 
    
    public boolean isVatTaxable(){
        return ((String) getValue("cVATaxabl")).equals("1");
    }
    
    public JSONObject setTransactionStatus(String transactionStatus){
        return setValue("cTranStat", transactionStatus);
    }
    
    public String getTransactionStatus(){
        return (String) getValue("cTranStat");
    }
    
    public JSONObject setCategoryCode(String categoryCode){
        return setValue("sCategrCd", categoryCode);
    }
    
    public String getCategoryCode(){
        return (String) getValue("sCategrCd");
    }
    
    public JSONObject setModifyingId(String modifyingId){
        return setValue("sModified", modifyingId);
    }
    
    public String getModifyingId(){
        return (String) getValue("sModified");
    }
    
    public JSONObject setModifiedDate(Date modifiedDate){
        return setValue("dModified", modifiedDate);
    }
    
    public Date getModifiedDate(){
        return (Date) getValue("dModified");
    }
    
    public Double getNetTotal(){
         //Net Total = Vat Amount - Tax Amount
        Double ldblNetTotal = 0.00;
        Double ldblTotal =  getTransactionTotal().doubleValue();
        Double ldblDiscount = getDiscount().doubleValue();
        Double ldblDiscountRate = getDiscountRate().doubleValue();
        Double ldblDiscountVatAmount = 0.0000;
        if(ldblDiscountRate > 0){
            ldblDiscountRate = ldblTotal * (ldblDiscountRate / 100);
        }
        ldblDiscount = ldblDiscount + ldblDiscountRate;
        if (isVatTaxable()) {
//            ldblDiscountVatAmount = ldblDiscount - (ldblDiscount / 1.12);
            ldblNetTotal = (getVatSales().doubleValue()
                        + getVatAmount().doubleValue()
                        + getVatExemptSales().doubleValue());
        } else {
//            ldblDiscountVatAmount = ldblDiscount * 0.12;
            ldblNetTotal = (ldblTotal + getVatAmount().doubleValue() + getFreight().doubleValue()) - ldblDiscount;
        }
        
        DecimalFormat format = new DecimalFormat("###0.0000");
        return Double.valueOf(format.format(ldblNetTotal));
        
    }
    
    @Override
    public String getNextCode() {
//        return "";
        return MiscUtil.getNextCode(this.getTable(), ID, true, poGRider.getGConnection().getConnection(), poGRider.getBranchCode());
    }
    
    //reference object models
    public Model_Branch Branch() throws SQLException, GuanzonException {
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

    public Model_Industry Industry() throws SQLException, GuanzonException {
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

    public Model_Category Category() throws SQLException, GuanzonException {
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

    public Model_Company Company() throws SQLException, GuanzonException {
        if (poCompany == null) {
            poCompany = new ParamModels(poGRider).Company();
        }

        String id = (String) (getValue("sCompnyID") == null ? "" : getValue("sCompnyID"));
        
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

    public Model_Client_Master Supplier() throws SQLException, GuanzonException {
        if (poSupplier == null) {
            poSupplier = new ClientModels(poGRider).ClientMaster();
        }
        
        String id = (String) (getValue("sSupplier") == null ? "" : getValue("sSupplier"));

        if (!"".equals(id)) {
            if (poSupplier.getEditMode() == EditMode.READY
                    && poSupplier.getClientId().equals(id)) {
                return poSupplier;
            } else {
                if (ReferenceCache.tryLoad("Client_Master", id, poSupplier)) {
                    return poSupplier;
                }

                poJSON = poSupplier.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Client_Master", id, poSupplier);
                    return poSupplier;
                } else {
                    poSupplier.initialize();
                    return poSupplier;
                }
            }
        } else {
            poSupplier.initialize();
            return poSupplier;
        }
    }

    public Model_Client_Address SupplierAddress() throws SQLException, GuanzonException {
        if (poSupplierAdress == null) {
            poSupplierAdress = new ClientModels(poGRider).ClientAddress();
        }
        
        String id = (String) (getValue("sAddressID") == null ? "" : getValue("sAddressID"));

        if (!"".equals(id)) {
            if (poSupplierAdress.getEditMode() == EditMode.READY
                    && poSupplierAdress.getClientId().equals(id)) {
                return poSupplierAdress;
            } else {
                if (ReferenceCache.tryLoad("Client_Address", id, poSupplierAdress)) {
                    return poSupplierAdress;
                }

                poJSON = poSupplierAdress.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Client_Address", id, poSupplierAdress);
                    return poSupplierAdress;
                } else {
                    poSupplierAdress.initialize();
                    return poSupplierAdress;
                }
            }
        } else {
            poSupplierAdress.initialize();
            return poSupplierAdress;
        }
    }

    public Model_Client_Institution_Contact SupplierContactPerson() throws SQLException, GuanzonException {
        if (poSupplierContactPerson == null) {
            poSupplierContactPerson = new ClientModels(poGRider).ClientInstitutionContact();
        }
        
        String id = (String) (getValue("sContctID") == null ? "" : getValue("sContctID"));

        if (!"".equals(id)) {
            if (poSupplierContactPerson.getEditMode() == EditMode.READY
                    && poSupplierContactPerson.getClientId().equals(id)) {
                return poSupplierContactPerson;
            } else {
                if (ReferenceCache.tryLoad("Client_Institution_Contact_Person", id, poSupplierContactPerson)) {
                    return poSupplierContactPerson;
                }

                poJSON = poSupplierContactPerson.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Client_Institution_Contact_Person", id, poSupplierContactPerson);
                    return poSupplierContactPerson;
                } else {
                    poSupplierContactPerson.initialize();
                    return poSupplierContactPerson;
                }
            }
        } else {
            poSupplierContactPerson.initialize();
            return poSupplierContactPerson;
        }
    }

    public Model_POR_Master PurchaseOrderReceivingMaster() throws SQLException, GuanzonException {
        if (poPurchaseOrderReceiving == null) {
            poPurchaseOrderReceiving = new PurchaseOrderReceivingModels(poGRider).PurchaseOrderReceivingMaster();
        }

        String id = (String) (getValue("sSourceNo") == null ? "" : getValue("sSourceNo"));
        
        if (!"".equals(id)) {
            if (poPurchaseOrderReceiving.getEditMode() == EditMode.READY
                    && poPurchaseOrderReceiving.getTransactionNo().equals(id)) {
                return poPurchaseOrderReceiving;
            } else {
                poJSON = poPurchaseOrderReceiving.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    return poPurchaseOrderReceiving;
                } else {
                    poPurchaseOrderReceiving.initialize();
                    return poPurchaseOrderReceiving;
                }
            }
        } else {
            poPurchaseOrderReceiving.initialize();
            return poPurchaseOrderReceiving;
        }
    }
    //end - reference object models
}
