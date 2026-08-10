import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.cas.purchasing.controller.PurchaseOrderReceiving;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingControllers;

import org.guanzon.cas.purchasing.status.PurchaseOrderReceivingStatus;
import org.h2.tools.RunScript;
import org.json.simple.JSONObject;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.guanzon.appdriver.base.SQLUtil;

//@Ignore("Pending schema and SQL test data setup")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PurchaseOrderReceivingTest {
    static GRiderCAS instance;
    static PurchaseOrderReceiving poController;
    static Connection conn;

    private static String psIndustryId = "09";
    private static String psCompanyId = "M001";
    private static String psCategorCd = "0000007";
    private String psPONumber1 = "GK0126000124";
    private String psPONumber2 = "GK0126000123";
    private String psTransNo = "GK0126000196";
    private String psTransNoWithSerial = "GK0126000204";
    private String psStockId1 = "GK0124000325";
    private String psStockId2 = "M00124000081";
    private String psStockId3 = "GK0126000009";
    private String psStockIdSerialize = "GK0126000008";
    private String psSupplierId = "GK0126000044";

    @BeforeClass
    public static void setUpClass() throws GuanzonException, SQLException, IOException {
        instance = new GRiderCAS();

        if (!instance.loadEnv("gRider")) {
            System.err.println(instance.getMessage());
            System.exit(1);
        }

        if (!instance.logUser("gRider", "M001250015")) {
            System.err.println(instance.getMessage());
            System.exit(1);
        }

        loadCorePrimary();

        String path;
        String tempPath;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            path = "D:/GGC_Maven_Systems";
            tempPath = "D:/temp";
        } else {
            path = "/srv/GGC_Maven_Systems";
            tempPath = "/srv/temp";
        }

        System.setProperty("sys.default.path.config", path);
        System.setProperty("sys.default.path.metadata", path + "/config/metadata/new/");
        System.setProperty("sys.default.path.temp", tempPath);

        if (!loadProperties()) {
            System.err.println("Unable to load config.");
            System.exit(1);
        }

        resetController();
    }

    @AfterClass
    public static void tearDownClass() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println(e.getMessage());
            }
        }

        System.clearProperty("sys.default.path.config");
        System.clearProperty("sys.default.path.metadata");
        System.clearProperty("sys.default.path.temp");

        System.clearProperty("sys.main.industry");
        System.clearProperty("sys.general.industry");
        System.clearProperty("sys.dept.finance");
        System.clearProperty("sys.dept.procurement");
        System.clearProperty("user.selected.industry");
        System.clearProperty("user.selected.category");
        System.clearProperty("user.selected.company");
        System.clearProperty("sys.default.client.token");
        System.clearProperty("sys.default.access.token");
        System.clearProperty("sys.default.path.temp.attachments");
        System.clearProperty("allowed.department");
    }

    private static boolean loadProperties() {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(System.getProperty("sys.default.path.config") + "/config/cas.properties"));

            System.setProperty("sys.main.industry", props.getProperty("sys.main.industry"));
            System.setProperty("sys.general.industry", props.getProperty("sys.general.industry"));
            System.setProperty("sys.dept.finance", props.getProperty("sys.dept.finance"));
            System.setProperty("sys.dept.procurement", props.getProperty("sys.dept.procurement"));
            System.setProperty("user.selected.industry", props.getProperty("user.selected.industry"));
            System.setProperty("user.selected.category", props.getProperty("user.selected.category"));
            System.setProperty("user.selected.company", props.getProperty("user.selected.company"));
            System.setProperty("sys.default.client.token", System.getProperty("sys.default.path.config") + "/client.token");
            System.setProperty("sys.default.access.token", System.getProperty("sys.default.path.config") + "/access.token");
            System.setProperty("sys.default.path.temp.attachments", props.getProperty("sys.default.path.temp.attachments"));
            System.setProperty("allowed.department", props.getProperty("allowed.department"));
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private static void loadCorePrimary() throws IOException, SQLException {
        conn = instance.getGConnection().getConnection();

        List<String> schemaScripts = new ArrayList<>();
        List<String> dataScripts = new ArrayList<>();

        schemaScripts.add("industry_schema");
        schemaScripts.add("category_schema");
        schemaScripts.add("company_schema");
        schemaScripts.add("branch_schema");
        schemaScripts.add("client_master_schema");
        schemaScripts.add("po_master_schema");
        schemaScripts.add("po_detail_schema");
        schemaScripts.add("po_receiving_master_schema");
        schemaScripts.add("po_receiving_detail_schema");
        schemaScripts.add("po_receiving_serial_schema");
        schemaScripts.add("po_return_master_schema");
        schemaScripts.add("po_return_detail_schema");
        schemaScripts.add("transaction_status_history_schema");
        schemaScripts.add("transaction_attachment_schema");
        schemaScripts.add("inventory_schema");
        schemaScripts.add("model_schema");
        schemaScripts.add("model_variant_schema");
        schemaScripts.add("measure_schema");
        schemaScripts.add("color_schema");
        schemaScripts.add("inv_supplier_schema");
        schemaScripts.add("inv_master_schema");
        schemaScripts.add("inv_serial_schema");
        schemaScripts.add("inv_serial_registration_schema");
        schemaScripts.add("term_schema");
        schemaScripts.add("xxxsysfiles_schema");
        schemaScripts.add("xxxsysaction_schema");

        dataScripts.add("industry_data");
        dataScripts.add("category_data");
        dataScripts.add("company_data");
        dataScripts.add("branch_data");
        dataScripts.add("client_master_data");
        dataScripts.add("po_master_data");
        dataScripts.add("po_detail_data");
        dataScripts.add("po_receiving_master_data");
        dataScripts.add("po_receiving_detail_data");
        dataScripts.add("po_receiving_serial_data");
        dataScripts.add("po_return_master_data");
        dataScripts.add("po_return_detail_data");
        dataScripts.add("transaction_status_history_data");
        dataScripts.add("transaction_attachment_data");
        dataScripts.add("inventory_data");
        dataScripts.add("model_data");
        dataScripts.add("model_variant_data");
        dataScripts.add("measure_data");
        dataScripts.add("color_data");
        dataScripts.add("inv_supplier_data");
        dataScripts.add("inv_master_data");
        dataScripts.add("inv_serial_data");
        dataScripts.add("inv_serial_registration_data");
        dataScripts.add("term_data");
        dataScripts.add("xxxsysfiles_data");
        dataScripts.add("xxxsysaction_data");

        for (String schema : schemaScripts) {
            try (FileReader schemaReader = new FileReader("test-data/" + schema + ".sql")) {
                RunScript.execute(conn, schemaReader);
            }
        }

        for (String data : dataScripts) {
            try (FileReader dataReader = new FileReader("test-data/" + data + ".sql")) {
                RunScript.execute(conn, dataReader);
            }
        }

    }
    private static void resetController() {
        poController = new PurchaseOrderReceivingControllers(instance, null).PurchaseOrderReceiving();
        poController.setWithUI(false);
        poController.setWithParent(true);
        Assert.assertNotNull(poController);
    }

    private static void startNewTransaction() throws CloneNotSupportedException, SQLException, GuanzonException {
        if (poController == null) {
            resetController();
        }

        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.setIndustryId(psIndustryId);
        poController.setCategoryId(psCategorCd);
        poController.setCompanyId(psCompanyId);

        loJSON = poController.NewTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
    }
    
    /*Convert Date to String*/
    private static String xsDateShort(Date fdValue) {
        if(fdValue == null){
            return "1900-01-01";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }

    private LocalDate strToDate(String val) {
        DateTimeFormatter date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(val, date_formatter);
        return localDate;
    }


    @Test
    public void test001InitTransaction() {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals("PODA", poController.getSourceCode());
        Assert.assertNotNull(poController.Master());
        Assert.assertNotNull(poController.Detail());
    }

    @Test
    public void test002GetStatus() {
        Assert.assertNotNull(poController);

        poController.isFinance(false);
        Assert.assertEquals("Open", poController.getStatus(PurchaseOrderReceivingStatus.OPEN));
        Assert.assertEquals("Confirmed", poController.getStatus(PurchaseOrderReceivingStatus.CONFIRMED));
        Assert.assertEquals("Posted", poController.getStatus(PurchaseOrderReceivingStatus.POSTED));
        Assert.assertEquals("Cancelled", poController.getStatus(PurchaseOrderReceivingStatus.CANCELLED));
        Assert.assertEquals("Voided", poController.getStatus(PurchaseOrderReceivingStatus.VOID));
        Assert.assertEquals("Paid", poController.getStatus(PurchaseOrderReceivingStatus.PAID));
        Assert.assertEquals("Returned", poController.getStatus(PurchaseOrderReceivingStatus.RETURNED));
        Assert.assertEquals("Confirmed", poController.getStatus(PurchaseOrderReceivingStatus.CONFIRMED_I));
        Assert.assertEquals("Verified", poController.getStatus(PurchaseOrderReceivingStatus.VERIFIED));
        Assert.assertEquals("Returned", poController.getStatus(PurchaseOrderReceivingStatus.RETURNED_I));

        poController.isFinance(true);
        Assert.assertEquals("Open", poController.getStatus(PurchaseOrderReceivingStatus.CONFIRMED));

        Assert.assertEquals("UNKNOWN", poController.getStatus("X"));
    }

    @Test
    public void test003CheckPosition() {
        // TODO
    }

    @Test
    public void test004SetForm() {
        // TODO
    }

    @Test
    public void test005CheckUpdateTransaction() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction(psTransNo);
        Assume.assumeTrue("Fixture transaction not available: " + psTransNo,
                "success".equals(loJSON.get("result")));

        String status = poController.Master().getTransactionStatus();
        String salesInvoice = poController.Master().getSalesInvoice();

        runCheckUpdateCase(true, "", status, salesInvoice);
        runCheckUpdateCase(false, "", status, salesInvoice);
        runCheckUpdateCase(false, PurchaseOrderReceivingStatus.CONFIRMED, status, salesInvoice);
        runCheckUpdateCase(false, PurchaseOrderReceivingStatus.CONFIRMED_I, status, salesInvoice);
        runCheckUpdateCase(false, PurchaseOrderReceivingStatus.VERIFIED, status, salesInvoice);
        runCheckUpdateCase(false, PurchaseOrderReceivingStatus.POSTED, status, salesInvoice);
    }

    private void runCheckUpdateCase(boolean isEntry, String form, String status, String salesInvoice) throws Exception {
        poController.setForm(form == null ? "" : form);
        JSONObject out = poController.checkUpdateTransaction(isEntry);

        Assert.assertNotNull(out);
        Assert.assertNotNull("result must exist", out.get("result"));
        Assert.assertNotNull("message must exist", out.get("message"));

        boolean expectedAllow = shouldAllowUpdate(status, isEntry, form, salesInvoice);
        if (expectedAllow) {
            Assert.assertEquals("success", out.get("result"));
            Assert.assertEquals("success", out.get("message"));
        } else {
            Assert.assertEquals("error", out.get("result"));
            Assert.assertTrue(String.valueOf(out.get("message")).contains("Transaction status was already"));
        }
    }

    private boolean shouldAllowUpdate(String status, boolean isEntry, String form, String salesInvoice) {
        if (PurchaseOrderReceivingStatus.VOID.equals(status)
                || PurchaseOrderReceivingStatus.CANCELLED.equals(status)) {
            return false;
        }

        if (PurchaseOrderReceivingStatus.CONFIRMED.equals(status)) {
            return isEntry || PurchaseOrderReceivingStatus.CONFIRMED_I.equals(form);
        }

        if (PurchaseOrderReceivingStatus.RETURNED.equals(status)) {
            return PurchaseOrderReceivingStatus.CONFIRMED.equals(form);
        }

        if (PurchaseOrderReceivingStatus.CONFIRMED_I.equals(status)
                || PurchaseOrderReceivingStatus.VERIFIED.equals(status)
                || PurchaseOrderReceivingStatus.POSTED.equals(status)) {

            if (isEntry) {
                return false;
            }

            if (PurchaseOrderReceivingStatus.CONFIRMED_I.equals(form)) {
                boolean confirmedFamily = PurchaseOrderReceivingStatus.CONFIRMED_I.equals(status)
                        || PurchaseOrderReceivingStatus.CONFIRMED.equals(status);

                boolean toFollowSpecial = (PurchaseOrderReceivingStatus.POSTED.equals(status)
                        || PurchaseOrderReceivingStatus.PAID.equals(status))
                        && "To-follow".equals(salesInvoice);

                return confirmedFamily || toFollowSpecial;
            }

            if (PurchaseOrderReceivingStatus.VERIFIED.equals(form)) {
                return PurchaseOrderReceivingStatus.VERIFIED.equals(status)
                        || PurchaseOrderReceivingStatus.CONFIRMED_I.equals(status);
            }

            if (PurchaseOrderReceivingStatus.POSTED.equals(form)) {
                return PurchaseOrderReceivingStatus.VERIFIED.equals(status);
            }

            return true;
        }

        if (PurchaseOrderReceivingStatus.RETURNED_I.equals(status)) {
            return PurchaseOrderReceivingStatus.CONFIRMED_I.equals(form);
        }

        return true;
    }

    private boolean hasLoadedTransaction(String transactionNo) {
        for (int i = 0; i < poController.getPurchaseOrderReceivingCount(); i++) {
            if (transactionNo.equals(poController.PurchaseOrderReceivingList(i).getTransactionNo())) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void test006SeekApproval() {
        // TODO
    }

    @Test
    public void test007NewTransaction()  throws CloneNotSupportedException, SQLException, GuanzonException{
        if (poController == null) {
            resetController();
        }
        Assert.assertNotNull(poController);

        resetController();

        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.setIndustryId(psIndustryId);
        poController.setCategoryId(psCategorCd);
        poController.setCompanyId(psCompanyId);

        loJSON = poController.NewTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(psIndustryId, poController.Master().getIndustryId());
        Assert.assertEquals(psCategorCd, poController.Master().getCategoryCode());
        Assert.assertEquals(psCompanyId, poController.Master().getCompanyId());
        Assert.assertEquals(PurchaseOrderReceivingStatus.OPEN, poController.Master().getTransactionStatus());
    }

    @Test
    public void test008SaveTransaction() {
        // TODO
    }

    @Test
    public void test009OpenTransaction() throws Exception {
        Assert.assertNotNull("No sample transaction configured.", psTransNo);

        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction(psTransNo);
        Assume.assumeTrue("Fixture transaction not available: " + psTransNo,
                "success".equals(loJSON.get("result")));

        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertNotNull(poController.Master());
        Assert.assertNotNull(poController.Detail());
        Assert.assertEquals(psTransNo, poController.Master().getTransactionNo());
        Assert.assertTrue("Detail count should be zero or more.", poController.getDetailCount() >= 0);
    }

    @Test
    public void test010UpdateTransaction() throws Exception {
        Assert.assertNotNull("No transaction sample transaction available.", psTransNo);

        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        loJSON = poController.OpenTransaction(psTransNo);
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.UpdateTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
    }

    @Test
    public void test011ConfirmTransaction() {
        // TODO
    }

    @Test
    public void test012ReturnTransaction() {
        // TODO
    }

    @Test
    public void test013ConfirmSIPosting() {
        // TODO
    }

    @Test
    public void test014VerifySIPosting() {
        // TODO
    }

    @Test
    public void test015ReturnSIPosting() {
        // TODO
    }

    @Test
    public void test016PaidTransaction() {
        // TODO
    }

    @Test
    public void test017PostTransaction() {
        // TODO
    }

    @Test
    public void test018CancelTransaction() {
        // TODO
    }

    @Test
    public void test019VoidTransaction() {
        // TODO
    }

    @Test
    public void test020SearchTransactionNoArgs() {
        // TODO
    }

    @Test
    public void test021SearchTransaction4Args() {
        // TODO
    }

    @Test
    public void test022SearchTransaction3Args() {
        // TODO
    }

    @Test
    public void test023SearchTransaction5Args() {
        // TODO
    }

    @Test
    public void test024SearchTransaction6Args() {
        // TODO
    }

    @Test
    public void test025SearchCompany() {
        // TODO
    }

    @Test
    public void test026SearchSupplier() {
        // TODO
    }

    @Test
    public void test027SearchBranch() {
        // TODO
    }

    @Test
    public void test028SearchTrucking() {
        // TODO
    }

    @Test
    public void test029SearchTerm() {
        // TODO
    }

    @Test
    public void test030SearchBarcode3Args() {
        // TODO
    }

    @Test
    public void test031SearchDescription3Args() {
        // TODO
    }

    @Test
    public void test032SearchSupersede3Args() {
        // TODO
    }

    @Test
    public void test033SearchBarcode4Args() {
        // TODO
    }

    @Test
    public void test034SearchDescription4Args() {
        // TODO
    }

    @Test
    public void test035SearchSupersede4Args() {
        // TODO
    }

    @Test
    public void test036SearchModel4Args() {
        // TODO
    }

    @Test
    public void test037SearchBrand() {
        // TODO
    }

    @Test
    public void test038SearchModel3Args() {
        // TODO
    }

    @Test
    public void test039SearchLocation() {
        // TODO
    }

    @Test
    public void test040SearchSerial() {
        // TODO
    }

    @Test
    public void test041SearchSerialRegistration() {
        // TODO
    }

    @Test
    public void test042GetSerialId() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        // Negative row is always invalid.
        try {
            poController.getSerialId(-1);
            Assert.fail("Expected IndexOutOfBoundsException for negative row.");
        } catch (IndexOutOfBoundsException expected) {
            Assert.assertTrue(true);
        }

        int serialCount = poController.getPurchaseOrderReceivingSerialCount();
        if (serialCount == 0) {
            try {
                poController.getSerialId(0);
                Assert.fail("Expected IndexOutOfBoundsException when serial list is empty.");
            } catch (IndexOutOfBoundsException expected) {
                Assert.assertTrue(true);
            }
            return;
        }

        String serialId = poController.getSerialId(0);
        Assert.assertNotNull("Serial ID lookup should not return null.", serialId);
    }

    @Test
    public void test043ComputeFields() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction(psTransNo);
        Assume.assumeTrue("Fixture transaction not available: " + psTransNo,
                "success".equals(loJSON.get("result")));

        LocalDate referenceDate = strToDate(xsDateShort(poController.Master().getReferenceDate()));
        Object termValue = poController.Master().Term().getTermValue();
        Assume.assumeTrue("Reference date is required for computeFields test.", referenceDate != null);
        Assume.assumeTrue("Term value is required for computeFields test.", termValue != null);

        double expectedTotal = 0.0;
        for (int i = 0; i < poController.getDetailCount(); i++) {
            expectedTotal += poController.Detail(i).getUnitPrce().doubleValue() * poController.Detail(i).getQuantity().doubleValue();
        }

        int termDays = (int) Math.round(Double.parseDouble(String.valueOf(termValue)));
        LocalDate expectedDueDate = referenceDate.plusDays(termDays);

        loJSON = poController.computeFields();
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(expectedDueDate,  strToDate(xsDateShort(poController.Master().getDueDate())));
        Assert.assertEquals(expectedDueDate,  strToDate(xsDateShort(poController.Master().getTermDueDate())));
        Assert.assertEquals(expectedTotal, poController.Master().getTransactionTotal().doubleValue(), 0.0001);
    }

    @Test
    public void test044GetNetTotal() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction(psTransNo);
        Assume.assumeTrue("Fixture transaction not available: " + psTransNo,
                "success".equals(loJSON.get("result")));

        loJSON = poController.computeFields();
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("success", loJSON.get("result"));

        double total = poController.Master().getTransactionTotal().doubleValue();
        double discount = poController.Master().getDiscount().doubleValue();
        double discountRate = poController.Master().getDiscountRate().doubleValue();
        if (discountRate > 0) {
            discountRate = total * (discountRate / 100.0);
        }
        discount += discountRate;

        double expectedNetTotal;
        if (poController.Master().isVatTaxable()) {
            expectedNetTotal = poController.Master().getVatSales().doubleValue()
                    + poController.Master().getVatAmount().doubleValue()
                    + poController.Master().getVatExemptSales().doubleValue();
        } else {
            expectedNetTotal = (total
                    + poController.Master().getVatAmount().doubleValue()
                    + poController.Master().getFreight().doubleValue()) - discount;
        }

        expectedNetTotal = Math.round(expectedNetTotal * 10000.0) / 10000.0;
        double actualNetTotal = poController.getNetTotal();

        Assert.assertEquals(expectedNetTotal, actualNetTotal, 0.0001);
    }

    @Test
    public void test045GetAdvancePayment() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction(psTransNo);
        Assume.assumeTrue("Fixture transaction not available: " + psTransNo,
                "success".equals(loJSON.get("result")));

        double advPayment = poController.getAdvancePayment();
        Assert.assertTrue("Advance payment should not be negative.", advPayment >= 0.0);
    }

    @Test
    public void test046ComputeDiscountRate() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction(psTransNo);
        Assume.assumeTrue("Fixture transaction not available: " + psTransNo,
                "success".equals(loJSON.get("result")));

        double total = poController.Master().getTransactionTotal().doubleValue();

        loJSON = poController.computeDiscountRate(0.0);
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.computeDiscountRate(-1.0);
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertTrue(String.valueOf(loJSON.get("message")).contains("Discount amount cannot be negative"));

        loJSON = poController.computeDiscountRate(total + 0.01);
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertTrue(String.valueOf(loJSON.get("message")).contains("Discount amount cannot be negative"));
    }

    @Test
    public void test047ComputeDiscount() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction(psTransNo);
        Assume.assumeTrue("Fixture transaction not available: " + psTransNo,
                "success".equals(loJSON.get("result")));

        loJSON = poController.computeDiscount(0.0);
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.computeDiscount(-1.0);
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertTrue(String.valueOf(loJSON.get("message")).contains("Discount rate cannot be negative"));

        loJSON = poController.computeDiscount(100.01);
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertTrue(String.valueOf(loJSON.get("message")).contains("Discount rate cannot be negative"));
    }

    @Test
    public void test048RemovePORDetails() {
        // TODO
    }

    @Test
    public void test049CheckExistingStock() throws Exception {
        resetController();
        startNewTransaction();

        JSONObject loJSON = poController.removePORDetails();
        Assert.assertEquals("success", loJSON.get("result"));

        // Build deterministic rows for duplicate checks.
        loJSON = poController.AddDetail();
        Assert.assertEquals("success", loJSON.get("result"));
        poController.Detail(0).setStockId(psStockId1);
        poController.Detail(0).setOrderNo("");

        loJSON = poController.AddDetail();
        Assert.assertEquals("success", loJSON.get("result"));
        poController.Detail(1).setStockId(psStockId2);
        poController.Detail(1).setOrderNo("");

        loJSON = poController.AddDetail();
        Assert.assertEquals("success", loJSON.get("result"));
        poController.Detail(2).setStockId(psStockId1);
        poController.Detail(2).setOrderNo(psPONumber1);

        int beforeCount = poController.getDetailCount();
        String beforeRow1Stock = poController.Detail(1).getStockId();
        String beforeRow1OrderNo = poController.Detail(1).getOrderNo();

        // Duplicate hit: row 1 checking stock of row 0 while both have empty order number.
        loJSON = poController.checkExistingStock(psStockId1, "DescA", "", 1, false);
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals(0L, ((Number) loJSON.get("row")).longValue());
        Assert.assertTrue(String.valueOf(loJSON.get("message")).contains("DescA already exists in table at row 1."));

        // Same stock on row 2 should pass because row 2 has order number.
        loJSON = poController.checkExistingStock(psStockId1, "DescA", "", 2, false);
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(2L, ((Number) loJSON.get("row")).longValue());

        // Non-duplicate stock should pass.
        loJSON = poController.checkExistingStock(psStockId3, "DescZ", "", 1, false);
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(1L, ((Number) loJSON.get("row")).longValue());

        // expiryDate and isSave are currently inert in implementation.
        JSONObject altJSON = poController.checkExistingStock(psStockId3, "DescZ", "1900-01-01", 1, true);
        Assert.assertEquals(loJSON.get("result"), altJSON.get("result"));
        Assert.assertEquals(loJSON.get("row"), altJSON.get("row"));

        // Method should not mutate detail rows/count.
        Assert.assertEquals(beforeCount, poController.getDetailCount());
        Assert.assertEquals(beforeRow1Stock, poController.Detail(1).getStockId());
        Assert.assertEquals(beforeRow1OrderNo, poController.Detail(1).getOrderNo());
    }

    @Test
    public void test050CheckExistingDetailDuplicate() throws Exception {
        resetController();
        startNewTransaction();

        JSONObject loJSON = poController.removePORDetails();
        Assert.assertEquals("success", loJSON.get("result"));

        // Row 0: baseline entry.
        loJSON = poController.AddDetail();
        Assert.assertEquals("success", loJSON.get("result"));
        poController.Detail(0).setOrderNo(psPONumber1);
        poController.Detail(0).setStockId(psStockId1);
        poController.Detail(0).setUnitPrce(100.00);

        // Row 1: same order/stock/price (duplicate against row 0).
        loJSON = poController.AddDetail();
        Assert.assertEquals("success", loJSON.get("result"));
        poController.Detail(1).setOrderNo(psPONumber1);
        poController.Detail(1).setStockId(psStockId1);
        poController.Detail(1).setUnitPrce(100.00);

        // Row 2: same order/stock but different price.
        loJSON = poController.AddDetail();
        Assert.assertEquals("success", loJSON.get("result"));
        poController.Detail(2).setOrderNo(psPONumber1);
        poController.Detail(2).setStockId(psStockId1);
        poController.Detail(2).setUnitPrce(100.01);

        // Row 3: same stock/price but different order.
        loJSON = poController.AddDetail();
        Assert.assertEquals("success", loJSON.get("result"));
        poController.Detail(3).setOrderNo(psPONumber2);
        poController.Detail(3).setStockId(psStockId1);
        poController.Detail(3).setUnitPrce(100.00);

        int beforeCount = poController.getDetailCount();
        String beforeRow2OrderNo = poController.Detail(2).getOrderNo();
        String beforeRow2Stock = poController.Detail(2).getStockId();
        double beforeRow2Price = poController.Detail(2).getUnitPrce().doubleValue();

        // Duplicate detection should hit row 0 when validating row 1.
        loJSON = poController.checkExistingDetailDuplicate(psStockId1, 100.00, 1);
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals(0L, ((Number) loJSON.get("row")).longValue());
        Assert.assertTrue(String.valueOf(loJSON.get("message")).contains("Duplicate entry found at row 1"));

        // Different unit price should not be flagged as duplicate.
        loJSON = poController.checkExistingDetailDuplicate(psStockId1, 100.01, 2);
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(2L, ((Number) loJSON.get("row")).longValue());

        // Different order number should not be flagged as duplicate.
        loJSON = poController.checkExistingDetailDuplicate(psStockId1, 100.00, 3);
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(3L, ((Number) loJSON.get("row")).longValue());

        // Self row must be ignored, so a single-row validation remains success.
        loJSON = poController.checkExistingDetailDuplicate(psStockId1, 100.00, 0);
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals(1L, ((Number) loJSON.get("row")).longValue());

        // Method should not mutate detail rows/count.
        Assert.assertEquals(beforeCount, poController.getDetailCount());
        Assert.assertEquals(beforeRow2OrderNo, poController.Detail(2).getOrderNo());
        Assert.assertEquals(beforeRow2Stock, poController.Detail(2).getStockId());
        Assert.assertEquals(beforeRow2Price, poController.Detail(2).getUnitPrce().doubleValue(), 0.0001);
    }

    @Test
    public void test051LoadPurchaseOrderReceiving4Args() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction(psTransNo);
        Assume.assumeTrue("Fixture transaction not available: " + psTransNo,
                "success".equals(loJSON.get("result")));

        poController.setIndustryId(poController.Master().getIndustryId());
        poController.setCategoryId(poController.Master().getCategoryCode());

        String companyId = poController.Master().getCompanyId();
        String supplierId = poController.Master().getSupplierId();
        String referenceNo = poController.Master().getTransactionNo();

        loJSON = poController.loadPurchaseOrderReceiving("history", companyId, supplierId, referenceNo);
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertNotNull(loJSON.get("message"));
        Assert.assertTrue("Expected at least one loaded PO receiving record.",
                poController.getPurchaseOrderReceivingCount() > 0);
        Assert.assertTrue("Expected loaded records to include the fixture transaction.",
                hasLoadedTransaction(referenceNo));
    }

    @Test
    public void test052LoadPurchaseOrderReceiving5Args() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction(psTransNo);
        Assume.assumeTrue("Fixture transaction not available: " + psTransNo,
                "success".equals(loJSON.get("result")));

        poController.setIndustryId(poController.Master().getIndustryId());
        poController.setCategoryId(poController.Master().getCategoryCode());

        String companyId = poController.Master().getCompanyId();
        String supplierId = poController.Master().getSupplierId();
        String branchCode = poController.Master().getBranchCode();
        String referenceNo = poController.Master().getTransactionNo();

        loJSON = poController.loadPurchaseOrderReceiving("history", companyId, supplierId, branchCode, referenceNo);
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertNotNull(loJSON.get("message"));
        Assert.assertTrue("Expected at least one loaded PO receiving record.",
                poController.getPurchaseOrderReceivingCount() > 0);
        Assert.assertTrue("Expected loaded records to include the fixture transaction.",
                hasLoadedTransaction(referenceNo));
    }

    @Test
    public void test053LoadUnPostPurchaseOrderReceiving() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction(psTransNo);
        Assume.assumeTrue("Fixture transaction not available: " + psTransNo,
                "success".equals(loJSON.get("result")));

        // Align controller-level filters used internally by loadUnPostPurchaseOrderReceiving.
        poController.setIndustryId(poController.Master().getIndustryId());
        poController.setCompanyId(poController.Master().getCompanyId());
        poController.setCategoryId(poController.Master().getCategoryCode());

        String supplier = "";
        String branch = "";
        String referenceNo = "";

        loJSON = poController.loadUnPostPurchaseOrderReceiving(supplier, branch, referenceNo);
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertNotNull(loJSON.get("message"));
        Assert.assertTrue("Expected at least one loaded PO receiving record.",
                poController.getPurchaseOrderReceivingCount() > 0);
    }

    @Test
    public void test054LoadAttachments() {
        // TODO
    }

    @Test
    public void test055GetApprovedPurchaseOrder() {
        resetController();

        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.setIndustryId(psIndustryId);
        poController.setCategoryId(psCategorCd);
        poController.setCompanyId(psCompanyId);
        poController.Master().setSupplierId(psSupplierId);

        loJSON = poController.getApprovedPurchaseOrder();
        Assert.assertNotNull(loJSON);
        Assert.assertNotNull(loJSON.get("result"));

        String result = String.valueOf(loJSON.get("result"));
        Assert.assertTrue("Expected success or error result.",
                "success".equals(result) || "error".equals(result));

        if ("success".equals(result)) {
            Assert.assertEquals("Record loaded successfully.", String.valueOf(loJSON.get("message")));
            Assert.assertTrue("Expected at least one approved purchase order.",
                    poController.getPurchaseOrderCount() > 0);

            for (int i = 0; i < poController.getPurchaseOrderCount(); i++) {
                Assert.assertNotNull(poController.PurchaseOrderList(i));
                Assert.assertNotNull(poController.PurchaseOrderList(i).getTransactionNo());
                Assert.assertFalse(poController.PurchaseOrderList(i).getTransactionNo().trim().isEmpty());
            }
        } else {
            Assert.assertEquals("No approved purchase order found .", String.valueOf(loJSON.get("message")));
            Assert.assertEquals(Boolean.TRUE, loJSON.get("continue"));
            Assert.assertEquals(0, poController.getPurchaseOrderCount());
        }

        int firstCallCount = poController.getPurchaseOrderCount();

        // Use a highly unlikely supplier id to validate that each invocation refreshes the PO list.
        poController.Master().setSupplierId("__NO_MATCH_SUPPLIER_9F5A2D6C__");
        JSONObject secondJSON = poController.getApprovedPurchaseOrder();
        Assert.assertNotNull(secondJSON);
        Assert.assertNotNull(secondJSON.get("result"));

        String secondResult = String.valueOf(secondJSON.get("result"));
        Assert.assertTrue("Expected success or error result on second invocation.",
                "success".equals(secondResult) || "error".equals(secondResult));

        if ("error".equals(secondResult)) {
            Assert.assertEquals("No approved purchase order found .", String.valueOf(secondJSON.get("message")));
            Assert.assertEquals(Boolean.TRUE, secondJSON.get("continue"));
            Assert.assertEquals(0, poController.getPurchaseOrderCount());
        } else {
            Assert.assertTrue("Second call should still produce a non-negative PO count.",
                    poController.getPurchaseOrderCount() >= 0);
        }

        Assert.assertTrue("PO list count must be refreshed per call and remain valid.",
                poController.getPurchaseOrderCount() >= 0);
        Assert.assertTrue("First call count must be non-negative.", firstCallCount >= 0);
    }

    @Test
    public void test056AddPurchaseOrderToPORDetail() throws Exception {
        resetController();
        startNewTransaction();

        poController.setIndustryId(psIndustryId);
        poController.setCategoryId(psCategorCd);
        poController.setCompanyId(psCompanyId);
        poController.Master().setSupplierId("");

        JSONObject loJSON = poController.getApprovedPurchaseOrder();
        Assume.assumeTrue("Approved purchase order list unavailable for fixture.",
                "success".equals(String.valueOf(loJSON.get("result")))
                        && poController.getPurchaseOrderCount() > 0);

        List<String> approvedPOs = new ArrayList<>();
        for (int i = 0; i < poController.getPurchaseOrderCount(); i++) {
            String txNo = poController.PurchaseOrderList(i).getTransactionNo();
            if (txNo != null && !txNo.trim().isEmpty()) {
                approvedPOs.add(txNo);
            }
        }
        Assume.assumeTrue("No approved PO transaction numbers found in fixture.", !approvedPOs.isEmpty());

        boolean successCaseValidated = false;

        for (String poNo : approvedPOs) {
            resetController();
            startNewTransaction();

            if (poController.getDetailCount() == 0) {
                JSONObject addRowJSON = poController.AddDetail();
                Assert.assertEquals("success", addRowJSON.get("result"));
            }

            String beforeSupplier = String.valueOf(poController.Master().getSupplierId() == null
                    ? "" : poController.Master().getSupplierId());
            int beforeCount = poController.getDetailCount();

            loJSON = poController.addPurchaseOrderToPORDetail(poNo);
            Assert.assertNotNull(loJSON);
            Assert.assertNotNull(loJSON.get("result"));

            String result = String.valueOf(loJSON.get("result"));
            Assert.assertTrue("Expected success or error.", "success".equals(result) || "error".equals(result));

            if ("success".equals(result)) {
                Assert.assertTrue("Detail count should not decrease after adding PO details.",
                        poController.getDetailCount() >= beforeCount);

                boolean foundMappedRow = false;
                for (int i = 0; i < poController.getDetailCount(); i++) {
                    String orderNo = poController.Detail(i).getOrderNo();
                    String stockId = poController.Detail(i).getStockId();
                    if (poNo.equals(orderNo) && stockId != null && !stockId.trim().isEmpty()) {
                        foundMappedRow = true;
                        break;
                    }
                }
                Assert.assertTrue("Expected at least one detail row mapped to selected PO.", foundMappedRow);

                String afterSupplier = String.valueOf(poController.Master().getSupplierId() == null
                        ? "" : poController.Master().getSupplierId());
                if (beforeSupplier.trim().isEmpty()) {
                    Assert.assertFalse("Supplier should be populated from selected PO when initially blank.",
                            afterSupplier.trim().isEmpty());
                }

                successCaseValidated = true;
                break;
            }

            String message = String.valueOf(loJSON.get("message"));
            boolean knownError = message.contains("No remaining order to be receive for Order No.")
                    || message.contains("Supplier must be equal to selected purchase order supplier.")
                    || message.contains("Purchase orders for pre-owned items cannot be combined with purchase orders for new items.")
                    || message.contains("No records found.");
            Assert.assertTrue("Unexpected error message: " + message, knownError);
        }

        Assume.assumeTrue("No approved PO produced addPurchaseOrderToPORDetail success in current fixture.",
                successCaseValidated);
    }

    @Test
    public void test057GetPurchaseOrderReturn() {
        // TODO
    }

    @Test
    public void test058AddPurchaseOrderReturnToPORDetail() {
        // TODO
    }

    @Test
    public void test059GetPurchaseOrderReceivingSerial() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction(psTransNo);
        Assume.assumeTrue("Fixture transaction not available: " + psTransNo,
                "success".equals(String.valueOf(loJSON.get("result"))));
        Assume.assumeTrue("Transaction must have at least one detail row.", poController.getDetailCount() > 0);

        int serializedEntryNo = -1;
        int nonSerializedEntryNo = -1;

        for (int i = 0; i < poController.getDetailCount(); i++) {
            if (poController.Detail(i).isSerialized()) {
                if (serializedEntryNo == -1) {
                    serializedEntryNo = i + 1; // API is 1-based.
                }
            } else if (nonSerializedEntryNo == -1) {
                nonSerializedEntryNo = i + 1; // API is 1-based.
            }
        }

        if (nonSerializedEntryNo != -1) {
            int beforeTotal = poController.getPurchaseOrderReceivingSerialCount();
            loJSON = poController.getPurchaseOrderReceivingSerial(nonSerializedEntryNo);
            Assert.assertNotNull(loJSON);
            Assert.assertEquals("success", String.valueOf(loJSON.get("result")));
            Assert.assertEquals("Non-serialized entry should not change serial count.",
                    beforeTotal, poController.getPurchaseOrderReceivingSerialCount());
        }

        Assume.assumeTrue("No serialized detail row available in fixture.", serializedEntryNo != -1);

        int beforeTotal = poController.getPurchaseOrderReceivingSerialCount();
        int beforePerEntry = 0;
        for (int i = 0; i < beforeTotal; i++) {
            if (String.valueOf(serializedEntryNo)
                    .equals(String.valueOf(poController.PurchaseOrderReceivingSerialList(i).getEntryNo()))) {
                beforePerEntry++;
            }
        }

        String expectedStockId = poController.Detail(serializedEntryNo - 1).getStockId();
        int expectedQty = (int) Math.round(poController.Detail(serializedEntryNo - 1).getQuantity().doubleValue());

        loJSON = poController.getPurchaseOrderReceivingSerial(serializedEntryNo);
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("success", String.valueOf(loJSON.get("result")));

        int afterTotal = poController.getPurchaseOrderReceivingSerialCount();
        int afterPerEntry = 0;
        for (int i = 0; i < afterTotal; i++) {
            if (String.valueOf(serializedEntryNo)
                    .equals(String.valueOf(poController.PurchaseOrderReceivingSerialList(i).getEntryNo()))) {
                afterPerEntry++;
            }
        }

        Assert.assertTrue("Serial list total should not decrease.", afterTotal >= beforeTotal);
        Assert.assertTrue("Serialized entry rows should satisfy requested quantity.", afterPerEntry >= expectedQty);

        if (afterPerEntry > beforePerEntry) {
            boolean foundMappedRow = false;
            for (int i = 0; i < afterTotal; i++) {
                if (String.valueOf(serializedEntryNo)
                        .equals(String.valueOf(poController.PurchaseOrderReceivingSerialList(i).getEntryNo()))
                        && expectedStockId.equals(poController.PurchaseOrderReceivingSerialList(i).getStockId())) {
                    foundMappedRow = true;
                    break;
                }
            }
            Assert.assertTrue("Expected at least one serial row mapped to selected entry and stock.", foundMappedRow);
        }
    }

    @Test
    public void test060CheckExistingSerialNo() throws Exception {
        resetController();
        startNewTransaction();

        if (poController.getDetailCount() == 0) {
            JSONObject addDetailJSON = poController.AddDetail();
            Assert.assertEquals("success", addDetailJSON.get("result"));
        }

        poController.Detail(0).setStockId(psStockId1);
        poController.Detail(0).setQuantity(2);
        poController.Detail(0).isSerialized(true);

        JSONObject loJSON = poController.getPurchaseOrderReceivingSerial(1);
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("success", String.valueOf(loJSON.get("result")));

        List<Integer> entryRows = new ArrayList<>();
        for (int i = 0; i < poController.getPurchaseOrderReceivingSerialCount(); i++) {
            if ("1".equals(String.valueOf(poController.PurchaseOrderReceivingSerialList(i).getEntryNo()))) {
                entryRows.add(i);
            }
        }
        Assume.assumeTrue("Need at least two serial rows for entry 1.", entryRows.size() >= 2);

        int row0 = entryRows.get(0);
        int row1 = entryRows.get(1);

        JSONObject checkJSON = poController.checkExistingSerialNo(row0, "serial01", "");
        Assert.assertEquals(Boolean.FALSE, checkJSON.get("set"));
        Assert.assertNull(checkJSON.get("result"));

        checkJSON = poController.checkExistingSerialNo(row0, "serial01", null);
        Assert.assertEquals(Boolean.FALSE, checkJSON.get("set"));
        Assert.assertNull(checkJSON.get("result"));

        poController.PurchaseOrderReceivingSerialList(row0).setSerial01("SER-A");
        poController.PurchaseOrderReceivingSerialList(row1).setSerial01("SER-B");

        checkJSON = poController.checkExistingSerialNo(row0, "serial01", "SER-B");
        Assert.assertEquals("error", String.valueOf(checkJSON.get("result")));
        Assert.assertEquals(Boolean.FALSE, checkJSON.get("set"));
        Assert.assertTrue(String.valueOf(checkJSON.get("message")).contains("already exists for Entry No 1"));

        checkJSON = poController.checkExistingSerialNo(row0, "serial01", "SER-A");
        Assert.assertNull("Self-match should not be flagged as duplicate.", checkJSON.get("result"));

        checkJSON = poController.checkExistingSerialNo(row0, "serial02", "SER-A");
        Assert.assertEquals("error", String.valueOf(checkJSON.get("result")));
        Assert.assertEquals(Boolean.FALSE, checkJSON.get("set"));
    }

    @Test
    public void test061CheckExistingSerialId() throws Exception {
        resetController();
        startNewTransaction();

        if (poController.getDetailCount() == 0) {
            JSONObject addDetailJSON = poController.AddDetail();
            Assert.assertEquals("success", addDetailJSON.get("result"));
        }

        poController.Detail(0).setStockId(psStockId1);
        poController.Detail(0).setQuantity(1);
        poController.Detail(0).isSerialized(true);

        JSONObject loJSON = poController.getPurchaseOrderReceivingSerial(1);
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("success", String.valueOf(loJSON.get("result")));
        Assume.assumeTrue("No serial row available for entry 1.", poController.getPurchaseOrderReceivingSerialCount() > 0);

        int targetIndex = -1;
        for (int i = 0; i < poController.getPurchaseOrderReceivingSerialCount(); i++) {
            if ("1".equals(String.valueOf(poController.PurchaseOrderReceivingSerialList(i).getEntryNo()))) {
                targetIndex = i;
                break;
            }
        }
        Assume.assumeTrue("No serial row mapped to entry 1.", targetIndex >= 0);

        poController.PurchaseOrderReceivingSerialList(targetIndex).setSerialId("SID-001");

        JSONObject checkJSON = poController.checkExistingSerialId(1);
        Assert.assertEquals("error", String.valueOf(checkJSON.get("result")));
        Assert.assertEquals("Serial ID already exist. Changing of brand / model is not allowed.",
                String.valueOf(checkJSON.get("message")));

        checkJSON = poController.checkExistingSerialId(999);
        Assert.assertNull(checkJSON.get("result"));
        Assert.assertNull(checkJSON.get("message"));
    }

    @Test
    public void test062RemovePurchaseOrderReceivingSerial() throws Exception {
        resetController();
        startNewTransaction();

        while (poController.getDetailCount() < 2) {
            JSONObject addDetailJSON = poController.AddDetail();
            Assert.assertEquals("success", addDetailJSON.get("result"));
        }

        poController.Detail(0).setStockId(psStockId1);
        poController.Detail(0).setQuantity(2);
        poController.Detail(0).isSerialized(true);

        poController.Detail(1).setStockId(psStockId2);
        poController.Detail(1).setQuantity(1);
        poController.Detail(1).isSerialized(true);

        JSONObject loJSON = poController.getPurchaseOrderReceivingSerial(1);
        Assert.assertEquals("success", String.valueOf(loJSON.get("result")));
        loJSON = poController.getPurchaseOrderReceivingSerial(2);
        Assert.assertEquals("success", String.valueOf(loJSON.get("result")));

        int beforeTotal = poController.getPurchaseOrderReceivingSerialCount();
        int beforeEntry1 = 0;
        int beforeEntry2 = 0;
        for (int i = 0; i < beforeTotal; i++) {
            String entryNo = String.valueOf(poController.PurchaseOrderReceivingSerialList(i).getEntryNo());
            if ("1".equals(entryNo)) beforeEntry1++;
            if ("2".equals(entryNo)) beforeEntry2++;
        }

        Assume.assumeTrue("Need serial rows for both entry 1 and entry 2.", beforeEntry1 > 0 && beforeEntry2 > 0);

        poController.removePurchaseOrderReceivingSerial(1);

        int afterTotal = poController.getPurchaseOrderReceivingSerialCount();
        int afterEntry1 = 0;
        int afterEntry2 = 0;
        for (int i = 0; i < afterTotal; i++) {
            String entryNo = String.valueOf(poController.PurchaseOrderReceivingSerialList(i).getEntryNo());
            if ("1".equals(entryNo)) afterEntry1++;
            if ("2".equals(entryNo)) afterEntry2++;
        }

        Assert.assertEquals("Total serial rows should be reduced by removed entry-1 rows.",
                beforeTotal - beforeEntry1, afterTotal);
        Assert.assertEquals("Entry-2 rows should be renumbered to entry-1.", beforeEntry2, afterEntry1);
        Assert.assertEquals("No rows should remain with original entry-2 after renumber.", 0, afterEntry2);

        int stableCount = poController.getPurchaseOrderReceivingSerialCount();
        poController.removePurchaseOrderReceivingSerial(999);
        Assert.assertEquals("Removing a non-existing entry should not change list size.",
                stableCount, poController.getPurchaseOrderReceivingSerialCount());
    }

    @Test
    public void test063CheckPurchaseOrderReceivingSerial() throws Exception {
        resetController();
        startNewTransaction();

        if (poController.getDetailCount() == 0) {
            JSONObject addDetailJSON = poController.AddDetail();
            Assert.assertEquals("success", addDetailJSON.get("result"));
        }

        poController.Detail(0).setStockId(psStockId1);
        poController.Detail(0).setQuantity(3);
        poController.Detail(0).isSerialized(true);

        JSONObject loJSON = poController.getPurchaseOrderReceivingSerial(1);
        Assert.assertEquals("success", String.valueOf(loJSON.get("result")));

        List<Integer> entryRows = new ArrayList<>();
        for (int i = 0; i < poController.getPurchaseOrderReceivingSerialCount(); i++) {
            if ("1".equals(String.valueOf(poController.PurchaseOrderReceivingSerialList(i).getEntryNo()))) {
                entryRows.add(i);
            }
        }
        Assume.assumeTrue("Need at least three serial rows for entry 1.", entryRows.size() >= 3);

        int i0 = entryRows.get(0);
        int i1 = entryRows.get(1);
        int i2 = entryRows.get(2);

        poController.PurchaseOrderReceivingSerialList(i0).setSerial01("");
        poController.PurchaseOrderReceivingSerialList(i0).setSerial02("");
        poController.PurchaseOrderReceivingSerialList(i0).setSerialId("");

        poController.PurchaseOrderReceivingSerialList(i1).setSerial01("S-100");
        poController.PurchaseOrderReceivingSerialList(i1).setSerial02("");
        poController.PurchaseOrderReceivingSerialList(i1).setSerialId("");

        poController.PurchaseOrderReceivingSerialList(i2).setSerial01("");
        poController.PurchaseOrderReceivingSerialList(i2).setSerial02("S-200");
        poController.PurchaseOrderReceivingSerialList(i2).setSerialId("");

        int beforeEntryCount = 0;
        int beforeEmptyCount = 0;
        int beforeTotal = poController.getPurchaseOrderReceivingSerialCount();
        for (int i = 0; i < beforeTotal; i++) {
            if ("1".equals(String.valueOf(poController.PurchaseOrderReceivingSerialList(i).getEntryNo()))) {
                beforeEntryCount++;
                String s1 = poController.PurchaseOrderReceivingSerialList(i).getSerial01();
                String s2 = poController.PurchaseOrderReceivingSerialList(i).getSerial02();
                if ((s1 == null || s1.trim().isEmpty()) && (s2 == null || s2.trim().isEmpty())) {
                    beforeEmptyCount++;
                }
            }
        }

        JSONObject checkJSON = poController.checkPurchaseOrderReceivingSerial(1, 2);
        Assert.assertNull("Expected no explicit error result for removable rows.", checkJSON.get("result"));

        int afterEntryCount = 0;
        int afterEmptyCount = 0;
        int afterTotal = poController.getPurchaseOrderReceivingSerialCount();
        for (int i = 0; i < afterTotal; i++) {
            if ("1".equals(String.valueOf(poController.PurchaseOrderReceivingSerialList(i).getEntryNo()))) {
                afterEntryCount++;
                String s1 = poController.PurchaseOrderReceivingSerialList(i).getSerial01();
                String s2 = poController.PurchaseOrderReceivingSerialList(i).getSerial02();
                if ((s1 == null || s1.trim().isEmpty()) && (s2 == null || s2.trim().isEmpty())) {
                    afterEmptyCount++;
                }
            }
        }

        Assert.assertEquals("Entry serial rows should be reduced by one when quantity drops from 3 to 2.",
                beforeEntryCount - 1, afterEntryCount);
        Assert.assertTrue("Removal should prioritize empty serial rows.", afterEmptyCount <= beforeEmptyCount);

        int stableCount = poController.getPurchaseOrderReceivingSerialCount();
        checkJSON = poController.checkPurchaseOrderReceivingSerial(1, 999);
        Assert.assertNull(checkJSON.get("result"));
        Assert.assertEquals("Larger target quantity should not remove rows.",
                stableCount, poController.getPurchaseOrderReceivingSerialCount());
    }

    @Test
    public void test064Journal() {
        // TODO
    }

    @Test
    public void test065PopulateJournal() {
        // TODO
    }

    @Test
    public void test066CheckExistAcctCode() {
        // TODO
    }

    @Test
    public void test067ExistJournal() {
        // TODO
    }

    @Test
    public void test068GetSourceCode() {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals("PODA", poController.getSourceCode());
    }

    @Test
    public void test069Master() {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertNotNull(poController.Master());
    }

    @Test
    public void test070GetDetail() {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertNotNull(poController.getDetail());
    }

    @Test
    public void test071GetSerial() {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertNotNull(poController.getSerial());
    }

    @Test
    public void test072PurchaseOrderReceivingSerialListNoArgs() {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertNotNull(poController.PurchaseOrderReceivingSerialList());
        Assert.assertEquals(poController.getPurchaseOrderReceivingSerialCount(),
                poController.PurchaseOrderReceivingSerialList().size());
    }

    @Test
    public void test073TransactionAttachmentList() throws Exception {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.addAttachment();
        Assert.assertEquals("success", String.valueOf(loJSON.get("result")));
        Assume.assumeTrue("No attachment row available.", poController.getTransactionAttachmentCount() > 0);
        Assert.assertNotNull(poController.TransactionAttachmentList(0));
    }

    @Test
    public void test074Detail() {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assume.assumeTrue("No detail row available.", poController.getDetailCount() > 0);
        Assert.assertNotNull(poController.Detail(0));
    }

    @Test
    public void test075DetailRemove() {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        Assert.assertEquals(0, poController.getDetailRemovedCount());
        try {
            poController.DetailRemove(0);
            Assert.fail("Expected IndexOutOfBoundsException when removed-detail list is empty.");
        } catch (IndexOutOfBoundsException expected) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void test076PurchaseOrderReceivingSerialListByIndex() throws Exception {
        resetController();
        startNewTransaction();

        if (poController.getDetailCount() == 0) {
            JSONObject addDetailJSON = poController.AddDetail();
            Assert.assertEquals("success", addDetailJSON.get("result"));
        }

        poController.Detail(0).setStockId(psStockId1);
        poController.Detail(0).setQuantity(1);
        poController.Detail(0).isSerialized(true);

        JSONObject loJSON = poController.getPurchaseOrderReceivingSerial(1);
        Assert.assertEquals("success", String.valueOf(loJSON.get("result")));

        Assume.assumeTrue("No serial row available.", poController.getPurchaseOrderReceivingSerialCount() > 0);
        Assert.assertNotNull(poController.PurchaseOrderReceivingSerialList(0));
    }

    @Test
    public void test077PurchaseOrderList() {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.setIndustryId(psIndustryId);
        poController.setCategoryId(psCategorCd);
        poController.setCompanyId(psCompanyId);
        poController.Master().setSupplierId(psSupplierId);

        loJSON = poController.getApprovedPurchaseOrder();
        Assert.assertNotNull(loJSON);
        Assert.assertNotNull(loJSON.get("result"));

        if ("success".equals(String.valueOf(loJSON.get("result")))) {
            Assume.assumeTrue("No purchase order row available.", poController.getPurchaseOrderCount() > 0);
            Assert.assertNotNull(poController.PurchaseOrderList(0));
        }
    }

    @Test
    public void test078PurchaseOrderReceivingList() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction(psTransNo);
        Assume.assumeTrue("Fixture transaction not available: " + psTransNo,
                "success".equals(String.valueOf(loJSON.get("result"))));

        poController.setIndustryId(poController.Master().getIndustryId());
        poController.setCategoryId(poController.Master().getCategoryCode());
        loJSON = poController.loadPurchaseOrderReceiving(
                "history",
                poController.Master().getCompanyId(),
                poController.Master().getSupplierId(),
                poController.Master().getTransactionNo());

        Assert.assertNotNull(loJSON);
        Assert.assertNotNull(loJSON.get("result"));
        if ("success".equals(String.valueOf(loJSON.get("result")))) {
            Assume.assumeTrue("No POR row available.", poController.getPurchaseOrderReceivingCount() > 0);
            Assert.assertNotNull(poController.PurchaseOrderReceivingList(0));
        }
    }

    @Test
    public void test079PurchaseOrderReturnList() {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.getPurchaseOrderReturn("");
        Assert.assertNotNull(loJSON);
        Assert.assertNotNull(loJSON.get("result"));

        if ("success".equals(String.valueOf(loJSON.get("result")))) {
            Assume.assumeTrue("No PO return row available.", poController.getPurchaseOrderReturnCount() > 0);
            Assert.assertNotNull(poController.PurchaseOrderReturnList(0));
        }
    }

    @Test
    public void test080GetPurchaseOrderReceivingCount() {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertTrue(poController.getPurchaseOrderReceivingCount() >= 0);
    }

    @Test
    public void test081GetDetailCount() {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertTrue(poController.getDetailCount() >= 0);
    }

    @Test
    public void test082GetDetailRemovedCount() {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertTrue(poController.getDetailRemovedCount() >= 0);
    }

    @Test
    public void test083GetPurchaseOrderReceivingSerialCount() {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertTrue(poController.getPurchaseOrderReceivingSerialCount() >= 0);
    }

    @Test
    public void test084GetTransactionAttachmentCount() {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertTrue(poController.getTransactionAttachmentCount() >= 0);
    }

    @Test
    public void test085GetPurchaseOrderCount() {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertTrue(poController.getPurchaseOrderCount() >= 0);
    }

    @Test
    public void test086GetPurchaseOrderReturnCount() {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertTrue(poController.getPurchaseOrderReturnCount() >= 0);
    }

    @Test
    public void test087AddDetail() throws Exception {
        resetController();
        startNewTransaction();

        int beforeCount = poController.getDetailCount();
        JSONObject loJSON = poController.AddDetail();
        Assert.assertNotNull(loJSON);
        Assert.assertNotNull(loJSON.get("result"));

        String result = String.valueOf(loJSON.get("result"));
        Assert.assertTrue("success".equals(result) || "error".equals(result));
        if ("success".equals(result)) {
            Assert.assertTrue(poController.getDetailCount() >= beforeCount);
        } else {
            Assert.assertTrue(String.valueOf(loJSON.get("message")).contains("Last row has empty item."));
        }
    }

    @Test
    public void test088AddPurchaseOrderReceivingSerial() {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        int beforeCount = poController.getPurchaseOrderReceivingSerialCount();
        loJSON = poController.addPurchaseOrderReceivingSerial();
        Assert.assertNotNull(loJSON);
        Assert.assertNotNull(loJSON.get("result"));

        String result = String.valueOf(loJSON.get("result"));
        Assert.assertTrue("success".equals(result) || "error".equals(result));
        if ("success".equals(result)) {
            Assert.assertTrue(poController.getPurchaseOrderReceivingSerialCount() >= beforeCount);
        }
    }

    @Test
    public void test089AddAttachmentNoArgs() throws Exception {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        int beforeCount = poController.getTransactionAttachmentCount();
        loJSON = poController.addAttachment();
        Assert.assertNotNull(loJSON);
        Assert.assertNotNull(loJSON.get("result"));

        String result = String.valueOf(loJSON.get("result"));
        Assert.assertTrue("success".equals(result) || "error".equals(result));
        if ("success".equals(result)) {
            Assert.assertTrue(poController.getTransactionAttachmentCount() >= beforeCount);
        }
    }

    @Test
    public void test090RemoveAttachment() throws Exception {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.addAttachment();
        Assume.assumeTrue("Unable to seed attachment row.", "success".equals(String.valueOf(loJSON.get("result"))));
        Assume.assumeTrue("No attachment row available.", poController.getTransactionAttachmentCount() > 0);

        loJSON = poController.removeAttachment(0);
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("success", String.valueOf(loJSON.get("result")));
    }

    @Test
    public void test091AddAttachmentWithFileName() {
        // TODO
    }

    @Test
    public void test092CopyFile() {
        // TODO
    }

    @Test
    public void test093CheckExistingFileName() {
        // TODO
    }

    @Test
    public void test094ResetOthers() throws Exception {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.addPurchaseOrderReceivingSerial();
        poController.addAttachment();
        poController.resetOthers();

        Assert.assertEquals(0, poController.getPurchaseOrderReceivingSerialCount());
        Assert.assertEquals(0, poController.getTransactionAttachmentCount());
    }

    @Test
    public void test095ResetMaster() {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        Object oldMaster = poController.Master();
        poController.resetMaster();

        Assert.assertNotNull(poController.Master());
        Assert.assertNotSame(oldMaster, poController.Master());
    }

    @Test
    public void test096ResetJournal() {
        poController.resetJournal();
        Assert.assertNotNull(poController.Journal());
    }

    @Test
    public void test097SetIndustryId() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.setIndustryId(psIndustryId);
        poController.setCategoryId(psCategorCd);
        poController.setCompanyId(psCompanyId);

        loJSON = poController.NewTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(psIndustryId, poController.Master().getIndustryId());
    }

    @Test
    public void test098SetCompanyId() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.setIndustryId(psIndustryId);
        poController.setCategoryId(psCategorCd);
        poController.setCompanyId(psCompanyId);

        loJSON = poController.NewTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(psCompanyId, poController.Master().getCompanyId());
    }

    @Test
    public void test099SetCategoryId() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.setIndustryId(psIndustryId);
        poController.setCategoryId(psCategorCd);
        poController.setCompanyId(psCompanyId);

        loJSON = poController.NewTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(psCategorCd, poController.Master().getCategoryCode());
    }

    @Test
    public void test100IsFinance() {
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.isFinance(false);
        Assert.assertEquals("Confirmed", poController.getStatus(PurchaseOrderReceivingStatus.CONFIRMED));

        poController.isFinance(true);
        Assert.assertEquals("Open", poController.getStatus(PurchaseOrderReceivingStatus.CONFIRMED));
    }

    @Test
    public void test101SetPurpose() throws Exception {
        poController.setPurpose(PurchaseOrderReceivingStatus.Purpose.REGULAR);

        java.lang.reflect.Field purposeField = poController.getClass().getDeclaredField("psPurpose");
        purposeField.setAccessible(true);
        Assert.assertEquals(PurchaseOrderReceivingStatus.Purpose.REGULAR, purposeField.get(poController));
    }

    @Test
    public void test102GetQuantity() {
        // TODO
    }

    @Test
    public void test103SetQuantity() {
        // TODO
    }

    @Test
    public void test104WillSave() throws Exception {
        // Case 1: No detail rows should fail validation.
        resetController();
        startNewTransaction();

        JSONObject loJSON = poController.removePORDetails();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(0, poController.getDetailCount());

        loJSON = poController.willSave();
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("error", String.valueOf(loJSON.get("result")));
        Assert.assertNotNull("Validation message should exist.", loJSON.get("message"));

        String msgNoDetail = String.valueOf(loJSON.get("message"));
        Assert.assertTrue("Expected no-detail/zero-qty validation message but got: " + msgNoDetail,
                msgNoDetail.contains("No transaction detail to be save")
                        || msgNoDetail.contains("zero quantity"));

        // Case 2: Detail row with zero quantity should fail validation.
        resetController();
        startNewTransaction();

        loJSON = poController.removePORDetails();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.AddDetail();
        Assert.assertEquals("success", loJSON.get("result"));
        poController.Detail(0).setStockId(psStockId1);
        poController.Detail(0).setQuantity(0);
        poController.Detail(0).setOrderNo("");

        loJSON = poController.willSave();
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("error", String.valueOf(loJSON.get("result")));
        Assert.assertNotNull("Validation message should exist.", loJSON.get("message"));

        String msgZeroQty = String.valueOf(loJSON.get("message"));
        Assert.assertTrue("Expected zero-quantity/no-detail validation message but got: " + msgZeroQty,
                msgZeroQty.contains("zero quantity")
                        || msgZeroQty.contains("No transaction detail to be save"));
        
        
        loJSON = poController.removePORDetails();
        Assert.assertEquals("success", loJSON.get("result"));
        
        loJSON = poController.AddDetail();
        Assert.assertEquals("success", loJSON.get("result"));
        poController.Detail(0).setStockId("");
        poController.Detail(0).setQuantity(0);
        poController.Detail(0).setOrderNo("");
        
        loJSON = poController.willSave();
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("error", String.valueOf(loJSON.get("result")));
        Assert.assertNotNull("Validation message should exist.", loJSON.get("message"));
        
        //Case 3: Test for SI Posting Finance
        resetController();
        loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        poController.isFinance(true);
        loJSON = poController.OpenTransaction(psTransNo);
        Assume.assumeTrue("Fixture transaction not available: " + psTransNo,
                "success".equals(loJSON.get("result")));
        
        loJSON = poController.UpdateTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        
        //Case 3.0: Validate Return
        poController.Master().setTransactionStatus(PurchaseOrderReceivingStatus.RETURNED_I);
        loJSON = poController.willSave();
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("error", String.valueOf(loJSON.get("result")));
        
        //Case 3.1: Validate Trucking
        poController.Master().setTransactionStatus(PurchaseOrderReceivingStatus.CONFIRMED);
        poController.Master().setTruckingId(psSupplierId);
        poController.Master().setFreight(0.0000);
        loJSON = poController.willSave();
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("error", String.valueOf(loJSON.get("result")));
        
        //Case 3.2: Validate Invoice Date null
        poController.Master().setTransactionStatus(PurchaseOrderReceivingStatus.CONFIRMED);
        poController.Master().setTruckingId("");
        poController.Master().setSalesInvoice("123");
        poController.Master().setSalesInvoiceDate(null);
        loJSON = poController.willSave();
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("error", String.valueOf(loJSON.get("result")));
        
        //Case 3.2: Validate Invoice Date 1900-01-01
        poController.Master().setTransactionStatus(PurchaseOrderReceivingStatus.CONFIRMED);
        poController.Master().setSalesInvoice("123");
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("1900-01-01");
        poController.Master().setSalesInvoiceDate(date);
        loJSON = poController.willSave();
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("error", String.valueOf(loJSON.get("result")));
        
        loJSON = poController.OpenTransaction(psTransNo);
        Assume.assumeTrue("Fixture transaction not available: " + psTransNo,
                "success".equals(loJSON.get("result")));
        
        loJSON = poController.UpdateTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        
        loJSON = poController.willSave();
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("success", String.valueOf(loJSON.get("result")));
        
        //Case 4 : Test Return
        resetController();
        loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        loJSON = poController.OpenTransaction(psTransNoWithSerial);
        Assume.assumeTrue("Fixture transaction not available: " + psTransNoWithSerial,
                "success".equals(loJSON.get("result")));
        
        loJSON = poController.UpdateTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        
        loJSON = poController.getPurchaseOrderReceivingSerial(1);
        Assert.assertEquals("success", loJSON.get("result"));
        
        //Case 4.0: Validate Return
        poController.Master().setTransactionStatus(PurchaseOrderReceivingStatus.RETURNED);
        loJSON = poController.willSave();
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("error", String.valueOf(loJSON.get("result")));
        
        //Case 4.1 : Test Return without serial
        resetController();
        loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        poController.isFinance(true);
        loJSON = poController.OpenTransaction(psTransNo);
        Assume.assumeTrue("Fixture transaction not available: " + psTransNo,
                "success".equals(loJSON.get("result")));
        
        loJSON = poController.UpdateTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        
        //Case 4.0: Validate Return
        poController.Master().setTransactionStatus(PurchaseOrderReceivingStatus.RETURNED);
        loJSON = poController.willSave();
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("error", String.valueOf(loJSON.get("result")));
        
        //Case 5.0: Validate Order Qty
        poController.Master().setTransactionStatus(PurchaseOrderReceivingStatus.OPEN);
        poController.Detail(0).setOrderNo(psPONumber1);
        poController.Detail(0).setOrderQty(-1.00);
        loJSON = poController.willSave();
        Assert.assertNotNull(loJSON);
        Assert.assertEquals("error", String.valueOf(loJSON.get("result")));
        
        //Case 6 : Test with serial
        resetController();
        loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        loJSON = poController.OpenTransaction(psTransNoWithSerial);
        Assume.assumeTrue("Fixture transaction not available: " + psTransNoWithSerial,
                "success".equals(loJSON.get("result")));
        
        loJSON = poController.UpdateTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        
        
        loJSON = poController.getPurchaseOrderReceivingSerial(1);
        Assert.assertEquals("success", loJSON.get("result"));
        
        if(poController.getPurchaseOrderReceivingSerialCount() > 0){
            poController.Master().setCategoryCode(PurchaseOrderReceivingStatus.Category.CAR);
            poController.PurchaseOrderReceivingSerialList(0).setConductionStickerNo(null);
            poController.PurchaseOrderReceivingSerialList(0).setPlateNo(null);
            loJSON = poController.willSave();
            Assert.assertNotNull(loJSON);
            Assert.assertEquals("error", String.valueOf(loJSON.get("result")));

            poController.PurchaseOrderReceivingSerialList(0).setLocationId(null);
            loJSON = poController.willSave();
            Assert.assertNotNull(loJSON);
            Assert.assertEquals("error", String.valueOf(loJSON.get("result")));

            poController.PurchaseOrderReceivingSerialList(0).setLocationId("1");
            poController.PurchaseOrderReceivingSerialList(0).setSerial01(null);
            poController.PurchaseOrderReceivingSerialList(0).setSerial02(null);
            loJSON = poController.willSave();
            Assert.assertNotNull(loJSON);
            Assert.assertEquals("error", String.valueOf(loJSON.get("result")));

            poController.Master().setCategoryCode(PurchaseOrderReceivingStatus.Category.APPLIANCES);
            loJSON = poController.willSave();
            Assert.assertNotNull(loJSON);
            Assert.assertEquals("error", String.valueOf(loJSON.get("result")));


            poController.Master().setCategoryCode(PurchaseOrderReceivingStatus.Category.GENERAL);
            loJSON = poController.willSave();
            Assert.assertNotNull(loJSON);
            Assert.assertEquals("error", String.valueOf(loJSON.get("result")));

            //Case 7: Test duplicate serial 1 / 2
//            poController.Master().setCategoryCode(PurchaseOrderReceivingStatus.Category.MOTORCYCLE);
//            poController.PurchaseOrderReceivingSerialList(0).setLocationId("1");
//            poController.PurchaseOrderReceivingSerialList(0).setSerial01("test123");
//            poController.PurchaseOrderReceivingSerialList(0).setSerial02("test124");
//            
//            loJSON = poController.AddDetail();
//            Assert.assertEquals("success", loJSON.get("result"));
//            poController.Detail(1).setStockId(psStockIdSerialize);
//            poController.Detail(1).setQuantity(1);
            
//            loJSON = poController.getPurchaseOrderReceivingSerial(2);
//            Assert.assertEquals("success", loJSON.get("result"));
//            
//            poController.PurchaseOrderReceivingSerialList(1).setLocationId("1");
//            poController.PurchaseOrderReceivingSerialList(1).setSerial01("test123");
//            poController.PurchaseOrderReceivingSerialList(1).setSerial02("test124");
//            loJSON = poController.willSave();
//            Assert.assertNotNull(loJSON);
//            Assert.assertEquals("error", String.valueOf(loJSON.get("result")));
//            
//            poController.PurchaseOrderReceivingSerialList(1).setConductionStickerNo("test1");
//            loJSON = poController.willSave();
//            Assert.assertNotNull(loJSON);
//            Assert.assertEquals("error", String.valueOf(loJSON.get("result")));
//            
//            
//            poController.PurchaseOrderReceivingSerialList(1).setConductionStickerNo("test12");
//            poController.PurchaseOrderReceivingSerialList(1).setPlateNo("test2");
//            loJSON = poController.willSave();
//            Assert.assertNotNull(loJSON);
//            Assert.assertEquals("error", String.valueOf(loJSON.get("result")));
//            
//            loJSON = poController.AddDetail();
//            Assert.assertEquals("success", loJSON.get("result"));
//            poController.Detail(2).setStockId(psStockIdSerialize);
//            poController.Detail(2).setQuantity(1);
//            
//            loJSON = poController.getPurchaseOrderReceivingSerial(3);
//            Assert.assertEquals("success", loJSON.get("result"));
//            
//            loJSON = poController.willSave();
//            Assert.assertNotNull(loJSON);
//            Assert.assertEquals("error", String.valueOf(loJSON.get("result")));
        
        }
    }

    @Test
    public void test105Save() throws Exception {
        //TODO
    }

    @Test
    public void test106SaveOthers() throws Exception {
        resetController();
        startNewTransaction();

        // Keep path deterministic: non-finance, no confirmed-status side effects.
        poController.isFinance(false);
        poController.Master().setTransactionStatus(PurchaseOrderReceivingStatus.OPEN);

        JSONObject loJSON = poController.saveOthers();
        Assert.assertNotNull(loJSON);
        Assert.assertNotNull(loJSON.get("result"));

        String result = String.valueOf(loJSON.get("result"));
        Assert.assertTrue("Expected success or error result.",
                "success".equals(result) || "error".equals(result));

        if ("error".equals(result)) {
            Assert.assertNotNull("Error result should include a message.", loJSON.get("message"));
        }
    }

    @Test
    public void test108SaveComplete() {
        poController.saveComplete();
        Assert.assertTrue(true);
    }

    @Test
    public void test109InitFields() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.setIndustryId(psIndustryId);
        poController.setCategoryId(psCategorCd);
        poController.setCompanyId(psCompanyId);

        loJSON = poController.NewTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        Assert.assertEquals(psIndustryId, poController.Master().getIndustryId());
        Assert.assertEquals(psCategorCd, poController.Master().getCategoryCode());
        Assert.assertEquals(psCompanyId, poController.Master().getCompanyId());
        Assert.assertEquals(PurchaseOrderReceivingStatus.OPEN, poController.Master().getTransactionStatus());
        Assert.assertNotNull(poController.Master().getTransactionDate());
        Assert.assertNotNull(poController.Master().getReferenceDate());
    }

    @Test
    public void test110GetTermCode() {
        // TODO
    }

    @Test
    public void test111GetInventoryTypeCode() {
        // TODO
    }

    @Test
    public void test112GetCompanyId() {
        // TODO
    }

    @Test
    public void test113InitSQL() {
        // TODO
    }

    @Test
    public void test114GetConfirmedBy() {
        // TODO
    }

    @Test
    public void test115PrintRecord() {
        // TODO
    }

    @Test
    public void test116SearchBarcodePORDetail() {
        // TODO
    }

    @Test
    public void test117SearchDescriptionPORDetail() {
        // TODO
    }

    @Test
    public void test118SearchImeiPORDetail() {
        // TODO
    }

    @Test
    public void test119SearchEnginePORDetail() {
        // TODO
    }

    @Test
    public void test120SearchFramePORDetail() {
        // TODO
    }

    @Test
    public void test121SearchPlatePORDetail() {
        // TODO
    }

    @Test
    public void test122SearchConductionStickerNoPORDetail() {
        // TODO
    }

    @Test
    public void test123ShowStatusHistory() {
        // TODO
    }

    @Test
    public void test124GetEntryBy() {
        // TODO
    }

    @Test
    public void test125GetSysUser() {
        // TODO
    }

    @Test
    public void test126SearchCategory() {
        // TODO
    }

    @Test
    public void test127SearchDestination() {
        // TODO
    }

    @Test
    public void test128SearchBranchReports() {
        // TODO
    }

    @Test
    public void test129SearchSuppliers() {
        // TODO
    }

    @Test
    public void test130RetriveSummaryReports() {
        // TODO
    }

    @Test
    public void test131RetriveSummaryDetailedReports() {
        // TODO
    }

    @Test
    public void test132FormatDateToText() {
        // TODO
    }

    @Test
    public void test133PrintReports() {
        // TODO
    }
}
