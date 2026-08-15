package org.guanzon.cas.purchasing.validator;

import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.purchasing.status.PurchaseOrderStatus;

public class PurchaseOrderValidatorFactory {

    public static GValidator make(String industryId) {
        switch (industryId) {
            case PurchaseOrderStatus.IndustryCode.MOTORCYCLE:
                return new PurchaseOrder_MC();

            case PurchaseOrderStatus.IndustryCode.MOBILEPHONE:
                return new PurchaseOrder_MP();

            case PurchaseOrderStatus.IndustryCode.AUTO_GROUP_HONDA:
            case PurchaseOrderStatus.IndustryCode.AUTO_GROUP_ANY:
            case PurchaseOrderStatus.IndustryCode.AUTO_GROUP_NISSAN:
                return new PurchaseOrder_Vehicle();

            case PurchaseOrderStatus.IndustryCode.MONARCH:
                return new PurchaseOrder_Hospitality();

            case PurchaseOrderStatus.IndustryCode.PEDRITOS:
                return new PurchaseOrder_LP();

            case PurchaseOrderStatus.IndustryCode.GENERAL:
                return new PurchaseOrder_LP();

            default:
                return new PurchaseOrder_General();
        }
    }
}
