package ph.com.guanzongroup.cas.purchasing.validator;

import static ph.com.guanzongroup.cas.purchasing.validator.FakeResultSet.row;
import static ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrderTestData.driver;
import static ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrderTestData.master;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.junit.Test;

/**
 * General Inventory rule: the engineering project title (PurchaseOrder_General).
 */
public class PurchaseOrderGeneralValidatorTest {

    private static final String ENGINEERING = "ENG";

    /** General validator for the engineering department, logged in as fsDepartment. */
    private static PurchaseOrder_General validator(String fsDepartment, Model_PO_Master foMaster, Integer fnProjectUsage) {
        ResultSet loUsage = fnProjectUsage == null ? null : FakeResultSet.of(row("nCount", fnProjectUsage));
        GRiderCAS loDriver = driver(fsDepartment, loUsage);

        PurchaseOrder_General loValidator = new PurchaseOrder_General() {
            @Override
            protected String getProjectTitleDepartment() {
                return ENGINEERING;
            }
        };
        loValidator.setApplicationDriver(loDriver);
        loValidator.setMaster(foMaster);
        return loValidator;
    }

    @Test
    public void projectTitle_onlyForEngineeringDepartment() throws Exception {
        assertFalse(validator("ACC", master().build(), 1)
                .isProjectTitleUpdateRequired(PurchaseOrderStatus.OPEN, false));
        assertTrue(validator(ENGINEERING, master().build(), 1)
                .isProjectTitleUpdateRequired(PurchaseOrderStatus.OPEN, false));
    }

    @Test
    public void projectTitle_needsReference() throws Exception {
        assertFalse(validator(ENGINEERING, master().reference("").build(), 1)
                .isProjectTitleUpdateRequired(PurchaseOrderStatus.OPEN, false));
        assertFalse(validator(ENGINEERING, master().reference(null).build(), 1)
                .isProjectTitleUpdateRequired(PurchaseOrderStatus.OPEN, false));
    }

    @Test
    public void projectTitle_onlyOnOpenCancelVoid() throws Exception {
        PurchaseOrder_General loValidator = validator(ENGINEERING, master().build(), 1);
        assertTrue(loValidator.isProjectTitleUpdateRequired(PurchaseOrderStatus.OPEN, false));
        assertTrue(loValidator.isProjectTitleUpdateRequired(PurchaseOrderStatus.CANCELLED, false));
        assertTrue(loValidator.isProjectTitleUpdateRequired(PurchaseOrderStatus.VOID, false));
        assertFalse(loValidator.isProjectTitleUpdateRequired(PurchaseOrderStatus.CONFIRMED, false));
        assertFalse(loValidator.isProjectTitleUpdateRequired(PurchaseOrderStatus.APPROVED, false));
    }

    @Test
    public void projectTitle_notUntaggedWhileAnotherPurchaseOrderUsesIt() throws Exception {
        assertTrue("used by this PO only",
                validator(ENGINEERING, master().build(), 1).isProjectTitleUpdateRequired(PurchaseOrderStatus.CANCELLED, true));
        assertFalse("used by another PO too",
                validator(ENGINEERING, master().build(), 2).isProjectTitleUpdateRequired(PurchaseOrderStatus.CANCELLED, true));
    }

    @Test
    public void projectUsageCount_readsTheCount() throws Exception {
        assertEquals(3, validator(ENGINEERING, master().build(), 3).getProjectUsageCount("REF-001"));
    }

    @Test
    public void generalValidator_keepsTheSharedRules() throws Exception {
        PurchaseOrder_General loValidator = validator(ENGINEERING, master().supplier("").build(), null);
        assertEquals("error", loValidator.commonNewRules().get("result"));
    }
}
