/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.tf to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package ph.com.guanzongroup.cas.purchasing.ui;

import ph.com.guanzongroup.cas.purchasing.ui.model.ModelPurchaseOrder;
import ph.com.guanzongroup.cas.purchasing.ui.model.ModelPurchaseOrderDetail;
import ph.com.guanzongroup.cas.purchasing.ui.utility.CustomCommonUtil;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanPropertyBase;
import javafx.beans.value.ChangeListener;
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
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
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
import org.guanzon.appdriver.agent.ShowDialogFX;
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
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import ph.com.guanzongroup.cas.purchasing.controller.PurchaseOrderGeneral;
import ph.com.guanzongroup.cas.purchasing.services.PurchaseOrderControllers;
import org.guanzon.cas.purchasing.status.PurchaseOrderStaticData;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.purchasing.ui.model.ModelPOAttachment;
import ph.com.guanzongroup.cas.purchasing.ui.utility.JFXUtil;

/**
 * Purchase Order confirmation screen for General Inventory, on the new
 * controller ({@link PurchaseOrderGeneral}).
 *
 * <p>
 * A copy of integsys' PurchaseOrder_ConfirmationController (which stays on the
 * old controller), rewired to ph.com.guanzongroup.cas.purchasing. Lists the
 * open / returned POs; opens one to edit, confirm (General Inventory
 * auto-approves after confirming), return or void it.</p>
 *
 * <p>
 * Run it with {@link PurchaseOrderGeneralApp} (-Dpo.screen=confirmation).</p>
 */
public class PurchaseOrderGeneral_ConfirmationController implements Initializable, ScreenInterface {

    private GRiderCAS poApp;
    private PurchaseOrderControllers poPurchasingController;
    private String psFormName = "Purchase Order Confirmation";
    private LogWrapper logWrapper;
    private JSONObject poJSON;

    private int pnEditMode;
    private int pnTblMainRow = -1;
    private int pnTblDetailRow = -1;
    private int pnTblMain_Page = 50;

    private String psIndustryID = "";
    private String psCompanyID = "";
    private String psCategoryID = "";
    private String psSupplierID = "";
    private String psReferID = "";
    private String psTransNo = "";
    private String psOldDate = "";

    /** What the Close button does (e.g. close the tab); default: close the window. */
    private Runnable poOnClose;
    private ObservableList<ModelPurchaseOrder> main_data = FXCollections.observableArrayList();
    private ObservableList<ModelPurchaseOrderDetail> detail_data = FXCollections.observableArrayList();
    @FXML
    private AnchorPane apBrowse, apButton;
    @FXML
    private AnchorPane AnchorMaster, AnchorDetails, AnchorMain;
    @FXML
    private Button btnUpdate, btnSave, btnCancel, btnConfirm, btnReturn, btnVoid, btnPrint,
            btnRetrieve, btnTransHistory, btnClose;
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
    private TableView<ModelPurchaseOrder> tblVwPurchaseOrder;
    @FXML
    private TableColumn<ModelPurchaseOrder, String> tblRowNo, tblTransactionNo, tblDate, tblSupplier;
    @FXML
    private Pagination pagination;
    
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
            
            po().setTransactionStatus(PurchaseOrderStatus.OPEN + PurchaseOrderStatus.RETURNED);
            
