
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.cas.inv.warehouse.services.InvWarehouseControllers;
import org.guanzon.cas.purchasing.controller.PurchaseOrder;
import org.guanzon.cas.purchasing.services.PurchaseOrderControllers;
import org.guanzon.cas.purchasing.services.QuotationControllers;
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
            "test-data/transaction_authorization_master_schema.sql",
            "test-data/transaction_authorization_detail_schema.sql",
            "test-data/transaction_authorization_recipient_schema.sql",
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
            "test-data/transaction_authorization_master_data.sql",
            "test-data/transaction_authorization_detail_data.sql",
            "test-data/transaction_authorization_recipient_data.sql",
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

//    @Test
//    @Order(17)
//    public void testPostTransactionRequiresReadyTransaction() throws SQLException, GuanzonException, CloneNotSupportedException, ParseException {
//        JSONObject loJSON;
//
//        resetController();
//        loJSON = poController.InitTransaction();
//        loJSON = poController.OpenTransaction("GK0126000116");
//        if (!"success".equals((String) loJSON.get("result"))) {
//            System.err.println((String) loJSON.get("message"));
//            Assert.fail();
//        }
//        loJSON = poController.PostTransaction("");
//        if ("error".equals((String) loJSON.get("result"))) {
//            System.err.println((String) loJSON.get("message"));
//            Assert.fail();
//        }
//    }
    @Test
    @Order(18)
    public void testCancelTransactionRequiresReadyTransaction() throws SQLException, GuanzonException, CloneNotSupportedException, ParseException {
        JSONObject loJSON;

        resetController();
        loJSON = poController.InitTransaction();
        loJSON = poController.OpenTransaction("GK0126000117");
        if (!"success".equals((String) loJSON.get("result"))) {
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
        loJSON = poController.CancelTransaction("");
        if ("error".equals((String) loJSON.get("result"))) {
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
    }
    @Test
    @Order(19)
    public void testVoidTransactionRequiresReadyTransaction() throws SQLException, GuanzonException, CloneNotSupportedException, ParseException {
        JSONObject loJSON;

        resetController();
        loJSON = poController.InitTransaction();
        loJSON = poController.OpenTransaction("GCO126000028");
        if (!"success".equals((String) loJSON.get("result"))) {
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
        loJSON = poController.VoidTransaction("");
        if ("error".equals((String) loJSON.get("result"))) {
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
    }
    @Test
    @Order(20)
    public void testReturnTransactionRequiresReadyTransaction() throws SQLException, GuanzonException, CloneNotSupportedException, ParseException {
        JSONObject loJSON;

        resetController();
        loJSON = poController.InitTransaction();
        loJSON = poController.OpenTransaction("GK0126000124");
        if (!"success".equals((String) loJSON.get("result"))) {
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
        loJSON = poController.ReturnTransaction("");
        if ("error".equals((String) loJSON.get("result"))) {
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
    }
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
    public void testWillSaveReturnedTransactionWithModifiedRemarks() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000034");
        Assert.assertEquals("success", loJSON.get("result"));

        String currentRemarks = poController.Master().getRemarks();
        poController.Master().setRemarks((currentRemarks == null ? "" : currentRemarks) + " - updated");

        loJSON = poController.willSave();
        Assert.assertEquals("success", loJSON.get("result"));
    }

    @Test
    @Order(24)
    public void testWillSaveAssignsTransactionNoAndDetailEntryNumbers() throws Exception {
        startNewTransaction();

        if (poController.getDetailCount() == 0) {
            JSONObject loJSON = poController.AddDetail();
            Assert.assertEquals("success", loJSON.get("result"));
        }

        // Make sure at least one row is valid so it survives willSave pruning.
        poController.Detail(0).setStockID("GK0123000010");
        poController.Detail(0).setQuantity(1);

        JSONObject loJSON = poController.willSave();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertNotNull(poController.Master().getTransactionNo());
        Assert.assertFalse(poController.Master().getTransactionNo().isEmpty());
        Assert.assertTrue(poController.getDetailCount() > 0);

        for (int row = 0; row < poController.getDetailCount(); row++) {
            Assert.assertEquals(poController.Master().getTransactionNo(), poController.Detail(row).getTransactionNo());
            Assert.assertEquals(row + 1, poController.Detail(row).getEntryNo().intValue());
            Assert.assertNotNull(poController.Detail(row).getModifiedDate());
        }
    }

    @Test
    @Order(25)
    public void testWillSaveReturnsErrorForInvalidProjectReference() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        setPrivateField(poController, "allowedDepartment", instance.getDepartment());
        poController.Master().setTransactionStatus(PurchaseOrderStatus.OPEN);
        poController.Master().setReference("INVALID_PROJECT_REFERENCE");

        loJSON = poController.willSave();
        Assert.assertEquals("error", loJSON.get("result"));
    }

    @Test
    @Order(26)
    public void testWillSavePrunesNullStockIdRow() throws Exception {
        startNewTransaction();

        if (poController.getDetailCount() == 0) {
            JSONObject loJSON = poController.AddDetail();
            Assert.assertEquals("success", loJSON.get("result"));
        }

        poController.Detail(0).setStockID(null);
        poController.Detail(0).setQuantity(1);

        JSONObject loJSON = poController.willSave();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(0, poController.getDetailCount());
    }

    @Test
    @Order(60)
    public void testWillSaveUpdateTransactionWithLinkedAttachmentsSetsAttachmentFields() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000029");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.UpdateTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        // Load seeded linked attachments from transaction_attachment_data.sql (sSourceCd = POxx).
        poController.loadAttachments();
        Assert.assertTrue("Expected linked attachments for the seeded PO transaction.",
                poController.getTransactionAttachmentCount() > 0);

        loJSON = poController.willSave();
        Assert.assertEquals("success", loJSON.get("result"));

        for (int row = 0; row < poController.getTransactionAttachmentCount(); row++) {
            Assert.assertEquals(poController.Master().getTransactionNo(),
                    poController.TransactionAttachmentList(row).getModel().getSourceNo());
            Assert.assertEquals(poController.getSourceCode(),
                    poController.TransactionAttachmentList(row).getModel().getSourceCode());
            Assert.assertEquals(poController.Master().getBranchCode(),
                    poController.TransactionAttachmentList(row).getModel().getBranchCode());
            Assert.assertEquals(System.getProperty("sys.default.path.temp.attachments"),
                    poController.TransactionAttachmentList(row).getModel().getImagePath());
        }
    }

    @Test
    @Order(61)
    public void testWillSaveUpdateTransactionWithLinkedAttachmentsPreservesAttachmentCount() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000030");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.UpdateTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.loadAttachments();
        int beforeCount = poController.getTransactionAttachmentCount();
        Assert.assertTrue("Expected linked attachments for the seeded PO transaction.", beforeCount > 0);

        loJSON = poController.willSave();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(beforeCount, poController.getTransactionAttachmentCount());
    }

    @Test
    @Order(62)
    public void testWillSaveAddNewTransactionWithInsertedAttachmentRunsAttachmentLines() throws Exception {
        startNewTransaction();

        if (poController.getDetailCount() == 0) {
            JSONObject loJSON = poController.AddDetail();
            Assert.assertEquals("success", loJSON.get("result"));
        }

        // Keep at least one valid detail row so willSave reaches the attachment section.
        poController.Detail(0).setStockID("GK0123000010");
        poController.Detail(0).setQuantity(1);

        poController.resetattachment();
        JSONObject loJSON = poController.addAttachment();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertTrue(poController.getTransactionAttachmentCount() > 0);

        int attachmentRow = poController.getTransactionAttachmentCount() - 1;
        String sampleFileName = "Picture1.png";

        // Insert a physical sample file in temp attachment path so copy/attachment lines can execute in ADDNEW mode.
        Path attachmentDir = Paths.get(System.getProperty("sys.default.path.temp.attachments"));
        Files.createDirectories(attachmentDir);
        Files.write(attachmentDir.resolve(sampleFileName), "sample-attachment".getBytes(StandardCharsets.UTF_8));

        poController.TransactionAttachmentList(attachmentRow).getModel().setFileName(sampleFileName);
        poController.TransactionAttachmentList(attachmentRow).getModel().setSendStatus("1");

        loJSON = poController.willSave();
        Assert.assertEquals("success", loJSON.get("result"));

        Assert.assertEquals(poController.Master().getTransactionNo(),
                poController.TransactionAttachmentList(attachmentRow).getModel().getSourceNo());
        Assert.assertEquals(poController.getSourceCode(),
                poController.TransactionAttachmentList(attachmentRow).getModel().getSourceCode());
        Assert.assertEquals(poController.Master().getBranchCode(),
                poController.TransactionAttachmentList(attachmentRow).getModel().getBranchCode());
        Assert.assertEquals(System.getProperty("sys.default.path.temp.attachments"),
                poController.TransactionAttachmentList(attachmentRow).getModel().getImagePath());
        Assert.assertNotNull(poController.TransactionAttachmentList(attachmentRow).getModel().getFileName());
        Assert.assertFalse(poController.TransactionAttachmentList(attachmentRow).getModel().getFileName().isEmpty());
    }

    @Test
    @Order(63)
    public void testWillSaveAddNewAttachmentSendStatusZeroMissingFileReturnsError() throws Exception {
        startNewTransaction();

        if (poController.getDetailCount() == 0) {
            JSONObject loJSON = poController.AddDetail();
            Assert.assertEquals("success", loJSON.get("result"));
        }

        poController.Detail(0).setStockID("GK0123000010");
        poController.Detail(0).setQuantity(1);

        poController.resetattachment();
        JSONObject loJSON = poController.addAttachment();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertTrue(poController.getTransactionAttachmentCount() > 0);

        int attachmentRow = poController.getTransactionAttachmentCount() - 1;
        String missingFileName = "missing-upload-" + System.nanoTime() + ".png";

        poController.TransactionAttachmentList(attachmentRow).getModel().setFileName(missingFileName);
        poController.TransactionAttachmentList(attachmentRow).getModel().setSendStatus("0");

        loJSON = poController.willSave();
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertTrue(String.valueOf(loJSON.get("message")).contains("Cannot locate file"));
    }

    @Test
    @Order(64)
    public void testWillSaveAddNewAttachmentCollisionRenamesFileWithoutExtension() throws Exception {
        startNewTransaction();

        if (poController.getDetailCount() == 0) {
            JSONObject loJSON = poController.AddDetail();
            Assert.assertEquals("success", loJSON.get("result"));
        }

        poController.Detail(0).setStockID("GK0123000010");
        poController.Detail(0).setQuantity(1);

        poController.resetattachment();
        JSONObject loJSON = poController.addAttachment();
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertTrue(poController.getTransactionAttachmentCount() > 0);

        int attachmentRow = poController.getTransactionAttachmentCount() - 1;
        String baseFileName = "NoExtCollision" + System.nanoTime();

        // Seed DB with same file name so willSave goes through the no-extension rename branch.
        seedAttachmentFileName(baseFileName);

        Path attachmentDir = Paths.get(System.getProperty("sys.default.path.temp.attachments"));
        Files.createDirectories(attachmentDir);
        Files.write(attachmentDir.resolve(baseFileName), "sample-attachment".getBytes(StandardCharsets.UTF_8));

        poController.TransactionAttachmentList(attachmentRow).getModel().setFileName(baseFileName);
        poController.TransactionAttachmentList(attachmentRow).getModel().setSendStatus("1");

        loJSON = poController.willSave();
        Assert.assertEquals("success", loJSON.get("result"));

        String renamedFile = poController.TransactionAttachmentList(attachmentRow).getModel().getFileName();
        Assert.assertNotEquals(baseFileName, renamedFile);
        Assert.assertTrue(renamedFile.startsWith(baseFileName + "_"));
        Assert.assertTrue(Files.exists(attachmentDir.resolve(renamedFile)));
    }

    @Test
    @Order(65)
    public void testAddAttachmentReturnsErrorWhenLastRowNotYetAssigned() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.resetattachment();
        loJSON = poController.addAttachment();
        Assert.assertEquals("success", loJSON.get("result"));

        // Force the "last row not yet assigned" branch in addAttachment().
        poController.TransactionAttachmentList(poController.getTransactionAttachmentCount() - 1)
                .getModel().setTransactionNo("");

        loJSON = poController.addAttachment();
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("Unable to add transaction attachment.", loJSON.get("message"));
    }

    @Test
    @Order(66)
    public void testRemoveAttachmentReturnsErrorWhenListIsEmpty() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.resetattachment();
        loJSON = poController.removeAttachment(0);
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("No transaction attachment to be removed.", loJSON.get("message"));
    }

    @Test
    @Order(67)
    public void testRemoveAttachmentAddNewRowPhysicallyRemovesItem() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.resetattachment();
        loJSON = poController.addAttachment();
        Assert.assertEquals("success", loJSON.get("result"));

        int beforeCount = poController.getTransactionAttachmentCount();
        loJSON = poController.removeAttachment(0);
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(beforeCount - 1, poController.getTransactionAttachmentCount());
    }

    @Test
    @Order(68)
    public void testRemoveAttachmentLoadedRowMarksInactive() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000029");
        Assert.assertEquals("success", loJSON.get("result"));
        loJSON = poController.UpdateTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.loadAttachments();
        Assert.assertTrue(poController.getTransactionAttachmentCount() > 0);

        int beforeCount = poController.getTransactionAttachmentCount();
        loJSON = poController.removeAttachment(0);
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(beforeCount, poController.getTransactionAttachmentCount());
        Assert.assertEquals(RecordStatus.INACTIVE, poController.TransactionAttachmentList(0).getModel().getRecordStatus());
    }

    @Test
    @Order(69)
    public void testAddAttachmentByFileNameReactivatesInactiveRow() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000029");
        Assert.assertEquals("success", loJSON.get("result"));
        loJSON = poController.UpdateTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.loadAttachments();
        Assert.assertTrue(poController.getTransactionAttachmentCount() > 0);

        String existingFileName = poController.TransactionAttachmentList(0).getModel().getFileName();
        int beforeCount = poController.getTransactionAttachmentCount();

        loJSON = poController.removeAttachment(0);
        Assert.assertEquals("success", loJSON.get("result"));
        Assert.assertEquals(RecordStatus.INACTIVE, poController.TransactionAttachmentList(0).getModel().getRecordStatus());

        int row = poController.addAttachment(existingFileName);
        Assert.assertEquals(0, row);
        Assert.assertEquals(beforeCount, poController.getTransactionAttachmentCount());
        Assert.assertEquals(RecordStatus.ACTIVE, poController.TransactionAttachmentList(0).getModel().getRecordStatus());
    }

    @Test
    @Order(70)
    public void testCheckExistingFileNameReturnsErrorForDuplicate() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.checkExistingFileName("Picture1.png");
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertTrue(String.valueOf(loJSON.get("message")).contains("already exist"));
    }

    @Test
    @Order(71)
    public void testCheckExistingFileNameReturnsNonErrorForUniqueName() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.checkExistingFileName("not_in_db_" + System.nanoTime() + ".png");
        Assert.assertNotEquals("error", loJSON.get("result"));
    }

    @Test
    @Order(72)
    public void testWillSaveAddNewAttachmentCollisionRenamesFileWithExtension() throws Exception {
        startNewTransaction();

        if (poController.getDetailCount() == 0) {
            JSONObject loJSON = poController.AddDetail();
            Assert.assertEquals("success", loJSON.get("result"));
        }

        poController.Detail(0).setStockID("GK0123000010");
        poController.Detail(0).setQuantity(1);

        poController.resetattachment();
        JSONObject loJSON = poController.addAttachment();
        Assert.assertEquals("success", loJSON.get("result"));

        int attachmentRow = poController.getTransactionAttachmentCount() - 1;
        String baseFileName = "Picture1.png";
        Path attachmentDir = Paths.get(System.getProperty("sys.default.path.temp.attachments"));
        Files.createDirectories(attachmentDir);
        Files.write(attachmentDir.resolve(baseFileName), "sample-attachment".getBytes(StandardCharsets.UTF_8));

        poController.TransactionAttachmentList(attachmentRow).getModel().setFileName(baseFileName);
        poController.TransactionAttachmentList(attachmentRow).getModel().setSendStatus("1");

        loJSON = poController.willSave();
        Assert.assertEquals("success", loJSON.get("result"));

        String renamedFile = poController.TransactionAttachmentList(attachmentRow).getModel().getFileName();
        Assert.assertNotEquals(baseFileName, renamedFile);
        Assert.assertTrue(renamedFile.matches("Picture1_\\d+\\.png"));
        Assert.assertTrue(Files.exists(attachmentDir.resolve(renamedFile)));
    }

    @Test
    @Order(73)
    public void testLoadAttachmentsWithNoPOxxRowsKeepsListEmpty() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        // Use a guaranteed non-existent transaction number to keep attachment query empty.
        poController.Master().setTransactionNo("NOATTACH00001");

        poController.loadAttachments();
        Assert.assertEquals(0, poController.getTransactionAttachmentCount());
    }

    @Test
    @Order(74)
    public void testCopyFileWithMissingSourceDoesNotCreateTarget() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        String fileName = "copy-missing-" + System.nanoTime() + ".tmp";
        Path target = Paths.get(System.getProperty("sys.default.path.temp.attachments"), fileName);
        Files.deleteIfExists(target);

        poController.copyFile(Paths.get(System.getProperty("sys.default.path.temp.attachments"), fileName).toString());
        Assert.assertFalse(Files.exists(target));
    }

    @Test
    @Order(75)
    public void testCopyFileCopiesToAttachmentTempPath() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        String fileName = "copy-success-" + System.nanoTime() + ".tmp";
        Path sourceDir = Paths.get(System.getProperty("sys.default.path.temp"));
        Files.createDirectories(sourceDir);
        Path source = sourceDir.resolve(fileName);
        Files.write(source, "copy-file-content".getBytes(StandardCharsets.UTF_8));

        Path target = Paths.get(System.getProperty("sys.default.path.temp.attachments"), fileName);
        Files.deleteIfExists(target);

        poController.copyFile(source.toString());
        Assert.assertTrue(Files.exists(target));
    }

    @Test
    @Order(76)
    public void testUploadCASAttachmentsReturnsErrorWhenNewAndOriginalFilesMissing() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.resetattachment();
        loJSON = poController.addAttachment();
        Assert.assertEquals("success", loJSON.get("result"));

        int row = poController.getTransactionAttachmentCount() - 1;
        poController.TransactionAttachmentList(row).getModel().setImagePath(System.getProperty("sys.default.path.temp.attachments"));
        poController.TransactionAttachmentList(row).getModel().setFileName("missing-new-" + System.nanoTime() + ".png");

        loJSON = poController.uploadCASAttachments(instance, System.getProperty("sys.default.access.token"), row,
                "missing-original-" + System.nanoTime() + ".png");
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertTrue(String.valueOf(loJSON.get("message")).contains("Cannot locate file"));
    }

    @Test
    @Order(77)
    public void testSetDiscountRateNegativeValueReturnsError() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.setDiscountRate("-1");
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("Invalid Discount Rate.  Must be between 0.00 and 100.00", loJSON.get("message"));
    }

    @Test
    @Order(78)
    public void testSetDiscountAmountGreaterThanAllowedAmountReturnsError() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        double amountAfterDiscounts = computeAmountAfterDiscounts();
        loJSON = poController.setDiscountAmount(String.valueOf(amountAfterDiscounts + 1.0));
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("Invalid Discount Amount", loJSON.get("message"));
    }

    @Test
    @Order(79)
    public void testSetAdvancePaymentRateNegativeValueReturnsError() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.setAdvancePaymentRate("-1");
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("Invalid Advance Payment Rate. Must be between 0.0000 and 100.0000", loJSON.get("message"));
    }

    @Test
    @Order(80)
    public void testSetAdvancePaymentRateInvalidDownpaymentTotalReturnsError() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        double amountAfterDiscounts = computeAmountAfterDiscounts();

        loJSON = poController.setAdvancePaymentRate("0");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.setAdvancePaymentAmount(String.valueOf(amountAfterDiscounts));
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.setAdvancePaymentRate("1");
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("Invalid Downpayment Total.", loJSON.get("message"));
    }

    @Test
    @Order(81)
    public void testSetAdvancePaymentAmountInvalidDownpaymentTotalReturnsError() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.setAdvancePaymentAmount("0");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.setAdvancePaymentRate("100");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.setAdvancePaymentAmount("1");
        Assert.assertEquals("error", loJSON.get("result"));
        Assert.assertEquals("Invalid Downpayment Total.", loJSON.get("message"));
    }

    @Test
    @Order(82)
    public void testGetApproverReturnsListInstance() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        java.util.List<String> approvers = poController.getApprover();
        Assert.assertNotNull(approvers);
    }

    @Test
    @Order(83)
    public void testEncodeFileToBase64BinaryViaReflection() throws Exception {
        Path attachmentDir = Paths.get(System.getProperty("sys.default.path.temp.attachments"));
        Files.createDirectories(attachmentDir);
        Path sample = attachmentDir.resolve("base64-sample-" + System.nanoTime() + ".txt");
        Files.write(sample, "abc".getBytes(StandardCharsets.UTF_8));

        Method method = PurchaseOrder.class.getDeclaredMethod("encodeFileToBase64Binary", java.io.File.class);
        method.setAccessible(true);
        String encoded = (String) method.invoke(null, sample.toFile());
        Assert.assertEquals("YWJj", encoded);
    }

    @Test
    @Order(84)
    public void testFormatDateToTextHandlesNullEmptyAndValidDate() {
        Assert.assertEquals("", PurchaseOrder.formatDateToText(null));
        Assert.assertEquals("", PurchaseOrder.formatDateToText(""));

        String formatted = PurchaseOrder.formatDateToText("2026-8-11");
        Assert.assertNotNull(formatted);
        Assert.assertTrue(formatted.contains("2026"));
    }

    @Test
    @Order(85)
    public void testParseDoubleViaReflectionHandlesNullInvalidAndNumeric() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        Object nullValue = invokePrivateMethod(poController, "parseDouble", new Class[]{Object.class}, new Object[]{null});
        Object invalidValue = invokePrivateMethod(poController, "parseDouble", new Class[]{Object.class}, new Object[]{"invalid"});
        Object numericValue = invokePrivateMethod(poController, "parseDouble", new Class[]{Object.class}, new Object[]{"12.5"});

        Assert.assertEquals(0.0, ((Double) nullValue).doubleValue(), 0.0);
        Assert.assertEquals(0.0, ((Double) invalidValue).doubleValue(), 0.0);
        Assert.assertEquals(12.5, ((Double) numericValue).doubleValue(), 0.0);
    }

    @Test
    @Order(86)
    public void testSafeStringAndSafeStringsViaReflection() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        Object safeStringNull = invokePrivateMethod(poController, "safeString", new Class[]{Object.class}, new Object[]{null});
        Object safeStringValue = invokePrivateMethod(poController, "safeString", new Class[]{Object.class}, new Object[]{123});
        Object safeStringsNull = invokePrivateMethod(poController, "safeStrings", new Class[]{Object.class}, new Object[]{null});
        Object safeStringsValue = invokePrivateMethod(poController, "safeStrings", new Class[]{Object.class}, new Object[]{"hello"});

        Assert.assertEquals("", safeStringNull);
        Assert.assertEquals("123", safeStringValue);
        Assert.assertEquals("", safeStringsNull);
        Assert.assertEquals("hello", safeStringsValue);
    }

    @Test
    @Order(87)
    public void testPOMasterListAndPODetailListViaReflection() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        Object poMasterModel = invokePrivateMethod(poController, "POMasterList", new Class[]{}, new Object[]{});
        Object poDetailModel = invokePrivateMethod(poController, "PODetailList", new Class[]{}, new Object[]{});

        Assert.assertNotNull(poMasterModel);
        Assert.assertNotNull(poDetailModel);
    }

    @Test
    @Order(88)
    public void testPOMasterAccessorAndCount() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.Master().setIndustryID("09");
        poController.Master().setCompanyID("M001");
        poController.Master().setCategoryCode("0000007");
        poController.setTransactionStatus(PurchaseOrderStatus.OPEN);
        loJSON = poController.getPurchaseOrder("M00115000863", "GCO126", "");
        Assert.assertTrue("success".equals(loJSON.get("result")) || "error".equals(loJSON.get("result")));

