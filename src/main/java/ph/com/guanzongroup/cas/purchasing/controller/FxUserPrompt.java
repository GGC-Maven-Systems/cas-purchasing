package ph.com.guanzongroup.cas.purchasing.controller;

import java.sql.SQLException;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.json.simple.JSONObject;

/** The desktop (JavaFX) dialogs: the default {@link UserPrompt}. */
public class FxUserPrompt implements UserPrompt {

    @Override
    public JSONObject getUserApproval(GRiderCAS foGRider) throws SQLException, GuanzonException {
        return ShowDialogFX.getUserApproval(foGRider);
    }

    @Override
    public void warning(String fsMessage, String fsTitle) {
        ShowMessageFX.Warning(fsMessage, fsTitle, null);
    }

    @Override
    public void information(String fsMessage, String fsTitle) {
        ShowMessageFX.Information(fsMessage, fsTitle, null);
    }

    @Override
    public JSONObject browse(GRiderCAS foGRider, String fsSQL, String fsValue, String fsHeaders,
            String fsColumns, String fsCriteria, int fnSort) throws SQLException, GuanzonException {
        return ShowDialogFX.Browse(foGRider, fsSQL, fsValue, fsHeaders, fsColumns, fsCriteria, fnSort);
    }
}
