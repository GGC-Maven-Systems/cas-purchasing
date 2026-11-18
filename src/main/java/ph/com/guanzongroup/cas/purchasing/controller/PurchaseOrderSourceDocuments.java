package ph.com.guanzongroup.cas.purchasing.controller;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.cas.inv.warehouse.StockRequest;
import org.guanzon.cas.inv.warehouse.services.InvWarehouseControllers;
import org.guanzon.cas.inv.warehouse.status.StockRequestStatus;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.QuotationControllers;
import org.guanzon.cas.purchasing.status.POQuotationStatus;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.guanzon.cas.purchasing.controller.POQuotation;

/**
 * The documents a Purchase Order is created from: stock requests and PO
 * quotations.
 *
 * <ul>
 * <li>Picking sources: {@link #getAvailableSources()},
 * {@link #addStockRequestLines(String)}, {@link #addQuotationLines(String)}.</li>
 * <li>Status changes: {@link #prepare(String, List)} before the DB
 * transaction, {@link #save()} and {@link #updateQuotationStatus(String)}
 * inside it.</li>
 * </ul>
 *
 * Works with any {@link PurchaseOrder} controller.
 */
public class PurchaseOrderSourceDocuments {

    private static final Logger LOG = Logger.getLogger(PurchaseOrderSourceDocuments.class.getName());

    /** Adds an empty detail row to the purchase order (the controller's AddDetail). */
    @FunctionalInterface
    public interface RowAdder {

        JSONObject addRow() throws CloneNotSupportedException;
    }

    private final PurchaseOrder poPO;
    private final GRiderCAS poGRider;
    private final Supplier<InvWarehouseControllers> poWarehouse;
    private final Supplier<QuotationControllers> poQuotations;
    private final RowAdder poRowAdder;

    /** Stock requests whose ordered quantity changes with this PO. */
    private List<StockRequest> paStockRequests = new ArrayList<>();
    /** Quotations referenced by this PO (saved with it). */
    private List<POQuotation> paQuotations = new ArrayList<>();
    /** Quotations on current lines, whose status follows the PO. */
    private List<POQuotation> paQuotationStatus = new ArrayList<>();
    /** Quotations on removed lines, released when no other PO uses them. */
    private List<POQuotation> paRemovedQuotationStatus = new ArrayList<>();

    public PurchaseOrderSourceDocuments(PurchaseOrder foPO, GRiderCAS foGRider,
            Supplier<InvWarehouseControllers> foWarehouse, Supplier<QuotationControllers> foQuotations,
            RowAdder foRowAdder) {
        poPO = foPO;
        poGRider = foGRider;
        poWarehouse = foWarehouse;
        poQuotations = foQuotations;
        poRowAdder = foRowAdder;
    }

    public void reset() {
        paStockRequests = new ArrayList<>();
        paQuotations = new ArrayList<>();
        paQuotationStatus = new ArrayList<>();
        paRemovedQuotationStatus = new ArrayList<>();
    }

    // =====================================================================
    // Status changes
    // =====================================================================
    /**
     * Before a status change or saving a confirmed PO: checks every source line
     * (validator) and loads the stock requests / quotations to update.
     *
     * @param faRemoved detail rows removed from the PO
     */
    public JSONObject prepare(String fsStatus, List<Model> faRemoved)
            throws SQLException, GuanzonException, CloneNotSupportedException {
        reset();

        JSONObject loJSON = poPO.rules(fsStatus).validateSourceLines();
        if (!isSuccess(loJSON)) {
            return loJSON;
        }

        for (int lnCtr = 0; lnCtr < poPO.getDetailCount(); lnCtr++) {
            prepareLine(fsStatus, poPO.Detail(lnCtr), false);
        }
        if (faRemoved != null) {
            for (Model loRemoved : faRemoved) {
                prepareLine(fsStatus, (Model_PO_Detail) loRemoved, true);
            }
        }
        return success();
    }

