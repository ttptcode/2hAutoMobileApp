package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;

public class FavoriteItem {
    @SerializedName("favoriteId")
    private String favoriteId;

    @SerializedName("userId")
    private String userId;

    @SerializedName("userName")
    private String userName;

    @SerializedName("listingId")
    private String listingId;

    @SerializedName("createdAt")
    private String createdAt;

    public String getFavoriteId() {
        return favoriteId;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getListingId() {
        return listingId;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
