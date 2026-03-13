package com.example.a2hauto.model.auth;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {

    @SerializedName("fullName")
    private final String fullName;

    @SerializedName("phoneNumber")
    private final String phoneNumber;

    @SerializedName("password")
    private final String password;

    public RegisterRequest(String fullName, String phoneNumber, String password) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.password = password;
    }
}