    private void prepareLine(String fsStatus, Model_PO_Detail foDetail, boolean fbRemoved)
            throws SQLException, GuanzonException, CloneNotSupportedException {
        String lsSourceNo = foDetail.getSouceNo();
        if (lsSourceNo == null || lsSourceNo.isEmpty()) {
            return;
        }
        if (PurchaseOrderStatus.SourceCode.STOCKREQUEST.equals(foDetail.getSouceCode())) {
            updateStockRequest(fsStatus, lsSourceNo, foDetail.getStockID(), foDetail.getQuantity().doubleValue(), fbRemoved);
        } else if (PurchaseOrderStatus.SourceCode.POQUOTATION.equals(foDetail.getSouceCode())) {
            addQuotation(lsSourceNo, fbRemoved);
        }
    }

    /** Loads the quotation once; quotation detail quantities are not updated yet. */
    private void addQuotation(String fsQuotationNo, boolean fbRemoved)
            throws SQLException, GuanzonException, CloneNotSupportedException {
        for (POQuotation loQuotation : paQuotations) {
            if (fsQuotationNo.equals(loQuotation.Master().getTransactionNo())) {
                return;
            }
        }

        POQuotation loStatus = openQuotation(fsQuotationNo);
        if (fbRemoved) {
            paRemovedQuotationStatus.add(loStatus);
        } else {
            paQuotationStatus.add(loStatus);
        }

        POQuotation loQuotation = openQuotation(fsQuotationNo);
        loQuotation.UpdateTransaction();
        paQuotations.add(loQuotation);
        //TODO update the ordered quantity of the quotation details when that is specified
    }

    private POQuotation openQuotation(String fsQuotationNo) throws SQLException, GuanzonException, CloneNotSupportedException {
        POQuotation loQuotation = poQuotations.get().POQuotation();
        loQuotation.InitTransaction();
        loQuotation.OpenTransaction(fsQuotationNo);
        return loQuotation;
    }

    /**
     * Sets the ordered quantity of one stock request item for the new status.
     *
     * <ul>
     * <li>CONFIRMED: what other POs ordered, plus this line (a line removed
     * from the PO no longer counts).</li>
     * <li>APPROVED: unchanged; the request is marked processed.</li>
     * <li>CANCELLED: what other POs ordered (this PO no longer counts).</li>
     * <li>VOID / RETURNED: the ordered total less this line.</li>
     * </ul>
     */
    private void updateStockRequest(String fsStatus, String fsRequestNo, String fsStockId, double fnQuantity,
            boolean fbRemoved)
            throws SQLException, GuanzonException, CloneNotSupportedException {
        StockRequest loRequest = null;
        for (StockRequest loLoaded : paStockRequests) {
            if (fsRequestNo.equals(loLoaded.Master().getTransactionNo())) {
                loRequest = loLoaded;
                break;
            }
        }
        if (loRequest == null) {
            loRequest = poWarehouse.get().StockRequest();
            loRequest.InitTransaction();
            loRequest.OpenTransaction(fsRequestNo);
            loRequest.UpdateTransaction();
            paStockRequests.add(loRequest);
        }

        for (int lnRow = 0; lnRow < loRequest.getDetailCount(); lnRow++) {
            if (!fsStockId.equals(loRequest.Detail(lnRow).getStockId())) {
                continue;
            }

            double lnOrdered;
            switch (fsStatus) {
                case PurchaseOrderStatus.CONFIRMED:
                    lnOrdered = getOrderedQuantity(fsRequestNo, fsStockId, loRequest.getSourceCode(), true)
                            + (fbRemoved ? 0 : fnQuantity);
                    break;
                case PurchaseOrderStatus.APPROVED:
                    lnOrdered = loRequest.Detail(lnRow).getPurchase();
                    loRequest.Master().setProcessed(true);
                    loRequest.Master().setModifiedDate(poGRider.getServerDate());
                    loRequest.Master().setModifyingId(poGRider.getUserID());
                    break;
                case PurchaseOrderStatus.CANCELLED:
                    //only this PO's quantity is released; other POs' orders stay
                    lnOrdered = getOrderedQuantity(fsRequestNo, fsStockId, loRequest.getSourceCode(), true);
                    break;
                case PurchaseOrderStatus.VOID:
                case PurchaseOrderStatus.RETURNED:
                    lnOrdered = getOrderedQuantity(fsRequestNo, fsStockId, loRequest.getSourceCode(), false) - fnQuantity;
                    break;
                default:
                    lnOrdered = 0;
            }
            loRequest.Detail(lnRow).setPurchase(Math.max(lnOrdered, 0));
            loRequest.Detail(lnRow).setModifiedDate(poGRider.getServerDate());
            break;
        }
    }

