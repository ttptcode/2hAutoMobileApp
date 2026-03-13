package com.example.a2hauto.auth;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.example.a2hauto.api.ApiClient;
import com.example.a2hauto.api.ApiService;
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.auth.LoginRequest;
import com.example.a2hauto.model.auth.RegisterRequest;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    public interface AuthCallback {
        void onSuccess(String token, String message);

        void onError(String message);
    }

    private final ApiService apiService;

    public AuthRepository() {
        this(ApiClient.getApiService());
    }

    public AuthRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public void login(String phoneNumber, String password, @NonNull AuthCallback callback) {
        LoginRequest request = new LoginRequest(AuthValidator.normalizePhone(phoneNumber), password);
        apiService.login(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<JsonElement>> call, @NonNull Response<ApiResponse<JsonElement>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<JsonElement>> call, @NonNull Throwable throwable) {
                callback.onError(getFailureMessage(throwable));
            }
        });
    }

    public void register(String fullName, String phoneNumber, String password, @NonNull AuthCallback callback) {
        RegisterRequest request = new RegisterRequest(fullName.trim(), AuthValidator.normalizePhone(phoneNumber), password);
        apiService.register(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<JsonElement>> call, @NonNull Response<ApiResponse<JsonElement>> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<JsonElement>> call, @NonNull Throwable throwable) {
                callback.onError(getFailureMessage(throwable));
            }
        });
    }

    private void handleResponse(Response<ApiResponse<JsonElement>> response, AuthCallback callback) {
        ApiResponse<JsonElement> body = response.body();

        if (response.isSuccessful() && body != null && body.isSuccess()) {
            callback.onSuccess(extractData(body.getData()), resolveMessage(body.getMessage()));
            return;
        }

        callback.onError(resolveErrorMessage(response, body));
    }

    private String resolveErrorMessage(Response<ApiResponse<JsonElement>> response, ApiResponse<JsonElement> body) {
        if (body != null) {
            List<String> errors = body.getErrors();
            if (errors != null && !errors.isEmpty() && !TextUtils.isEmpty(errors.get(0))) {
                return errors.get(0);
            }

            if (!TextUtils.isEmpty(body.getMessage())) {
                return body.getMessage();
            }
        }

        try (ResponseBody errorBody = response.errorBody()) {
            if (errorBody != null) {
                String rawError = errorBody.string();
                if (!TextUtils.isEmpty(rawError)) {
                    String parsedMessage = extractMessageFromRawError(rawError);
                    if (!TextUtils.isEmpty(parsedMessage)) {
                        return parsedMessage;
                    }
                    return rawError;
                }
            }
        } catch (IOException ignored) {
            // Fallback to generic message below.
        }

        return "Không thể kết nối tới dịch vụ xác thực. Vui lòng thử lại.";
    }

    private String resolveMessage(String message) {
        return TextUtils.isEmpty(message) ? "Thao tác thành công." : message;
    }

    private String extractData(JsonElement data) {
        if (data == null || data.isJsonNull()) {
            return "";
        }

        if (data.isJsonPrimitive()) {
            return data.getAsString();
        }

        return data.toString();
    }

    private String getFailureMessage(Throwable throwable) {
        if (throwable == null || TextUtils.isEmpty(throwable.getMessage())) {
            return "Không thể kết nối tới dịch vụ xác thực. Vui lòng thử lại.";
        }
        return "Lỗi kết nối: " + throwable.getMessage();
    }

    private String extractMessageFromRawError(String rawError) {
        try {
            JsonElement jsonElement = new JsonParser().parse(rawError);
            if (!jsonElement.isJsonObject()) {
                return rawError;
            }

            JsonObject jsonObject = jsonElement.getAsJsonObject();

            if (jsonObject.has("message") && !jsonObject.get("message").isJsonNull()) {
                String message = jsonObject.get("message").getAsString();
                if (!TextUtils.isEmpty(message)) {
                    return message;
                }
            }

            if (jsonObject.has("errors") && jsonObject.get("errors").isJsonObject()) {
                JsonObject errorsObject = jsonObject.getAsJsonObject("errors");
                for (Map.Entry<String, JsonElement> entry : errorsObject.entrySet()) {
                    JsonElement value = entry.getValue();
                    if (value != null && value.isJsonArray() && value.getAsJsonArray().size() > 0) {
                        return value.getAsJsonArray().get(0).getAsString();
                    }
                }
            }

            if (jsonObject.has("title") && !jsonObject.get("title").isJsonNull()) {
                String title = jsonObject.get("title").getAsString();
                if (!TextUtils.isEmpty(title)) {
                    return title;
                }
            }
        } catch (Exception ignored) {
            // Keep original raw error fallback.
        }

        return rawError;
    }
}




