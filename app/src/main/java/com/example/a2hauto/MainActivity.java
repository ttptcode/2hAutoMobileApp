package com.example.a2hauto;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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
import com.example.a2hauto.model.Listing;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
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
    private View miniHeaderCard;
    private AppBarLayout appBarLayout;
    private boolean isMiniHeaderVisible;
    private AuthSessionManager authSessionManager;
    private ApiService apiService;

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
        miniHeaderCard = findViewById(R.id.miniHeaderCard);
        appBarLayout = findViewById(R.id.appBarLayout);
        authSessionManager = new AuthSessionManager(this);

        rvVehicles.setLayoutManager(new LinearLayoutManager(this));
        rvVehicles.setHasFixedSize(true);
        adapter = new VehicleAdapter(new ArrayList<>());
        rvVehicles.setAdapter(adapter);

        setupActions();
        setupMiniHeaderBehavior();
        initRetrofit();
        fetchListings();
    }

    private void setupActions() {
        findViewById(R.id.btnMenu).setOnClickListener(v -> showComingSoon(getString(R.string.menu)));
        findViewById(R.id.btnHeaderFavorite).setOnClickListener(v -> showComingSoon(getString(R.string.nav_favorites)));
        findViewById(R.id.btnMiniMenu).setOnClickListener(v -> showComingSoon(getString(R.string.menu)));
        findViewById(R.id.btnMiniFavorite).setOnClickListener(v -> showComingSoon(getString(R.string.nav_favorites)));
        findViewById(R.id.miniSearchBar).setOnClickListener(v -> showComingSoon(getString(R.string.search_hint)));
        findViewById(R.id.navHome).setOnClickListener(v -> rvVehicles.smoothScrollToPosition(0));
        findViewById(R.id.navFavorites).setOnClickListener(v -> showComingSoon(getString(R.string.nav_favorites)));
        findViewById(R.id.navAccount).setOnClickListener(v -> handleAccountAction());
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

        isMiniHeaderVisible = progress > 0.01f;
        miniHeaderCard.setVisibility(isMiniHeaderVisible ? View.VISIBLE : View.INVISIBLE);
        miniHeaderCard.setAlpha(progress);
        miniHeaderCard.setTranslationY((1f - progress) * -20f);
    }

    private void handleAccountAction() {
        if (authSessionManager.isLoggedIn()) {
            showAccountDialog();
            return;
        }

        showLoginDialog();
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

    private void showAccountDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.account_dialog_title)
                .setMessage(getString(
                        R.string.account_dialog_message,
                        authSessionManager.getDisplayName(),
                        authSessionManager.getPhoneNumber()
                ))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_logout, (dialog, which) -> {
                    authSessionManager.logout();
                    Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();
                })
                .show();
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
        tvListingCount.setText(getString(R.string.listing_count_format, activeListings.size()));
        tvMiniListingCount.setText(getString(R.string.listing_count_format, activeListings.size()));
        tvSectionSubtitle.setText(getString(R.string.featured_section_subtitle));

        boolean isEmpty = activeListings.isEmpty();
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvVehicles.setVisibility(isEmpty ? View.INVISIBLE : View.VISIBLE);

    }

    @Override
    public void onLoginSuccess(String displayName) {
        // Auth dialog already shows success feedback.
    }

    @Override
    public void onOpenRegisterRequested() {
        showRegisterDialog();
    }

    @Override
    public void onRegisterSuccess(String displayName) {
        // Auth dialog already shows success feedback.
    }

    @Override
    public void onOpenLoginRequested() {
        showLoginDialog();
    }
}
