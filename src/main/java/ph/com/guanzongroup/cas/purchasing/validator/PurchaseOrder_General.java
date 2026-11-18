package ph.com.guanzongroup.cas.purchasing.validator;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.json.simple.JSONObject;

/**
 * Purchase Order validator for General Inventory.
 *
 * Uses the shared rules of {@link PurchaseOrderValidator} plus the rules that
 * apply to General Inventory only: advance payment and the engineering
 * project title.
 */
public class PurchaseOrder_General extends PurchaseOrderValidator {

    // =====================================================================
    // Rule: advance payment (General Inventory only)
    // =====================================================================
    /** Saving an OPEN PO: "with advance payment" needs a rate or an amount. */
    @Override
    protected JSONObject industryNewRules() throws SQLException, GuanzonException {
        if (poMaster.getWithAdvPaym()
                && money(poMaster.getDownPaymentRatesPercentage()).signum() <= 0
                && money(poMaster.getDownPaymentRatesAmount()).signum() <= 0) {
            return error("You selected advance rate, but no Adv Rate and Adv Amount.");
        }
        return success();
    }

    public JSONObject validateAdvancePaymentRate(double fnRate) {
        BigDecimal ldAfterDiscounts = getAmountAfterDiscounts();
        if (ldAfterDiscounts.signum() <= 0) {
            return error("Invalid Advance Payment Rate, the total transaction amount is 0.0000");
        }
        if (fnRate < 0.0000 || fnRate > 100.0000) {
            return error("Invalid Advance Payment Rate. Must be between 0.0000 and 100.0000");
        }
        BigDecimal ldTotalAdvance = percentOf(ldAfterDiscounts, money(fnRate))
                .add(money(poMaster.getDownPaymentRatesAmount()));
        if (ldTotalAdvance.compareTo(ldAfterDiscounts) > 0) {
            return error("Invalid Downpayment Total.");
        }
        return success();
    }

    public JSONObject validateAdvancePaymentAmount(double fnAmount) {
        BigDecimal ldAfterDiscounts = getAmountAfterDiscounts();
        if (ldAfterDiscounts.signum() <= 0) {
            return error("Invalid Advance Payment Amount, the total transaction amount is 0.0000");
        }
        BigDecimal ldAmount = money(fnAmount);
        if (ldAmount.signum() < 0 || ldAmount.compareTo(ldAfterDiscounts) > 0) {
            return error("Invalid Advance Payment Amount");
        }
        BigDecimal ldTotalAdvance = percentOf(ldAfterDiscounts, money(poMaster.getDownPaymentRatesPercentage()))
                .add(ldAmount);
        if (ldTotalAdvance.compareTo(ldAfterDiscounts) > 0) {
            return error("Invalid Downpayment Total.");
        }
        return success();
    }

    // =====================================================================
    // Rule: engineering project title (General Inventory only)
    // =====================================================================
    /**
     * Whether saving or changing the status must tag/untag the project
     * referenced by this PO.
     *
     * <p>
     * Applies only to the department in {@link #getProjectTitleDepartment()},
     * when a reference is set, for OPEN, CANCELLED and VOID. With
     * fbCheckUsage, the project must not be used by more than one PO.</p>
     */
    public boolean isProjectTitleUpdateRequired(String fsStatus, boolean fbCheckUsage) throws SQLException {
        String lsDepartment = getProjectTitleDepartment();
        if (isEmpty(lsDepartment) || !lsDepartment.equals(poGrider.getDepartment())) {
            return false;
        }
        if (isEmpty(poMaster.getReference())) {
            return false;
        }
        if (!PurchaseOrderStatus.OPEN.equals(fsStatus)
                && !PurchaseOrderStatus.CANCELLED.equals(fsStatus)
                && !PurchaseOrderStatus.VOID.equals(fsStatus)) {
            return false;
        }
        return !fbCheckUsage || getProjectUsageCount(poMaster.getReference()) <= 1;
    }

    /** Number of Purchase Orders that reference the project. */
    public int getProjectUsageCount(String fsProjectCode) throws SQLException {
        String lsSQL = "SELECT COUNT(*) AS nCount"
                + " FROM PO_Master"
                + " WHERE sReferNox = " + SQLUtil.toSQL(fsProjectCode);
        ResultSet loRS = poGrider.executeQuery(lsSQL);
        if (loRS == null) {
            throw new SQLException("Unable to check project usage.");
        }
        try {
            return loRS.next() ? loRS.getInt("nCount") : 0;
        } finally {
            loRS.close();
        }
    }

    /** Department that tags project titles. Default: system property allowed.department. */
    protected String getProjectTitleDepartment() {
        return System.getProperty("allowed.department");
    }
}
