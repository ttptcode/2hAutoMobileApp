package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Listing implements Serializable {
    @SerializedName("listingId")
    private String listingId;
    @SerializedName("userId")
    private String userId;
    @SerializedName("listingType")
    private String listingType;
    @SerializedName("buyNowPrice")
    private double buyNowPrice;
    @SerializedName("userName")
    private String userName;
    @SerializedName("itemTitle")
    private String itemTitle;
    @SerializedName("status")
    private String status;
    @SerializedName("item")
    private Item item;
    @SerializedName("address")
    private String address;
    @SerializedName("detail")
    private String detail;
    @SerializedName("createdAt")
    private String createdAt;
    @SerializedName("endDate")
    private String endDate;

    public String getListingId() { return listingId; }
    public String getUserId() { return userId; }
    public String getListingType() { return listingType; }
    public double getBuyNowPrice() { return buyNowPrice; }
    public String getUserName() { return userName; }
    public String getItemTitle() { return itemTitle; }
    public String getStatus() { return status; }
    public Item getItem() { return item; }
    public String getAddress() { return address; }
    public String getDetail() { return detail; }
    public String getCreatedAt() { return createdAt; }
    public String getEndDate() { return endDate; }
    
    public String getDisplayTitle() {
        if (itemTitle != null && !itemTitle.isEmpty()) return itemTitle;
        if (item != null && item.getTitle() != null && !item.getTitle().isEmpty()) {
            return item.getTitle();
        }
        if (item != null) return item.getDisplayName();
        return "Không có tiêu đề";
    }
}
