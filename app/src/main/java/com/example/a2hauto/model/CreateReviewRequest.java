package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;

public class CreateReviewRequest {
    @SerializedName("reviewerId")
    private final String reviewerId;

    @SerializedName("revieweeId")
    private final String revieweeId;

    @SerializedName("listingId")
    private final String listingId;

    @SerializedName("rating")
    private final int rating;

    @SerializedName("comment")
    private final String comment;

    public CreateReviewRequest(String reviewerId, String revieweeId, String listingId, int rating, String comment) {
        this.reviewerId = reviewerId;
        this.revieweeId = revieweeId;
        this.listingId = listingId;
        this.rating = rating;
        this.comment = comment;
    }
}
