package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;

public class Listing {
    @SerializedName("listingId")
    private String listingId;
    @SerializedName("listingType")
    private String listingType;
    @SerializedName("buyNowPrice")
    private double buyNowPrice;
    @SerializedName("userName")
    private String userName;
    @SerializedName("itemTitle")
    private String itemTitle;
    @SerializedName("item")
    private Item item;
    @SerializedName("address")
    private String address;

    public String getListingId() { return listingId; }
    public String getListingType() { return listingType; }
    public double getBuyNowPrice() { return buyNowPrice; }
    public String getUserName() { return userName; }
    public String getItemTitle() { return itemTitle; }
    public Item getItem() { return item; }
    public String getAddress() { return address; }
    
    public String getDisplayTitle() {
        if (itemTitle != null && !itemTitle.isEmpty()) return itemTitle;
        if (item != null) return item.getDisplayName();
        return "Không có tiêu đề";
    }
}
