package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;

public class UserPackage {

    @SerializedName("userId")
    private String userId;

    @SerializedName("feeId")
    private String feeId;

    @SerializedName("remainingListings")
    private int remainingListings;

    @SerializedName("activatedAt")
    private String activatedAt;

    @SerializedName("expiredAt")
    private String expiredAt;

    @SerializedName("status")
    private String status;

    @SerializedName("month")
    private int month;

    @SerializedName("totalAmount")
    private double totalAmount;

    @SerializedName("feeCommission")
    private FeeCommission feeCommission;

    public String getUserId() {
        return userId;
    }

    public String getFeeId() {
        return feeId;
    }

    public int getRemainingListings() {
        return remainingListings;
    }

    public String getActivatedAt() {
        return activatedAt;
    }

    public String getExpiredAt() {
        return expiredAt;
    }

    public String getStatus() {
        return status;
    }

    public int getMonth() {
        return month;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public FeeCommission getFeeCommission() {
        return feeCommission;
    }
}
