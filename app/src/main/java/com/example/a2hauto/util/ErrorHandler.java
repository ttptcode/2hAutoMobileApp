package com.example.a2hauto.util;

import android.content.Context;
import android.widget.Toast;

import com.example.a2hauto.model.ApiResponse;

import retrofit2.Response;

/**
 * Global error handler utility class
 * Xử lý lỗi API response một cách tập trung
 */
public class ErrorHandler {

    /**
     * Xử lý error response từ API
     * @param context Activity context để hiển thị Toast
     * @param response Response từ API
     */
    public static void handleErrorResponse(Context context, Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
                com.google.gson.JsonObject jsonObject = parser.parse(errorBody).getAsJsonObject();
                
                String errorMessage = extractErrorMessage(jsonObject);
                showError(context, errorMessage);
            } else {
                String errorMsg = "Lỗi: " + response.code() + " " + response.message();
                showError(context, errorMsg);
            }
        } catch (Exception e) {
            showError(context, "Lỗi phân tích response: " + e.getMessage());
        }
    }

    /**
     * Xử lý lỗi kết nối
     * @param context Activity context để hiển thị Toast
     * @param throwable Exception từ onFailure
     */
    public static void handleNetworkError(Context context, Throwable throwable) {
        String message = "Lỗi kết nối: " + (throwable.getMessage() != null ? throwable.getMessage() : "Không xác định");
        showError(context, message);
    }

    /**
     * Trích xuất message lỗi từ JSON response
     * Ưu tiên lỗi chi tiết trong mảng errors, nếu không có thì lấy message chung
     */
    private static String extractErrorMessage(com.google.gson.JsonObject jsonObject) {
        // Lấy lỗi chi tiết từ mảng errors
        if (jsonObject.has("errors") && jsonObject.get("errors").isJsonArray()) {
            com.google.gson.JsonArray errorsArray = jsonObject.getAsJsonArray("errors");
            if (errorsArray.size() > 0) {
                return errorsArray.get(0).getAsString();
            }
        }
        
        // Lấy message chung nếu không có lỗi chi tiết
        if (jsonObject.has("message")) {
            return jsonObject.get("message").getAsString();
        }
        
        return "Lỗi không xác định";
    }

    /**
     * Hiển thị Toast lỗi
     */
    private static void showError(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Lấy error message mà không hiển thị Toast (để xử lý sau)
     */
    public static String getErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
                com.google.gson.JsonObject jsonObject = parser.parse(errorBody).getAsJsonObject();
                return extractErrorMessage(jsonObject);
            } else {
                return "Lỗi: " + response.code() + " " + response.message();
            }
        } catch (Exception e) {
            return "Lỗi phân tích response: " + e.getMessage();
        }
    }
}

