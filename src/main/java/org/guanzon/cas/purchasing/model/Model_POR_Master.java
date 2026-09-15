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
//import org.guanzon.cas.client.services.ClientModel;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Category;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Department;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.model.Model_Term;
import org.guanzon.cas.parameter.services.ParamModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderReceivingStatus;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela 03-12-2025
 */
public class Model_POR_Master extends Model {

    //reference objects
    //poBranch/poIndustry/poCategory/poCompany/poDepartment/poTerm/poSupplier/poSupplierAdress/
    //poSupplierContactPerson are intentionally NOT constructed in initialize() - see their
    //accessors below, which build them lazily on first access so opening this record never
    //touches those tables.
    Model_Branch poBranch;
    Model_Industry poIndustry;
    Model_Category poCategory;
    Model_Company poCompany;
    Model_Department poDepartment;
    Model_Term poTerm;
    Model_Client_Master poSupplier;
    Model_Client_Address poSupplierAdress;
    Model_Client_Institution_Contact poSupplierContactPerson;
    private boolean isSummarized;
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            //assign default values
            poEntity.updateObject("dTransact", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("dRefernce", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("dSalesInv", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("dTermDuex", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("dDueDatex", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("dModified", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("nDiscount", 0.00);
            poEntity.updateObject("nAddDiscx", 0.0000);
            poEntity.updateObject("nTranTotl", 0.0000);
            poEntity.updateObject("nVATRatex", 0.00);
            poEntity.updateObject("nTWithHld", 0.0000);
            poEntity.updateObject("nAmtPaidx", 0.0000);
            poEntity.updateObject("nFreightx", 0.00);
            poEntity.updateObject("nVATSales", 0.0000);
            poEntity.updateObject("nVATAmtxx", 0.0000);
            poEntity.updateObject("nZroVATSl", 0.0000);
            poEntity.updateObject("nVATExmpt", 0.0000);
            poEntity.updateString("cPrintxxx", Logical.NO);
            poEntity.updateString("cProcessd", Logical.NO);
            poEntity.updateString("cTranStat", PurchaseOrderReceivingStatus.OPEN);
            poEntity.updateString("cPurposex", PurchaseOrderReceivingStatus.Purpose.REGULAR);
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

    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    public JSONObject setBranchCode(String branchCode) {
        return setValue("sBranchCd", branchCode);
    }

    public String getBranchCode() {
        return (String) getValue("sBranchCd");
    }

    public JSONObject setIndustryId(String industryID) {
        return setValue("sIndstCdx", industryID);
    }

    public String getIndustryId() {
        return (String) getValue("sIndstCdx");
    }

    public JSONObject setDepartmentId(String departmentID) {
        return setValue("sDeptIDxx", departmentID);
    }

    public String getDepartmentId() {
        return (String) getValue("sDeptIDxx");
    }

    public JSONObject setTransactionDate(Date transactionDate) {
        return setValue("dTransact", transactionDate);
    }

    public Date getTransactionDate() {
        return (Date) getValue("dTransact");
    }

    public JSONObject setCompanyId(String companyID) {
        return setValue("sCompnyID", companyID);
    }

    public String getCompanyId() {
        return (String) getValue("sCompnyID");
    }

    public JSONObject setSupplierId(String supplierID) {
        return setValue("sSupplier", supplierID);
    }

    public String getSupplierId() {
        return (String) getValue("sSupplier");
    }

    public JSONObject setAddressId(String addressID) {
        return setValue("sAddrssID", addressID);
    }

    public String getAddressId() {
        return (String) getValue("sAddrssID");
    }

    public JSONObject setContactId(String contactID) {
        return setValue("sContctID", contactID);
    }

    public String getContactId() {
        return (String) getValue("sContctID");
    }

    public JSONObject setTruckingId(String truckingID) {
        return setValue("sTrucking", truckingID);
    }

    public String getTruckingId() {
        return (String) getValue("sTrucking");
    }

    public JSONObject setReferenceNo(String referenceNo) {
        return setValue("sReferNox", referenceNo);
    }

    public String getReferenceNo() {
        return (String) getValue("sReferNox");
    }

    public JSONObject setReferenceDate(Date refernceDate) {
        return setValue("dRefernce", refernceDate);
    }

    public Date getReferenceDate() {
        return (Date) getValue("dRefernce");
    }

    public JSONObject setSalesInvoiceDate(Date salesInvoiceDate) {
        return setValue("dSalesInv", salesInvoiceDate);
    }

    public Date getSalesInvoiceDate() {
        return (Date) getValue("dSalesInv");
    }

    public JSONObject setTermCode(String termCode) {
        return setValue("sTermCode", termCode);
    }

    public String getTermCode() {
        return (String) getValue("sTermCode");
    }

    public JSONObject setTermDueDate(Date termDueDate) {
        return setValue("dTermDuex", termDueDate);
    }

    public Date getTermDueDate() {
        return (Date) getValue("dTermDuex");
    }

    public JSONObject setDueDate(Date dueDate) {
        return setValue("dDueDatex", dueDate);
    }

    public Date getDueDate() {
        return (Date) getValue("dDueDatex");
    }

    public JSONObject setDiscountRate(Number discountRate) {
        return setValue("nDiscount", discountRate);
    }

    public Number getDiscountRate() {
        if(getValue("nDiscount") == null || "".equals(getValue("nDiscount"))){
            return 0.00;
        } 
        return (Number) getValue("nDiscount");
    }

    public JSONObject setDiscount(Number discount) {
        return setValue("nAddDiscx", discount);
    }

    public Number getDiscount() {
        if(getValue("nAddDiscx") == null || "".equals(getValue("nAddDiscx"))){
            return 0.0000;
        } 
        return (Number) getValue("nAddDiscx");
    }

    public JSONObject setTransactionTotal(Number transactionTotal) {
        return setValue("nTranTotl", transactionTotal);
    }

    public Number getTransactionTotal() {
        if(getValue("nTranTotl") == null || "".equals(getValue("nTranTotl"))){
            return 0.0000;
        } 
        return (Number) getValue("nTranTotl");
    }

    public JSONObject setSalesInvoice(String salesInvoice) {
        return setValue("sSalesInv", salesInvoice);
    }

    public String getSalesInvoice() {
        return (String) getValue("sSalesInv");
    }

    public JSONObject isVatTaxable(boolean isVatable){
        return setValue("cVATaxabl", isVatable ? "1" : "0");
    } 
    
    public boolean isVatTaxable(){
        return ((String) getValue("cVATaxabl")).equals("1");
    }

    public JSONObject setVatRate(Number vatRate) {
        return setValue("nVATRatex", vatRate);
    }

    public Number getVatRate() {
        if(getValue("nVATRatex") == null || "".equals(getValue("nVATRatex"))){
            return 0.00;
        } 
        return (Number) getValue("nVATRatex");
    }

    public JSONObject isTaxWithHold(boolean taxWithHold){
        return setValue("cTWithHld", taxWithHold ? "1" : "0");
    } 
    
    public boolean isTaxWithHold(){
        return ((String) getValue("cTWithHld")).equals("1");
    }

    public JSONObject setWithHoldingTax(Number withHoldingTax) {
        return setValue("nTWithHld", withHoldingTax);
    }

    public Number getWithHoldingTax() {
        if(getValue("nTWithHld") == null || "".equals(getValue("nTWithHld"))){
            return 0.0000;
        } 
        return (Number) getValue("nTWithHld");
    }

    public JSONObject setAmountPaid(Number amountPaid) {
        return setValue("nAmtPaidx", amountPaid);
    }

    public Number getAmountPaid() {
        if(getValue("nAmtPaidx") == null || "".equals(getValue("nAmtPaidx"))){
            return 0.0000;
        } 
        return (Number) getValue("nAmtPaidx");
    }

    public JSONObject setFreight(Number freight) {
        return setValue("nFreightx", freight);
    }

    public Number getFreight() {
        if(getValue("nFreightx") == null || "".equals(getValue("nFreightx"))){
            return 0.00;
        } 
        return (Number) getValue("nFreightx");
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

    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }

    public JSONObject setPurpose(String purpose) {
        return setValue("cPurposex", purpose);
    }

    public String getPurpose() {
        return (String) getValue("cPurposex");
    }

    public JSONObject setPrint(String print) {
        return setValue("cPrintxxx", print);
    }

    public String getPrint() {
        return (String) getValue("cPrintxxx");
    }

    public JSONObject setEntryNo(int entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public int getEntryNo() {
        return (int) getValue("nEntryNox");
    }

    public JSONObject setInventoryTypeCode(String inventoryTypeCode) {
        return setValue("sInvTypCd", inventoryTypeCode);
    }

    public String getInventoryTypeCode() {
        return (String) getValue("sInvTypCd");
    }

    public JSONObject isProcessed(boolean isProcessed) {
        return setValue("cProcessd", isProcessed ? "1" : "0");
    }

    public boolean isProcessed() {
        return ((String) getValue("cProcessd")).equals("1");
    }

    public JSONObject setTransactionStatus(String transactionStatus) {
        return setValue("cTranStat", transactionStatus);
    }

    public String getTransactionStatus() {
        return (String) getValue("cTranStat");
    }

    public JSONObject setCategoryCode(String categoryCode) {
        return setValue("sCategrCd", categoryCode);
    }

    public String getCategoryCode() {
        return (String) getValue("sCategrCd");
    }

    public JSONObject setModifyingId(String modifyingId) {
        return setValue("sModified", modifyingId);
    }

    public String getModifyingId() {
        return (String) getValue("sModified");
    }

    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
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
        
        
//         //Net Total = Vat Amount - Tax Amount
//        Double ldblNetTotal = 0.00;
//        Double ldblTotal =  getTransactionTotal().doubleValue();
//        Double ldblDiscount = getDiscount().doubleValue();
//        Double ldblDiscountRate = getDiscountRate().doubleValue();
//        if(ldblDiscountRate > 0){
//            ldblDiscountRate = ldblTotal * (ldblDiscountRate / 100);
//        }
//        ldblDiscount = ldblDiscount + ldblDiscountRate;
//        if (isVatTaxable()) {
//            ldblNetTotal = getVatSales().doubleValue()
//                        + getVatAmount().doubleValue()
//                        + getVatExemptSales().doubleValue();
//        } else {
//            ldblNetTotal = ldblTotal + getVatAmount().doubleValue();
//        }
//        
//        ldblNetTotal = (ldblNetTotal - ldblDiscount) + getFreight().doubleValue();
//        
//        return ldblNetTotal;
    }
    public void setSummarized(boolean summarized) {
        this.isSummarized = summarized;
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

        if (!"".equals((String) getValue("sBranchCd"))) {
            if (poBranch.getEditMode() == EditMode.READY
                    && poBranch.getBranchCode().equals((String) getValue("sBranchCd"))) {
                return poBranch;
            } else {
                if (ReferenceCache.tryLoad("Branch", (String) getValue("sBranchCd"), poBranch)) {
                    return poBranch;
                }

                poJSON = poBranch.openRecord((String) getValue("sBranchCd"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Branch", (String) getValue("sBranchCd"), poBranch);
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

        if (!"".equals((String) getValue("sIndstCdx"))) {
            if (poIndustry.getEditMode() == EditMode.READY
                    && poIndustry.getIndustryId().equals((String) getValue("sIndstCdx"))) {
                return poIndustry;
            } else {
                if (ReferenceCache.tryLoad("Industry", (String) getValue("sIndstCdx"), poIndustry)) {
                    return poIndustry;
                }

                poJSON = poIndustry.openRecord((String) getValue("sIndstCdx"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Industry", (String) getValue("sIndstCdx"), poIndustry);
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

        if (!"".equals((String) getValue("sCategrCd"))) {
            if (poCategory.getEditMode() == EditMode.READY
                    && poCategory.getCategoryId().equals((String) getValue("sCategrCd"))) {
                return poCategory;
            } else {
                if (ReferenceCache.tryLoad("Category", (String) getValue("sCategrCd"), poCategory)) {
                    return poCategory;
                }

                poJSON = poCategory.openRecord((String) getValue("sCategrCd"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Category", (String) getValue("sCategrCd"), poCategory);
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

        if (!"".equals((String) getValue("sCompnyID"))) {
            if (poCompany.getEditMode() == EditMode.READY
                    && poCompany.getCompanyId().equals((String) getValue("sCompnyID"))) {
                return poCompany;
            } else {
                if (ReferenceCache.tryLoad("Company", (String) getValue("sCompnyID"), poCompany)) {
                    return poCompany;
                }

                poJSON = poCompany.openRecord((String) getValue("sCompnyID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Company", (String) getValue("sCompnyID"), poCompany);
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

    public Model_Department Department() throws SQLException, GuanzonException {
        if (poDepartment == null) {
            poDepartment = new ParamModels(poGRider).Department();
        }

        if (!"".equals((String) getValue("sDeptIDxx"))) {
            if (poDepartment.getEditMode() == EditMode.READY
                    && poDepartment.getDepartmentId().equals((String) getValue("sDeptIDxx"))) {
                return poDepartment;
            } else {
                if (ReferenceCache.tryLoad("Department", (String) getValue("sDeptIDxx"), poDepartment)) {
                    return poDepartment;
                }

                poJSON = poDepartment.openRecord((String) getValue("sDeptIDxx"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Department", (String) getValue("sDeptIDxx"), poDepartment);
                    return poDepartment;
                } else {
                    poDepartment.initialize();
                    return poDepartment;
                }
            }
        } else {
            poDepartment.initialize();
            return poDepartment;
        }
    }

    public Model_Term Term() throws SQLException, GuanzonException {
        if (poTerm == null) {
            poTerm = new ParamModels(poGRider).Term();
        }

        if (!"".equals((String) getValue("sTermCode"))) {
            if (poTerm.getEditMode() == EditMode.READY
                    && poTerm.getTermId().equals((String) getValue("sTermCode"))) {
                return poTerm;
            } else {
                if (ReferenceCache.tryLoad("Term", (String) getValue("sTermCode"), poTerm)) {
                    return poTerm;
                }

                poJSON = poTerm.openRecord((String) getValue("sTermCode"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Term", (String) getValue("sTermCode"), poTerm);
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

    public Model_Client_Master Supplier() throws SQLException, GuanzonException {
        if (poSupplier == null) {
            poSupplier = new ClientModels(poGRider).ClientMaster();
        }

        if (!"".equals((String) getValue("sSupplier"))) {
            if (poSupplier.getEditMode() == EditMode.READY
                    && poSupplier.getClientId().equals((String) getValue("sSupplier"))) {
                return poSupplier;
            } else {
                if (ReferenceCache.tryLoad("Client_Master", (String) getValue("sSupplier"), poSupplier)) {
                    return poSupplier;
                }

                poJSON = poSupplier.openRecord((String) getValue("sSupplier"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Client_Master", (String) getValue("sSupplier"), poSupplier);
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

        if (!"".equals((String) getValue("sAddressID"))) {
            if (poSupplierAdress.getEditMode() == EditMode.READY
                    && poSupplierAdress.getClientId().equals((String) getValue("sAddressID"))) {
                return poSupplierAdress;
            } else {
                if (ReferenceCache.tryLoad("Client_Address", (String) getValue("sAddressID"), poSupplierAdress)) {
                    return poSupplierAdress;
                }

                poJSON = poSupplierAdress.openRecord((String) getValue("sAddressID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Client_Address", (String) getValue("sAddressID"), poSupplierAdress);
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

        if (!"".equals((String) getValue("sContctID"))) {
            if (poSupplierContactPerson.getEditMode() == EditMode.READY
                    && poSupplierContactPerson.getClientId().equals((String) getValue("sContctID"))) {
                return poSupplierContactPerson;
            } else {
                if (ReferenceCache.tryLoad("Client_Institution_Contact_Person", (String) getValue("sContctID"), poSupplierContactPerson)) {
                    return poSupplierContactPerson;
                }

                poJSON = poSupplierContactPerson.openRecord((String) getValue("sContctID"));

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Client_Institution_Contact_Person", (String) getValue("sContctID"), poSupplierContactPerson);
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

    public Model_Client_Master Trucking() throws SQLException, GuanzonException {
        if (poSupplier == null) {
            poSupplier = new ClientModels(poGRider).ClientMaster();
        }

        if (!"".equals((String) getValue("sTrucking"))) {
            if (poSupplier.getEditMode() == EditMode.READY
                    && poSupplier.getClientId().equals((String) getValue("sTrucking"))) {
                return poSupplier;
            } else {
                poJSON = poSupplier.openRecord((String) getValue("sTrucking"));

                if ("success".equals((String) poJSON.get("result"))) {
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
    //end - reference object models

}
