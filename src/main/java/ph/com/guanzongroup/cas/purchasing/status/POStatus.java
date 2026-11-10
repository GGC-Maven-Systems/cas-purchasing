package ph.com.guanzongroup.cas.purchasing.status;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;

/**
 * Purchase Order statuses in one place: code, label, and the pending
 * (authorization requested) letter.
 *
 * <p>
 * A status waiting for matrix authorization is stored as a letter: 64 + the
 * status code ('A' = 1 CONFIRMED pending, 'E' = 5 APPROVED pending, ...).</p>
 *
 * <p>
 * The codes come from {@link PurchaseOrderStatus}, so they stay the single
 * source of truth.</p>
 */
public enum POStatus {

    OPEN(PurchaseOrderStatus.OPEN, "OPEN"),
    CONFIRMED(PurchaseOrderStatus.CONFIRMED, "CONFIRMED"),
    PROCESSED(PurchaseOrderStatus.PROCESSED, "PROCESSED"),
    CANCELLED(PurchaseOrderStatus.CANCELLED, "CANCELLED"),
    VOID(PurchaseOrderStatus.VOID, "VOID"),
    APPROVED(PurchaseOrderStatus.APPROVED, "APPROVED"),
    POSTED(PurchaseOrderStatus.POSTED, "POSTED"),
    RETURNED(PurchaseOrderStatus.RETURNED, "RETURNED");

    private final String psCode;
    private final String psLabel;

    POStatus(String fsCode, String fsLabel) {
        psCode = fsCode;
        psLabel = fsLabel;
    }

    /** Stored status code ("0", "1", ...). */
    public String getCode() {
        return psCode;
    }

    public String getLabel() {
        return psLabel;
    }

    /** Letter stored while this status waits for authorization (64 + code). */
    public String getPendingCode() {
        return Character.toString((char) (64 + Integer.parseInt(psCode)));
    }

    // =====================================================================
    // Lookups
    // =====================================================================
    /** Status for a stored code or pending letter; null when unknown or empty. */
    public static POStatus of(String fsCode) {
        if (fsCode == null || fsCode.isEmpty()) {
            return null;
        }
        String lsCode = isPending(fsCode) ? String.valueOf(fsCode.charAt(0) - 64) : fsCode;
        for (POStatus loStatus : values()) {
            if (loStatus.psCode.equals(lsCode)) {
                return loStatus;
            }
        }
        return null;
    }

    /** Whether the stored code is a pending letter ('A'..'Z'). */
    public static boolean isPending(String fsCode) {
        return fsCode != null && fsCode.length() == 1
                && fsCode.charAt(0) >= 'A' && fsCode.charAt(0) <= 'Z';
    }

    // =====================================================================
    // Labels used by the screens and reports
    // =====================================================================
    /**
     * Report label: "CONFIRMED", or "CONFIRMED+" while pending; the raw code
     * when unknown.
     */
    public static String label(String fsCode) {
        POStatus loStatus = of(fsCode);
        if (loStatus == null) {
            return fsCode == null ? "" : fsCode;
        }
        return loStatus.psLabel + (isPending(fsCode) ? "+" : "");
    }

    /**
     * Status history label: "-" when empty, "APRVL REQUESTD" for a pending
     * approval, the plain label for other statuses (pending or not).
     */
    public static String historyLabel(String fsCode) {
        if (fsCode == null || fsCode.isEmpty()) {
            return "-";
        }
        POStatus loStatus = of(fsCode);
        if (loStatus == null) {
            return fsCode;
        }
        if (loStatus == APPROVED && isPending(fsCode)) {
            return "APRVL REQUESTD";
        }
        return loStatus.psLabel;
    }

    /**
     * Label on the PO screen: "VOIDED" for void, "UNKNOWN" when unknown, with
     * "+" while pending.
     */
    public static String screenLabel(String fsCode) {
        POStatus loStatus = of(fsCode);
        String lsLabel = loStatus == null ? "UNKNOWN" : (loStatus == VOID ? "VOIDED" : loStatus.psLabel);
        return lsLabel + (isPending(fsCode) ? "+" : "");
    }
}
