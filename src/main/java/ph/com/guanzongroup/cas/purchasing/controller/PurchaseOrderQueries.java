package ph.com.guanzongroup.cas.purchasing.controller;

import java.util.StringJoiner;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;

/**
 * The Purchase Order controller SQL in one place.
 *
 * <p>
 * Base queries here; the controllers add their filters with
 * {@code MiscUtil.addCondition} and the status helpers below. Values are
 * always quoted with {@link SQLUtil#toSQL(Object)}. (The validator and
 * {@link PurchaseOrderAudit} keep their own few queries next to the rules that
 * use them.)</p>
 */
public final class PurchaseOrderQueries {

    private PurchaseOrderQueries() {
    }

    // =====================================================================
    // Status filters
    // =====================================================================
    /**
     * "col = '1'" or "col IN ('1', '5')" for a string of one-character status
     * codes; empty when fsCodes is empty.
     */
    public static String statusIn(String fsColumn, String fsCodes) {
        if (fsCodes == null || fsCodes.isEmpty()) {
            return "";
        }
        if (fsCodes.length() == 1) {
            return fsColumn + " = " + SQLUtil.toSQL(fsCodes);
        }
        return fsColumn + " IN (" + quoteEach(fsCodes) + ")";
    }

    /**
     * Status filter that also matches the pending letters (64 + code) of
     * fsPendingCodes: "( col IN (...) OR (ASCII(col) - 64) IN (...) )".
     * Empty when fsCodes is empty.
     */
    public static String statusOrPending(String fsColumn, String fsCodes, String fsPendingCodes) {
        if (fsCodes == null || fsCodes.isEmpty()) {
            return "";
        }
        String lsPending = fsPendingCodes == null || fsPendingCodes.isEmpty()
                ? ""
                : " OR (ASCII(" + fsColumn + ") - 64) IN (" + quoteEach(fsPendingCodes) + ")";
        return "( " + statusIn(fsColumn, fsCodes) + lsPending + " )";
    }

    private static String quoteEach(String fsCodes) {
        StringJoiner loList = new StringJoiner(", ");
        for (char lcCode : fsCodes.toCharArray()) {
            loList.add(SQLUtil.toSQL(Character.toString(lcCode)));
        }
        return loList.toString();
    }

    // =====================================================================
    // Purchase orders
    // =====================================================================
    /** Browse dialog (SearchTransaction). */
    public static String browse() {
        return "SELECT"
                + "  a.sTransNox"
                + ", a.dTransact"
                + ", b.sDescript"
                + ", c.sCompnyNm"
                + ", e.sCompnyNm"
                + ", f.sDescript"
                + ", a.sBranchCd"
                + ", a.sReferNox"
                + " FROM PO_Master a"
                + " LEFT JOIN Industry b ON a.sIndstCdx = b.sIndstCdx"
                + " LEFT JOIN Company c ON c.sCompnyID = a.sCompnyID"
                + " LEFT JOIN Client_Master e ON a.sSupplier = e.sClientID"
                + " JOIN Category f ON a.sCategrCd = f.sCategrCd";
    }

    /** Purchase order list (getPurchaseOrder / getConfirmedPurchaseOrder). */
    public static String purchaseOrderList() {
        return "SELECT"
                + "  a.sTransNox"
                + ", c.sBranchNm"
                + ", a.sBranchCd"
                + ", a.dTransact"
                + ", a.cTranStat"
                + ", e.sCompnyNm"
                + " FROM PO_Master a"
                + " LEFT JOIN Branch c ON a.sBranchCd = c.sBranchCd"
                + " LEFT JOIN Client_Master e ON e.sClientID = a.sSupplier";
    }

