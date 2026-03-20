package com.example.a2hauto.api;

import com.example.a2hauto.model.auth.LoginRequest;
import com.example.a2hauto.model.auth.RegisterRequest;
import com.example.a2hauto.model.Conversation;
import com.example.a2hauto.model.FavoriteItem;
import com.example.a2hauto.model.Message;
import com.example.a2hauto.model.SendMessageRequest;
import com.example.a2hauto.model.ToggleFavoriteRequest;
import com.example.a2hauto.model.FeeCommission;
import com.example.a2hauto.model.FeeCommissionResponse;
import com.example.a2hauto.model.FavoriteItem;
import com.example.a2hauto.model.ToggleFavoriteRequest;
import com.example.a2hauto.model.UserPackage;
import com.example.a2hauto.model.UserProfileResponse;
import com.example.a2hauto.model.PaymentRequest;
import com.example.a2hauto.model.PaymentResponse;
import com.google.gson.JsonElement;
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.CreateReviewRequest;
import com.example.a2hauto.model.Listing;
import com.example.a2hauto.model.ItemType;
import java.util.List;
import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Body;
import retrofit2.http.Query;


public interface ApiService {
    @GET("api/Listings")
    Call<ApiResponse<List<Listing>>> getListings();

    @GET("api/Listings/by-user/{userId}")
    Call<ApiResponse<List<Listing>>> getListingsByUser(
            @Header("Authorization") String authorization,
            @Path("userId") String userId
    );

    @GET("api/Listings/{listingId}")
    Call<ApiResponse<Listing>> getListingById(
            @Header("Authorization") String authorization,
            @Path("listingId") String listingId
    );

    @Multipart
    @PUT("api/Listings/with-item")
    Call<ApiResponse<Listing>> updateListingWithItem(
            @Header("Authorization") String authorization,
            @PartMap Map<String, RequestBody> fields
    );

    @DELETE("api/Listings/{listingId}")
    Call<ApiResponse<Void>> deleteListing(
            @Header("Authorization") String authorization,
            @Path("listingId") String listingId
    );

    @GET("api/Conversations/{userId}")
    Call<ApiResponse<List<JsonElement>>> getConversations(
            @Header("Authorization") String authorization,
            @Path("userId") String userId
    );

    @GET("api/UserReputationReviews/listing/{listingId}")
    Call<ApiResponse<List<JsonElement>>> getReviewsByListingId(
            @Header("Authorization") String authorization,
            @Path("listingId") String listingId
    );

    @POST("api/UserReputationReviews")
    Call<ApiResponse<JsonElement>> createReview(
            @Header("Authorization") String authorization,
            @Body CreateReviewRequest request
    );

    @GET("api/ItemTypes")
    Call<ApiResponse<List<ItemType>>> getItemTypes();

    @Multipart
    @POST("api/Listings/with-item")
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
        @GET("api/UserPackages")
        Call<ApiResponse<List<FeeCommission>>> getUserPackages();

        @GET("api/FeeCommissions")
        Call<FeeCommissionResponse> getPackages();

        @GET("api/UserPackages/active")
        Call<ApiResponse<List<UserPackage>>> getActiveUserPackages();

    @GET("api/Users/{id}")
    Call<UserProfileResponse> getUserProfile(@Path("id") String userId);

    @POST("api/VNpay/create-payment")
    Call<ApiResponse<PaymentResponse>> createPayment(
            @Body PaymentRequest request
    );
}
