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
import org.json.simple.JSONObject;
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

    // =====================================================================
    // Advance payment (General Inventory only)
    // =====================================================================
    /** General validator with a plain driver (no project lookups). */
    private static PurchaseOrder_General general(Model_PO_Master foMaster) {
        return PurchaseOrderValidatorTest.setUp(new PurchaseOrder_General(), foMaster);
    }

    private static void assertOk(JSONObject foResult) {
        assertEquals("expected success but got: " + foResult, "success", foResult.get("result"));
    }

    private static void assertError(String fsMessagePart, JSONObject foResult) {
        assertEquals("expected an error but got: " + foResult, "error", foResult.get("result"));
        String lsMessage = String.valueOf(foResult.get("message"));
        assertTrue("message \"" + lsMessage + "\" should contain \"" + fsMessagePart + "\"",
                lsMessage.contains(fsMessagePart));
    }

    @Test
    public void newRules_advancePaymentNeedsRateOrAmount() throws Exception {
        assertError("no Adv Rate and Adv Amount", general(master().advance(0, 0).build()).industryNewRules());
        assertOk(general(master().advance(10, 0).build()).industryNewRules());
        assertOk(general(master().advance(0, 100).build()).industryNewRules());
    }

    @Test
    public void advance_exactCentsAreAllowed() {
        //after discounts: 0.30 - 0.10 = 0.20; 50% = 0.10 + 0.10 fixed = 0.20
        PurchaseOrder_General loValidator = general(master().tranTotal(0.30).addDiscount(0.10).advance(0, 0.10).build());
        assertOk(loValidator.validateAdvancePaymentRate(50));
    }

    @Test
    public void advanceRate_limits() {
        //amount after discounts: 10000 - 10% - 1000 = 8000
        PurchaseOrder_General loValidator = general(master().tranTotal(10000).discount(10).addDiscount(1000)
                .advance(0, 2000).build());
        assertOk(loValidator.validateAdvancePaymentRate(75));          //6000 + 2000 = 8000
        assertError("Invalid Downpayment Total", loValidator.validateAdvancePaymentRate(76)); //6080 + 2000 > 8000
        assertError("Must be between 0.0000 and 100.0000", loValidator.validateAdvancePaymentRate(101));
        assertError("total transaction amount is 0.0000",
                general(master().tranTotal(0).build()).validateAdvancePaymentRate(10));
    }

    @Test
    public void advanceAmount_limits() {
        //amount after discounts: 8000; rate 50% = 4000
        PurchaseOrder_General loValidator = general(master().tranTotal(10000).discount(10).addDiscount(1000)
                .advance(50, 0).build());
        assertOk(loValidator.validateAdvancePaymentAmount(4000));
        assertError("Invalid Downpayment Total", loValidator.validateAdvancePaymentAmount(4000.01));
        assertError("Invalid Advance Payment Amount", loValidator.validateAdvancePaymentAmount(8000.01));
    }
}
