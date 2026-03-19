package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;

public class UserProfile {
    @SerializedName(value = "id", alternate = {"userId", "_id"})
    private String id;

    @SerializedName(value = "fullName", alternate = {"name", "displayName"})
    private String fullName;

    @SerializedName(value = "email", alternate = {"mail", "emailAddress"})
    private String email;

    @SerializedName(value = "phoneNumber", alternate = {"phone", "phone_number", "mobile"})
    private String phoneNumber;

    @SerializedName(value = "createdAt", alternate = {"createdDate", "created_time"})
    private String createdAt;

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
