package ph.com.guanzongroup.cas.purchasing.status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;

/** Status codes, pending letters and labels (replaces three copied switch blocks). */
public class POStatusTest {

    @Test
    public void codesMatchPurchaseOrderStatus() {
        assertEquals(PurchaseOrderStatus.OPEN, POStatus.OPEN.getCode());
        assertEquals(PurchaseOrderStatus.APPROVED, POStatus.APPROVED.getCode());
        assertEquals(PurchaseOrderStatus.RETURNED, POStatus.RETURNED.getCode());
    }

    @Test
    public void pendingLetterIs64PlusCode() {
        assertEquals("A", POStatus.CONFIRMED.getPendingCode());
        assertEquals("E", POStatus.APPROVED.getPendingCode());
        assertEquals("I", POStatus.RETURNED.getPendingCode());
    }

    @Test
    public void lookupAcceptsCodesAndPendingLetters() {
        assertEquals(POStatus.APPROVED, POStatus.of("5"));
        assertEquals(POStatus.APPROVED, POStatus.of("E"));
        assertEquals(POStatus.CONFIRMED, POStatus.of("A"));
        assertNull(POStatus.of(""));
        assertNull(POStatus.of(null));
        assertNull(POStatus.of("7"));
        assertTrue(POStatus.isPending("E"));
        assertFalse(POStatus.isPending("5"));
    }

    @Test
    public void reportLabels() {
        assertEquals("OPEN", POStatus.label("0"));
        assertEquals("VOID", POStatus.label("4"));
        assertEquals("APPROVED+", POStatus.label("E"));
        assertEquals("CONFIRMED+", POStatus.label("A"));
        assertEquals("7", POStatus.label("7"));
    }

    @Test
    public void historyLabels() {
        assertEquals("-", POStatus.historyLabel(""));
        assertEquals("APPROVED", POStatus.historyLabel("5"));
        assertEquals("APRVL REQUESTD", POStatus.historyLabel("E"));
        assertEquals("CANCELLED", POStatus.historyLabel("C"));
        assertEquals("VOID", POStatus.historyLabel("4"));
    }

    @Test
    public void screenLabels() {
        assertEquals("VOIDED", POStatus.screenLabel("4"));
        assertEquals("CONFIRMED", POStatus.screenLabel("1"));
        assertEquals("APPROVED+", POStatus.screenLabel("E"));
        assertEquals("UNKNOWN", POStatus.screenLabel("7"));
    }
}
