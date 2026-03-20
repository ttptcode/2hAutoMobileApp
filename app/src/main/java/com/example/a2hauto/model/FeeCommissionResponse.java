package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FeeCommissionResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private List<FeeCommission> data;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<FeeCommission> getData() {
        return data;
    }
}
