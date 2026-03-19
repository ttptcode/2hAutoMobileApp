package com.example.a2hauto;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a2hauto.adapter.FavoritesAdapter;
import com.example.a2hauto.api.ApiClient;
import com.example.a2hauto.api.ApiService;
import com.example.a2hauto.auth.AuthSessionManager;
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.FavoriteItem;
import com.example.a2hauto.model.Listing;
import com.example.a2hauto.model.ToggleFavoriteRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView rvFavorites;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private TextView tvSelectionCount;
    private MaterialButton btnToggleSelectionMode;
    private MaterialButton btnDeleteSelected;

    private FavoritesAdapter favoritesAdapter;
    private ApiService apiService;
    private AuthSessionManager authSessionManager;

    private boolean selectionMode = false;
    private final Set<String> selectedListingIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        apiService = ApiClient.getApiService();
        authSessionManager = new AuthSessionManager(this);

        rvFavorites = findViewById(R.id.rvFavorites);
        progressBar = findViewById(R.id.progressBarFavorites);
        tvEmptyState = findViewById(R.id.tvEmptyFavorites);
        tvSelectionCount = findViewById(R.id.tvSelectionCount);
        btnToggleSelectionMode = findViewById(R.id.btnToggleSelectionMode);
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected);

        findViewById(R.id.btnBackFavorites).setOnClickListener(v -> finish());

        rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        favoritesAdapter = new FavoritesAdapter(new FavoritesAdapter.FavoriteActionListener() {
            @Override
            public void onSelectionChanged(int selectedCount) {
                selectedListingIds.clear();
                selectedListingIds.addAll(favoritesAdapter.getSelectedListingIds());
                bindSelectionUi();
            }

            @Override
            public void onFavoriteRemoved(String listingId) {
                toggleFavoriteForSingleListing(listingId);
            }
        });
        rvFavorites.setAdapter(favoritesAdapter);

        btnToggleSelectionMode.setOnClickListener(v -> {
            selectionMode = !selectionMode;
            favoritesAdapter.setSelectionMode(selectionMode);
            if (!selectionMode) {
                selectedListingIds.clear();
            }
            bindSelectionUi();
        });

        btnDeleteSelected.setOnClickListener(v -> {
            int selectedCount = favoritesAdapter.getSelectedListingIds().size();
            if (selectedCount <= 0) {
            return;
            }

            new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa " + selectedCount
                    + " sản phẩm đã chọn khỏi danh sách yêu thích không?")
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Xóa", (dialog, which) -> deleteSelectedFavorites())
                .show();
        });

        bindSelectionUi();
        fetchFavorites();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchFavorites();
    }

    private void bindSelectionUi() {
        tvSelectionCount.setText(getString(R.string.favorite_selected_count, selectedListingIds.size()));
        btnDeleteSelected.setEnabled(!selectedListingIds.isEmpty());
        btnDeleteSelected.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        btnToggleSelectionMode.setText(selectionMode
                ? getString(R.string.favorite_selection_done)
                : getString(R.string.favorite_selection_mode));
    }

    private void fetchFavorites() {
        String userId = authSessionManager.getUserId();
        if (TextUtils.isEmpty(userId)) {
            progressBar.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText(getString(R.string.favorite_login_required));
            favoritesAdapter.setListings(new ArrayList<>());
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);

        apiService.getFavoritesByUser(userId).enqueue(new Callback<ApiResponse<List<FavoriteItem>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<FavoriteItem>>> call,
                                   @NonNull Response<ApiResponse<List<FavoriteItem>>> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    progressBar.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.VISIBLE);
                    tvEmptyState.setText(getString(R.string.favorite_load_failed));
                    return;
                }

                List<FavoriteItem> favoriteItems = response.body().getData() == null
                        ? new ArrayList<>()
                        : response.body().getData();

                Set<String> favoriteListingIds = favoriteItems.stream()
                        .map(FavoriteItem::getListingId)
                        .filter(id -> !TextUtils.isEmpty(id))
                        .collect(Collectors.toSet());

                if (favoriteListingIds.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.VISIBLE);
                    tvEmptyState.setText(getString(R.string.favorite_empty));
                    favoritesAdapter.setListings(new ArrayList<>());
                    return;
                }

                fetchFavoriteListings(favoriteListingIds);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<FavoriteItem>>> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText(getString(R.string.favorite_load_failed));
                Toast.makeText(FavoritesActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchFavoriteListings(Set<String> favoriteListingIds) {
        apiService.getListings().enqueue(new Callback<ApiResponse<List<Listing>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Listing>>> call,
                                   @NonNull Response<ApiResponse<List<Listing>>> response) {
                progressBar.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()
                        || response.body().getData() == null) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    tvEmptyState.setText(getString(R.string.favorite_load_failed));
                    return;
                }

                List<Listing> filteredFavorites = response.body().getData().stream()
                        .filter(item -> !TextUtils.isEmpty(item.getListingId()) && favoriteListingIds.contains(item.getListingId()))
                        .collect(Collectors.toList());

                favoritesAdapter.setListings(filteredFavorites);
                tvEmptyState.setVisibility(filteredFavorites.isEmpty() ? View.VISIBLE : View.GONE);
                if (filteredFavorites.isEmpty()) {
                    tvEmptyState.setText(getString(R.string.favorite_empty));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Listing>>> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText(getString(R.string.favorite_load_failed));
                Toast.makeText(FavoritesActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleFavoriteForSingleListing(String listingId) {
        if (TextUtils.isEmpty(listingId)) {
            return;
        }

        String userId = authSessionManager.getUserId();
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, R.string.favorite_login_required, Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.toggleFavorite(new ToggleFavoriteRequest(userId, listingId))
                .enqueue(new Callback<ApiResponse<JsonElement>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<JsonElement>> call,
                                           @NonNull Response<ApiResponse<JsonElement>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Set<String> removed = new HashSet<>();
                            removed.add(listingId);
                            favoritesAdapter.removeListingIds(removed);
                            Toast.makeText(FavoritesActivity.this, R.string.favorite_removed, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(FavoritesActivity.this, R.string.favorite_action_failed, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<JsonElement>> call, @NonNull Throwable t) {
                        Toast.makeText(FavoritesActivity.this, R.string.favorite_action_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteSelectedFavorites() {
        if (selectedListingIds.isEmpty()) {
            return;
        }

        String userId = authSessionManager.getUserId();
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, R.string.favorite_login_required, Toast.LENGTH_SHORT).show();
            return;
        }

        btnDeleteSelected.setEnabled(false);

        List<String> targets = new ArrayList<>(selectedListingIds);
        Set<String> removedIds = new HashSet<>();
        final int[] pendingCount = {targets.size()};

        for (String listingId : targets) {
            apiService.toggleFavorite(new ToggleFavoriteRequest(userId, listingId))
                    .enqueue(new Callback<ApiResponse<JsonElement>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<JsonElement>> call,
                                               @NonNull Response<ApiResponse<JsonElement>> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                removedIds.add(listingId);
                            }
                            finishDeleteSelected(removedIds, pendingCount);
                        }

                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<JsonElement>> call, @NonNull Throwable t) {
                            finishDeleteSelected(removedIds, pendingCount);
                        }
                    });
        }
    }

    private void finishDeleteSelected(Set<String> removedIds, int[] pendingCount) {
        pendingCount[0]--;
        if (pendingCount[0] > 0) {
            return;
        }

        btnDeleteSelected.setEnabled(true);

        if (removedIds.isEmpty()) {
            Toast.makeText(this, R.string.favorite_action_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        favoritesAdapter.removeListingIds(removedIds);
        selectedListingIds.removeAll(removedIds);
        favoritesAdapter.clearSelection();
        selectionMode = false;
        favoritesAdapter.setSelectionMode(false);
        bindSelectionUi();

        Toast.makeText(this, getString(R.string.favorite_deleted_count, removedIds.size()), Toast.LENGTH_SHORT).show();
    }
}
