package ph.com.guanzongroup.cas.purchasing.ui.utility;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/**
 * Formatting and field helpers used by the Purchase Order screens.
 *
 * <p>
 * Copied (only the methods the screens use) from
 * ph.com.guanzongroup.integsys.utility.CustomCommonUtil, without its JDK 8
 * internal imports, so it works on any Java/JavaFX version.</p>
 */
public final class CustomCommonUtil {

    private CustomCommonUtil() {
    }

    // =====================================================================
    // Dates
    // =====================================================================
    public static String formatLocalDateToShortString(LocalDate foLocalDate) {
        return foLocalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /** yyyy-MM-dd text to LocalDate; null when empty or invalid. */
    public static LocalDate parseDateStringToLocalDate(String fsDateValue) {
        if (fsDateValue == null || fsDateValue.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(fsDateValue.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // =====================================================================
    // Numbers
    // =====================================================================
    /** Whole number with thousands separators (rounded). */
    public static String setDecimalValueToIntegerFormat(double fnValue) {
        return new DecimalFormat("#,##0").format(Math.round(fnValue));
    }

    /** 2 decimals with thousands separators; 0.00 when null or not a number. */
    public static String setIntegerValueToDecimalFormat(Object foObject) {
        return setIntegerValueToDecimalFormat(foObject, false);
    }

    public static String setIntegerValueToDecimalFormat(double fnValue) {
        return setIntegerValueToDecimalFormat(fnValue, false);
    }

    /** 2 or 4 decimals with thousands separators; zero when null or not a number. */
    public static String setIntegerValueToDecimalFormat(Object foObject, boolean fbIs4Decimal) {
        DecimalFormat loFormat = new DecimalFormat(fbIs4Decimal ? "#,##0.0000" : "#,##0.00");
        try {
            if (foObject != null) {
                return loFormat.format(Double.parseDouble(String.valueOf(foObject)));
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid number format for input - " + foObject);
        }
        return fbIs4Decimal ? "0.0000" : "0.00";
    }

    public static String setIntegerValueToDecimalFormat(double fnValue, boolean fbIs4Decimal) {
        return new DecimalFormat(fbIs4Decimal ? "#,##0.0000" : "#,##0.00").format(fnValue);
    }

    // =====================================================================
    // Input filters
    // =====================================================================
    /** Digits and commas only. */
    public static void inputIntegersOnly(TextField... foTxtFields) {
        Pattern loPattern = Pattern.compile("[0-9,]*");
        for (TextField loField : foTxtFields) {
            if (loField != null) {
                UnaryOperator<TextFormatter.Change> loFilter
                        = change -> loPattern.matcher(change.getControlNewText()).matches() ? change : null;
                loField.setTextFormatter(new TextFormatter<>(loFilter));
            }
        }
    }

    // =====================================================================
    // Several controls at once
    // =====================================================================
    public static void setDisable(boolean fbDisable, Node... foNodes) {
        for (Node loNode : foNodes) {
            if (loNode != null) {
                loNode.setDisable(fbDisable);
            }
        }
    }

    public static void setManaged(boolean fbManaged, Node... foNodes) {
        for (Node loNode : foNodes) {
            if (loNode != null) {
                loNode.setManaged(fbManaged);
            }
        }
    }

    public static void setVisible(boolean fbVisible, Node... foNodes) {
        for (Node loNode : foNodes) {
            if (loNode != null) {
                loNode.setVisible(fbVisible);
            }
        }
    }

    public static void setSelected(boolean fbValue, CheckBox... foCheckBoxes) {
        for (CheckBox loCheckBox : foCheckBoxes) {
            if (loCheckBox != null) {
                loCheckBox.setSelected(fbValue);
            }
        }
    }

    public static void setText(String fsValue, TextField... foFields) {
        for (TextField loField : foFields) {
            if (loField != null) {
                loField.setText(fsValue);
            }
        }
    }

    public static void setText(String fsValue, TextArea... foAreas) {
        for (TextArea loArea : foAreas) {
            if (loArea != null) {
                loArea.setText(fsValue);
            }
        }
    }

    public static void setText(String fsValue, Label... foLabels) {
        for (Label loLabel : foLabels) {
            if (loLabel != null) {
                loLabel.setText(fsValue);
            }
        }
    }
}
