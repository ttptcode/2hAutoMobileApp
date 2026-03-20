package com.example.a2hauto;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsListingsActivity extends AppCompatActivity implements NewsListingAdapter.ListingActionListener {

    private enum NewsTab {
        SHOWING, EXPIRED, DRAFT, HIDDEN
    }

    private enum CategoryFilter {
        ALL, CAR, MOTORBIKE, BICYCLE, ELECTRIC, PARTS
    }

    private RecyclerView rvListings;
    private ProgressBar progressBar;
    private TextView tvUserName, tvUserPhone, tvTabShowing, tvTabExpired, tvTabDraft, tvTabHidden;
    private TextView chipAll, chipCar, chipMotorbike, chipBicycle, chipElectric, chipParts;
    private TextView tvNewsEmptyState;
    private View layoutEmptyState;
    private MaterialButton btnCreatePost, btnCreatePostMain;

    private NewsListingAdapter listingAdapter;
    private ApiService apiService;
    private AuthSessionManager authSessionManager;
    private final List<Listing> allListings = new ArrayList<>();

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
        // Quan trọng: Sử dụng ApiClient.getApiService(this) để đính kèm Token
        apiService = ApiClient.getApiService(this);

        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchListings();
    }

    private void bindViews() {
        rvListings = findViewById(R.id.rvConversations);
        progressBar = findViewById(R.id.progressBarNews);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        tvTabShowing = findViewById(R.id.tvTabShowing);
        tvTabExpired = findViewById(R.id.tvTabExpired);
        tvTabDraft = findViewById(R.id.tvTabDraft);
        tvTabHidden = findViewById(R.id.tvTabHidden);
        chipAll = findViewById(R.id.chipAll);
        chipCar = findViewById(R.id.chipCar);
        chipMotorbike = findViewById(R.id.chipMotorbike);
        chipBicycle = findViewById(R.id.chipBicycle);
        chipElectric = findViewById(R.id.chipElectric);
        chipParts = findViewById(R.id.chipParts);
        tvNewsEmptyState = findViewById(R.id.tvNewsEmptyState);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        btnCreatePost = findViewById(R.id.btnCreatePost);
        btnCreatePostMain = findViewById(R.id.btnCreatePostMain);

        tvUserName.setText(authSessionManager.getDisplayName());
        String phone = authSessionManager.getPhoneNumber();
        if (TextUtils.isEmpty(phone)) phone = getString(R.string.news_phone_missing);
        tvUserPhone.setText(getString(R.string.news_phone_format, phone));

        updateFilterChipStyles();
        updateTabStyles();
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
        btnCreatePostMain.setOnClickListener(view -> startActivity(new Intent(this, ChooseCategoryActivity.class)));

        tvTabShowing.setOnClickListener(v -> { selectedTab = NewsTab.SHOWING; updateTabStyles(); applyFilters(); });
        tvTabExpired.setOnClickListener(v -> { selectedTab = NewsTab.EXPIRED; updateTabStyles(); applyFilters(); });
        tvTabDraft.setOnClickListener(v -> { selectedTab = NewsTab.DRAFT; updateTabStyles(); applyFilters(); });
        tvTabHidden.setOnClickListener(v -> { selectedTab = NewsTab.HIDDEN; updateTabStyles(); applyFilters(); });

        chipAll.setOnClickListener(v -> setCategoryFilter(CategoryFilter.ALL));
        chipCar.setOnClickListener(v -> setCategoryFilter(CategoryFilter.CAR));
        chipMotorbike.setOnClickListener(v -> setCategoryFilter(CategoryFilter.MOTORBIKE));
        chipBicycle.setOnClickListener(v -> setCategoryFilter(CategoryFilter.BICYCLE));
        chipElectric.setOnClickListener(v -> setCategoryFilter(CategoryFilter.ELECTRIC));
        chipParts.setOnClickListener(v -> setCategoryFilter(CategoryFilter.PARTS));
    }

    private void setCategoryFilter(CategoryFilter categoryFilter) {
        selectedCategory = categoryFilter;
        updateFilterChipStyles();
        applyFilters();
    }

    private void fetchListings() {
        if (!authSessionManager.isLoggedIn()) return;

        String token = sanitizeToken(authSessionManager.getAuthToken());
        String userId = JwtUtils.extractUserId(token);
        if (TextUtils.isEmpty(userId)) return;

        progressBar.setVisibility(View.VISIBLE);
        apiService.getListings().enqueue(new Callback<ApiResponse<List<Listing>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Listing>>> call, Response<ApiResponse<List<Listing>>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Listing> data = response.body().getData();
                    allListings.clear();
                    if (data != null) {
                        for (Listing l : data) if (userId.equals(l.getUserId())) allListings.add(l);
                    }
                    applyFilters();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Listing>>> call, Throwable t) { progressBar.setVisibility(View.GONE); }
        });
    }

    private void applyFilters() {
        int showing = 0, expired = 0, draft = 0, hidden = 0;
        int car = 0, moto = 0, bike = 0, elec = 0, parts = 0;

        List<Listing> filtered = new ArrayList<>();
        for (Listing listing : allListings) {
            NewsTab tab = resolveTab(listing);
            CategoryFilter cat = resolveCategory(listing);

            if (tab == NewsTab.SHOWING) showing++; else if (tab == NewsTab.EXPIRED) expired++; else if (tab == NewsTab.DRAFT) draft++; else if (tab == NewsTab.HIDDEN) hidden++;
            if (cat == CategoryFilter.CAR) car++; else if (cat == CategoryFilter.MOTORBIKE) moto++; else if (cat == CategoryFilter.BICYCLE) bike++; else if (cat == CategoryFilter.ELECTRIC) elec++; else if (cat == CategoryFilter.PARTS) parts++;

            if (selectedTab == tab && (selectedCategory == CategoryFilter.ALL || selectedCategory == cat)) filtered.add(listing);
        }

        updateTabAndChipCounts(showing, expired, draft, hidden, car, moto, bike, elec, parts);
        listingAdapter.setData(filtered);
        layoutEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rvListings.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void updateTabAndChipCounts(int s, int ex, int d, int h, int c, int m, int b, int el, int p) {
        tvTabShowing.setText(getString(R.string.news_tab_showing, s));
        tvTabExpired.setText(getString(R.string.news_tab_expired, ex));
        tvTabDraft.setText(getString(R.string.news_tab_draft, d));
        tvTabHidden.setText("ĐÃ ẨN (" + h + ")");
        chipAll.setText(getString(R.string.news_chip_all_count, c+m+b+el+p));
        chipCar.setText(getString(R.string.news_chip_car_count, c));
        chipMotorbike.setText(getString(R.string.news_chip_motorbike_count, m));
        chipBicycle.setText(getString(R.string.news_chip_bicycle_count, b));
        chipElectric.setText(getString(R.string.news_chip_electric_count, el));
        chipParts.setText(getString(R.string.news_chip_parts_count, p));
        tvNewsEmptyState.setText(selectedTab == NewsTab.SHOWING ? R.string.news_empty : R.string.news_has_data_hint);
    }

    private CategoryFilter resolveCategory(Listing listing) {
        if (listing == null) return CategoryFilter.ALL;
        String title = (listing.getDisplayTitle() + " " + (listing.getItem() != null ? listing.getItem().getItemTypeName() : "")).toLowerCase(Locale.ROOT);
        if (title.contains("ô tô") || title.contains("oto") || title.contains("car")) return CategoryFilter.CAR;
        if (title.contains("xe máy") || title.contains("xemay") || title.contains("moto")) return CategoryFilter.MOTORBIKE;
        if (title.contains("xe đạp") || title.contains("xedap") || title.contains("bicycle")) return CategoryFilter.BICYCLE;
        if (title.contains("xe điện") || title.contains("xedien") || title.contains("electric")) return CategoryFilter.ELECTRIC;
        if (title.contains("phụ tùng") || title.contains("phụ kiện") || title.contains("part")) return CategoryFilter.PARTS;
        return CategoryFilter.ALL;
    }

    private NewsTab resolveTab(Listing listing) {
        String status = (listing.getStatus() != null ? listing.getStatus() : "").toLowerCase(Locale.ROOT);
        if (status.contains("draft")) return NewsTab.DRAFT;
        if (status.contains("expired")) return NewsTab.EXPIRED;
        if (status.contains("hidden")) return NewsTab.HIDDEN;
        return NewsTab.SHOWING;
    }

    private void updateTabStyles() {
        styleTab(tvTabShowing, selectedTab == NewsTab.SHOWING);
        styleTab(tvTabExpired, selectedTab == NewsTab.EXPIRED);
        styleTab(tvTabDraft, selectedTab == NewsTab.DRAFT);
        styleTab(tvTabHidden, selectedTab == NewsTab.HIDDEN);
    }

    private void styleTab(TextView tab, boolean isSelected) {
        tab.setTextColor(getColor(isSelected ? R.color.primary_teal_dark : R.color.text_secondary));
        tab.setTypeface(tab.getTypeface(), isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
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

    @Override
    public void onToggleVisibility(Listing listing, boolean hide) {
        if (listing == null) return;
        progressBar.setVisibility(View.VISIBLE);
        apiService.toggleStatus(listing.getListingId()).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(NewsListingsActivity.this, R.string.news_status_update_success, Toast.LENGTH_SHORT).show();
                    fetchListings();
                } else {
                    Toast.makeText(NewsListingsActivity.this, R.string.news_status_update_failed, Toast.LENGTH_SHORT).show();
                    fetchListings();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NewsListingsActivity.this, R.string.news_status_update_failed, Toast.LENGTH_SHORT).show();
                fetchListings();
            }
        });
    }

    @Override public void onViewListing(Listing l) { Intent i = new Intent(this, DetailActivity.class); i.putExtra("listing", l); startActivity(i); }
    @Override public void onDeleteListing(Listing l) {
        new AlertDialog.Builder(this).setTitle(R.string.news_delete_confirm_title).setMessage(R.string.news_delete_confirm_message)
                .setNegativeButton(R.string.news_action_cancel, null).setPositiveButton(R.string.news_action_confirm_delete, (d, w) -> {}).show();
    }

    @Override
    public void onEditListing(Listing listing) {
        if (listing == null) return;
        CategoryFilter cat = resolveCategory(listing);
        Class<?> target = CreatePostActivity.class;
        String name = "Ô tô";
        String typeId = "e6aee7cb-dff5-41a5-8cce-55f174768daa";

        if (cat == CategoryFilter.MOTORBIKE) { target = CreateMotoPostActivity.class; name = "Xe máy"; typeId = "a7d95ab6-0267-4e91-bc7f-04977f1b402a"; }
        else if (cat == CategoryFilter.BICYCLE) { target = CreateBikePostActivity.class; name = "Xe đạp"; typeId = "7880097c-9b84-48bc-b286-36940dfcc471"; }
        else if (cat == CategoryFilter.ELECTRIC) { target = CreateElectricBikePostActivity.class; name = "Xe điện"; typeId = "96d7f02d-531e-4530-9759-33512b9d6288"; }
        else if (cat == CategoryFilter.PARTS) { target = CreateAccessoryPostActivity.class; name = "Phụ tùng"; typeId = "c8f2a281-b51c-4b36-a53c-2358999331e2"; }

        if (listing.getItem() != null && listing.getItem().getItemTypeId() != null) typeId = listing.getItem().getItemTypeId();

        Intent intent = new Intent(this, target);
        intent.putExtra("isEditMode", true);
        intent.putExtra("listingData", listing);
        intent.putExtra("categoryName", name);
        intent.putExtra("itemTypeId", typeId);
        startActivity(intent);
    }

    public void onPublishListing(Listing listing) {
        onToggleVisibility(listing, false);
    }

    private String sanitizeToken(String t) {
        if (TextUtils.isEmpty(t)) return "";
        String tr = t.trim();
        return tr.toLowerCase(Locale.ROOT).startsWith("bearer ") ? tr.substring(7).trim() : tr;
    }
}
