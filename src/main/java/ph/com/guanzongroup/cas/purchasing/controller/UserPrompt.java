package ph.com.guanzongroup.cas.purchasing.controller;

import java.sql.SQLException;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.json.simple.JSONObject;

/**
 * Everything the Purchase Order controllers need from a user interface.
 *
 * <p>
 * The controllers never call JavaFX/Swing dialogs directly; they call this.
 * The desktop screens use {@link FxUserPrompt} (the default). A background
 * job, a web/mobile front end or a test supplies its own implementation, e.g.
 * {@link HeadlessUserPrompt}:</p>
 * <pre>
 * PurchaseOrder loPO = PurchaseOrderRegistry.create(industryId);
 * loPO.setUserPrompt(new HeadlessUserPrompt());
 * </pre>
 */
public interface UserPrompt {

    /**
     * Asks an approving officer to sign in.
     *
     * @return "result" ("success"/"error") and, on success, "sUserIDxx" and
     * "nUserLevl" of the officer
     */
    JSONObject getUserApproval(GRiderCAS foGRider) throws SQLException, GuanzonException;

    void warning(String fsMessage, String fsTitle);

    void information(String fsMessage, String fsTitle);

    /**
     * Lets the user pick one record from a query.
     *
     * @return the picked row, or null when nothing was picked
     */
    JSONObject browse(GRiderCAS foGRider, String fsSQL, String fsValue, String fsHeaders,
            String fsColumns, String fsCriteria, int fnSort) throws SQLException, GuanzonException;
}
