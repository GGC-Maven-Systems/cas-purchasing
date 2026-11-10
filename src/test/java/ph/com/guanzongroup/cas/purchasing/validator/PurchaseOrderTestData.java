package ph.com.guanzongroup.cas.purchasing.validator;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;

/**
 * Test data for the Purchase Order validator tests.
 *
 * <p>
 * Models and the application driver are Mockito mocks, so no database is
 * needed. Values are converted to whatever type each getter really returns
 * (Double, BigDecimal, Number, java.sql.Date, ...), so the tests do not depend
 * on those details. Requires mockito-inline (or Mockito 5+) if the model
 * classes or their getters are final.</p>
 */
final class PurchaseOrderTestData {

    /** Fixed "today" for every test. */
    static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    private PurchaseOrderTestData() {
    }

    static Date date(LocalDate fdDate) {
        return Date.from(fdDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    // =====================================================================
    // Application driver
    // =====================================================================
    static GRiderCAS driver(String fsDepartment) {
        return driver(fsDepartment, null);
    }

    /** Driver whose server date is TODAY; every query returns foResult (may be null). */
    static GRiderCAS driver(String fsDepartment, ResultSet foResult) {
        GRiderCAS loDriver = mock(GRiderCAS.class);
        stub(loDriver, "getServerDate", date(TODAY));
        stub(loDriver, "getDepartment", fsDepartment);
        try {
            doReturn(foResult).when(loDriver).executeQuery(anyString());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return loDriver;
    }

    // =====================================================================
    // Master
    // =====================================================================
    static MasterBuilder master() {
        return new MasterBuilder();
    }

    /** A valid OPEN purchase order unless changed. */
    static final class MasterBuilder {

        String transNo = "M001PO000001";
        String status = PurchaseOrderStatus.OPEN;
        String supplier = "SUP001";
        String destination = "M001";
        String term = "T030";
        String reference = "REF-001";
        String branch = "M001";
        String remarks = "";
        LocalDate transact = TODAY;
        LocalDate expected = TODAY.plusDays(7);
        int editMode = EditMode.UPDATE;
        boolean withAdvance = false;
        double tranTotal = 1000;
        double netTotal = 1000;
        double discount = 0;
        double addDiscount = 0;
        double advanceRate = 0;
        double advanceAmount = 0;

        MasterBuilder status(String v) { status = v; return this; }
        MasterBuilder supplier(String v) { supplier = v; return this; }
        MasterBuilder destination(String v) { destination = v; return this; }
        MasterBuilder term(String v) { term = v; return this; }
        MasterBuilder reference(String v) { reference = v; return this; }
        MasterBuilder remarks(String v) { remarks = v; return this; }
        MasterBuilder transact(LocalDate v) { transact = v; return this; }
        MasterBuilder expected(LocalDate v) { expected = v; return this; }
        MasterBuilder addNew() { editMode = EditMode.ADDNEW; return this; }
        MasterBuilder tranTotal(double v) { tranTotal = v; netTotal = v; return this; }
        MasterBuilder netTotal(double v) { netTotal = v; return this; }
        MasterBuilder discount(double v) { discount = v; return this; }
        MasterBuilder addDiscount(double v) { addDiscount = v; return this; }
        MasterBuilder advance(double rate, double amount) {
            withAdvance = true;
            advanceRate = rate;
            advanceAmount = amount;
            return this;
        }

        Model_PO_Master build() {
            Model_PO_Master loMaster = mock(Model_PO_Master.class);
            stub(loMaster, "getTransactionNo", transNo);
            stub(loMaster, "getTransactionStatus", status);
            stub(loMaster, "getSupplierID", supplier);
            stub(loMaster, "getDestinationID", destination);
            stub(loMaster, "getTermCode", term);
            stub(loMaster, "getReference", reference);
            stub(loMaster, "getBranchCode", branch);
            stub(loMaster, "getRemarks", remarks);
            stub(loMaster, "getTransactionDate", transact == null ? null : date(transact));
            stub(loMaster, "getExpectedDate", expected == null ? null : date(expected));
            stub(loMaster, "getEditMode", editMode);
            stub(loMaster, "getWithAdvPaym", withAdvance);
            stub(loMaster, "getTranTotal", tranTotal);
            stub(loMaster, "getNetTotal", netTotal);
            stub(loMaster, "getDiscount", discount);
            stub(loMaster, "getAdditionalDiscount", addDiscount);
            stub(loMaster, "getDownPaymentRatesPercentage", advanceRate);
            stub(loMaster, "getDownPaymentRatesAmount", advanceAmount);
            return loMaster;
        }
    }

    // =====================================================================
    // Detail
    // =====================================================================
    /** Manual line (no source document). */
    static Model_PO_Detail detail(String fsStockId, double fnQuantity) {
        return detail(fsStockId, fnQuantity, "");
    }

    static Model_PO_Detail detail(String fsStockId, double fnQuantity, String fsSourceNo) {
        Model_PO_Detail loDetail = mock(Model_PO_Detail.class);
        stub(loDetail, "getStockID", fsStockId);
        stub(loDetail, "getQuantity", fnQuantity);
        stub(loDetail, "getSouceNo", fsSourceNo);
        return loDetail;
    }

    // =====================================================================
    // Stubbing that adapts to the getter's real return type
    // =====================================================================
    private static void stub(Object foMock, String fsGetter, Object foValue) {
        try {
            Method loGetter = foMock.getClass().getMethod(fsGetter);
            Object loValue = convert(foValue, loGetter.getReturnType());
            loGetter.invoke(doReturn(loValue).when(foMock));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Cannot stub " + fsGetter, ex);
        }
    }

    private static Object convert(Object foValue, Class<?> foType) {
        if (foValue == null) {
            return null;
        }
        if (foValue instanceof Double) {
            double lnValue = (Double) foValue;
            if (foType == BigDecimal.class || foType == Number.class || foType == Object.class) {
                return BigDecimal.valueOf(lnValue);
            }
            if (foType == Float.class || foType == float.class) {
                return (float) lnValue;
            }
            if (foType == Integer.class || foType == int.class) {
                return (int) lnValue;
            }
            if (foType == Long.class || foType == long.class) {
                return (long) lnValue;
            }
            return lnValue;
        }
        if (foValue instanceof Date) {
            long lnTime = ((Date) foValue).getTime();
            if (foType == java.sql.Date.class) {
                return new java.sql.Date(lnTime);
            }
            if (foType == java.sql.Timestamp.class) {
                return new java.sql.Timestamp(lnTime);
            }
        }
        return foValue;
    }
}
