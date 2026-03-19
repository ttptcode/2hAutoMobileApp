package com.example.a2hauto;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.a2hauto.api.ApiClient;
import com.example.a2hauto.api.ApiService;
import com.example.a2hauto.auth.AuthSessionManager;
import com.example.a2hauto.model.UserProfile;
import com.example.a2hauto.model.UserProfileResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

        authSessionManager = new AuthSessionManager(this);
        apiService = ApiClient.getApiService();

        // Fill basic values first for better perceived loading.
        String fallbackName = authSessionManager.getDisplayName();
        String fallbackPhone = authSessionManager.getPhoneNumber();
        tvProfileFullName.setText(TextUtils.isEmpty(fallbackName) ? "Chưa cập nhật" : fallbackName);
        tvProfileNameValue.setText(TextUtils.isEmpty(fallbackName) ? "Chưa cập nhật" : fallbackName);
        tvProfileEmailValue.setText("Chưa cập nhật");
        tvProfilePhoneValue.setText(TextUtils.isEmpty(fallbackPhone) ? "Chưa cập nhật" : fallbackPhone);
        tvProfileCreatedAtValue.setText("Chưa cập nhật");

        loadUserProfile();
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
}