            poJSON = new JSONObject();
            po().setWithUI(true);
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
            Logger.getLogger(PurchaseOrderGeneral_ConfirmationController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadRecordSearch() {
        try {
            lblSource.setText(po().Master().Company().getCompanyName());
        } catch (GuanzonException | SQLException ex) {
            Logger.getLogger(PurchaseOrderGeneral_ConfirmationController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void initAll() {
        initButtonsClickActions();
        initTextFieldFocus();
        initTextAreaFocus();
        initTextFieldKeyPressed();
        initDatePickerActions();
        initTextFieldPattern();
        initCheckBoxActions();
        initTableMain();
        initTableDetail();
        initTextFieldsProperty();
        initAttachmentsGrid();
        initAttachmentPreviewPane();
        initStackPaneListener();
        initComboBoxes();
        tblVwPurchaseOrder.setOnMouseClicked(this::tblVwMain_Clicked);
        tblVwOrderDetails.setOnMouseClicked(this::tblVwDetail_Clicked);
        pnEditMode = EditMode.UNKNOWN;
        initButtons(pnEditMode);
        initFields(pnEditMode);
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
            Logger.getLogger(PurchaseOrderGeneral_ConfirmationController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void loadRecordDetail() {
        try {
            if (pnTblDetailRow < 0 || pnTblDetailRow > po().getDetailCount() - 1) {
                return;
            }
            boolean lbShow = pnEditMode == EditMode.UPDATE && JFXUtil.isObjectEqualTo(po().Detail(pnTblDetailRow).getSouceCode(), null, "");
            JFXUtil.setDisabled(!lbShow, tfCost);
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
            Logger.getLogger(PurchaseOrderGeneral_ConfirmationController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initButtonsClickActions() {
        List<Button> buttons = Arrays.asList(btnUpdate, btnSave, btnCancel, btnVoid, btnReturn,
                btnPrint, btnRetrieve, btnTransHistory, btnClose, btnConfirm,btnAddAttachment, btnRemoveAttachment, btnArrowLeft, btnArrowRight);

        buttons.forEach(button -> button.setOnAction(this::handleButtonAction));

    }

    private void handleButtonAction(ActionEvent event) {
        try {
            poJSON = new JSONObject();
            String lsButton = ((Button) event.getSource()).getId();
            switch (lsButton) {
                case "btnUpdate":
                    poJSON = po().UpdateTransaction();
                    pnEditMode = po().getEditMode();
                    if ("error".equals((String) poJSON.get("result"))) {
                        ShowMessageFX.Warning((String) poJSON.get("message"), "Warning", null);
                    }
                    clearDetailFields();
                    pnTblDetailRow = -1;
                    loadTableDetail();
                    pagination.toFront();
                    break;
                case "btnConfirm":
                    if (ShowMessageFX.YesNo(null, psFormName, "Are you sure you want to confirm transaction?")) {
                        poJSON = po().ConfirmTransaction("");
                        if (!"success".equals((String) poJSON.get("result"))) {
                            ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                            break;
                        }
                        ShowMessageFX.Information((String) poJSON.get("message"), psFormName, null);
                        
                        po().OpenTransaction(po().Master().getTransactionNo());
                        
                        if (!"success".equals((poJSON = po().OpenTransaction(po().Master().getTransactionNo())).get("result"))) {
                            ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                            return;
                        }
                        if (ShowMessageFX.YesNo(null, psFormName, "Do you want to print this transaction?")) {
                            poJSON = po().printTransaction(PurchaseOrderStaticData.Printing_CARSp_MCSp_General);
                            if (!"success".equals((String) poJSON.get("result"))) {
                                ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                            }
                        }
                        clearMasterFields();
                        clearDetailFields();
                        detail_data.clear();
                        attachment_data.clear();
                        po().resetattachment();
                        pnEditMode = EditMode.UNKNOWN;
                        pnTblDetailRow = -1;
                        //this code below use to highlight tblpurchase
                        tblVwPurchaseOrder.refresh();
                        main_data.get(pnTblMainRow).setIndex05(PurchaseOrderStatus.CONFIRMED);
                        pagination.toBack();
                    } else {
                        return;
                    }
                    break;
                case "btnSave":
                    if (!ShowMessageFX.YesNo(null, psFormName, "Are you sure you want to save?")) {
                        return;
                    }
                    LocalDate selectedLocalDate = dpTransactionDate.getValue();
                    if (!psOldDate.isEmpty()) {
                        if (!CustomCommonUtil.formatLocalDateToShortString(selectedLocalDate).equals(psOldDate) && tfReferenceNo.getText().isEmpty()) {
                            ShowMessageFX.Warning("A reference number is required for backdated transactions.", psFormName, null);
                            return;
                        }
                    }
                    pnTblDetailRow = -1;
                    if (pnEditMode == EditMode.UPDATE) {
                        po().Master().setModifiedDate(poApp.getServerDate());
                        po().Master().setModifyingId(poApp.getUserID());
                        for (int i = 0; i < po().getDetailCount(); i++) {
                            po().Detail(i).setModifiedDate(poApp.getServerDate());
                        }
                    }

                    poJSON = po().isDetailHasZeroQty();
                    if (!"success".equals((String) poJSON.get("result"))) {
                        if ("true".equals((String) poJSON.get("warning"))) {
                            ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                            pnTblDetailRow = (int) poJSON.get("tableRow");
                            loadTableDetail();
                            loadRecordDetail();
                            initDetailFocus();
                            return;
                        } else {
                            if (!ShowMessageFX.YesNo((String) poJSON.get("message"), psFormName, null)) {
                                pnTblDetailRow = (int) poJSON.get("tableRow");
                                loadTableDetail();
                                loadRecordDetail();
                                initDetailFocus();
                                return;
                            }
                        }
                    }

                    poJSON = po().SaveTransaction();
                    if (!"success".equals((String) poJSON.get("result"))) {
                        ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                        loadTableDetail();
                        return;
                    }
                    ShowMessageFX.Information((String) poJSON.get("message"), psFormName, null);
                    poJSON = po().OpenTransaction(po().Master().getTransactionNo());
                    if ("error".equals(poJSON.get("result"))) {
                        ShowMessageFX.Information((String) poJSON.get("message"), psFormName, null);
                        return;
                    } else {
                        if (po().Master().getConvertedTransactionStatus().equals(PurchaseOrderStatus.OPEN)) {
                            if (ShowMessageFX.YesNo(null, psFormName, "Do you want to confirm this transaction?")) {
                                if ("success".equals((poJSON = po().ConfirmTransaction("")).get("result"))) {
                                    ShowMessageFX.Information((String) poJSON.get("message"), psFormName, null);
                                } else {
                                    ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                                    return;
                                }
                            }
                        }
                    }

                    // Print Transaction Prompt
                    if (ShowMessageFX.YesNo(null, psFormName, "Do you want to print this transaction?")) {
                        poJSON = po().printTransaction(PurchaseOrderStaticData.Printing_CARSp_MCSp_General);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            ShowMessageFX.Warning((String) poJSON.get("message"), "Print Purchase Order", null);
                        }
                    }
                    poJSON = po().OpenTransaction(po().Master().getTransactionNo());
                    pnEditMode = po().getEditMode();
                    loadRecordMaster();
                    loadRecordDetail();
                    loadTableDetail();
                    loadTableDetailFromMain();
                    pagination.toBack();
                    break;
                case "btnCancel":
                    if (ShowMessageFX.YesNo(null, "Cancel Confirmation", "Are you sure you want to cancel?")) {
                        clearMasterFields();
                        clearDetailFields();
                        detail_data.clear();
                        attachment_data.clear();
                        poJSON = po().OpenTransaction(po().Master().getTransactionNo());
                        if ("success".equals((String) poJSON.get("result"))) {
                            pnTblDetailRow = -1;
                            loadRecordMaster();
                            clearDetailFields();
                            pnEditMode = po().getEditMode();
                            loadTableDetail();
                            pagination.toBack();
                        }
                    }
                    break;
                case "btnPrint":
                    poJSON = po().printTransaction(PurchaseOrderStaticData.Printing_CARSp_MCSp_General);
                    if ("error".equals((String) poJSON.get("result"))) {
                        ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                    }
                    break;
                case "btnRetrieve":
                    loadTableMain();
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
                case "btnReturn":
                    if (ShowMessageFX.YesNo(null, psFormName, "Are you sure you want to return transaction?")) {
                        poJSON = po().ReturnTransaction("");
                        if (!"success".equals((String) poJSON.get("result"))) {
                            ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                            break;
                        }
                        ShowMessageFX.Information((String) poJSON.get("message"), psFormName, null);
                        clearMasterFields();
                        clearDetailFields();
                        detail_data.clear();
                        attachment_data.clear();
                        po().resetattachment();
                        pnEditMode = EditMode.UNKNOWN;

                        tblVwPurchaseOrder.refresh();
                        main_data.get(pnTblMainRow).setIndex05(PurchaseOrderStatus.RETURNED);
                    } else {
                        return;
                    }
                    break;
                case "btnVoid":
                    if (ShowMessageFX.YesNo(null, psFormName, "Are you sure you want to void transaction?")) {
                        poJSON = po().VoidTransaction("");
                        if (!"success".equals((String) poJSON.get("result"))) {
                            ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                            break;
                        }
                        ShowMessageFX.Information((String) poJSON.get("message"), psFormName, null);
                        clearMasterFields();
                        clearDetailFields();
                        detail_data.clear();
                        attachment_data.clear();
                        po().resetattachment();
                        pnEditMode = EditMode.UNKNOWN;

                        //this code below use to highlight tblpurchase
                        tblVwPurchaseOrder.refresh();
                        main_data.get(pnTblMainRow).setIndex05(PurchaseOrderStatus.VOID);
                    } else {
                        return;
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
                            } catch (IOException ex) {
                                Logger.getLogger(PurchaseOrderGeneral_ConfirmationController.class.getName()).log(Level.SEVERE, null, ex);
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
            initFields(pnEditMode);
        } catch (CloneNotSupportedException | SQLException | GuanzonException | ParseException ex) {
//            Logger.getLogger(PurchaseOrderGeneral_ConfirmationController.class.getName()).log(Level.SEVERE, null, ex);
            ShowMessageFX.Warning(ex.toString(), psFormName, null);
        } catch (IOException ex) {
            Logger.getLogger(PurchaseOrderGeneral_ConfirmationController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initTextFieldFocus() {
        List<TextField> loTxtField = Arrays.asList(tfReferenceNo, tfDiscountRate, tfDiscountAmount,
                tfAdvancePRate, tfAdvancePAmount, tfOrderQuantity, tfSearchReferenceNo,tfTransactionNo);
        loTxtField.forEach(tf -> tf.focusedProperty().addListener(txtField_Focus));
    }

    private void initTextAreaFocus() {
        taRemarks.focusedProperty().addListener(txtArea_Focus);
    }
    final ChangeListener<? super Boolean> txtField_Focus = (o, ov, nv) -> {
        TextField loTextField = (TextField) ((ReadOnlyBooleanPropertyBase) o).getBean();
        String lsTextFieldID = loTextField.getId();
        String lsValue = loTextField.getText();
        if (lsValue == null) {
            return;
        }
        poJSON = new JSONObject();
        if (!nv) {
            /*Lost Focus*/
            switch (lsTextFieldID) {
                case "tfReferenceNo":
                    po().Master().setReference(lsValue);
                    break;
                case "tfDiscountRate":
                    lsValue = JFXUtil.removeComma(lsValue);
                    poJSON = po().setDiscountRate(lsValue);
                    if (!"success".equals((String) poJSON.get("result"))) {
                        ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                    }
                    tfDiscountRate.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getDiscount().doubleValue()));
                    tfNetAmount.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getNetTotal(), true));
                    break;
                case "tfDiscountAmount":
                    lsValue = JFXUtil.removeComma(lsValue);
                    poJSON = po().setDiscountAmount(lsValue);
                    if (!"success".equals((String) poJSON.get("result"))) {
                        ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                    }
                    tfDiscountAmount.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getAdditionalDiscount(), true));
                    tfNetAmount.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getNetTotal(), true));
                    break;
                case "tfAdvancePRate":
                    lsValue = JFXUtil.removeComma(lsValue);
                    poJSON = po().setAdvancePaymentRate(lsValue);
                    if (!"success".equals((String) poJSON.get("result"))) {
                        ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                    }

                    tfAdvancePRate.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getDownPaymentRatesPercentage().doubleValue()));
                    tfNetAmount.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getNetTotal(), true));
                    break;
                case "tfAdvancePAmount":
                    lsValue = JFXUtil.removeComma(lsValue);
                    poJSON = po().setAdvancePaymentAmount(lsValue);
                    if (!"success".equals((String) poJSON.get("result"))) {
                        ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                    }
                    tfAdvancePAmount.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getDownPaymentRatesAmount(), true));
                    tfNetAmount.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getNetTotal(), true));
                    break;
                case "tfOrderQuantity":
                    break;
                case "tfSearchReferenceNo":
                    psReferID = tfSearchReferenceNo.getText();
                    loadTableMain();
                    break;
                case "tfSearchTransNo":
                    psTransNo = tfSearchTransNo.getText();
                    loadTableMain();
                    break;
                case "tfCost":
                    lsValue = JFXUtil.removeComma(lsValue);
                    setOrderCost(lsValue);
                    loadTableDetailAndSelectedRow();
                    break;
            }
        } else {
            loTextField.selectAll();
        }
    };

    final ChangeListener<? super Boolean> txtArea_Focus = (o, ov, nv) -> {
        TextArea loTextArea = (TextArea) ((ReadOnlyBooleanPropertyBase) o).getBean();
        String lsTextAreaID = loTextArea.getId();
        String lsValue = loTextArea.getText();
        if (lsValue == null) {
            return;
        }
        if (!nv) {
            /*Lost Focus*/
            switch (lsTextAreaID) {
                case "taRemarks":
                    po().Master().setRemarks(lsValue);
                    break;
            }
        } else {
            loTextArea.selectAll();
        }
    };

    private void initTextFieldKeyPressed() {
        List<TextField> loTxtField = Arrays.asList(tfAdvancePAmount,
                tfReferenceNo, tfDiscountRate, tfDiscountAmount,
                tfAdvancePRate,
                tfOrderQuantity, tfSearchSupplier, tfSearchReferenceNo,tfSearchTransNo, tfCost,tfTerm);

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
                             case "tfReferenceNo":
                                poJSON = po().SearchProject(lsValue);
                                if ("error".equals(poJSON.get("result"))) {
                                    ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                                    tfReferenceNo.setText("");
                                    break;
                                }
                                tfReferenceNo.setText(po().Master().getReference());
                                break;
                            case "tfDestination":
                                poJSON = po().SearchDestination(lsValue, false);
                                if ("error".equals(poJSON.get("result"))) {
                                    ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                                    tfDestination.setText("");
                                    break;
                                }
                                tfDestination.setText(po().Master().Branch().getBranchName());
                                break;
                            case "tfSearchSupplier":
                                poJSON = po().SearchSupplier(lsValue, false);
                                if ("error".equals(poJSON.get("result"))) {
                                    ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                                    tfSupplier.setText("");
                                    break;
                                }
                                psSupplierID = po().Master().getSupplierID();
                                tfSearchSupplier.setText(po().Master().Supplier().getCompanyName());
                                loadTableMain();
                                break;
                            case "tfTerm":
                                poJSON = po().SearchTerm(lsValue, false);
                                if ("error".equals(poJSON.get("result"))) {
                                    ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                                    tfTerm.setText("");
                                    break;
                                }
                                tfTerm.setText(po().Master().Term().getDescription());
                                break;

                        }
                        event.consume();
                        switch (txtFieldID) {
                            case "tfDestination":
                            case "tfReferenceNo":
                            case "tfTerm":
                            case "tfAdvancePAmount":
                            case "tfAdvancePRate":
                            case "tfDiscountRate":
                            case "tfDiscountAmount":
                            case "tfSearchReferenceNo":
                            case "tfSearchTransNo":
                                CommonUtils.SetNextFocus((TextField) event.getSource());
                                break;
                            case "tfCost":
                                setOrderCost(tfCost.getText().replace(",", ""));
                                loadTableDetailAndSelectedRow();
                                break;
                            case "tfOrderQuantity":
                                setOrderQuantityToDetail(tfOrderQuantity.getText());
                                poJSON = po().netTotalChecker(pnTblDetailRow);
                                if ("error".equals(poJSON.get("result"))) {
                                    ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                                    tfOrderQuantity.setText("0");
                                    loadRecordMaster();
                                    loadTableDetailAndSelectedRow();
                                    break;
                                }
                                poJSON = po().netTotalChecker(pnTblDetailRow);
                                if ("error".equals(poJSON.get("result"))) {
                                    ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                                    tfOrderQuantity.setText("0");
                                    loadRecordMaster();
                                    loadTableDetailAndSelectedRow();
                                    break;
                                }
                                if (!detail_data.isEmpty() && pnTblDetailRow < detail_data.size() - 1) {
                                    pnTblDetailRow++;
                                }
                                CommonUtils.SetNextFocus((TextField) event.getSource());
                                loadTableDetailAndSelectedRow();
                                break;
                        }
                        event.consume();
                        break;
                    case UP:
                        setOrderQuantityToDetail(tfOrderQuantity.getText());
                        poJSON = po().netTotalChecker(pnTblDetailRow);
                        if ("error".equals(poJSON.get("result"))) {
                            ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                            tfOrderQuantity.setText("0");
                            loadRecordMaster();
                            loadTableDetailAndSelectedRow();
                            break;
                        }
                        if (!lsTxtField.equals("tfBrand") && !lsTxtField.equals("tfModel")) {
                            if (pnTblDetailRow > 0 && !detail_data.isEmpty()) {
                                pnTblDetailRow--;
                            }
                        }

                        // Prevent going from 'tfOrderQuantity' to 'taRemarks'
                        if (!lsTxtField.equals("tfBrand") && !lsTxtField.equals("tfOrderQuantity")) {
                            CommonUtils.SetPreviousFocus((TextField) event.getSource());
                        }
                        loadTableDetailAndSelectedRow();
                        event.consume();
                        break;
                    case DOWN:
                        setOrderQuantityToDetail(tfOrderQuantity.getText());
                        poJSON = po().netTotalChecker(pnTblDetailRow);
                        if ("error".equals(poJSON.get("result"))) {
                            ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                            tfOrderQuantity.setText("0");
                            loadRecordMaster();
                            loadTableDetailAndSelectedRow();
                            break;
                        }
                        if ("tfOrderQuantity".equals(lsTxtField.getId())) {
                            if (!detail_data.isEmpty() && pnTblDetailRow < detail_data.size() - 1) {
                                pnTblDetailRow++;
                            }
                        }
                        CommonUtils.SetNextFocus(lsTxtField);
                        loadTableDetailAndSelectedRow();
                        event.consume(); // Consume event after handling focus
                        break;
                    default:
                        break;

                }
            }
        } catch (ExceptionInInitializerError | SQLException | GuanzonException | NullPointerException ex) {
            Logger.getLogger(PurchaseOrderGeneral_ConfirmationController.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setOrderCost(String fsValue) {
        if (fsValue.isEmpty()) {
            fsValue = "0.0000";
        }

        double lnCostDetail = Double.parseDouble(fsValue);
        if (lnCostDetail < 0.0000) {
            ShowMessageFX.Warning("Invalid Order Cost", psFormName, null);
            fsValue = "0.0000";
        }

        po().Detail(pnTblDetailRow).setUnitPrice(lnCostDetail);
        tfCost.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Detail(pnTblDetailRow).getUnitPrice(), true));
    }

    private void setOrderQuantityToDetail(String fsValue) {
        if (fsValue.isEmpty()) {
            fsValue = "0";
        }
        if (Integer.parseInt(fsValue) < 0) {
            ShowMessageFX.Warning("Invalid Order Quantity", psFormName, null);
            fsValue = "0";
        }
        if (pnTblDetailRow < 0) {
            fsValue = "0";
            ShowMessageFX.Warning("Invalid row to update.", psFormName, null);
            clearDetailFields();
            int detailCount = po().getDetailCount();
            pnTblDetailRow = detailCount > 0 ? detailCount - 1 : 0;
        }
        double lnRequestQuantity = 0;
        try {
            System.out.println("SOURCE CODE: " + po().Detail(pnTblDetailRow).getSouceCode());
            if (PurchaseOrderStatus.SourceCode.POQUOTATION.equals(po().Detail(pnTblDetailRow).getSouceCode())) {
                lnRequestQuantity = po().Detail(pnTblDetailRow).POQuotationDetail().getQuantity();
            } else {
                lnRequestQuantity = po().Detail(pnTblDetailRow).InvStockRequestDetail().getApproved();
            }
            if (!po().Detail(pnTblDetailRow).getSouceNo().isEmpty()) {
                if (Integer.parseInt(fsValue) > lnRequestQuantity) {
                    ShowMessageFX.Warning("Invalid order quantity entered. The item is from a stock request, and the order quantity must not be greater than the requested quantity.", psFormName, null);
                    fsValue = "0";

                }
            }
        } catch (GuanzonException | SQLException ex) {
            Logger.getLogger(PurchaseOrderGeneral_ConfirmationController.class.getName()).log(Level.SEVERE, null, ex);
        }
        po().Detail(pnTblDetailRow).setQuantity(Double.valueOf(fsValue));
        tfOrderQuantity.setText(String.valueOf(po().Detail(pnTblDetailRow).getQuantity().intValue()));
    }

    private void initTextFieldPattern() {
        JFXUtil.inputDecimalOnly(tfDiscountRate, tfAdvancePRate);
        JFXUtil.setCommaFormatter(tfCost, tfDiscountAmount, tfAdvancePAmount);
        CustomCommonUtil.inputIntegersOnly(tfOrderQuantity);
    }

    private void initDatePickerActions() {
        dpTransactionDate.setOnAction(e -> {
            if (pnEditMode == EditMode.UPDATE) {
                LocalDate selectedLocalDate = dpTransactionDate.getValue();
                LocalDate transactionDate = new java.sql.Date(po().Master().getTransactionDate().getTime()).toLocalDate();
                if (selectedLocalDate == null) {
                    return;
                }
                LocalDate dateNow = LocalDate.now();
                String lsReferNo = tfReferenceNo.getText().trim();
                boolean approved = true;
                psOldDate = CustomCommonUtil.formatLocalDateToShortString(transactionDate);
                if (selectedLocalDate.isAfter(dateNow)) {
                    ShowMessageFX.Warning("Invalid to future date.", psFormName, null);
                    approved = false;
                }

                if (selectedLocalDate.isBefore(transactionDate) && lsReferNo.isEmpty()) {
                    ShowMessageFX.Warning("Invalid to backdate. Please enter a reference number first.", psFormName, null);
                    approved = false;
                }
                if (selectedLocalDate.isBefore(transactionDate) && !lsReferNo.isEmpty()) {
                    boolean proceed = ShowMessageFX.YesNo(
                            "You are changing the transaction date\n"
                            + "If YES, seek approval to proceed with the changed date.\n"
                            + "If NO, the transaction date will be remain.",
                            psFormName, null
                    );
                    if (proceed) {
                        if (poApp.getUserLevel() <= UserRight.ENCODER) {
                            poJSON = ShowDialogFX.getUserApproval(poApp);
                            if (!"success".equals((String) poJSON.get("result"))) {
                                approved = false;
                                return;
                            } else {
                                if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                                    ShowMessageFX.Warning("User is not an authorized approving officer..", psFormName, null);
                                    approved = false;
                                    return;
                                }
                                po().setApproving((String) poJSON.get("sUserIDxx"));
                            }
                        }
                    } else {
                        approved = false;
                    }
                }
                if (approved) {
                    po().Master().setTransactionDate(
                            SQLUtil.toDate(selectedLocalDate.toString(), SQLUtil.FORMAT_SHORT_DATE));
                } else {
                    po().Master().setTransactionDate(
                            SQLUtil.toDate(psOldDate, SQLUtil.FORMAT_SHORT_DATE));
                }
                dpTransactionDate.setValue(CustomCommonUtil.parseDateStringToLocalDate(
                        SQLUtil.dateFormat(po().Master().getTransactionDate(), SQLUtil.FORMAT_SHORT_DATE)));
            }
        }
        );

        dpExpectedDlvrDate.setOnAction(e
                -> {
            if (pnEditMode == EditMode.ADDNEW || pnEditMode == EditMode.UPDATE) {
                if (dpExpectedDlvrDate.getValue() != null) {
                    LocalDate selectedLocalDate = dpExpectedDlvrDate.getValue();
                    Date selectedDate = SQLUtil.toDate(selectedLocalDate.toString(), SQLUtil.FORMAT_SHORT_DATE);
                    Date transactionDate = po().Master().getTransactionDate();
                    LocalDate transactionLocalDate = LocalDate.now();

                    if (selectedDate.before(transactionDate)) {
                        ShowMessageFX.Warning("Please select an expected delivery date that is on or after the transaction date.", "Invalid Expected Delivery Date", null);
                        dpExpectedDlvrDate.setValue(transactionLocalDate);
                        po().Master().setExpectedDate(transactionDate);
                        return;
                    }

                    po().Master().setExpectedDate(selectedDate);
                }
            }
        }
        );
    }

    private void initCheckBoxActions() {
        chkbAdvancePayment.setOnAction(event -> {
            if (pnEditMode == EditMode.ADDNEW || pnEditMode == EditMode.UPDATE) {
                poJSON = new JSONObject();
                if (tfTotalAmount.getText().isEmpty()
                        || Double.parseDouble(tfTotalAmount.getText().replace(",", "")) > 0.0000) {

                    if (chkbAdvancePayment.isSelected()) {
                        chkbAdvancePayment.setSelected(true);
                        po().Master().setWithAdvPaym(true);
                    } else {
                        if (Double.parseDouble(tfAdvancePAmount.getText().replace(",", "")) > 0.0000
                                || Double.parseDouble(tfAdvancePRate.getText().replace(",", "")) > 0.00) {
                            if (ShowMessageFX.YesNo("An advance payment amount has already been entered. Are you sure you want to unselect it?", psFormName, null)) {
                                po().Master().setWithAdvPaym(false);
                                chkbAdvancePayment.setSelected(false);
                                po().Master().setDownPaymentRatesAmount(0.0000);
                                po().Master().setDownPaymentRatesPercentage(0.00);
                                tfAdvancePRate.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getDownPaymentRatesPercentage().doubleValue()));
                                tfAdvancePAmount.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getDownPaymentRatesAmount(), true));
                                po().computeNetTotal();
                                tfNetAmount.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getNetTotal(), true));
                            } else {
                                chkbAdvancePayment.setSelected(true);
                                po().Master().setWithAdvPaym(true);
                                return;
                            }
                        } else {
                            po().Master().setWithAdvPaym(false);
                            chkbAdvancePayment.setSelected(false);
                        }
                    }
                } else {
                    ShowMessageFX.Warning("Advance payment cannot be entered until the total amount is greater than 0.00.", psFormName, null);
                    po().Master().setWithAdvPaym(false);
                    chkbAdvancePayment.setSelected(false);
                }
                initFields(pnEditMode);
            }
        });
    }

    private void clearMasterFields() {
        /* Master Fields*/
        lblTransactionStatus.setText("UNKNOWN");
        pnTblDetailRow = -1;
        dpTransactionDate.setValue(null);
        dpExpectedDlvrDate.setValue(null);
        taRemarks.setText("");
        psOldDate = "";
        CustomCommonUtil.setSelected(false, chkbAdvancePayment);
        CustomCommonUtil.setText("", tfTransactionNo,
                tfDestination, tfReferenceNo, tfTerm);
        CustomCommonUtil.setText("0.0000", tfTotalAmount, tfAdvancePAmount, tfAdvancePRate,
                tfDiscountAmount, tfDiscountRate, tfNetAmount);
    }

    private void clearDetailFields() {
        CustomCommonUtil.setText("", tfBarcode, tfDescription, tfBrand, tfModel,
                tfColor, tfCategory, tfInventoryType, tfMeasure, tfClass, tfAMC
        );
        CustomCommonUtil.setText("0", tfOrderQuantity, tfQOH, tfRequestQuantity, tfBO, tfRO, tfROQ);
        tfCost.setText("0.0000");
    }

    private void initButtons(int fnEditMode) {
        boolean lbShow = (pnEditMode == EditMode.UPDATE);

        btnClose.setVisible(!lbShow);
        btnClose.setManaged(!lbShow);

        CustomCommonUtil.setVisible(lbShow, btnSave, btnCancel);
        CustomCommonUtil.setManaged(lbShow, btnSave, btnCancel);

        CustomCommonUtil.setVisible(false, btnConfirm, btnReturn, btnVoid, btnUpdate, btnPrint);
        CustomCommonUtil.setManaged(false, btnConfirm, btnReturn, btnVoid, btnUpdate, btnPrint);

        btnTransHistory.setVisible(fnEditMode != EditMode.UNKNOWN);
        btnTransHistory.setManaged(fnEditMode != EditMode.UNKNOWN);
        if (po().Master().getPrint().equals("1")) {
            btnPrint.setText("Reprint");
        } else {
            btnPrint.setText("Print");
        }
        if (fnEditMode == EditMode.READY) {
            switch (po().Master().getConvertedTransactionStatus()) {
                case PurchaseOrderStatus.OPEN:
                case PurchaseOrderStatus.RETURNED:
                    CustomCommonUtil.setVisible(true, btnConfirm, btnVoid, btnUpdate, btnPrint);
                    CustomCommonUtil.setManaged(true, btnConfirm, btnVoid, btnUpdate, btnPrint);
                    break;
                case PurchaseOrderStatus.CONFIRMED:
                    CustomCommonUtil.setVisible(true, btnReturn, btnVoid, btnUpdate, btnPrint);
                    CustomCommonUtil.setManaged(true, btnReturn, btnVoid, btnUpdate, btnPrint);
                    break;
                case PurchaseOrderStatus.APPROVED:
                    btnPrint.setVisible(true);
                    btnPrint.setManaged(true);
                    break;
            }
        }
    }

    private void initFields(int fnEditMode) {
        boolean lbShow = (fnEditMode == EditMode.UPDATE);
         if (po().Master().getConvertedTransactionStatus().equals(PurchaseOrderStatus.OPEN) || 
                po().Master().getConvertedTransactionStatus().equals(PurchaseOrderStatus.RETURNED)) {
            CustomCommonUtil.setDisable(!lbShow, AnchorMaster, AnchorDetails);
            CustomCommonUtil.setDisable(!lbShow,
                    dpTransactionDate, tfDestination, taRemarks,
                    dpExpectedDlvrDate, tfReferenceNo, tfTerm,
                    chkbAdvancePayment);

            CustomCommonUtil.setDisable(true, tfSupplier, tfDiscountRate, tfDiscountAmount,
                    tfAdvancePRate, tfAdvancePAmount, tfBarcode, tfDescription, tfOrderQuantity, tfCost);

            CustomCommonUtil.setDisable(!lbShow, tfOrderQuantity, tfCost);

            if (!tfReferenceNo.getText().isEmpty()) {
                dpTransactionDate.setDisable(!lbShow);
            }
            if (chkbAdvancePayment.isSelected()) {
                CustomCommonUtil.setDisable(!lbShow, tfAdvancePRate, tfAdvancePAmount);
            }
            if (po().Master().getTranTotal().doubleValue() > 0.0000) {
                CustomCommonUtil.setDisable(!lbShow, tfDiscountRate, tfDiscountAmount);
            }
        } else {
            CustomCommonUtil.setDisable(true, AnchorMaster, AnchorDetails);
        }
        if (tblVwPurchaseOrder.getItems().isEmpty()) {
            pagination.setVisible(false);
            pagination.setManaged(false);
        }
    }

    private void loadTableMain() {
        btnRetrieve.setDisable(true);
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxHeight(50); // Set size to 200x200
        progressIndicator.setStyle("-fx-progress-color: #FF8201;");
        StackPane loadingPane = new StackPane(progressIndicator);
        loadingPane.setAlignment(Pos.CENTER); // Center it

        tblVwPurchaseOrder.setPlaceholder(loadingPane); // Show while loading
        progressIndicator.setVisible(true); // Make sure it's visible

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    main_data.clear();
                    poJSON = po().getPurchaseOrder(psSupplierID, psReferID,psTransNo);
                    if ("success".equals(poJSON.get("result"))) {
                        if (po().getPOMasterCount() > 0) {
                            for (int lnCntr = 0; lnCntr <= po().getPOMasterCount() - 1; lnCntr++) {
                                main_data.add(new ModelPurchaseOrder(
                                        String.valueOf(lnCntr + 1),
                                        po().POMaster(lnCntr).Supplier().getCompanyName(),
                                        SQLUtil.dateFormat(po().POMaster(lnCntr).getTransactionDate(), SQLUtil.FORMAT_SHORT_DATE),
                                        po().POMaster(lnCntr).getTransactionNo(),
                                        "",
                                        "",
                                        "",
                                        "",
                                        "",
                                        ""));
                            }
                        } else {
                            main_data.clear();
                        }
                    }

                    Platform.runLater(() -> {
                        if (main_data.isEmpty()) {
                            tblVwPurchaseOrder.setPlaceholder(new Label("NO RECORD TO LOAD"));
                            tblVwPurchaseOrder.setItems(FXCollections.observableArrayList(main_data));
                        } else {
                            tblVwPurchaseOrder.setItems(FXCollections.observableArrayList(main_data));
                        }
                    });

                } catch (SQLException | GuanzonException ex) {
                    Logger.getLogger(PurchaseOrderGeneral_ConfirmationController.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
                return null;
            }

            @Override
            protected void succeeded() {
                progressIndicator.setVisible(false);
                btnRetrieve.setDisable(false);
                if (main_data == null || main_data.isEmpty()) {
                    tblVwPurchaseOrder.setPlaceholder(new Label("NO RECORD TO LOAD"));
                } else {
                    if (pagination != null) {
                        int pageCount = (int) Math.ceil((double) main_data.size() / pnTblMain_Page);
                        pagination.setPageCount(pageCount);
                        pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> createPage(newIndex.intValue()));
                    }
                    createPage(0);
                    pagination.setVisible(true);
                    pagination.setManaged(true);
                    tblVwPurchaseOrder.toFront();
                }
            }

            @Override
            protected void failed() {
                progressIndicator.setVisible(false);
                btnRetrieve.setDisable(false);
            }
        };
        new Thread(task).start(); // Run task in background
    }

    private Node createPage(int pageIndex) {
        int totalPages = (int) Math.ceil((double) main_data.size() / pnTblMain_Page);
        if (totalPages == 0) {
            totalPages = 1;
        }

        pageIndex = Math.max(0, Math.min(pageIndex, totalPages - 1));
        int fromIndex = pageIndex * pnTblMain_Page;
        int toIndex = Math.min(fromIndex + pnTblMain_Page, main_data.size());

        if (!main_data.isEmpty()) {
            tblVwPurchaseOrder.setItems(FXCollections.observableArrayList(main_data.subList(fromIndex, toIndex)));
        }

        if (pagination != null) { // Replace with your actual Pagination variable
            pagination.setPageCount(totalPages);
            pagination.setCurrentPageIndex(pageIndex);
        }

        return tblVwPurchaseOrder;
    }

    private void initTableMain() {
        JFXUtil.setColumnCenter(tblRowNo, tblDate, tblTransactionNo);
        JFXUtil.setColumnLeft(tblSupplier);
        JFXUtil.setColumnsIndexAndDisableReordering(tblVwPurchaseOrder);
        initTableHighlithers();
    }

    private void initTableHighlithers() {
        tblVwPurchaseOrder.setRowFactory(tv -> new TableRow<ModelPurchaseOrder>() {
            @Override
            protected void updateItem(ModelPurchaseOrder item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setStyle("");
                } else {
                    // Assuming empIndex05 corresponds to an employee status
                    String status = item.getIndex05(); // Replace with actual getter
                    switch (status) {
                        case PurchaseOrderStatus.CONFIRMED:
                            setStyle("-fx-background-color: #C1E1C1;");
                            break;
                        case PurchaseOrderStatus.VOID:
                            setStyle("-fx-background-color: #FAA0A0;");
                            break;
                        case PurchaseOrderStatus.RETURNED:
                            setStyle("-fx-background-color: #FAC898;");
                            break;
                        default:
                            setStyle("");
                            break;
                    }
                    tblVwPurchaseOrder.refresh();
                }
            }
        });
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
                    if ((pnEditMode == EditMode.ADDNEW || pnEditMode == EditMode.UPDATE)) {
                        Model_PO_Detail lastDetail = po().Detail(detailCount - 1);
                        if (lastDetail.getStockID() != null && !lastDetail.getStockID().isEmpty()) {
                            po().AddDetail();
                            detailCount++;
                        }
                    }
                    double grandTotalAmount = 0.0000;
                    List<ModelPurchaseOrderDetail> detailsList = new ArrayList<>();

                    for (int lnCtr = 0; lnCtr < detailCount; lnCtr++) {
                        Model_PO_Detail orderDetail = po().Detail(lnCtr);
                        double lnTotalAmount = orderDetail.getUnitPrice().doubleValue() * orderDetail.getQuantity().intValue();
                        grandTotalAmount += lnTotalAmount;
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
                                CustomCommonUtil.setDecimalValueToIntegerFormat(orderDetail.getQuantity().doubleValue()),
                                CustomCommonUtil.setIntegerValueToDecimalFormat(lnTotalAmount, true),
                                status
                        ));
                    }
                    final double totalAmountFinal = grandTotalAmount;
                    Platform.runLater(() -> {
                        detail_data.setAll(detailsList); // Properly update list
                        tblVwOrderDetails.setItems(detail_data);
                        if (pnEditMode == EditMode.ADDNEW || pnEditMode == EditMode.UPDATE) {
                            if (totalAmountFinal <= 0.0000) {
                                tfDiscountRate.setText("0.00");
                                tfAdvancePRate.setText("0.00");
                                tfDiscountAmount.setText("0.0000");
                                tfAdvancePAmount.setText("0.0000");
                                po().Master().setAdditionalDiscount(0.0000);
                                po().Master().setDiscount(0.00);
                                po().Master().setDownPaymentRatesAmount(0.0000);
                                po().Master().setDownPaymentRatesPercentage(0.00);
                            }
                            po().Master().setTranTotal(totalAmountFinal);
                            po().computeNetTotal();
                            tfDiscountRate.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getDiscount().doubleValue()));
                            tfAdvancePRate.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getDownPaymentRatesPercentage().doubleValue()));
                            tfTotalAmount.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getTranTotal(), true
                            ));
                            tfNetAmount.setText(CustomCommonUtil.setIntegerValueToDecimalFormat(po().Master().getNetTotal(), true));
                        }
                        reselectLastDetailRow();
                        initFields(pnEditMode);
                    });

                    return detailsList;

                } catch (GuanzonException | SQLException ex) {
                    Logger.getLogger(PurchaseOrderGeneral_ConfirmationController.class
                            .getName()).log(Level.SEVERE, null, ex);
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

    private void reselectLastDetailRow() {
        if (pnTblDetailRow >= 0 && pnTblDetailRow < tblVwOrderDetails.getItems().size()) {
            tblVwOrderDetails.getSelectionModel().clearAndSelect(pnTblDetailRow);
            tblVwOrderDetails.getSelectionModel().focus(pnTblDetailRow);
        }
    }

    private void tblVwDetail_Clicked(MouseEvent event) {
        if (pnEditMode == EditMode.UPDATE || pnEditMode == EditMode.READY) {
            pnTblDetailRow = tblVwOrderDetails.getSelectionModel().getSelectedIndex();
            ModelPurchaseOrderDetail selectedItem = tblVwOrderDetails.getSelectionModel().getSelectedItem();
            if (event.getClickCount() == 1) {
                clearDetailFields();
                if (selectedItem != null) {
                    if (pnTblDetailRow >= 0) {
                        loadRecordDetail();
                        initDetailFocus();
                    }
                }
                initFields(pnEditMode);
            }
        }
    }

    private void tblVwMain_Clicked(MouseEvent event) {
        poJSON = new JSONObject();
        pnTblMainRow = tblVwPurchaseOrder.getSelectionModel().getSelectedIndex();
        if (pnTblMainRow < 0 || pnTblMainRow >= tblVwPurchaseOrder.getItems().size()) {
            ShowMessageFX.Warning("Please select valid purchase order information.", "Warning", null);
            return;
        }

        if (event.getClickCount() == 2) {
            ModelPurchaseOrder loSelectedPurchaseOrder = (ModelPurchaseOrder) tblVwPurchaseOrder.getSelectionModel().getSelectedItem();
            if (loSelectedPurchaseOrder != null) {
                String lsTransactionNo = loSelectedPurchaseOrder.getIndex04();
                try {
                    poJSON = po().InitTransaction();
                    if ("success".equals((String) poJSON.get("result"))) {
                        poJSON = po().OpenTransaction(lsTransactionNo);
                        if ("success".equals((String) poJSON.get("result"))) {
                            loadRecordMaster();
                            initTableDetail();
                            loadTableDetail();
                            
                            loadTableDetailFromMain();
                            pnTblDetailRow = -1;
                            clearDetailFields();
                            pnEditMode = po().getEditMode();
                        } else {
                            ShowMessageFX.Warning((String) poJSON.get("message"), psFormName, null);
                            pnEditMode = EditMode.UNKNOWN;
                        }
                        
                        initButtons(pnEditMode);
                        initFields(pnEditMode);

                    }
                } catch (CloneNotSupportedException | SQLException | GuanzonException ex) {
                    Logger.getLogger(PurchaseOrderGeneral_ConfirmationController.class
                            .getName()).log(Level.SEVERE, null, ex);
                    ShowMessageFX.Warning("Error loading data: " + ex.getMessage(), psFormName, null);
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

        if (focusedCell != null && "tblVwOrderDetails".equals(currentTable.getId())) {
            switch (event.getCode()) {
                case TAB:
                case DOWN:
                    pnTblDetailRow = pnTblDetailRow;
                    if (pnEditMode != EditMode.ADDNEW || pnEditMode != EditMode.UPDATE) {
                        pnTblDetailRow = moveToNextRow(currentTable, focusedCell);
                    }
                    break;
                case UP:
                    pnTblDetailRow = pnTblDetailRow;
                    if (pnEditMode != EditMode.ADDNEW || pnEditMode != EditMode.UPDATE) {
                        pnTblDetailRow = moveToPreviousRow(currentTable, focusedCell);
                    }
                    break;
                default:
                    return;
            }
            currentTable.getSelectionModel().select(pnTblDetailRow);
            currentTable.getFocusModel().focus(pnTblDetailRow);
            loadRecordDetail();
            initDetailFocus();
            event.consume();
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
                    loadTableMain();
                }

            }
        });
        tfSearchReferenceNo.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue.isEmpty()) {
                    po().Master().setReference("");
                    tfSearchReferenceNo.setText("");
                    psReferID = "";
                    loadTableMain();
                }
            }
        });
    }

    private void initDetailFocus() {
        if (pnEditMode == EditMode.UPDATE) {
            if (pnTblDetailRow >= 0) {
                tfOrderQuantity.requestFocus();
            }
        }
    }

    private void loadTableDetailAndSelectedRow() {
        if (pnTblDetailRow >= 0) {
            Platform.runLater(() -> {
                PauseTransition delay = new PauseTransition(Duration.millis(10));
                delay.setOnFinished(event -> {
                    Platform.runLater(() -> {
                        loadTableDetail();

                    });
                });
                delay.play();
            });
            loadRecordDetail();
            initDetailFocus();
        }
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
