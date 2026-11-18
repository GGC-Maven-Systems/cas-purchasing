package ph.com.guanzongroup.cas.purchasing.ui.utility;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * JavaFX helpers used by the Purchase Order screens.
 *
 * <p>
 * Copied (only the methods the screens use) from
 * ph.com.guanzongroup.integsys.utility.JFXUtil, with two changes so it works on
 * any Java/JavaFX version (no com.sun.javafx internals, no javafx-swing):</p>
 * <ul>
 * <li>column reordering is undone through the columns list instead of the
 * internal TableHeaderRow;</li>
 * <li>PDF pages are converted to JavaFX images without SwingFXUtils.</li>
 * </ul>
 */
public final class JFXUtil {

    private JFXUtil() {
    }

    // =====================================================================
    // Values
    // =====================================================================
    /** Whether source equals any of others (null matches null). */
    public static boolean isObjectEqualTo(Object source, Object... others) {
        if (source == null && others != null) {
            for (Object other : others) {
                if (other == null) {
                    return true;
                }
            }
            return false;
        }
        for (Object other : others) {
            if (source != null && source.equals(other)) {
                return true;
            }
        }
        return false;
    }

    /** Text without commas; "0" when empty or negative zero. */
    public static String removeComma(String numberStr) {
        if (numberStr == null || numberStr.isEmpty()) {
            return "0";
        }
        String clean = numberStr.replace(",", "");
        if (clean.matches("-0+(\\.0+)?")) {
            return "0";
        }
        return clean.isEmpty() ? "0" : clean;
    }

    // =====================================================================
    // Fields
    // =====================================================================
    public static void setDisabled(boolean disable, Object... elements) {
        for (Object obj : elements) {
            if (obj instanceof DatePicker) {
                DatePicker dp = (DatePicker) obj;
                dp.setDisable(disable);
                dp.getEditor().setEditable(!disable);
                dp.getEditor().setDisable(false);
            } else if (obj instanceof TextArea) {
                TextArea ta = (TextArea) obj;
                ta.setDisable(disable);
                ta.setEditable(!disable);
                ta.setMouseTransparent(false);
                ta.setCursor(!disable ? Cursor.TEXT : Cursor.DEFAULT);
            } else if (obj instanceof TextField) {
                TextField tf = (TextField) obj;
                tf.setDisable(disable);
                tf.setEditable(!disable);
                if (disable) {
                    if (!tf.getStyleClass().contains("DisabledTextField")) {
                        tf.getStyleClass().add("DisabledTextField");
                    }
                } else {
                    tf.getStyleClass().remove("DisabledTextField");
                }
            } else if (obj instanceof Node) {
                ((Node) obj).setDisable(disable);
            }
        }
    }

    /** Clears every text input, date, check box and combo box inside the panes. */
    public static void clearTextFields(AnchorPane... anchorPanes) {
        for (AnchorPane pane : anchorPanes) {
            clearTextInputsRecursive(pane);
        }
    }

