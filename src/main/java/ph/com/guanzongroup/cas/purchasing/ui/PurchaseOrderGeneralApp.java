package ph.com.guanzongroup.cas.purchasing.ui;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Optional;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.RecordStatus;

/**
 * Runs the General Inventory Purchase Order screens on their own, for testing
 * the new controller without the integsys dashboard.
 *
 * <p>
 * Screen: -Dpo.screen=entry (default), confirmation or history. Without it,
 * a small window asks which one to open.</p>
 *
 * <p>
 * Connects the same way as testPurchaseOrder ({@code MiscUtil.Connect()} with
 * the metadata in {@code <config>/config/metadata/new/}) and loads the same
 * cas.properties settings as integsys' Login.</p>
 *
 * <p>
 * Industry and category are the ones the integsys menu passes to the screen
 * (xxxSysMenuOthers row of PurchaseOrder_Entry/Confirmation/HistoryController), so the searches
 * use the same filters as in integsys. When there is no such row, they come
 * from user.selected.industry / category in cas.properties. Override with
 * -Dpo.industry=... / -Dpo.category=... . Company comes from
 * user.selected.company. The values used are printed on the console.</p>
 *
 * <p>
 * In NetBeans: right-click this file, Run File.</p>
 */
public class PurchaseOrderGeneralApp extends Application {

    private static final String VIEWS = "/ph/com/guanzongroup/cas/purchasing/ui/";
    private static final String TITLE = "Purchase Order - General (new controller)";

    private static GRiderCAS poApp;

    public static void main(String[] args) {
        boolean lbWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String lsConfig = lbWindows ? "D:/GGC_Maven_Systems" : "/srv/GGC_Maven_Systems";
        String lsTemp = lbWindows ? "D:/temp" : "/srv/temp";

        System.setProperty("sys.default.path.config", lsConfig);
        System.setProperty("sys.default.path.metadata", lsConfig + "/config/metadata/new/");
        System.setProperty("sys.default.path.temp", lsTemp);

        if (!loadProperties(lsConfig)) {
            System.err.println("Unable to load " + lsConfig + "/config/cas.properties.");
            System.exit(1);
        }

        poApp = MiscUtil.Connect();
        if (poApp == null) {
            System.err.println("Unable to connect to the database.");
            System.exit(1);
        }

        launch(args);
    }

    @Override
    public void start(Stage foStage) throws Exception {
        String lsScreen = System.getProperty("po.screen");
        if (lsScreen == null || lsScreen.trim().isEmpty()) {
            ChoiceDialog<String> loDialog = new ChoiceDialog<>("entry", "entry", "confirmation", "history");
            loDialog.setTitle(TITLE);
            loDialog.setHeaderText("Which Purchase Order screen?");
            loDialog.setContentText("Screen:");
            Optional<String> loChoice = loDialog.showAndWait();
            if (!loChoice.isPresent()) {
                Platform.exit();
                return;
            }
            lsScreen = loChoice.get();
        }

        ScreenInterface loController;
        String lsFxml;
        String lsTitle;
        String lsMenuController; //the integsys screen whose menu item gives industry/category
        switch (lsScreen.trim().toLowerCase()) {
            case "confirmation":
                loController = new PurchaseOrderGeneral_ConfirmationController();
                lsFxml = "PurchaseOrderGeneral_Confirmation.fxml";
                lsTitle = "Purchase Order Confirmation - General (new controller)";
                lsMenuController = "PurchaseOrder_ConfirmationController";
                break;
            case "history":
                loController = new PurchaseOrderGeneral_HistoryController();
                lsFxml = "PurchaseOrderGeneral_History.fxml";
                lsTitle = "Purchase Order History - General (new controller)";
                lsMenuController = "PurchaseOrder_HistoryController";
                break;
            default:
                loController = new PurchaseOrderGeneral_EntryController();
                lsFxml = "PurchaseOrderGeneral_Entry.fxml";
                lsTitle = TITLE;
                lsMenuController = "PurchaseOrder_EntryController";
        }
        loController.setGRider(poApp);
        String[] laMenu = menuIndustryAndCategory(lsMenuController);
        String lsIndustry = System.getProperty("po.industry", laMenu[0]);
        String lsCategory = System.getProperty("po.category", laMenu[1]);
        String lsCompany = System.getProperty("user.selected.company", "");
        System.out.println("Purchase Order screen: industry=" + lsIndustry
                + ", category=" + lsCategory + ", company=" + lsCompany);

        loController.setIndustryID(lsIndustry);
        loController.setCompanyID(lsCompany);
        loController.setCategoryID(lsCategory);

        FXMLLoader loLoader = new FXMLLoader(getClass().getResource(VIEWS + lsFxml));
        loLoader.setController(loController);
        Parent loRoot = loLoader.load();

        Rectangle2D loBounds = Screen.getPrimary().getVisualBounds();
        foStage.setScene(new Scene(loRoot));
        foStage.setTitle(lsTitle);
        foStage.setX(loBounds.getMinX());
        foStage.setY(loBounds.getMinY());
        foStage.setWidth(loBounds.getWidth());
        foStage.setHeight(loBounds.getHeight());
        foStage.show();
    }

