package ph.com.guanzongroup.cas.purchasing.controller;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.json.simple.JSONObject;

/**
 * Who did what on a Purchase Order: prepared by, confirmed by, approvers.
 * Used by printing and the status history; nothing industry-specific.
 */
public class PurchaseOrderAudit {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");

    private final GRiderCAS poGRider;
    private final String psSourceCode;

    public PurchaseOrderAudit(GRiderCAS foGRider, String fsSourceCode) {
        poGRider = foGRider;
        psSourceCode = fsSourceCode;
    }

    /** Approvers of an approved PO, as "employee id - name date". */
    public List<String> getApprovers(String fsTransNo) throws SQLException {
        String lsSQL = "SELECT CONCAT(c.sEmployID, ' - ', d.sCompnyNm, ' ', c.dApproved) AS sApprover"
                + " FROM Transaction_Authorization_Master a"
                + " JOIN Transaction_Authorization_Detail b ON a.sTransNox = b.sSourceNo"
                + " JOIN Transaction_Authorization_Recipient c ON b.sTransNox = c.sSourceNo"
                + " LEFT JOIN Client_Master d ON c.sEmployID = d.sClientID"
                + " WHERE a.sSourceCD = " + SQLUtil.toSQL(psSourceCode)
                + " AND a.sSourceNo = " + SQLUtil.toSQL(fsTransNo)
                + " AND a.cTranStat = '2'"
                + " AND c.cTranStat = '1'";

        List<String> laApprovers = new ArrayList<>();
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (loRS == null) {
            return laApprovers;
        }
        try {
            while (loRS.next()) {
                laApprovers.add(loRS.getString("sApprover"));
            }
        } finally {
            MiscUtil.close(loRS);
        }
        return laApprovers;
    }

    /** Name of a system user; empty when not found. */
    public String getSysUserName(String fsUserId) throws SQLException {
        String lsSQL = "SELECT b.sCompnyNm"
                + " FROM xxxSysUser a"
                + " LEFT JOIN Client_Master b ON b.sClientID = a.sEmployNo"
                + " WHERE a.sUserIDxx = " + SQLUtil.toSQL(fsUserId);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (loRS == null) {
            return "";
        }
        try {
            if (loRS.next()) {
                String lsName = loRS.getString("sCompnyNm");
                return lsName == null ? "" : lsName;
            }
            return "";
        } finally {
            MiscUtil.close(loRS);
        }
    }

    /** Who encoded the PO. Result keys: sCompnyNm, sEntryDte. */
    public JSONObject getEntryBy(String fsTable, String fsTransNo) throws SQLException, GuanzonException {
        String lsSQL = "SELECT b.sModified, b.dModified"
                + " FROM PO_Master a"
                + " LEFT JOIN xxxAuditLogMaster b ON b.sSourceNo = a.sTransNox"
                + "     AND b.sEventNme LIKE 'ADD%NEW'"
                + "     AND b.sRemarksx = " + SQLUtil.toSQL(fsTable)
                + " WHERE a.sTransNox = " + SQLUtil.toSQL(fsTransNo)
                + " ORDER BY b.dModified ASC LIMIT 1";
        String[] laUser = readUserAndDate(lsSQL);

        JSONObject loJSON = success();
        loJSON.put("sCompnyNm", laUser[0]);
        loJSON.put("sEntryDte", laUser[1]);
        return loJSON;
    }

    /**
     * Who confirmed the PO (the latest confirmation, in case it was returned
     * and confirmed again). Result keys: sConfirmed, sConfrmDte.
     */
    public JSONObject getConfirmedBy(String fsTransNo) throws SQLException, GuanzonException {
        String lsSQL = "SELECT b.sModified, b.dModified"
                + " FROM PO_Master a"
                + " LEFT JOIN Transaction_Status_History b ON b.sSourceNo = a.sTransNox"
                + "     AND b.sTableNme = 'PO_Master'"
                + "     AND ( b.cRefrStat = " + SQLUtil.toSQL(PurchaseOrderStatus.CONFIRMED)
                + "         OR (ASCII(b.cRefrStat) - 64) = " + SQLUtil.toSQL(PurchaseOrderStatus.CONFIRMED) + " )"
                + " WHERE a.sTransNox = " + SQLUtil.toSQL(fsTransNo)
                + " ORDER BY b.dModified DESC LIMIT 1";
        String[] laUser = readUserAndDate(lsSQL);

        JSONObject loJSON = success();
        loJSON.put("sConfirmed", laUser[0]);
        loJSON.put("sConfrmDte", laUser[1]);
        return loJSON;
    }

    /** [user name, "MM-dd-yyyy HH:mm:ss"] from a query returning sModified, dModified. */
    private String[] readUserAndDate(String fsSQL) throws SQLException, GuanzonException {
        String[] laResult = {"", ""};
        ResultSet loRS = poGRider.executeQuery(fsSQL);
        if (loRS == null) {
            return laResult;
        }
        try {
            if (loRS.next()) {
                String lsUser = loRS.getString("sModified");
                if (lsUser != null && !lsUser.isEmpty()) {
                    //user ids longer than 10 characters are stored encrypted
                    laResult[0] = getSysUserName(lsUser.length() > 10 ? poGRider.Decrypt(lsUser) : lsUser);
                    LocalDateTime ldModified = loRS.getObject("dModified", LocalDateTime.class);
                    laResult[1] = ldModified == null ? "" : ldModified.format(DATE_TIME);
                }
            }
        } finally {
            MiscUtil.close(loRS);
        }
        return laResult;
    }

    private static JSONObject success() {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "success");
        return loJSON;
    }
}
