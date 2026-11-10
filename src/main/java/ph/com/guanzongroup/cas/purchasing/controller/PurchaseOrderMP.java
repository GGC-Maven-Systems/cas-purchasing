package ph.com.guanzongroup.cas.purchasing.controller;

import java.sql.SQLException;
import org.guanzon.appdriver.base.GuanzonException;
import ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrderValidator;
import ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrder_MP;
import org.json.simple.JSONObject;

/**
 * Purchase Order controller for the Mobile Phone industry.
 *
 * <p>
 * Extends {@link PurchaseOrderMain}, which already runs InitTransaction,
 * NewTransaction, OpenTransaction, UpdateTransaction, SaveTransaction,
 * ConfirmTransaction, ApproveTransaction and CancelTransaction (including the
 * authorization matrix and the inventory ledger). Fill in the TODOs with the
 * Mobile Phone-specific steps, or override a whole procedure when Mobile Phone works
 * differently. Rules belong in {@link PurchaseOrder_MP}.</p>
 *
 * <p>
 * Reusable helpers (see PurchaseOrderGeneral.create... for how to build them):
 * PurchaseOrderSearch, PurchaseOrderSourceDocuments, PurchaseOrderAdvancePayment,
 * PurchaseOrderAttachments, PurchaseOrderAudit, PurchaseOrderPrinter.</p>
 */
public class PurchaseOrderMP extends PurchaseOrderMain {

    // =====================================================================
    // Required
    // =====================================================================
    @Override
    protected PurchaseOrderValidator createValidator() {
        return new PurchaseOrder_MP();
    }

    @Override
    public void initSQL() {
        //TODO Mobile Phone browse SQL
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

        //TODO Mobile Phone-specific preparation before saving
        return successJSON();
    }

    @Override
    public JSONObject saveOthers() {
        //TODO Mobile Phone records saved together with the PO
        return successJSON();
    }

    // =====================================================================
    // Optional hooks (delete the ones you don't need)
    // =====================================================================
    @Override
    protected void initOthers() {
        //TODO Mobile Phone objects created by InitTransaction
    }

    @Override
    protected JSONObject prepareStatusChange(String fsStatus)
            throws SQLException, GuanzonException, CloneNotSupportedException {
        //TODO Mobile Phone work before the status-change transaction (CONFIRMED, APPROVED, CANCELLED)
        return successJSON();
    }

    @Override
    protected JSONObject processStatusChange(String fsStatus, String fsRemarks) throws Exception {
        //TODO Mobile Phone work inside the status-change transaction; a non-success result rolls back
        return successJSON();
    }

    // Override a whole procedure when Mobile Phone differs, e.g.:
    // @Override
    // public JSONObject ConfirmTransaction(String remarks) throws ParseException, SQLException, CloneNotSupportedException, GuanzonException {
    //     JSONObject loJSON = super.ConfirmTransaction(remarks);
    //     ...
    //     return loJSON;
    // }
}
