package com.example.a2hauto;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.a2hauto.adapter.CategoryAdapter;
import com.example.a2hauto.api.ApiService;
import com.example.a2hauto.auth.AuthInterceptor;
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.ItemType;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChooseCategoryActivity extends AppCompatActivity {

    private RecyclerView rvCategories;
    private CategoryAdapter adapter;
    private ProgressBar progressBar;
    private ApiService apiService;
    private FrameLayout bottomNavContainer;
    
    // Navbar items
    private LinearLayout navHome, navFavorites, navPost, navChat, navAccount;
    private int currentNavItem = 2; // Default to Post since we're on category selection
    private ImageView ivNavAccountIcon;
    private TextView tvNavAccountLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_category);

        rvCategories = findViewById(R.id.rvCategories);
        progressBar = findViewById(R.id.progressBar);
        bottomNavContainer = findViewById(R.id.bottomNavContainer);

        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryAdapter(new ArrayList<>(), this::onCategorySelected);
        rvCategories.setAdapter(adapter);

        initRetrofit();
        setupBottomNavigation();
        fetchItemTypes();
    }

    private void initRetrofit() {
        // Tạo OkHttpClient với AuthInterceptor
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(new AuthInterceptor(this));

        apiService = new Retrofit.Builder()
                .baseUrl("http://vehiclemarket.runasp.net/")
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }

    private void setupBottomNavigation() {
        // Inflate the bottom navigation bar layout
        View navView = getLayoutInflater().inflate(R.layout.bottom_navigation_bar, bottomNavContainer, true);

        // Get references to all nav items
        navHome = navView.findViewById(R.id.navHome);
        navFavorites = navView.findViewById(R.id.navFavorites);
        navPost = navView.findViewById(R.id.navPost);
        navChat = navView.findViewById(R.id.navChat);
        navAccount = navView.findViewById(R.id.navAccount);
        
        // Get account icon and label
        ivNavAccountIcon = navView.findViewById(R.id.ivNavAccountIcon);
        tvNavAccountLabel = navView.findViewById(R.id.tvNavAccountLabel);

        // Set up navigation click listeners with highlight
        navHome.setOnClickListener(v -> {
            selectNavItem(0);
            navigateToHome();
        });
        
        navFavorites.setOnClickListener(v -> {
            selectNavItem(1);
            navigateToFavorites();
        });
        
        navPost.setOnClickListener(v -> {
            selectNavItem(2);
            navigateToPost();
        });
        
        navChat.setOnClickListener(v -> {
            selectNavItem(3);
            navigateToChat();
        });
        
        navAccount.setOnClickListener(v -> {
            selectNavItem(4);
            navigateToAccount();
        });
        
        // Set initial highlight (Post is selected)
        selectNavItem(2);
    }

    private void selectNavItem(int navIndex) {
        // Remove highlight from all items
        resetAllNavItems();
        
        // Highlight selected item
        currentNavItem = navIndex;
        LinearLayout selectedNav = null;
        
        switch (navIndex) {
            case 0:
                selectedNav = navHome;
                break;
            case 1:
                selectedNav = navFavorites;
                break;
            case 2:
                selectedNav = navPost;
                break;
            case 3:
                selectedNav = navChat;
                break;
            case 4:
                selectedNav = navAccount;
                break;
        }
        
        if (selectedNav != null) {
            highlightNavItem(selectedNav);
        }
    }

    private void resetAllNavItems() {
        if (navHome != null) unhighlightNavItem(navHome);
        if (navFavorites != null) unhighlightNavItem(navFavorites);
        if (navPost != null) unhighlightNavItem(navPost);
        if (navChat != null) unhighlightNavItem(navChat);
        if (navAccount != null) unhighlightNavItem(navAccount);
    }

    private void highlightNavItem(LinearLayout navItem) {
        // Add scale animation for transition
        navItem.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(200)
                .start();
        
        // Set background and update colors
        navItem.setBackgroundResource(R.drawable.bg_nav_active);
        
        // Update icon and text color for highlighted state
        for (int i = 0; i < navItem.getChildCount(); i++) {
            android.view.View child = navItem.getChildAt(i);
            if (child instanceof ImageView) {
                ((ImageView) child).setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.primary_teal_dark), android.graphics.PorterDuff.Mode.SRC_IN);
            } else if (child instanceof TextView) {
                ((TextView) child).setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary_teal_dark));
                ((TextView) child).setTypeface(((TextView) child).getTypeface(), android.graphics.Typeface.BOLD);
            }
        }
    }

    private void unhighlightNavItem(LinearLayout navItem) {
        // Reset scale animation
        navItem.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .start();
        
        // Remove background
        navItem.setBackground(null);
        
        // Reset icon and text color for unhighlighted state
        for (int i = 0; i < navItem.getChildCount(); i++) {
            android.view.View child = navItem.getChildAt(i);
            if (child instanceof ImageView) {
                ((ImageView) child).setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.text_muted), android.graphics.PorterDuff.Mode.SRC_IN);
            } else if (child instanceof TextView) {
                ((TextView) child).setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
                ((TextView) child).setTypeface(((TextView) child).getTypeface(), android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToFavorites() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("tab", "favorites");
        startActivity(intent);
        finish();
    }

    private void navigateToPost() {
        // Already on choose category, just show a toast
        Toast.makeText(this, "Đang trên trang chọn danh mục", Toast.LENGTH_SHORT).show();
    }

    private void navigateToChat() {
        Toast.makeText(this, "Tính năng chat đang được phát triển", Toast.LENGTH_SHORT).show();
    }

    private void navigateToAccount() {
        Toast.makeText(this, "Tính năng tài khoản đang được phát triển", Toast.LENGTH_SHORT).show();
    }

    private void fetchItemTypes() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getItemTypes().enqueue(new Callback<ApiResponse<List<ItemType>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ItemType>>> call, Response<ApiResponse<List<ItemType>>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setCategories(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ItemType>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ChooseCategoryActivity.this, "Lỗi tải danh mục", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onCategorySelected(ItemType type) {
        Intent intent;
        String id = type.getItemTypeId();
        String name = type.getName();
        String nameLower = name.toLowerCase();

        if (nameLower.contains("ô tô")) {
            intent = new Intent(this, CreatePostActivity.class);
        } else if (nameLower.contains("phụ tùng") || nameLower.contains("phụ kiện")) {
            intent = new Intent(this, CreateAccessoryPostActivity.class);
        } else if (nameLower.contains("xe đạp")) {
            intent = new Intent(this, CreateBikePostActivity.class);
        } else if (nameLower.contains("xe điện")) {
            intent = new Intent(this, CreateElectricBikePostActivity.class);
        } else if (nameLower.contains("ắc quy") || nameLower.contains("pin")) {
            intent = new Intent(this, CreateBatteryPostActivity.class);
        } else if (nameLower.contains("xe máy")) {
            intent = new Intent(this, CreateMotoPostActivity.class);
        } else if (nameLower.contains("xe tải") || nameLower.contains("xe ben")) {
            intent = new Intent(this, CreateTruckPostActivity.class);
        } else {
            Toast.makeText(this, "Danh mục này đang được cập nhật", Toast.LENGTH_SHORT).show();
            return;
        }

        intent.putExtra("itemTypeId", id);
        intent.putExtra("categoryName", name);
        startActivity(intent);
    }
}
