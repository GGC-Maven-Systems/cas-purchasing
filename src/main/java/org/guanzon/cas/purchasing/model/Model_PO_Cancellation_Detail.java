package org.guanzon.cas.purchasing.model;

import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.inv.model.Model_Inv_Master;
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.services.InvModels;
import org.guanzon.cas.purchasing.services.PurchaseOrderModels;
import org.json.simple.JSONObject;

/**
 *
 * @author maynevval 08-09-2025
 */
public class Model_PO_Cancellation_Detail extends Model {

    //poPurchaseOrder/poInventory/poInventoryMaster are intentionally NOT constructed in
    //initialize() - see their accessors below, which build them lazily on first access so
    //opening this record never touches those tables.
    Model_PO_Detail poPurchaseOrder;
    Model_Inventory poInventory;
    Model_Inv_Master poInventoryMaster;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);

            poEntity.updateObject("nEntryNox", 1);
            poEntity.updateNull("sOrderNox");
            poEntity.updateDouble("nQuantity", 0.00d);
            poEntity.updateDouble("nUnitPrce", 0.00d);
            poEntity.updateObject("dModified", poGRider.getServerDate());

            ID = poEntity.getMetaData().getColumnLabel(1);
            ID2 = poEntity.getMetaData().getColumnLabel(2);

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }
    //Getter & Setter 
    //sTransNox
    //nEntryNox*
    //sOrderNox*
    //sStockIDx*
    //nQuantity*
    //nUnitPrce*
    //dModified*

    //sTransNox
    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    //nEntryNox
    public JSONObject setEntryNo(int entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public int getEntryNo() {
        return (int) getValue("nEntryNox");
    }

    //sOrderNox
    public JSONObject setOrderNo(String orderNo) {
        return setValue("sOrderNox", orderNo);
    }

    public String getOrderNo() {
        return (String) getValue("sOrderNox");
    }

    //sStockIDx
    public JSONObject setStockId(String stockId) {
        return setValue("sStockIDx", stockId);
    }

    public String getStockId() {
        return (String) getValue("sStockIDx");
    }

    //nQuantity
    public JSONObject setQuantity(Double quantity) {
        return setValue("nQuantity", quantity);
    }

    public Double getQuantity() {
        return Double.valueOf(getValue("nQuantity").toString());
    }

    //nUnitPrce
    public JSONObject setUnitPrice(Double unitPrice) {
        return setValue("nUnitPrce", unitPrice);
    }

    public Double getUnitPrice() {
        return Double.valueOf(getValue("nUnitPrce").toString());
    }

    //dModified
    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    @Override
    public String getNextCode() {
        return "";
    }

    public Model_Inventory Inventory() throws GuanzonException, SQLException {
        if (poInventory == null) {
            poInventory = new InvModels(poGRider).Inventory();
        }

        if (!"".equals((String) getValue("sStockIDx"))) {
            if (poInventory.getEditMode() == EditMode.READY
                    && poInventory.getStockId().equals((String) getValue("sStockIDx"))) {
                return poInventory;
            } else {

                poJSON = poInventory.openRecord((String) getValue("sStockIDx"));
                if ("success".equals((String) poJSON.get("result"))) {
                    return poInventory;
                } else {
                    poInventory.initialize();
                    return poInventory;
                }

            }
        } else {
            poInventory.initialize();
            return poInventory;
        }
    }

    public Model_Inv_Master InventoryMaster() throws GuanzonException, SQLException {
        if (poInventoryMaster == null) {
            poInventoryMaster = new InvModels(poGRider).InventoryMaster();
        }

        if (!"".equals((String) getValue("sStockIDx"))) {
            if (poInventoryMaster.getEditMode() == EditMode.READY
                    && poInventoryMaster.getStockId().equals((String) getValue("sStockIDx"))) {
                return poInventoryMaster;
            } else {
                poJSON = poInventoryMaster.openRecord((String) getValue("sStockIDx"), poGRider.getBranchCode());

                if ("success".equals((String) poJSON.get("result"))) {
                    return poInventoryMaster;
                } else {
                    poInventoryMaster.initialize();
                    return poInventoryMaster;
                }
            }
        } else {
            poInventoryMaster.initialize();
            return poInventoryMaster;
        }
    }

    public Model_PO_Detail PurchaseOrderDetail() throws SQLException, GuanzonException {
        if (poPurchaseOrder == null) {
            poPurchaseOrder = new PurchaseOrderModels(poGRider).PurchaseOrderDetails();
        }

        if (!"".equals(getValue("sOrderNox")) && !"".equals(getValue("sStockIDx"))) {
            if (this.poPurchaseOrder.getEditMode() == 1 && this.poPurchaseOrder
                    .getTransactionNo().equals(getValue("sOrderNox"))
                    && this.poPurchaseOrder.getEditMode() == 1 && this.poPurchaseOrder
                    .getStockID().equals(getValue("sStockIDx"))) {
                return this.poPurchaseOrder;
            }
            this.poJSON = this.poPurchaseOrder.openRecordByReference((String) getValue("sOrderNox"),
                    (String) getValue("sStockIDx"));
            if ("success".equals(this.poJSON.get("result"))) {
                return this.poPurchaseOrder;
            }
            this.poPurchaseOrder.initialize();
            return this.poPurchaseOrder;
        }
        poPurchaseOrder.initialize();
        return this.poPurchaseOrder;
    }
}
