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
                    "userId",
                    "userid",
                    "id",
                    "Id",
                    "sub",
                    "Subject",
                    "nameid",
                    "NameIdentifier",
                    "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier"
            };

            for (String key : candidateKeys) {
                String value = getClaimValue(jsonObject, key);
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        } catch (Exception ignored) {
            // Ignore malformed token content and fallback elsewhere.
        }

        return "";
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

    private static String getClaimValue(JsonObject object, String key) {
        if (object == null || key == null || key.isEmpty()) {
            return "";
        }

        if (object.has(key) && !object.get(key).isJsonNull()) {
            try {
                return object.get(key).getAsString();
            } catch (Exception ignored) {
                return "";
            }
        }

        for (String candidateKey : object.keySet()) {
            if (candidateKey == null) {
                continue;
            }

            String normalizedCandidate = candidateKey.toLowerCase();
            String normalizedTarget = key.toLowerCase();
            boolean isSame = normalizedCandidate.equals(normalizedTarget);
            boolean isUriSuffixMatch = normalizedCandidate.endsWith("/" + normalizedTarget);
            if (!isSame && !isUriSuffixMatch) {
                continue;
            }

            try {
                JsonElement element = object.get(candidateKey);
                if (element != null && !element.isJsonNull()) {
                    return element.getAsString();
                }
            } catch (Exception ignored) {
                return "";
            }
        }

        return "";
    }
}


