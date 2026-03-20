package com.example.a2hauto;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a2hauto.adapter.PlanAdapter;
import com.example.a2hauto.api.ApiClient;
import com.example.a2hauto.api.ApiService;
import com.example.a2hauto.auth.AuthSessionManager;
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.FeeCommission;
import com.example.a2hauto.model.FeeCommissionResponse;
import com.example.a2hauto.model.PaymentRequest;
import com.example.a2hauto.model.PaymentResponse;
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
    private AuthSessionManager authSessionManager;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plans);

        rvPlans = findViewById(R.id.rvPlans);
        progressBar = findViewById(R.id.progressBar);
        ImageButton btnBack = findViewById(R.id.btnBack);

        rvPlans.setLayoutManager(new LinearLayoutManager(this));
        rvPlans.setHasFixedSize(true);

        authSessionManager = new AuthSessionManager(this);
        apiService = ApiClient.getApiService(this);
        fetchPackages();

        btnBack.setOnClickListener(v -> finish());
    }

    private void fetchPackages() {
        String token = authSessionManager.getAuthToken();
        if (token == null || token.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Debug log
        android.util.Log.d("PlanActivity", "Token: " + token);

        // AuthInterceptor sẽ tự động thêm token vào header
        apiService.getPackages().enqueue(new Callback<FeeCommissionResponse>() {
            @Override
            public void onResponse(Call<FeeCommissionResponse> call, Response<FeeCommissionResponse> response) {
                // Debug log
                android.util.Log.d("PlanActivity", "Response code: " + response.code());

                if (!response.isSuccessful()) {
                    String errorMessage = "Lỗi HTTP: " + response.code();
                    
                    // Handle 401 Unauthorized
                    if (response.code() == 401) {
                        errorMessage = "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.";
                        authSessionManager.clearToken();
                        finish();
                    } else if (response.code() == 403) {
                        errorMessage = "Bạn không có quyền truy cập.";
                    } else {
                        try {
                            // Cố gắng lấy error message từ response body
                            String errorBody = response.errorBody() != null ? 
                                response.errorBody().string() : "";
                            android.util.Log.d("PlanActivity", "Error body: " + errorBody);
                            if (!errorBody.isEmpty()) {
                                errorMessage = errorBody;
                            }
                        } catch (Exception e) {
                            android.util.Log.e("PlanActivity", "Error parsing error body", e);
                        }
                    }
                    
                    Toast.makeText(PlanActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    if (response.code() != 401) {
                        return;
                    }
                    return;
                }

                if (response.body() == null) {
                    Toast.makeText(PlanActivity.this, "Không thể lấy dữ liệu từ server", Toast.LENGTH_SHORT).show();
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
                android.util.Log.e("PlanActivity", "Network error", t);
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
        RadioGroup radioGroup = view.findViewById(R.id.rgDuration);
        MaterialButton btnPay = view.findViewById(R.id.btnPay);

        tvPlanName.setText(plan.getFeeName());
        tvPlanDescription.setText(plan.getDescription());

        double basePrice = plan.getAmount();
        rbOption1.setText(String.format(Locale.getDefault(), "1 tháng - %,.0f VNĐ", basePrice));
        rbOption3.setText(String.format(Locale.getDefault(), "3 tháng - %,.0f VNĐ", basePrice * 3));
        rbOption6.setText(String.format(Locale.getDefault(), "6 tháng - %,.0f VNĐ", basePrice * 6));
        rbOption1.setChecked(true);

        btnPay.setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            int months = 1;
            
            if (selectedId == rbOption3.getId()) {
                months = 3;
            } else if (selectedId == rbOption6.getId()) {
                months = 6;
            }

            double amount = basePrice * months;
            PaymentRequest paymentRequest = new PaymentRequest(amount, plan.getFeeId());
            
            progressBar.setVisibility(View.VISIBLE);
            btnPay.setEnabled(false);
            
            createPayment(paymentRequest, btnPay, bottomSheetDialog);
        });

        bottomSheetDialog.show();
    }

    private void createPayment(PaymentRequest request, MaterialButton btnPay, BottomSheetDialog dialog) {
        String token = authSessionManager.getAuthToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            btnPay.setEnabled(true);
            return;
        }

        // AuthInterceptor sẽ tự động thêm token vào header
        apiService.createPayment(request)
                .enqueue(new Callback<ApiResponse<PaymentResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<PaymentResponse>> call, 
                                         Response<ApiResponse<PaymentResponse>> response) {
                        progressBar.setVisibility(View.GONE);
                        btnPay.setEnabled(true);

                        // Handle unsuccessful responses
                        if (!response.isSuccessful()) {
                            String errorMessage = "Lỗi HTTP: " + response.code();
                            
                            if (response.code() == 401) {
                                errorMessage = "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.";
                                authSessionManager.clearToken();
                            } else if (response.code() == 403) {
                                errorMessage = "Bạn không có quyền thực hiện hành động này.";
                            } else if (response.code() == 400) {
                                errorMessage = "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.";
                            }
                            
                            Toast.makeText(PlanActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            return;
                        }

                        if (response.body() == null) {
                            Toast.makeText(PlanActivity.this, "Không thể lấy dữ liệu từ server", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        ApiResponse<PaymentResponse> apiResponse = response.body();
                        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                            String paymentUrl = apiResponse.getData().getPaymentUrl();
                            if (paymentUrl != null && !paymentUrl.isEmpty()) {
                                dialog.dismiss();
                                // Mở PaymentActivity để xử lý thanh toán trong app
                                Intent intent = new Intent(PlanActivity.this, PaymentActivity.class);
                                intent.putExtra("paymentUrl", paymentUrl);
                                startActivity(intent);
                            } else {
                                Toast.makeText(PlanActivity.this, "Không thể lấy URL thanh toán", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String errorMsg = apiResponse.getMessage() != null ? 
                                apiResponse.getMessage() : "Không thể tạo thanh toán";
                            Toast.makeText(PlanActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<PaymentResponse>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnPay.setEnabled(true);
                        android.util.Log.e("PlanActivity", "Payment error", t);
                        Toast.makeText(PlanActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
