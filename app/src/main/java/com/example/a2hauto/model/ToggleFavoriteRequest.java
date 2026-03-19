package com.example.a2hauto.model;

public class ToggleFavoriteRequest {
    private final String userId;
    private final String listingId;

    public ToggleFavoriteRequest(String userId, String listingId) {
        this.userId = userId;
        this.listingId = listingId;
    }

    public String getUserId() {
        return userId;
    }

    public String getListingId() {
        return listingId;
    }
}