    /** Quantity ordered by confirmed/approved POs (excluding this PO when fbExcludeThis). */
    private double getOrderedQuantity(String fsSourceNo, String fsStockId, String fsSourceCode, boolean fbExcludeThis)
            throws SQLException {
        String lsSQL = PurchaseOrderQueries.orderedQuantity(fsSourceNo, fsStockId, fsSourceCode,
                fbExcludeThis ? poPO.Master().getTransactionNo() : null);
        LOG.log(Level.FINE, lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (loRS == null) {
            return 0;
        }
        try {
            return loRS.next() ? loRS.getDouble("nQuantity") : 0;
        } finally {
            MiscUtil.close(loRS);
        }
    }

    /** Saves the prepared stock requests and quotations (inside the PO's DB transaction). */
    public JSONObject save() throws SQLException, GuanzonException, CloneNotSupportedException {
        for (StockRequest loRequest : paStockRequests) {
            loRequest.Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
            loRequest.Master().setModifiedDate(poGRider.getServerDate());
            loRequest.setWithParent(true);
            JSONObject loJSON = loRequest.SaveTransaction();
            if ("error".equals((String) loJSON.get("result"))) {
                LOG.log(Level.WARNING, "Stock request saving failed: {0}", loJSON.get("message"));
                return loJSON;
            }
        }

        for (POQuotation loQuotation : paQuotations) {
            loQuotation.Master().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
            loQuotation.Master().setModifiedDate(poGRider.getServerDate());
            loQuotation.setWithParent(true);
            JSONObject loJSON = loQuotation.SaveTransaction();
            if ("error".equals((String) loJSON.get("result"))) {
                LOG.log(Level.WARNING, "PO quotation saving failed: {0}", loJSON.get("message"));
                return loJSON;
            }
        }
        return success();
    }

    /**
     * Quotation status follows the PO (inside the PO's DB transaction):
     * <ul>
     * <li>CONFIRMED: the quotations are posted;</li>
     * <li>RETURNED / VOID / CANCELLED: quotations no other PO uses go back to
     * approved;</li>
     * <li>quotations of removed lines go back to approved when no other PO uses
     * them.</li>
     * </ul>
     */
    public JSONObject updateQuotationStatus(String fsStatus)
            throws SQLException, GuanzonException, CloneNotSupportedException, ParseException {
        for (POQuotation loQuotation : paQuotationStatus) {
            switch (fsStatus) {
                case PurchaseOrderStatus.CONFIRMED: {
                    loQuotation.setWithParent(true);
                    JSONObject loJSON = loQuotation.PostTransaction("");
                    if ("error".equals((String) loJSON.get("result"))) {
                        LOG.log(Level.WARNING, "PO quotation posting failed: {0}", loJSON.get("message"));
                        return loJSON;
                    }
                    break;
                }
                case PurchaseOrderStatus.RETURNED:
                case PurchaseOrderStatus.VOID:
                case PurchaseOrderStatus.CANCELLED: {
                    JSONObject loJSON = releaseQuotation(fsStatus, loQuotation);
                    if (!isSuccess(loJSON)) {
                        return loJSON;
                    }
                    break;
                }
                default:
                    break;
            }
        }

        for (POQuotation loQuotation : paRemovedQuotationStatus) {
            JSONObject loJSON = releaseQuotation(fsStatus, loQuotation);
            if (!isSuccess(loJSON)) {
                return loJSON;
            }
        }
        return success();
    }

    /** Back to approved when no other confirmed/approved PO uses the quotation. */
    private JSONObject releaseQuotation(String fsStatus, POQuotation foQuotation)
            throws SQLException, GuanzonException, CloneNotSupportedException, ParseException {
        JSONObject loUsed = poPO.rules(fsStatus).validateQuotationNotOrdered(foQuotation.Master().getTransactionNo());
        if (!isSuccess(loUsed)) {
            return success(); //used by another PO: leave it
        }
        foQuotation.setWithParent(true);
        JSONObject loJSON = foQuotation.ApproveTransaction("");
        if ("error".equals((String) loJSON.get("result"))) {
            LOG.log(Level.WARNING, "PO quotation approving failed: {0}", loJSON.get("message"));
            return loJSON;
        }
        return success();
    }

    // =====================================================================
    // Picking sources
    // =====================================================================
    /**
     * Stock requests and approved quotations that can still be ordered for the
     * PO's industry, company, supplier and category.
     *
     * @return "data": rows with sTransNox, sBranchCd, dTransact, sReferNox,
     * cTranStat, sBranchNm, total_details, request_type
     */
    public JSONObject getAvailableSources() throws SQLException {
        Model_PO_Master loMaster = poPO.Master();

        String lsStockRequests = MiscUtil.addCondition(PurchaseOrderQueries.stockRequestSources(), String.join(" AND ",
                "a.sIndstCdx = " + SQLUtil.toSQL(loMaster.getIndustryID()),
                "e.sCompnyID = " + SQLUtil.toSQL(loMaster.getCompanyID()),
                "(g.sSupplier = " + SQLUtil.toSQL(loMaster.getSupplierID()) + " OR g.sSupplier IS NULL)",
                "b.nApproved > 0",
                "a.cProcessd = " + SQLUtil.toSQL(Logical.NO),
                "b.nApproved <> (b.nIssueQty + b.nOrderQty)",
                "c.sCategCd1 = " + SQLUtil.toSQL(loMaster.getCategoryCode()),
                "a.cTranStat = " + SQLUtil.toSQL(StockRequestStatus.CONFIRMED)));
        //branches other than main office / warehouse see their own requests only
        if (!poGRider.isMainOffice() && !poGRider.isWarehouse()) {
            lsStockRequests += " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode());
        }
        lsStockRequests += " GROUP BY a.sTransNox"
                + " HAVING SUM(b.nApproved - (b.nIssueQty + b.nOrderQty)) > 0";

        //quotations are not filtered by branch (as requested by purchasing)
        String lsQuotations = PurchaseOrderQueries.quotationSources()
                + " WHERE " + String.join(" AND ",
                        "a.sIndstCdx = " + SQLUtil.toSQL(loMaster.getIndustryID()),
                        "a.sCompnyID = " + SQLUtil.toSQL(loMaster.getCompanyID()),
                        "a.sSupplier = " + SQLUtil.toSQL(loMaster.getSupplierID()),
                        "a.sCategrCd = " + SQLUtil.toSQL(loMaster.getCategoryCode()),
                        "a.cTranStat = " + SQLUtil.toSQL(POQuotationStatus.APPROVED))
                + " GROUP BY a.sTransNox";

        String lsSQL = lsStockRequests + " UNION " + lsQuotations + " ORDER BY dTransact, sBranchNm, sTransNox ASC";
        LOG.log(Level.FINE, lsSQL);

        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (loRS == null) {
            return error("Query execution failed.");
        }

        JSONArray laData = new JSONArray();
        try {
            while (loRS.next()) {
                JSONObject loRow = new JSONObject();
                loRow.put("sTransNox", loRS.getString("sTransNox"));
                loRow.put("sBranchCd", loRS.getString("sBranchCd"));
                loRow.put("dTransact", loRS.getDate("dTransact"));
                loRow.put("sReferNox", loRS.getString("sReferNox"));
                loRow.put("cTranStat", loRS.getString("cTranStat"));
                loRow.put("sBranchNm", loRS.getString("sBranchNm"));
                loRow.put("total_details", loRS.getInt("total_details"));
                loRow.put("request_type", loRS.getString("request_type"));
                laData.add(loRow);
            }
        } finally {
            MiscUtil.close(loRS);
        }

        JSONObject loJSON = success("Record loaded successfully.");
        loJSON.put("data", laData);
        return loJSON;
    }

    /** Adds the open items of a confirmed stock request as PO lines (quantity 0). */
    public JSONObject addStockRequestLines(String fsRequestNo)
            throws SQLException, GuanzonException, CloneNotSupportedException {
        StockRequest loRequest = poWarehouse.get().StockRequest();
        if (!isSuccess(loRequest.InitTransaction()) || !isSuccess(loRequest.OpenTransaction(fsRequestNo))) {
            return error("No records found.");
        }

        if (allLinesAdded(loRequest)) {
            return error("All stock request details are already in purchase order detail.");
        }

        Set<String> loMatching = getMatchingStockRequestItems();
        if (loMatching == null) {
            return error("Query execution failed.");
        }
        boolean lbAllProcessed = true;

        for (int lnCtr = 0; lnCtr < loRequest.getDetailCount(); lnCtr++) {
            String lsStockId = loRequest.Detail(lnCtr).getStockId();
            if (!loMatching.contains(lsStockId)) {
                continue;
            }

            double lnApproved = loRequest.Detail(lnCtr).getApproved();
            double lnServed = loRequest.Detail(lnCtr).getIssued() + loRequest.Detail(lnCtr).getPurchase();
            if (lnApproved != lnServed) {
                lbAllProcessed = false;
            }

            if (hasLine(loRequest.Detail(lnCtr).getTransactionNo(), lsStockId) || lnApproved - lnServed <= 0) {
                continue;
            }

            //adds a row when the last row is used; otherwise the empty last row is filled
            poRowAdder.addRow();
            Model_PO_Detail loLine = poPO.Detail(poPO.getDetailCount() - 1);
            loLine.setSouceNo(loRequest.Detail(lnCtr).getTransactionNo());
            loLine.setTransactionNo(loRequest.Detail(lnCtr).getTransactionNo());
            loLine.setEntryNo(loRequest.Detail(lnCtr).getEntryNumber());
            loLine.setStockID(lsStockId);
            loLine.setRecordOrder(0);
            loLine.setUnitPrice(loRequest.Detail(lnCtr).Inventory().getCost().doubleValue());
            loLine.setQuantity(0);
            //stock-request lines are marked InvR so status changes update the request
            loLine.setSouceCode(PurchaseOrderStatus.SourceCode.STOCKREQUEST);
        }

        if (lbAllProcessed) {
            return error("All records are already processed!");
        }
        return success("Record loaded successfully.");
    }

    /** Items of confirmed stock requests matching the PO; null when the query fails. */
    private Set<String> getMatchingStockRequestItems() throws SQLException {
        Model_PO_Master loMaster = poPO.Master();
        String lsSQL = MiscUtil.addCondition(PurchaseOrderQueries.stockRequestItems(), String.join(" AND ",
                "a.sIndstCdx LIKE " + SQLUtil.toSQL("%" + loMaster.getIndustryID()),
                "e.sCompnyID LIKE " + SQLUtil.toSQL("%" + loMaster.getCompanyID()),
                "h.sCategrCd LIKE " + SQLUtil.toSQL("%" + loMaster.getCategoryCode()),
                "g.sSupplier LIKE " + SQLUtil.toSQL("%" + loMaster.getSupplierID()),
                "b.nApproved > 0",
                "a.cTranStat = " + SQLUtil.toSQL(Logical.YES)));
        LOG.log(Level.FINE, lsSQL);

        Set<String> loIds = new HashSet<>();
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (loRS == null) {
            return null;
        }
        try {
            while (loRS.next()) {
                loIds.add(loRS.getString("sStockIDx"));
            }
        } finally {
            MiscUtil.close(loRS);
        }
        return loIds;
    }

    private boolean allLinesAdded(StockRequest foRequest) {
        for (int lnCtr = 0; lnCtr < foRequest.getDetailCount(); lnCtr++) {
            if (!hasLine(foRequest.Detail(lnCtr).getTransactionNo(), foRequest.Detail(lnCtr).getStockId())) {
                return false;
            }
        }
        return true;
    }

    /** Whether the PO already has a line for this source document item. */
    private boolean hasLine(String fsSourceNo, String fsStockId) {
        for (int lnRow = 0; lnRow < poPO.getDetailCount(); lnRow++) {
            if (Objects.equals(poPO.Detail(lnRow).getSouceNo(), fsSourceNo)
                    && Objects.equals(poPO.Detail(lnRow).getStockID(), fsStockId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the lines of an approved PO quotation. Unit prices are net of the
     * line discounts, with the quotation's VAT and header discounts spread
     * evenly across the lines.
     *
     * <p>
     * NOTE: kept exactly as before (double arithmetic, even split per line).
     * Whether header discounts and VAT should be split by line value instead
     * is a question for accounting; change it here once decided.</p>
     */
    public JSONObject addQuotationLines(String fsQuotationNo)
            throws SQLException, GuanzonException, CloneNotSupportedException {
        POQuotation loQuotation = poQuotations.get().POQuotation();
        if (!isSuccess(loQuotation.InitTransaction()) || !isSuccess(loQuotation.OpenTransaction(fsQuotationNo))) {
            return error("No records found.");
        }

        if (allLinesAdded(loQuotation)) {
            return error("All PO Quotation details are already in purchase order detail.");
        }

        //a quotation may feed only one PO (rule in the validator)
        JSONObject loJSON = poPO.rules().validateQuotationNotOrdered(fsQuotationNo);
        if ("error".equals(loJSON.get("result"))) {
            return loJSON;
        }

        int lnLines = loQuotation.getDetailCount();
        double ldblVATAmount = 0.0000;
        double ldblDiscountAmt = 0.0000;
        double ldblDiscountRateAmt = 0.0000;
        double ldblRemainingDiscount = 0.0000;
        if (loQuotation.Master().getVatAmount() > 0.0000) {
            ldblVATAmount = loQuotation.Master().getVatAmount() / lnLines;
        }
        if (loQuotation.Master().getAdditionalDiscountAmount() > 0.0000) {
            ldblDiscountAmt = loQuotation.Master().getAdditionalDiscountAmount() / lnLines;
        }
        if (loQuotation.Master().getDiscountRate() > 0) {
            ldblDiscountRateAmt = loQuotation.Master().getGrossAmount() * (loQuotation.Master().getDiscountRate() / 100) / lnLines;
        }

        for (int lnCtr = 0; lnCtr < lnLines; lnCtr++) {
            String lsStockId = quotedStockId(loQuotation, lnCtr);
            double ldblMasterDiscount;
            double ldblUnitPrice;

            if (!hasLine(loQuotation.Detail(lnCtr).getTransactionNo(), lsStockId)) {
                double lnQuantity = loQuotation.Detail(lnCtr).getQuantity();
                int lnLastIndex = poPO.getDetailCount() - 1;
                Model_PO_Detail loLine = poPO.Detail(lnLastIndex);
                loLine.setSouceNo(loQuotation.Detail(lnCtr).getTransactionNo());
                loLine.setTransactionNo(loQuotation.Detail(lnCtr).getTransactionNo());
                loLine.setRecordOrder(loQuotation.Detail(lnCtr).getQuantity());
                loLine.setUnitPrice(loQuotation.Detail(lnCtr).getUnitPrice());
                loLine.setQuantity(loQuotation.Detail(lnCtr).getQuantity());
                loLine.setSouceCode(loQuotation.getSourceCode());
                loLine.setStockID(lsStockId);

                //line discount
                double ldblDetailDiscountRate = 0.0000;
                if (loQuotation.Detail(lnCtr).getDiscountRate() > 0.00) {
                    ldblDetailDiscountRate = loQuotation.Detail(lnCtr).getUnitPrice() * (loQuotation.Detail(lnCtr).getDiscountRate() / 100);
                }
                ldblUnitPrice = loQuotation.Detail(lnCtr).getUnitPrice() - (ldblDetailDiscountRate + loQuotation.Detail(lnCtr).getDiscountAmount());
                if (loQuotation.Master().isVatable()) {
                    ldblUnitPrice = ldblUnitPrice + (ldblVATAmount / lnQuantity);
                }

                //header discount per unit; what does not fit carries over to the next line
                ldblMasterDiscount = (ldblDiscountAmt + ldblDiscountRateAmt + ldblRemainingDiscount) / lnQuantity;
                if (ldblMasterDiscount > ldblUnitPrice) {
                    ldblRemainingDiscount = (ldblDiscountAmt + ldblDiscountRateAmt + ldblRemainingDiscount) - (ldblUnitPrice * lnQuantity);
                    ldblUnitPrice = 0.0000;
                } else {
                    ldblRemainingDiscount = 0.0000;
                    ldblUnitPrice = ldblUnitPrice - ldblMasterDiscount;
                }

                loLine.setUnitPrice(ldblUnitPrice);
                poRowAdder.addRow();
            }

            //after the last line: spread any discount left over the lines that still have a price
            if (lnCtr == lnLines - 1 && ldblRemainingDiscount > 0.0000) {
                for (int lnRow = 0; lnRow < poPO.getDetailCount(); lnRow++) {
                    Model_PO_Detail loLine = poPO.Detail(lnRow);
                    if (Objects.equals(loLine.getSouceNo(), loQuotation.Master().getTransactionNo())
                            && Objects.equals(loLine.getSouceCode(), loQuotation.getSourceCode())
                            && loLine.getUnitPrice().doubleValue() > 0.0000) {
                        ldblUnitPrice = loLine.getUnitPrice().doubleValue();
                        ldblMasterDiscount = ldblRemainingDiscount / loLine.getRecordOrder().doubleValue();
                        if (ldblMasterDiscount > ldblUnitPrice) {
                            ldblRemainingDiscount = ldblRemainingDiscount - (ldblUnitPrice * loLine.getRecordOrder().doubleValue());
                            ldblUnitPrice = 0.0000;
                        } else {
                            ldblRemainingDiscount = 0.0000;
                            ldblUnitPrice = ldblUnitPrice - ldblMasterDiscount;
                        }
                        loLine.setUnitPrice(ldblUnitPrice);
                        if (ldblRemainingDiscount <= 0.0000) {
                            break;
                        }
                    }
                }
            }
        }

        return success("Record loaded successfully.");
    }

    /** The quoted item, or its replacement when the supplier offered one. */
    private static String quotedStockId(POQuotation foQuotation, int fnRow) {
        String lsReplace = foQuotation.Detail(fnRow).getReplaceId();
        return lsReplace != null && !lsReplace.isEmpty() ? lsReplace : foQuotation.Detail(fnRow).getStockId();
    }

    private boolean allLinesAdded(POQuotation foQuotation) {
        for (int lnCtr = 0; lnCtr < foQuotation.getDetailCount(); lnCtr++) {
            if (!hasLine(foQuotation.Detail(lnCtr).getTransactionNo(), quotedStockId(foQuotation, lnCtr))) {
                return false;
            }
        }
        return true;
    }

    // =====================================================================
    // Helpers
    // =====================================================================
    private static JSONObject success() {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "success");
        return loJSON;
    }

    private static JSONObject success(String fsMessage) {
        JSONObject loJSON = success();
        loJSON.put("message", fsMessage);
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