    /**
     * Industry and category of the integsys menu item that opens the same
     * Purchase Order screen (what its dashboard passes); cas.properties values
     * when the menu has no such item.
     */
    private static String[] menuIndustryAndCategory(String fsMenuController) {
        String[] laResult = {
            System.getProperty("user.selected.industry", ""),
            System.getProperty("user.selected.category", "")};
        String lsSQL = "SELECT b.sIndstCdx, b.sCategrCd, a.sMenuDesc"
                + " FROM xxxSysMenu a"
                + " JOIN xxxSysMenuOthers b ON b.sMenuCDxx = a.sMenuCDxx"
                + " WHERE b.sObjectNm LIKE " + SQLUtil.toSQL("%." + fsMenuController)
                + " AND a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                + " AND b.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                + " ORDER BY a.sMenuCDxx";
        try {
            ResultSet loRS = poApp.executeQuery(lsSQL);
            if (loRS == null) {
                return laResult;
            }
            try {
                boolean lbFirst = true;
                while (loRS.next()) {
                    System.out.println("Menu item \"" + loRS.getString("sMenuDesc") + "\": industry="
                            + loRS.getString("sIndstCdx") + ", category=" + loRS.getString("sCategrCd"));
                    if (lbFirst) {
                        laResult[0] = nvl(loRS.getString("sIndstCdx"));
                        laResult[1] = nvl(loRS.getString("sCategrCd"));
                        lbFirst = false;
                    }
                }
                if (lbFirst) {
                    System.out.println("No menu item for " + fsMenuController + "; using cas.properties.");
                }
            } finally {
                MiscUtil.close(loRS);
            }
        } catch (SQLException ex) {
            Logger.getLogger(PurchaseOrderGeneralApp.class.getName()).log(Level.WARNING,
                    "Unable to read the menu; using cas.properties.", ex);
        }
        return laResult;
    }

    private static String nvl(String fsValue) {
        return fsValue == null ? "" : fsValue;
    }

    /** Same settings integsys' Login reads from cas.properties. */
    private static boolean loadProperties(String fsConfig) {
        Properties loProps = new Properties();
        try (InputStream loFile = new FileInputStream(fsConfig + "/config/cas.properties")) {
            loProps.load(loFile);
        } catch (IOException ex) {
            Logger.getLogger(PurchaseOrderGeneralApp.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        String[] laKeys = {
            "sys.main.industry", "sys.general.industry",
            "sys.dept.finance", "sys.dept.procurement",
            "user.selected.industry", "user.selected.category", "user.selected.company",
            "sys.default.path.temp.attachments",
            "allowed.department"};
        for (String lsKey : laKeys) {
            String lsValue = loProps.getProperty(lsKey);
            if (lsValue != null) {
                System.setProperty(lsKey, lsValue);
            } else {
                System.err.println("cas.properties has no " + lsKey + ".");
            }
        }
        System.setProperty("sys.default.client.token", fsConfig + "/client.token");
        System.setProperty("sys.default.access.token", fsConfig + "/access.token");
        return true;
    }
}
