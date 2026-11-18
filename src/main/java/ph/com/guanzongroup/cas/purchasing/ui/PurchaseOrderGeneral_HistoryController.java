/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package ph.com.guanzongroup.cas.purchasing.ui;

import ph.com.guanzongroup.cas.purchasing.ui.model.ModelPurchaseOrderDetail;
import ph.com.guanzongroup.cas.purchasing.ui.utility.CustomCommonUtil;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.F3;
import static javafx.scene.input.KeyCode.TAB;
import static javafx.scene.input.KeyCode.UP;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.base.CommonUtils;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.LogWrapper;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.DocumentType;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import ph.com.guanzongroup.cas.purchasing.controller.PurchaseOrderGeneral;
import ph.com.guanzongroup.cas.purchasing.services.PurchaseOrderControllers;
import org.guanzon.cas.purchasing.status.PurchaseOrderStaticData;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.cas.purchasing.ui.model.ModelPOAttachment;
import ph.com.guanzongroup.cas.purchasing.ui.utility.JFXUtil;

/**
 * Purchase Order history (inquiry) screen for General Inventory, on the new
 * controller ({@link PurchaseOrderGeneral}).
 *
 * <p>
 * A copy of integsys' PurchaseOrder_HistoryController (which stays on the old
 * controller), rewired to ph.com.guanzongroup.cas.purchasing. Read-only: find
 * a PO, view its details and attachments, approval / status history, print.</p>
 *
 * <p>
 * Run it with {@link PurchaseOrderGeneralApp} (-Dpo.screen=history).</p>
 */
public class PurchaseOrderGeneral_HistoryController implements Initializable, ScreenInterface {

    private GRiderCAS poApp;
    private PurchaseOrderControllers poPurchasingController;
    private String psFormName = "Purchase Order History";
    private LogWrapper logWrapper;
    private JSONObject poJSON;

    private int pnEditMode;
    private int pnTblDetailRow = -1;

    private String psIndustryID = "";
    private String psCompanyID = "";
    private String psSupplierID = "";
    private String psCategoryID = "";
    private String psReferID = "";
    private String psTransNo = "";

    /** What the Close button does (e.g. close the tab); default: close the window. */
    private Runnable poOnClose;
    private ObservableList<ModelPurchaseOrderDetail> detail_data = FXCollections.observableArrayList();

    @FXML
    private AnchorPane apBrowse, apButton;
    @FXML
    private AnchorPane AnchorMain;
    @FXML
    private Button btnBrowse, btnPrint, btnTransHistory, btnClose,btnHistApproval;
    @FXML
    private Label lblTransactionStatus, lblSource;
    @FXML
    private TextField tfSearchSupplier, tfSearchReferenceNo,tfSearchTransNo;
    @FXML
    private TextField tfTransactionNo, tfSupplier, tfDestination, tfReferenceNo,
            tfTerm, tfDiscountRate, tfDiscountAmount, tfAdvancePRate, tfAdvancePAmount, tfTotalAmount, tfNetAmount;
    @FXML
    private TextField tfBarcode, tfDescription, tfBrand, tfModel, tfColor, tfCategory, tfInventoryType,
            tfMeasure, tfClass, tfAMC, tfROQ, tfRO, tfBO, tfQOH, tfCost, tfRequestQuantity, tfOrderQuantity;
    @FXML
    private CheckBox chkbAdvancePayment;
    @FXML
    private TextArea taRemarks;
    @FXML
    private DatePicker dpTransactionDate, dpExpectedDlvrDate;
    @FXML
    private TableView<ModelPurchaseOrderDetail> tblVwOrderDetails;
    @FXML
    private TableColumn<ModelPurchaseOrderDetail, String> tblRowNoDetail, tblOrderNoDetail, tblBarcodeDetail,
            tblDescriptionDetail, tblCostDetail, tblROQDetail, tblRequestQuantityDetail, tblOrderQuantityDetail,
            tblTotalAmountDetail;
    
    @FXML
    private TableView tblAttachments;
    @FXML
    private TableColumn tblRowNoAttachment,tblFileNameAttachment;
    
    @FXML
    private ImageView imageView;
    
    @FXML
    private ComboBox cmbAttachmentType;
    
    @FXML
    private TextField tfAttachmentNo;
    @FXML
    private AnchorPane apAttachments;
    
    private FileChooser fileChooser;
    private int pnAttachment;
    private double mouseAnchorX;
    private double mouseAnchorY;
    private double scaleFactor = 1.0;
    private int currentIndex = 0;
    double ldstackPaneWidth = 0;
    double ldstackPaneHeight = 0;
    @FXML
    private StackPane stackPane1;
    private ObservableList<ModelPOAttachment> attachment_data = FXCollections.observableArrayList();
    ObservableList<String> documentType = ModelPOAttachment.documentType;
    @FXML
    private Button  btnAddAttachment, btnRemoveAttachment, btnArrowLeft, btnArrowRight;
    


    @Override
    public void setGRider(GRiderCAS foValue) {
        poApp = foValue;
    }

    @Override
    public void setIndustryID(String fsValue) {
        psIndustryID = fsValue;
    }

    @Override
    public void setCompanyID(String fsValue) {
        psCompanyID = fsValue;
    }

    @Override
    public void setCategoryID(String fsValue) {
        psCategoryID = fsValue;
    }

    /** What the Close button does (e.g. remove the tab); default: close the window. */
    public void setOnClose(Runnable foOnClose) {
        poOnClose = foOnClose;
    }

