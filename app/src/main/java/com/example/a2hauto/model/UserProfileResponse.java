package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;

public class UserProfileResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private UserProfile data;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public UserProfile getData() {
        return data;
    }
}
