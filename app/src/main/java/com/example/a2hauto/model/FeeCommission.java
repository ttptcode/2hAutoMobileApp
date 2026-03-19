package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;

public class FeeCommission {
    @SerializedName("feeId")
    private String feeId;

    @SerializedName("feeName")
    private String feeName;

    @SerializedName("feeType")
    private String feeType;

    @SerializedName("amount")
    private double amount;

    @SerializedName("packageDurationDays")
    private int packageDurationDays;

    @SerializedName("maxListings")
    private int maxListings;

    @SerializedName("savingAmount")
    private double savingAmount;

    @SerializedName("description")
    private String description;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("status")
    private boolean status;

    public String getFeeId() {
        return feeId;
    }

    public String getFeeName() {
        return feeName;
    }

    public String getFeeType() {
        return feeType;
    }

    public double getAmount() {
        return amount;
    }

    public int getPackageDurationDays() {
        return packageDurationDays;
    }

    public int getMaxListings() {
        return maxListings;
    }

    public double getSavingAmount() {
        return savingAmount;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public boolean isStatus() {
        return status;
    }
}
