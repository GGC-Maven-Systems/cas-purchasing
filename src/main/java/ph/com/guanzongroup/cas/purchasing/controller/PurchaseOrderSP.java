package ph.com.guanzongroup.cas.purchasing.controller;

import java.sql.SQLException;
import org.guanzon.appdriver.base.GuanzonException;
import ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrderValidator;
import ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrder_SP;
import org.json.simple.JSONObject;

/**
 * Purchase Order controller for the Spare Parts industry.
 *
 * <p>
 * Extends {@link PurchaseOrder}, which already runs InitTransaction,
 * NewTransaction, OpenTransaction, UpdateTransaction, SaveTransaction,
 * ConfirmTransaction, ApproveTransaction and CancelTransaction (including the
 * authorization matrix and the inventory ledger). Fill in the TODOs with the
 * Spare Parts-specific steps, or override a whole procedure when Spare Parts works
 * differently. Rules belong in {@link PurchaseOrder_SP}.</p>
 *
 * <p>
 * Reusable helpers (see PurchaseOrderGeneral.create... for how to build them):
 * PurchaseOrderSearch, PurchaseOrderSourceDocuments,
 * PurchaseOrderAttachments, PurchaseOrderAudit, PurchaseOrderPrinter.</p>
 */
public class PurchaseOrderSP extends PurchaseOrder {

    // =====================================================================
    // Required
    // =====================================================================
    @Override
    protected PurchaseOrderValidator createValidator() {
        return new PurchaseOrder_SP();
    }

    @Override
    public void initSQL() {
        //TODO Spare Parts browse SQL
        SQL_BROWSE = "SELECT"
                + "  a.sTransNox"
                + ", a.dTransact"
                + ", a.sReferNox"
                + " FROM PO_Master a";
    }

    // Saving: PurchaseOrder.willSave / saveOthers already prepare the rows,
    // run the shared save rules and save the attachments.
    @Override
    protected JSONObject prepareIndustrySave() throws SQLException, CloneNotSupportedException, GuanzonException {
        //TODO Spare Parts-specific preparation before saving
        return successJSON();
    }

    @Override
    protected JSONObject saveIndustryRecords() throws SQLException, CloneNotSupportedException, GuanzonException {
        //TODO Spare Parts records saved together with the PO; a non-success result rolls back
        return successJSON();
    }

    // =====================================================================
    // Optional hooks (delete the ones you don't need)
    // =====================================================================
    @Override
    protected void resetOthers() {
        //TODO clear Spare Parts objects of the previous PO (called by New / Open)
    }

    @Override
    protected void initOthers() {
        //TODO Spare Parts objects created by InitTransaction
    }

    @Override
    protected JSONObject prepareStatusChange(String fsStatus)
            throws SQLException, GuanzonException, CloneNotSupportedException {
        //TODO Spare Parts work before the status-change transaction (CONFIRMED, APPROVED, CANCELLED)
        return successJSON();
    }

    @Override
    protected JSONObject processStatusChange(String fsStatus, String fsRemarks) throws Exception {
        //TODO Spare Parts work inside the status-change transaction; a non-success result rolls back
        return successJSON();
    }

    // Override a whole procedure when Spare Parts differs, e.g.:
    // @Override
    // public JSONObject ConfirmTransaction(String remarks) throws ParseException, SQLException, CloneNotSupportedException, GuanzonException {
    //     JSONObject loJSON = super.ConfirmTransaction(remarks);
    //     ...
    //     return loJSON;
    // }
}
