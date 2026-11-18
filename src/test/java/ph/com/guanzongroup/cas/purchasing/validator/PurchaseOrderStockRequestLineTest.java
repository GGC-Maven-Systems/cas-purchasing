package ph.com.guanzongroup.cas.purchasing.validator;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ph.com.guanzongroup.cas.purchasing.validator.FakeResultSet.row;
import static ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrderTestData.driver;
import static ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrderTestData.master;

import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.json.simple.JSONObject;
import org.junit.Test;

/**
 * Stock request lines: the "already processed" check counts only what OTHER
 * purchase orders ordered, so a confirmed PO can still be approved.
 */
public class PurchaseOrderStockRequestLineTest {

    /** Request of 50 approved, nothing cancelled or issued; PO line of fnQuantity. */
    private static JSONObject check(String fsStatus, double fnQuantity, double fnOrderedByOthers) throws Exception {
        Model_Inv_Stock_Request_Detail loRequest = mock(Model_Inv_Stock_Request_Detail.class);
        doReturn(50.0).when(loRequest).getApproved();
        doReturn(0.0).when(loRequest).getCancelled();
        doReturn(0.0).when(loRequest).getIssued();

        Model_PO_Detail loLine = mock(Model_PO_Detail.class);
        doReturn("SR-001").when(loLine).getSouceNo();
        doReturn("STK-1").when(loLine).getStockID();
        doReturn(fnQuantity).when(loLine).getQuantity();
        doReturn(loRequest).when(loLine).InvStockRequestDetail();

        PurchaseOrderValidator loValidator = new PurchaseOrderValidator() {
        };
        loValidator.setApplicationDriver(driver("D001", FakeResultSet.of(row("nQuantity", fnOrderedByOthers))));
        loValidator.setMaster(master().build());
        loValidator.setTransactionStatus(fsStatus);
        return loValidator.validateStockRequestLine(loLine);
    }

    @Test
    public void approvingAPoThatOrderedEverythingIsAllowed() throws Exception {
        //this PO ordered all 50 at confirmation; no other PO ordered any
        assertEquals("success", check(PurchaseOrderStatus.APPROVED, 50, 0).get("result"));
    }

    @Test
    public void blockedWhenOtherPosOrderedEverything() throws Exception {
        assertEquals("error", check(PurchaseOrderStatus.CONFIRMED, 10, 50).get("result"));
    }

    @Test
    public void allowedWhileSomethingIsLeft() throws Exception {
        assertEquals("success", check(PurchaseOrderStatus.CONFIRMED, 20, 30).get("result"));
    }

    @Test
    public void orderCannotExceedTheApprovedQuantity() throws Exception {
        assertEquals("error", check(PurchaseOrderStatus.CONFIRMED, 51, 0).get("result"));
    }
}