    /** Most recent earlier status of a purchase order (RevertStatus). */
    public static String previousStatus(String fsTransNo, String fsTable, String fsCurrentStatus) {
        return "SELECT cRefrStat"
                + " FROM Transaction_Status_History"
                + " WHERE sSourceNo = " + SQLUtil.toSQL(fsTransNo)
                + " AND sTableNme = " + SQLUtil.toSQL(fsTable)
                + " AND cRefrStat <> " + SQLUtil.toSQL(fsCurrentStatus)
                + " ORDER BY sTransNox DESC LIMIT 1";
    }

    /** Saved detail lines of a purchase order, in entry order (returned-PO check). */
    public static String savedLines(String fsTransNo) {
        return "SELECT sStockIDx, nQuantity"
                + " FROM PO_Detail"
                + " WHERE sTransNox = " + SQLUtil.toSQL(fsTransNo)
                + " ORDER BY nEntryNox";
    }

    /** Inventory type of an industry + category. */
    public static String inventoryType(String fsIndustryId, String fsCategoryCode) {
        return "SELECT a.sInvTypCd"
                + " FROM Category a"
                + " WHERE a.sIndstCdx = " + SQLUtil.toSQL(fsIndustryId)
                + " AND a.sCategrCd = " + SQLUtil.toSQL(fsCategoryCode);
    }

    // =====================================================================
    // Source documents
    // =====================================================================
    /** Stock requests that can be ordered (part 1 of the source list). */
    public static String stockRequestSources() {
        return "SELECT"
                + "  a.sTransNox AS sTransNox"
                + ", e.sBranchNm AS sBranchNm"
                + ", a.sBranchCd"
                + ", a.cTranStat"
                + ", a.dTransact AS dTransact"
                + ", a.sReferNox"
                + ", a.sIndstCdx"
                + ", COUNT(DISTINCT b.sStockIDx) AS total_details"
                + ", SUM(b.nApproved - (b.nIssueQty + b.nOrderQty)) AS total_request"
                + ", " + SQLUtil.toSQL(PurchaseOrderStatus.SourceCode.STOCKREQUEST) + " AS request_type"
                + " FROM Inv_Stock_Request_Master a"
                + " LEFT JOIN Inv_Stock_Request_Detail b ON a.sTransNox = b.sTransNox"
                + " LEFT JOIN Inventory c ON b.sStockIDx = c.sStockIDx"
                + " LEFT JOIN Branch e ON a.sBranchCd = e.sBranchCd"
                + " LEFT JOIN Inv_Supplier g ON g.sStockIDx = c.sStockIDx";
    }

    /** Approved PO quotations (part 2 of the source list). */
    public static String quotationSources() {
        return "SELECT"
                + "  a.sTransNox AS sTransNox"
                + ", e.sBranchNm AS sBranchNm"
                + ", a.sBranchCd"
                + ", a.cTranStat"
                + ", a.dTransact AS dTransact"
                + ", a.sReferNox"
                + ", a.sIndstCdx"
                + ", COUNT(DISTINCT b.sStockIDx) AS total_details"
                + ", SUM(b.nQuantity) AS total_request"
                + ", " + SQLUtil.toSQL(PurchaseOrderStatus.SourceCode.POQUOTATION) + " AS request_type"
                + " FROM PO_Quotation_Master a"
                + " LEFT JOIN PO_Quotation_Detail b ON a.sTransNox = b.sTransNox"
                + " LEFT JOIN Branch e ON a.sBranchCd = e.sBranchCd";
    }

    /** Items of confirmed stock requests that match the PO (addStockRequestOrdersToPODetail). */
    public static String stockRequestItems() {
        return "SELECT b.sStockIDx"
                + " FROM Inv_Stock_Request_Master a"
                + " LEFT JOIN Branch e ON a.sBranchCd = e.sBranchCd"
                + " LEFT JOIN Inv_Stock_Request_Detail b ON a.sTransNox = b.sTransNox"
                + " LEFT JOIN Inventory c ON b.sStockIDx = c.sStockIDx"
                + " LEFT JOIN Inv_Supplier g ON g.sStockIDx = c.sStockIDx"
                + " LEFT JOIN Category h ON h.sCategrCd = c.sCategCd1";
    }