    private static void clearTextInputsRecursive(Parent parent) {
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof TextInputControl) {
                ((TextInputControl) node).clear();
            } else if (node instanceof DatePicker) {
                DatePicker dp = (DatePicker) node;
                dp.setValue(null);
                if (dp.getEditor() != null) {
                    dp.getEditor().clear();
                }
            } else if (node instanceof CheckBox) {
                ((CheckBox) node).setSelected(false);
            } else if (node instanceof ComboBox) {
                ComboBox<?> combo = (ComboBox<?>) node;
                EventHandler<ActionEvent> savedHandler = combo.getOnAction();
                combo.setOnAction(null);
                combo.getSelectionModel().select(null);
                combo.setOnAction(savedHandler);
            } else if (node instanceof Parent) {
                clearTextInputsRecursive((Parent) node);
            }
        }
    }

    /** Digits, commas and at most one dot. */
    public static void inputDecimalOnly(TextField... foTxtFields) {
        for (TextField txtField : foTxtFields) {
            if (txtField != null) {
                UnaryOperator<TextFormatter.Change> filter = change -> {
                    String newText = change.getControlNewText();
                    if (!newText.matches("[0-9,]*\\.?[0-9]*")) {
                        return null;
                    }
                    return change;
                };
                txtField.setTextFormatter(new TextFormatter<>(filter));
            }
        }
    }

    /** Numbers with thousands separators while typing. */
    public static void setCommaFormatter(TextField... textFields) {
        for (TextField textField : textFields) {
            final AtomicBoolean isAdjusting = new AtomicBoolean(false);
            final boolean[] isUpdating = {false};
            final int[] newCaretPos = {0};

            UnaryOperator<TextFormatter.Change> filter = change -> {
                String newText = change.getControlNewText();
                if (!newText.matches("[\\d,\\.]*")) {
                    return null;
                }
                if (newText.chars().filter(c -> c == '.').count() > 1) {
                    return null;
                }
                return change;
            };
            textField.setTextFormatter(new TextFormatter<>(filter));

            textField.textProperty().addListener((obs, oldValue, newValue) -> {
                if (isAdjusting.get() || isUpdating[0] || newValue == null) {
                    return;
                }
                isUpdating[0] = true;
                try {
                    String clean = newValue.replaceAll(",", "");
                    if (clean.isEmpty() || clean.equals(".") || clean.matches("0*\\.0*")) {
                        return;
                    }
                    String integerPart = clean;
                    String decimalPart = "";
                    int dotIndex = clean.indexOf('.');
                    if (dotIndex >= 0) {
                        integerPart = clean.substring(0, dotIndex);
                        decimalPart = clean.substring(dotIndex);
                    }
                    long integerVal = integerPart.isEmpty() ? 0 : Long.parseLong(integerPart);
                    String formatted = NumberFormat.getIntegerInstance(Locale.US).format(integerVal) + decimalPart;
                    Platform.runLater(() -> {
                        isAdjusting.set(true);
                        int originalCaretPos = textField.getCaretPosition();
                        textField.setText(formatted);
                        newCaretPos[0] = Math.max(0, Math.min(formatted.length(),
                                originalCaretPos + formatted.length() - newValue.length()));
                        isAdjusting.set(false);
                    });
                    Platform.runLater(() -> textField.positionCaret(newCaretPos[0]));
                } catch (NumberFormatException e) {
                    //leave the text as typed
                } finally {
                    isUpdating[0] = false;
                }
            });
        }
    }

    /** Combo box list cells highlighted with hexColor. */
    public static void initComboBoxCellDesignColor(String hexColor, ComboBox<?>... comboBoxes) {
        for (ComboBox<?> comboBox : comboBoxes) {
            initComboBoxCellDesignColor(comboBox, hexColor);
        }
    }

    public static <T> void initComboBoxCellDesignColor(ComboBox<T> comboBox, String hexcolor) {
        comboBox.setCellFactory(param -> new ListCell<T>() {
            {
                hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                    if (!isEmpty() && getItem() != null) {
                        if (isNowHovered) {
                            setStyle("-fx-background-color: " + hexcolor + "; -fx-text-fill: "
                                    + (isSelected() ? "black" : "white") + ";");
                        } else {
                            updateColor();
                        }
                    }
                });
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item.toString());
                updateColor();
                selectedProperty().addListener((obs, oldSel, newSel) -> updateColor());
                focusedProperty().addListener((obs, oldFoc, newFoc) -> updateColor());
            }

            private void updateColor() {
                if (isSelected() || isFocused()) {
                    setStyle("-fx-background-color: " + hexcolor + "; -fx-text-fill: black;");
                } else {
                    setStyle("-fx-background-color: transparent; -fx-text-fill: black;");
                }
            }
        });
        comboBox.setButtonCell(new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    setStyle("-fx-background-color: transparent; -fx-text-fill: black;");
                }
            }
        });
    }

    // =====================================================================
    // Tables
    // =====================================================================
    public static void setColumnCenter(TableColumn<?, ?>... columns) {
        for (TableColumn<?, ?> column : columns) {
            column.setStyle("-fx-alignment: CENTER;");
        }
    }

    public static void setColumnLeft(TableColumn<?, ?>... columns) {
        for (TableColumn<?, ?> column : columns) {
            column.setStyle("-fx-alignment: CENTER-LEFT;-fx-padding: 0 5 0 5;");
        }
    }

    public static void setColumnRight(TableColumn<?, ?>... columns) {
        for (TableColumn<?, ?> column : columns) {
            column.setStyle("-fx-alignment: CENTER-RIGHT;-fx-padding: 0 5 0 5;");
        }
    }

    /**
     * Binds column N to property indexNN of the row model, turns off sorting
     * and editing, and keeps the columns in their FXML order.
     */
    public static void setColumnsIndexAndDisableReordering(final TableView<?> tableView) {
        int counter = 1;
        for (Object obj : tableView.getColumns()) {
            @SuppressWarnings("unchecked")
            final TableColumn<Object, Object> column = (TableColumn<Object, Object>) obj;
            column.setCellValueFactory(new PropertyValueFactory<>(String.format("index%02d", counter++)));
            column.setSortable(false);
            column.setEditable(false);
            column.setCellFactory(col -> {
                TableCell<Object, Object> cell = new TableCell<Object, Object>() {
                    @Override
                    protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.toString().replaceAll("\\r?\\n", ""));
                    }
                };
                cell.setWrapText(false);
                cell.setTextOverrun(OverrunStyle.ELLIPSIS);
                return cell;
            });
        }
        keepColumnOrder(tableView);
    }

    /** Puts the columns back in their original order when the user drags one. */
    private static <S> void keepColumnOrder(TableView<S> tableView) {
        if (tableView.getProperties().containsKey("keepColumnOrder")) {
            return;
        }
        tableView.getProperties().put("keepColumnOrder", Boolean.TRUE);
        final List<TableColumn<S, ?>> laOrder = new ArrayList<>(tableView.getColumns());
        final boolean[] lbRestoring = {false};
        tableView.getColumns().addListener((ListChangeListener<TableColumn<S, ?>>) change -> {
            if (lbRestoring[0] || tableView.getColumns().equals(laOrder)) {
                return;
            }
            lbRestoring[0] = true;
            Platform.runLater(() -> {
                tableView.getColumns().setAll(laOrder);
                lbRestoring[0] = false;
            });
        });
    }

    public static <T> void selectAndFocusRow(TableView<T> tableView, int index) {
        tableView.getSelectionModel().select(index);
        tableView.getFocusModel().focus(index);
    }

    /** Index01-based row of the item whose indexNN (columnIndex) equals lnpn. */
    public static int getDetailRow(ObservableList<?> dataList, int lnpn, int columnIndex) {
        int result = lnpn - 1;
        try {
            for (Object item : dataList) {
                String value = (String) item.getClass().getMethod(String.format("getIndex%02d", columnIndex)).invoke(item);
                if (String.valueOf(lnpn).equals(value)) {
                    String index01 = (String) item.getClass().getMethod("getIndex01").invoke(item);
                    result = Integer.parseInt(index01) - 1;
                    break;
                }
            }
        } catch (ReflectiveOperationException | NumberFormatException e) {
            Logger.getLogger(JFXUtil.class.getName()).log(Level.WARNING, null, e);
        }
        return result;
    }

    /** indexNN (columnIndex) of the item whose index01 equals lnpn; 0 when not found. */
    public static int getDetailTempRow(ObservableList<?> dataList, int lnpn, int columnIndex) {
        int result = 0;
        try {
            for (Object item : dataList) {
                String index01 = (String) item.getClass().getMethod("getIndex01").invoke(item);
                if (String.valueOf(lnpn).equals(index01)) {
                    String value = (String) item.getClass().getMethod(String.format("getIndex%02d", columnIndex)).invoke(item);
                    result = Integer.parseInt(value);
                    break;
                }
            }
        } catch (ReflectiveOperationException | NumberFormatException e) {
            Logger.getLogger(JFXUtil.class.getName()).log(Level.WARNING, null, e);
        }
        return result;
    }

    // =====================================================================
    // Attachment preview
    // =====================================================================
    public static void adjustImageSize(Image image, ImageView imageView, double ldstackPaneWidth, double ldstackPaneHeight) {
        double imageRatio = image.getWidth() / image.getHeight();
        double containerRatio = ldstackPaneWidth / ldstackPaneHeight;
        imageView.fitWidthProperty().unbind();
        imageView.fitHeightProperty().unbind();
        if (imageRatio > containerRatio) {
            imageView.setFitWidth(ldstackPaneWidth);
            imageView.setFitHeight(ldstackPaneWidth / imageRatio);
        } else {
            imageView.setFitHeight(ldstackPaneHeight);
            imageView.setFitWidth(ldstackPaneHeight * imageRatio);
        }
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
    }

    public static void resetImageBounds(ImageView imageView, StackPane stackPane1) {
        imageView.setScaleX(1.0);
        imageView.setScaleY(1.0);
        imageView.setTranslateX(0);
        imageView.setTranslateY(0);
        StackPane.setAlignment(imageView, Pos.CENTER);
    }

    public static void stackPaneClip(StackPane stackPane1) {
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(
                stackPane1.getWidth() - 8, stackPane1.getHeight() - 8);
        clip.setArcWidth(8);
        clip.setArcHeight(8);
        clip.setLayoutX(4);
        clip.setLayoutY(4);
        stackPane1.setClip(clip);
    }

    /** Shows the pages of a PDF in the preview pane (Ctrl+wheel zooms, drag pans). */
    public static void PDFViewConfig(String filePath2, StackPane stackPane1, Button btnArrowLeft, Button btnArrowRight,
            Double ldstackPaneWidth, Double ldstackPaneHeight) {
        try (PDDocument document = PDDocument.load(new File(filePath2))) {
            PDFRenderer renderer = new PDFRenderer(document);
            VBox pdfContainer = new VBox(10);
            pdfContainer.setAlignment(Pos.CENTER);
            pdfContainer.setPrefWidth(ldstackPaneWidth);

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                Image fxImage = toFXImage(renderer.renderImageWithDPI(i, 150));
                ImageView pageView = new ImageView(fxImage);
                pageView.setPreserveRatio(true);
                pageView.setFitWidth(ldstackPaneWidth);
                adjustImageSize(fxImage, pageView, ldstackPaneWidth, ldstackPaneHeight);
                pdfContainer.getChildren().add(pageView);
            }

            Group pdfGroup = new Group(pdfContainer);
            StackPane centerPane = new StackPane(pdfGroup);
            centerPane.setAlignment(Pos.CENTER);
            ScrollPane scrollPane = new ScrollPane(centerPane);
            scrollPane.setPannable(true);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);

            stackPane1.getChildren().setAll(scrollPane, btnArrowLeft, btnArrowRight);
            StackPane.setAlignment(btnArrowLeft, Pos.CENTER_LEFT);
            StackPane.setAlignment(btnArrowRight, Pos.CENTER_RIGHT);
            StackPane.setMargin(btnArrowLeft, new Insets(0, 0, 0, 10));
            StackPane.setMargin(btnArrowRight, new Insets(0, 10, 0, 0));

            PauseTransition delay = new PauseTransition(Duration.seconds(2));
            delay.setOnFinished(event -> Platform.runLater(() -> stackPaneClip(stackPane1)));
            delay.play();

            final DoubleProperty zoomFactor = new SimpleDoubleProperty(1.0);
            scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
                if (event.isControlDown()) {
                    event.consume();
                    zoomFactor.set(zoomFactor.get() * (event.getDeltaY() > 0 ? 1.1 : 0.9));
                    pdfGroup.setScaleX(zoomFactor.get());
                    pdfGroup.setScaleY(zoomFactor.get());

                    Bounds viewportBounds = scrollPane.getViewportBounds();
                    Bounds contentBounds = pdfGroup.getBoundsInParent();
                    double mouseX = event.getX();
                    double mouseY = event.getY();
                    double hRatio = (scrollPane.getHvalue() * (contentBounds.getWidth() - viewportBounds.getWidth()) + mouseX) / contentBounds.getWidth();
                    double vRatio = (scrollPane.getVvalue() * (contentBounds.getHeight() - viewportBounds.getHeight()) + mouseY) / contentBounds.getHeight();
                    Platform.runLater(() -> {
                        Bounds newBounds = pdfGroup.getBoundsInParent();
                        double newH = (hRatio * newBounds.getWidth() - mouseX) / (newBounds.getWidth() - viewportBounds.getWidth());
                        double newV = (vRatio * newBounds.getHeight() - mouseY) / (newBounds.getHeight() - viewportBounds.getHeight());
                        scrollPane.setHvalue(Double.isNaN(newH) ? 0.5 : Math.min(Math.max(0, newH), 1.0));
                        scrollPane.setVvalue(Double.isNaN(newV) ? 0.5 : Math.min(Math.max(0, newV), 1.0));
                    });
                }
            });

            final ObjectProperty<Point2D> lastMouse = new SimpleObjectProperty<>();
            pdfGroup.setOnMousePressed(e -> lastMouse.set(new Point2D(e.getSceneX(), e.getSceneY())));
            pdfGroup.setOnMouseDragged(e -> {
                if (lastMouse.get() != null) {
                    pdfGroup.setTranslateX(pdfGroup.getTranslateX() + e.getSceneX() - lastMouse.get().getX());
                    pdfGroup.setTranslateY(pdfGroup.getTranslateY() + e.getSceneY() - lastMouse.get().getY());
                    lastMouse.set(new Point2D(e.getSceneX(), e.getSceneY()));
                }
            });
            pdfGroup.setOnMouseReleased(e -> lastMouse.set(null));
        } catch (IOException ex) {
            Logger.getLogger(JFXUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /** BufferedImage to a JavaFX image (what SwingFXUtils.toFXImage does). */
    private static Image toFXImage(BufferedImage foImage) {
        WritableImage loImage = new WritableImage(foImage.getWidth(), foImage.getHeight());
        PixelWriter loWriter = loImage.getPixelWriter();
        for (int y = 0; y < foImage.getHeight(); y++) {
            for (int x = 0; x < foImage.getWidth(); x++) {
                loWriter.setArgb(x, y, foImage.getRGB(x, y));
            }
        }
        return loImage;
    }
}
