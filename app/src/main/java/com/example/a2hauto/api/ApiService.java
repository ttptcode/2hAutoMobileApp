package com.example.a2hauto.api;

import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.Listing;
import com.example.a2hauto.model.ItemType;
import java.util.List;
import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;

public interface ApiService {
    @GET("api/Listings")
    Call<ApiResponse<List<Listing>>> getListings();

    @GET("api/ItemTypes")
    Call<ApiResponse<List<ItemType>>> getItemTypes();

    @Multipart
    @POST("api/Listings")
    Call<ApiResponse<Listing>> createListing(
            @PartMap Map<String, RequestBody> fields,
            @Part List<MultipartBody.Part> Images,
            @Part MultipartBody.Part Video
    );

    @PATCH("api/Listings/{listingId}/toggle-status")
    Call<ApiResponse<Void>> toggleStatus(@Path("listingId") String listingId);
}