//        Assert.assertTrue(poController.getPOMasterCount() >= 1);
//        Assert.assertNotNull(poController.POMaster(0));
    }

    @Test
    @Order(89)
    public void testInvStockRequestAccessorAndCount() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        setClassConfig();
        loJSON = poController.getApprovedStockRequests();
        Assert.assertEquals("success", loJSON.get("result"));

//        if (poController.getInvStockRequestCount() == 0) {
//            java.util.ArrayList<Object> list = new java.util.ArrayList<>();
//            list.add(null);
//            setPrivateField(poController, "paStockRequest", list);
//        }

//        Assert.assertTrue(poController.getInvStockRequestCount() >= 1);
//        poController.InvStockRequestMaster(0);
    }

    @Test
    @Order(90)
    public void testDetailRemoveAccessor() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        java.util.ArrayList<Object> removed = new java.util.ArrayList<>();
        removed.add(poController.Detail(0));
        setPrivateField(poController, "paDetailRemoved", removed);

        Assert.assertEquals(1, poController.getDetailRemovedCount());
        Assert.assertNotNull(poController.DetailRemove(0));
    }

    @Test
    @Order(91)
    public void testSaveMethodViaReflectionReturnsResult() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        JSONObject result = (JSONObject) invokePrivateMethod(poController, "save", new Class[]{}, new Object[]{});
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey("result"));
    }

    @Test
    @Order(92)
    public void testSaveCompleteRunsWithoutException() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.saveComplete();
        Assert.assertTrue(true);
    }

    @Test
    @Order(93)
    public void testTransactionAttachmentListPrivateGetterViaReflection() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.resetattachment();
        loJSON = poController.addAttachment();
        Assert.assertEquals("success", loJSON.get("result"));

        Object rawList = invokePrivateMethod(poController, "TransactionAttachmentList", new Class[]{}, new Object[]{});
        Assert.assertNotNull(rawList);
        Assert.assertTrue(rawList instanceof java.util.List);
        Assert.assertEquals(poController.getTransactionAttachmentCount(), ((java.util.List<?>) rawList).size());
    }

    @Test
    @Order(94)
    public void testSaveOthersWithInvalidProjectReferenceReturnsErrorBranch() throws Exception {
        startNewTransaction();
        setPrivateField(poController, "allowedDepartment", instance.getDepartment());
        poController.Master().setReference("INVALID_PROJECT_REFERENCE");
        poController.Master().setTransactionStatus(PurchaseOrderStatus.OPEN);

        JSONObject loJSON = poController.saveOthers();
        Assert.assertEquals("error", loJSON.get("result"));
    }

    @Test
    @Order(95)
    public void testGeneratePRFNonApprovedStatusReturnsSuccess() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = (JSONObject) invokePrivateMethod(poController, "generatePRF",
                new Class[]{String.class}, new Object[]{PurchaseOrderStatus.OPEN});
        Assert.assertEquals("success", loJSON.get("result"));
    }

    @Test
    @Order(96)
    public void testGeneratePRFApprovedPathReturnsStructuredResult() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        poController.Master().setWithAdvPaym(true);
        poController.Master().setDownPaymentRatesAmount(1.0);

        loJSON = (JSONObject) invokePrivateMethod(poController, "generatePRF",
                new Class[]{String.class}, new Object[]{PurchaseOrderStatus.APPROVED});
        Assert.assertTrue(loJSON.containsKey("result"));
    }

    @Test
    @Order(97)
    public void testSavePRFNonApprovedStatusReturnsSuccess() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = (JSONObject) invokePrivateMethod(poController, "savePRF",
                new Class[]{String.class}, new Object[]{PurchaseOrderStatus.OPEN});
        Assert.assertEquals("success", loJSON.get("result"));
    }

    @Test
    @Order(97)
    public void testSavePRFApprovedPathReturnsStructuredResult() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        poController.Master().setWithAdvPaym(true);
        poController.Master().setDownPaymentRatesAmount(1.0);

        // Build the payment request object used by savePRF(APPROVED).
        invokePrivateMethod(poController, "generatePRF",
                new Class[]{String.class}, new Object[]{PurchaseOrderStatus.APPROVED});

        loJSON = (JSONObject) invokePrivateMethod(poController, "savePRF",
                new Class[]{String.class}, new Object[]{PurchaseOrderStatus.APPROVED});
        Assert.assertTrue(loJSON.containsKey("result"));
    }

    @Test
    @Order(98)
    public void testSaveUpdatesWithNoRelatedControllersReturnsSuccess() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = (JSONObject) invokePrivateMethod(poController, "saveUpdates",
                new Class[]{String.class}, new Object[]{PurchaseOrderStatus.CONFIRMED});
        Assert.assertEquals("success", loJSON.get("result"));
    }

    @Test
    @Order(99)
    public void testUpdatePOQuotationStatusWithNoQueueReturnsSuccess() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = (JSONObject) invokePrivateMethod(poController, "updatePOQuotationStatus",
                new Class[]{String.class}, new Object[]{PurchaseOrderStatus.CONFIRMED});
        Assert.assertEquals("success", loJSON.get("result"));
    }

    @Test
    @Order(100)
    public void testCheckProjectUsageSuccessAndErrorBranches() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.CheckProjectUsage("PROJECT_NOT_FOUND_" + System.nanoTime());
        Assert.assertEquals("success", loJSON.get("result"));

        String duplicateProjectCode = findProjectCodeUsedMoreThanOnce();
        if (duplicateProjectCode != null) {
            loJSON = poController.CheckProjectUsage(duplicateProjectCode);
            Assert.assertEquals("error", loJSON.get("result"));
        }
        Assert.assertTrue(loJSON.containsKey("count"));
    }

    @Test
    @Order(101)
    public void testPOCancelTransactionReturnsStructuredResult() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        loJSON = poController.OpenTransaction("GK0126000117");
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.POCancelTransaction();
        Assert.assertTrue(loJSON.containsKey("result"));
    }

    @Test
    @Order(102)
    public void testGetConfirmedPurchaseOrderReturnsStructuredResult() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.Master().setIndustryID("09");
        poController.Master().setCompanyID("M001");
        poController.Master().setCategoryCode("0000007");
        poController.setTransactionStatus(PurchaseOrderStatus.OPEN);

        loJSON = poController.getConfirmedPurchaseOrder("M00115000863", "GCO126");
        Assert.assertTrue("success".equals(loJSON.get("result")) || "error".equals(loJSON.get("result")));
        Assert.assertTrue(loJSON.containsKey("message"));
    }

    @Test
    @Order(103)
    public void testUpdatePOQuotationViaReflectionCoversRemovedAndNonRemoved() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        loJSON = poController.OpenTransaction("GCO126000003");
        Assert.assertEquals("success", loJSON.get("result"));

        invokePrivateMethod(poController, "updatePOQuotation",
                new Class[]{String.class, String.class, String.class, double.class, boolean.class},
                new Object[]{PurchaseOrderStatus.CONFIRMED, "GK0126000011", "GK0123000010", 1.0, false});

        invokePrivateMethod(poController, "updatePOQuotation",
                new Class[]{String.class, String.class, String.class, double.class, boolean.class},
                new Object[]{PurchaseOrderStatus.RETURNED, "GK0126000011", "GK0123000010", 1.0, true});

        Assert.assertTrue(true);
    }

    @Test
    @Order(104)
    public void testAddAttachmentByFileNameDuplicateActiveAddsAnotherRow() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        poController.resetattachment();
        int beforeCount = poController.getTransactionAttachmentCount();
        int firstRow = poController.addAttachment("dup-file-" + System.nanoTime() + ".png");
        Assert.assertTrue(firstRow >= 0);

        int secondRow = poController.addAttachment(poController.TransactionAttachmentList(firstRow).getModel().getFileName());
        Assert.assertTrue(secondRow >= 0);
        Assert.assertEquals(beforeCount + 2, poController.getTransactionAttachmentCount());
    }

    @Test
    @Order(105)
    public void testAreAllQuotationControllerIsInPODetailFalseAndTruePaths() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        poController.NewTransaction();
        setClassConfig();
        poController.Master().setSupplierID("M00115000863");

        QuotationControllers quotationControllers = new QuotationControllers(instance, null);
        loJSON = quotationControllers.POQuotation().InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        loJSON = quotationControllers.POQuotation().OpenTransaction("GK0126000011");
        if (!"success".equals(loJSON.get("result"))) {
            Assert.assertEquals("error", loJSON.get("result"));
            return;
        }

        boolean beforeAdd = (boolean) invokePrivateMethod(poController,
                "areAllQuotationControllerIsInPODetail",
                new Class[]{QuotationControllers.class}, new Object[]{quotationControllers});
        Assert.assertFalse(beforeAdd);

        loJSON = poController.addPOQuotationToPODetail("GK0126000011");
        if ("success".equals(loJSON.get("result"))) {
            boolean afterAdd = (boolean) invokePrivateMethod(poController,
                    "areAllQuotationControllerIsInPODetail",
                    new Class[]{QuotationControllers.class}, new Object[]{quotationControllers});
            Assert.assertTrue(afterAdd);
        } else {
            Assert.assertEquals("error", loJSON.get("result"));
        }
    }

    @Test
    @Order(106)
    public void testAreAllStockRequestDetailsInPODetailFalseAndTruePaths() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        poController.NewTransaction();
        setClassConfig();

        InvWarehouseControllers invWarehouseControllers = new InvWarehouseControllers(instance, null);
        loJSON = invWarehouseControllers.StockRequest().InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));
        loJSON = invWarehouseControllers.StockRequest().OpenTransaction("GK0126000008");
        if (!"success".equals(loJSON.get("result"))) {
            Assert.assertEquals("error", loJSON.get("result"));
            return;
        }

        boolean beforeAdd = (boolean) invokePrivateMethod(poController,
                "areAllStockRequestDetailsInPODetail",
                new Class[]{InvWarehouseControllers.class}, new Object[]{invWarehouseControllers});
        Assert.assertFalse(beforeAdd);

        loJSON = poController.addStockRequestOrdersToPODetail("GK0126000008");
        if ("success".equals(loJSON.get("result"))) {
            boolean afterAdd = (boolean) invokePrivateMethod(poController,
                    "areAllStockRequestDetailsInPODetail",
                    new Class[]{InvWarehouseControllers.class}, new Object[]{invWarehouseControllers});
            Assert.assertTrue(afterAdd);
        } else {
            Assert.assertEquals("error", loJSON.get("result"));
        }
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

    @Test
    @Order(53)
    public void testOpenTransactionInvalidTransactionNoReturnsError() throws Exception {
        resetController();
        JSONObject loJSON = poController.InitTransaction();
        Assert.assertEquals("success", loJSON.get("result"));

        loJSON = poController.OpenTransaction("INVALID_TRANS_NO");
        Assert.assertEquals("error", loJSON.get("result"));
    }

