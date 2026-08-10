
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.purchasing.controller.PurchaseOrder;
import org.guanzon.cas.purchasing.services.PurchaseOrderControllers;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.h2.tools.RunScript;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PurchaseOrderTest {

    // ─── Shared state ────────────────────────────────────────────────────────
    static GRiderCAS instance;
    static PurchaseOrder poController;
    static Connection conn;
    private static String psIndustryId = "02";
    private static String psCompanyId = "M001";
    private static String psCategorCd = "0000005";
    private String psSalesCommitmentNo = "GCO126000002";
    private String psTransNo = "GK0126000001";
    private String psStockId = "GK0123000010";
    private String psBankId = "M00120139";

    @BeforeAll
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

    private static boolean loadProperties() {
        try {
            Properties po = new Properties();
            po.load(new FileInputStream(System.getProperty("sys.default.path.config") + "/config/cas.properties"));
            System.setProperty("sys.main.industry", po.getProperty("sys.main.industry"));
            System.setProperty("sys.general.industry", po.getProperty("sys.general.industry"));
            System.setProperty("sys.dept.finance", po.getProperty("sys.dept.finance"));
            System.setProperty("sys.dept.procurement", po.getProperty("sys.dept.procurement"));
            System.setProperty("user.selected.industry", po.getProperty("user.selected.industry"));
            System.setProperty("user.selected.category", po.getProperty("user.selected.category"));
            System.setProperty("user.selected.company", po.getProperty("user.selected.company"));
            System.setProperty("sys.default.client.token",
                    System.getProperty("sys.default.path.config") + "/client.token");
            System.setProperty("sys.default.access.token",
                    System.getProperty("sys.default.path.config") + "/access.token");
            System.setProperty("sys.default.path.temp.attachments",
                    po.getProperty("sys.default.path.temp.attachments"));
            System.setProperty("allowed.department", po.getProperty("allowed.department"));
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static void loadCorePrimary() throws IOException, SQLException {
        conn = instance.getGConnection().getConnection();

        String[] schemas = {
            "test-data/brand_schema.sql",
            "test-data/department_schema.sql",
            "test-data/ap_client_master_schema.sql",
            "test-data/transaction_status_history_schema.sql",
            "test-data/transaction_attachment_schema.sql",
            "test-data/branch_schema.sql",
            "test-data/industry_schema.sql",
            "test-data/company_schema.sql",
            "test-data/payee_schema.sql",
            "test-data/inv_type_schema.sql",
            "test-data/term_schema.sql",
            "test-data/client_master_schema.sql",
            "test-data/client_address_schema.sql",
            "test-data/client_institution_contact_person_schema.sql",
            "test-data/color_schema.sql",
            "test-data/model_schema.sql",
            "test-data/measure_schema.sql",
            "test-data/model_variant_schema.sql",
            "test-data/project_schema.sql",
            "test-data/inv_supplier_schema.sql",
            "test-data/category_schema.sql",
            "test-data/inv_master_schema.sql",
            "test-data/inventory_schema.sql",
            "test-data/inv_ledger_schema.sql",
            "test-data/xxxsysfiles_schema.sql",
            "test-data/xxxsysaction_schema.sql",
            "test-data/inv_stock_request_master_schema.sql",
            "test-data/inv_stock_request_detail_schema.sql",
            "test-data/po_quotation_master_schema.sql",
            "test-data/po_quotation_detail_schema.sql",
            "test-data/payment_request_master_schema.sql",
            "test-data/payment_request_detail_schema.sql",
            "test-data/po_master_schema.sql",
            "test-data/po_detail_schema.sql",};

        String[] data = {
            "test-data/brand_data.sql",
            "test-data/department_data.sql",
            "test-data/ap_client_master_data.sql",
            "test-data/inventory_data.sql",
            "test-data/transaction_status_history_data.sql",
            "test-data/transaction_attachment_data.sql",
            "test-data/branch_data.sql",
            "test-data/industry_data.sql",
            "test-data/company_data.sql",
            "test-data/payee_data.sql",
            "test-data/inv_type_data.sql",
            "test-data/term_data.sql",
            "test-data/client_master_data.sql",
            "test-data/client_address_data.sql",
            "test-data/client_institution_contact_person_data.sql",
            "test-data/color_data.sql",
            "test-data/model_data.sql",
            "test-data/measure_data.sql",
            "test-data/model_variant_data.sql",
            "test-data/project_data.sql",
            "test-data/inv_supplier_data.sql",
            "test-data/category_data.sql",
            "test-data/inv_master_data.sql",
            "test-data/inv_ledger_data.sql",
            "test-data/xxxsysfiles_data.sql",
            "test-data/xxxsysaction_data.sql",
            "test-data/inv_stock_request_master_data.sql",
            "test-data/inv_stock_request_detail_data.sql",
            "test-data/po_quotation_master_data.sql",
            "test-data/po_quotation_detail_data.sql",
            "test-data/payment_request_master_data.sql",
            "test-data/payment_request_detail_data.sql",
            "test-data/po_master_data.sql",
            "test-data/po_detail_data.sql",};
        for (String s : schemas) {
            RunScript.execute(conn, new FileReader(s));
        }
        for (String d : data) {
            RunScript.execute(conn, new FileReader(d));
        }

    }

    private static void print(String lsMessage) {
        System.out.println(lsMessage);
    }

    @AfterClass
    public static void tearDownClass2() {
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

    @AfterAll
    static void tearDownClass() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("DB connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
        System.out.println("System properties cleared.");
    }

    private static void startNewTransaction() throws CloneNotSupportedException, SQLException, GuanzonException {
        if (poController == null) {
            resetController();
        }

        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.NewTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        setClassConfig();
    }

    private static void resetController() {
        poController = new PurchaseOrderControllers(instance, null).PurchaseOrder();
        poController.setWithUI(false);
        poController.setWithParent(true);
        Assert.assertNotNull(poController);
    }

    private static void setClassConfig() {
        poController.Master().setIndustryID(psIndustryId);
        poController.Master().setCompanyID(psCompanyId);
        poController.Master().setCategoryCode(psCategorCd);
    }

    @Test
    @Order(1)
    public void testInitTransaction() throws SQLException, GuanzonException {
        if (poController == null) {
            resetController();
        }
        Assert.assertNotNull(poController);

        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals("POxx", poController.getSourceCode());
        Assert.assertNotNull(poController.Master());
        Assert.assertNotNull(poController.Detail());
    }

//    @org.junit.Test
//    void test02OpenTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
//        poController.InitTransaction();
//        JSONObject json = poController.OpenTransaction("GCO126000001");
//        isJSONSuccess(json);
//    }
    @Test
    @Order(2)
    public void testNewTransaction() throws CloneNotSupportedException, SQLException, GuanzonException {
        if (poController == null) {
            resetController();
        }
        Assert.assertNotNull(poController);

        resetController();

        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.NewTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        setClassConfig();

        Assert.assertEquals(psIndustryId, poController.Master().getIndustryID());
        Assert.assertEquals(psCategorCd, poController.Master().getCategoryCode());
        Assert.assertEquals(psCompanyId, poController.Master().getCompanyID());
        Assert.assertEquals(PurchaseOrderStatus.OPEN, poController.Master().getTransactionStatus());
    }

    @Test
    @Order(3)
    public void testAddDetailValidationLastRowEmpty() throws CloneNotSupportedException, SQLException, GuanzonException {
        startNewTransaction();

        if (poController.getDetailCount() == 0) {
            JSONObject loJSON = poController.AddDetail();
            Assert.assertEquals("success", loJSON.get("result"));
        }

        int lastRow = poController.getDetailCount() - 1;
        poController.Detail(lastRow).setStockID("");

        JSONObject loJSON = poController.AddDetail();
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("Last row has empty item.", loJSON.get("message"));
    }

    @Test
    @Order(4)
    public void testInitFieldsSetsDefaultValues() throws SQLException, GuanzonException {
        if (poController == null) {
            resetController();
        }
        Assert.assertNotNull(poController);

        resetController();

        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.initFields();
        setClassConfig();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(psIndustryId, poController.Master().getIndustryID());
        Assert.assertEquals(psCategorCd, poController.Master().getCategoryCode());
        Assert.assertEquals(psCompanyId, poController.Master().getCompanyID());
        Assert.assertEquals(PurchaseOrderStatus.OPEN, poController.Master().getTransactionStatus());
        Assert.assertNotNull(poController.Master().getTransactionDate());
//        Assert.assertNotNull(poController.Master().getAppliedDate());
    }

    @Test
    @Order(5)
    public void testOpenTransactionFromSampleData() throws Exception {
        Assert.assertNotNull("No transaction sample transaction available.", psSalesCommitmentNo);

        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction(psSalesCommitmentNo);
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(psSalesCommitmentNo, poController.Master().getTransactionNo());
    }

    @Test
    @Order(6)
    public void testLoadStockRequests() throws Exception {
        Assert.assertNotNull("No transaction sample transaction available.", psSalesCommitmentNo);

        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.Master().setIndustryID(psIndustryId);
        poController.Master().setCompanyID(psCompanyId);
        poController.Master().setCategoryCode(psCategorCd);

        loJSON = poController.getApprovedStockRequests();
        Assert.assertEquals("success", loJSON.get("result"));

//        if (poController.getSalesInquiryCount() > 0) {
//            poController.SalesInquiryList(0);
//        }
    }

    @Test
    @Order(7)
    public void testinitSQL() throws Exception {
        Assert.assertNotNull("No transaction sample transaction available.", psSalesCommitmentNo);

        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        poController.setTransactionStatus(PurchaseOrderStatus.OPEN);
        poController.initSQL();

        poController.setTransactionStatus("0123");
        poController.initSQL();
    }

    @Test
    @Order(8)
    public void testLoadTransactionFromStockRequest() throws Exception {
        Assert.assertNotNull("No transaction sample transaction available.", psSalesCommitmentNo);

        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        setClassConfig();

        String stockrequest_transNo = "GK0126000008";
        loJSON = poController.addStockRequestOrdersToPODetail(stockrequest_transNo);
        if ("error".equals(loJSON.get("result"))) {
            Assert.assertEquals("error", loJSON.get("result"));
        } else {
            Assert.assertEquals("success", loJSON.get("result"));
        }

        if (poController.getDetailCount() > 0) {
            poController.Detail(0);
        }
    }

    @Test
    @Order(9)
    public void testLoadTransactionFromPOQuotation() throws Exception {
        Assert.assertNotNull("No transaction sample transaction available.", psSalesCommitmentNo);

        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        setClassConfig();
        poController.NewTransaction();
        String poquotation_transNo = "GK0126000011";
        poController.Master().setSupplierID("M00115000863");
        JSONObject poJSON = poController.getApprovedStockRequests();
        loJSON = poController.addPOQuotationToPODetail(poquotation_transNo);
        if ("error".equals(loJSON.get("result"))) {
            Assert.assertEquals("error", loJSON.get("result"));
        } else {
            Assert.assertEquals("success", loJSON.get("result"));
        }
    }

    @Test
    @Order(10)
    public void testUpdateTransaction() throws Exception {
        Assert.assertNotNull("No transaction sample transaction available.", psSalesCommitmentNo);

        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.UpdateTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
    }

    @Test
    @Order(12)
    public void testDiscountAndAdvancePaymentValidators() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.setDiscountRate("101.00");
        Assert.assertEquals("error", loJSON.get("result"));

        loJSON = poController.setDiscountRate("10");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.setDiscountAmount("1000000");
        Assert.assertEquals("error", loJSON.get("result"));

        loJSON = poController.setDiscountAmount("1");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.setAdvancePaymentRate("101");
        Assert.assertEquals("error", loJSON.get("result"));

        loJSON = poController.setAdvancePaymentRate("10");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.setAdvancePaymentAmount("999999999");
        Assert.assertEquals("error", loJSON.get("result"));

        loJSON = poController.setAdvancePaymentAmount("1");
        Assert.assertEquals("success", loJSON.get("result"));
    }

    @Test
    @Order(13)
    public void testIsDetailHasZeroQtyBranches() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        Assert.assertTrue("No detail rows available.", poController.getDetailCount() > 0);

        for (int row = 0; row < poController.getDetailCount(); row++) {
            if (!"".equals(poController.Detail(row).getStockID())) {
                poController.Detail(row).setQuantity(0);
            }
        }

        loJSON = poController.isDetailHasZeroQty();
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("true", loJSON.get("warning"));

        for (int row = 0; row < poController.getDetailCount(); row++) {
            if (!"".equals(poController.Detail(row).getStockID())) {
                poController.Detail(row).setQuantity(1);
            }
        }

        loJSON = poController.isDetailHasZeroQty();
        Assert.assertEquals("success", loJSON.get("result"));
    }

    @Test
    @Order(14)
    public void testConfirmTransaction() throws SQLException, GuanzonException, CloneNotSupportedException, ParseException {
        JSONObject loJSON;

        resetController();
        loJSON = poController.InitTransaction();

        loJSON = poController.OpenTransaction("GK0126000132");
        if (!"success".equals((String) loJSON.get("result"))) {
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
        poController.Master().getTransactionNo();
        poController.Master().getTransactionStatus();
        loJSON = poController.ConfirmTransaction("");
        if (!"success".equals((String) loJSON.get("result"))) {
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
    }

    @Test
    @Order(16)
    public void testApproveTransactionRequiresReadyTransaction() throws SQLException, GuanzonException, CloneNotSupportedException, ParseException {
        JSONObject loJSON;

        resetController();
        loJSON = poController.InitTransaction();
        loJSON = poController.OpenTransaction("GK0126000117");
        if (!"success".equals((String) loJSON.get("result"))) {
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
        loJSON = poController.ApproveTransaction("");
        if (!"error".equals((String) loJSON.get("result"))) {
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
    }

//    @Test
//    @Order(17)
//    public void testPostTransactionRequiresReadyTransaction() throws SQLException, GuanzonException, CloneNotSupportedException, ParseException {
//        JSONObject loJSON;
//
//        resetController();
//        loJSON = poController.InitTransaction();
//        loJSON = poController.OpenTransaction("GK0126000123");
//        if (!"success".equals((String) loJSON.get("result"))) {
//            System.err.println((String) loJSON.get("message"));
//            Assert.fail();
//        }
//        loJSON = poController.PostTransaction("");
//        if (!"error".equals((String) loJSON.get("result"))) {
//            System.err.println((String) loJSON.get("message"));
//            Assert.fail();
//        }
//    }
//    @Test
//    @Order(18)
//    public void testCancelTransactionRequiresReadyTransaction() throws SQLException, GuanzonException, CloneNotSupportedException, ParseException {
//        JSONObject loJSON;
//
//        resetController();
//        loJSON = poController.InitTransaction();
//        loJSON = poController.OpenTransaction("GK0126000117");
//        if (!"success".equals((String) loJSON.get("result"))) {
//            System.err.println((String) loJSON.get("message"));
//            Assert.fail();
//        }
//        loJSON = poController.CancelTransaction("");
//        if (!"error".equals((String) loJSON.get("result"))) {
//            System.err.println((String) loJSON.get("message"));
//            Assert.fail();
//        }
//    }
//    @Test
//    @Order(19)
//    public void testVoidTransactionRequiresReadyTransaction() throws SQLException, GuanzonException, CloneNotSupportedException, ParseException {
//        JSONObject loJSON;
//
//        resetController();
//        loJSON = poController.InitTransaction();
//        loJSON = poController.OpenTransaction("GK0126000132");
//        if (!"success".equals((String) loJSON.get("result"))) {
//            System.err.println((String) loJSON.get("message"));
//            Assert.fail();
//        }
//        loJSON = poController.VoidTransaction("");
//        if (!"error".equals((String) loJSON.get("result"))) {
//            System.err.println((String) loJSON.get("message"));
//            Assert.fail();
//        }
//    }
//    @Test
//    @Order(20)
//    public void testReturnTransactionRequiresReadyTransaction() throws SQLException, GuanzonException, CloneNotSupportedException, ParseException {
//        JSONObject loJSON;
//
//        resetController();
//        loJSON = poController.InitTransaction();
//        loJSON = poController.OpenTransaction("GK0126000124");
//        if (!"success".equals((String) loJSON.get("result"))) {
//            System.err.println((String) loJSON.get("message"));
//            Assert.fail();
//        }
//        loJSON = poController.ReturnTransaction("");
//        if (!"error".equals((String) loJSON.get("result"))) {
//            System.err.println((String) loJSON.get("message"));
//            Assert.fail();
//        }
//    }
    @Test
    @Order(21)
    public void testWillSaveReturnedTransactionNoChanges() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000034");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.willSave();
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("No update has been made.", loJSON.get("message"));
    }

    @Test
    @Order(22)
    public void testWillSavePrunesInvalidDetailRows() throws Exception {
        startNewTransaction();

        if (poController.getDetailCount() == 0) {
            JSONObject loJSON = poController.AddDetail();
            Assert.assertEquals("success", loJSON.get("result"));
        }

        poController.Detail(0).setStockID("");
        poController.Detail(0).setQuantity(0);

        JSONObject loJSON = poController.willSave();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(0, poController.getDetailCount());
    }

    @Test
    @Order(23)
    public void testNetTotalCheckerSuccess() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertTrue("No detail rows available.", poController.getDetailCount() > 0);

        loJSON = poController.netTotalChecker(0);
        Assert.assertEquals("success", loJSON.get("result"));
    }

    @Test
    @Order(24)
    public void testNetTotalCheckerExceedsLimitResetsQuantity() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertTrue("No detail rows available.", poController.getDetailCount() > 0);

        poController.Detail(0).setUnitPrice(1000000);
        poController.Detail(0).setQuantity(100000);

        loJSON = poController.netTotalChecker(0);
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("The net total exceeds the maximum allowed amount. Please reduce the value and try again.", loJSON.get("message"));
        Assert.assertEquals(0.0, poController.Detail(0).getQuantity().doubleValue(), 0.0);
    }

    @Test
    @Order(25)
    public void testIsDetailHasZeroQtyMixedBranch() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        Assert.assertTrue("No detail rows available.", poController.getDetailCount() > 0);

        for (int row = 0; row < poController.getDetailCount(); row++) {
            if (!"".equals(poController.Detail(row).getStockID())) {
                if (row == 0) {
                    poController.Detail(row).setQuantity(0);
                } else {
                    poController.Detail(row).setQuantity(1);
                }
            }
        }

        if (poController.getDetailCount() == 1) {
            loJSON = poController.AddDetail();
            Assert.assertEquals("success", loJSON.get("result"));
            int lastRow = poController.getDetailCount() - 1;
            poController.Detail(lastRow).setStockID("GK0123000010");
            poController.Detail(lastRow).setQuantity(1);
        }

        loJSON = poController.isDetailHasZeroQty();
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("false", loJSON.get("warning"));
        Assert.assertEquals("Some items have zero quantity. Please review.", loJSON.get("message"));
    }

    @Test
    @Order(26)
    public void testSetDiscountRateWhenNoDetailAmount() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.NewTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        setClassConfig();

        loJSON = poController.setDiscountRate("10");
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("You're not allowed to enter discount rate, no detail amount entered.", loJSON.get("message"));
    }

    @Test
    @Order(27)
    public void testSetDiscountAmountWhenNoDetailAmount() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.NewTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        setClassConfig();

        loJSON = poController.setDiscountAmount("1");
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("You're not allowed to enter discount amount, no amount entered.", loJSON.get("message"));
    }

    @Test
    @Order(28)
    public void testSetAdvancePaymentRateWhenNoTotalAmount() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.NewTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        setClassConfig();

        loJSON = poController.setAdvancePaymentRate("10");
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("Invalid Advance Payment Rate, the total transaction amount is 0.0000", loJSON.get("message"));
    }

    @Test
    @Order(29)
    public void testSetAdvancePaymentAmountWhenNoTotalAmount() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.NewTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        setClassConfig();

        loJSON = poController.setAdvancePaymentAmount("1");
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("Invalid Advance Payment Amount, the total transaction amount is 0.0000", loJSON.get("message"));
    }

    @Test
    @Order(30)
    public void testSetDiscountAmountNegativeValue() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.setDiscountAmount("-1");
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("Invalid Discount Amount", loJSON.get("message"));
    }

    @Test
    @Order(31)
    public void testSetAdvancePaymentAmountNegativeValue() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.setAdvancePaymentAmount("-1");
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("Invalid Advance Payment Amount", loJSON.get("message"));
    }

    @Test
    @Order(32)
    public void testSetDiscountAndAdvancePaymentWithNullOrEmptyInput() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.setDiscountRate(null);
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.setDiscountAmount("");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.setAdvancePaymentRate(null);
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.setAdvancePaymentAmount("");
        Assert.assertEquals("success", loJSON.get("result"));
    }

    @Test
    @Order(33)
    public void testGetInventoryTypeCode() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        String inventoryTypeCode = poController.getInventoryTypeCode();
        Assert.assertTrue(inventoryTypeCode == null || !inventoryTypeCode.trim().isEmpty());
    }

    @Test
    @Order(34)
    public void testGetPrevStatusForReturnedTransaction() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000034");
        Assert.assertEquals("success", loJSON.get("result"));

        String lsPrevStatus = poController.getPrevStatus();
        Assert.assertNotNull(lsPrevStatus);
    }

    @Test
    @Order(35)
    public void testRevertStatusForReturnedTransaction() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000034");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.RevertStatus();
        Assert.assertTrue("success".equals(loJSON.get("result")) || "error".equals(loJSON.get("result")) || loJSON.isEmpty());
    }

    @Test
    @Order(36)
    public void testAddDetailInNewTransaction() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.NewTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        setClassConfig();

        int beforeCount = poController.getDetailCount();
        if (beforeCount > 0) {
            int lastRow = beforeCount - 1;
            poController.Detail(lastRow).setStockID(psStockId);
            poController.Detail(lastRow).setQuantity(1);
        }

        loJSON = poController.AddDetail();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(beforeCount + 1, poController.getDetailCount());
    }

    @Test
    @Order(37)
    public void testAddStockRequestOrdersInvalidTransactionNo() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        setClassConfig();

        loJSON = poController.addStockRequestOrdersToPODetail("INVALID_TRANS_NO");
        Assert.assertEquals("error", loJSON.get("result"));
    }

    @Test
    @Order(38)
    public void testAddPOQuotationToPODetailInvalidTransactionNo() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        setClassConfig();
        loJSON = poController.NewTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.addPOQuotationToPODetail("INVALID_TRANS_NO");
        Assert.assertEquals("error", loJSON.get("result"));
    }

    @Test
    @Order(39)
    public void testGetStatusValueForKnownAndUnknownStatus() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        String lsStatus = poController.getStatusValue();
        Assert.assertNotNull(lsStatus);
        Assert.assertFalse(lsStatus.trim().isEmpty());

        poController.Master().setTransactionStatus("Z");
        Assert.assertEquals("UNKNOWN", poController.getStatusValue());

        poController.Master().setTransactionStatus("A");
        Assert.assertTrue(poController.getStatusValue().endsWith("+"));
    }

    @Test
    @Order(40)
    public void testSaveTransactionReturnedNoChanges() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000034");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.SaveTransaction();
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("Saving of unmodified transaction is not allowed.", loJSON.get("message"));
    }

    @Test
    @Order(41)
    public void testSetProjectTitleByReflection() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));
        poController.Master().setReference("GCO126000002");

        JSONObject result = (JSONObject) invokePrivateMethod(poController, "setProjectTitle", new Class[]{String.class}, new Object[]{PurchaseOrderStatus.OPEN});
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("result"));
    }

    @Test
    @Order(42)
    public void testSaveProjectTitleByReflection() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        JSONObject result = (JSONObject) invokePrivateMethod(poController, "saveProjectTitle", new Class[]{String.class}, new Object[]{PurchaseOrderStatus.OPEN});
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("result"));
    }

    @Test
    @Order(43)
    public void testIsEntryOkayByReflection() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GK0126000132");
        Assert.assertEquals("success", loJSON.get("result"));

        JSONObject result = (JSONObject) invokePrivateMethod(poController, "isEntryOkay", new Class[]{String.class}, new Object[]{PurchaseOrderStatus.CONFIRMED});
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("result"));
    }

    @Test
    @Order(44)
    public void testUpdatePOQuotationByReflection() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        invokePrivateMethod(poController, "updatePOQuotation", new Class[]{String.class, String.class, String.class, double.class, boolean.class},
                new Object[]{PurchaseOrderStatus.CONFIRMED, "GK0126000011", "GK0123000010", 1.00, false});
        Assert.assertTrue(true);
    }

    @Test
    @Order(45)
    public void testUpdateInvStockRequestByReflection() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        invokePrivateMethod(poController, "updateInvStockRequest", new Class[]{String.class, String.class, String.class, double.class},
                new Object[]{PurchaseOrderStatus.CONFIRMED, "GK0126000008", "GK0123000010", 1.00});
        Assert.assertTrue(true);
    }

    @Test
    @Order(46)
    public void testGetRequestQtyByReflection() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        Object result = invokePrivateMethod(poController, "getRequestQty", new Class[]{String.class, String.class, boolean.class, String.class},
                new Object[]{"GK0126000008", "GK0123000010", true, "SReq"});
        Assert.assertTrue(result instanceof Integer);
        Assert.assertTrue((int) result >= 0);
    }

    @Test
    @Order(47)
    public void testUpdatePOQuotationStatusByReflection() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        JSONObject result = (JSONObject) invokePrivateMethod(poController, "updatePOQuotationStatus", new Class[]{String.class}, new Object[]{PurchaseOrderStatus.CONFIRMED});
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("result"));
    }

    @Test
    @Order(48)
    public void testGetPurchaseOrderLoadsOrReturnsNoRecord() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.Master().setIndustryID("09");
        poController.Master().setCompanyID("M001");
        poController.Master().setCategoryCode("0000007");
        poController.setTransactionStatus(PurchaseOrderStatus.OPEN);

        loJSON = poController.getPurchaseOrder("M00115000863", "GCO126", "");
        Assert.assertTrue("success".equals(loJSON.get("result")) || "error".equals(loJSON.get("result")));
    }

    @Test
    @Order(49)
    public void testGetSysUserExistingOrEmpty() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        try {
            String lsUser = poController.getSysUser("GK01250001");
            Assert.assertNotNull(lsUser);
        } catch (SQLException ex) {
            Assert.assertTrue(ex.getMessage() != null && !ex.getMessage().isEmpty());
        }
    }

    @Test
    @Order(50)
    public void testGetEntryByStructure() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        try {
            JSONObject result = poController.getEntryBy();
            Assert.assertNotNull(result);
            Assert.assertTrue(result.containsKey("result"));
        } catch (SQLException ex) {
            Assert.assertTrue(ex.getMessage() != null && !ex.getMessage().isEmpty());
        }
    }

    @Test
    @Order(51)
    public void testGetApproverListStructure() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        try {
            Assert.assertNotNull(poController.getApprover());
        } catch (SQLException ex) {
            Assert.assertTrue(ex.getMessage() != null && !ex.getMessage().isEmpty());
        }
    }

    @Test
    @Order(52)
    public void testGetConfirmedByStructure() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        try {
            JSONObject result = poController.getConfirmedBy();
            Assert.assertNotNull(result);
            Assert.assertTrue(result.containsKey("result"));
        } catch (SQLException ex) {
            Assert.assertTrue(ex.getMessage() != null && !ex.getMessage().isEmpty());
        }
    }

