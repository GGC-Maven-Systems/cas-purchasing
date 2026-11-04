package org.guanzon.cas.purchasing.services;

import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.LogWrapper;
import org.guanzon.cas.purchasing.controller.PurchaseOrder;
import org.guanzon.cas.purchasing.controller.PurchaseOrderGeneral;
import org.guanzon.cas.purchasing.controller.PurchaseOrderMain;
public class PurchaseOrderControllers {
    public PurchaseOrderControllers(GRiderCAS applicationDriver, LogWrapper logWrapper){
        poGRider = applicationDriver;
        poLogWrapper = logWrapper;
    }

    public PurchaseOrderMain PurchaseOrder(String industryId) {
       PurchaseOrderMain loPO;
       switch (industryId) {          //same codes as PurchaseOrderValidatorFactory
           case MC_CODE:        loPO = new PurchaseOrderMC(); break;
           case MP_CODE:        loPO = new PurchaseOrderMP(); break;
           case SP_CODE:        loPO = new PurchaseOrderSP(); break;
           case APPLIANCE_CODE: loPO = new PurchaseOrderAppliance(); break;
           default:             loPO = new PurchaseOrderGeneral();
       }
       //...same setup the existing PurchaseOrder() method does...
       return loPO;
   }
    
    public PurchaseOrder PurchaseOrder(){
        if (poGRider == null){
            poLogWrapper.severe("PurchaseOrderControllers.PurchaseOrder: Application driver is not set.");
            return null;
        }
        
        if (poPurchaseOrder != null) return poPurchaseOrder;
        
        poPurchaseOrder = new PurchaseOrder();
        poPurchaseOrder.setApplicationDriver(poGRider);
        poPurchaseOrder.setBranchCode(poGRider.getBranchCode());
        poPurchaseOrder.setVerifyEntryNo(true);
        poPurchaseOrder.setWithParent(false);
        poPurchaseOrder.setLogWrapper(poLogWrapper);
        return poPurchaseOrder;        
    }
    
    @Override
    protected void finalize() throws Throwable {
        try {
            poPurchaseOrder = null;
                    
            poLogWrapper = null;
            poGRider = null;
        } finally {
            super.finalize();
        }
    }
    
    private GRiderCAS poGRider;
    private LogWrapper poLogWrapper;
    
    private PurchaseOrder poPurchaseOrder;
}
