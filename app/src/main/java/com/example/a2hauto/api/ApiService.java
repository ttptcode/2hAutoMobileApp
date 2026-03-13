package com.example.a2hauto.api;

import com.example.a2hauto.model.auth.LoginRequest;
import com.example.a2hauto.model.auth.RegisterRequest;
import com.google.gson.JsonElement;
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.Listing;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @GET("api/Listings")
    Call<ApiResponse<List<Listing>>> getListings();

    @POST("api/Auth/login")
    Call<ApiResponse<JsonElement>> login(@Body LoginRequest request);

    @POST("api/Auth/register")
    Call<ApiResponse<JsonElement>> register(@Body RegisterRequest request);
}
