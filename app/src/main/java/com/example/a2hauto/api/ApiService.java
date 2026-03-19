package com.example.a2hauto.api;

import com.example.a2hauto.model.auth.LoginRequest;
import com.example.a2hauto.model.auth.RegisterRequest;
import com.example.a2hauto.model.Conversation;
import com.example.a2hauto.model.FavoriteItem;
import com.example.a2hauto.model.Message;
import com.example.a2hauto.model.SendMessageRequest;
import com.example.a2hauto.model.ToggleFavoriteRequest;
import com.google.gson.JsonElement;
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.Listing;
import com.example.a2hauto.model.ItemType;
import java.util.List;
import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Body;
import retrofit2.http.Query;


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

    @POST("api/Auth/login")
    Call<ApiResponse<JsonElement>> login(@Body LoginRequest request);

    @POST("api/Auth/register")
    Call<ApiResponse<JsonElement>> register(@Body RegisterRequest request);

    @GET("api/Favorites/user/{userId}")
    Call<ApiResponse<List<FavoriteItem>>> getFavoritesByUser(@Path("userId") String userId);

    @POST("api/Favorites/toggle")
    Call<ApiResponse<JsonElement>> toggleFavorite(@Body ToggleFavoriteRequest request);

    @GET("api/Conversations/{userId}")
    Call<ApiResponse<List<Conversation>>> getConversations(
            @Header("Authorization") String authorization,
            @Path("userId") String userId
    );

    @POST("api/Conversations/create")
    Call<ApiResponse<Conversation>> createConversation(
            @Header("Authorization") String authorization,
            @Query("listingId") String listingId,
            @Query("buyerId") String buyerId
    );

    @GET("api/Messages/{conversationId}")
    Call<ApiResponse<List<Message>>> getMessages(
            @Header("Authorization") String authorization,
            @Path("conversationId") String conversationId
    );

    @GET("api/Messages/incoming")
    Call<ApiResponse<List<Message>>> getIncomingMessages(
            @Header("Authorization") String authorization
    );

    @POST("api/Messages")
    Call<ApiResponse<Message>> sendMessage(
            @Header("Authorization") String authorization,
            @Body SendMessageRequest request
    );
}
