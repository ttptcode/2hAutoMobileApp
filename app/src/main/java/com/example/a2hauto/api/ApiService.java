package com.example.a2hauto.api;

import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.Listing;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("api/Listings")
    Call<ApiResponse<List<Listing>>> getListings();
}
