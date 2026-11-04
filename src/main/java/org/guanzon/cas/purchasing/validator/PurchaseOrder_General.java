package org.guanzon.cas.purchasing.validator;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;

/**
 * Purchase Order validator for General Inventory.
 *
 * Uses the shared rules of {@link PurchaseOrderValidator} plus the rules that
 * apply to General Inventory only (engineering project title).
 */
public class PurchaseOrder_General extends PurchaseOrderValidator {

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
     *
     * @author TEEJEI DE CELIS (rule), 04/09/2026
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