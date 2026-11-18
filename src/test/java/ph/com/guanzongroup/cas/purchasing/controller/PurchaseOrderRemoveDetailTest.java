package ph.com.guanzongroup.cas.purchasing.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrderValidator;
import ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrder_General;

/** RemoveDetail: saved rows go to the removed list, new rows are just dropped. */
public class PurchaseOrderRemoveDetailTest {

    /** Smallest concrete controller; only the detail list is used. */
    private static class TestPO extends PurchaseOrder {

        @Override
        protected PurchaseOrderValidator createValidator() {
            return new PurchaseOrder_General();
        }

        @Override
        public void initSQL() {
        }

        @Override
        public JSONObject willSave() {
            return successJSON();
        }

        @Override
        public JSONObject saveOthers() {
            return successJSON();
        }

        void setMode(int fnMode) {
            pnEditMode = fnMode;
        }
    }

    private TestPO po;
    private Model_PO_Detail savedRow;
    private Model_PO_Detail newRow1;
    private Model_PO_Detail newRow2;

    private static Model_PO_Detail row(int fnEditMode) {
        Model_PO_Detail loRow = mock(Model_PO_Detail.class);
        when(loRow.getEditMode()).thenReturn(fnEditMode);
        return loRow;
    }

    @Before
    public void setUp() {
        po = new TestPO();
        savedRow = row(EditMode.UPDATE);
        newRow1 = row(EditMode.ADDNEW);
        newRow2 = row(EditMode.ADDNEW);
        po.paDetail = new ArrayList<Model>();
        po.paDetail.add(savedRow);
        po.paDetail.add(newRow1);
        po.paDetail.add(newRow2);
        po.setMode(EditMode.UPDATE);
    }

    @Test
    public void savedRowGoesToRemovedList() throws Exception {
        assertEquals("success", po.RemoveDetail(0).get("result"));
        assertEquals(2, po.getDetailCount());
        assertEquals(1, po.getDetailRemovedCount());
        assertSame(savedRow, po.DetailRemove(0));
    }

    @Test
    public void newRowIsOnlyDropped() throws Exception {
        assertEquals("success", po.RemoveDetail(1).get("result"));
        assertEquals(2, po.getDetailCount());
        assertEquals(0, po.getDetailRemovedCount());
    }

    @Test
    public void invalidRowIsRejected() throws Exception {
        assertEquals("error", po.RemoveDetail(3).get("result"));
        assertEquals("error", po.RemoveDetail(-1).get("result"));
        assertEquals(3, po.getDetailCount());
    }

    @Test
    public void onlyWhileAddingOrEditing() throws Exception {
        po.setMode(EditMode.READY);
        assertEquals("error", po.RemoveDetail(0).get("result"));
        assertEquals(3, po.getDetailCount());
    }
}
