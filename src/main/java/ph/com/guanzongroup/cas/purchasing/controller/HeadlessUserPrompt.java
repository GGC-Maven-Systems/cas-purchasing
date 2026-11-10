package ph.com.guanzongroup.cas.purchasing.controller;

import java.util.logging.Logger;
import org.guanzon.appdriver.base.GRiderCAS;
import org.json.simple.JSONObject;

/**
 * {@link UserPrompt} for code that runs with nobody at the screen (scheduled
 * jobs, services, tests): approvals are refused, messages are logged, and
 * browse picks nothing.
 */
public class HeadlessUserPrompt implements UserPrompt {

    private static final Logger LOG = Logger.getLogger(HeadlessUserPrompt.class.getName());

    @Override
    public JSONObject getUserApproval(GRiderCAS foGRider) {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "error");
        loJSON.put("message", "An approving officer is required, but no user interface is available.");
        return loJSON;
    }

    @Override
    public void warning(String fsMessage, String fsTitle) {
        LOG.warning(fsTitle + ": " + fsMessage);
    }

    @Override
    public void information(String fsMessage, String fsTitle) {
        LOG.info(fsTitle + ": " + fsMessage);
    }

    @Override
    public JSONObject browse(GRiderCAS foGRider, String fsSQL, String fsValue, String fsHeaders,
            String fsColumns, String fsCriteria, int fnSort) {
        return null;
    }
}
