package ph.com.guanzongroup.cas.purchasing.controller;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionListener;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.swing.JRViewerToolbar;
import net.sf.jasperreports.view.JasperViewer;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.status.PurchaseOrderStaticData;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.guanzon.cas.purchasing.utility.CustomJasperViewerReports;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Printing for Purchase Order: the PO form and the summary/detailed reports.
 *
 * <p>
 * Works with any {@link PurchaseOrder} controller. An industry that prints
 * differently extends this class and overrides
 * {@link #getPrintTemplatePath(String)} and/or
 * {@link #buildPrintDescription(int, String)}, then returns it from its
 * controller.</p>
 *
 * <p>
 * Compiled reports are cached per JRXML file (recompiled when the file
 * changes), so printing no longer compiles the report every time.</p>
 */
public class PurchaseOrderPrinter {

    protected final PurchaseOrder poPO;
    protected final GRiderCAS poGRider;
    protected final PurchaseOrderAudit poAudit;

    public PurchaseOrderPrinter(PurchaseOrder foPO, GRiderCAS foGRider, PurchaseOrderAudit foAudit) {
        poPO = foPO;
        poGRider = foGRider;
        poAudit = foAudit;
    }

    protected static String configPath() {
        return System.getProperty("sys.default.path.config");
    }

    protected static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    // =====================================================================
    // Industry options
    // =====================================================================
    /** JRXML for the PO form; null when the template type is unknown. */
    protected String getPrintTemplatePath(String jasperType) {
        String lsPath = configPath() + "/reports/";
        switch (jasperType) {
            case PurchaseOrderStaticData.Printing_CAR_MC_MPUnit_Appliance:
                return lsPath + "PurchaseOrderCarMcMPUnitAppliance.jrxml";
            case PurchaseOrderStaticData.Printing_CARSp_MCSp_General:
                return lsPath + "PurchaseOrderCARSpMCSpGeneral.jrxml";
            case PurchaseOrderStaticData.Printing_Pedritos:
                return lsPath + "PurchaseOrderPedritos.jrxml";
            default:
                return null;
        }
    }

    /** Item description printed for one detail row. */
    protected String buildPrintDescription(int fnRow, String jasperType) throws Exception {
        Model_PO_Detail loDetail = poPO.Detail(fnRow);
        String lsDescription = text(loDetail.Inventory().getDescription());
        switch (jasperType) {
            case PurchaseOrderStaticData.Printing_CARSp_MCSp_General:
            case PurchaseOrderStaticData.Printing_Pedritos:
                String lsFullDesc = join(text(loDetail.Inventory().Brand().getDescription()),
                        text(loDetail.Inventory().Model().getDescription()));
                return join(lsFullDesc, lsDescription);
            default:
                return lsDescription;
        }
    }

    /** JRXML for the summary (true) or detailed (false) report. */
    protected String getReportTemplatePath(boolean fbSummarized) {
        return configPath() + (fbSummarized
                ? "/reports/PurchaseOrderSummary.jrxml"
                : "/reports/PurchaseOrderSummaryDetail.jrxml");
    }

    // =====================================================================
    // PO form
    // =====================================================================
    public JSONObject printTransaction(String jasperType) {
        try {
            String lsTemplate = getPrintTemplatePath(jasperType);
            if (lsTemplate == null) {
                return error("Unknown print template: " + jasperType);
            }

            Model_PO_Master loMaster = poPO.Master();
            Map<String, Object> laParams = new HashMap<>();

            //prepared by / confirmed by
            laParams.put("sCompnyNm", "");
            laParams.put("sConfirmed", "");
            JSONObject loEntry = poAudit.getEntryBy(loMaster.getTable(), loMaster.getTransactionNo());
            if (!text(loEntry.get("sCompnyNm")).isEmpty()) {
                laParams.put("sCompnyNm", "Prepared by: " + loEntry.get("sCompnyNm") + " " + text(loEntry.get("sEntryDte")));
            }
            JSONObject loConfirm = poAudit.getConfirmedBy(loMaster.getTransactionNo());
            if (!text(loConfirm.get("sConfirmed")).isEmpty()) {
                laParams.put("sConfirmed", "Confirmed by: " + loConfirm.get("sConfirmed") + " " + text(loConfirm.get("sConfrmDte")));
            }

            laParams.put("sBranchNm", poGRider.getBranchName());
            laParams.put("sAddressx", address(loMaster.Company().getCompanyAddress(),
                    loMaster.Company().TownCity().getDescription(),
                    loMaster.Company().TownCity().Province().getDescription()));
            laParams.put("sTransNox", loMaster.getTransactionNo());
            laParams.put("sDestination", loMaster.Branch().getBranchName());
            laParams.put("sCompany", loMaster.Company().getCompanyName());
            laParams.put("sSupplier", loMaster.Supplier().getCompanyName());
            laParams.put("sTerm", loMaster.Term().getDescription());
            laParams.put("sDestinationAddress", address(loMaster.Branch().getAddress(),
                    loMaster.Branch().TownCity().getDescription(),
                    loMaster.Branch().TownCity().Province().getDescription()));

            //approvers (filled only when approved)
            laParams.put("sApprval1", "");
            laParams.put("sApprval2", "");
            laParams.put("sApprval3", "");
            if (PurchaseOrderStatus.APPROVED.equals(loMaster.getTransactionStatus())) {
                List<String> laApprovers = poAudit.getApprovers(loMaster.getTransactionNo());
                for (int lnCtr = 0; lnCtr < laApprovers.size(); lnCtr++) {
                    laParams.put("sApprval" + (lnCtr + 1), laApprovers.get(lnCtr));
                }
            }

            laParams.put("sRemarks", loMaster.getRemarks());
            laParams.put("dTransDte", new java.sql.Date(loMaster.getTransactionDate().getTime()));
            laParams.put("dDatexxx", new java.sql.Date(poGRider.getServerDate().getTime()));
            laParams.put("nTtlAmntx", loMaster.getTranTotal().doubleValue());
            laParams.put("nDiscRate", loMaster.getDiscount().doubleValue());
            laParams.put("nDiscAmnt", loMaster.getAdditionalDiscount().doubleValue());
            laParams.put("nAdvAmntx", loMaster.getDownPaymentRatesAmount().doubleValue());
            laParams.put("nAdvRatex", loMaster.getDownPaymentRatesPercentage().doubleValue());
            laParams.put("nNetAmntx", loMaster.getNetTotal().doubleValue());
            laParams.put("watermarkImagePath", getWatermarkPath(loMaster));

            List<OrderDetail> laRows = new ArrayList<>();
            for (int lnCtr = 0; lnCtr < poPO.getDetailCount(); lnCtr++) {
                Model_PO_Detail loDetail = poPO.Detail(lnCtr);
                double lnPrice = loDetail.getUnitPrice() != null ? loDetail.getUnitPrice().doubleValue() : 0.00;
                double lnQty = loDetail.getQuantity() != null ? loDetail.getQuantity().doubleValue() : 0.00;
                laRows.add(new OrderDetail(lnCtr + 1,
                        text(loDetail.getSouceNo()),
                        text(loDetail.Inventory().getBarCode()),
                        text(loDetail.Inventory().Brand().getDescription()),
                        text(loDetail.Inventory().Variant().getDescription()) + " "
                        + text(loDetail.Inventory().Variant().getYearModel()),
                        text(loDetail.Inventory().Model().getDescription()),
                        text(loDetail.Inventory().Measure().getDescription()),
                        text(loDetail.Inventory().Color().getDescription()),
                        buildPrintDescription(lnCtr, jasperType),
                        lnPrice,
                        lnQty,
                        lnPrice * lnQty));
            }

            JasperPrint loPrint = JasperFillManager.fillReport(compile(lsTemplate), laParams,
                    new JRBeanCollectionDataSource(laRows));

            if (isWindows()) {
                new PrintViewer(loPrint).setVisible(true);
            } else {
                //not Windows: export a PDF instead of opening the viewer
                JasperExportManager.exportReportToPdfFile(loPrint, configPath() + "/temp/" + loMaster.getTransactionNo() + ".pdf");
            }
            return success();
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Transaction print aborted", ex);
            return error("Transaction print aborted!");
        }
    }

    /** Draft by default; approved / approved-reprint for approved or posted POs. */
    protected String getWatermarkPath(Model_PO_Master foMaster) {
        String lsImages = configPath() + "/reports/images/";
        switch (foMaster.getTransactionStatus()) {
            case PurchaseOrderStatus.POSTED:
            case PurchaseOrderStatus.APPROVED:
                return lsImages + ("1".equals(foMaster.getPrint()) ? "approvedreprint.png" : "approved.png");
            default:
                return lsImages + "draft.png";
        }
    }

    // =====================================================================
    // Reports
    // =====================================================================
    /**
     * Prints the summary (fbSummarized) or detailed report from the rows of
     * RetrieveSummaryReports / RetrieveSummaryDetailedReports.
     *
     * @param fsDateFrom date range shown in the header (yyyy-M-d), may be empty
     * @param fsDateThru date range shown in the header (yyyy-M-d), may be empty
     */
    public JSONObject printReports(boolean fbSummarized, JSONArray faData, String fsDateFrom, String fsDateThru) {
        try {
            Model_PO_Master loMaster = poPO.Master();
            Map<String, Object> laParams = new HashMap<>();
            laParams.put("sLogCompny", poGRider.getCompnyId());
            laParams.put("sAddressx", address(loMaster.Company().getCompanyAddress(),
                    loMaster.Company().TownCity().getDescription(),
                    loMaster.Company().TownCity().Province().getDescription()));
            laParams.put("sCompany", loMaster.Company().getCompanyName());
            laParams.put("sIndustry", loMaster.Industry().getDescription());
            laParams.put("sBranch", loMaster.Branchx().getBranchName());
            laParams.put("sDateRange", "FROM " + formatDateToText(fsDateFrom) + " TO " + formatDateToText(fsDateThru));

            List<Reports> laRows = new ArrayList<>();
            for (int lnCtr = 0; lnCtr < faData.size(); lnCtr++) {
                JSONObject loRow = (JSONObject) faData.get(lnCtr);
                if (fbSummarized) {
                    laRows.add(new Reports(lnCtr + 1,
                            text(loRow.get("Supplier")),
                            text(loRow.get("Destination")),
                            text(loRow.get("Branch")),
                            text(loRow.get("sTransNox")),
                            text(loRow.get("sReferNox")),
                            (java.sql.Date) loRow.get("dTransact"),
                            text(loRow.get("Category")),
                            text(loRow.get("Term")),
                            text(loRow.get("cTranStat")),
                            number(loRow.get("Total"))));
                } else {
                    laRows.add(new Reports(lnCtr + 1,
                            text(loRow.get("Supplier")),
                            text(loRow.get("Destination")),
                            text(loRow.get("sTransNox")),
                            (java.sql.Date) loRow.get("dTransact"),
                            text(loRow.get("sBarCodex")),
                            text(loRow.get("Description")),
                            text(loRow.get("Brand")),
                            text(loRow.get("ModelCode")),
                            text(loRow.get("ModelName")),
                            text(loRow.get("Color")),
                            number(loRow.get("Quantity")),
                            number(loRow.get("nReceived")),
                            number(loRow.get("nCancelld")),
                            number(loRow.get("UnitPrice")),
                            number(loRow.get("Total"))));
                }
            }

            JasperPrint loPrint = JasperFillManager.fillReport(compile(getReportTemplatePath(fbSummarized)), laParams,
                    new JRBeanCollectionDataSource(laRows));

            if (isWindows()) {
                CustomJasperViewerReports.show(loPrint, "Purchase Order Reports");
            } else {
                //not Windows: export a PDF instead of opening the viewer
                JasperExportManager.exportReportToPdfFile(loPrint, configPath() + "/temp/" + loMaster.getTransactionNo() + ".pdf");
            }
            return success();
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Report print aborted", ex);
            return error("Report print aborted: " + ex.getMessage());
        }
    }

    /** "2026-1-5" to "January 05, 2026"; empty when blank. */
    public static String formatDateToText(String fsDate) {
        if (fsDate == null || fsDate.isEmpty() || "null".equals(fsDate)) {
            return "";
        }
        LocalDate ldDate = LocalDate.parse(fsDate, DateTimeFormatter.ofPattern("yyyy-M-d"));
        return ldDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
    }

    // =====================================================================
    // Compiled report cache
    // =====================================================================
    private static final class CachedReport {

        final long lastModified;
        final JasperReport report;

        CachedReport(long lastModified, JasperReport report) {
            this.lastModified = lastModified;
            this.report = report;
        }
    }

    private static final Map<String, CachedReport> REPORTS = new ConcurrentHashMap<>();

    /** Compiled report for the JRXML, compiled again only when the file changed. */
    protected static JasperReport compile(String fsJrxml) throws JRException {
        long lnModified = new File(fsJrxml).lastModified();
        CachedReport loCached = REPORTS.get(fsJrxml);
        if (loCached == null || loCached.lastModified != lnModified) {
            loCached = new CachedReport(lnModified, JasperCompileManager.compileReport(fsJrxml));
            REPORTS.put(fsJrxml, loCached);
        }
        return loCached.report;
    }

    // =====================================================================
    // Viewer whose Print button marks an approved PO as printed
    // =====================================================================
    public class PrintViewer extends JasperViewer {

        public PrintViewer(JasperPrint foPrint) {
            super(foPrint, false);
            replacePrintButton(foPrint);
        }

        private void replacePrintButton(JasperPrint foPrint) {
            JRViewer loViewer = findJRViewer(this);
            if (loViewer == null) {
                Logger.getLogger(PurchaseOrderPrinter.class.getName()).warning("JRViewer not found.");
                return;
            }

            for (Component loPart : loViewer.getComponents()) {
                if (!(loPart instanceof JRViewerToolbar)) {
                    continue;
                }
                JRViewerToolbar loToolbar = (JRViewerToolbar) loPart;
                for (Component loTool : loToolbar.getComponents()) {
                    if (loTool instanceof JButton && "Print".equals(((JButton) loTool).getToolTipText())) {
                        JButton loButton = (JButton) loTool;
                        for (ActionListener loListener : loButton.getActionListeners()) {
                            loButton.removeActionListener(loListener);
                        }
                        loButton.addActionListener(e -> onPrint(foPrint));
                    }
                }
                loToolbar.revalidate();
                loToolbar.repaint();
            }
        }

        private void onPrint(JasperPrint foPrint) {
            try {
                if (JasperPrintManager.printReport(foPrint, true)) {
                    markPrinted();
                } else {
                    message(false, "Printing was canceled by the user.");
                }
            } catch (JRException ex) {
                message(false, "Print Failed: " + ex.getMessage());
            } catch (Exception ex) {
                Logger.getLogger(PurchaseOrderPrinter.class.getName()).log(Level.SEVERE, null, ex);
                message(false, "Printing aborted.");
            }
        }

        /** An approved PO is flagged printed (so reprints get the reprint watermark). */
        private void markPrinted() throws Exception {
            Model_PO_Master loMaster = poPO.Master();
            if (PurchaseOrderStatus.APPROVED.equals(loMaster.getValue("cTranStat"))) {
                JSONObject loJSON = poPO.OpenTransaction((String) loMaster.getValue("sTransNox"));
                if (isSuccess(loJSON)) {
                    loJSON = poPO.UpdateTransaction();
                }
                if (isSuccess(loJSON)) {
                    poPO.Master().setValue("dModified", poGRider.getServerDate());
                    poPO.Master().setValue("sModified", poGRider.getUserID());
                    poPO.Master().setValue("cPrintxxx", Logical.YES);
                    loJSON = poPO.SaveTransaction();
                }
                if (!isSuccess(loJSON)) {
                    message(false, text(loJSON.get("message")));
                    return;
                }
            }
            message(true, "Transaction printed successfully.");
        }

        private void message(boolean fbInfo, String fsMessage) {
            Platform.runLater(() -> {
                if (fbInfo) {
                    poPO.prompt().information(fsMessage, "Print Purchase Order");
                } else {
                    poPO.prompt().warning(fsMessage, "Print Purchase Order");
                }
                SwingUtilities.invokeLater(PrintViewer.this::toFront);
            });
        }

        private JRViewer findJRViewer(Component foParent) {
            if (foParent instanceof JRViewer) {
                return (JRViewer) foParent;
            }
            if (foParent instanceof Container) {
                for (Component loChild : ((Container) foParent).getComponents()) {
                    JRViewer loViewer = findJRViewer(loChild);
                    if (loViewer != null) {
                        return loViewer;
                    }
                }
            }
            return null;
        }
    }

    // =====================================================================
    // Report rows (bean properties read by the JRXML files)
    // =====================================================================
    public static class OrderDetail {

        private final Integer nRowNo;
        private final String sOrderNo;
        private final String sBarcode;
        private final String sBrandName;
        private final String sVariant;
        private final String sModel;
        private final String sMeasure;
        private final String sColor;
        private final String sDescription;
        private final double nUprice;
        private final double nOrder;
        private final double nTotal;

        public OrderDetail(Integer rowNo, String orderNo, String barcode, String brandName, String variant, String model,
                String measure, String color, String description, double uprice, double order, double total) {
            this.nRowNo = rowNo;
            this.sOrderNo = orderNo;
            this.sBarcode = barcode;
            this.sBrandName = brandName;
            this.sVariant = variant;
            this.sModel = model;
            this.sMeasure = measure;
            this.sColor = color;
            this.sDescription = description;
            this.nUprice = uprice;
            this.nOrder = order;
            this.nTotal = total;
        }

        public Integer getnRowNo() { return nRowNo; }
        public String getsOrderNo() { return sOrderNo; }
        public String getsBarcode() { return sBarcode; }
        public String getsBrandName() { return sBrandName; }
        public String getsVariant() { return sVariant; }
        public String getsModel() { return sModel; }
        public String getsMeasure() { return sMeasure; }
        public String getsColor() { return sColor; }
        public String getsDescription() { return sDescription; }
        public double getnUprice() { return nUprice; }
        public double getnOrder() { return nOrder; }
        public double getnTotal() { return nTotal; }
        //kept for templates that reference them; never filled per line
        public double getnDiscRate() { return 0; }
        public double getnDiscAmt() { return 0; }
        public double getnAdvRate() { return 0; }
        public double getnAdvAmt() { return 0; }
        public double getnNetAmt() { return 0; }
    }

    public static class Reports {

        private Integer nRowNo;
        private String sSupplier;
        private String sDestination;
        private String sPONo;
        private java.sql.Date dTransact;
        private String sBarCodex;
        private String sDescript;
        private String sBrand;
        private String sModelCode;
        private String sModelName;
        private String sColor;
        private double nQty;
        private double nReceived;
        private double nCancelld;
        private double nUnitPrice;
        private double nTotal;
        private String sBranch;
        private String sCategory;
        private String sTerm;
        private String sReferNo;
        private String cTranStat;

        /** Detailed report row. */
        public Reports(Integer nrowNo, String sSupplier, String sDestination, String sPONo, java.sql.Date dTransact,
                String sBarCodex, String sDescript, String sBrand, String sModelCode, String sModelName, String sColor,
                double nQty, double nReceived, double nCancelld, double nUnitPrice, double nTotal) {
            this.nRowNo = nrowNo;
            this.sSupplier = sSupplier;
            this.sDestination = sDestination;
            this.sPONo = sPONo;
            this.dTransact = dTransact;
            this.sBarCodex = sBarCodex;
            this.sDescript = sDescript;
            this.sBrand = sBrand;
            this.sModelCode = sModelCode;
            this.sModelName = sModelName;
            this.sColor = sColor;
            this.nQty = nQty;
            this.nReceived = nReceived;
            this.nCancelld = nCancelld;
            this.nUnitPrice = nUnitPrice;
            this.nTotal = nTotal;
        }

        /** Summary report row. */
        public Reports(Integer nrowNo, String sSupplier, String sDestination, String sBranch, String sPONo, String sReferNo,
                java.sql.Date dTransact, String sCategory, String sTerm, String cTranStat, double nTotal) {
            this.nRowNo = nrowNo;
            this.sSupplier = sSupplier;
            this.sDestination = sDestination;
            this.sBranch = sBranch;
            this.sPONo = sPONo;
            this.sReferNo = sReferNo;
            this.dTransact = dTransact;
            this.sCategory = sCategory;
            this.sTerm = sTerm;
            this.cTranStat = cTranStat;
            this.nTotal = nTotal;
        }

        public Integer getnRowNo() { return nRowNo; }
        public String getsSupplier() { return sSupplier; }
        public String getsDestination() { return sDestination; }
        public String getsPONo() { return sPONo; }
        public java.sql.Date getdTransact() { return dTransact; }
        public String getsBarCodex() { return sBarCodex; }
        public String getsDescript() { return sDescript; }
        public String getsBrand() { return sBrand; }
        public String getsModelCode() { return sModelCode; }
        public String getsModelName() { return sModelName; }
        public String getsColor() { return sColor; }
        public double getnQty() { return nQty; }
        public double getnReceived() { return nReceived; }
        public double getnCancelld() { return nCancelld; }
        public double getnUnitPrice() { return nUnitPrice; }
        public double getnTotal() { return nTotal; }
        public String getsBranch() { return sBranch; }
        public String getsCategory() { return sCategory; }
        public String getsTerm() { return sTerm; }
        public String getsReferNo() { return sReferNo; }
        public String getcTranStat() { return cTranStat; }
    }

    // =====================================================================
    // Helpers
    // =====================================================================
    protected static String text(Object foValue) {
        return foValue == null ? "" : foValue.toString();
    }

    protected static double number(Object foValue) {
        try {
            return foValue == null ? 0.0000 : Double.parseDouble(foValue.toString());
        } catch (NumberFormatException ex) {
            return 0.0000;
        }
    }

    protected static String join(String fsLeft, String fsRight) {
        if (fsLeft.isEmpty()) {
            return fsRight;
        }
        return fsRight.isEmpty() ? fsLeft : fsLeft + " " + fsRight;
    }

    /** "street town, province" without empty parts. */
    protected static String address(String fsStreet, String fsTown, String fsProvince) {
        String lsAddress = text(fsStreet).trim();
        if (!text(fsTown).trim().isEmpty()) {
            lsAddress = join(lsAddress, fsTown.trim());
        }
        if (!text(fsProvince).trim().isEmpty()) {
            lsAddress = lsAddress.isEmpty() ? fsProvince.trim() : lsAddress + ", " + fsProvince.trim();
        }
        return lsAddress;
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
