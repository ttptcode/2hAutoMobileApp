package com.example.a2hauto.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class JwtUtils {

    private JwtUtils() {
    }

    public static String extractDisplayName(String token) {
        if (token == null || token.trim().isEmpty()) {
            return "";
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return "";
            }

            byte[] decodedPayload = Base64.getUrlDecoder().decode(addPadding(parts[1]));
            String payload = new String(decodedPayload, StandardCharsets.UTF_8);
            JsonElement element = new JsonParser().parse(payload);
            if (!element.isJsonObject()) {
                return "";
            }

            JsonObject jsonObject = element.getAsJsonObject();
            String[] candidateKeys = new String[] {
                    "fullName",
                    "name",
                    "unique_name",
                    "given_name",
                    "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name"
            };

            for (String key : candidateKeys) {
                if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
                    String value = jsonObject.get(key).getAsString();
                    if (value != null && !value.trim().isEmpty()) {
                        return value.trim();
                    }
                }
            }
        } catch (Exception ignored) {
            // Ignore malformed token content and fallback elsewhere.
        }

        return "";
    }

    public static String extractUserId(String token) {
        JsonObject jsonObject = parsePayload(token);
        if (jsonObject == null) {
            return "";
        }

        String[] candidateKeys = new String[] {
                "userId",
                "userid",
                "sub",
                "nameid",
                "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier"
        };

        for (String key : candidateKeys) {
            if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
                String value = jsonObject.get(key).getAsString();
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }

        return "";
    }

    private static JsonObject parsePayload(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }

            byte[] decodedPayload = Base64.getUrlDecoder().decode(addPadding(parts[1]));
            String payload = new String(decodedPayload, StandardCharsets.UTF_8);
            JsonElement element = new JsonParser().parse(payload);
            if (!element.isJsonObject()) {
                return null;
            }

            return element.getAsJsonObject();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String addPadding(String value) {
        int padding = value.length() % 4;
        if (padding == 2) {
            return value + "==";
        }
        if (padding == 3) {
            return value + "=";
        }
        if (padding == 1) {
            return value + "===";
        }
        return value;
    }
}


