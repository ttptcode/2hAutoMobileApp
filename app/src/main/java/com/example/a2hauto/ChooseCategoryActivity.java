package com.example.a2hauto;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.a2hauto.adapter.CategoryAdapter;
import com.example.a2hauto.api.ApiService;
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.ItemType;
import java.util.ArrayList;
import java.util.List;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_category);

        rvCategories = findViewById(R.id.rvCategories);
        progressBar = findViewById(R.id.progressBar);

        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryAdapter(new ArrayList<>(), this::onCategorySelected);
        rvCategories.setAdapter(adapter);

        initRetrofit();
        fetchItemTypes();
    }

    private void initRetrofit() {
        apiService = new Retrofit.Builder()
                .baseUrl("http://vehiclemarket.runasp.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
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
