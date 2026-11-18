/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.cas.purchasing.ui.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author User
 */
public class ModelPOAttachment {

    public StringProperty index01;
    public StringProperty index02;
    public StringProperty index03;
    public StringProperty index04;
    public StringProperty index05;
    public StringProperty index06;
    public StringProperty index07;
    public StringProperty index08;
    public StringProperty index09;
    public StringProperty index10;
    public StringProperty index11;
    public StringProperty index12;
    public StringProperty index13;
    public StringProperty index14;
    public StringProperty index15;
    public StringProperty index16;
    public StringProperty index17;
    public StringProperty index18;
    public StringProperty index19;
    public StringProperty index20;
    /*04272026 - Team 1 Removed 'Billing Statement' option in documentType as it dd not exist to options used in DV*/
    public static ObservableList<String> documentType = FXCollections.observableArrayList("Other", "Delivery Receipt", "Sales Invoice", "Official Receipt", "Quotation");
    
    public ModelPOAttachment(String index01,
            String index02,
            String index03,
            String index04,
            String index05,
            String index06,
            String index07,
            String index08,
            String index09,
            String index10,
            String index11,
            String index12,
            String index13,
            String index14,
            String index15,
            String index16,
            String index17,
            String index18,
            String index19,
            String index20) {

        this.index01 = new SimpleStringProperty(index01);
        this.index02 = new SimpleStringProperty(index02);
        this.index03 = new SimpleStringProperty(index03);
        this.index04 = new SimpleStringProperty(index04);
        this.index05 = new SimpleStringProperty(index05);
        this.index06 = new SimpleStringProperty(index06);
        this.index07 = new SimpleStringProperty(index07);
        this.index08 = new SimpleStringProperty(index08);
        this.index09 = new SimpleStringProperty(index09);
        this.index10 = new SimpleStringProperty(index10);
        this.index11 = new SimpleStringProperty(index11);
        this.index12 = new SimpleStringProperty(index12);
        this.index13 = new SimpleStringProperty(index13);
        this.index14 = new SimpleStringProperty(index14);
        this.index15 = new SimpleStringProperty(index15);
        this.index16 = new SimpleStringProperty(index16);
        this.index17 = new SimpleStringProperty(index17);
        this.index18 = new SimpleStringProperty(index18);
        this.index19 = new SimpleStringProperty(index19);
        this.index20 = new SimpleStringProperty(index20);
    }

    public ModelPOAttachment(String index01,
            String index02) {

        this.index01 = new SimpleStringProperty(index01);
        this.index02 = new SimpleStringProperty(index02);

    }
    public ModelPOAttachment(String index01,
            String index02,
            String index03) {

        this.index01 = new SimpleStringProperty(index01);
        this.index02 = new SimpleStringProperty(index02);
        this.index03 = new SimpleStringProperty(index03);

    }

    public String getIndex01() {
        return index01.get();
    }

    public void setIndex01(String index01) {
        this.index01.set(index01);
    }

    public String getIndex02() {
        return index02.get();
    }

    public void setIndex02(String index02) {
        this.index02.set(index02);
    }
    
     public String getIndex03() {
        return index03.get();
    }

    public void setIndex03(String index03) {
        this.index03.set(index03);
    }
    

}
