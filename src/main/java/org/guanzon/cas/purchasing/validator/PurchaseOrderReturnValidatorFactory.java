/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.validator;

import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.purchasing.status.PurchaseOrderReturnStatus;

/**
 *
 * @author Arsiela 04-28-2025
 */
public class PurchaseOrderReturnValidatorFactory {
    public static GValidator make(String industryId){
        switch (industryId) {
            case PurchaseOrderReturnStatus.Industry.MOBILEPHONE: //Mobile Phone
                return new PurchaseOrderReturn_MP();
            case PurchaseOrderReturnStatus.Industry.MOTORCYCLE: //Motorcycle
                return new PurchaseOrderReturn_MC();
            case PurchaseOrderReturnStatus.Industry.CAR: //Vehicle
            case PurchaseOrderReturnStatus.Industry.CAR_Nissan: //Vehicle
            case PurchaseOrderReturnStatus.Industry.CAR_Any: //Vehicle
                return new PurchaseOrderReturn_Vehicle();
            case PurchaseOrderReturnStatus.Industry.HOSPITALITY: //Hospitality
                return new PurchaseOrderReturn_Hospitality();
            case PurchaseOrderReturnStatus.Industry.LPFOOD: //Los Pedritos
                return new PurchaseOrderReturn_LP();
            case PurchaseOrderReturnStatus.Industry.APPLIANCES: //Appliances
                return new PurchaseOrderReturn_Appliances();
            default:
                return new PurchaseOrderReturn_General();
        }
    }
    
}
