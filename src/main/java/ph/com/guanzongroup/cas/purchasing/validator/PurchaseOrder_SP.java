package ph.com.guanzongroup.cas.purchasing.validator;

import java.sql.SQLException;
import org.guanzon.appdriver.base.GuanzonException;
import org.json.simple.JSONObject;

/**
 * Purchase Order validator for the Spare Parts industry.
 *
 * The shared General Inventory rules always run first (see
 * {@link PurchaseOrderValidator}). Put Spare Parts-only rules in the hooks below.
 * Delete the hooks you don't need; the base class already returns success.
 */
public class PurchaseOrder_SP extends PurchaseOrderValidator {

    // ------------------------------------------------------------------
    // Settings: tune a shared rule for Spare Parts (remove if not needed)
    // ------------------------------------------------------------------
    // @Override
    // protected double getPriceTolerancePct() {
    //     return 0.0000; //e.g. allow a small price difference vs quotation
    // }

    // ------------------------------------------------------------------
    // Spare Parts rules per status
    // ------------------------------------------------------------------
    @Override
    protected JSONObject industryNewRules() throws SQLException, GuanzonException {
        //TODO Spare Parts rules for saving an OPEN purchase order
        //Example (needs setDetail() from the controller):
        //for (Model_PO_Detail loDetail : getDetails()) {
        //    if (...) return error("...");
        //}
        return success();
    }

    @Override
    protected JSONObject industryConfirmedRules() throws SQLException, GuanzonException {
        //TODO Spare Parts rules before confirmation
        return success();
    }

    @Override
    protected JSONObject industryApprovedRules() throws SQLException, GuanzonException {
        //TODO Spare Parts rules before approval (run before the authorization matrix)
        return success();
    }

    @Override
    protected JSONObject industryCancelledRules() throws SQLException, GuanzonException {
        //TODO Spare Parts rules before cancellation
        return success();
    }

    @Override
    protected JSONObject industryVoidRules() throws SQLException, GuanzonException {
        //TODO Spare Parts rules before voiding
        return success();
    }
}
