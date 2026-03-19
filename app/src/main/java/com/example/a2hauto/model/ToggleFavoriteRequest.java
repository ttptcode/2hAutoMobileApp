package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;

public class ToggleFavoriteRequest {
    @SerializedName("userId")
    private final String userId;

    @SerializedName("listingId")
    private final String listingId;

    public ToggleFavoriteRequest(String userId, String listingId) {
        this.userId = userId;
        this.listingId = listingId;
    }
}
