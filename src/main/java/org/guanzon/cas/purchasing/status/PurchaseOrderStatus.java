package org.guanzon.cas.purchasing.status;

public class PurchaseOrderStatus {

    public static final String OPEN = "0";
    public static final String CONFIRMED = "1";
    public static final String PROCESSED = "2";
    public static final String CANCELLED = "3";
    public static final String VOID = "4";
    public static final String APPROVED = "5";
    public static final String POSTED = "6";
    public static final String RETURNED = "9";
    
    public static class Reverse  {
        public static final  String INCLUDE = "+"; 
        public static final  String EXCLUDE = "-"; 
    }
    
    public static class SourceCode  {
        public static final  String STOCKREQUEST = "InvR"; 
        public static final  String POQUOTATION = "POQt"; 
    }

    public static class IndustryCode {
        public static final String MOTORCYCLE = "01";
        public static final String MOBILEPHONE = "02";
        public static final String AUTO_GROUP_HONDA = "03";
        public static final String MONARCH = "04";
        public static final String AUTO_GROUP_NISSAN = "05";
        public static final String AUTO_GROUP_ANY = "06";
        public static final String GUANZON_SERVICE_OFFICE = "07";
        public static final String MAIN_OFFICE = "08";
        public static final String GENERAL = "09";
        public static final String ENGINEERING = "10";
        public static final String APPLIANCES = "11";
        public static final String PEDRITOS = "12";
    }
}
