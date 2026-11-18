package ph.com.guanzongroup.cas.purchasing.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.status.PurchaseOrderStaticData;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.cashflow.Payee;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;

/**
 * Payment request (PRF) for a PO's advance payment, created when the PO is
 * approved.
 *
 * <ul>
 * <li>{@link #prepare()} builds it before the approval's DB transaction;</li>
 * <li>{@link #save()} saves it (with the PO's attachments) inside it.</li>
 * </ul>
 *
 * Advance = net total x rate% + fixed amount (4 decimals).
 */
public class PurchaseOrderAdvancePayment {

    private static final Logger LOG = Logger.getLogger(PurchaseOrderAdvancePayment.class.getName());
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PurchaseOrder poPO;
    private final GRiderCAS poGRider;
    private final Supplier<CashflowControllers> poCashflow;

    private CashflowControllers poPaymentRequest;
    private String psPayeeId;

    public PurchaseOrderAdvancePayment(PurchaseOrder foPO, GRiderCAS foGRider, Supplier<CashflowControllers> foCashflow) {
        poPO = foPO;
        poGRider = foGRider;
        poCashflow = foCashflow;
    }

    /** Whether the PO asks for an advance payment with a rate or an amount. */
    public boolean isRequired() {
        Model_PO_Master loMaster = poPO.Master();
        return loMaster.getWithAdvPaym()
                && (money(loMaster.getDownPaymentRatesAmount()).signum() > 0
                || money(loMaster.getDownPaymentRatesPercentage()).signum() > 0);
    }

    /** Advance payment amount: net total x rate% + fixed amount. */
    public BigDecimal getAmount() {
        Model_PO_Master loMaster = poPO.Master();
        return money(loMaster.getNetTotal())
                .multiply(money(loMaster.getDownPaymentRatesPercentage()))
                .divide(HUNDRED, 4, RoundingMode.HALF_UP)
                .add(money(loMaster.getDownPaymentRatesAmount()));
    }

    /** Looks up the payee of a supplier. */
    public JSONObject searchPayee(String fsSupplierId) throws SQLException, GuanzonException {
        Payee loPayee = poCashflow.get().Payee();
        loPayee.setRecordStatus("1");

        JSONObject loJSON = loPayee.searchPayee(fsSupplierId);
        if (isSuccess(loJSON)) {
            psPayeeId = (String) loJSON.get("sPayeeIDx");
        }
        return loJSON;
    }

    public String getPayeeId() {
        return psPayeeId;
    }

    /** Builds the payment request when the PO needs one (nothing to do otherwise). */
    public JSONObject prepare() {
        poPaymentRequest = null;
        if (!isRequired()) {
            return success();
        }

        try {
            Model_PO_Master loMaster = poPO.Master();
            CashflowControllers loCashflow = new CashflowControllers(poGRider, null);
            loCashflow.PaymentRequest().InitTransaction();
            loCashflow.PaymentRequest().NewTransaction();

            searchPayee(loMaster.getSupplierID());

            double lnAdvance = getAmount().doubleValue();

            loCashflow.PaymentRequest().Master().setTransactionDate(loMaster.getTransactionDate());
            loCashflow.PaymentRequest().Master().setBranchCode(loMaster.getDestinationID());
            if (poGRider.isMainOffice() || poGRider.isWarehouse()) {
                loCashflow.PaymentRequest().Master().setDepartmentID(poGRider.getDepartment());
            }
            loCashflow.PaymentRequest().Master().setRemarks(loMaster.getRemarks());
            loCashflow.PaymentRequest().Master().setIndustryID(loMaster.getIndustryID());
            loCashflow.PaymentRequest().Master().setCompanyID(loMaster.getCompanyID());
            loCashflow.PaymentRequest().Master().setSourceCode(poPO.getSourceCode());
            loCashflow.PaymentRequest().Master().setSourceNo(loMaster.getTransactionNo());
            loCashflow.PaymentRequest().Master().setPayeeID(psPayeeId);
            loCashflow.PaymentRequest().Master().setEntryNo(1);
            loCashflow.PaymentRequest().Master().setSeriesNo(loCashflow.PaymentRequest().getSeriesNoByBranch());
            loCashflow.PaymentRequest().Master().setTranTotal(lnAdvance);
            loCashflow.PaymentRequest().Master().setNetTotal(lnAdvance);
            loCashflow.PaymentRequest().Master().setTransactionStatus(PurchaseOrderStatus.CONFIRMED);

            loCashflow.PaymentRequest().Detail(0).setEntryNo(1);
            loCashflow.PaymentRequest().Detail(0).setParticularID(PurchaseOrderStaticData.default_empty_string);
            loCashflow.PaymentRequest().Detail(0).setAmount(lnAdvance);
            loCashflow.PaymentRequest().Detail(0).setDiscount(PurchaseOrderStaticData.default_value_double);
            loCashflow.PaymentRequest().Detail(0).setAddDiscount(PurchaseOrderStaticData.default_value_double);
            loCashflow.PaymentRequest().Detail(0).setVatable(PurchaseOrderStaticData.default_value_string);
            loCashflow.PaymentRequest().Detail(0).setWithHoldingTax(PurchaseOrderStaticData.default_value_double);
            loCashflow.PaymentRequest().Detail(0).setPRFRemarks(loMaster.getRemarks());
            loCashflow.PaymentRequest().Detail(0).isReverse(true);
            loCashflow.PaymentRequest().AddDetail();

            poPaymentRequest = loCashflow;
            return success();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Unable to prepare the payment request", ex);
            return error(MiscUtil.getException(ex));
        }
    }

    /** Saves the prepared payment request with the PO's attachments. */
    public JSONObject save() {
        if (poPaymentRequest == null) {
            return success();
        }
        try {
            poPaymentRequest.PaymentRequest().loadPOAttachment(poPO.Master().getTransactionNo());
            poPaymentRequest.PaymentRequest().setWithParent(true);
            JSONObject loJSON = poPaymentRequest.PaymentRequest().SaveTransaction();
            if ("error".equals((String) loJSON.get("result"))) {
                return loJSON;
            }
            return success();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Unable to save the payment request", ex);
            return error(MiscUtil.getException(ex));
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================
    static BigDecimal money(Number fnValue) {
        if (fnValue == null) {
            return BigDecimal.ZERO;
        }
        if (fnValue instanceof BigDecimal) {
            return (BigDecimal) fnValue;
        }
        return BigDecimal.valueOf(fnValue.doubleValue());
    }

    private static JSONObject success() {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "success");
        return loJSON;
    }

    private static JSONObject error(String fsMessage) {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "error");
        loJSON.put("message", fsMessage);
        return loJSON;
    }

    private static boolean isSuccess(JSONObject foJSON) {
        return foJSON != null && "success".equals((String) foJSON.get("result"));
    }
}