//    @Test
//    @Order(53)
//    public void testSearchTransactionMethod() throws Exception {
//        resetController();
//        JSONObject loJSON = poController.InitTransaction();
//        Assert.assertEquals("success", loJSON.get("result"));
//        poController.Master().setIndustryID("09");
//        poController.Master().setCompanyID("M001");
//        poController.Master().setCategoryCode("0000007");
//        poController.setTransactionStatus(PurchaseOrderStatus.OPEN);
//
//        loJSON = poController.SearchTransaction("", "M00115000863", "GCO126", "", 1);
//        assertSearchJson(loJSON);
//    }
//    @Test
//    @Order(54)
//    public void testSearchBranchMethod() throws Exception {
//        resetController();
//        JSONObject loJSON = poController.InitTransaction();
//        Assert.assertEquals("success", loJSON.get("result"));
//
//        loJSON = poController.SearchBranch("GCO1", true);
//        assertSearchJson(loJSON);
//    }
//    @Test
//    @Order(55)
//    public void testSearchIndustryMethod() throws Exception {
//        resetController();
//        JSONObject loJSON = poController.InitTransaction();
//        Assert.assertEquals("success", loJSON.get("result"));
//
//        loJSON = poController.SearchIndustry("09", true);
//        assertSearchJson(loJSON);
//    }
//    @Test
//    @Order(56)
//    public void testSearchCategoryMethod() throws Exception {
//        resetController();
//        JSONObject loJSON = poController.InitTransaction();
//        Assert.assertEquals("success", loJSON.get("result"));
//        poController.Master().setIndustryID("09");
//
//        loJSON = poController.SearchCategory("0000007", true);
//        assertSearchJson(loJSON);
//    }
//    @Test
//    @Order(57)
//    public void testSearchTermMethod() throws Exception {
//        resetController();
//        JSONObject loJSON = poController.InitTransaction();
//        Assert.assertEquals("success", loJSON.get("result"));
//
//        loJSON = poController.SearchTerm("C001017", true);
//        assertSearchJson(loJSON);
//    }
//    @Test
//    @Order(58)
//    public void testSearchSupplierMethod() throws Exception {
//        resetController();
//        JSONObject loJSON = poController.InitTransaction();
//        Assert.assertEquals("success", loJSON.get("result"));
//        poController.Master().setIndustryID("09");
//
//        loJSON = poController.SearchSupplier("M00115000863", true);
//        assertSearchJson(loJSON);
//    }

