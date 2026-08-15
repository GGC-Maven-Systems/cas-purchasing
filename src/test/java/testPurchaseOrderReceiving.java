
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.cas.purchasing.controller.PurchaseOrderReceiving;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingControllers;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Arsiela 03-12-2025
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class testPurchaseOrderReceiving {
    
    static GRiderCAS instance;
    static PurchaseOrderReceiving poPurchaseReceivingController;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("sys.default.path.metadata", "D:/GGC_Maven_Systems/config/metadata/new/");

        instance = MiscUtil.Connect();
        
        poPurchaseReceivingController = new PurchaseOrderReceivingControllers(instance, null).PurchaseOrderReceiving();
    }

//    @Test
    public void testNewTransaction() {
        String branchCd = instance.getBranchCode();
        String industryId = "01";
        String remarks = "this is a test RSIE Class 3.";
        
        String stockId = "C0W125000001";
        int quantity = 110;
        
        JSONObject loJSON;
        
        try {
            
            loJSON = poPurchaseReceivingController.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 
            
            loJSON = poPurchaseReceivingController.NewTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 
            try {
                poPurchaseReceivingController.setIndustryId(industryId);
                poPurchaseReceivingController.setCompanyId("0002");
                poPurchaseReceivingController.setCategoryId("0001");
                
                poPurchaseReceivingController.initFields();
                poPurchaseReceivingController.Master().setCategoryCode("0001"); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getCategoryCode(), "0001");
                poPurchaseReceivingController.Master().setIndustryId(industryId); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getIndustryId(), industryId);
                poPurchaseReceivingController.Master().setTransactionDate(instance.getServerDate()); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getTransactionDate(), instance.getServerDate());
                poPurchaseReceivingController.Master().setReferenceDate(instance.getServerDate()); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getReferenceDate(), instance.getServerDate());
                poPurchaseReceivingController.Master().setReferenceNo("01"); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getReferenceNo(), "01");
                poPurchaseReceivingController.Master().setCompanyId("0002"); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getCompanyId(), "0002");
                poPurchaseReceivingController.Master().setDepartmentId("026"); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getDepartmentId(), "026");
                poPurchaseReceivingController.Master().setSupplierId("C00124000020"); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getSupplierId(), "C00124000020");
                poPurchaseReceivingController.Master().setTruckingId("C00124000010"); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getTruckingId(), "C00124000010");
                poPurchaseReceivingController.Master().setTermCode("0000003"); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getTermCode(), "0000003");
                poPurchaseReceivingController.Master().setBranchCode(branchCd); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getBranchCode(), branchCd);
                
                poPurchaseReceivingController.Master().setRemarks(remarks);
                Assert.assertEquals(poPurchaseReceivingController.Master().getRemarks(), remarks);

                poPurchaseReceivingController.Detail(0).setStockId(stockId);
                poPurchaseReceivingController.Detail(0).setQuantity(quantity);
                poPurchaseReceivingController.Detail(0).isSerialized(true);
                poPurchaseReceivingController.Detail(0).setQuantity(1);
                poPurchaseReceivingController.AddDetail();
                
                poPurchaseReceivingController.Detail(1).setStockId("C0W125000004");
                poPurchaseReceivingController.Detail(1).setQuantity(quantity);
                poPurchaseReceivingController.Detail(1).isSerialized(true);
                poPurchaseReceivingController.Detail(1).setQuantity(0);
                poPurchaseReceivingController.AddDetail();
                
                poPurchaseReceivingController.Detail(2).setStockId("M00125000001");
                poPurchaseReceivingController.Detail(2).setQuantity(quantity);
                poPurchaseReceivingController.Detail(2).isSerialized(true);
                poPurchaseReceivingController.Detail(2).setQuantity(0);
                poPurchaseReceivingController.AddDetail();
                
                poPurchaseReceivingController.Detail(3).setStockId("M00124000002");
                poPurchaseReceivingController.Detail(3).setQuantity(quantity);
                poPurchaseReceivingController.Detail(3).isSerialized(true);
                poPurchaseReceivingController.Detail(3).setQuantity(1);
                poPurchaseReceivingController.AddDetail();
                
                poPurchaseReceivingController.computeFields();
                
                //populate POR Serial
                loJSON = poPurchaseReceivingController.getPurchaseOrderReceivingSerial(1);
                if("success".equals((String) loJSON.get("result"))){
                    System.out.println("inv serial cnt : " + poPurchaseReceivingController.getPurchaseOrderReceivingSerialCount());
                    poPurchaseReceivingController.PurchaseOrderReceivingSerialList(0).setLocationId("333");
                    poPurchaseReceivingController.PurchaseOrderReceivingSerialList(0).setStockId(stockId);
                    poPurchaseReceivingController.PurchaseOrderReceivingSerialList(0).setSerial01("mobilephone101");
                    poPurchaseReceivingController.PurchaseOrderReceivingSerialList(0).setSerial02("mobilephone202");
//                    poPurchaseReceivingController.PurchaseOrderReceivingSerialList(0).setPlateNo("001sa1");
//                    poPurchaseReceivingController.PurchaseOrderReceivingSerialList(0).setConductionStickerNo("333");
                }
                
                //populate POR Serial
                loJSON = poPurchaseReceivingController.getPurchaseOrderReceivingSerial(4);
                if("success".equals((String) loJSON.get("result"))){
                    System.out.println("inv serial cnt : " + poPurchaseReceivingController.getPurchaseOrderReceivingSerialCount());
                    poPurchaseReceivingController.PurchaseOrderReceivingSerialList(1).setLocationId("333");
                    poPurchaseReceivingController.PurchaseOrderReceivingSerialList(1).setStockId("M00124000002");
                    poPurchaseReceivingController.PurchaseOrderReceivingSerialList(1).setSerial01("mob");
                    poPurchaseReceivingController.PurchaseOrderReceivingSerialList(1).setSerial02("phone");
//                    poPurchaseReceivingController.PurchaseOrderReceivingSerialList(0).setPlateNo("001sa1");
//                    poPurchaseReceivingController.PurchaseOrderReceivingSerialList(0).setConductionStickerNo("333");
                }
                
                System.out.println("Industry ID : " + instance.getIndustry());
                System.out.println("Industry : " + poPurchaseReceivingController.Master().Industry().getDescription());
                System.out.println("TransNox : " + poPurchaseReceivingController.Master().getTransactionNo());
                
                loJSON = poPurchaseReceivingController.SaveTransaction();
                if (!"success".equals((String) loJSON.get("result"))) {
                    System.err.println((String) loJSON.get("message"));
                    Assert.fail();
                }
            
            } catch (SQLException | GuanzonException ex) {
                Logger.getLogger(testPurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (ExceptionInInitializerError e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(testPurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(testPurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GuanzonException ex) {
            Logger.getLogger(testPurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
//    @Test
    public void testUpdateTransaction() {
        JSONObject loJSON;
       
        try {
            loJSON = poPurchaseReceivingController.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = poPurchaseReceivingController.OpenTransaction("M00125000006");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = poPurchaseReceivingController.UpdateTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 
            
            //Populate purhcase receiving serials
            for(int lnCtr = 0; lnCtr <= poPurchaseReceivingController.getDetailCount()-1; lnCtr++){
                poPurchaseReceivingController.getPurchaseOrderReceivingSerial(poPurchaseReceivingController.Detail(lnCtr).getEntryNo());
            }

//            poPurchaseReceivingController.Detail(1).setQuantity(0);
//            poPurchaseReceivingController.AddDetail();

//            for(int lnCtr = 0;lnCtr <= poPurchaseReceivingController.getDetailCount()-1; lnCtr++){
//                poPurchaseReceivingController.Detail(0).setQuantity(2);
//                poPurchaseReceivingController.Detail(1).setQuantity(5);
//                System.out.println("DATA Before SAVE TRANSACTION Method");
//                System.out.println("TransNo : " + (lnCtr+1) + " : " + poPurchaseReceivingController.Detail(lnCtr).getTransactionNo());
//                System.out.println("OrderNo : " + (lnCtr+1) + " : " + poPurchaseReceivingController.Detail(lnCtr).getOrderNo());
//                System.out.println("StockId : " + (lnCtr+1) + " : " + poPurchaseReceivingController.Detail(lnCtr).getStockId());
//                System.out.println("Quantty : " + (lnCtr+1) + " : " + poPurchaseReceivingController.Detail(lnCtr).getQuantity());
//                System.out.println("---------------------------------------------------------------------");
//            }

            loJSON = poPurchaseReceivingController.SaveTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }
        } catch (CloneNotSupportedException | SQLException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (GuanzonException ex) {
            Logger.getLogger(testPurchaseOrderReceiving.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        
    }   
    
//    @Test
    public void testgetPurchaseOrderList() {
        String industryId = "02";
        String companyId = "0001";
        String supplierId = "C00124000009";
        
        JSONObject loJSON;
        
        loJSON = poPurchaseReceivingController.InitTransaction();
        if (!"success".equals((String) loJSON.get("result"))){
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
        
        poPurchaseReceivingController.Master().setIndustryId(industryId); //direct assignment of value
        poPurchaseReceivingController.Master().setCompanyId(companyId); //direct assignment of value
        poPurchaseReceivingController.Master().setSupplierId(supplierId); //direct assignment of value
        
        
        loJSON = poPurchaseReceivingController.getApprovedPurchaseOrder();
        if (!"success".equals((String) loJSON.get("result"))) {
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
        
        //retreiving using column index
        for (int lnCtr = 0; lnCtr <= poPurchaseReceivingController.getPurchaseOrderCount() - 1; lnCtr++){
            try {
                System.out.println("PO Row No ->> " + lnCtr);
                System.out.println("PO Transaction No ->> " + poPurchaseReceivingController.PurchaseOrderList(lnCtr).getTransactionNo());
                System.out.println("PO Transaction Date ->> " + poPurchaseReceivingController.PurchaseOrderList(lnCtr).getTransactionDate());
                System.out.println("PO Industry ->> " + poPurchaseReceivingController.PurchaseOrderList(lnCtr).Industry().getDescription());
                System.out.println("PO Company ->> " + poPurchaseReceivingController.PurchaseOrderList(lnCtr).Company().getCompanyName());
                System.out.println("PO Supplier ->> " + poPurchaseReceivingController.PurchaseOrderList(lnCtr).Supplier().getCompanyName());
                System.out.println("----------------------------------------------------------------------------------");
            } catch (GuanzonException | SQLException ex) {
                Logger.getLogger(testPurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
//     @Test
    public void testOpenTransaction() {
        JSONObject loJSON;
        
        try {
            loJSON = poPurchaseReceivingController.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = poPurchaseReceivingController.OpenTransaction("M00125000002");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            //retreiving using column index
            for (int lnCol = 1; lnCol <= poPurchaseReceivingController.Master().getColumnCount(); lnCol++){
                System.out.println(poPurchaseReceivingController.Master().getColumn(lnCol) + " ->> " + poPurchaseReceivingController.Master().getValue(lnCol));
            }
            //retreiving using field descriptions
            System.out.println(poPurchaseReceivingController.Master().Branch().getBranchName());
            System.out.println(poPurchaseReceivingController.Master().Category().getDescription());

            //retreiving using column index
            for (int lnCtr = 0; lnCtr <= poPurchaseReceivingController.Detail().size() - 1; lnCtr++){
                for (int lnCol = 1; lnCol <= poPurchaseReceivingController.Detail(lnCtr).getColumnCount(); lnCol++){
                    System.out.println(poPurchaseReceivingController.Detail(lnCtr).getColumn(lnCol) + " ->> " + poPurchaseReceivingController.Detail(lnCtr).getValue(lnCol));
                }
            }
        } catch (CloneNotSupportedException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(testPurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }   
    
//    @Test
    public void testConfirmTransaction() {
        JSONObject loJSON;
        
        try {
            loJSON = poPurchaseReceivingController.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = poPurchaseReceivingController.OpenTransaction("M00125000001");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            //retreiving using column index
            for (int lnCol = 1; lnCol <= poPurchaseReceivingController.Master().getColumnCount(); lnCol++){
                System.out.println(poPurchaseReceivingController.Master().getColumn(lnCol) + " ->> " + poPurchaseReceivingController.Master().getValue(lnCol));
            }
            //retreiving using field descriptions
            System.out.println(poPurchaseReceivingController.Master().Branch().getBranchName());
            System.out.println(poPurchaseReceivingController.Master().Category().getDescription());

            //retreiving using column index
            for (int lnCtr = 0; lnCtr <= poPurchaseReceivingController.Detail().size() - 1; lnCtr++){
                for (int lnCol = 1; lnCol <= poPurchaseReceivingController.Detail(lnCtr).getColumnCount(); lnCol++){
                    System.out.println(poPurchaseReceivingController.Detail(lnCtr).getColumn(lnCol) + " ->> " + poPurchaseReceivingController.Detail(lnCtr).getValue(lnCol));
                }
            }
            
            loJSON = poPurchaseReceivingController.ConfirmTransaction("test confirm");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 
            
            System.out.println((String) loJSON.get("message"));
        } catch (CloneNotSupportedException |ParseException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(testPurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        }
    }   
    
//    @Test
//    public void testReturnTransaction() {
//        JSONObject loJSON;
//        
//        try {
//            loJSON = poPurchaseReceivingController.InitTransaction();
//            if (!"success".equals((String) loJSON.get("result"))){
//                System.err.println((String) loJSON.get("message"));
//                Assert.fail();
//            } 
//
//            loJSON = poPurchaseReceivingController.OpenTransaction("M00125000003");
//            if (!"success".equals((String) loJSON.get("result"))){
//                System.err.println((String) loJSON.get("message"));
//                Assert.fail();
//            } 
//
//            //retreiving using column index
//            for (int lnCol = 1; lnCol <= poPurchaseReceivingController.Master().getColumnCount(); lnCol++){
//                System.out.println(poPurchaseReceivingController.Master().getColumn(lnCol) + " ->> " + poPurchaseReceivingController.Master().getValue(lnCol));
//            }
//            //retreiving using field descriptions
//            System.out.println(poPurchaseReceivingController.Master().Branch().getBranchName());
//            System.out.println(poPurchaseReceivingController.Master().Category().getDescription());
//
//            //retreiving using column index
//            for (int lnCtr = 0; lnCtr <= poPurchaseReceivingController.Detail().size() - 1; lnCtr++){
//                for (int lnCol = 1; lnCol <= poPurchaseReceivingController.Detail(lnCtr).getColumnCount(); lnCol++){
//                    System.out.println(poPurchaseReceivingController.Detail(lnCtr).getColumn(lnCol) + " ->> " + poPurchaseReceivingController.Detail(lnCtr).getValue(lnCol));
//                }
//            }
//            
//            loJSON = poPurchaseReceivingController.ReturnTransaction("test return");
//            if (!"success".equals((String) loJSON.get("result"))){
//                System.err.println((String) loJSON.get("message"));
//                Assert.fail();
//            } 
//            
//            System.out.println((String) loJSON.get("message"));
//        } catch (CloneNotSupportedException |ParseException e) {
//            System.err.println(MiscUtil.getException(e));
//            Assert.fail();
//        } catch (SQLException | GuanzonException ex) {
//            Logger.getLogger(testPurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
    
//    @Test
    public void testOpenTransactionSIPosting() {
        JSONObject loJSON;
        
        try {
            loJSON = poPurchaseReceivingController.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = poPurchaseReceivingController.OpenTransaction("A00125000050");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            //retreiving using column index
            for (int lnCol = 1; lnCol <= poPurchaseReceivingController.Master().getColumnCount(); lnCol++){
                System.out.println(poPurchaseReceivingController.Master().getColumn(lnCol) + " ->> " + poPurchaseReceivingController.Master().getValue(lnCol));
            }
            //retreiving using field descriptions
            System.out.println(poPurchaseReceivingController.Master().Branch().getBranchName());
            System.out.println(poPurchaseReceivingController.Master().Category().getDescription());

            //retreiving using column index
            for (int lnCtr = 0; lnCtr <= poPurchaseReceivingController.Detail().size() - 1; lnCtr++){
                for (int lnCol = 1; lnCol <= poPurchaseReceivingController.Detail(lnCtr).getColumnCount(); lnCol++){
                    System.out.println(poPurchaseReceivingController.Detail(lnCtr).getColumn(lnCol) + " ->> " + poPurchaseReceivingController.Detail(lnCtr).getValue(lnCol));
                }
            }
            
            poPurchaseReceivingController.populateJournal();
            
            System.out.println("Journal Master : " + poPurchaseReceivingController.Journal().Master().getTransactionNo());
            System.out.println("Journal Master : " + poPurchaseReceivingController.Journal().Master().getTransactionDate());
            System.out.println("Journal Master : " + poPurchaseReceivingController.Journal().Master().getBranchCode());
            System.out.println("Journal Master : " + poPurchaseReceivingController.Journal().Master().getCompanyId());
            System.out.println("Journal Master : " + poPurchaseReceivingController.Journal().Master().getIndustryCode());
            System.out.println("Journal Master : " + poPurchaseReceivingController.Journal().Master().getDepartmentId());
            System.out.println("Journal Master : " + poPurchaseReceivingController.Journal().Master().getEntryNumber());
            System.out.println("Journal Master : " + poPurchaseReceivingController.Journal().Master().getSourceNo());
            System.out.println("Journal Master : " + poPurchaseReceivingController.Journal().Master().getSourceCode());
            System.out.println("Journal Master : " + poPurchaseReceivingController.Journal().Master().getRemarks());
            
            for(int lnCtr = 0; lnCtr <= poPurchaseReceivingController.Journal().Detail().size()-1;lnCtr++){
                System.out.println("Journal Detail : " + poPurchaseReceivingController.Journal().Detail(lnCtr).getTransactionNo());
                System.out.println("Journal Detail : " + poPurchaseReceivingController.Journal().Detail(lnCtr).getEntryNumber());
                System.out.println("Journal Detail : " + poPurchaseReceivingController.Journal().Detail(lnCtr).getForMonthOf());
                System.out.println("Journal Detail : " + poPurchaseReceivingController.Journal().Detail(lnCtr).getCreditAmount());
                System.out.println("Journal Detail : " + poPurchaseReceivingController.Journal().Detail(lnCtr).getDebitAmount());
            }
            
        } catch (CloneNotSupportedException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(testPurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ScriptException ex) {
            Logger.getLogger(testPurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    } 
        
//    @Test
    public void testApproveTransaction() {
        JSONObject loJSON;

        try {
            loJSON = poPurchaseReceivingController.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = poPurchaseReceivingController.OpenTransaction("A00125000050");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            //retreiving using column index
            for (int lnCol = 1; lnCol <= poPurchaseReceivingController.Master().getColumnCount(); lnCol++){
                System.out.println(poPurchaseReceivingController.Master().getColumn(lnCol) + " ->> " + poPurchaseReceivingController.Master().getValue(lnCol));
            }
            //retreiving using field descriptions
            System.out.println(poPurchaseReceivingController.Master().Branch().getBranchName());
            System.out.println(poPurchaseReceivingController.Master().Category().getDescription());

            //retreiving using column index
            for (int lnCtr = 0; lnCtr <= poPurchaseReceivingController.Detail().size() - 1; lnCtr++){
                for (int lnCol = 1; lnCol <= poPurchaseReceivingController.Detail(lnCtr).getColumnCount(); lnCol++){
                    System.out.println(poPurchaseReceivingController.Detail(lnCtr).getColumn(lnCol) + " ->> " + poPurchaseReceivingController.Detail(lnCtr).getValue(lnCol));
                }
            }

            poPurchaseReceivingController.populateJournal();
            loJSON = poPurchaseReceivingController.PostTransaction("test post");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            System.out.println((String) loJSON.get("message"));
        } catch (CloneNotSupportedException |ParseException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(testPurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ScriptException ex) {
            Logger.getLogger(testPurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
        }
    }  
    
//    @Test
    public void testgetPurchaseOrderReturnList() {
        String industryId = "01";
        String companyId = "0001";
        String categoryId = "0001";
        
        JSONObject loJSON;
        
        loJSON = poPurchaseReceivingController.InitTransaction();
        if (!"success".equals((String) loJSON.get("result"))){
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
        
        poPurchaseReceivingController.setIndustryId(industryId); //direct assignment of value
        poPurchaseReceivingController.setCompanyId(companyId); //direct assignment of value
        poPurchaseReceivingController.setCategoryId(categoryId); //direct assignment of value
        
        poPurchaseReceivingController.Master().setIndustryId(industryId); //direct assignment of value
        poPurchaseReceivingController.Master().setCompanyId(companyId); //direct assignment of value
        
        loJSON = poPurchaseReceivingController.getPurchaseOrderReturn("");
        if (!"success".equals((String) loJSON.get("result"))) {
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
        
        //retreiving using column index
        for (int lnCtr = 0; lnCtr <= poPurchaseReceivingController.getPurchaseOrderReturnCount() - 1; lnCtr++){
            try {
                System.out.println("PO Return Row No ->> " + lnCtr);
                System.out.println("PO Return Transaction No ->> " + poPurchaseReceivingController.PurchaseOrderReturnList(lnCtr).getTransactionNo());
                System.out.println("PO Return Transaction Date ->> " + poPurchaseReceivingController.PurchaseOrderReturnList(lnCtr).getTransactionDate());
                System.out.println("PO Return Industry ->> " + poPurchaseReceivingController.PurchaseOrderReturnList(lnCtr).Industry().getDescription());
                System.out.println("PO Return Company ->> " + poPurchaseReceivingController.PurchaseOrderReturnList(lnCtr).Company().getCompanyName());
                System.out.println("PO Return Supplier ->> " + poPurchaseReceivingController.PurchaseOrderReturnList(lnCtr).Supplier().getCompanyName());
                System.out.println("----------------------------------------------------------------------------------");
            } catch (GuanzonException | SQLException ex) {
                Logger.getLogger(testPurchaseOrderReceiving.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
