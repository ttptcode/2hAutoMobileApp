package com.example.a2hauto;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.a2hauto.api.ApiClient;
import com.example.a2hauto.api.ApiService;
import com.example.a2hauto.auth.AuthSessionManager;
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.FeeCommission;
import com.example.a2hauto.model.UserPackage;
import com.example.a2hauto.model.UserProfile;
import com.example.a2hauto.model.UserProfileResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvProfileFullName;
    private TextView tvProfileNameValue;
    private TextView tvProfileEmailValue;
    private TextView tvProfilePhoneValue;
    private TextView tvProfileCreatedAtValue;
    private TextView tabPersonalInfo;
    private TextView tabYourPackages;
    private View indicatorPersonalInfo;
    private View indicatorPackages;
    private View layoutPersonalInfo;
    private View layoutPackages;
    private View layoutEmptyPackage;
    private View layoutActivePackage;
    private MaterialButton btnViewPackages;
    private MaterialButton btnUpgradePackage;
    private MaterialButton btnUpdateProfile;
    private TextView tvPackageName;
    private TextView tvPackageDesc;
    private TextView tvRemainingListings;
    private TextView tvPackagePrice;
    private TextView tvActivationDate;
    private TextView tvExpirationDate;

    private AuthSessionManager authSessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvProfileFullName = findViewById(R.id.tvProfileFullName);
        tvProfileNameValue = findViewById(R.id.tvProfileNameValue);
        tvProfileEmailValue = findViewById(R.id.tvProfileEmailValue);
        tvProfilePhoneValue = findViewById(R.id.tvProfilePhoneValue);
        tvProfileCreatedAtValue = findViewById(R.id.tvProfileCreatedAtValue);
        tabPersonalInfo = findViewById(R.id.tabPersonalInfo);
        tabYourPackages = findViewById(R.id.tabYourPackages);
        indicatorPersonalInfo = findViewById(R.id.indicatorPersonalInfo);
        indicatorPackages = findViewById(R.id.indicatorPackages);
        layoutPersonalInfo = findViewById(R.id.layoutPersonalInfo);
        layoutPackages = findViewById(R.id.layoutPackages);
        layoutEmptyPackage = findViewById(R.id.layoutEmptyPackage);
        layoutActivePackage = findViewById(R.id.layoutActivePackage);
        btnViewPackages = findViewById(R.id.btnViewPackages);
        btnUpgradePackage = findViewById(R.id.btnUpgradePackage);
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile);
        tvPackageName = findViewById(R.id.tvPackageName);
        tvPackageDesc = findViewById(R.id.tvPackageDesc);
        tvRemainingListings = findViewById(R.id.tvRemainingListings);
        tvPackagePrice = findViewById(R.id.tvPackagePrice);
        tvActivationDate = findViewById(R.id.tvActivationDate);
        tvExpirationDate = findViewById(R.id.tvExpirationDate);

        authSessionManager = new AuthSessionManager(this);
        apiService = ApiClient.getApiService(this);

        // Fill basic values first for better perceived loading.
        String fallbackName = authSessionManager.getDisplayName();
        String fallbackPhone = authSessionManager.getPhoneNumber();
        tvProfileFullName.setText(TextUtils.isEmpty(fallbackName) ? "Chưa cập nhật" : fallbackName);
        tvProfileNameValue.setText(TextUtils.isEmpty(fallbackName) ? "Chưa cập nhật" : fallbackName);
        tvProfileEmailValue.setText("Chưa cập nhật");
        tvProfilePhoneValue.setText(TextUtils.isEmpty(fallbackPhone) ? "Chưa cập nhật" : fallbackPhone);
        tvProfileCreatedAtValue.setText("Chưa cập nhật");

        setupTabs();
        btnUpdateProfile.setOnClickListener(v -> showUpdateProfileDialog());
        btnViewPackages.setOnClickListener(v -> startActivity(new Intent(this, PlanActivity.class)));
        btnUpgradePackage.setOnClickListener(v -> startActivity(new Intent(this, PlanActivity.class)));
        loadUserProfile();
        fetchUserPackages();
    }

    private void showUpdateProfileDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update_profile, null, false);
        TextInputLayout tilPhone = dialogView.findViewById(R.id.tilDialogPhone);
        TextInputLayout tilPassword = dialogView.findViewById(R.id.tilDialogPassword);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etDialogPhone);
        TextInputEditText etPassword = dialogView.findViewById(R.id.etDialogPassword);
        TextView tvRuleLength = dialogView.findViewById(R.id.tvRuleLength);
        TextView tvRuleLowercase = dialogView.findViewById(R.id.tvRuleLowercase);
        TextView tvRuleUppercase = dialogView.findViewById(R.id.tvRuleUppercase);
        TextView tvRuleDigit = dialogView.findViewById(R.id.tvRuleDigit);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
        MaterialButton btnUpdate = dialogView.findViewById(R.id.btnDialogUpdate);

        String currentPhone = tvProfilePhoneValue.getText() == null
                ? ""
                : tvProfilePhoneValue.getText().toString().trim();
        if (TextUtils.isEmpty(currentPhone) || "Chưa cập nhật".equalsIgnoreCase(currentPhone)) {
            currentPhone = authSessionManager.getPhoneNumber();
        }
        etPhone.setText(currentPhone);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        TextWatcher formWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            String phoneValue = etPhone.getText() == null ? "" : etPhone.getText().toString().trim();
            String passwordValue = etPassword.getText() == null ? "" : etPassword.getText().toString();

            updatePasswordRuleViews(passwordValue,
                        tvRuleLength, tvRuleLowercase, tvRuleUppercase, tvRuleDigit, false);

            boolean isPhoneValid = isValidVietnamesePhoneForUpdate(phoneValue);
            boolean isPasswordValid = TextUtils.isEmpty(passwordValue) || isValidPasswordInputForUpdate(passwordValue);
            boolean canUpdate = isPhoneValid && isPasswordValid;
            btnUpdate.setEnabled(canUpdate);
            btnUpdate.setAlpha(canUpdate ? 1f : 0.5f);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        etPhone.addTextChangedListener(formWatcher);
        etPassword.addTextChangedListener(formWatcher);

        updatePasswordRuleViews(etPassword.getText() == null ? "" : etPassword.getText().toString(),
            tvRuleLength, tvRuleLowercase, tvRuleUppercase, tvRuleDigit, false);
        boolean initialPhoneValid = isValidVietnamesePhoneForUpdate(etPhone.getText() == null
            ? ""
            : etPhone.getText().toString().trim());
        boolean initialPasswordValid = TextUtils.isEmpty(etPassword.getText())
            || isValidPasswordInputForUpdate(etPassword.getText().toString());
        boolean initialCanUpdate = initialPhoneValid && initialPasswordValid;
        btnUpdate.setEnabled(initialCanUpdate);
        btnUpdate.setAlpha(initialCanUpdate ? 1f : 0.5f);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnUpdate.setOnClickListener(v -> {
            String phoneInput = etPhone.getText() == null ? "" : etPhone.getText().toString().trim();
            String passwordInput = etPassword.getText() == null ? "" : etPassword.getText().toString();

            // Clear previous errors before running validation.
            tilPhone.setError(null);
            tilPassword.setError(null);

            if (TextUtils.isEmpty(phoneInput)) {
                tilPhone.setError("Số điện thoại không được để trống");
                return;
            }
            if (!phoneInput.matches("^0\\d{9}$")) {
                tilPhone.setError("Số điện thoại không hợp lệ");
                return;
            }

            if (!TextUtils.isEmpty(passwordInput)) {
                boolean validLength = passwordInput.length() >= 8 && passwordInput.length() <= 32;
                boolean hasLowercase = passwordInput.matches(".*[a-z].*");
                boolean hasUppercase = passwordInput.matches(".*[A-Z].*");
                boolean hasDigit = passwordInput.matches(".*\\d.*");

                updatePasswordRuleViews(passwordInput,
                        tvRuleLength, tvRuleLowercase, tvRuleUppercase, tvRuleDigit, true);

                if (!validLength || !hasLowercase || !hasUppercase || !hasDigit) {
                    tilPassword.setError("Mật khẩu chưa đạt đủ điều kiện");
                    return;
                }
            }

            // TODO: call PUT /api/Auth/update-phone-password with
            // userId = authSessionManager.getUserId(), phone = phoneInput, password = passwordInput.

            if (!TextUtils.isEmpty(phoneInput)) {
                tvProfilePhoneValue.setText(phoneInput);
            }
            Toast.makeText(this, "Validate thành công, chuẩn bị gọi API", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private boolean isValidVietnamesePhoneForUpdate(String phone) {
        return !TextUtils.isEmpty(phone) && phone.matches("^0\\d{9}$");
    }

    private boolean isValidPasswordInputForUpdate(String password) {
        if (TextUtils.isEmpty(password)) {
            return true;
        }

        boolean validLength = password.length() >= 8 && password.length() <= 32;
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        return validLength && hasLowercase && hasUppercase && hasDigit;
    }

    private void updatePasswordRuleViews(String password,
                                         TextView tvRuleLength,
                                         TextView tvRuleLowercase,
                                         TextView tvRuleUppercase,
                                         TextView tvRuleDigit,
                                         boolean highlightInvalid) {
        boolean isEmpty = TextUtils.isEmpty(password);
        boolean validLength = !isEmpty && password.length() >= 8 && password.length() <= 32;
        boolean hasLowercase = !isEmpty && password.matches(".*[a-z].*");
        boolean hasUppercase = !isEmpty && password.matches(".*[A-Z].*");
        boolean hasDigit = !isEmpty && password.matches(".*\\d.*");

        int neutralColor = ContextCompat.getColor(this, R.color.text_secondary);
        int validColor = ContextCompat.getColor(this, R.color.success_green);
        int invalidColor = ContextCompat.getColor(this, android.R.color.holo_red_dark);

        if (isEmpty && !highlightInvalid) {
            tvRuleLength.setTextColor(neutralColor);
            tvRuleLowercase.setTextColor(neutralColor);
            tvRuleUppercase.setTextColor(neutralColor);
            tvRuleDigit.setTextColor(neutralColor);
            return;
        }

        tvRuleLength.setTextColor(validLength ? validColor : invalidColor);
        tvRuleLowercase.setTextColor(hasLowercase ? validColor : invalidColor);
        tvRuleUppercase.setTextColor(hasUppercase ? validColor : invalidColor);
        tvRuleDigit.setTextColor(hasDigit ? validColor : invalidColor);
    }

    private void setupTabs() {
        tabPersonalInfo.setOnClickListener(v -> showPersonalInfoTab());
        tabYourPackages.setOnClickListener(v -> showPackagesTab());
        showPersonalInfoTab();
    }

    private void showPersonalInfoTab() {
        layoutPersonalInfo.setVisibility(View.VISIBLE);
        layoutPackages.setVisibility(View.GONE);

        tabPersonalInfo.setTextColor(ContextCompat.getColor(this, R.color.primary_teal_dark));
        tabYourPackages.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        indicatorPersonalInfo.setVisibility(View.VISIBLE);
        indicatorPackages.setVisibility(View.INVISIBLE);
    }

    private void showPackagesTab() {
        layoutPersonalInfo.setVisibility(View.GONE);
        layoutPackages.setVisibility(View.VISIBLE);

        tabPersonalInfo.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tabYourPackages.setTextColor(ContextCompat.getColor(this, R.color.primary_teal_dark));

        indicatorPersonalInfo.setVisibility(View.INVISIBLE);
        indicatorPackages.setVisibility(View.VISIBLE);
    }

    private void loadUserProfile() {
        String userId = authSessionManager.getUserId();
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, R.string.favorite_login_required, Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getUserProfile(userId).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ProfileActivity.this, "Không thể tải hồ sơ", Toast.LENGTH_SHORT).show();
                    return;
                }

                UserProfileResponse body = response.body();
                if (!body.isSuccess() || body.getData() == null) {
                    String message = TextUtils.isEmpty(body.getMessage()) ? "Không thể tải hồ sơ" : body.getMessage();
                    Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                    return;
                }

                bindUserProfile(body.getData());
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindUserProfile(UserProfile profile) {
        String fullName = profile.getFullName();
        String email = profile.getEmail();
        String phone = profile.getPhoneNumber();
        if (TextUtils.isEmpty(phone)) {
            phone = authSessionManager.getPhoneNumber();
        }
        String formattedCreatedAt = formatCreatedAtVietnamese(profile.getCreatedAt());

        tvProfileFullName.setText(TextUtils.isEmpty(fullName) ? "Chưa cập nhật" : fullName);
        tvProfileNameValue.setText(TextUtils.isEmpty(fullName) ? "Chưa cập nhật" : fullName);
        tvProfileEmailValue.setText(TextUtils.isEmpty(email) ? "Chưa cập nhật" : email);
        tvProfilePhoneValue.setText(TextUtils.isEmpty(phone) ? "Chưa cập nhật" : phone);
        tvProfileCreatedAtValue.setText(TextUtils.isEmpty(formattedCreatedAt) ? "Chưa cập nhật" : formattedCreatedAt);
    }

    private void fetchUserPackages() {
        String token = authSessionManager.getAuthToken();
        if (TextUtils.isEmpty(token)) {
            showEmptyPackageState();
            return;
        }

        // AuthInterceptor sẽ tự động thêm token vào header
        apiService.getActiveUserPackages().enqueue(new Callback<ApiResponse<List<UserPackage>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<UserPackage>>> call, Response<ApiResponse<List<UserPackage>>> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    showEmptyPackageState();
                    return;
                }

                List<UserPackage> list = response.body().getData();
                if (list == null || list.isEmpty()) {
                    showEmptyPackageState();
                    return;
                }

                UserPackage pkg = list.get(0);
                FeeCommission fee = pkg.getFeeCommission();
                if (fee == null) {
                    showEmptyPackageState();
                    return;
                }

                layoutActivePackage.setVisibility(View.VISIBLE);
                layoutEmptyPackage.setVisibility(View.GONE);

                tvPackageName.setText(TextUtils.isEmpty(fee.getFeeName()) ? "Gói đăng tin" : fee.getFeeName());
                tvPackageDesc.setText(TextUtils.isEmpty(fee.getDescription()) ? "" : fee.getDescription());
                tvRemainingListings.setText(pkg.getRemainingListings() + "/" + fee.getMaxListings());
                tvPackagePrice.setText(String.format(new Locale("vi", "VN"), "%,.0f VNĐ", pkg.getTotalAmount()));
                tvActivationDate.setText("Kích hoạt: " + formatDate(pkg.getActivatedAt()));
                tvExpirationDate.setText(formatDate(pkg.getExpiredAt()));
            }

            @Override
            public void onFailure(Call<ApiResponse<List<UserPackage>>> call, Throwable t) {
                showEmptyPackageState();
            }
        });
    }

    private void showEmptyPackageState() {
        layoutEmptyPackage.setVisibility(View.VISIBLE);
        layoutActivePackage.setVisibility(View.GONE);
    }

    private String formatCreatedAtVietnamese(String createdAt) {
        if (TextUtils.isEmpty(createdAt)) {
            return "";
        }

        String[] patterns = new String[] {
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss"
        };

        Date parsedDate = null;
        for (String pattern : patterns) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat(pattern, Locale.US);
                parser.setLenient(false);
                if (pattern.endsWith("'Z'")) {
                    parser.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                parsedDate = parser.parse(createdAt);
                if (parsedDate != null) {
                    break;
                }
            } catch (ParseException ignored) {
                // Try next pattern.
            }
        }

        if (parsedDate == null) {
            return "";
        }

        SimpleDateFormat output = new SimpleDateFormat("'Lúc' HH:mm dd 'tháng' MM, yyyy", new Locale("vi", "VN"));
        return output.format(parsedDate);
    }

    private String formatDate(String isoDate) {
        if (TextUtils.isEmpty(isoDate)) {
            return "--/--/----";
        }

        String[] patterns = new String[] {
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat input = new SimpleDateFormat(pattern, Locale.US);
                input.setLenient(false);
                if (pattern.endsWith("'Z'")) {
                    input.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                Date date = input.parse(isoDate);
                if (date != null) {
                    return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
                }
            } catch (ParseException ignored) {
            }
        }

        return "--/--/----";
    }
}
