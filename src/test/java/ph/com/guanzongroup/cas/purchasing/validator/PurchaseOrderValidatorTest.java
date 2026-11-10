package ph.com.guanzongroup.cas.purchasing.validator;

import static ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrderTestData.TODAY;
import static ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrderTestData.detail;
import static ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrderTestData.driver;
import static ph.com.guanzongroup.cas.purchasing.validator.PurchaseOrderTestData.master;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.json.simple.JSONObject;
import org.junit.Test;

/**
 * Shared Purchase Order rules (PurchaseOrderValidator). Every industry
 * validator inherits these, so they must pass for all industries.
 */
public class PurchaseOrderValidatorTest {

    // =====================================================================
    // Setup helpers
    // =====================================================================
    /** Base validator with only the shared rules. */
    private static PurchaseOrderValidator validator(Model_PO_Master foMaster, Model_PO_Detail... faDetails) {
        PurchaseOrderValidator loValidator = new PurchaseOrderValidator() {
        };
        return setUp(loValidator, foMaster, faDetails);
    }

    static <T extends PurchaseOrderValidator> T setUp(T foValidator, Model_PO_Master foMaster, Model_PO_Detail... faDetails) {
        foValidator.setApplicationDriver(driver("D001"));
        foValidator.setMaster(foMaster);
        foValidator.setDetail(new ArrayList<Object>(Arrays.asList(faDetails)));
        foValidator.setTransactionStatus(PurchaseOrderStatus.OPEN);
        return foValidator;
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

    // =====================================================================
    // Saving an OPEN purchase order
    // =====================================================================
    @Test
    public void newRules_validPurchaseOrder_passes() throws Exception {
        assertOk(validator(master().build()).commonNewRules());
    }

    @Test
    public void newRules_supplierRequired() throws Exception {
        assertError("Invalid Supplier", validator(master().supplier("").build()).commonNewRules());
        assertError("Invalid Supplier", validator(master().supplier(null).build()).commonNewRules());
    }

    @Test
    public void newRules_transactionDateRequired() throws Exception {
        assertError("Invalid Transaction Date", validator(master().transact(null).build()).commonNewRules());
    }

    @Test
    public void newRules_futureDateNotAllowed() throws Exception {
        assertError("Future transaction dates",
                validator(master().transact(TODAY.plusDays(1)).expected(TODAY.plusDays(2)).build()).commonNewRules());
    }

    @Test
    public void newRules_backdateLimitIsOneYear() throws Exception {
        assertError("beyond 1 year",
                validator(master().transact(TODAY.minusYears(1).minusDays(1)).build()).commonNewRules());
        assertOk(validator(master().transact(TODAY.minusYears(1)).build()).commonNewRules());
    }

    @Test
    public void newRules_destinationRequired() throws Exception {
        assertError("Destination is not set", validator(master().destination(" ").build()).commonNewRules());
    }

    @Test
    public void newRules_expectedDateRules() throws Exception {
        assertError("Invalid Expected Delivery Date", validator(master().expected(null).build()).commonNewRules());
        assertError("cannot be earlier than the transaction date",
                validator(master().expected(TODAY.minusDays(1)).build()).commonNewRules());
    }

    @Test
    public void newRules_termRequired() throws Exception {
        assertError("Invalid Term", validator(master().term("").build()).commonNewRules());
    }

    @Test
    public void newRules_advancePaymentNeedsRateOrAmount() throws Exception {
        assertError("no Adv Rate and Adv Amount", validator(master().advance(0, 0).build()).commonNewRules());
        assertOk(validator(master().advance(10, 0).build()).commonNewRules());
        assertOk(validator(master().advance(0, 100).build()).commonNewRules());
    }

    @Test
    public void newRules_newBackdatedPurchaseOrderNeedsReference() throws Exception {
        assertError("reference number is required",
                validator(master().addNew().transact(TODAY.minusDays(3)).reference("").build()).commonNewRules());
        assertError("reference number is required",
                validator(master().addNew().transact(TODAY.minusDays(3)).reference(null).build()).commonNewRules());
        assertOk(validator(master().addNew().transact(TODAY.minusDays(3)).reference("REF-9").build()).commonNewRules());
    }

    @Test
    public void newRules_industryCanChangeBackdateLimit() throws Exception {
        PurchaseOrderValidator loTwoYears = setUp(new PurchaseOrderValidator() {
            @Override
            protected int getMaxBackdateYears() {
                return 2;
            }
        }, master().transact(TODAY.minusMonths(18)).build());
        assertOk(loTwoYears.commonNewRules());
    }

    // =====================================================================
    // Confirm / return
    // =====================================================================
    @Test
    public void confirmRules_attachmentRequiredAbove100k() throws Exception {
        PurchaseOrderValidator loValidator = validator(master().tranTotal(150000).build());
        loValidator.setActiveAttachmentCount(0);
        assertError("attachment cannot be empty", loValidator.commonConfirmedRules());

        loValidator.setActiveAttachmentCount(1);
        assertOk(loValidator.commonConfirmedRules());
    }

    @Test
    public void confirmRules_noAttachmentNeededAtOrBelow100k() throws Exception {
        PurchaseOrderValidator loValidator = validator(master().tranTotal(100000).build());
        loValidator.setActiveAttachmentCount(0);
        assertOk(loValidator.commonConfirmedRules());
    }

    @Test
    public void returnRules_cannotReturnTwice() throws Exception {
        assertError("already returned",
                validator(master().status(PurchaseOrderStatus.RETURNED).build()).commonReturnedRules());
        assertOk(validator(master().status(PurchaseOrderStatus.CONFIRMED).build()).commonReturnedRules());
    }

    // =====================================================================
    // Discounts, advance payment, totals
    // =====================================================================
    @Test
    public void discountRate_needsDetailAmountAndRange() {
        assertError("no detail amount", validator(master().tranTotal(0).build()).validateDiscountRate(10));
        assertError("Must be between 0.00 and 100.00", validator(master().build()).validateDiscountRate(100.01));
        assertError("Must be between 0.00 and 100.00", validator(master().build()).validateDiscountRate(-1));
        assertOk(validator(master().build()).validateDiscountRate(12.5));
    }

    @Test
    public void discountAmount_limits() {
        PurchaseOrderValidator loValidator = validator(master().tranTotal(5000).discount(10).build());
        assertError("must not exceed", validator(master().tranTotal(5000000).build()).validateDiscountAmount(1000000));
        assertError("Invalid Discount Amount", loValidator.validateDiscountAmount(4500.01)); //total after 10% = 4500
        assertError("Invalid Discount Amount", loValidator.validateDiscountAmount(-1));
        assertOk(loValidator.validateDiscountAmount(4500));
    }

    @Test
    public void discountAmount_exactCentsAreAllowed() {
        //1000.10 less 10% = 900.09 exactly (plain double math gives 900.0899999...)
        PurchaseOrderValidator loValidator = validator(master().tranTotal(1000.10).discount(10).build());
        assertOk(loValidator.validateDiscountAmount(900.09));
        assertError("Invalid Discount Amount", loValidator.validateDiscountAmount(900.10));
    }

    @Test
    public void advance_exactCentsAreAllowed() {
        //after discounts: 0.30 - 0.10 = 0.20; 50% = 0.10 + 0.10 fixed = 0.20
        PurchaseOrderValidator loValidator = validator(master().tranTotal(0.30).addDiscount(0.10).advance(0, 0.10).build());
        assertOk(loValidator.validateAdvancePaymentRate(50));
    }

    @Test
    public void discountAmount_needsAmountAfterRate() {
        assertError("no amount entered", validator(master().tranTotal(1000).discount(100).build()).validateDiscountAmount(1));
    }

    @Test
    public void advanceRate_limits() {
        //amount after discounts: 10000 - 10% - 1000 = 8000
        PurchaseOrderValidator loValidator = validator(master().tranTotal(10000).discount(10).addDiscount(1000)
                .advance(0, 2000).build());
        assertOk(loValidator.validateAdvancePaymentRate(75));          //6000 + 2000 = 8000
        assertError("Invalid Downpayment Total", loValidator.validateAdvancePaymentRate(76)); //6080 + 2000 > 8000
        assertError("Must be between 0.0000 and 100.0000", loValidator.validateAdvancePaymentRate(101));
        assertError("total transaction amount is 0.0000",
                validator(master().tranTotal(0).build()).validateAdvancePaymentRate(10));
    }

    @Test
    public void advanceAmount_limits() {
        //amount after discounts: 8000; rate 50% = 4000
        PurchaseOrderValidator loValidator = validator(master().tranTotal(10000).discount(10).addDiscount(1000)
                .advance(50, 0).build());
        assertOk(loValidator.validateAdvancePaymentAmount(4000));
        assertError("Invalid Downpayment Total", loValidator.validateAdvancePaymentAmount(4000.01));
        assertError("Invalid Advance Payment Amount", loValidator.validateAdvancePaymentAmount(8000.01));
    }

    @Test
    public void netTotal_cannotBeNegative() {
        assertError("Invalid Total Transaction Amount", validator(master().netTotal(-0.01).build()).validateNetTotal());
        assertOk(validator(master().netTotal(0).build()).validateNetTotal());
    }

    @Test
    public void netTotal_capIs100Million() {
        PurchaseOrderValidator loValidator = validator(master().netTotal(99999000).build());
        assertOk(loValidator.validateNetTotalWithLine(999.99));
        assertError("exceeds the maximum", loValidator.validateNetTotalWithLine(1000));
    }

    // =====================================================================
    // Detail grid
    // =====================================================================
    @Test
    public void addDetail_lastRowMustHaveItem() {
        assertError("Last row has empty item",
                validator(master().build(), detail("STK1", 1), detail("", 0)).validateCanAddDetail());
        assertOk(validator(master().build(), detail("STK1", 1)).validateCanAddDetail());
        assertOk(validator(master().build()).validateCanAddDetail());
    }

    @Test
    public void uniqueItem_manualLinesOnly() {
        PurchaseOrderValidator loValidator = validator(master().build(),
                detail("STK1", 1), detail("STK2", 1, "SR-0001"), detail("", 0));

        JSONObject loResult = loValidator.validateUniqueItem(2, "STK1", "Barcode: ABC");
        assertError("already exist in table at row 1", loResult);
        assertEquals(0, loResult.get("tableRow"));

        //same item pulled from a stock request may appear again
        assertOk(loValidator.validateUniqueItem(2, "STK2", "Barcode: DEF"));
        //the row being edited does not count
        assertOk(loValidator.validateUniqueItem(0, "STK1", "Barcode: ABC"));
    }

    @Test
    public void quantities_allZero_isBlockingWarning() {
        JSONObject loResult = validator(master().build(), detail("STK1", 0), detail("STK2", 0)).validateQuantities();
        assertError("All items have zero quantity", loResult);
        assertEquals("true", loResult.get("warning"));
        assertEquals(0, loResult.get("tableRow"));
    }

    @Test
    public void quantities_someZero_isWarning() {
        JSONObject loResult = validator(master().build(), detail("STK1", 2), detail("STK2", 0)).validateQuantities();
        assertError("Some items have zero quantity", loResult);
        assertEquals("false", loResult.get("warning"));
        assertEquals(1, loResult.get("tableRow"));
    }

    @Test
    public void quantities_emptyRowsIgnored() {
        assertOk(validator(master().build(), detail("STK1", 2), detail("", 0)).validateQuantities());
    }

    // =====================================================================
    // Saving a returned purchase order
    // =====================================================================
    @Test
    public void returnedEdit_requiresAChange() {
        Model_PO_Master loSaved = master().status(PurchaseOrderStatus.RETURNED).build();
        List<Model_PO_Detail> laSaved = Arrays.asList(detail("STK1", 5));

        assertError("No update has been made",
                validator(master().status(PurchaseOrderStatus.RETURNED).build(), detail("STK1", 5))
                        .validateReturnedEdit(loSaved, laSaved));
    }

    @Test
    public void returnedEditLines_comparesSavedLinesFromTheDatabase() {
        Model_PO_Master loSaved = master().status(PurchaseOrderStatus.RETURNED).build();
        List<PurchaseOrderValidator.SavedLine> laSaved = Arrays.asList(
                new PurchaseOrderValidator.SavedLine("STK1", 5), new PurchaseOrderValidator.SavedLine("STK2", 1));

        assertError("No update has been made",
                validator(master().build(), detail("STK1", 5), detail("STK2", 1)).validateReturnedEditLines(loSaved, laSaved));
        assertOk(validator(master().build(), detail("STK1", 5), detail("STK2", 2)).validateReturnedEditLines(loSaved, laSaved));
        assertOk(validator(master().build(), detail("STK1", 5)).validateReturnedEditLines(loSaved, laSaved));
    }

    @Test
    public void returnedEdit_detectsChanges() {
        Model_PO_Master loSaved = master().status(PurchaseOrderStatus.RETURNED).build();
        List<Model_PO_Detail> laSaved = Arrays.asList(detail("STK1", 5));

        //quantity changed
        assertOk(validator(master().build(), detail("STK1", 6)).validateReturnedEdit(loSaved, laSaved));
        //item changed
        assertOk(validator(master().build(), detail("STK2", 5)).validateReturnedEdit(loSaved, laSaved));
        //line added
        assertOk(validator(master().build(), detail("STK1", 5), detail("STK3", 1)).validateReturnedEdit(loSaved, laSaved));
        //remarks changed (null-safe)
        assertOk(validator(master().remarks(null).build(), detail("STK1", 5)).validateReturnedEdit(loSaved, laSaved));
        //term changed
        assertOk(validator(master().term("T060").build(), detail("STK1", 5)).validateReturnedEdit(loSaved, laSaved));
    }

    // =====================================================================
    // Approval matrix decision
    // =====================================================================
    private static PurchaseOrderValidator.QuotationVariance variance(int records, int unquoted, double qtyPct, double pricePct) {
        PurchaseOrderValidator.QuotationVariance loVariance = new PurchaseOrderValidator.QuotationVariance();
        loVariance.nRecords = records;
        loVariance.nUnquoted = unquoted;
        loVariance.nMaxQtyDiffPct = qtyPct;
        loVariance.nMaxPriceDiffPct = pricePct;
        return loVariance;
    }

    @Test
    public void approval_matchingQuotation_needsNoAuthorization() {
        assertFalse(validator(master().build()).isApprovalAuthRequired(variance(3, 0, 0, 0)));
    }

    @Test
    public void approval_unquotedOrDifferentLines_needAuthorization() {
        PurchaseOrderValidator loValidator = validator(master().build());
        assertTrue("a line without quotation", loValidator.isApprovalAuthRequired(variance(3, 1, 0, 0)));
        assertTrue("no lines at all", loValidator.isApprovalAuthRequired(variance(0, 0, 0, 0)));
        assertTrue("price differs", loValidator.isApprovalAuthRequired(variance(3, 0, 0, 5)));
        assertTrue("quantity differs", loValidator.isApprovalAuthRequired(variance(3, 0, 0.5, 0)));
    }

    @Test
    public void approval_industryCanAllowPriceTolerance() {
        PurchaseOrderValidator loTolerant = setUp(new PurchaseOrderValidator() {
            @Override
            protected double getPriceTolerancePct() {
                return 10;
            }
        }, master().build());
        assertFalse(loTolerant.isApprovalAuthRequired(variance(3, 0, 0, 10)));
        assertTrue(loTolerant.isApprovalAuthRequired(variance(3, 0, 0, 10.01)));
    }

    // =====================================================================
    // Validator wiring
    // =====================================================================
    @Test
    public void validate_withoutSetup_returnsErrorNotNull() {
        JSONObject loResult = new PurchaseOrderValidator() {
        }.validate();
        assertError("not initialized", loResult);
    }
}
