package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;

public class UserBrief {
    @SerializedName("userId")
    private String userId;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("avatar")
    private String avatar;

    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAvatar() {
        return avatar;
    }
}

