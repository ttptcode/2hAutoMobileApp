package com.example.a2hauto;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
    
    // Navbar fields
    private LinearLayout navHome, navFavorites, navPost, navChat, navAccount;
    private int currentNavItem = 1; // Current position: 1=Favorites
    private int previousNavItem = 0; // Track previous position for smart transitions
    
    // Gesture detection
    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

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
        
        setupBottomNavigation();

        bindSelectionUi();
        fetchFavorites();
        setupGestureDetection();
    }
    
    private void setupBottomNavigation() {
        FrameLayout bottomNavContainer = findViewById(R.id.bottomNavContainer);
        if (bottomNavContainer != null) {
            View navView = getLayoutInflater().inflate(R.layout.bottom_navigation_bar, bottomNavContainer, true);
            
            navHome = navView.findViewById(R.id.navHome);
            navFavorites = navView.findViewById(R.id.navFavorites);
            navPost = navView.findViewById(R.id.navPost);
            navChat = navView.findViewById(R.id.navChat);
            navAccount = navView.findViewById(R.id.navAccount);
            
            if (navHome != null) {
                navHome.setOnClickListener(v -> {
                    // Favorites(1) → Home(0): right to left
                    previousNavItem = currentNavItem;
                    currentNavItem = 0;
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                });
                // Unhighlight navHome
                unhighlightNavItem(navHome);
            }
            
            if (navFavorites != null) {
                // Highlight navFavorites with icon and text color
                highlightNavItem(navFavorites);
            }
            
            if (navPost != null) {
                navPost.setOnClickListener(v -> {
                    // Favorites(1) → Post(2): left to right
                    previousNavItem = currentNavItem;
                    currentNavItem = 2;
                    startActivity(new Intent(this, NewsListingsActivity.class));
                    finish();
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                });
            }
            
            if (navChat != null) {
                navChat.setOnClickListener(v -> {
                    Toast.makeText(this, "Chức năng Chat sẽ sớm được bổ sung", Toast.LENGTH_SHORT).show();
                });
            }
            
            if (navAccount != null) {
                navAccount.setOnClickListener(v -> {
                    if (authSessionManager.isLoggedIn()) {
                        String accountMessage = getString(R.string.account_dialog_message, 
                            authSessionManager.getDisplayName(), 
                            authSessionManager.getPhoneNumber());
                        Toast.makeText(this, accountMessage, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Vui lòng đăng nhập để xem tài khoản", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
    
    private void highlightNavItem(LinearLayout navItem) {
        navItem.setBackgroundResource(R.drawable.bg_nav_active);
        
        // Update icon and text color for highlighted state
        for (int i = 0; i < navItem.getChildCount(); i++) {
            View child = navItem.getChildAt(i);
            if (child instanceof android.widget.ImageView) {
                ((android.widget.ImageView) child).setColorFilter(
                    androidx.core.content.ContextCompat.getColor(this, R.color.primary_teal_dark), 
                    android.graphics.PorterDuff.Mode.SRC_IN);
            } else if (child instanceof TextView) {
                ((TextView) child).setTextColor(
                    androidx.core.content.ContextCompat.getColor(this, R.color.primary_teal_dark));
                ((TextView) child).setTypeface(((TextView) child).getTypeface(), android.graphics.Typeface.BOLD);
            }
        }
    }
    
    private void unhighlightNavItem(LinearLayout navItem) {
        navItem.setBackground(null);
        
        // Reset icon and text color
        for (int i = 0; i < navItem.getChildCount(); i++) {
            View child = navItem.getChildAt(i);
            if (child instanceof android.widget.ImageView) {
                ((android.widget.ImageView) child).setColorFilter(
                    androidx.core.content.ContextCompat.getColor(this, R.color.text_muted), 
                    android.graphics.PorterDuff.Mode.SRC_IN);
            } else if (child instanceof TextView) {
                ((TextView) child).setTextColor(
                    androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
                ((TextView) child).setTypeface(((TextView) child).getTypeface(), android.graphics.Typeface.NORMAL);
            }
        }
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

    private void setupGestureDetection() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                try {
                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();
                    
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                // Swipe Right: Favorites(1) → Home(0)
                                onSwipeRight();
                            } else {
                                // Swipe Left: Favorites(1) → Post(2)
                                onSwipeLeft();
                            }
                            return true;
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return false;
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(event);
        }
        return super.dispatchTouchEvent(event);
    }

    private void onSwipeRight() {
        // Swipe Right: Favorites(1) → Home(0)
        previousNavItem = currentNavItem;
        currentNavItem = 0;
        startActivity(new Intent(this, MainActivity.class));
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void onSwipeLeft() {
        // Swipe Left: Favorites(1) → Post(2)
        previousNavItem = currentNavItem;
        currentNavItem = 2;
        startActivity(new Intent(this, NewsListingsActivity.class));
        finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
