package ph.com.guanzongroup.cas.purchasing.controller;

import java.sql.SQLException;
import org.guanzon.appdriver.base.GuanzonException;
import ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrderValidator;
import ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrder_MC;
import org.json.simple.JSONObject;

/**
 * Purchase Order controller for the Motorcycle industry.
 *
 * <p>
 * Extends {@link PurchaseOrderMain}, which already runs InitTransaction,
 * NewTransaction, OpenTransaction, UpdateTransaction, SaveTransaction,
 * ConfirmTransaction, ApproveTransaction and CancelTransaction (including the
 * authorization matrix and the inventory ledger). Fill in the TODOs with the
 * Motorcycle-specific steps, or override a whole procedure when Motorcycle works
 * differently. Rules belong in {@link PurchaseOrder_MC}.</p>
 *
 * <p>
 * Reusable helpers (see PurchaseOrderGeneral.create... for how to build them):
 * PurchaseOrderSearch, PurchaseOrderSourceDocuments, PurchaseOrderAdvancePayment,
 * PurchaseOrderAttachments, PurchaseOrderAudit, PurchaseOrderPrinter.</p>
 */
public class PurchaseOrderMC extends PurchaseOrderMain {

    // =====================================================================
    // Required
    // =====================================================================
    @Override
    protected PurchaseOrderValidator createValidator() {
        return new PurchaseOrder_MC();
    }

    @Override
    public void initSQL() {
        //TODO Motorcycle browse SQL
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

        //TODO Motorcycle-specific preparation before saving
        return successJSON();
    }

    @Override
    public JSONObject saveOthers() {
        //TODO Motorcycle records saved together with the PO
        return successJSON();
    }

    // =====================================================================
    // Optional hooks (delete the ones you don't need)
    // =====================================================================
    @Override
    protected void initOthers() {
        //TODO Motorcycle objects created by InitTransaction
    }

    @Override
    protected JSONObject prepareStatusChange(String fsStatus)
            throws SQLException, GuanzonException, CloneNotSupportedException {
        //TODO Motorcycle work before the status-change transaction (CONFIRMED, APPROVED, CANCELLED)
        return successJSON();
    }

    @Override
    protected JSONObject processStatusChange(String fsStatus, String fsRemarks) throws Exception {
        //TODO Motorcycle work inside the status-change transaction; a non-success result rolls back
        return successJSON();
    }

    // Override a whole procedure when Motorcycle differs, e.g.:
    // @Override
    // public JSONObject ConfirmTransaction(String remarks) throws ParseException, SQLException, CloneNotSupportedException, GuanzonException {
    //     JSONObject loJSON = super.ConfirmTransaction(remarks);
    //     ...
    //     return loJSON;
    // }
}
