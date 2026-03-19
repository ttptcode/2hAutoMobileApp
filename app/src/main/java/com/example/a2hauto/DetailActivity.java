package com.example.a2hauto;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.a2hauto.adapter.ReviewAdapter;
import com.example.a2hauto.adapter.VehicleAdapter;
import com.example.a2hauto.api.ApiClient;
import com.example.a2hauto.api.ApiService;
import com.example.a2hauto.auth.AuthSessionManager;
import com.example.a2hauto.auth.JwtUtils;
import com.example.a2hauto.auth.LoginDialogFragment;
import com.example.a2hauto.auth.RegisterDialogFragment;
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.CreateReviewRequest;
import com.example.a2hauto.model.Item;
import com.example.a2hauto.model.Listing;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailActivity extends AppCompatActivity implements
        LoginDialogFragment.LoginDialogListener,
        RegisterDialogFragment.RegisterDialogListener {

    private static final int SINGLE_COLUMN_BREAKPOINT_DP = 360;

    private ApiService apiService;
    private AuthSessionManager authSessionManager;

    private View btnHeaderUpgrade;
    private View btnHeaderLogin;
    private TextView tvHeaderAvatar;

    private TextView tvDetailSpecsLeft;
    private TextView tvDetailSpecsRight;
    private TextView tvRelatedEmpty;
    private ProgressBar progressRelated;
    private VehicleAdapter relatedAdapter;

    private RecyclerView rvReviews;
    private ProgressBar progressReviews;
    private TextView tvReviewEmpty;
    private TextView tvReviewSummary;
    private TextView tvRatingValue;
    private RatingBar ratingBarReview;
    private TextInputEditText etReviewComment;
    private MaterialButton btnSubmitReview;
    private ReviewAdapter reviewAdapter;

    private Listing currentListing;
    private String token;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        currentListing = (Listing) getIntent().getSerializableExtra("listing");
        if (currentListing == null) {
            finish();
            return;
        }

        apiService = ApiClient.getApiService();
        authSessionManager = new AuthSessionManager(this);

        token = sanitizeToken(authSessionManager.getAuthToken());
        currentUserId = JwtUtils.extractUserId(token);

        bindHeaderViews();
        setupHeaderActions();
        refreshAuthHeaderUi();
        
        setupSpecsViews();
        setupRelatedSection();
        setupReviewViews();

        bindListingData();
        
        fetchRelatedListings();
        loadReviews();
    }

    private void setupSpecsViews() {
        tvDetailSpecsLeft = findViewById(R.id.tvDetailSpecsLeft);
        tvDetailSpecsRight = findViewById(R.id.tvDetailSpecsRight);
    }

    private void bindListingData() {
        ImageView ivDetail = findViewById(R.id.ivDetail);
        TextView tvDetailName = findViewById(R.id.tvDetailName);
        TextView tvDetailPrice = findViewById(R.id.tvDetailPrice);
        TextView tvDetailDesc = findViewById(R.id.tvDetailDesc);
        TextView tvDetailSeller = findViewById(R.id.tvDetailSeller);

        tvDetailName.setText(currentListing.getDisplayTitle());
        
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvDetailPrice.setText(formatter.format(currentListing.getBuyNowPrice()));

        Item item = currentListing.getItem();
        bindSpecs(item);

        String detail = currentListing.getDetail();
        tvDetailDesc.setText(!TextUtils.isEmpty(detail)
                ? detail
                : getString(R.string.news_empty)); // Fallback from available strings

        String seller = !TextUtils.isEmpty(currentListing.getUserName())
                ? currentListing.getUserName()
                : getString(R.string.seller_fallback);
        String address = !TextUtils.isEmpty(currentListing.getAddress())
                ? currentListing.getAddress()
                : "Chưa cập nhật địa chỉ";
        tvDetailSeller.setText(String.format("Người bán: %s\nĐịa chỉ: %s", seller, address));

        String imageUrl = null;
        if (item != null && item.getImageUrls() != null && !item.getImageUrls().isEmpty()) {
            imageUrl = item.getImageUrls().get(0);
        }

        Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(ivDetail);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAuthHeaderUi();
        token = sanitizeToken(authSessionManager.getAuthToken());
        currentUserId = JwtUtils.extractUserId(token);
    }

    private void bindHeaderViews() {
        btnHeaderUpgrade = findViewById(R.id.btnHeaderUpgrade);
        btnHeaderLogin = findViewById(R.id.btnHeaderLogin);
        tvHeaderAvatar = findViewById(R.id.tvHeaderAvatar);
    }

    private void setupHeaderActions() {
        View backBtn = findViewById(R.id.btnDetailBackSmall);
        if (backBtn != null) backBtn.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        
        View menuBtn = findViewById(R.id.btnMenu);
        if (menuBtn != null) menuBtn.setOnClickListener(v -> showCategoryMenuDialog());
        
        View favBtn = findViewById(R.id.btnHeaderFavorite);
        if (favBtn != null) favBtn.setOnClickListener(v -> showComingSoon(getString(R.string.nav_favorites)));
        
        if (btnHeaderUpgrade != null) btnHeaderUpgrade.setOnClickListener(v -> showUpgradeDialog());
        if (btnHeaderLogin != null) btnHeaderLogin.setOnClickListener(v -> handleAccountAction());
        if (tvHeaderAvatar != null) tvHeaderAvatar.setOnClickListener(v -> handleAccountAction());
    }

    private void setupRelatedSection() {
        RecyclerView rvRelatedListings = findViewById(R.id.rvRelatedListings);
        tvRelatedEmpty = findViewById(R.id.tvRelatedEmpty);
        progressRelated = findViewById(R.id.progressRelated);

        if (rvRelatedListings != null) {
            rvRelatedListings.setLayoutManager(new LinearLayoutManager(this));
            rvRelatedListings.setNestedScrollingEnabled(false);
            relatedAdapter = new VehicleAdapter(new ArrayList<>());
            rvRelatedListings.setAdapter(relatedAdapter);
        }
    }

    private void bindSpecs(Item item) {
        if (tvDetailSpecsLeft == null) return;

        List<String> specRows = new ArrayList<>();
        appendSpec(specRows, "Loại", item != null ? item.getItemTypeName() : null);
        appendSpec(specRows, "Thương hiệu", item != null ? item.getBrand() : null);
        appendSpec(specRows, "Mẫu xe", item != null ? item.getModel() : null);
        appendSpec(specRows, "Năm", item != null && item.getYear() != null ? String.valueOf(item.getYear()) : null);
        appendSpec(specRows, "Tình trạng", item != null ? item.getCondition() : null);
        appendSpec(specRows, "Odo", item != null ? item.getMileage() : null);
        appendSpec(specRows, "Nhiên liệu", item != null ? item.getFuel() : null);
        appendSpec(specRows, "Hộp số", item != null ? item.getGearbox() : null);
        appendSpec(specRows, "Màu sắc", item != null ? item.getColor() : null);
        appendSpec(specRows, "Chỗ ngồi", item != null ? item.getSeat() : null);
        appendSpec(specRows, "Xuất xứ", item != null ? item.getOrigin() : null);
        appendSpec(specRows, "Biển số", item != null ? item.getLicensePlate() : null);

        if (specRows.isEmpty()) {
            tvDetailSpecsLeft.setText("Không có thông số kỹ thuật.");
            if (tvDetailSpecsRight != null) {
                tvDetailSpecsRight.setText("");
                tvDetailSpecsRight.setVisibility(View.GONE);
            }
            return;
        }

        if (shouldUseSingleColumnSpecs() || tvDetailSpecsRight == null) {
            tvDetailSpecsLeft.setText(TextUtils.join("\n", specRows));
            if (tvDetailSpecsRight != null) {
                tvDetailSpecsRight.setText("");
                tvDetailSpecsRight.setVisibility(View.GONE);
            }
            return;
        }

        int midPoint = (specRows.size() + 1) / 2;
        tvDetailSpecsLeft.setText(TextUtils.join("\n", specRows.subList(0, midPoint)));
        tvDetailSpecsRight.setText(TextUtils.join("\n", specRows.subList(midPoint, specRows.size())));
        tvDetailSpecsRight.setVisibility(View.VISIBLE);
    }

    private boolean shouldUseSingleColumnSpecs() {
        return getResources().getConfiguration().screenWidthDp < SINGLE_COLUMN_BREAKPOINT_DP;
    }

    private void appendSpec(List<String> specs, String label, String value) {
        if (TextUtils.isEmpty(value)) return;
        specs.add("- " + label + ": " + value);
    }

    private void handleAccountAction() {
        if (authSessionManager.isLoggedIn()) {
            showAccountDialog();
        } else {
            showLoginDialog();
        }
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
                .setNeutralButton(R.string.action_upgrade, (dialog, which) -> showUpgradeDialog())
                .setPositiveButton(R.string.action_logout, (dialog, which) -> {
                    authSessionManager.logout();
                    refreshAuthHeaderUi();
                    token = "";
                    currentUserId = "";
                    Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();
                    loadReviews(); // Refresh review UI to show login hint
                })
                .show();
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
                if (menuDialogRef[0] != null) menuDialogRef[0].dismiss();
            });
            optionContainer.addView(optionView);
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();
        menuDialogRef[0] = dialog;
        dialog.show();
    }

    private void refreshAuthHeaderUi() {
        boolean isLoggedIn = authSessionManager.isLoggedIn();
        String initials = getAvatarInitials(authSessionManager.getDisplayName(), authSessionManager.getPhoneNumber());

        if (btnHeaderLogin != null) btnHeaderLogin.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        if (tvHeaderAvatar != null) {
            tvHeaderAvatar.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
            tvHeaderAvatar.setText(initials);
        }
    }

    private String getAvatarInitials(String displayName, String phoneNumber) {
        if (!TextUtils.isEmpty(displayName)) {
            String[] parts = displayName.trim().split("\\s+");
            if (parts.length >= 2) {
                return "" + Character.toUpperCase(parts[0].charAt(0))
                        + Character.toUpperCase(parts[parts.length - 1].charAt(0));
            }
            if (parts.length == 1 && !parts[0].isEmpty()) {
                return "" + Character.toUpperCase(parts[0].charAt(0));
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

    private void fetchRelatedListings() {
        if (progressRelated != null) progressRelated.setVisibility(ProgressBar.VISIBLE);
        if (tvRelatedEmpty != null) tvRelatedEmpty.setVisibility(TextView.GONE);

        apiService.getListings().enqueue(new Callback<ApiResponse<List<Listing>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Listing>>> call, Response<ApiResponse<List<Listing>>> response) {
                if (progressRelated != null) progressRelated.setVisibility(ProgressBar.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null) {
                    List<Listing> filtered = buildRelatedListings(response.body().getData());
                    if (relatedAdapter != null) relatedAdapter.setListings(filtered);
                    if (tvRelatedEmpty != null) tvRelatedEmpty.setVisibility(filtered.isEmpty() ? TextView.VISIBLE : TextView.GONE);
                } else {
                    showRelatedEmpty();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Listing>>> call, Throwable t) {
                if (progressRelated != null) progressRelated.setVisibility(ProgressBar.GONE);
                showRelatedEmpty();
            }
        });
    }

    private void setupReviewViews() {
        rvReviews = findViewById(R.id.rvReviews);
        progressReviews = findViewById(R.id.progressReviews);
        tvReviewEmpty = findViewById(R.id.tvReviewEmpty);
        tvReviewSummary = findViewById(R.id.tvReviewSummary);
        tvRatingValue = findViewById(R.id.tvRatingValue);
        ratingBarReview = findViewById(R.id.ratingBarReview);
        etReviewComment = findViewById(R.id.etReviewComment);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);

        if (rvReviews != null) {
            rvReviews.setLayoutManager(new LinearLayoutManager(this));
            reviewAdapter = new ReviewAdapter(new ArrayList<>());
            rvReviews.setAdapter(reviewAdapter);
        }

        if (ratingBarReview != null && tvRatingValue != null) {
            tvRatingValue.setText(getString(R.string.review_rating_selected, (int) ratingBarReview.getRating()));
            ratingBarReview.setOnRatingBarChangeListener((ratingBar, rating, fromUser) ->
                    tvRatingValue.setText(getString(R.string.review_rating_selected, (int) rating)));
        }

        updateReviewInputUi();
    }

    private void updateReviewInputUi() {
        if (btnSubmitReview == null || etReviewComment == null) return;

        boolean canReview = authSessionManager.isLoggedIn() && !TextUtils.isEmpty(token) && !TextUtils.isEmpty(currentUserId);
        btnSubmitReview.setEnabled(canReview);
        etReviewComment.setEnabled(canReview);
        
        if (!canReview) {
            etReviewComment.setHint(R.string.review_login_to_comment);
            btnSubmitReview.setOnClickListener(v -> showLoginDialog()); // Allow clicking to open login
        } else {
            etReviewComment.setHint(R.string.review_input_hint);
            btnSubmitReview.setOnClickListener(view -> submitReview());
        }
    }

    private void loadReviews() {
        String listingId = currentListing.getListingId();
        if (TextUtils.isEmpty(listingId)) {
            showReviewEmpty(getString(R.string.review_listing_id_missing));
            return;
        }

        updateReviewInputUi();

        if (progressReviews != null) progressReviews.setVisibility(android.view.View.VISIBLE);
        if (tvReviewEmpty != null) tvReviewEmpty.setVisibility(android.view.View.GONE);

        String authorization = TextUtils.isEmpty(token) ? null : "Bearer " + token;
        apiService.getReviewsByListingId(authorization, listingId).enqueue(new Callback<ApiResponse<List<JsonElement>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<JsonElement>>> call, Response<ApiResponse<List<JsonElement>>> response) {
                if (progressReviews != null) progressReviews.setVisibility(android.view.View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<JsonElement> reviews = response.body().getData();
                    if (reviews == null) reviews = new ArrayList<>();
                    if (reviewAdapter != null) reviewAdapter.setData(reviews);
                    updateReviewSummary(reviews);
                    if (reviews.isEmpty()) {
                        showReviewEmpty(getString(R.string.review_empty));
                    } else if (tvReviewEmpty != null) {
                        tvReviewEmpty.setVisibility(android.view.View.GONE);
                    }
                    return;
                }

                if (reviewAdapter != null) reviewAdapter.setData(new ArrayList<>());
                updateReviewSummary(new ArrayList<>());
                showReviewEmpty(extractResponseErrorMessage(response, getString(R.string.review_load_failed)));
            }

            @Override
            public void onFailure(Call<ApiResponse<List<JsonElement>>> call, Throwable throwable) {
                if (progressReviews != null) progressReviews.setVisibility(android.view.View.GONE);
                if (reviewAdapter != null) reviewAdapter.setData(new ArrayList<>());
                updateReviewSummary(new ArrayList<>());
                showReviewEmpty(getString(R.string.review_load_failed));
            }
        });
    }

    private void showRelatedEmpty() {
        if (relatedAdapter != null) relatedAdapter.setListings(Collections.emptyList());
        if (tvRelatedEmpty != null) tvRelatedEmpty.setVisibility(TextView.VISIBLE);
    }

    private List<Listing> buildRelatedListings(List<Listing> allListings) {
        List<Listing> candidates = new ArrayList<>();
        for (Listing listing : allListings) {
            if (listing == null) continue;
            if (!"Active".equalsIgnoreCase(listing.getStatus())) continue;
            if (!TextUtils.isEmpty(currentListing.getListingId()) && currentListing.getListingId().equals(listing.getListingId())) continue;
            candidates.add(listing);
        }

        candidates.sort(Comparator.comparingInt(this::relatedScore).reversed());
        return candidates.size() > 6 ? new ArrayList<>(candidates.subList(0, 6)) : candidates;
    }

    private int relatedScore(Listing candidate) {
        int score = 0;
        Item currentItem = currentListing.getItem();
        Item candidateItem = candidate.getItem();

        if (currentItem != null && candidateItem != null) {
            if (!TextUtils.isEmpty(currentItem.getItemTypeName()) && currentItem.getItemTypeName().equalsIgnoreCase(candidateItem.getItemTypeName())) score += 2;
            if (!TextUtils.isEmpty(currentItem.getBrand()) && currentItem.getBrand().equalsIgnoreCase(candidateItem.getBrand())) score += 1;
        }
        return score;
    }

    @Override
    public void onLoginSuccess(String displayName) {
        refreshAuthHeaderUi();
        token = sanitizeToken(authSessionManager.getAuthToken());
        currentUserId = JwtUtils.extractUserId(token);
        loadReviews();
    }

    @Override
    public void onOpenRegisterRequested() {
        showRegisterDialog();
    }

    @Override
    public void onRegisterSuccess(String displayName) {
        refreshAuthHeaderUi();
        token = sanitizeToken(authSessionManager.getAuthToken());
        currentUserId = JwtUtils.extractUserId(token);
        loadReviews();
    }

    @Override
    public void onOpenLoginRequested() {
        showLoginDialog();
    }

    private static class CategoryMenuItem {
        private final String title;
        private final int iconResId;
        private CategoryMenuItem(String title, int iconResId) {
            this.title = title;
            this.iconResId = iconResId;
        }
    }

    private void submitReview() {
        if (!authSessionManager.isLoggedIn()) {
            Toast.makeText(this, R.string.review_login_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String revieweeId = currentListing.getUserId();
        if (TextUtils.isEmpty(revieweeId)) {
            Toast.makeText(this, R.string.review_reviewee_missing, Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, R.string.review_user_id_missing, Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = etReviewComment.getText() == null ? "" : etReviewComment.getText().toString().trim();
        if (TextUtils.isEmpty(comment)) {
            etReviewComment.setError(getString(R.string.review_comment_required));
            return;
        }

        int rating = Math.max(1, Math.min(5, (int) ratingBarReview.getRating()));
        String listingId = currentListing.getListingId();
        CreateReviewRequest request = new CreateReviewRequest(currentUserId, revieweeId, listingId, rating, comment);
        
        String authorization = "Bearer " + token;

        btnSubmitReview.setEnabled(false);
        apiService.createReview(authorization, request).enqueue(new Callback<ApiResponse<JsonElement>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonElement>> call, Response<ApiResponse<JsonElement>> response) {
                btnSubmitReview.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    etReviewComment.setText("");
                    ratingBarReview.setRating(5);
                    tvRatingValue.setText(getString(R.string.review_rating_selected, 5));
                    Toast.makeText(DetailActivity.this, R.string.review_submit_success, Toast.LENGTH_SHORT).show();
                    loadReviews();
                    return;
                }
                Toast.makeText(DetailActivity.this, extractResponseErrorMessage(response, getString(R.string.review_submit_failed)), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonElement>> call, Throwable throwable) {
                btnSubmitReview.setEnabled(true);
                Toast.makeText(DetailActivity.this, R.string.review_submit_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateReviewSummary(List<JsonElement> reviews) {
        if (tvReviewSummary == null) return;
        if (reviews == null || reviews.isEmpty()) {
            tvReviewSummary.setText(getString(R.string.review_summary_empty));
            return;
        }

        int total = 0;
        int counted = 0;
        for (JsonElement element : reviews) {
            JsonObject object = asJsonObject(element);
            if (object.has("rating") && !object.get("rating").isJsonNull()) {
                try {
                    int rating = object.get("rating").getAsInt();
                    if (rating > 0) {
                        total += rating;
                        counted++;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (counted == 0) {
            tvReviewSummary.setText(getString(R.string.review_summary_count_only, reviews.size()));
        } else {
            double average = (double) total / counted;
            tvReviewSummary.setText(getString(R.string.review_summary_format, average, reviews.size()));
        }
    }

    private void showReviewEmpty(String message) {
        if (tvReviewEmpty != null) {
            tvReviewEmpty.setText(message);
            tvReviewEmpty.setVisibility(android.view.View.VISIBLE);
        }
    }

    private JsonObject asJsonObject(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private String sanitizeToken(String rawToken) {
        if (TextUtils.isEmpty(rawToken)) return "";
        String trimmed = rawToken.trim();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("bearer ")) return trimmed.substring(7).trim();
        return trimmed;
    }

    private <T> String extractResponseErrorMessage(Response<ApiResponse<T>> response, String fallback) {
        if (response != null) {
            ApiResponse<T> body = response.body();
            if (body != null) {
                if (body.getErrors() != null && !body.getErrors().isEmpty()) {
                    String firstError = body.getErrors().get(0);
                    if (!TextUtils.isEmpty(firstError)) return firstError;
                }
                if (!TextUtils.isEmpty(body.getMessage())) return body.getMessage();
            }
            try (ResponseBody errorBody = response.errorBody()) {
                if (errorBody != null) {
                    String raw = errorBody.string();
                    if (!TextUtils.isEmpty(raw)) return raw;
                }
            } catch (IOException ignored) {}
        }
        return fallback;
    }
}
