package org.guanzon.cas.purchasing.model;

import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Category;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Department;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.services.ParamModels;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.json.simple.JSONObject;

/**
 *
 * @author maynevval 10-07-2025 3PM
 */
public class Model_PO_Cancellation_Master extends Model {

    //reference objects
    //All reference fields below are intentionally NOT constructed in initialize() - see their
    //accessors, which build them lazily on first access so opening this record never touches
    //those tables.
    Model_Industry poIndustry;
    Model_Category poCategory;
    Model_Company poCompany;
    Model_Branch poBranch;
    Model_PO_Master poPurchaseOrder;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            poEntity.updateString("sBranchCd", poGRider.getBranchCode());
            poEntity.updateObject("dTransact", poGRider.getServerDate());
            poEntity.updateNull("sCompnyID");
            poEntity.updateNull("sSupplier");
            poEntity.updateDouble("nTranTotl", 0.00d);
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateNull("sSourceNo");
            poEntity.updateNull("sSourceCd");
            poEntity.updateNull("sEntryByx");
            poEntity.updateNull("dEntryDte");
            poEntity.updateObject("dModified", poGRider.getServerDate());
            poEntity.updateString("cTranStat", "0");

            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);

            ID = poEntity.getMetaData().getColumnLabel(1);

            //add model here
            pnEditMode = EditMode.UNKNOWN;

        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }
    //Getter & Setter 
    //sTransNox
    //sBranchCd
    //sIndstCdx*
    //sCategrCd*
    //dTransact
    //sCompnyID*
    //sSupplier*
    //nTranTotl
    //sRemarksx
    //sSourceNo
    //sSourceCd
    //nEntryNox
    //cTranStat
    //sEntryByx
    //dEntryDte
    //sModified
    //dModified

    //sTransNox
    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    //sBranchCd
    public JSONObject setBranchCd(String branchCd) {
        return setValue("sBranchCd", branchCd);
    }

    public String getBranchCd() {
        return (String) getValue("sBranchCd");
    }

    //sIndstCdx
    public JSONObject setIndustryId(String industryId) {
        return setValue("sIndstCdx", industryId);
    }

    public String getIndustryId() {
        return (String) getValue("sIndstCdx");
    }

    //sCategrCd 
    public JSONObject setCategory(String categoryCode) {
        return setValue("sCategrCd", categoryCode);
    }

    public String getCategory() {
        return (String) getValue("sCategrCd");
    }

    //dTransact
    public JSONObject setTransactionDate(Date transactionDate) {
        return setValue("dTransact", transactionDate);
    }

    public Date getTransactionDate() {
        return (Date) getValue("dTransact");
    }

    //sCompnyID
    public JSONObject setCompanyID(String companyID) {
        return setValue("sCompnyID", companyID);
    }

    public String getCompanyID() {
        return (String) getValue("sCompnyID");
    }

    //sSupplier
    public JSONObject setSupplierID(String supplierID) {
        return setValue("sSupplier", supplierID);
    }

    public String getSupplierID() {
        return (String) getValue("sSupplier");
    }

    //nTranTotl
    public JSONObject setTransactionTotal(Double transactionTotal) {
        return setValue("nTranTotl", transactionTotal);
    }

    public Double getTransactionTotal() {
        return Double.valueOf(getValue("nTranTotl").toString());
    }

    //sRemarksx
    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }

    //sSourceNo
    public JSONObject setSourceNo(String source) {
        return setValue("sSourceNo", source);
    }

    public String getSourceNo() {
        return (String) getValue("sSourceNo");
    }

    //sSourceCd
    public JSONObject setSourceCode(String sourcecd) {
        return setValue("sSourceCd", sourcecd);
    }

    public String getSourceCode() {
        return (String) getValue("sSourceCd");
    }

    //nEntryNox
    public JSONObject setEntryNo(int entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public int getEntryNo() {
        return (int) getValue("nEntryNox");
    }

    //cTranStat
    public JSONObject setTransactionStatus(String transactionStatus) {
        return setValue("cTranStat", transactionStatus);
    }

    public String getTransactionStatus() {
        return (String) getValue("cTranStat");
    }

    //sEntryByx
    public JSONObject setEntryId(String entryId) {
        return setValue("sEntryByx", entryId);
    }

    public String getEntryId() {
        return (String) getValue("sEntryByx");
    }

    //dEntryDte
    public JSONObject setEntryDate(Date modifiedDate) {
        return setValue("dEntryDte", modifiedDate);
    }

    public Date getEntryDate() {
        return (Date) getValue("dEntryDte");
    }

    //sModified
    public JSONObject setModifyingId(String modifyingId) {
        return setValue("sModified", modifyingId);
    }

    public String getModifyingId() {
        return (String) getValue("sModified");
    }

    //dModified
    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    @Override
    public String getNextCode() {
        return MiscUtil.getNextCode(this.getTable(), ID, true, poGRider.getGConnection().getConnection(), poGRider.getBranchCode());
    }

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

    public Model_PO_Master PurchaseOrderMaster() throws SQLException, GuanzonException {
        if (poPurchaseOrder == null) {
            poPurchaseOrder = new PurchaseOrderModels(poGRider).PurchaseOrderMaster();
        }

        if (!"".equals(getValue("sSourceNo"))) {
            if (this.poPurchaseOrder.getEditMode() == 1 && this.poPurchaseOrder
                    .getTransactionNo().equals(getValue("sSourceNo"))) {
                return this.poPurchaseOrder;
            }
            this.poJSON = this.poPurchaseOrder.openRecord((String) getValue("sSourceNo"));
            if ("success".equals(this.poJSON.get("result"))) {
                return this.poPurchaseOrder;
            }
            this.poPurchaseOrder.initialize();
            return this.poPurchaseOrder;
        }
        poPurchaseOrder.initialize();
        return this.poPurchaseOrder;
    }

}
