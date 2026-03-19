package com.example.a2hauto.util;

import android.content.Context;
import android.util.Log;

import com.example.a2hauto.auth.AuthSessionManager;
import com.example.a2hauto.auth.JwtUtils;

/**
 * Utility class để debug authentication issues
 */
public class AuthDebugger {
    private static final String TAG = "AuthDebugger";

    public static void debugAuthStatus(Context context) {
        AuthSessionManager authSessionManager = new AuthSessionManager(context);
        
        Log.d(TAG, "=== AUTH DEBUG INFO ===");
        Log.d(TAG, "Is Logged In: " + authSessionManager.isLoggedIn());
        Log.d(TAG, "Display Name: " + authSessionManager.getDisplayName());
        Log.d(TAG, "Phone Number: " + authSessionManager.getPhoneNumber());
        
        String token = authSessionManager.getAuthToken();
        Log.d(TAG, "Token exists: " + (token != null && !token.isEmpty()));
        
        if (token != null && !token.isEmpty()) {
            Log.d(TAG, "Token length: " + token.length());
            Log.d(TAG, "Token first 50 chars: " + token.substring(0, Math.min(50, token.length())));
            
            // Kiểm tra token parts
            String[] parts = token.split("\\.");
            Log.d(TAG, "Token parts count: " + parts.length);
            
            if (parts.length == 3) {
                Log.d(TAG, "Header length: " + parts[0].length());
                Log.d(TAG, "Payload length: " + parts[1].length());
                Log.d(TAG, "Signature length: " + parts[2].length());
            }
            
            String displayName = JwtUtils.extractDisplayName(token);
            Log.d(TAG, "Extracted Display Name from token: " + displayName);
            
            String userId = JwtUtils.extractUserId(token);
            Log.d(TAG, "Extracted User ID from token: " + userId);
        } else {
            Log.w(TAG, "❌ NO TOKEN FOUND! User probably not logged in yet");
        }
        
        Log.d(TAG, "=== END DEBUG INFO ===");
    }
}

