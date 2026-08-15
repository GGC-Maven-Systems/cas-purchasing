/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.status;

/**
 *
 * @author Arsiela 03-12-2025
 */
public class PurchaseOrderReceivingStatus {
    public static final String OPEN = "0";
    public static final  String CONFIRMED = "1";
    public static final  String POSTED = "2"; 
    public static final  String CANCELLED = "3";
    public static final  String VOID = "4";
    public static final  String PAID = "5"; 
    public static final  String RETURNED = "6";
    
    //New status for SI Posting - Arsiela 06-22-2026
    public static final  String CONFIRMED_I = "7";
    public static final  String VERIFIED = "8";
    public static final  String RETURNED_I = "9";
    
    //Category
    public static class Category  {
        public static final String MOBILEPHONE = "0000001";   //Cellphone    
        public static final String APPLIANCES  = "0000002";   //Appliances   
        public static final String MOTORCYCLE  = "0000003";   //Motorcycle   
        public static final String SPMC        = "0000004";   //Motorcycle SP
        public static final String CAR         = "0000005";   //CAR          
        public static final String SPCAR       = "0000006";   //CAR SP       
        public static final String GENERAL     = "0000007";   //General      
        public static final String FOOD        = "0000008";   //Food         
        public static final String HOSPITALITY = "0000009";   //Hospitality  
    }
    
    //Industry  Arsiela 08-07-2026 Data for industry must be configure and finalize to prevent frequently changes.
    /* SIR MAC 08-15-2026 no final need to update 
    01 MC
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
    public static class Industry  {
        public static final String MOTORCYCLE  = "01";   //Motorcycle   / SP MC
        public static final String MOBILEPHONE = "02";   //Cellphone    
        public static final String CAR         = "03";   //CAR  / SP CAR / Honda
        public static final String HOSPITALITY = "04";   //Hospitality       
        public static final String CAR_Nissan  = "05";   //Auto Group - Nissan 
        public static final String CAR_Any     = "06";   //Auto Group - Any 
        public static final String GUANZON_Service     = "07";   //Guanzon Service office 
        public static final String MAINOFFICE  = "08";   //MainOffice
        public static final String GENERAL     = "09";   //General     
        public static final String ENGINEERING = "10";   //General         
        public static final String APPLIANCES  = "11";   //Appliances      
        public static final String LPFOOD      = "12";   //Food         
    }
    
    public static class Reverse  {
        public static final  String INCLUDE = "+"; 
        public static final  String EXCLUDE = "-"; 
    }
    
    public static class Purpose  {
        public static final  String REGULAR = "0"; 
        public static final  String CONSIGNMENT = "1"; 
        public static final  String REPLACEMENT = "2"; 
        public static final  String WARRANTY = "3"; 
        public static final  String SUPPLIER_GIVEAWAY = "4"; 
    }
    //0->Regular;1->Consignment;2->Replacement;3->Warranty;4->Supplier Giveaway;
}
