package com.example.a2hauto.chat;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.example.a2hauto.api.ApiService;
import com.example.a2hauto.auth.AuthSessionManager;
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.Conversation;
import com.example.a2hauto.model.Message;
import com.example.a2hauto.model.SendMessageRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatRepository {

    public interface RepositoryCallback<T> {
        void onSuccess(T data);

        void onError(String message);
    }

    private final ApiService apiService;
    private final AuthSessionManager authSessionManager;

    public ChatRepository(ApiService apiService, AuthSessionManager authSessionManager) {
        this.apiService = apiService;
        this.authSessionManager = authSessionManager;
    }

    public boolean isLoggedIn() {
        return authSessionManager.isLoggedIn()
                && !TextUtils.isEmpty(getAuthorizationHeader())
                && !TextUtils.isEmpty(getCurrentUserId());
    }

    public String getCurrentUserId() {
        return authSessionManager.getUserId();
    }

    public void getConversations(@NonNull RepositoryCallback<List<Conversation>> callback) {
        String auth = getAuthorizationHeader();
        String userId = getCurrentUserId();
        if (TextUtils.isEmpty(auth) || TextUtils.isEmpty(userId)) {
            callback.onError("Bạn cần đăng nhập để sử dụng chat.");
            return;
        }

        apiService.getConversations(auth, userId).enqueue(new Callback<ApiResponse<List<Conversation>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Conversation>>> call,
                                   @NonNull Response<ApiResponse<List<Conversation>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Conversation> data = response.body().getData();
                    List<Conversation> safeData = data == null ? new ArrayList<>() : new ArrayList<>(data);
                    safeData.sort((left, right) -> compareUpdatedAtDesc(left.getUpdatedAt(), right.getUpdatedAt()));
                    callback.onSuccess(safeData);
                    return;
                }

                callback.onError(resolveErrorMessage(response, "Không thể tải danh sách chat."));
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Conversation>>> call, @NonNull Throwable t) {
                callback.onError("Không thể kết nối máy chủ chat.");
            }
        });
    }

    public void createConversation(String listingId, String buyerId, @NonNull RepositoryCallback<Conversation> callback) {
        String auth = getAuthorizationHeader();
        if (TextUtils.isEmpty(auth) || TextUtils.isEmpty(listingId) || TextUtils.isEmpty(buyerId)) {
            callback.onError("Thiếu dữ liệu để tạo cuộc trò chuyện.");
            return;
        }

        apiService.createConversation(auth, listingId, buyerId).enqueue(new Callback<ApiResponse<Conversation>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Conversation>> call,
                                   @NonNull Response<ApiResponse<Conversation>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                    return;
                }

                callback.onError(resolveErrorMessage(response, "Không thể tạo cuộc trò chuyện."));
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Conversation>> call, @NonNull Throwable t) {
                callback.onError("Không thể kết nối máy chủ chat.");
            }
        });
    }

    public void getMessages(String conversationId, @NonNull RepositoryCallback<List<Message>> callback) {
        String auth = getAuthorizationHeader();
        if (TextUtils.isEmpty(auth) || TextUtils.isEmpty(conversationId)) {
            callback.onError("Không thể tải tin nhắn.");
            return;
        }

        apiService.getMessages(auth, conversationId).enqueue(new Callback<ApiResponse<List<Message>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Message>>> call,
                                   @NonNull Response<ApiResponse<List<Message>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Message> data = response.body().getData();
                    List<Message> safeData = data == null ? new ArrayList<>() : new ArrayList<>(data);
                    safeData.sort(Comparator.comparing(Message::getCreatedAt, Comparator.nullsLast(String::compareTo)));
                    callback.onSuccess(safeData);
                    return;
                }

                callback.onError(resolveErrorMessage(response, "Không thể tải nội dung chat."));
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Message>>> call, @NonNull Throwable t) {
                callback.onError("Không thể kết nối máy chủ chat.");
            }
        });
    }

    public void sendMessage(String conversationId, String content, @NonNull RepositoryCallback<Message> callback) {
        String auth = getAuthorizationHeader();
        String senderId = getCurrentUserId();
        if (TextUtils.isEmpty(auth) || TextUtils.isEmpty(senderId)
                || TextUtils.isEmpty(conversationId) || TextUtils.isEmpty(content)) {
            callback.onError("Tin nhắn không hợp lệ.");
            return;
        }

        SendMessageRequest request = new SendMessageRequest(conversationId, senderId, content);
        apiService.sendMessage(auth, request).enqueue(new Callback<ApiResponse<Message>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Message>> call,
                                   @NonNull Response<ApiResponse<Message>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                    return;
                }

                callback.onError(resolveErrorMessage(response, "Không thể gửi tin nhắn."));
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Message>> call, @NonNull Throwable t) {
                callback.onError("Không thể kết nối máy chủ chat.");
            }
        });
    }

    public void getIncomingUnread(@NonNull RepositoryCallback<List<Message>> callback) {
        String auth = getAuthorizationHeader();
        if (TextUtils.isEmpty(auth)) {
            callback.onSuccess(Collections.emptyList());
            return;
        }

        apiService.getIncomingMessages(auth).enqueue(new Callback<ApiResponse<List<Message>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Message>>> call,
                                   @NonNull Response<ApiResponse<List<Message>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Message> data = response.body().getData();
                    callback.onSuccess(data == null ? Collections.emptyList() : data);
                    return;
                }

                callback.onSuccess(Collections.emptyList());
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Message>>> call, @NonNull Throwable t) {
                callback.onSuccess(Collections.emptyList());
            }
        });
    }

    private String getAuthorizationHeader() {
        String token = authSessionManager.getAuthToken();
        if (TextUtils.isEmpty(token)) {
            return "";
        }

        String normalized = token.trim();
        if (normalized.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            normalized = normalized.substring(7).trim();
        }

        if (TextUtils.isEmpty(normalized)) {
            return "";
        }

        return "Bearer " + normalized;
    }

    private int compareUpdatedAtDesc(String left, String right) {
        String l = left == null ? "" : left;
        String r = right == null ? "" : right;
        return r.compareTo(l);
    }

    private <T> String resolveErrorMessage(Response<ApiResponse<T>> response, String fallback) {
        try {
            if (response.body() != null && !TextUtils.isEmpty(response.body().getMessage())) {
                return response.body().getMessage();
            }

            ResponseBody errorBody = response.errorBody();
            if (errorBody != null) {
                String raw = errorBody.string();
                if (!TextUtils.isEmpty(raw)) {
                    return raw;
                }
            }
        } catch (IOException ignored) {
            // Keep fallback for parse failures.
        }

        return fallback;
    }
}

