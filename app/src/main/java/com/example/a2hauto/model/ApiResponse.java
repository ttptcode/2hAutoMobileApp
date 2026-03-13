package com.example.a2hauto.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ApiResponse<T> {
    @SerializedName("success")
    private boolean success;
    @SerializedName("message")
    private String message;
    @SerializedName("data")
    private T data;
    @SerializedName("errors")
    private List<String> errors;
    @SerializedName("timestamp")
    private String timestamp;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public List<String> getErrors() { return errors; }
    public String getTimestamp() { return timestamp; }
}