    /** The General Inventory Purchase Order controller. */
    private PurchaseOrderGeneral po() {
        return poPurchasingController.PurchaseOrderGeneral();
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            poPurchasingController = new PurchaseOrderControllers(poApp, logWrapper);
            po().setTransactionStatus(
                    PurchaseOrderStatus.OPEN
                    + PurchaseOrderStatus.CONFIRMED
                    + PurchaseOrderStatus.RETURNED
                    + PurchaseOrderStatus.APPROVED
                    + PurchaseOrderStatus.CANCELLED
                    + PurchaseOrderStatus.VOID
                    + PurchaseOrderStatus.RETURNED
                    + PurchaseOrderStatus.POSTED
                    + PurchaseOrderStatus.PROCESSED);
            poJSON = po().InitTransaction();
            if (!"success".equals(poJSON.get("result"))) {
                ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
            }

            tblVwOrderDetails.addEventFilter(KeyEvent.KEY_PRESSED, this::tableKeyEvents);
            Platform.runLater((() -> {
//                psIndustryID = "";
                po().Master().setIndustryID(psIndustryID);
                po().Master().setCompanyID(psCompanyID);
                po().Master().setCategoryCode(psCategoryID);
                loadRecordSearch();
            }));
            initAll();
        } catch (ExceptionInInitializerError ex) {
            Logger.getLogger(PurchaseOrderGeneral_HistoryController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadRecordSearch() {
        try {
            lblSource.setText(po().Master().Company().getCompanyName());
        } catch (GuanzonException | SQLException ex) {
            Logger.getLogger(PurchaseOrderGeneral_HistoryController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void initAll() {
        initButtonsClickActions();
        initTextFieldKeyPressed();
        initTextFieldsProperty();
        initTableDetail();
        initAttachmentsGrid();
        initAttachmentPreviewPane();
        initStackPaneListener();
        initComboBoxes();
        tblVwOrderDetails.setOnMouseClicked(this::tblVwDetail_Clicked);
        pnEditMode = EditMode.UNKNOWN;
        initButtons(pnEditMode);
        tblAttachments.setOnMouseClicked(event -> {
            pnAttachment = tblAttachments.getSelectionModel().getSelectedIndex();
            if (pnAttachment >= 0) {
                scaleFactor = 1.0;
                loadRecordAttachment(true);
                resetImageBounds();
            }
        });
    }

    private void loadRecordMaster() {
        try {
            tfTransactionNo.setText(po().Master().getTransactionNo());
            lblTransactionStatus.setText(po().getStatusValue());
            dpTransactionDate.setValue(CustomCommonUtil.parseDateStringToLocalDate(SQLUtil.dateFormat(po().Master().getTransactionDate(), SQLUtil.FORMAT_SHORT_DATE)));
            tfSupplier.setText(po().Master().Supplier().getCompanyName() != null ? po().Master().Supplier().getCompanyName() : "");
            tfDestination.setText(po().Master().Branch().getBranchName() != null ? po().Master().Branch().getBranchName() : "");
            tfReferenceNo.setText(po().Master().getReference());
            tfTerm.setText(po().Master().Term().getDescription() != null ? po().Master().Term().getDescription() : "");
            taRemarks.setText(po().Master().getRemarks());
            dpExpectedDlvrDate.setValue(CustomCommonUtil.parseDateStringToLocalDate(SQLUtil.dateFormat(po().Master().getExpectedDate(), SQLUtil.FORMAT_SHORT_DATE)));
            tfDiscountRate.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getDiscount().doubleValue()));
            tfDiscountAmount.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getAdditionalDiscount(), true));
            chkbAdvancePayment.setSelected(po().Master().getWithAdvPaym() == true);
            tfAdvancePRate.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getDownPaymentRatesPercentage().doubleValue()));
            tfAdvancePAmount.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getDownPaymentRatesAmount(), true));
            tfTotalAmount.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getTranTotal(), true));
            tfNetAmount.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getNetTotal(), true));
        } catch (GuanzonException | SQLException ex) {
            Logger.getLogger(PurchaseOrderGeneral_HistoryController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void loadRecordDetail() {
        try {
            if (pnTblDetailRow < 0 || pnTblDetailRow > po().getDetailCount() - 1) {
                return;
            }
            if (pnTblDetailRow >= 0) {
                String lsStockId = po().Detail(pnTblDetailRow).getStockID();
                if (lsStockId == null || lsStockId.isEmpty()) {
                    clearDetailFields(); //the empty row: nothing to look up
                    return;
                }
                tfBarcode.setText(po().Detail(pnTblDetailRow).Inventory().getBarCode() != null ? po().Detail(pnTblDetailRow).Inventory().getBarCode() : "");
                tfDescription.setText(po().Detail(pnTblDetailRow).Inventory().getDescription() != null ? po().Detail(pnTblDetailRow).Inventory().getDescription() : "");
                tfBrand.setText(po().Detail(pnTblDetailRow).Inventory().Brand().getDescription());
                tfModel.setText(po().Detail(pnTblDetailRow).Inventory().Model().getDescription());
                tfColor.setText(po().Detail(pnTblDetailRow).Inventory().Color().getDescription());
                tfCategory.setText(po().Detail(pnTblDetailRow).Inventory().Category().getDescription());
                tfInventoryType.setText(po().Detail(pnTblDetailRow).Inventory().InventoryType().getDescription());
                tfMeasure.setText(po().Detail(pnTblDetailRow).Inventory().Measure().getDescription());
                tfClass.setText(po().Detail(pnTblDetailRow).InventoryMaster().getInventoryClassification());
                tfAMC.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Detail(pnTblDetailRow).InventoryMaster().getAverageCost()));
                tfROQ.setText("0");
                double lnRO = 0, lnBO = 0, lnQOH = 0, lnRequestQuantity = 0;
                switch (po().Detail(pnTblDetailRow).getSouceCode()) {
                    case PurchaseOrderStatus.SourceCode.STOCKREQUEST:
                        lnRO = po().Detail(pnTblDetailRow).InvStockRequestDetail().getReceived();
                        lnBO = po().Detail(pnTblDetailRow).InvStockRequestDetail().getBackOrder();
                        lnQOH = po().Detail(pnTblDetailRow).InvStockRequestDetail().getQuantityOnHand();
                        lnRequestQuantity = po().Detail(pnTblDetailRow).InvStockRequestDetail().getApproved();
                        break;
                    case PurchaseOrderStatus.SourceCode.POQUOTATION:
                        lnRO = 0;
                        lnBO = 0;
                        lnQOH = 0;
                        lnRequestQuantity = po().Detail(pnTblDetailRow).POQuotationDetail().getQuantity();
                        break;
                }
                tfRO.setText(CustomCommonUtil.setDecimalValueToIntegerFormat(lnRO));
                tfBO.setText(CustomCommonUtil.setDecimalValueToIntegerFormat(lnBO));
                tfQOH.setText(CustomCommonUtil.setDecimalValueToIntegerFormat(lnQOH));
                tfCost.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Detail(pnTblDetailRow).getUnitPrice().doubleValue(), true));
                tfRequestQuantity.setText(CustomCommonUtil.setDecimalValueToIntegerFormat(lnRequestQuantity));
                tfOrderQuantity.setText(String.valueOf(po().Detail(pnTblDetailRow).getQuantity().intValue()));
            }
        } catch (GuanzonException | SQLException ex) {
            Logger.getLogger(PurchaseOrderGeneral_HistoryController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initButtonsClickActions() {
        List<Button> buttons = Arrays.asList(
                btnPrint, btnTransHistory, btnClose, btnBrowse,btnHistApproval
                ,btnAddAttachment, btnRemoveAttachment, btnArrowLeft, btnArrowRight);
        buttons.forEach(button -> button.setOnAction(this::handleButtonAction));
    }

    private void handleButtonAction(ActionEvent event) {
        try {

            poJSON = new JSONObject();
            String lsButton = ((Button) event.getSource()).getId();
            switch (lsButton) {
                case "btnBrowse":
                    poJSON = po().SearchTransaction("",
                            psSupplierID,
                            psReferID,psTransNo,1);
                    if ("success".equals((String) poJSON.get("result"))) {
                        clearDetailFields();
                        pnTblDetailRow = -1;
                        loadRecordMaster();
                        loadRecordDetail();
                        loadTableDetail();
                        loadTableAttachment();
                        loadTableDetailFromMain();                        
                        pnEditMode = po().getEditMode();
                    } else {
                        ShowMessageFX.Warning((String) poJSON.get("message"), "Search Information", null);
                    }
                    break;
                case "btnPrint":
                    poJSON = po().printTransaction(PurchaseOrderStaticData.Printing_CARSp_MCSp_General);
                    if (!"success".equals((String) poJSON.get("result"))) {
                        ShowMessageFX.Warning((String) poJSON.get("message"), "Print Purchase Order", null);
                    }
                    break;
                case "btnTransHistory":
                    if (pnEditMode != EditMode.READY && pnEditMode != EditMode.UPDATE) {
                        ShowMessageFX.Warning("No transaction status history to load!", psFormName, null);
                        return;
                    }

                    try {
                        po().ShowStatusHistory();
                    } catch (NullPointerException npe) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(npe), npe);
                        ShowMessageFX.Error("No transaction status history to load!", psFormName, null);
                    } catch (Exception ex) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
                        ShowMessageFX.Error(MiscUtil.getException(ex), psFormName, null);
                    }
                    break;
                case "btnClose":
                    if (ShowMessageFX.YesNo("Are you sure you want to close this form?", psFormName, null)) {
                        if (poOnClose != null) {
                            poOnClose.run();
                        } else {
                            ((Stage) AnchorMain.getScene().getWindow()).close();
                        }
                    }
                    break;
                case "btnHistApproval":
                    if (pnEditMode != EditMode.READY && pnEditMode != EditMode.UPDATE) {
                        ShowMessageFX.Warning("No Approval history to load!", psFormName, null);
                        return;
                    }
                    po().ShowApprovalHistory();
                    break;
                case "btnAddAttachment":
                    fileChooser = new FileChooser();
                    fileChooser.setTitle("Choose Image");
                    fileChooser.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif", "*.pdf")
                    );
                    java.io.File selectedFile = fileChooser.showOpenDialog((Stage) btnAddAttachment.getScene().getWindow());

                    if (selectedFile != null) {
                        // Read image from the selected file
                        Path imgPath = selectedFile.toPath();
                        Image loimage = new Image(Files.newInputStream(imgPath));
                        imageView.setImage(loimage);

                        //Validate attachment
                        String imgPath2 = selectedFile.getName().toString();
                        for (int lnCtr = 0; lnCtr <= po().getTransactionAttachmentCount() - 1; lnCtr++) {
                            if (imgPath2.equals(po().TransactionAttachmentList(lnCtr).getModel().getFileName())
                                    && RecordStatus.ACTIVE.equals(po().TransactionAttachmentList(lnCtr).getModel().getRecordStatus())) {
                                ShowMessageFX.Warning(null, psFormName, "File name already exists.");
                                pnAttachment = lnCtr;
                                loadRecordAttachment(true);
                                return;
                            }
                        }

                        //Limit maximum pages of pdf to add
                        if (imgPath2.toLowerCase().endsWith(".pdf")) {
                            try (PDDocument document = PDDocument.load(selectedFile)) {
                                PDFRenderer pdfRenderer = new PDFRenderer(document);
                                int pageCount = document.getNumberOfPages();
                                if (pageCount > 20) {
                                    ShowMessageFX.Warning(null, psFormName, "PDF exceeds maximum allowed pages.");
                                    return;
                                }
                            }
                        }

//                            int lnTempRow = JFXUtil.getDetailTempRow(attachment_data,  po().addAttachment(imgPath2), 3);
//                            pnAttachment = lnTempRow;
                        pnAttachment = po().addAttachment(imgPath2);
                        //Copy file to Attachment path
                        po().copyFile(selectedFile.toString());
                        loadTableAttachment();
                        tblAttachments.getFocusModel().focus(pnAttachment);
                        tblAttachments.getSelectionModel().select(pnAttachment);
                    }
                    break;
                case "btnRemoveAttachment":
                    if (po().getTransactionAttachmentCount() <= 0) {
                        return;
                    } else {
                        for (int lnCtr = 0; lnCtr < po().getTransactionAttachmentCount(); lnCtr++) {
                            if (RecordStatus.INACTIVE.equals(po().TransactionAttachmentList(lnCtr).getModel().getRecordStatus())) {
                                if (pnAttachment == lnCtr) {
                                    return;
                                }
                            }
                        }
                    }
                    poJSON = po().removeAttachment(pnAttachment);
                    if ("error".equals((String) poJSON.get("result"))) {
                        ShowMessageFX.Warning(null, psFormName, (String) poJSON.get("message"));
                        return;
                    }
                    attachment_data.remove(tblAttachments.getSelectionModel().getSelectedIndex());
                    if (pnAttachment != 0) {
                        pnAttachment -= 1;
                    }
                    loadRecordAttachment(false);
                    loadTableAttachment();
                    if (attachment_data.size() <= 0) {
                        JFXUtil.clearTextFields(apAttachments);
                    }
                    initAttachmentsGrid();
                    break;
                case "btnArrowLeft":
                    slideImage(-1);
                    break;
                case "btnArrowRight":
                    slideImage(1);
                    break;
                default:
                    ShowMessageFX.Warning("Please contact admin to assist about no button available", psFormName, null);
                    break;
            }
            if (lsButton.equals("btnRetrieve") || lsButton.equals("btnAddAttachment") || lsButton.equals("btnRemoveAttachment")
                    || lsButton.equals("btnArrowRight") || lsButton.equals("btnArrowLeft") || lsButton.equals("btnRetrieve") || lsButton.equals("btnHistory")) {
            } else {
                loadRecordMaster();
                loadTableDetail();
                loadTableAttachment();
            }
            initButtons(pnEditMode);
        } catch (CloneNotSupportedException | SQLException | GuanzonException ex) {
            Logger.getLogger(PurchaseOrderGeneral_HistoryController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(PurchaseOrderGeneral_HistoryController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initTextFieldKeyPressed() {
        List<TextField> loTxtField = Arrays.asList(tfSearchSupplier,
                tfSearchReferenceNo,tfSearchTransNo);

        loTxtField.forEach(tf -> tf.setOnKeyPressed(event -> txtField_KeyPressed(event)));
    }

    private void txtField_KeyPressed(KeyEvent event) {
        TextField lsTxtField = (TextField) event.getSource();
        String txtFieldID = ((TextField) event.getSource()).getId();
        String lsValue = "";
        if (lsTxtField.getText() == null) {
            lsValue = "";
        } else {
            lsValue = lsTxtField.getText();
        }
        poJSON = new JSONObject();
        try {
            if (null != event.getCode()) {
                switch (event.getCode()) {
                    case TAB:
                    case ENTER:
                    case F3:
                        switch (txtFieldID) {
                            case "tfSearchSupplier":
                                poJSON = po().SearchSupplier(lsValue, false);
                                if ("error".equals(poJSON.get("result"))) {
                                    ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                                    tfSearchSupplier.setText("");
                                    break;
                                }
                                psSupplierID = po().Master().getSupplierID();
                                tfSearchSupplier.setText(po().Master().Supplier().getCompanyName());
                                break;
                            case "tfSearchReferenceNo":
                                psReferID = lsValue;
                                poJSON = po().SearchTransaction(psReferID,
                                        psSupplierID,
                                        psReferID,psTransNo,2);
                                if ("success".equals((String) poJSON.get("result"))) {
                                    clearDetailFields();
                                    pnTblDetailRow = -1;
                                    loadRecordMaster();
                                    loadRecordDetail();
                                    loadTableDetail();
                                    tfSearchReferenceNo.clear();
                                    pnEditMode = po().getEditMode();
                                } else {
                                    ShowMessageFX.Warning((String) poJSON.get("message"), "Search Information", null);
                                }
                                break;
                            case "tfSearchTransNo":
                                psTransNo = lsValue;
                                poJSON = po().SearchTransaction(psTransNo,
                                        psSupplierID,
                                        psReferID,psTransNo,1);
                                if ("success".equals((String) poJSON.get("result"))) {
                                    clearDetailFields();
                                    pnTblDetailRow = -1;
                                    loadRecordMaster();
                                    loadRecordDetail();
                                    loadTableDetail();
                                    tfSearchTransNo.clear();
                                    pnEditMode = po().getEditMode();
                                } else {
                                    ShowMessageFX.Warning((String) poJSON.get("message"), "Search Information", null);
                                }
                                break;
                        }
                        event.consume();
                        CommonUtils.SetNextFocus((TextField) event.getSource());
                        break;
                    case UP:
                        event.consume();
                        CommonUtils.SetPreviousFocus((TextField) event.getSource());
                        break;
                    case DOWN:
                        event.consume();
                        CommonUtils.SetNextFocus((TextField) event.getSource());
                        break;
                    default:
                        break;
                }
            }
        } catch (ExceptionInInitializerError | SQLException | GuanzonException | CloneNotSupportedException ex) {
            Logger.getLogger(PurchaseOrderGeneral_HistoryController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void clearDetailFields() {
        /* Detail Fields*/
        CustomCommonUtil.setText("", tfBarcode, tfDescription, tfBrand, tfModel,
                tfColor, tfCategory, tfInventoryType, tfMeasure, tfClass,
                tfAMC, tfROQ, tfRO, tfBO, tfQOH,
                tfCost, tfRequestQuantity, tfOrderQuantity);
    }

    private void initButtons(int fnEditMode) {
        btnPrint.setVisible(false);
        btnPrint.setManaged(false);
        btnTransHistory.setVisible(fnEditMode != EditMode.UNKNOWN);
        btnTransHistory.setManaged(fnEditMode != EditMode.UNKNOWN);
        btnHistApproval.setVisible(fnEditMode == EditMode.READY);
        btnHistApproval.setManaged(fnEditMode == EditMode.READY);
        if (po().Master().getPrint().equals("1")) {
            btnPrint.setText("Reprint");
        } else {
            btnPrint.setText("Print");
        }
        if (fnEditMode == EditMode.READY) {
            switch (po().Master().getConvertedTransactionStatus()) {
                case PurchaseOrderStatus.OPEN:
                case PurchaseOrderStatus.APPROVED:
                case PurchaseOrderStatus.CONFIRMED:
                    btnPrint.setVisible(true);
                    btnPrint.setManaged(true);
                    break;
            }
        }
    }

    private void loadTableDetail() {
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(50, 50);
        progressIndicator.setStyle("-fx-accent: #FF8201;");

        StackPane loadingPane = new StackPane(progressIndicator);
        loadingPane.setAlignment(Pos.CENTER);
        loadingPane.setStyle("-fx-background-color: transparent;");

        detail_data.clear();
        tblVwOrderDetails.setPlaceholder(loadingPane);
        progressIndicator.setVisible(true);

        Task<List<ModelPurchaseOrderDetail>> task = new Task<List<ModelPurchaseOrderDetail>>() {
            @Override
            protected List<ModelPurchaseOrderDetail> call() throws Exception {
                try {
                    int detailCount = po().getDetailCount();
                    List<ModelPurchaseOrderDetail> detailsList = new ArrayList<>();

                    for (int lnCtr = 0; lnCtr < detailCount; lnCtr++) {
                        Model_PO_Detail orderDetail = po().Detail(lnCtr);
                        double lnTotalAmount = orderDetail.getUnitPrice().doubleValue() * orderDetail.getQuantity().intValue();
                        double lnRequestQuantity = 0;
                        String status = "0";
                        double lnTotalQty = 0.0000;
                        switch (po().Detail(lnCtr).getSouceCode()) {
                            case PurchaseOrderStatus.SourceCode.STOCKREQUEST:
                                lnRequestQuantity = po().Detail(lnCtr).InvStockRequestDetail().getApproved();
                                lnTotalQty = (po().Detail(lnCtr).InvStockRequestDetail().getPurchase()
                                        + po().Detail(lnCtr).InvStockRequestDetail().getIssued()
                                        + po().Detail(lnCtr).InvStockRequestDetail().getCancelled());
                                if (!po().Detail(lnCtr).getSouceNo().isEmpty()) {
                                    if (lnRequestQuantity != lnTotalQty) {
                                        status = "1";
                                    }
                                }
                                break;
                            case PurchaseOrderStatus.SourceCode.POQUOTATION:
                                lnRequestQuantity = po().Detail(lnCtr).POQuotationDetail().getQuantity();
                                break;
                        }
                        detailsList.add(new ModelPurchaseOrderDetail(
                                String.valueOf(lnCtr + 1),
                                orderDetail.getSouceNo(),
                                orderDetail.Inventory().getBarCode(),
                                orderDetail.Inventory().getDescription(),
                                CustomCommonUtil.setIntegerValueToDecimalFormat(orderDetail.getUnitPrice(), true),
                                        "0",
                                CustomCommonUtil.setDecimalValueToIntegerFormat(lnRequestQuantity),
                                CustomCommonUtil.setDecimalValueToIntegerFormat(orderDetail.getQuantity().intValue()),
                                CustomCommonUtil.setIntegerValueToDecimalFormat(lnTotalAmount, true),
                                status
                        ));
                    }
                    Platform.runLater(() -> {
                        detail_data.setAll(detailsList); // Properly update list
                        tblVwOrderDetails.setItems(detail_data);
                    });

                    return detailsList;

                } catch (GuanzonException | SQLException ex) {
                    Logger.getLogger(PurchaseOrderGeneral_HistoryController.class.getName()).log(Level.SEVERE, null, ex);
                    return null;
                }
            }

            @Override
            protected void succeeded() {
                progressIndicator.setVisible(false);
            }

            @Override
            protected void failed() {
                progressIndicator.setVisible(false);
            }
        };

        //Runs on the JavaFX thread: the PO detail models are not thread-safe
        //and the screen reads them at the same time (loadRecordDetail).
        task.run();
    }

    private void initTableDetail() {
        JFXUtil.setColumnCenter(tblRowNoDetail, tblOrderNoDetail);
        JFXUtil.setColumnLeft(tblBarcodeDetail, tblDescriptionDetail);
        JFXUtil.setColumnRight(tblCostDetail, tblRequestQuantityDetail, tblOrderQuantityDetail, tblTotalAmountDetail, tblROQDetail);
        JFXUtil.setColumnsIndexAndDisableReordering(tblVwOrderDetails);
        initTableHighlithers();

    }

    private void initTableHighlithers() {
        tblVwOrderDetails.setRowFactory(tv -> new TableRow<ModelPurchaseOrderDetail>() {
            @Override
            protected void updateItem(ModelPurchaseOrderDetail item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setStyle("");
                } else {
                    String status = item.getIndex10(); // Replace with actual getter
                    switch (status) {
                        case "1":
                            setStyle("-fx-background-color: #FAA0A0;");
                            break;
                        default:
                            setStyle("");
                    }
                    tblVwOrderDetails.refresh();
                }
            }
        });
    }

    private void tblVwDetail_Clicked(MouseEvent event) {
        if (pnEditMode == EditMode.READY) {
            pnTblDetailRow = tblVwOrderDetails.getSelectionModel().getSelectedIndex();

            ModelPurchaseOrderDetail selectedItem = tblVwOrderDetails.getSelectionModel().getSelectedItem();
            if (event.getClickCount() == 1) {
                clearDetailFields();
                if (selectedItem != null) {
                    if (pnTblDetailRow >= 0) {
                        loadRecordDetail();
                    }
                }
            }
        }
    }

    private int moveToNextRow(TableView<?> table, TablePosition<?, ?> focusedCell) {
        if (table.getItems().isEmpty()) {
            return -1; // No movement possible
        }
        int nextRow = (focusedCell.getRow() + 1) % table.getItems().size();
        table.getSelectionModel().select(nextRow);
        return nextRow;
    }

    private int moveToPreviousRow(TableView<?> table, TablePosition<?, ?> focusedCell) {
        if (table.getItems().isEmpty()) {
            return -1; // No movement possible
        }
        int previousRow = (focusedCell.getRow() - 1 + table.getItems().size()) % table.getItems().size();
        table.getSelectionModel().select(previousRow);
        return previousRow;
    }

    private void tableKeyEvents(KeyEvent event) {
        TableView<?> currentTable = (TableView<?>) event.getSource();
        TablePosition<?, ?> focusedCell = currentTable.getFocusModel().getFocusedCell();
        if (focusedCell != null) {
            if ("tblVwOrderDetails".equals(currentTable.getId())) {
                switch (event.getCode()) {
                    case TAB:
                    case DOWN:
                        pnTblDetailRow = moveToNextRow(currentTable, focusedCell);
                        break;
                    case UP:
                        pnTblDetailRow = moveToPreviousRow(currentTable, focusedCell);
                        break;
                    default:
                        return; // Ignore other keys
                }

                loadRecordDetail();
                event.consume();
            }

        }
    }

    private void initTextFieldsProperty() {
        tfSearchSupplier.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue.isEmpty()) {
                    po().Master().setSupplierID("");
                    po().Master().setAddressID("");
                    po().Master().setContactID("");
                    tfSearchSupplier.setText("");
                    psSupplierID = "";
                }

            }
        });
        tfSearchReferenceNo.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue.isEmpty()) {
                    po().Master().setReference("");
                    tfSearchReferenceNo.setText("");
                    psReferID = "";
                }
            }
        });
    }
    
    private void loadTableAttachment() {
        // Setting data to table detail
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxHeight(50);
        progressIndicator.setStyle("-fx-progress-color: #FF8201;");
        StackPane loadingPane = new StackPane(progressIndicator);
        loadingPane.setAlignment(Pos.CENTER);
        tblAttachments.setPlaceholder(loadingPane);
        progressIndicator.setVisible(true);

        Label placeholderLabel = new Label("NO RECORD TO LOAD");
        placeholderLabel.setStyle("-fx-font-size: 10px;"); // Adjust the size as needed

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                scaleFactor = 1.0;
                JFXUtil.resetImageBounds(imageView, stackPane1);
                Platform.runLater(() -> {
                    try {
                        attachment_data.clear();
                        int lnCtr;
                        int lnCount = 0;
                        for (lnCtr = 0; lnCtr < po().getTransactionAttachmentCount(); lnCtr++) {
                            if (RecordStatus.INACTIVE.equals(po().TransactionAttachmentList(lnCtr).getModel().getRecordStatus())) {
                                continue;
                            }
                            lnCount += 1;
                            attachment_data.add(
                                    new ModelPOAttachment(String.valueOf(lnCount),
                                            String.valueOf(po().TransactionAttachmentList(lnCtr).getModel().getFileName()),
                                            String.valueOf(lnCtr)
                                    ));
                        }
                        int lnTempRow = JFXUtil.getDetailRow(attachment_data, pnAttachment, 3); //this method is used only when Reverse is applied
                        if (lnTempRow < 0 || lnTempRow
                                >= attachment_data.size()) {
                            if (!attachment_data.isEmpty()) {
                                /* FOCUS ON FIRST ROW */
                                JFXUtil.selectAndFocusRow(tblAttachments, 0);
                                int lnRow = Integer.parseInt(attachment_data.get(0).getIndex03());
                                pnAttachment = lnRow;
                                loadRecordAttachment(true);
                            }
                        } else {
                            /* FOCUS ON THE ROW THAT pnRowDetail POINTS TO */
                            JFXUtil.selectAndFocusRow(tblAttachments, lnTempRow);
                            int lnRow = Integer.parseInt(attachment_data.get(tblAttachments.getSelectionModel().getSelectedIndex()).getIndex03());
                            pnAttachment = lnRow;
                            loadRecordAttachment(true);
                        }
                        if (attachment_data.size() <= 0) {
                            loadRecordAttachment(false);
                        }
                    } catch (Exception e) {
                    }
                });

                return null;
            }

            @Override
            protected void succeeded() {
                if (attachment_data == null || attachment_data.isEmpty()) {
                    tblAttachments.setPlaceholder(placeholderLabel);
                } else {
                    tblAttachments.toFront();
                }
                progressIndicator.setVisible(false);

            }

            @Override
            protected void failed() {
                if (attachment_data == null || attachment_data.isEmpty()) {
                    tblAttachments.setPlaceholder(placeholderLabel);
                }
                progressIndicator.setVisible(false);
            }

        };
        new Thread(task).start(); // Run task in background

    }
    
    public void loadRecordAttachment(boolean lbloadImage) {
        try {
            boolean lbShow2 = pnEditMode == EditMode.UPDATE || pnEditMode == EditMode.ADDNEW;
            JFXUtil.setDisabled(!lbShow2, cmbAttachmentType, btnAddAttachment, btnRemoveAttachment);
            if (attachment_data.size() > 0) {
                tfAttachmentNo.setText(attachment_data.get(tblAttachments.getSelectionModel().getSelectedIndex()).getIndex01());
                String lsAttachmentType = po().TransactionAttachmentList(pnAttachment).getModel().getDocumentType();
                if (lsAttachmentType.equals("")) {
                    po().TransactionAttachmentList(pnAttachment).getModel().setDocumentType(DocumentType.OTHER);
                    lsAttachmentType = po().TransactionAttachmentList(pnAttachment).getModel().getDocumentType();
                }
                int lnAttachmentType = 0;
                lnAttachmentType = Integer.parseInt(lsAttachmentType);
                cmbAttachmentType.getSelectionModel().select(lnAttachmentType);
                if (lbloadImage) {
                    try {
                        String filePath = (String) attachment_data.get(tblAttachments.getSelectionModel().getSelectedIndex()).getIndex02();
                        String filePath2 = "";

                        // in server
                        if (po().TransactionAttachmentList(pnAttachment).getModel().getImagePath() != null && !"".equals(po().TransactionAttachmentList(pnAttachment).getModel().getImagePath())) {
                            filePath2 = po().TransactionAttachmentList(pnAttachment).getModel().getImagePath() + "/" + (String) attachment_data.get(tblAttachments.getSelectionModel().getSelectedIndex()).getIndex02();
                        } else {
                            filePath2 = System.getProperty("sys.default.path.temp.attachments") + "/" + (String) attachment_data.get(tblAttachments.getSelectionModel().getSelectedIndex()).getIndex02();
                        }

                        if (filePath != null && !filePath.isEmpty()) {
                            Path imgPath = Paths.get(filePath2);
                            String convertedPath = imgPath.toUri().toString();
                            boolean isPdf = filePath.toLowerCase().endsWith(".pdf");

                            // Clear previous content
                            stackPane1.getChildren().clear();
                            if (!isPdf) {
                                // ----- IMAGE VIEW -----
                                Image loimage = new Image(convertedPath);
                                imageView.setImage(loimage);
                                JFXUtil.adjustImageSize(loimage, imageView, ldstackPaneWidth, ldstackPaneHeight);

                                PauseTransition delay = new PauseTransition(Duration.seconds(2)); // 2-second delay
                                delay.setOnFinished(event -> {
                                    Platform.runLater(() -> {
                                        JFXUtil.stackPaneClip(stackPane1);
                                    });
                                });
                                delay.play();

                                // Add ImageView directly to stackPane
                                stackPane1.getChildren().add(imageView);
                                stackPane1.getChildren().addAll(btnArrowLeft, btnArrowRight);

                                // Align buttons on top
                                StackPane.setAlignment(btnArrowLeft, Pos.CENTER_LEFT);
                                StackPane.setAlignment(btnArrowRight, Pos.CENTER_RIGHT);

                                // Optional: add some margin
                                StackPane.setMargin(btnArrowLeft, new Insets(0, 0, 0, 10));
                                StackPane.setMargin(btnArrowRight, new Insets(0, 10, 0, 0));

                            } else {
                                // ----- PDF VIEW -----
                                JFXUtil.PDFViewConfig(filePath2, stackPane1, btnArrowLeft, btnArrowRight, ldstackPaneWidth, ldstackPaneHeight);
                            }
                        } else {
                            imageView.setImage(null);
                        }

                    } catch (Exception e) {
                        imageView.setImage(null);
                    }
                }
            } else {
                if (!lbloadImage) {
                    imageView.setImage(null);
                    // Clear previous content
                    stackPane1.getChildren().clear();
                    // Add ImageView directly to stackPane
                    stackPane1.getChildren().add(imageView);
                    stackPane1.getChildren().addAll(btnArrowLeft, btnArrowRight);
                    Platform.runLater(() -> JFXUtil.stackPaneClip(stackPane1));
                    pnAttachment = 0;
                }
            }
        } catch (Exception ex) {
        }
    }
    
    private void initStackPaneListener() {
        stackPane1.widthProperty().addListener((observable, oldValue, newWidth) -> {
            double computedWidth = newWidth.doubleValue();
            ldstackPaneWidth = computedWidth;

        });
        stackPane1.heightProperty().addListener((observable, oldValue, newHeight) -> {
            double computedHeight = newHeight.doubleValue();
            ldstackPaneHeight = computedHeight;
            loadTableAttachment();
            loadRecordAttachment(true);
            initAttachmentsGrid();
        });
    }

    private void initAttachmentPreviewPane() {
        stackPane1.layoutBoundsProperty().addListener((observable, oldBounds, newBounds) -> {
            stackPane1.setClip(new javafx.scene.shape.Rectangle(
                    newBounds.getMinX(),
                    newBounds.getMinY(),
                    newBounds.getWidth(),
                    newBounds.getHeight()
            ));
        });
        imageView.setOnScroll((ScrollEvent event) -> {
            double delta = event.getDeltaY();
            scaleFactor = Math.max(0.5, Math.min(scaleFactor * (delta > 0 ? 1.1 : 0.9), 5.0));
            imageView.setScaleX(scaleFactor);
            imageView.setScaleY(scaleFactor);
        });

        imageView.setOnMousePressed((MouseEvent event) -> {
            mouseAnchorX = event.getSceneX() - imageView.getTranslateX();
            mouseAnchorY = event.getSceneY() - imageView.getTranslateY();
        });

        imageView.setOnMouseDragged((MouseEvent event) -> {
            double translateX = event.getSceneX() - mouseAnchorX;
            double translateY = event.getSceneY() - mouseAnchorY;
            imageView.setTranslateX(translateX);
            imageView.setTranslateY(translateY);
        });

        stackPane1.widthProperty().addListener((observable, oldValue, newWidth) -> {
            double computedWidth = newWidth.doubleValue();
            ldstackPaneWidth = computedWidth;

        });
        stackPane1.heightProperty().addListener((observable, oldValue, newHeight) -> {
            double computedHeight = newHeight.doubleValue();
            ldstackPaneHeight = computedHeight;

            //Placed to get height and width of stack pane in computed size before loading the image
            initStackPaneListener();
            initAttachmentsGrid();
        });

    }

    private void initAttachmentsGrid() {
        /*FOCUS ON FIRST ROW*/
        JFXUtil.setColumnCenter(tblRowNoAttachment);
        JFXUtil.setColumnLeft(tblFileNameAttachment);
        JFXUtil.setColumnsIndexAndDisableReordering(tblAttachments);
        tblAttachments.setItems(attachment_data);

        if (pnAttachment < 0 || pnAttachment >= attachment_data.size()) {
            if (!attachment_data.isEmpty()) {
                /* FOCUS ON FIRST ROW */
                tblAttachments.getSelectionModel().select(0);
                tblAttachments.getFocusModel().focus(0);
                pnAttachment = tblAttachments.getSelectionModel().getSelectedIndex();
            }
        } else {
            /* FOCUS ON THE ROW THAT pnRowDetail POINTS TO */
            tblAttachments.getSelectionModel().select(pnAttachment);
            tblAttachments.getFocusModel().focus(pnAttachment);
        }

    }

    private void slideImage(int direction) {
        if (attachment_data.size() <= 0) {
            return;
        }

        currentIndex = pnAttachment;
        int newIndex = currentIndex + direction;

        if (newIndex != -1 && (newIndex <= attachment_data.size() - 1)) {
            ModelPOAttachment image = attachment_data.get(newIndex);
            String filePath2 = "D:\\GGC_Maven_Systems\\temp\\attachments\\" + image.getIndex02();
            TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), imageView);
            slideOut.setByX(direction * -400); // Move left or right

            tblAttachments.getFocusModel().focus(newIndex);
            tblAttachments.getSelectionModel().select(newIndex);
            pnAttachment = newIndex;
            loadRecordAttachment(false);

            // Create a transition animation
            slideOut.setOnFinished(event -> {
                imageView.setTranslateX(direction * 400);
                TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), imageView);
                slideIn.setToX(0);
                slideIn.play();

                loadRecordAttachment(true);
            });

            slideOut.play();
        }
        if (isImageViewOutOfBounds(imageView, stackPane1)) {
            resetImageBounds();
        }
    }

    private boolean isImageViewOutOfBounds(ImageView imageView, StackPane stackPane) {
        Bounds clipBounds = stackPane.getClip().getBoundsInParent();
        Bounds imageBounds = imageView.getBoundsInParent();

        return imageBounds.getMaxX() < clipBounds.getMinX()
                || imageBounds.getMinX() > clipBounds.getMaxX()
                || imageBounds.getMaxY() < clipBounds.getMinY()
                || imageBounds.getMinY() > clipBounds.getMaxY();
    }

    private void resetImageBounds() {
        imageView.setScaleX(1.0);
        imageView.setScaleY(1.0);
        imageView.setTranslateX(0);
        imageView.setTranslateY(0);
        stackPane1.setAlignment(imageView, javafx.geometry.Pos.CENTER);
    }

    private void stackPaneClip() {
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(
                stackPane1.getWidth() - 8, // Subtract 10 for padding (5 on each side)
                stackPane1.getHeight() - 8 // Subtract 10 for padding (5 on each side)
        );
        clip.setArcWidth(8); // Optional: Rounded corners for aesthetics
        clip.setArcHeight(8);
        clip.setLayoutX(4); // Set padding offset for X
        clip.setLayoutY(4); // Set padding offset for Y
        stackPane1.setClip(clip);

    }
    private void initComboBoxes() {
        JFXUtil.initComboBoxCellDesignColor("#FF8201", cmbAttachmentType);
        cmbAttachmentType.setItems(documentType);
        cmbAttachmentType.setOnAction(event -> {
            if (attachment_data.size() > 0) {
                int selectedIndex = cmbAttachmentType.getSelectionModel().getSelectedIndex();
                po().TransactionAttachmentList(pnAttachment).getModel().setDocumentType("000" + String.valueOf(selectedIndex));
                cmbAttachmentType.getSelectionModel().select(selectedIndex);
            }
        });
    }
    public void loadTableDetailFromMain() {
        try {
            poJSON = new JSONObject();
            if (po().getEditMode() == EditMode.READY || po().getEditMode() == EditMode.UPDATE
                    || po().getEditMode() == EditMode.UPDATE) {
                po().loadAttachments();

                tfAttachmentNo.clear();
                cmbAttachmentType.setItems(documentType);
                imageView.setImage(null);
                stackPaneClip();
                Platform.runLater(() -> {
                    loadTableAttachment();
                });
            }

        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
    }
}
