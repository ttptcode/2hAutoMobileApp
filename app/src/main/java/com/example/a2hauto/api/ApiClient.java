package com.example.a2hauto.api;

import android.content.Context;

import com.example.a2hauto.auth.AuthInterceptor;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    public static final String BASE_URL = "http://vehiclemarket.runasp.net/";
    private static Retrofit retrofit;

    private ApiClient() {
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    public static ApiService getApiService(Context context) {
        // Tạo OkHttpClient với AuthInterceptor để tự động thêm token
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(new AuthInterceptor(context));

        Retrofit retrofitWithAuth = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofitWithAuth.create(ApiService.class);
    }
}
