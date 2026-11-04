package org.guanzon.cas.purchasing.controller;

import java.util.ArrayList;

import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.cas.inv.InvTransCons;
import org.guanzon.cas.purchasing.model.Model_PO_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Master;
import org.guanzon.cas.purchasing.services.QuotationModels;
import org.json.simple.JSONObject;

public class PurchaseOrderx extends Transaction{
    JSONObject poJSON;

    public JSONObject InitTransaction() {
         SOURCE_CODE = InvTransCons.PURCHASE_ORDER_QUOTATION_REQUEST;

        poMaster = new QuotationModels(poGRider).POQuotationRequestMaster();
        poDetail = new QuotationModels(poGRider).POQuotationRequestDetails();
        paDetail = new ArrayList<>();

        return initialize();
    }

    public JSONObject NewTransaction() {
        return null;
    }

    public JSONObject OpenTransaction() {
        return null;
    }

    public JSONObject UpdateTransaction() {
        return null;
    }

    public JSONObject SaveTransaction() {
        return null;
    }

    public JSONObject ApproveTransaction(String remarks) {
        return null;
    }

    public JSONObject CancelTransaction(String remarks) {
        return null;
    }

    public JSONObject ConfirmTransaction(String remarks) {
        return null;
    }

    public JSONObject PostTransaction(String remarks) {
        return null;
    }

    public JSONObject AddDetail() throws CloneNotSupportedException {
        JSONObject loJSON = new JSONObject();
        if (Detail(getDetailCount() - 1).getStockID().isEmpty()) {
            loJSON = new JSONObject();
            loJSON.put("result", "error");
            loJSON.put("message", "Last row has empty item.");
            return loJSON;
        }
        return addDetail();
    }

    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }

    @Override
    public Model_PO_Master Master() {
        return (Model_PO_Master) poMaster;
    }

    @Override
    public Model_PO_Detail Detail(int row) {
        return (Model_PO_Detail) paDetail.get(row);
    }
}