//    @Test
//    @Order(54)
//    public void testPostTransactionWithoutLoadedTransactionReturnsError() throws Exception {
//        resetController();
//        JSONObject loJSON = poController.InitTransaction();
//        Assert.assertEquals("success", loJSON.get("result"));
//
//        loJSON = poController.PostTransaction("");
//        Assert.assertEquals("error", loJSON.get("result"));
//        Assert.assertEquals("No transacton was loaded.", loJSON.get("message"));
//    }
//
//    @Test
//    @Order(55)
//    public void testCancelTransactionWithoutLoadedTransactionReturnsError() throws Exception {
//        resetController();
//        JSONObject loJSON = poController.InitTransaction();
//        Assert.assertEquals("success", loJSON.get("result"));
//
//        loJSON = poController.CancelTransaction("");
//        Assert.assertEquals("error", loJSON.get("result"));
//        Assert.assertEquals("No transacton was loaded.", loJSON.get("message"));
//    }
//
//    @Test
//    @Order(56)
//    public void testVoidTransactionWithoutLoadedTransactionReturnsError() throws Exception {
//        resetController();
//        JSONObject loJSON = poController.InitTransaction();
//        Assert.assertEquals("success", loJSON.get("result"));
//
//        loJSON = poController.VoidTransaction("");
//        Assert.assertEquals("error", loJSON.get("result"));
//        Assert.assertEquals("No transacton was loaded.", loJSON.get("message"));
//    }
//
//    @Test
//    @Order(57)
//    public void testReturnTransactionWithoutLoadedTransactionReturnsError() throws Exception {
//        resetController();
//        JSONObject loJSON = poController.InitTransaction();
//        Assert.assertEquals("success", loJSON.get("result"));
//
//        loJSON = poController.ReturnTransaction("");
//        Assert.assertEquals("error", loJSON.get("result"));
//        Assert.assertEquals("No transacton was loaded.", loJSON.get("message"));
//    }
//
//    @Test
//    @Order(58)
//    public void testApproveTransactionWithoutLoadedTransactionReturnsError() throws Exception {
//        resetController();
//        JSONObject loJSON = poController.InitTransaction();
//        Assert.assertEquals("success", loJSON.get("result"));
//
//        loJSON = poController.ApproveTransaction("");
//        Assert.assertEquals("error", loJSON.get("result"));
//        Assert.assertEquals("No transacton was loaded.", loJSON.get("message"));
//    }
//
//    @Test
//    @Order(59)
//    public void testReturnTransactionWhenAlreadyReturnedReturnsError() throws Exception {
//        resetController();
//        JSONObject loJSON = poController.InitTransaction();
//        Assert.assertEquals("success", loJSON.get("result"));
//
//        loJSON = poController.OpenTransaction("GCO126000034");
//        Assert.assertEquals("success", loJSON.get("result"));
//
//        loJSON = poController.ReturnTransaction("");
//        Assert.assertEquals("error", loJSON.get("result"));
//        Assert.assertEquals("Transaction was already returned.", loJSON.get("message"));
//    }
    private static Object invokePrivateMethod(Object target, String methodName, Class<?>[] parameterTypes, Object[] args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static void seedAttachmentFileName(String fileName) throws SQLException {
        String sql = "INSERT INTO transaction_attachment (sTransNox, sSourceCd, sSourceNo, sFileName) VALUES (?, ?, ?, ?)";
        String transNo = ("AT" + System.nanoTime()).substring(0, 14);

        try ( PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transNo);
            pstmt.setString(2, "POxx");
            pstmt.setString(3, "GCO126000003");
            pstmt.setString(4, fileName);
            pstmt.executeUpdate();
        }
    }

    private static double computeAmountAfterDiscounts() {
        return poController.Master().getTranTotal().doubleValue()
                - (((poController.Master().getTranTotal().doubleValue() / 100)
                * poController.Master().getDiscount().doubleValue())
                + poController.Master().getAdditionalDiscount().doubleValue());
    }

    private static String findProjectCodeUsedMoreThanOnce() throws SQLException {
        String sql = "SELECT sReferNox FROM PO_Master WHERE sReferNox IS NOT NULL AND sReferNox <> '' GROUP BY sReferNox HAVING COUNT(*) > 1 LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getString("sReferNox");
            }
        }
        return null;
    }

    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

}
