package ph.com.guanzongroup.cas.purchasing.controller;

import java.sql.SQLException;
import org.guanzon.appdriver.base.GuanzonException;
import ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrderValidator;
import ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrder_Appliance;
import org.json.simple.JSONObject;

/**
 * Purchase Order controller for the Appliance industry.
 *
 * <p>
 * Extends {@link PurchaseOrderMain}, which already runs InitTransaction,
 * NewTransaction, OpenTransaction, UpdateTransaction, SaveTransaction,
 * ConfirmTransaction, ApproveTransaction and CancelTransaction (including the
 * authorization matrix and the inventory ledger). Fill in the TODOs with the
 * Appliance-specific steps, or override a whole procedure when Appliance works
 * differently. Rules belong in {@link PurchaseOrder_Appliance}.</p>
 *
 * <p>
 * Reusable helpers (see PurchaseOrderGeneral.create... for how to build them):
 * PurchaseOrderSearch, PurchaseOrderSourceDocuments, PurchaseOrderAdvancePayment,
 * PurchaseOrderAttachments, PurchaseOrderAudit, PurchaseOrderPrinter.</p>
 */
public class PurchaseOrderAppliance extends PurchaseOrderMain {

    // =====================================================================
    // Required
    // =====================================================================
    @Override
    protected PurchaseOrderValidator createValidator() {
        return new PurchaseOrder_Appliance();
    }

    @Override
    public void initSQL() {
        //TODO Appliance browse SQL
        SQL_BROWSE = "SELECT"
                + "  a.sTransNox"
                + ", a.dTransact"
                + ", a.sReferNox"
                + " FROM PO_Master a";
    }

    @Override
    public JSONObject willSave() throws SQLException, CloneNotSupportedException, GuanzonException {
        //transaction no., drop empty rows, number the rows
        prepareDetailsForSave();

        //TODO Appliance-specific preparation before saving
        return successJSON();
    }

    @Override
    public JSONObject saveOthers() {
        //TODO Appliance records saved together with the PO
        return successJSON();
    }

    // =====================================================================
    // Optional hooks (delete the ones you don't need)
    // =====================================================================
    @Override
    protected void initOthers() {
        //TODO Appliance objects created by InitTransaction
    }

    @Override
    protected JSONObject prepareStatusChange(String fsStatus)
            throws SQLException, GuanzonException, CloneNotSupportedException {
        //TODO Appliance work before the status-change transaction (CONFIRMED, APPROVED, CANCELLED)
        return successJSON();
    }

    @Override
    protected JSONObject processStatusChange(String fsStatus, String fsRemarks) throws Exception {
        //TODO Appliance work inside the status-change transaction; a non-success result rolls back
        return successJSON();
    }

    // Override a whole procedure when Appliance differs, e.g.:
    // @Override
    // public JSONObject ConfirmTransaction(String remarks) throws ParseException, SQLException, CloneNotSupportedException, GuanzonException {
    //     JSONObject loJSON = super.ConfirmTransaction(remarks);
    //     ...
    //     return loJSON;
    // }
}
