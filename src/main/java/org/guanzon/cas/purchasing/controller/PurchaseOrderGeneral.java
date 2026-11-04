package org.guanzon.cas.purchasing.controller;

import java.sql.SQLException;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.parameter.model.Model_Project;
import org.guanzon.cas.parameter.services.ParamModels;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;
import org.guanzon.cas.purchasing.validator.PurchaseOrderValidator;
import org.guanzon.cas.purchasing.validator.PurchaseOrder_General;
import org.json.simple.JSONObject;

/**
 * Purchase Order controller for General Inventory.
 *
 * <p>
 * Inherits every shared procedure from {@link PurchaseOrderMain}. Adds the
 * General-only procedure: tagging/untagging the engineering project title
 * referenced by the PO. The rule that decides when it applies is in
 * {@link PurchaseOrder_General}.</p>
 *
 * <p>
 * Project title flow (same as before the split):</p>
 * <ul>
 * <li>Save: the project is prepared in willSave and saved with a new PO.</li>
 * <li>Cancel / Void: the project is untagged when no other PO uses it,
 * prepared before the transaction and saved inside it.</li>
 * </ul>
 *
 * @author TEEJEI DE CELIS (project title), 04/09/2026
 */
public class PurchaseOrderGeneral extends PurchaseOrderMain {

    private Model_Project poProject;

    /** Set when a Cancel/Void prepared the project; saved inside the transaction. */
    private boolean pbProjectPrepared = false;

    @Override
    protected PurchaseOrderValidator createValidator() {
        return new PurchaseOrder_General();
    }

    /** General Inventory validator (gives access to the project rule). */
    protected PurchaseOrder_General generalRules(String fsStatus) {
        return (PurchaseOrder_General) rules(fsStatus);
    }

    // =====================================================================
    // Hooks
    // =====================================================================
    @Override
    protected JSONObject industryWillSave() throws SQLException, GuanzonException, CloneNotSupportedException {
        String lsStatus = Master().getTransactionStatus();
        if (generalRules(lsStatus).isProjectTitleUpdateRequired(lsStatus, false)) {
            JSONObject loJSON = setProjectTitle(lsStatus);
            if (!PurchaseOrderValidator.isSuccess(loJSON)) {
                return loJSON;
            }
        }
        return super.industryWillSave();
    }

    @Override
    protected JSONObject industrySaveOthers() throws SQLException, GuanzonException, CloneNotSupportedException {
        String lsStatus = Master().getTransactionStatus();
        if (Master().getEditMode() == EditMode.ADDNEW
                && generalRules(lsStatus).isProjectTitleUpdateRequired(lsStatus, false)) {
            JSONObject loJSON = saveProjectTitle();
            if (!PurchaseOrderValidator.isSuccess(loJSON)) {
                return loJSON;
            }
        }
        return super.industrySaveOthers();
    }

    @Override
    protected JSONObject industryPrepareStatusChange(String fsStatus) throws SQLException, GuanzonException, CloneNotSupportedException {
        pbProjectPrepared = false;

        if (PurchaseOrderStatus.CANCELLED.equals(fsStatus) || PurchaseOrderStatus.VOID.equals(fsStatus)) {
            //untag only when no other PO uses the project
            if (generalRules(fsStatus).isProjectTitleUpdateRequired(fsStatus, true)) {
                JSONObject loJSON = setProjectTitle(fsStatus);
                if (!PurchaseOrderValidator.isSuccess(loJSON)) {
                    return loJSON;
                }
                pbProjectPrepared = true;
            }
        }
        return super.industryPrepareStatusChange(fsStatus);
    }

    @Override
    protected JSONObject industryStatusChange(String fsStatus) throws Exception {
        if (pbProjectPrepared) {
            pbProjectPrepared = false;
            JSONObject loJSON = saveProjectTitle();
            if (!PurchaseOrderValidator.isSuccess(loJSON)) {
                return loJSON;
            }
        }
        return super.industryStatusChange(fsStatus);
    }

    // =====================================================================
    // Project title procedure
    // =====================================================================
    private Model_Project Project() {
        if (poProject == null) {
            poProject = new ParamModels(poGRider).Project();
        }
        return poProject;
    }

    /** Opens the referenced project and tags (OPEN) or untags (CANCELLED/VOID) it. */
    private JSONObject setProjectTitle(String fsStatus) throws SQLException, GuanzonException, CloneNotSupportedException {
        Model_Project loProject = Project();
        loProject.initialize();

        JSONObject loJSON = loProject.openRecord(Master().getReference());
        if ("error".equalsIgnoreCase((String) loJSON.get("result"))) {
            return loJSON;
        }
        loJSON = loProject.updateRecord();
        if ("error".equalsIgnoreCase((String) loJSON.get("result"))) {
            return loJSON;
        }

        switch (fsStatus) {
            case PurchaseOrderStatus.OPEN:
                loJSON = loProject.isUsed(true);
                break;
            case PurchaseOrderStatus.CANCELLED:
            case PurchaseOrderStatus.VOID:
                loJSON = loProject.isUsed(false);
                break;
        }
        loProject.setModifiedDate(poGRider.getServerDate());
        loProject.setModifyingId(poGRider.getUserID());

        if ("error".equalsIgnoreCase((String) loJSON.get("result"))) {
            return loJSON;
        }
        loJSON.put("result", "success");
        return loJSON;
    }

    private JSONObject saveProjectTitle() throws SQLException, GuanzonException, CloneNotSupportedException {
        JSONObject loJSON = Project().saveRecord();
        if ("error".equalsIgnoreCase((String) loJSON.get("result"))) {
            return loJSON;
        }
        loJSON.put("result", "success");
        return loJSON;
    }

    /**
     * Checks whether a project is already used in more than one PO.
     *
     * Returns {"result": "success"|"error", "message": ..., "count": n};
     * "error" when the project is used by more than one PO.
     *
     * @author TEEJEI DE CELIS, 2026-04-09
     */
    public JSONObject CheckProjectUsage(String fsProjectCode) throws SQLException, GuanzonException {
        int lnCount = generalRules(Master().getTransactionStatus()).getProjectUsageCount(fsProjectCode);

        poJSON = new JSONObject();
        if (lnCount > 1) {
            poJSON.put("result", "error");
            poJSON.put("message", "Project is already used in multiple transactions.");
        } else {
            poJSON.put("result", "success");
            poJSON.put("message", "No usage found. You can proceed.");
        }
        poJSON.put("count", lnCount);
        return poJSON;
    }
}
