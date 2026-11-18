package ph.com.guanzongroup.cas.purchasing.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The one place that maps an industry to its Purchase Order controller.
 *
 * <p>
 * Each controller creates its own validator ({@code createValidator()}), so
 * this list also decides which rules an industry uses. Adding an industry is
 * one line here plus its controller/validator classes.</p>
 *
 * <p>
 * Usage: the old org.guanzon.cas.purchasing.services.PurchaseOrderControllers
 * stays as it is (it builds the old controller). Give the new classes their
 * own factory in this package tree, with the same setup steps as the old one:</p>
 * <pre>
 * package ph.com.guanzongroup.cas.purchasing.services;
 *
 * public class PurchaseOrderControllers {
 *     public PurchaseOrder PurchaseOrder(String industryId) {
 *         PurchaseOrder loPO = PurchaseOrderRegistry.create(industryId);
 *         //...same setup the old PurchaseOrder() method does...
 *         return loPO;
 *     }
 * }
 * </pre>
 */
public final class PurchaseOrderRegistry {

    /**
     * Industry codes. TODO: set each to the code stored in sIndstCdx (the same
     * codes PurchaseOrderValidatorFactory uses today). An empty code is not
     * registered, so that industry uses the default (General) until it is set.
     */
    public static final class IndustryCode {

        public static final String MOTORCYCLE = "";
        public static final String MOBILE_PHONE = "";
        public static final String SPARE_PARTS = "";
        public static final String APPLIANCE = "";

        private IndustryCode() {
        }
    }

    /** Controller for any industry not registered below. */
    private static final Supplier<PurchaseOrder> DEFAULT = PurchaseOrderGeneral::new;

    private static final Map<String, Supplier<PurchaseOrder>> CONTROLLERS;

    static {
        Map<String, Supplier<PurchaseOrder>> loMap = new HashMap<>();
        //Keep an industry commented out (it then uses General) until its
        //controller's TODOs are filled in.
        register(loMap, IndustryCode.MOTORCYCLE, PurchaseOrderMC::new);
        register(loMap, IndustryCode.MOBILE_PHONE, PurchaseOrderMP::new);
        register(loMap, IndustryCode.SPARE_PARTS, PurchaseOrderSP::new);
        register(loMap, IndustryCode.APPLIANCE, PurchaseOrderAppliance::new);
        CONTROLLERS = Collections.unmodifiableMap(loMap);
    }

    private PurchaseOrderRegistry() {
    }

    private static void register(Map<String, Supplier<PurchaseOrder>> foMap,
            String fsIndustryId, Supplier<PurchaseOrder> foController) {
        if (fsIndustryId == null || fsIndustryId.trim().isEmpty()) {
            return;
        }
        if (foMap.put(fsIndustryId, foController) != null) {
            throw new IllegalStateException("Industry " + fsIndustryId + " is registered twice.");
        }
    }

    /** New, uninitialized controller for the industry (General when not registered). */
    public static PurchaseOrder create(String fsIndustryId) {
        Supplier<PurchaseOrder> loController = fsIndustryId == null ? null : CONTROLLERS.get(fsIndustryId);
        return (loController != null ? loController : DEFAULT).get();
    }

    /** Whether the industry has its own controller. */
    public static boolean isRegistered(String fsIndustryId) {
        return fsIndustryId != null && CONTROLLERS.containsKey(fsIndustryId);
    }
}
