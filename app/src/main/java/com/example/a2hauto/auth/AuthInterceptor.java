package com.example.a2hauto.auth;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Interceptor để tự động thêm JWT token vào header Authorization của mọi API request
 * Lấy token từ AuthSessionManager
 */
public class AuthInterceptor implements Interceptor {
    private static final String TAG = "AuthInterceptor";
    private final AuthSessionManager authSessionManager;

    public AuthInterceptor(Context context) {
        this.authSessionManager = new AuthSessionManager(context);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // Lấy token từ AuthSessionManager
        String token = authSessionManager.getAuthToken();
        
        Log.d(TAG, "Intercepting request to: " + originalRequest.url());
        Log.d(TAG, "Token available: " + (!TextUtils.isEmpty(token)));
        
        if (!TextUtils.isEmpty(token)) {
            Log.d(TAG, "Token length: " + token.length());
            Log.d(TAG, "Token first 50 chars: " + token.substring(0, Math.min(50, token.length())));
            
            Request authorizedRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
            
            Log.d(TAG, "Authorization header added: Bearer " + token.substring(0, Math.min(20, token.length())) + "...");
            return chain.proceed(authorizedRequest);
        }

        Log.d(TAG, "No token found! Sending request without Authorization header");
        return chain.proceed(originalRequest);
    }
}

