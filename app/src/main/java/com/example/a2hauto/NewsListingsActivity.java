package com.example.a2hauto;

import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
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

import com.example.a2hauto.adapter.NewsListingAdapter;
import com.example.a2hauto.api.ApiClient;
import com.example.a2hauto.api.ApiService;
import com.example.a2hauto.auth.AuthSessionManager;
import com.example.a2hauto.auth.JwtUtils;
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.Item;
import com.example.a2hauto.model.Listing;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.IOException;

import okhttp3.ResponseBody;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsListingsActivity extends AppCompatActivity implements NewsListingAdapter.ListingActionListener {

    private enum NewsTab {
        ACTIVE,
        EXPIRED,
        HIDDEN,
        DRAFT
    }

    private enum CategoryFilter {
        ALL,
        CAR,
        MOTORBIKE,
        BICYCLE,
        ELECTRIC,
        PARTS
    }

    private RecyclerView rvListings;
    private ProgressBar progressBar;
    private TextView tvUserName;
    private TextView tvUserPhone;
    private TextView tvTabShowing;
    private TextView tvTabExpired;
    private TextView tvTabHidden;
    private TextView tvTabDraft;
    private TextView chipAll;
    private TextView chipCar;
    private TextView chipMotorbike;
    private TextView chipBicycle;
    private TextView chipElectric;
    private TextView chipParts;
    private TextView tvNewsEmptyState;
    private View layoutEmptyState;
    private MaterialButton btnCreatePost;

    private NewsListingAdapter listingAdapter;
    private ApiService apiService;
    private AuthSessionManager authSessionManager;
    private final List<Listing> allListings = new ArrayList<>();
    private final List<Listing> activeListings = new ArrayList<>();
    private final List<Listing> expiredListings = new ArrayList<>();
    private final List<Listing> hiddenListings = new ArrayList<>();
    private final List<Listing> draftListings = new ArrayList<>();
    private NewsTab selectedTab = NewsTab.ACTIVE;
    private CategoryFilter selectedCategory = CategoryFilter.ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_news_listings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.newsRoot), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        authSessionManager = new AuthSessionManager(this);
        apiService = ApiClient.getApiService();

        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupActions();

        fetchListings();
    }

    private void bindViews() {
        rvListings = findViewById(R.id.rvListings);
        progressBar = findViewById(R.id.progressBarNews);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        tvTabShowing = findViewById(R.id.tvTabShowing);
        tvTabExpired = findViewById(R.id.tvTabExpired);
        tvTabHidden = findViewById(R.id.tvTabHidden);
        tvTabDraft = findViewById(R.id.tvTabDraft);
        chipAll = findViewById(R.id.chipAll);
        chipCar = findViewById(R.id.chipCar);
        chipMotorbike = findViewById(R.id.chipMotorbike);
        chipBicycle = findViewById(R.id.chipBicycle);
        chipElectric = findViewById(R.id.chipElectric);
        chipParts = findViewById(R.id.chipParts);
        tvNewsEmptyState = findViewById(R.id.tvNewsEmptyState);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        btnCreatePost = findViewById(R.id.btnCreatePost);

        tvUserName.setText(authSessionManager.getDisplayName());
        String phone = authSessionManager.getPhoneNumber();
        if (TextUtils.isEmpty(phone)) {
            phone = getString(R.string.news_phone_missing);
        }
        tvUserPhone.setText(getString(R.string.news_phone_format, phone));

        updateTabStyles();
        updateTabCounts();
        updateCategoryChipCounts(0);
        updateCategoryChipStyles();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
    }

    private void setupRecyclerView() {
        rvListings.setLayoutManager(new LinearLayoutManager(this));
        listingAdapter = new NewsListingAdapter(new ArrayList<>(), this);
        rvListings.setAdapter(listingAdapter);
    }

    private void setupActions() {
        btnCreatePost.setOnClickListener(view -> startActivity(new Intent(this, ChooseCategoryActivity.class)));
        tvTabShowing.setOnClickListener(view -> {
            selectedTab = NewsTab.ACTIVE;
            updateTabStyles();
            renderSelectedTab();
        });
        tvTabExpired.setOnClickListener(view -> {
            selectedTab = NewsTab.EXPIRED;
            updateTabStyles();
            renderSelectedTab();
        });
        tvTabHidden.setOnClickListener(view -> {
            selectedTab = NewsTab.HIDDEN;
            updateTabStyles();
            renderSelectedTab();
        });
        tvTabDraft.setOnClickListener(view -> {
            selectedTab = NewsTab.DRAFT;
            updateTabStyles();
            renderSelectedTab();
        });

        chipAll.setOnClickListener(view -> {
            selectedCategory = CategoryFilter.ALL;
            updateCategoryChipStyles();
            renderSelectedTab();
        });
        chipCar.setOnClickListener(view -> {
            selectedCategory = CategoryFilter.CAR;
            updateCategoryChipStyles();
            renderSelectedTab();
        });
        chipMotorbike.setOnClickListener(view -> {
            selectedCategory = CategoryFilter.MOTORBIKE;
            updateCategoryChipStyles();
            renderSelectedTab();
        });
        chipBicycle.setOnClickListener(view -> {
            selectedCategory = CategoryFilter.BICYCLE;
            updateCategoryChipStyles();
            renderSelectedTab();
        });
        chipElectric.setOnClickListener(view -> {
            selectedCategory = CategoryFilter.ELECTRIC;
            updateCategoryChipStyles();
            renderSelectedTab();
        });
        chipParts.setOnClickListener(view -> {
            selectedCategory = CategoryFilter.PARTS;
            updateCategoryChipStyles();
            renderSelectedTab();
        });
    }

    private void fetchListings() {
        if (!authSessionManager.isLoggedIn()) {
            Toast.makeText(this, R.string.news_auth_required, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String rawToken = authSessionManager.getAuthToken();
        String token = sanitizeToken(rawToken);
        if (TextUtils.isEmpty(token)) {
            Toast.makeText(this, R.string.news_auth_required, Toast.LENGTH_SHORT).show();
            clearAndRenderEmpty();
            return;
        }

        String userId = JwtUtils.extractUserId(token);
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, R.string.news_user_id_missing, Toast.LENGTH_SHORT).show();
            clearAndRenderEmpty();
            return;
        }

        String authorization = "Bearer " + token;
        progressBar.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
        rvListings.setVisibility(View.GONE);

        apiService.getListingsByUser(authorization, userId).enqueue(new Callback<ApiResponse<List<Listing>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Listing>>> call, Response<ApiResponse<List<Listing>>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Listing> data = response.body().getData();
                    allListings.clear();
                    if (data != null) {
                        allListings.addAll(data);
                    }
                    splitListingsByTab();
                    updateTabCounts();
                    renderSelectedTab();
                    return;
                }

                clearAndRenderEmpty();
                Toast.makeText(NewsListingsActivity.this, resolveErrorMessage(response), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Listing>>> call, Throwable throwable) {
                progressBar.setVisibility(View.GONE);
                clearAndRenderEmpty();
                Toast.makeText(NewsListingsActivity.this, R.string.news_load_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String resolveErrorMessage(Response<ApiResponse<List<Listing>>> response) {
        if (response != null) {
            ApiResponse<List<Listing>> responseBody = response.body();
            if (responseBody != null) {
                if (responseBody.getErrors() != null && !responseBody.getErrors().isEmpty()) {
                    String firstError = responseBody.getErrors().get(0);
                    if (!TextUtils.isEmpty(firstError)) {
                        return firstError;
                    }
                }

                if (!TextUtils.isEmpty(responseBody.getMessage())) {
                    return responseBody.getMessage();
                }
            }

            try (ResponseBody errorBody = response.errorBody()) {
                if (errorBody != null) {
                    String raw = errorBody.string();
                    if (!TextUtils.isEmpty(raw)) {
                        return raw;
                    }
                }
            } catch (IOException ignored) {
                // Fallback below.
            }
        }

        return getString(R.string.news_load_failed);
    }

    private void splitListingsByTab() {
        activeListings.clear();
        expiredListings.clear();
        hiddenListings.clear();
        draftListings.clear();

        Date now = new Date();
        for (Listing listing : allListings) {
            Date endDate = parseEndDate(listing == null ? null : listing.getEndDate());

            if (endDate != null && endDate.before(now)) {
                // Expired is always prioritized regardless of status.
                expiredListings.add(listing);
                continue;
            }

            String status = listing == null || listing.getStatus() == null
                    ? ""
                    : listing.getStatus().trim().toLowerCase(Locale.ROOT);

            if ("active".equals(status)) {
                activeListings.add(listing);
            } else if ("hidden".equals(status)) {
                hiddenListings.add(listing);
            } else if ("draft".equals(status)) {
                draftListings.add(listing);
            }
        }
    }

    private void updateTabCounts() {
        tvTabShowing.setText(getString(R.string.news_tab_showing, activeListings.size()));
        tvTabExpired.setText(getString(R.string.news_tab_expired, expiredListings.size()));
        tvTabHidden.setText(getString(R.string.news_tab_hidden, hiddenListings.size()));
        tvTabDraft.setText(getString(R.string.news_tab_draft, draftListings.size()));
        updateCategoryChipCounts(getTabBaseList().size());
    }

    private void updateCategoryChipCounts(int total) {
        int car = 0;
        int motorbike = 0;
        int bicycle = 0;
        int electric = 0;
        int parts = 0;

        for (Listing listing : getTabBaseList()) {
            CategoryFilter category = resolveCategory(listing);
            if (category == CategoryFilter.CAR) {
                car++;
            } else if (category == CategoryFilter.MOTORBIKE) {
                motorbike++;
            } else if (category == CategoryFilter.BICYCLE) {
                bicycle++;
            } else if (category == CategoryFilter.ELECTRIC) {
                electric++;
            } else if (category == CategoryFilter.PARTS) {
                parts++;
            }
        }

        int computedTotal = total >= 0 ? total : (car + motorbike + bicycle + electric + parts);
        chipAll.setText(getString(R.string.news_chip_all_count, computedTotal));
        chipCar.setText(getString(R.string.news_chip_car_count, car));
        chipMotorbike.setText(getString(R.string.news_chip_motorbike_count, motorbike));
        chipBicycle.setText(getString(R.string.news_chip_bicycle_count, bicycle));
        chipElectric.setText(getString(R.string.news_chip_electric_count, electric));
        chipParts.setText(getString(R.string.news_chip_parts_count, parts));
    }

    private void renderSelectedTab() {
        List<Listing> filteredList = new ArrayList<>();
        List<Listing> tabBaseList = getTabBaseList();
        for (Listing listing : tabBaseList) {
            CategoryFilter category = resolveCategory(listing);
            if (selectedCategory == CategoryFilter.ALL || selectedCategory == category) {
                filteredList.add(listing);
            }
        }

        updateCategoryChipCounts(tabBaseList.size());
        listingAdapter.setData(filteredList);

        boolean isEmpty = filteredList.isEmpty();
        layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvListings.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvNewsEmptyState.setText(R.string.news_empty_hint);
    }

    private List<Listing> getTabBaseList() {
        if (selectedTab == NewsTab.ACTIVE) {
            return activeListings;
        }
        if (selectedTab == NewsTab.EXPIRED) {
            return expiredListings;
        }
        if (selectedTab == NewsTab.HIDDEN) {
            return hiddenListings;
        }
        return draftListings;
    }

    private CategoryFilter resolveCategory(Listing listing) {
        if (listing == null) {
            return CategoryFilter.ALL;
        }

        StringBuilder textBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(listing.getDisplayTitle())) {
            textBuilder.append(listing.getDisplayTitle()).append(' ');
        }

        Item item = listing.getItem();
        if (item != null) {
            if (!TextUtils.isEmpty(item.getItemTypeName())) {
                textBuilder.append(item.getItemTypeName()).append(' ');
            }
            if (!TextUtils.isEmpty(item.getStyle())) {
                textBuilder.append(item.getStyle()).append(' ');
            }
            if (!TextUtils.isEmpty(item.getBrand())) {
                textBuilder.append(item.getBrand()).append(' ');
            }
        }

        String candidate = textBuilder.toString().toLowerCase(Locale.ROOT);
        if (candidate.contains("ô tô") || candidate.contains("oto") || candidate.contains("car") || candidate.contains("sedan") || candidate.contains("suv")) {
            return CategoryFilter.CAR;
        }
        if (candidate.contains("xe máy") || candidate.contains("xemay") || candidate.contains("moto") || candidate.contains("motor") || candidate.contains("scooter")) {
            return CategoryFilter.MOTORBIKE;
        }
        if (candidate.contains("xe đạp") || candidate.contains("xedap") || candidate.contains("bicycle") || candidate.contains("bike")) {
            return CategoryFilter.BICYCLE;
        }
        if (candidate.contains("xe điện") || candidate.contains("xedien") || candidate.contains("electric") || candidate.contains("ev")) {
            return CategoryFilter.ELECTRIC;
        }
        if (candidate.contains("phụ tùng") || candidate.contains("phu tung") || candidate.contains("phụ kiện") || candidate.contains("phu kien") || candidate.contains("part") || candidate.contains("accessory")) {
            return CategoryFilter.PARTS;
        }
        return CategoryFilter.ALL;
    }

    private void clearAndRenderEmpty() {
        allListings.clear();
        splitListingsByTab();
        updateTabCounts();
        renderSelectedTab();
    }

    @Override
    public void onToggleVisibility(Listing listing, boolean hide) {
        String authorization = getAuthorizationHeader();
        if (TextUtils.isEmpty(authorization) || listing == null || TextUtils.isEmpty(listing.getListingId())) {
            Toast.makeText(this, R.string.news_status_update_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        String targetStatus = hide ? "Hidden" : "Active";
        Map<String, RequestBody> fields = buildWithItemUpdateFields(listing, targetStatus);
        progressBar.setVisibility(View.VISIBLE);

        apiService.updateListingWithItem(authorization, fields).enqueue(new Callback<ApiResponse<Listing>>() {
            @Override
            public void onResponse(Call<ApiResponse<Listing>> call, Response<ApiResponse<Listing>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(NewsListingsActivity.this, R.string.news_status_update_success, Toast.LENGTH_SHORT).show();
                    fetchListings();
                    return;
                }
                Toast.makeText(NewsListingsActivity.this, resolveErrorMessageFromRaw(response), Toast.LENGTH_SHORT).show();
                renderSelectedTab();
            }

            @Override
            public void onFailure(Call<ApiResponse<Listing>> call, Throwable throwable) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NewsListingsActivity.this, R.string.news_status_update_failed, Toast.LENGTH_SHORT).show();
                renderSelectedTab();
            }
        });
    }

    @Override
    public void onViewListing(Listing listing) {
        String authorization = getAuthorizationHeader();
        if (TextUtils.isEmpty(authorization) || listing == null || TextUtils.isEmpty(listing.getListingId())) {
            Toast.makeText(this, R.string.news_view_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        apiService.getListingById(authorization, listing.getListingId()).enqueue(new Callback<ApiResponse<Listing>>() {
            @Override
            public void onResponse(Call<ApiResponse<Listing>> call, Response<ApiResponse<Listing>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null) {
                    showListingDialog(response.body().getData());
                    return;
                }
                Toast.makeText(NewsListingsActivity.this, R.string.news_view_failed, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<ApiResponse<Listing>> call, Throwable throwable) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NewsListingsActivity.this, R.string.news_view_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDeleteListing(Listing listing) {
        if (listing == null || TextUtils.isEmpty(listing.getListingId())) {
            Toast.makeText(this, R.string.news_delete_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.news_delete_confirm_title)
                .setMessage(R.string.news_delete_confirm_message)
                .setNegativeButton(R.string.news_action_cancel, null)
                .setPositiveButton(R.string.news_action_confirm_delete, (dialog, which) -> performDeleteListing(listing.getListingId()))
                .show();
    }

    private void performDeleteListing(String listingId) {
        String authorization = getAuthorizationHeader();
        if (TextUtils.isEmpty(authorization) || TextUtils.isEmpty(listingId)) {
            Toast.makeText(this, R.string.news_delete_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        apiService.deleteListing(authorization, listingId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(NewsListingsActivity.this, R.string.news_delete_success, Toast.LENGTH_SHORT).show();
                    fetchListings();
                    return;
                }
                Toast.makeText(NewsListingsActivity.this, resolveErrorMessageFromRaw(response), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable throwable) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NewsListingsActivity.this, R.string.news_delete_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showListingDialog(Listing listing) {
        String missing = getString(R.string.news_dialog_value_missing);
        String title = safeString(listing == null ? null : listing.getDisplayTitle(), missing);
        String status = safeString(listing == null ? null : listing.getStatus(), missing);
        String endDate = safeString(listing == null ? null : listing.getEndDate(), missing);
        String detail = safeString(listing == null ? null : listing.getDetail(), missing);
        String address = safeString(listing == null ? null : listing.getAddress(), missing);
        String price = formatDisplayPrice(listing == null ? 0d : listing.getBuyNowPrice());

        String content = getString(R.string.news_dialog_content, title, price, status, endDate, detail, address);
        new AlertDialog.Builder(this)
                .setTitle(R.string.news_dialog_title)
                .setMessage(content)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private String safeString(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private String formatDisplayPrice(double price) {
        if (price <= 0d) {
            return getString(R.string.news_price_contact);
        }
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(price);
    }

    private Map<String, RequestBody> buildWithItemUpdateFields(Listing listing, String targetStatus) {
        Map<String, RequestBody> fields = new HashMap<>();
        putField(fields, "listingId", listing.getListingId());
        putField(fields, "status", targetStatus);
        putField(fields, "listingType", listing.getListingType());
        putField(fields, "buyNowPrice", String.valueOf(listing.getBuyNowPrice()));
        putField(fields, "endDate", listing.getEndDate());
        putField(fields, "detail", listing.getDetail());
        putField(fields, "address", listing.getAddress());

        Item item = listing.getItem();
        if (item != null) {
            putField(fields, "itemId", item.getItemId());
            putField(fields, "title", item.getTitle());
            putField(fields, "serialNumber", item.getSerialNumber());
            putField(fields, "itemTypeName", item.getItemTypeName());
            putField(fields, "brand", item.getBrand());
            putField(fields, "model", item.getModel());
            if (item.getYear() != null) {
                putField(fields, "year", String.valueOf(item.getYear()));
            }
            putField(fields, "mileage", item.getMileage());
            putField(fields, "condition", item.getCondition());
            putField(fields, "color", item.getColor());
            putField(fields, "seat", item.getSeat());
            putField(fields, "origin", item.getOrigin());
            putField(fields, "fuel", item.getFuel());
            putField(fields, "gearbox", item.getGearbox());
            putField(fields, "ownerCount", item.getOwnerCount());
            putField(fields, "style", item.getStyle());
            putField(fields, "licensePlate", item.getLicensePlate());
        }

        return fields;
    }

    private void putField(Map<String, RequestBody> fields, String key, String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        fields.put(key, RequestBody.create(MultipartBody.FORM, value));
    }

    private String getAuthorizationHeader() {
        String token = sanitizeToken(authSessionManager.getAuthToken());
        if (TextUtils.isEmpty(token)) {
            return "";
        }
        return "Bearer " + token;
    }

    private String resolveErrorMessageFromRaw(Response<?> response) {
        if (response != null) {
            try (ResponseBody errorBody = response.errorBody()) {
                if (errorBody != null) {
                    String raw = errorBody.string();
                    if (!TextUtils.isEmpty(raw)) {
                        return raw;
                    }
                }
            } catch (IOException ignored) {
                // Fallback below.
            }
        }
        return getString(R.string.news_load_failed);
    }

    private Date parseEndDate(String rawEndDate) {
        if (TextUtils.isEmpty(rawEndDate)) {
            return null;
        }

        String value = rawEndDate.trim();
        String[] patterns = new String[]{
                "yyyy-MM-dd'T'HH:mm",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS"
        };

        for (String pattern : patterns) {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
            sdf.setLenient(false);
            try {
                return sdf.parse(value);
            } catch (ParseException ignored) {
                // Try next pattern.
            }
        }

        return null;
    }

    private String sanitizeToken(String token) {
        if (TextUtils.isEmpty(token)) {
            return "";
        }

        String trimmed = token.trim();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            return trimmed.substring(7).trim();
        }
        return trimmed;
    }

    private void updateTabStyles() {
        styleTab(tvTabShowing, selectedTab == NewsTab.ACTIVE);
        styleTab(tvTabExpired, selectedTab == NewsTab.EXPIRED);
        styleTab(tvTabHidden, selectedTab == NewsTab.HIDDEN);
        styleTab(tvTabDraft, selectedTab == NewsTab.DRAFT);
    }

    private void styleTab(TextView tabView, boolean isSelected) {
        tabView.setTextColor(getColor(isSelected ? R.color.primary_teal_dark : R.color.text_secondary));
        tabView.setTypeface(tabView.getTypeface(), isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void updateCategoryChipStyles() {
        styleChip(chipAll, selectedCategory == CategoryFilter.ALL);
        styleChip(chipCar, selectedCategory == CategoryFilter.CAR);
        styleChip(chipMotorbike, selectedCategory == CategoryFilter.MOTORBIKE);
        styleChip(chipBicycle, selectedCategory == CategoryFilter.BICYCLE);
        styleChip(chipElectric, selectedCategory == CategoryFilter.ELECTRIC);
        styleChip(chipParts, selectedCategory == CategoryFilter.PARTS);
    }

    private void styleChip(TextView chip, boolean isSelected) {
        chip.setBackgroundResource(isSelected ? R.drawable.bg_filter_chip : R.drawable.bg_search_surface);
        chip.setTextColor(getColor(isSelected ? R.color.primary_teal_dark : R.color.text_secondary));
        chip.setTypeface(chip.getTypeface(), isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }
}
