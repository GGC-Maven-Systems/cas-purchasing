/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.model;

import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.client.model.Model_Client_Address;
import org.guanzon.cas.client.model.Model_Client_Master;
import org.guanzon.cas.client.model.Model_Client_Mobile;
import org.guanzon.cas.client.services.ClientModels;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.model.Model_Term;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import org.guanzon.cas.purchasing.services.QuotationModels;
import org.guanzon.cas.purchasing.status.POQuotationStatus;

/**
 *
 * @author Arsiela
 */
public class Model_PO_Quotation_Master extends Model {
    
    //All reference fields below are intentionally NOT constructed in initialize() - see their
    //accessors, which build them lazily on first access so opening this record never touches
    //those tables.
    Model_Industry poIndustry;
    Model_Company poCompany;
    Model_Branch poBranch;
    Model_Term poTerm;
    Model_Client_Master poSupplier;
    Model_Client_Address poSupplierAddress;
    Model_Client_Mobile poSupplierMobile;

    Model_PO_Quotation_Request_Master poQuotationRequest;
    
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

//            poEntity.updateObject("dModified", SQLUtil.toDate("1900-01-01", SQLUtil.FORMAT_SHORT_DATE));

            //assign default values
            poEntity.updateNull("dTransact");
            poEntity.updateNull("dReferDte");
            poEntity.updateNull("dValidity");
            poEntity.updateNull("dModified");
            poEntity.updateNull("dLastMail");
            poEntity.updateObject("nGrossAmt", 0.0000);
            poEntity.updateObject("nDiscount", 0.0000);
            poEntity.updateObject("nAddDiscx", 0.0000);
            poEntity.updateObject("nVATRatex", 0.00);
            poEntity.updateObject("nVATAmtxx", 0.0000);
            poEntity.updateObject("nFreightx", 0.00);
            poEntity.updateObject("nTWithHld", 0.00);
            poEntity.updateObject("nTranTotl", 0.0000);
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("nMailSent", 0);
//            poEntity.updateString("cProcessd", "0");
            poEntity.updateString("cTranStat", POQuotationStatus.OPEN);
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
    
    public JSONObject setTransactionDate(Date transactionDate) {
        return setValue("dTransact", transactionDate);
    }

    public Date getTransactionDate() {
        return (Date) getValue("dTransact");
    }

    public JSONObject setIndustryId(String industryId) {
        return setValue("sIndstCdx", industryId);
    }

    public String getIndustryId() {
        return (String) getValue("sIndstCdx");
    }

    public JSONObject setCompanyId(String companyId) {
        return setValue("sCompnyID", companyId);
    }

    public String getCompanyId() {
        return (String) getValue("sCompnyID");
    }

    public JSONObject setCategoryCode(String categoryCode) {
        return setValue("sCategrCd", categoryCode);
    }

    public String getCategoryCode() {
        return (String) getValue("sCategrCd");
    }

    public JSONObject setBranchCode(String branchCode) {
        return setValue("sBranchCd", branchCode);
    }

    public String getBranchCode() {
        return (String) getValue("sBranchCd");
    }