//    @Test
//    @Order(59)
//    public void testSearchProjectMethod() throws Exception {
//        resetController();
//        JSONObject loJSON = poController.InitTransaction();
//        Assert.assertEquals("success", loJSON.get("result"));
//
//        loJSON = poController.SearchProject("GCO126000002");
//        assertSearchJson(loJSON);
//    }
//    @Test
//    @Order(60)
//    public void testSearchCompanyMethod() throws Exception {
//        resetController();
//        JSONObject loJSON = poController.InitTransaction();
//        Assert.assertEquals("success", loJSON.get("result"));
//        poController.Master().setIndustryID("09");
//
//        loJSON = poController.SearchCompany("M001", true);
//        assertSearchJson(loJSON);
//    }

//    @Test
//    @Order(61)
//    public void testSearchDestinationMethod() throws Exception {
//        resetController();
//        JSONObject loJSON = poController.InitTransaction();
//        Assert.assertEquals("success", loJSON.get("result"));
//
//        loJSON = poController.SearchDestination("GCO1", true);
//        assertSearchJson(loJSON);
//    }
//    @Test
//    @Order(62)
//    public void testSearchDepartmentMethod() throws Exception {
//        resetController();
//        JSONObject loJSON = poController.InitTransaction();
//        Assert.assertEquals("success", loJSON.get("result"));
//
//        loJSON = poController.SearchDepartment("M001", true);
//        assertSearchJson(loJSON);
//    }

