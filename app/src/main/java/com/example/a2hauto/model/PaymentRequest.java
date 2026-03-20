package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;

public class PaymentRequest {
    @SerializedName("amount")
    private double amount;

    @SerializedName("feeId")
    private String feeId;

    public PaymentRequest(double amount, String feeId) {
        this.amount = amount;
        this.feeId = feeId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getFeeId() {
        return feeId;
    }

    public void setFeeId(String feeId) {
        this.feeId = feeId;
    }
}