    public JSONObject setSupplierId(String supplier) {
        return setValue("sSupplier", supplier);
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

    public JSONObject setReferenceNo(String referenceNo) {
        return setValue("sReferNox", referenceNo);
    }

    public String getReferenceNo() {
        return (String) getValue("sReferNox");
    }
    
    public JSONObject setReferenceDate(Date referenceDate) {
        return setValue("dReferDte", referenceDate);
    }

    public Date getReferenceDate() {
        return (Date) getValue("dReferDte");
    }
    
    public JSONObject setValidityDate(Date validityDate) {
        if(validityDate == null){
            try {
                poEntity.updateNull("dValidity");
                return null;
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        return setValue("dValidity", validityDate);
    }

    public Date getValidityDate() {
        return (Date) getValue("dValidity");
    }

    public JSONObject setTerm(String term) {
        return setValue("sTermCode", term);
    }

    public String getTerm() {
        return (String) getValue("sTermCode");
    }
    
    public JSONObject setGrossAmount(Double grossAmount) {
        return setValue("nGrossAmt", grossAmount);
    }

    public Double getGrossAmount() {
        if (getValue("nGrossAmt") == null || "".equals(getValue("nGrossAmt"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nGrossAmt").toString());
    }
    
    public JSONObject setDiscountRate(Double grossAmount) {
        return setValue("nDiscount", grossAmount);
    }

    public Double getDiscountRate() {
        if (getValue("nDiscount") == null || "".equals(getValue("nDiscount"))) {
            return 0.00;
        }
        return Double.valueOf(getValue("nDiscount").toString());
    }
    
    public JSONObject setAdditionalDiscountAmount(Double additionalDiscountAmount) {
        return setValue("nAddDiscx", additionalDiscountAmount);
    }

    public Double getAdditionalDiscountAmount() {
        if (getValue("nAddDiscx") == null || "".equals(getValue("nAddDiscx"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nAddDiscx").toString());
    }
    
    public JSONObject setVatRate(Double vatRate) {
        return setValue("nVATRatex", vatRate);
    }

    public Double getVatRate() {
        if (getValue("nVATRatex") == null || "".equals(getValue("nVATRatex"))) {
            return 0.00;
        }
        return Double.valueOf(getValue("nVATRatex").toString());
    }
    
    public JSONObject setVatAmount(Double vatAmount) {
        return setValue("nVATAmtxx", vatAmount);
    }

    public Double getVatAmount() {
        if (getValue("nVATAmtxx") == null || "".equals(getValue("nVATAmtxx"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nVATAmtxx").toString());
    }
    
    public JSONObject isVatable(boolean isVatable) {
        return setValue("cVATAdded", isVatable ? "1" : "0");
    }

    public boolean isVatable() {
        return ((String) getValue("cVATAdded")).equals("1");
    }
    
    
//    public JSONObject setTaxAmount(Double taxAmount) {
//        return setValue("nTWithHld", taxAmount);
//    }
//
//    public Double getTaxAmount() {
//        if (getValue("nTWithHld") == null || "".equals(getValue("nTWithHld"))) {
//            return 0.0000;
//        }
//        return Double.valueOf(getValue("nTWithHld").toString());
//    }
    
    public JSONObject setFreightAmount(Double freighAmount) {
        return setValue("nFreightx", freighAmount);
    }

    public Double getFreightAmount() {
        if (getValue("nFreightx") == null || "".equals(getValue("nFreightx"))) {
            return 0.00;
        }
        return Double.valueOf(getValue("nFreightx").toString());
    }
    
    public JSONObject setTransactionTotal(Double transactionTotal) {
        return setValue("nTranTotl", transactionTotal);
    }

    public Double getTransactionTotal() {
        if (getValue("nTranTotl") == null || "".equals(getValue("nTranTotl"))) {
            return 0.0000;
        }
        return Double.valueOf(getValue("nTranTotl").toString());
    }
    
    public JSONObject setWitholdingTax(Double transactionTotal) {
        return setValue("nTWithHld", transactionTotal);
    }

    public Double getWitholdingTax() {
        if (getValue("nTWithHld") == null || "".equals(getValue("nTWithHld"))) {
            return 0.00;
        }
        return Double.valueOf(getValue("nTWithHld").toString());
    }

    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }

    public JSONObject setSourceNo(String sourceNo) {
        return setValue("sSourceNo", sourceNo);
    }

    public String getSourceNo() {
        return (String) getValue("sSourceNo");
    }

    public JSONObject setSourceCode(String sourceCode) {
        return setValue("sSourceCd", sourceCode);
    }

    public String getSourceCode() {
        return (String) getValue("sSourceCd");
    }
    
    public JSONObject setEntryNo(Number entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public Number getEntryNo() {
        return (Number) getValue("nEntryNox");
    }
    
    public JSONObject setTransactionStatus(String transactionStatus) {
        return setValue("cTranStat", transactionStatus);
    }
    
    public JSONObject isMailSent(boolean isMailSent) {
        return setValue("cMailSent", isMailSent ? "1" : "0");
    }

    public boolean isMailSent() {
        return ((String) getValue("cMailSent")).equals("1");
    }
    
    public JSONObject setNumberMailSent(Number numberMailSent) {
        return setValue("nMailSent", numberMailSent);
    }

    public Number getNumberMailSent() {
        return (Number) getValue("nMailSent");
    }

    public JSONObject setLastMail(Date lastMail) {
        return setValue("dLastMail", lastMail);
    }

    public Date getLastMail() {
        return (Date) getValue("dLastMail");
    }

    public String getTransactionStatus() {
        return (String) getValue("cTranStat");
    }
    
    public JSONObject isProcessed(boolean isProcessed) {
        return setValue("cProcessd", isProcessed ? "1" : "0");
    }

    public boolean isProcessed() {
        return ((String) getValue("cProcessd")).equals("1");
    }

    public JSONObject setPrepared(String preparedBy) {
        return setValue("sPrepared", preparedBy);
    }

    public String getPrepared() {
        return (String) getValue("sPrepared");
    }

    public JSONObject setPreparedDate(Date preparedDate) {
        return setValue("dPrepared", preparedDate);
    }

    public Date setPreparedDate() {
        return (Date) getValue("dPrepared");
    }

    public JSONObject setModifyingId(String modifiedBy) {
        return setValue("sModified", modifiedBy);
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

    public Model_Client_Address Address() throws SQLException, GuanzonException {
        if (poSupplierAddress == null) {
            poSupplierAddress = new ClientModels(poGRider).ClientAddress();
        }

        String id = (String) (getValue("sSupplier") == null ? "" : getValue("sSupplier"));
        
        if (!"".equals(id)) {
            if (poSupplierAddress.getEditMode() == EditMode.READY
                    && poSupplierAddress.getClientId().equals(id)) {
                return poSupplierAddress;
            } else {
                if (ReferenceCache.tryLoad("Client_Address", id, poSupplierAddress)) {
                    return poSupplierAddress;
                }

                poJSON = poSupplierAddress.openRecord(id); //sAddrssID

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Client_Address", id, poSupplierAddress);
                    return poSupplierAddress;
                } else {
                    poSupplierAddress.initialize();
                    return poSupplierAddress;
                }
            }
        } else {
            poSupplierAddress.initialize();
            return poSupplierAddress;
        }
    }

    public Model_Client_Mobile Contact() throws SQLException, GuanzonException {
        if (poSupplierMobile == null) {
            poSupplierMobile = new ClientModels(poGRider).ClientMobile();
        }

        String id = (String) (getValue("sContctID") == null ? "" : getValue("sContctID"));
        
        if (!"".equals(id)) {
            if (poSupplierMobile.getEditMode() == EditMode.READY
                    && poSupplierMobile.getClientId().equals(id)) {
                return poSupplierMobile;
            } else {
                if (ReferenceCache.tryLoad("Client_Mobile", id, poSupplierMobile)) {
                    return poSupplierMobile;
                }

                poJSON = poSupplierMobile.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Client_Mobile", id, poSupplierMobile);
                    return poSupplierMobile;
                } else {
                    poSupplierMobile.initialize();
                    return poSupplierMobile;
                }
            }
        } else {
            poSupplierMobile.initialize();
            return poSupplierMobile;
        }
    }

    public Model_Term Term() throws SQLException, GuanzonException {
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

    public Model_PO_Quotation_Request_Master POQuotationRequest() throws SQLException, GuanzonException {
        if (poQuotationRequest == null) {
            poQuotationRequest = new QuotationModels(poGRider).POQuotationRequestMaster();
        }
        
        String id = (String) (getValue("sSourceNo") == null ? "" : getValue("sSourceNo"));

        if (!"".equals(id)) {
            if (poQuotationRequest.getEditMode() == EditMode.READY
                    && poQuotationRequest.getIndustryId().equals(id)) {
                return poQuotationRequest;
            } else {
                poJSON = poQuotationRequest.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    return poQuotationRequest;
                } else {
                    poQuotationRequest.initialize();
                    return poQuotationRequest;
                }
            }
        } else {
            poQuotationRequest.initialize();
            return poQuotationRequest;
        }
    }
    //end reference object models
}
