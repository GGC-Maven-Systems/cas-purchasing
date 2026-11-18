package ph.com.guanzongroup.cas.purchasing.services;

import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.LogWrapper;
import ph.com.guanzongroup.cas.purchasing.controller.PurchaseOrder;
import ph.com.guanzongroup.cas.purchasing.controller.PurchaseOrderGeneral;
import ph.com.guanzongroup.cas.purchasing.controller.PurchaseOrderRegistry;

/**
 * Factory for the new Purchase Order controllers
 * (ph.com.guanzongroup.cas.purchasing.controller).
 *
 * <p>
 * Same setup as the old org.guanzon.cas.purchasing.services.PurchaseOrderControllers,
 * which stays as it is for the screens that still use the old controller.</p>
 *
 * <pre>
 * PurchaseOrderControllers loControllers = new PurchaseOrderControllers(poApp, poLogWrapper);
 * PurchaseOrderGeneral loPO = loControllers.PurchaseOrderGeneral();
 * loPO.InitTransaction();
 * loPO.NewTransaction();
 * </pre>
 */
public class PurchaseOrderControllers {

    private GRiderCAS poGRider;
    private LogWrapper poLogWrapper;

    private PurchaseOrderGeneral poGeneral;

    public PurchaseOrderControllers(GRiderCAS applicationDriver, LogWrapper logWrapper) {
        poGRider = applicationDriver;
        poLogWrapper = logWrapper;
    }

    /** General Inventory controller (one instance per factory). */
    public PurchaseOrderGeneral PurchaseOrderGeneral() {
        if (!hasDriver("PurchaseOrderGeneral")) {
            return null;
        }
        if (poGeneral == null) {
            poGeneral = setUp(new PurchaseOrderGeneral());
        }
        return poGeneral;
    }

    /**
     * New controller for an industry (see PurchaseOrderRegistry; General when
     * the industry has no controller of its own). A new instance per call.
     */
    public PurchaseOrder PurchaseOrder(String industryId) {
        if (!hasDriver("PurchaseOrder")) {
            return null;
        }
        return setUp(PurchaseOrderRegistry.create(industryId));
    }

    /** Same steps as the old factory. */
    private <T extends PurchaseOrder> T setUp(T foPO) {
        foPO.setApplicationDriver(poGRider);
        foPO.setBranchCode(poGRider.getBranchCode());
        foPO.setVerifyEntryNo(true);
        foPO.setWithParent(false);
        foPO.setLogWrapper(poLogWrapper);
        return foPO;
    }

    private boolean hasDriver(String fsMethod) {
        if (poGRider != null) {
            return true;
        }
        if (poLogWrapper != null) {
            poLogWrapper.severe("PurchaseOrderControllers." + fsMethod + ": Application driver is not set.");
        }
        return false;
    }
}
