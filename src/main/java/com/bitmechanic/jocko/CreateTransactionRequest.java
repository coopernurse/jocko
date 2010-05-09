package com.bitmechanic.jocko;

import java.util.Map;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 5, 2010
 */
public class CreateTransactionRequest {

    public enum Type {
        AuthOnly, AuthCapture;
    }

    private Type type;
    private String billingProfileId;
    private String billingPaymentMethodId;
    private double totalAmount;
    private double taxAmount;
    private String externalSystemOrderId;

    public String getBillingPaymentMethodId() {
        return billingPaymentMethodId;
    }

    public void setBillingPaymentMethodId(String billingPaymentMethodId) {
        this.billingPaymentMethodId = billingPaymentMethodId;
    }

    public String getBillingProfileId() {
        return billingProfileId;
    }

    public void setBillingProfileId(String billingProfileId) {
        this.billingProfileId = billingProfileId;
    }

    public String getExternalSystemOrderId() {
        return externalSystemOrderId;
    }

    public void setExternalSystemOrderId(String externalSystemOrderId) {
        this.externalSystemOrderId = externalSystemOrderId;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(double taxAmount) {
        this.taxAmount = taxAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
