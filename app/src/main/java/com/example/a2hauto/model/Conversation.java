package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;

public class Conversation {
    @SerializedName("conversationId")
    private String conversationId;

    @SerializedName("updatedAt")
    private String updatedAt;

    @SerializedName("lastMessage")
    private String lastMessage;

    @SerializedName("listingId")
    private String listingId;

    @SerializedName("seller")
    private UserBrief seller;

    @SerializedName("buyer")
    private UserBrief buyer;

    public String getConversationId() {
        return conversationId;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getListingId() {
        return listingId;
    }

    public UserBrief getSeller() {
        return seller;
    }

    public UserBrief getBuyer() {
        return buyer;
    }
}