    /**
     * Total quantity already ordered for one source document item by confirmed
     * or approved purchase orders (optionally excluding one PO).
     */
    public static String orderedQuantity(String fsSourceNo, String fsStockId, String fsSourceCode, String fsExcludeTransNo) {
        return "SELECT IFNULL(SUM(b.nQuantity), 0) AS nQuantity"
                + " FROM PO_Master a"
                + " JOIN PO_Detail b ON b.sTransNox = a.sTransNox"
                + " WHERE b.sSourceNo = " + SQLUtil.toSQL(fsSourceNo)
                + " AND b.sStockIDx = " + SQLUtil.toSQL(fsStockId)
                + " AND b.sSourceCd = " + SQLUtil.toSQL(fsSourceCode)
                + " AND " + statusIn("a.cTranStat", PurchaseOrderStatus.CONFIRMED + PurchaseOrderStatus.APPROVED)
                + (fsExcludeTransNo == null ? "" : " AND a.sTransNox <> " + SQLUtil.toSQL(fsExcludeTransNo));
    }

    // =====================================================================
    // Reports
    // =====================================================================
    /** Summary report (one row per PO). Filters use alias a = PO_Master. */
    public static String summaryReport() {
        return "SELECT DISTINCT"
                + "  g.sCompnyNm AS Supplier"
                + ", c.sBranchNm AS Destination"
                + ", d.sBranchNm AS Branch"
                + ", a.sTransNox"
                + ", a.sReferNox"
                + ", a.dTransact"
                + ", e.sDescript AS Category"
                + ", f.sDescript AS Term"
                + ", a.nTranTotl"
                + ", a.cTranStat"
                + " FROM PO_Master a"
                + " LEFT JOIN AP_Client_Master b ON a.sSupplier = b.sClientID"
                + " LEFT JOIN Branch c ON a.sDestinat = c.sBranchCd"
                + " LEFT JOIN Branch d ON a.sBranchCd = d.sBranchCd"
                + " LEFT JOIN Category e ON a.sCategrCd = e.sCategrCd"
                + " LEFT JOIN Term f ON a.sTermCode = f.sTermCode"
                + " LEFT JOIN Client_Master g ON b.sClientID = g.sClientID";
    }

    /** Detailed report (one row per PO line). Filters use alias b = PO_Master, a = PO_Detail. */
    public static String detailedReport() {
        return "SELECT DISTINCT"
                + "  h.sCompnyNm AS Supplier"
                + ", d.sBranchNm AS Destination"
                + ", b.sTransNox"
                + ", b.dTransact"
                + ", j.sDescript AS Brand"
                + ", k.sModelCde AS ModelCode"
                + ", k.sDescript AS ModelName"
                + ", l.sDescript AS Color"
                + ", a.nQuantity"
                + ", a.nReceived"
                + ", a.nCancelld"
                + ", i.nUnitPrce"
                + ", i.sBarCodex"
                + ", i.sDescript AS Description"
                + ", ROUND(a.nQuantity * i.nUnitPrce, 4) AS Total"
                + " FROM PO_Detail a"
                + " LEFT JOIN PO_Master b ON a.sTransNox = b.sTransNox"
                + " LEFT JOIN AP_Client_Master c ON b.sSupplier = c.sClientID"
                + " LEFT JOIN Branch d ON b.sDestinat = d.sBranchCd"
                + " LEFT JOIN Client_Master h ON c.sClientID = h.sClientID"
                + " LEFT JOIN Inventory i ON a.sStockIDx = i.sStockIDx"
                + " LEFT JOIN Brand j ON i.sBrandIDx = j.sBrandIDx"
                + " LEFT JOIN Model k ON i.sModelIDx = k.sModelIDx"
                + " LEFT JOIN Color l ON i.sColorIDx = l.sColorIDx";
    }
}
