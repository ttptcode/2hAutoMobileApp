package com.example.a2hauto.model.auth;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {

    @SerializedName("phoneNumber")
    private final String phoneNumber;

    @SerializedName("password")
    private final String password;

    public LoginRequest(String phoneNumber, String password) {
        this.phoneNumber = phoneNumber;
        this.password = password;
    }
}

