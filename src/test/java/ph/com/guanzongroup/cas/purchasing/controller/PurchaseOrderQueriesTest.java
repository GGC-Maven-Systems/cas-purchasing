package ph.com.guanzongroup.cas.purchasing.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Status filters shared by the lists, the browse dialog and the reports. */
public class PurchaseOrderQueriesTest {

    @Test
    public void statusIn_emptyMeansNoFilter() {
        assertEquals("", PurchaseOrderQueries.statusIn("a.cTranStat", ""));
        assertEquals("", PurchaseOrderQueries.statusIn("a.cTranStat", null));
        assertEquals("", PurchaseOrderQueries.statusOrPending("a.cTranStat", "", "5"));
    }

    @Test
    public void statusIn_oneCodeUsesEquals() {
        String lsFilter = PurchaseOrderQueries.statusIn("a.cTranStat", "1");
        assertTrue(lsFilter, lsFilter.startsWith("a.cTranStat = "));
    }

    @Test
    public void statusIn_severalCodesUseIn() {
        String lsFilter = PurchaseOrderQueries.statusIn("a.cTranStat", "015");
        assertTrue(lsFilter, lsFilter.startsWith("a.cTranStat IN ("));
        assertEquals("three codes", 2, lsFilter.split(", ", -1).length - 1);
    }

    @Test
    public void statusOrPending_addsPendingLetters() {
        String lsFilter = PurchaseOrderQueries.statusOrPending("a.cTranStat", "1", "5");
        assertTrue(lsFilter, lsFilter.startsWith("( a.cTranStat = "));
        assertTrue(lsFilter, lsFilter.contains(" OR (ASCII(a.cTranStat) - 64) IN ("));
        assertTrue(lsFilter, lsFilter.endsWith(" )"));
    }

    @Test
    public void statusOrPending_withoutPendingCodes() {
        String lsFilter = PurchaseOrderQueries.statusOrPending("a.cTranStat", "1", "");
        assertTrue(lsFilter, !lsFilter.contains("ASCII"));
    }
}
