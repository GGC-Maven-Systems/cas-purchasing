package org.guanzon.cas.purchasing.model;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
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
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.services.InvModels;
import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Master;
import org.guanzon.cas.inv.warehouse.services.InvWarehouseModels;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Category;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.model.Model_Term;
import org.guanzon.cas.parameter.services.ParamModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderProcessedStatus;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.json.simple.JSONObject;

public class Model_PO_Master extends Model {

    //reference objects
    //All reference fields below are intentionally NOT constructed in initialize() - see their
    //accessors, which build them lazily on first access so opening this record never touches
    //those tables.
    Model_Branch poBranch;
    Model_Industry poIndustry;
    Model_Category poCategory;
    Model_Company poCompany;
    Model_Term poTerm;
    Model_Inv_Stock_Request_Master poInvStockMaster;
    Model_Inventory poInventory;

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
            poEntity.updateObject("cProcessd", PurchaseOrderProcessedStatus.NO);
            poEntity.updateObject("cPreOwned", Logical.NO);
            poEntity.updateObject("cWithAddx", Logical.NO);
            poEntity.updateObject("dExpected", SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("dTransact", SQLUtil.toDate(xsDateShort(poGRider.getServerDate()), SQLUtil.FORMAT_SHORT_DATE));
            poEntity.updateObject("sBranchCd", poGRider.getBranchCode());
            poEntity.updateObject("sTermCode", "M0W1004");
            poEntity.updateObject("sDestinat", poGRider.getBranchCode());
            poEntity.updateObject("nDiscount", 0.00);
            poEntity.updateObject("nAddDiscx", 0.0000);
            poEntity.updateObject("nTranTotl", 0.0000);
            poEntity.updateObject("nAmtPaidx", 0.0000);
            poEntity.updateObject("nDPRatexx", 0.00);
            poEntity.updateObject("nAdvAmtxx", 0.0000);
            poEntity.updateObject("nNetTotal", 0.0000);
            poEntity.updateObject("cEmailSnt", Logical.NO);
            poEntity.updateObject("nEmailSnt", 0);
            poEntity.updateObject("cPrintxxx", Logical.NO);
            
            poEntity.updateString("cTranStat", PurchaseOrderStatus.OPEN);
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

    private static String xsDateShort(Date fdValue) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
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

    public JSONObject setIndustryID(String industryID) {
        return setValue("sIndstCdx", industryID);
    }

    public String getIndustryID() {
        return (String) getValue("sIndstCdx");
    }

    public JSONObject setCategoryCode(String categoryCode) {
        return setValue("sCategrCd", categoryCode);
    }

    public String getCategoryCode() {
        return (String) getValue("sCategrCd");
    }

    public JSONObject setTransactionDate(Date transactionDate) {
        return setValue("dTransact", transactionDate);
    }

    public Date getTransactionDate() {
        return (Date) getValue("dTransact");
    }

    public JSONObject setCompanyID(String companyID) {
        return setValue("sCompnyID", companyID);
    }

    public String getCompanyID() {
        return (String) getValue("sCompnyID");
    }

    public JSONObject setDestinationID(String destinationID) {
        return setValue("sDestinat", destinationID);
    }

    public String getDestinationID() {
        return (String) getValue("sDestinat");
    }

    public JSONObject setSupplierID(String supplierID) {
        return setValue("sSupplier", supplierID);
    }

    public String getSupplierID() {
        return (String) getValue("sSupplier");
    }

    public JSONObject setAddressID(String addressID) {
        return setValue("sAddrssID", addressID);
    }

    public String getAddressID() {
        return (String) getValue("sAddrssID");
    }

    public JSONObject setContactID(String contactID) {
        return setValue("sContctID", contactID);
    }

    public String getContactID() {
        return (String) getValue("sContctID");
    }

    public JSONObject setReference(String reference) {
        return setValue("sReferNox", reference);
    }

    public String getReference() {
        return (String) getValue("sReferNox");
    }

    public JSONObject setTermCode(String termCode) {
        return setValue("sTermCode", termCode);
    }

    public String getTermCode() {
        return (String) getValue("sTermCode");
    }

    public JSONObject setDiscount(Number discount) {
        return setValue("nDiscount", discount);
    }

    public Number getDiscount() {
        return (Number) getValue("nDiscount");
    }

    public JSONObject setAdditionalDiscount(Number additionalDiscount) {
        return setValue("nAddDiscx", additionalDiscount);
    }

    public Number getAdditionalDiscount() {
        return (Number) getValue("nAddDiscx");
    }

    public JSONObject setTranTotal(Number tranTotal) {
        return setValue("nTranTotl", tranTotal);
    }

    public Number getTranTotal() {
        return (Number) getValue("nTranTotl");
    }

    public JSONObject setAmountPaid(Number amountPaid) {
        return setValue("nAmtPaidx", amountPaid);
    }

    public Number getAmountPaid() {
        return (Number) getValue("nAmtPaidx");
    }

    public JSONObject setWithAdvPaym(boolean isWithAdvPaym) {
        return setValue("cWithAddx", isWithAdvPaym ? "1" : "0");
    }

    public boolean getWithAdvPaym() {
        return ((String) getValue("cWithAddx")).equals("1");
    }

    public JSONObject setDownPaymentRatesPercentage(Number downPaymentRatesPercentage) {
        return setValue("nDPRatexx", downPaymentRatesPercentage);
    }

    public Number getDownPaymentRatesPercentage() {
        return (Number) getValue("nDPRatexx");
    }

    public JSONObject setDownPaymentRatesAmount(Number downPaymentRatesAmount) {
        return setValue("nAdvAmtxx", downPaymentRatesAmount);
    }

    public Number getDownPaymentRatesAmount() {
        return (Number) getValue("nAdvAmtxx");
    }

    public JSONObject setNetTotal(Number netTotal) {
        return setValue("nNetTotal", netTotal);
    }

    public Number getNetTotal() {
        return (Number) getValue("nNetTotal");
    }

    public JSONObject setRemarks(String industryId) {
        return setValue("sRemarksx", industryId);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }

    public JSONObject setExpectedDate(Date expectedDate) {
        return setValue("dExpected", expectedDate);
    }

    public Date getExpectedDate() {
        return (Date) getValue("dExpected");
    }

    public JSONObject setEmailSent(String emailSent) {
        return setValue("cEmailSnt", emailSent);
    }

    public String getEmailSent() {
        return (String) getValue("cEmailSnt");
    }

    public JSONObject setNoEmailSent(Number noEmailSent) {
        return setValue("nEmailSnt", noEmailSent);
    }

    public Number getNoEmailSent() {
        return (Number) getValue("nEmailSnt");
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

    public Number getEntryNo() {
        return (Number) getValue("nEntryNox");
    }

    public JSONObject setInventoryTypeCode(String inventoryTypeCode) {
        return setValue("sInvTypCd", inventoryTypeCode);
    }

    public String getInventoryTypeCode() {
        return (String) getValue("sInvTypCd");
    }

    public JSONObject setPreOwned(boolean isWithAdvPaym) {
        return setValue("cPreOwned", isWithAdvPaym ? "1" : "0");
    }

    public boolean getPreOwned() {
        return ((String) getValue("cPreOwned")).equals("1");
    }

    public JSONObject setProcessed(boolean isProcessed) {
        return setValue("cProcessd", isProcessed ? "1" : "0");
    }

    public boolean getProcessed() {
        return ((String) getValue("cProcessd")).equals("1");
    }

    public JSONObject setTransactionStatus(String transactionStatus) {
        return setValue("cTranStat", transactionStatus);
    }

    public String getTransactionStatus() {
        return (String) getValue("cTranStat");
    }
    
    public String getConvertedTransactionStatus() {
        if("ABCDEFGHIJ".contains((String) getValue("cTranStat"))){
            return String.valueOf(((String) getValue("cTranStat")).getBytes()[0] - 64);
        }
        return (String) getValue("cTranStat");
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
    public void setSummarized(boolean summarized) {
        this.isSummarized = summarized;
    }

    public boolean isSummarized() {
        return isSummarized;
    }

    @Override
    public String getNextCode() {
        return MiscUtil.getNextCode(this.getTable(), ID, true, poGRider.getGConnection().getConnection(), poGRider.getBranchCode());
    }

    //reference object models
    public Model_Branch Branch() throws GuanzonException, SQLException {
        if (poBranch == null) {
            poBranch = new ParamModels(poGRider).Branch();
        }

        if (!"".equals((String) getValue("sDestinat"))) {
            if (poBranch.getEditMode() == EditMode.READY
                    && poBranch.getBranchCode().equals((String) getValue("sDestinat"))) {
                return poBranch;
            } else {
                if (ReferenceCache.tryLoad("Branch", (String) getValue("sDestinat"), poBranch)) {
                    return poBranch;
                }

                poJSON = poBranch.openRecord((String) getValue("sDestinat"));
                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Branch", (String) getValue("sDestinat"), poBranch);
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

    public Model_Branch Branchx() throws GuanzonException, SQLException {
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

    public Model_Industry Industry() throws GuanzonException, SQLException {
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

    public Model_Category Category() throws GuanzonException, SQLException {
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

    public Model_Company Company() throws GuanzonException, SQLException {
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

    public Model_Term Term() throws GuanzonException, SQLException {
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

    public Model_Client_Master Supplier() throws GuanzonException, SQLException {
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

    public Model_Client_Address SupplierAddress() throws GuanzonException, SQLException {
        if (poSupplierAdress == null) {
            poSupplierAdress = new ClientModels(poGRider).ClientAddress();
        }

        if (!"".equals((String) getValue("sClientID"))) {
            if (poSupplierAdress.getEditMode() == EditMode.READY
                    && poSupplierAdress.getClientId().equals((String) getValue("sAddressID"))) {
                return poSupplierAdress;
            } else {
                if (ReferenceCache.tryLoad("Client_Address", (String) getValue("sClientID"), poSupplierAdress)) {
                    return poSupplierAdress;
                }

                poJSON = poSupplierAdress.openRecord((String) getValue("sClientID"));
                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Client_Address", (String) getValue("sClientID"), poSupplierAdress);
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

    public Model_Client_Institution_Contact SupplierContactPerson() throws GuanzonException, SQLException {
        if (poSupplierContactPerson == null) {
            poSupplierContactPerson = new ClientModels(poGRider).ClientInstitutionContact();
        }

        if (!"".equals((String) getValue("sClientID"))) {
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

    public Model_Inv_Stock_Request_Master InvStockRequestMaster() throws GuanzonException, SQLException {
        if (poInvStockMaster == null) {
            poInvStockMaster = new InvWarehouseModels(poGRider).InventoryStockRequestMaster();
        }

        if (!"".equals((String) getValue("sTransNox"))) {
            if (poInvStockMaster.getEditMode() == EditMode.READY
                    && poInvStockMaster.getTransactionNo().equals((String) getValue("sTransNox"))) {
                return poInvStockMaster;
            } else {
                poJSON = poInvStockMaster.openRecord((String) getValue("sTransNox"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poInvStockMaster;
                } else {
                    poSupplierContactPerson.initialize();
                    return poInvStockMaster;
                }
            }
        } else {
            poInvStockMaster.initialize();
            return poInvStockMaster;
        }
    }

    public Model_Inventory Inventory() throws GuanzonException, SQLException {
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
    //end - reference object models
}
