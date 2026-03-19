package com.example.a2hauto;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a2hauto.adapter.VehicleAdapter;
import com.example.a2hauto.api.ApiClient;
import com.example.a2hauto.api.ApiService;
import com.example.a2hauto.auth.AuthSessionManager;
import com.example.a2hauto.auth.LoginDialogFragment;
import com.example.a2hauto.auth.RegisterDialogFragment;
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.FavoriteItem;
import com.example.a2hauto.model.Listing;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements LoginDialogFragment.LoginDialogListener, RegisterDialogFragment.RegisterDialogListener {

    private static final String TAG = "MainActivity";
    private static final float MINI_HEADER_FADE_START = 0.38f;
    private static final float MINI_HEADER_FADE_END = 0.72f;
    private RecyclerView rvVehicles;
    private VehicleAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvListingCount;
    private TextView tvMiniListingCount;
    private TextView tvSectionSubtitle;
    private TextView tvEmptyState;
    private View btnHeaderUpgrade;
    private View btnHeaderLogin;
    private View btnMiniHeaderLogin;
    private TextView tvHeaderAvatar;
    private TextView tvMiniHeaderAvatar;
    private ImageView ivNavAccountIcon;
    private TextView tvNavAccountLabel;
    private View miniHeaderCard;
    private AppBarLayout appBarLayout;
    private AuthSessionManager authSessionManager;
    private ApiService apiService;
    private final Set<String> cachedFavoriteListingIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvVehicles = findViewById(R.id.rvVehicles);
        progressBar = findViewById(R.id.progressBar);
        tvListingCount = findViewById(R.id.tvListingCount);
        tvMiniListingCount = findViewById(R.id.tvMiniListingCount);
        tvSectionSubtitle = findViewById(R.id.tvSectionSubtitle);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnHeaderUpgrade = findViewById(R.id.btnHeaderUpgrade);
        btnHeaderLogin = findViewById(R.id.btnHeaderLogin);
        btnMiniHeaderLogin = findViewById(R.id.btnMiniHeaderLogin);
        tvHeaderAvatar = findViewById(R.id.tvHeaderAvatar);
        tvMiniHeaderAvatar = findViewById(R.id.tvMiniHeaderAvatar);
        ivNavAccountIcon = findViewById(R.id.ivNavAccountIcon);
        tvNavAccountLabel = findViewById(R.id.tvNavAccountLabel);
        miniHeaderCard = findViewById(R.id.miniHeaderCard);
        appBarLayout = findViewById(R.id.appBarLayout);
        authSessionManager = new AuthSessionManager(this);

        rvVehicles.setLayoutManager(new LinearLayoutManager(this));
        rvVehicles.setHasFixedSize(true);
        adapter = new VehicleAdapter(new ArrayList<>());
        rvVehicles.setAdapter(adapter);

        setupActions();
        setupMiniHeaderBehavior();
        refreshAuthHeaderUi();
        initRetrofit();
        fetchListings();
        syncFavoritesFromServer();
    }

    private void setupActions() {
        findViewById(R.id.btnMenu).setOnClickListener(v -> showCategoryMenuDialog());
        findViewById(R.id.btnHeaderFavorite).setOnClickListener(v -> openFavoritesScreen());
        findViewById(R.id.btnMiniMenu).setOnClickListener(v -> showCategoryMenuDialog());
        findViewById(R.id.btnMiniFavorite).setOnClickListener(v -> openFavoritesScreen());
        findViewById(R.id.miniSearchBar).setOnClickListener(v -> showComingSoon(getString(R.string.search_hint)));
        btnHeaderUpgrade.setOnClickListener(v -> showUpgradeDialog());
        btnHeaderLogin.setOnClickListener(v -> handleAccountAction());
        btnMiniHeaderLogin.setOnClickListener(v -> handleAccountAction());
        tvHeaderAvatar.setOnClickListener(v -> handleAccountAction());
        tvMiniHeaderAvatar.setOnClickListener(v -> handleAccountAction());
        findViewById(R.id.navHome).setOnClickListener(v -> rvVehicles.smoothScrollToPosition(0));
        findViewById(R.id.navFavorites).setOnClickListener(v -> openFavoritesScreen());
        findViewById(R.id.navChat).setOnClickListener(v -> showComingSoon(getString(R.string.nav_chat)));
        findViewById(R.id.navPost).setOnClickListener(v -> handlePostAction());
        findViewById(R.id.navAccount).setOnClickListener(v -> handleAccountAction());
    }


    @Override
    protected void onResume() {
        super.onResume();
        refreshAuthHeaderUi();
        syncFavoritesFromServer();
    }

    private void openFavoritesScreen() {
        if (!authSessionManager.isLoggedIn()) {
            showLoginDialog();
            return;
        }
        startActivity(new Intent(this, FavoritesActivity.class));
    }

    private void setupMiniHeaderBehavior() {
        updateMiniHeaderProgress(0f);
        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            if (appBarLayout.getTotalScrollRange() == 0) {
                return;
            }

            float collapseRatio = Math.abs(verticalOffset) / (float) appBarLayout.getTotalScrollRange();
            float progress = (collapseRatio - MINI_HEADER_FADE_START) / (MINI_HEADER_FADE_END - MINI_HEADER_FADE_START);
            updateMiniHeaderProgress(Math.max(0f, Math.min(1f, progress)));
        });
    }

    private void updateMiniHeaderProgress(float progress) {
        miniHeaderCard.animate().cancel();

        miniHeaderCard.setVisibility(progress > 0.01f ? View.VISIBLE : View.INVISIBLE);
        miniHeaderCard.setAlpha(progress);
        miniHeaderCard.setTranslationY((1f - progress) * -20f);
    }

    private void handleAccountAction() {
        if (!authSessionManager.isLoggedIn()) {
            showLoginDialog();
            return;
        }

        showAccountBottomSheet();
    }

    private void showAccountBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View contentView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_account_actions, findViewById(android.R.id.content), false);
        dialog.setContentView(contentView);

        View optionProfile = contentView.findViewById(R.id.optionProfile);
        View optionLogout = contentView.findViewById(R.id.optionLogout);

        optionProfile.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, ProfileActivity.class));
        });

        optionLogout.setOnClickListener(v -> {
            dialog.dismiss();
            authSessionManager.logout();
            refreshAuthHeaderUi();
            syncFavoritesFromServer();
            Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();
            showLoginDialog();
        });

        dialog.show();
    }

    private void showLoginDialog() {
        if (getSupportFragmentManager().findFragmentByTag(LoginDialogFragment.TAG) == null) {
            new LoginDialogFragment().show(getSupportFragmentManager(), LoginDialogFragment.TAG);
        }
    }

    private void showRegisterDialog() {
        if (getSupportFragmentManager().findFragmentByTag(RegisterDialogFragment.TAG) == null) {
            new RegisterDialogFragment().show(getSupportFragmentManager(), RegisterDialogFragment.TAG);
        }
    }

    private void showUpgradeDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_upgrade_freemium, null, false);
        new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_upgrade_now,
                        (dialog, which) -> showComingSoon(getString(R.string.action_upgrade)))
                .show();
    }

    private void handlePostAction() {
        if (!authSessionManager.isLoggedIn()) {
            showLoginDialog();
            return;
        }

        startActivity(new Intent(this, NewsListingsActivity.class));
    }

    private void showCategoryMenuDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_category_menu, null, false);
        LinearLayout optionContainer = dialogView.findViewById(R.id.menuOptionContainer);

        List<CategoryMenuItem> menuItems = new ArrayList<>();
        menuItems.add(new CategoryMenuItem(getString(R.string.menu_option_car), R.drawable.ic_menu_category_car));
        menuItems.add(new CategoryMenuItem(getString(R.string.menu_option_motorbike), R.drawable.ic_menu_category_motorbike));
        menuItems.add(new CategoryMenuItem(getString(R.string.menu_option_truck), R.drawable.ic_menu_category_truck));
        menuItems.add(new CategoryMenuItem(getString(R.string.menu_option_electric), R.drawable.ic_menu_category_electric));
        menuItems.add(new CategoryMenuItem(getString(R.string.menu_option_bicycle), R.drawable.ic_menu_category_bicycle));
        menuItems.add(new CategoryMenuItem(getString(R.string.menu_option_other_vehicle), android.R.drawable.ic_menu_mapmode));
        menuItems.add(new CategoryMenuItem(getString(R.string.menu_option_spare_parts), android.R.drawable.ic_menu_manage));

        final androidx.appcompat.app.AlertDialog[] menuDialogRef = new androidx.appcompat.app.AlertDialog[1];
        for (CategoryMenuItem item : menuItems) {
            View optionView = inflater.inflate(R.layout.item_menu_option, optionContainer, false);
            ImageView ivIcon = optionView.findViewById(R.id.ivMenuOptionIcon);
            TextView tvTitle = optionView.findViewById(R.id.tvMenuOptionTitle);

            ivIcon.setImageResource(item.iconResId);
            tvTitle.setText(item.title);
            optionView.setOnClickListener(v -> {
                showComingSoon(item.title);
                if (menuDialogRef[0] != null) {
                    menuDialogRef[0].dismiss();
                }
            });

            optionContainer.addView(optionView);
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();
        menuDialogRef[0] = dialog;
        dialog.show();
    }

    private static class CategoryMenuItem {
        private final String title;
        private final int iconResId;

        private CategoryMenuItem(String title, int iconResId) {
            this.title = title;
            this.iconResId = iconResId;
        }
    }

    private void refreshAuthHeaderUi() {
        boolean isLoggedIn = authSessionManager.isLoggedIn();
        String initials = getAvatarInitials(authSessionManager.getDisplayName(), authSessionManager.getPhoneNumber());

        btnHeaderLogin.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        btnMiniHeaderLogin.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        tvHeaderAvatar.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        tvMiniHeaderAvatar.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);

        tvHeaderAvatar.setText(initials);
        tvMiniHeaderAvatar.setText(initials);
        ivNavAccountIcon.setImageResource(R.drawable.ic_profile_outline);
        ivNavAccountIcon.setColorFilter(ContextCompat.getColor(this, isLoggedIn ? R.color.success_green : R.color.text_muted));
        tvNavAccountLabel.setTextColor(ContextCompat.getColor(this, isLoggedIn ? R.color.success_green : R.color.text_secondary));
    }

    private String getAvatarInitials(String displayName, String phoneNumber) {
        if (!TextUtils.isEmpty(displayName)) {
            String[] parts = displayName.trim().split("\\s+");
            if (parts.length >= 2) {
                return ("" + Character.toUpperCase(parts[0].charAt(0))
                        + Character.toUpperCase(parts[parts.length - 1].charAt(0)));
            }
            if (parts.length == 1 && !parts[0].isEmpty()) {
                return ("" + Character.toUpperCase(parts[0].charAt(0)));
            }
        }

        String normalizedPhone = phoneNumber == null ? "" : phoneNumber.replaceAll("\\D", "");
        if (normalizedPhone.length() >= 2) {
            return normalizedPhone.substring(normalizedPhone.length() - 2);
        }
        return "2H";
    }

    private void showComingSoon(String featureName) {
        Toast.makeText(this, getString(R.string.coming_soon, featureName), Toast.LENGTH_SHORT).show();
    }

    private void initRetrofit() {
        apiService = ApiClient.getApiService();
    }

    private void fetchListings() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        apiService.getListings().enqueue(new Callback<ApiResponse<List<Listing>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Listing>>> call, Response<ApiResponse<List<Listing>>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Listing>> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        // Filter listings with status "Active"
                        List<Listing> activeListings = apiResponse.getData().stream()
                                .filter(listing -> "Active".equalsIgnoreCase(listing.getStatus()))
                                .collect(Collectors.toList());

                        updateListingUi(activeListings);

                        if (activeListings.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Hiện không có bài đăng nào đang hoạt động", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        updateListingUi(new ArrayList<>());
                        Toast.makeText(MainActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    updateListingUi(new ArrayList<>());
                    Log.e(TAG, "Error: " + response.code());
                    Toast.makeText(MainActivity.this, "Không thể tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Listing>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                updateListingUi(new ArrayList<>());
                Log.e(TAG, "Failure: " + t.getMessage());
                Toast.makeText(MainActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateListingUi(List<Listing> activeListings) {
        adapter.setListings(activeListings);
        adapter.setFavoriteListingIds(cachedFavoriteListingIds);
        tvListingCount.setText(getString(R.string.listing_count_format, activeListings.size()));
        tvMiniListingCount.setText(getString(R.string.listing_count_format, activeListings.size()));
        tvSectionSubtitle.setText(getString(R.string.featured_section_subtitle));

        boolean isEmpty = activeListings.isEmpty();
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvVehicles.setVisibility(isEmpty ? View.INVISIBLE : View.VISIBLE);

    }

    private void syncFavoritesFromServer() {
        String userId = authSessionManager.getUserId();
        if (TextUtils.isEmpty(userId)) {
            if (!cachedFavoriteListingIds.isEmpty()) {
                for (String listingId : new HashSet<>(cachedFavoriteListingIds)) {
                    adapter.updateFavoriteState(listingId, false);
                }
                cachedFavoriteListingIds.clear();
            }
            return;
        }

        apiService.getFavoritesByUser(userId).enqueue(new Callback<ApiResponse<List<FavoriteItem>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<FavoriteItem>>> call,
                                   Response<ApiResponse<List<FavoriteItem>>> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    return;
                }

                List<FavoriteItem> favoriteItems = response.body().getData() == null
                        ? new ArrayList<>()
                        : response.body().getData();
                Set<String> latestFavoriteIds = favoriteItems.stream()
                        .map(FavoriteItem::getListingId)
                        .filter(id -> !TextUtils.isEmpty(id))
                        .collect(Collectors.toSet());

                Set<String> staleIds = new HashSet<>(cachedFavoriteListingIds);
                staleIds.removeAll(latestFavoriteIds);
                for (String listingId : staleIds) {
                    adapter.updateFavoriteState(listingId, false);
                }

                Set<String> addedIds = new HashSet<>(latestFavoriteIds);
                addedIds.removeAll(cachedFavoriteListingIds);
                for (String listingId : addedIds) {
                    adapter.updateFavoriteState(listingId, true);
                }

                cachedFavoriteListingIds.clear();
                cachedFavoriteListingIds.addAll(latestFavoriteIds);
                adapter.setFavoriteListingIds(cachedFavoriteListingIds);
            }

            @Override
            public void onFailure(Call<ApiResponse<List<FavoriteItem>>> call, Throwable t) {
                Log.w(TAG, "Favorite sync failed: " + t.getMessage());
            }
        });
    }

    @Override
    public void onLoginSuccess(String displayName) {
        refreshAuthHeaderUi();
    }

    @Override
    public void onOpenRegisterRequested() {
        showRegisterDialog();
    }

    @Override
    public void onRegisterSuccess(String displayName) {
        refreshAuthHeaderUi();
    }

    @Override
    public void onOpenLoginRequested() {
        showLoginDialog();
    }
}
