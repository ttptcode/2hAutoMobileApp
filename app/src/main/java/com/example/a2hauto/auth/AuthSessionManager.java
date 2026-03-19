package com.example.a2hauto.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.a2hauto.R;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
<<<<<<< feature/chat_V3

import java.util.regex.Matcher;
import java.util.regex.Pattern;
=======
>>>>>>> main

public class AuthSessionManager {

    private static final String PREFS_NAME = "a2h_auto_auth";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_PENDING_FULL_NAME = "pending_full_name";
    private static final String KEY_PENDING_PHONE = "pending_phone";
    private static final Pattern JWT_PATTERN = Pattern.compile("([A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+)");

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
        String normalizedToken = normalizeAuthToken(authToken);
        sharedPreferences.edit()
                .putString(KEY_FULL_NAME, sanitizedName)
                .putString(KEY_PHONE, normalizedPhone)
<<<<<<< feature/chat_V3
                .putString(KEY_AUTH_TOKEN, normalizedToken)
=======
                .putString(KEY_AUTH_TOKEN, normalizeAuthToken(authToken))
>>>>>>> main
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
<<<<<<< feature/chat_V3
        String rawToken = sharedPreferences.getString(KEY_AUTH_TOKEN, "");
        String normalizedToken = normalizeAuthToken(rawToken);
        if (!TextUtils.equals(rawToken, normalizedToken)) {
            sharedPreferences.edit().putString(KEY_AUTH_TOKEN, normalizedToken).apply();
        }
        return normalizedToken;
=======
        return normalizeAuthToken(sharedPreferences.getString(KEY_AUTH_TOKEN, ""));
>>>>>>> main
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

<<<<<<< feature/chat_V3
        String directJwt = extractJwtCandidate(trimmed);
        if (!TextUtils.isEmpty(directJwt)) {
            return directJwt;
        }

        try {
            // Parse JSON-like auth payloads first (e.g. {"token":"..."}) before dot checks.
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                JsonElement element = new JsonParser().parse(trimmed);
                String nestedToken = findToken(element);
                if (!TextUtils.isEmpty(nestedToken)) {
                    return normalizeAuthToken(nestedToken);
                }
            }

            if (trimmed.contains(".")) {
                return trimmed;
            }

            JsonElement element = new JsonParser().parse(trimmed);
            String nestedToken = findToken(element);
            if (!TextUtils.isEmpty(nestedToken)) {
                return normalizeAuthToken(nestedToken);
            }

            String fallbackJwt = extractJwtCandidate(trimmed);
            return TextUtils.isEmpty(fallbackJwt) ? trimmed : fallbackJwt;
        } catch (Exception ignored) {
            String fallbackJwt = extractJwtCandidate(trimmed);
            return TextUtils.isEmpty(fallbackJwt) ? trimmed : fallbackJwt;
=======
        if (trimmed.contains(".")) {
            return trimmed;
        }

        try {
            JsonElement element = new JsonParser().parse(trimmed);
            return findToken(element);
        } catch (Exception ignored) {
            return trimmed;
>>>>>>> main
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
<<<<<<< feature/chat_V3

    private String extractJwtCandidate(String input) {
        if (TextUtils.isEmpty(input)) {
            return "";
        }

        Matcher matcher = JWT_PATTERN.matcher(input.trim());
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }
=======
>>>>>>> main
}