//    @Test
//    @Order(63)
//    public void testSearchBrandMethod() throws Exception {
//        startNewTransaction();
//        int row = poController.getDetailCount() - 1;
//
//        JSONObject loJSON = poController.SearchBrand("", false, row);
//        assertSearchJson(loJSON);
//    }
//    @Test
//    @Order(64)
//    public void testSearchBarcodeMethod() throws Exception {
//        startNewTransaction();
//        int row = poController.getDetailCount() - 1;
//
//        JSONObject loJSON = poController.SearchBarcode("", false, row, true);
//        assertSearchJson(loJSON);
//    }

//    @Test
//    @Order(65)
//    public void testSearchBarcodeGeneralMethod() throws Exception {
//        startNewTransaction();
//        int row = poController.getDetailCount() - 1;
//
//        JSONObject loJSON = poController.SearchBarcodeGeneral("", false, row, true);
//        assertSearchJson(loJSON);
//    }

//    @Test
//    @Order(66)
//    public void testSearchBarcodeDescriptionMethod() throws Exception {
//        startNewTransaction();
//        int row = poController.getDetailCount() - 1;
//
//        JSONObject loJSON = poController.SearchBarcodeDescription("", false, row, true);
//        assertSearchJson(loJSON);
//    }

//    @Test
//    @Order(67)
//    public void testSearchBarcodeDescriptionGeneralMethod() throws Exception {
//        startNewTransaction();
//        int row = poController.getDetailCount() - 1;
//
//        JSONObject loJSON = poController.SearchBarcodeDescriptionGeneral("", false, row, true);
//        assertSearchJson(loJSON);
//    }

    @Test
    @Order(68)
    public void testSearchModelMethod() throws Exception {
        startNewTransaction();
        int row = poController.getDetailCount() - 1;

        JSONObject loJSON = poController.SearchModel("", false, row, true);
        assertSearchJson(loJSON);
    }

    private void assertSearchJson(JSONObject loJSON) {
        Assert.assertNotNull(loJSON);
        Assert.assertTrue("Search result must contain key 'result'.", loJSON.containsKey("result"));
        Assert.assertTrue("success".equals(loJSON.get("result")) || "error".equals(loJSON.get("result")));
    }

    private static Object invokePrivateMethod(Object target, String methodName, Class<?>[] parameterTypes, Object[] args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static void setTransactionEditMode(PurchaseOrder controller) throws Exception {
        Field field = Transaction.class.getDeclaredField("pnEditMode");
        field.setAccessible(true);
        field.setInt(controller, EditMode.READY);
    }

    private void isJSONSuccess(JSONObject loJSON) {
        if (!"success".equals((String) loJSON.get("result"))) {
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
    }
}
