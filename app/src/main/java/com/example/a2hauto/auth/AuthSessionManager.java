package com.example.a2hauto.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.a2hauto.R;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AuthSessionManager {

    private static final String PREFS_NAME = "a2h_auto_auth";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_PENDING_FULL_NAME = "pending_full_name";
    private static final String KEY_PENDING_PHONE = "pending_phone";

    private final SharedPreferences sharedPreferences;
    private final Context appContext;

    public AuthSessionManager(Context context) {
        appContext = context.getApplicationContext();
        sharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
                && !TextUtils.isEmpty(sharedPreferences.getString(KEY_PHONE, ""));
    }

    public void saveSession(String fullName, String phone, String authToken) {
        String sanitizedName = fullName == null ? "" : fullName.trim();
        String normalizedPhone = AuthValidator.normalizePhone(phone);
        sharedPreferences.edit()
                .putString(KEY_FULL_NAME, sanitizedName)
                .putString(KEY_PHONE, normalizedPhone)
                .putString(KEY_AUTH_TOKEN, normalizeAuthToken(authToken))
                .remove(KEY_PENDING_FULL_NAME)
                .remove(KEY_PENDING_PHONE)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply();
    }

    public void savePendingRegistration(String fullName, String phone) {
        sharedPreferences.edit()
                .putString(KEY_PENDING_FULL_NAME, fullName == null ? "" : fullName.trim())
                .putString(KEY_PENDING_PHONE, AuthValidator.normalizePhone(phone))
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply();
    }

    public void logout() {
        sharedPreferences.edit().clear().apply();
    }

    public void clearToken() {
        sharedPreferences.edit()
                .remove(KEY_AUTH_TOKEN)
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply();
    }

    public String getDisplayName() {
        String displayName = sharedPreferences.getString(KEY_FULL_NAME, "");
        if (TextUtils.isEmpty(displayName)) {
            String phoneNumber = getPhoneNumber();
            if (!TextUtils.isEmpty(phoneNumber)) {
                return phoneNumber;
            }
            return appContext.getString(R.string.account_guest_name);
        }
        return displayName;
    }

    public String getPhoneNumber() {
        return sharedPreferences.getString(KEY_PHONE, "");
    }

    public String getAuthToken() {
        return normalizeAuthToken(sharedPreferences.getString(KEY_AUTH_TOKEN, ""));
    }

    public String getUserId() {
        return JwtUtils.extractUserId(getAuthToken());
    }

    public String getPendingFullName() {
        return sharedPreferences.getString(KEY_PENDING_FULL_NAME, "");
    }

    public String getPendingPhoneNumber() {
        return sharedPreferences.getString(KEY_PENDING_PHONE, "");
    }

    public void clearPendingRegistration() {
        sharedPreferences.edit()
                .remove(KEY_PENDING_FULL_NAME)
                .remove(KEY_PENDING_PHONE)
                .apply();
    }

    private String normalizeAuthToken(String rawToken) {
        if (TextUtils.isEmpty(rawToken)) {
            return "";
        }

        String trimmed = rawToken.trim();
        if (trimmed.toLowerCase().startsWith("bearer ")) {
            trimmed = trimmed.substring(7).trim();
        }

        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }

        if (trimmed.contains(".")) {
            return trimmed;
        }

        try {
            JsonElement element = new JsonParser().parse(trimmed);
            return findToken(element);
        } catch (Exception ignored) {
            return trimmed;
        }
    }

    private String findToken(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }

        if (element.isJsonPrimitive()) {
            String value = element.getAsString();
            return value == null ? "" : value.trim();
        }

        if (!element.isJsonObject()) {
            return "";
        }

        JsonObject object = element.getAsJsonObject();
        String[] tokenKeys = new String[] {
                "token",
                "accessToken",
                "access_token",
                "jwt",
                "jwtToken"
        };

        for (String key : tokenKeys) {
            if (object.has(key) && !object.get(key).isJsonNull()) {
                String value = findToken(object.get(key));
                if (!TextUtils.isEmpty(value)) {
                    return value;
                }
            }
        }

        if (object.has("data") && !object.get("data").isJsonNull()) {
            String value = findToken(object.get("data"));
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }

        return "";
    }
}



