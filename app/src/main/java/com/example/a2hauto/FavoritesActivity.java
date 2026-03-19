package com.example.a2hauto;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a2hauto.adapter.FavoritesAdapter;
import com.example.a2hauto.api.ApiClient;
import com.example.a2hauto.api.ApiService;
import com.example.a2hauto.auth.AuthSessionManager;
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.FavoriteItem;
import com.example.a2hauto.model.FavoriteResponse;
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

public class FavoritesActivity extends AppCompatActivity implements FavoritesAdapter.FavoriteActionListener {

    private static final String TAG = "FavoritesActivity";

    private RecyclerView rvFavorites;
    private LinearLayout emptyStateContainer;
    private ProgressBar progressBar;
    private TextView tvFavoritesCount;
    private MaterialButton btnEditFavorites;
    private MaterialButton btnDeleteSelected;

    private ApiService apiService;
    private AuthSessionManager authSessionManager;
    private FavoritesAdapter adapter;

    private final List<Listing> favoriteListings = new ArrayList<>();
    private String userId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_favorites);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.favoritesRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initDependencies();
        bindActions();
        fetchFavoriteListings();
    }

    private void initViews() {
        rvFavorites = findViewById(R.id.rvFavorites);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        progressBar = findViewById(R.id.progressBarFavorites);
        tvFavoritesCount = findViewById(R.id.tvFavoritesCount);
        btnEditFavorites = findViewById(R.id.btnEditFavorites);
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected);

        rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        rvFavorites.setHasFixedSize(true);

        adapter = new FavoritesAdapter(new ArrayList<>(), this);
        rvFavorites.setAdapter(adapter);
    }

    private void initDependencies() {
        apiService = ApiClient.getApiService();
        authSessionManager = new AuthSessionManager(this);
        userId = authSessionManager.getUserId();
    }

    private void bindActions() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        MaterialButton btnExploreNow = findViewById(R.id.btnExploreNow);
        btnExploreNow.setOnClickListener(v -> finish());

        btnEditFavorites.setOnClickListener(v -> adapter.setSelectionMode(!adapter.isSelectionMode()));
        btnDeleteSelected.setOnClickListener(v -> showBulkDeleteDialog());
    }

    private void fetchFavoriteListings() {
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, R.string.favorite_userid_missing, Toast.LENGTH_SHORT).show();
            updateUiState(new ArrayList<>());
            return;
        }

        setLoading(true);
        apiService.getFavoritesByUser(userId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<FavoriteResponse> call, @NonNull Response<FavoriteResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setLoading(false);
                    updateUiState(new ArrayList<>());
                    Toast.makeText(FavoritesActivity.this, R.string.favorite_load_failed, Toast.LENGTH_SHORT).show();
                    return;
                }

                FavoriteResponse favoriteResponse = response.body();
                if (!favoriteResponse.isSuccess() || favoriteResponse.getData() == null) {
                    setLoading(false);
                    updateUiState(new ArrayList<>());
                    String message = TextUtils.isEmpty(favoriteResponse.getMessage())
                            ? getString(R.string.favorite_load_failed)
                            : favoriteResponse.getMessage();
                    Toast.makeText(FavoritesActivity.this, message, Toast.LENGTH_SHORT).show();
                    return;
                }

                Set<String> favoriteListingIds = favoriteResponse.getData().stream()
                        .map(FavoriteItem::getListingId)
                        .filter(id -> !TextUtils.isEmpty(id))
                        .collect(Collectors.toCollection(HashSet::new));

                if (favoriteListingIds.isEmpty()) {
                    setLoading(false);
                    updateUiState(new ArrayList<>());
                    return;
                }

                fetchListingsByIds(favoriteListingIds);
            }

            @Override
            public void onFailure(@NonNull Call<FavoriteResponse> call, @NonNull Throwable t) {
                setLoading(false);
                Log.e(TAG, "Favorites API error", t);
                updateUiState(new ArrayList<>());
                Toast.makeText(FavoritesActivity.this, getString(R.string.connection_error, t.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchListingsByIds(Set<String> favoriteListingIds) {
        apiService.getListings().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Listing>>> call, @NonNull Response<ApiResponse<List<Listing>>> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    updateUiState(new ArrayList<>());
                    Toast.makeText(FavoritesActivity.this, R.string.favorite_load_failed, Toast.LENGTH_SHORT).show();
                    return;
                }

                ApiResponse<List<Listing>> apiResponse = response.body();
                List<Listing> allListings = apiResponse.getData() == null ? new ArrayList<>() : apiResponse.getData();
                List<Listing> matchedFavorites = allListings.stream()
                        .filter(listing -> listing != null && !TextUtils.isEmpty(listing.getListingId()))
                        .filter(listing -> favoriteListingIds.contains(listing.getListingId()))
                        .collect(Collectors.toList());

                updateUiState(matchedFavorites);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Listing>>> call, @NonNull Throwable t) {
                setLoading(false);
                Log.e(TAG, "Listings API error", t);
                updateUiState(new ArrayList<>());
                Toast.makeText(FavoritesActivity.this, getString(R.string.connection_error, t.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onToggleFavorite(Listing listing) {
        if (listing == null || TextUtils.isEmpty(listing.getListingId()) || TextUtils.isEmpty(userId)) {
            Toast.makeText(this, R.string.favorite_toggle_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        ToggleFavoriteRequest request = new ToggleFavoriteRequest(userId, listing.getListingId());
        apiService.toggleFavorite(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<JsonElement>> call, @NonNull Response<ApiResponse<JsonElement>> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    Toast.makeText(FavoritesActivity.this, R.string.favorite_toggle_failed, Toast.LENGTH_SHORT).show();
                    return;
                }

                favoriteListings.remove(listing);
                updateUiState(new ArrayList<>(favoriteListings));
                Toast.makeText(FavoritesActivity.this, R.string.favorite_removed_success, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<JsonElement>> call, @NonNull Throwable t) {
                Log.e(TAG, "Toggle favorite failed", t);
                Toast.makeText(FavoritesActivity.this, getString(R.string.connection_error, t.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSelectionChanged(int selectedCount, boolean isSelectionMode) {
        btnEditFavorites.setText(isSelectionMode ? R.string.action_cancel : R.string.favorites_select_mode);
        btnDeleteSelected.setVisibility(selectedCount > 0 ? View.VISIBLE : View.GONE);
        btnDeleteSelected.setText(getString(R.string.favorites_delete_format, selectedCount));
    }

    private void showBulkDeleteDialog() {
        Set<String> selectedIds = adapter.getSelectedListingIds();
        if (selectedIds.isEmpty()) {
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_delete_favorites, null, false);
        TextView tvMessage = dialogView.findViewById(R.id.tvDeleteDialogMessage);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelDelete);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirmDelete);

        tvMessage.setText(getString(R.string.favorites_delete_dialog_message_count, selectedIds.size()));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            bulkDeleteFavorites(new ArrayList<>(selectedIds));
        });

        dialog.show();
    }

    private void bulkDeleteFavorites(List<String> selectedIds) {
        if (selectedIds.isEmpty()) {
            return;
        }

        setLoading(true);
        executeBulkToggleSequentially(selectedIds, 0, new HashSet<>(), 0);
    }

    private void executeBulkToggleSequentially(List<String> selectedIds, int index, Set<String> successIds, int failedCount) {
        if (index >= selectedIds.size()) {
            setLoading(false);
            if (!successIds.isEmpty()) {
                favoriteListings.removeIf(listing -> listing != null && successIds.contains(listing.getListingId()));
                updateUiState(new ArrayList<>(favoriteListings));
            }
            adapter.setSelectionMode(false);

            if (!successIds.isEmpty()) {
                Toast.makeText(this, getString(R.string.favorites_deleted_success_count, successIds.size()), Toast.LENGTH_SHORT).show();
            }
            if (failedCount > 0) {
                Toast.makeText(this, getString(R.string.favorites_deleted_failed_count, failedCount), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String listingId = selectedIds.get(index);
        ToggleFavoriteRequest request = new ToggleFavoriteRequest(userId, listingId);
        apiService.toggleFavorite(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<JsonElement>> call, @NonNull Response<ApiResponse<JsonElement>> response) {
                boolean success = response.isSuccessful() && response.body() != null && response.body().isSuccess();
                if (success) {
                    successIds.add(listingId);
                }
                executeBulkToggleSequentially(selectedIds, index + 1, successIds, success ? failedCount : failedCount + 1);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<JsonElement>> call, @NonNull Throwable t) {
                Log.e(TAG, "Bulk delete toggle failed", t);
                executeBulkToggleSequentially(selectedIds, index + 1, successIds, failedCount + 1);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (adapter != null && adapter.isSelectionMode()) {
            adapter.setSelectionMode(false);
            return;
        }
        super.onBackPressed();
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        rvFavorites.setVisibility(isLoading ? View.GONE : rvFavorites.getVisibility());
        emptyStateContainer.setVisibility(isLoading ? View.GONE : emptyStateContainer.getVisibility());
    }

    private void updateUiState(List<Listing> listings) {
        favoriteListings.clear();
        favoriteListings.addAll(listings);
        adapter.setListings(favoriteListings);

        boolean isEmpty = favoriteListings.isEmpty();
        rvFavorites.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyStateContainer.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        tvFavoritesCount.setText(getString(R.string.favorites_count_format, favoriteListings.size()));
    }
}
