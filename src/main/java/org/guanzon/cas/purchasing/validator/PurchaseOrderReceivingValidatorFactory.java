/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.validator;

import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.purchasing.status.PurchaseOrderReceivingStatus;

/**
 *
 * @author Arsiela 03-12-2025
 * 
 *  01 MC
    02 MP
    03 CAR
    04 Monarch
    05 Auto Group - Nissan (need to ask ito later. sa existing system, ginagamit siya for payrol)
    06 Auto Group - Any (need to ask ito later. sa existing system, ginagamit siya for payrol)
    07 Guanzon Service office - (need to ask ito later. sa existing system, ginagamit siya for payrol)
    08 Main Office (gamit ng finance sa CAS)
    09 General (PSD & engineering)
    10 Engineering (ask ito kung para saan)
    11 Appliances
    12 Pedritos
 */
public class PurchaseOrderReceivingValidatorFactory {
    public static GValidator make(String industryId){
        switch (industryId) {
            case PurchaseOrderReceivingStatus.Industry.MOBILEPHONE: //Mobile Phone
                return new PurchaseOrderReceiving_MP();
            case PurchaseOrderReceivingStatus.Industry.MOTORCYCLE: //Motorcycle
                return new PurchaseOrderReceiving_MC();
            case PurchaseOrderReceivingStatus.Industry.CAR: //Vehicle
            case PurchaseOrderReceivingStatus.Industry.CAR_Nissan: //Vehicle
            case PurchaseOrderReceivingStatus.Industry.CAR_Any: //Vehicle
                return new PurchaseOrderReceiving_Vehicle();
            case PurchaseOrderReceivingStatus.Industry.HOSPITALITY: //Hospitality
                return new PurchaseOrderReceiving_Hospitality();
            case PurchaseOrderReceivingStatus.Industry.LPFOOD: //Los Pedritos
                return new PurchaseOrderReceiving_LP();
            case PurchaseOrderReceivingStatus.Industry.APPLIANCES: //Appliances
                return new PurchaseOrderReceiving_Appliances();
            default:
                return new PurchaseOrderReceiving_General();
        }
    }
    
}
