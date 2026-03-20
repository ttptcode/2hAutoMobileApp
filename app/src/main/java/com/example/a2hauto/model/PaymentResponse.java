package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;

public class PaymentResponse {
    @SerializedName("paymentUrl")
    private String paymentUrl;

    @SerializedName("paymentId")
    private String paymentId;

    @SerializedName("amount")
    private double amount;

    @SerializedName("status")
    private String status;

    public PaymentResponse() {}

    public PaymentResponse(String paymentUrl, String paymentId, double amount, String status) {
        this.paymentUrl = paymentUrl;
        this.paymentId = paymentId;
        this.amount = amount;
        this.status = status;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

