package com.example.a2hauto;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a2hauto.adapter.PlanAdapter;
import com.example.a2hauto.api.ApiClient;
import com.example.a2hauto.api.ApiService;
import com.example.a2hauto.auth.AuthSessionManager;
import com.example.a2hauto.model.FeeCommission;
import com.example.a2hauto.model.FeeCommissionResponse;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlanActivity extends AppCompatActivity {

    private RecyclerView rvPlans;
    private PlanAdapter planAdapter;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plans);

        rvPlans = findViewById(R.id.rvPlans);
        ImageButton btnBack = findViewById(R.id.btnBack);

        rvPlans.setLayoutManager(new LinearLayoutManager(this));
        rvPlans.setHasFixedSize(true);

        apiService = ApiClient.getApiService();
        fetchPackages();

        btnBack.setOnClickListener(v -> finish());
    }

    private void fetchPackages() {
        AuthSessionManager authSessionManager = new AuthSessionManager(this);
        String token = authSessionManager.getAuthToken();
        if (token == null || token.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService.getPackages("Bearer " + token).enqueue(new Callback<FeeCommissionResponse>() {
            @Override
            public void onResponse(Call<FeeCommissionResponse> call, Response<FeeCommissionResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(PlanActivity.this, "Lỗi HTTP: " + response.code(), Toast.LENGTH_LONG).show();
                    return;
                }

                FeeCommissionResponse body = response.body();
                if (!body.isSuccess()) {
                    String message = body.getMessage() == null || body.getMessage().trim().isEmpty()
                            ? "Không thể tải danh sách gói"
                            : body.getMessage();
                    Toast.makeText(PlanActivity.this, message, Toast.LENGTH_SHORT).show();
                    return;
                }

                List<FeeCommission> plans = body.getData();
                if (plans != null && !plans.isEmpty()) {
                    planAdapter = new PlanAdapter(plans, PlanActivity.this::showPlanDetailsBottomSheet);
                    rvPlans.setAdapter(planAdapter);
                } else {
                    rvPlans.setAdapter(new PlanAdapter(new ArrayList<>(), null));
                    Toast.makeText(PlanActivity.this, "Hiện chưa có gói đăng tin", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FeeCommissionResponse> call, Throwable t) {
                Toast.makeText(PlanActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPlanDetailsBottomSheet(FeeCommission plan) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_plan_details, null);
        bottomSheetDialog.setContentView(view);

        TextView tvPlanName = view.findViewById(R.id.tvPlanName);
        TextView tvPlanDescription = view.findViewById(R.id.tvPlanDescription);
        RadioButton rbOption1 = view.findViewById(R.id.rbOption1);
        RadioButton rbOption3 = view.findViewById(R.id.rbOption3);
        RadioButton rbOption6 = view.findViewById(R.id.rbOption6);
        MaterialButton btnPay = view.findViewById(R.id.btnPay);

        tvPlanName.setText(plan.getFeeName());
        tvPlanDescription.setText(plan.getDescription());

        double basePrice = plan.getAmount();
        rbOption1.setText(String.format(Locale.getDefault(), "1 tháng - %,.0f VNĐ", basePrice));
        rbOption3.setText(String.format(Locale.getDefault(), "3 tháng - %,.0f VNĐ", basePrice * 3));
        rbOption6.setText(String.format(Locale.getDefault(), "6 tháng - %,.0f VNĐ", basePrice * 6));
        rbOption1.setChecked(true);

        btnPay.setOnClickListener(v -> {
            Toast.makeText(PlanActivity.this, "Tính năng thanh toán đang phát triển", Toast.LENGTH_SHORT).show();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }
}
