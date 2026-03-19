package com.example.a2hauto;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
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

import com.example.a2hauto.adapter.ConversationAdapter;
import com.example.a2hauto.api.ApiClient;
import com.example.a2hauto.api.ApiService;
import com.example.a2hauto.auth.AuthSessionManager;
import com.example.a2hauto.auth.JwtUtils;
import com.example.a2hauto.model.ApiResponse;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsListingsActivity extends AppCompatActivity {

    private enum NewsTab {
        SHOWING,
        EXPIRED,
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

    private RecyclerView rvConversations;
    private ProgressBar progressBar;
    private TextView tvUserName;
    private TextView tvUserPhone;
    private TextView tvTabShowing;
    private TextView tvTabExpired;
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

    private ConversationAdapter conversationAdapter;
    private ApiService apiService;
    private AuthSessionManager authSessionManager;
    private final List<JsonElement> allConversations = new ArrayList<>();
    private NewsTab selectedTab = NewsTab.SHOWING;
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

        fetchConversations();
    }

    private void bindViews() {
        rvConversations = findViewById(R.id.rvConversations);
        progressBar = findViewById(R.id.progressBarNews);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        tvTabShowing = findViewById(R.id.tvTabShowing);
        tvTabExpired = findViewById(R.id.tvTabExpired);
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

        updateFilterChipStyles();
        updateTabStyles();
        updateTabAndChipCounts(0, 0, 0, 0, 0, 0, 0, 0);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
    }

    private void setupRecyclerView() {
        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        conversationAdapter = new ConversationAdapter(new ArrayList<>());
        rvConversations.setAdapter(conversationAdapter);
    }

    private void setupActions() {
        btnCreatePost.setOnClickListener(view -> startActivity(new Intent(this, ChooseCategoryActivity.class)));
        tvTabShowing.setOnClickListener(view -> {
            selectedTab = NewsTab.SHOWING;
            updateTabStyles();
            applyFilters();
        });
        tvTabExpired.setOnClickListener(view -> {
            selectedTab = NewsTab.EXPIRED;
            updateTabStyles();
            applyFilters();
        });
        tvTabDraft.setOnClickListener(view -> {
            selectedTab = NewsTab.DRAFT;
            updateTabStyles();
            applyFilters();
        });

        chipAll.setOnClickListener(view -> setCategoryFilter(CategoryFilter.ALL));
        chipCar.setOnClickListener(view -> setCategoryFilter(CategoryFilter.CAR));
        chipMotorbike.setOnClickListener(view -> setCategoryFilter(CategoryFilter.MOTORBIKE));
        chipBicycle.setOnClickListener(view -> setCategoryFilter(CategoryFilter.BICYCLE));
        chipElectric.setOnClickListener(view -> setCategoryFilter(CategoryFilter.ELECTRIC));
        chipParts.setOnClickListener(view -> setCategoryFilter(CategoryFilter.PARTS));
    }

    private void setCategoryFilter(CategoryFilter categoryFilter) {
        selectedCategory = categoryFilter;
        updateFilterChipStyles();
        applyFilters();
    }

    private void fetchConversations() {
        if (!authSessionManager.isLoggedIn()) {
            Toast.makeText(this, R.string.news_auth_required, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String rawToken = authSessionManager.getAuthToken();
        String token = sanitizeToken(rawToken);
        if (TextUtils.isEmpty(token)) {
            Toast.makeText(this, R.string.news_auth_required, Toast.LENGTH_SHORT).show();
            allConversations.clear();
            applyFilters();
            return;
        }

        String userId = JwtUtils.extractUserId(token);
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, R.string.news_user_id_missing, Toast.LENGTH_SHORT).show();
            allConversations.clear();
            applyFilters();
            return;
        }

        String authorization = "Bearer " + token;
        progressBar.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
        rvConversations.setVisibility(View.GONE);

        apiService.getConversations(authorization, userId).enqueue(new Callback<ApiResponse<List<JsonElement>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<JsonElement>>> call, Response<ApiResponse<List<JsonElement>>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<JsonElement> data = response.body().getData();
                    allConversations.clear();
                    if (data != null) {
                        allConversations.addAll(data);
                    }
                    applyFilters();
                    return;
                }

                allConversations.clear();
                applyFilters();
                Toast.makeText(NewsListingsActivity.this, resolveErrorMessage(response), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<JsonElement>>> call, Throwable throwable) {
                progressBar.setVisibility(View.GONE);
                allConversations.clear();
                applyFilters();
                Toast.makeText(NewsListingsActivity.this, R.string.news_load_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String resolveErrorMessage(Response<ApiResponse<List<JsonElement>>> response) {
        if (response != null) {
            ApiResponse<List<JsonElement>> responseBody = response.body();
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

    private void applyFilters() {
        int showingCount = 0;
        int expiredCount = 0;
        int draftCount = 0;
        int carCount = 0;
        int motorbikeCount = 0;
        int bicycleCount = 0;
        int electricCount = 0;
        int partsCount = 0;

        List<JsonElement> filtered = new ArrayList<>();
        for (JsonElement element : allConversations) {
            JsonObject conversation = asJsonObject(element);
            NewsTab tab = resolveTab(conversation);
            CategoryFilter category = resolveCategory(conversation);

            if (tab == NewsTab.SHOWING) {
                showingCount++;
            } else if (tab == NewsTab.EXPIRED) {
                expiredCount++;
            } else {
                draftCount++;
            }

            if (category == CategoryFilter.CAR) {
                carCount++;
            } else if (category == CategoryFilter.MOTORBIKE) {
                motorbikeCount++;
            } else if (category == CategoryFilter.BICYCLE) {
                bicycleCount++;
            } else if (category == CategoryFilter.ELECTRIC) {
                electricCount++;
            } else if (category == CategoryFilter.PARTS) {
                partsCount++;
            }

            boolean tabMatch = selectedTab == tab;
            boolean categoryMatch = selectedCategory == CategoryFilter.ALL || selectedCategory == category;
            if (tabMatch && categoryMatch) {
                filtered.add(element);
            }
        }

        updateTabAndChipCounts(showingCount, expiredCount, draftCount, carCount, motorbikeCount, bicycleCount, electricCount, partsCount);
        conversationAdapter.setData(filtered);

        boolean isEmpty = filtered.isEmpty();
        layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvConversations.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void updateTabAndChipCounts(int showing, int expired, int draft, int car, int motorbike, int bicycle, int electric, int parts) {
        tvTabShowing.setText(getString(R.string.news_tab_showing, showing));
        tvTabExpired.setText(getString(R.string.news_tab_expired, expired));
        tvTabDraft.setText(getString(R.string.news_tab_draft, draft));

        int all = car + motorbike + bicycle + electric + parts;
        chipAll.setText(getString(R.string.news_chip_all_count, all));
        chipCar.setText(getString(R.string.news_chip_car_count, car));
        chipMotorbike.setText(getString(R.string.news_chip_motorbike_count, motorbike));
        chipBicycle.setText(getString(R.string.news_chip_bicycle_count, bicycle));
        chipElectric.setText(getString(R.string.news_chip_electric_count, electric));
        chipParts.setText(getString(R.string.news_chip_parts_count, parts));

        if (selectedTab == NewsTab.SHOWING) {
            tvNewsEmptyState.setText(R.string.news_empty);
        } else {
            tvNewsEmptyState.setText(R.string.news_has_data_hint);
        }
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

    private JsonObject asJsonObject(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private String getAsString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }

        try {
            return object.get(key).getAsString();
        } catch (Exception exception) {
            return "";
        }
    }

    private NewsTab resolveTab(JsonObject conversation) {
        String status = getAsString(conversation, "status").toLowerCase(Locale.ROOT);
        if (status.contains("draft") || status.contains("nhap")) {
            return NewsTab.DRAFT;
        }
        if (status.contains("expired") || status.contains("het han")) {
            return NewsTab.EXPIRED;
        }
        return NewsTab.SHOWING;
    }

    private CategoryFilter resolveCategory(JsonObject conversation) {
        Pair<String, String> listingInfo = extractListingInfo(conversation);
        String candidate = (listingInfo.first + " " + listingInfo.second).toLowerCase(Locale.ROOT);

        if (candidate.contains("ô tô") || candidate.contains("oto") || candidate.contains("car")) {
            return CategoryFilter.CAR;
        }
        if (candidate.contains("xe máy") || candidate.contains("xemay") || candidate.contains("moto") || candidate.contains("motor")) {
            return CategoryFilter.MOTORBIKE;
        }
        if (candidate.contains("xe đạp") || candidate.contains("xedap") || candidate.contains("bicycle") || candidate.contains("bike")) {
            return CategoryFilter.BICYCLE;
        }
        if (candidate.contains("xe điện") || candidate.contains("xedien") || candidate.contains("electric")) {
            return CategoryFilter.ELECTRIC;
        }
        if (candidate.contains("phụ tùng") || candidate.contains("phu tung") || candidate.contains("phụ kiện") || candidate.contains("phu kien") || candidate.contains("part") || candidate.contains("accessor")) {
            return CategoryFilter.PARTS;
        }
        return CategoryFilter.ALL;
    }

    private Pair<String, String> extractListingInfo(JsonObject conversation) {
        if (conversation == null || !conversation.has("listing") || !conversation.get("listing").isJsonObject()) {
            return new Pair<>("", "");
        }

        JsonObject listing = conversation.getAsJsonObject("listing");
        String title = getAsString(listing, "itemTitle");
        String category = getAsString(listing, "itemTypeName");

        if (listing.has("item") && listing.get("item").isJsonObject()) {
            JsonObject item = listing.getAsJsonObject("item");
            if (TextUtils.isEmpty(title)) {
                title = getAsString(item, "title");
            }
            if (TextUtils.isEmpty(category)) {
                category = getAsString(item, "itemTypeName");
            }
            if (TextUtils.isEmpty(category)) {
                category = getAsString(item, "category");
            }
        }

        return new Pair<>(title, category);
    }

    private void updateTabStyles() {
        styleTab(tvTabShowing, selectedTab == NewsTab.SHOWING);
        styleTab(tvTabExpired, selectedTab == NewsTab.EXPIRED);
        styleTab(tvTabDraft, selectedTab == NewsTab.DRAFT);
    }

    private void styleTab(TextView tabView, boolean isSelected) {
        tabView.setTextColor(getColor(isSelected ? R.color.primary_teal_dark : R.color.text_secondary));
        tabView.setTypeface(tabView.getTypeface(), isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void updateFilterChipStyles() {
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
