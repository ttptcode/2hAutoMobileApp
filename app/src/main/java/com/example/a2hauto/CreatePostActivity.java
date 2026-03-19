package com.example.a2hauto;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.a2hauto.api.ApiService;
import com.example.a2hauto.auth.AuthInterceptor;
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.Listing;
import com.example.a2hauto.util.AuthDebugger;
import com.example.a2hauto.util.ErrorHandler;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CreatePostActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_VIDEO_REQUEST = 2;
    
    private List<Uri> selectedImageUris = new ArrayList<>();
    private Uri selectedVideoUri = null;
    
    private LinearLayout layoutImages;
    private TextView tvVideoStatus, tvSelectedCategory;
    private ApiService apiService;

    private AutoCompleteTextView spinnerCondition, spinnerBrand, spinnerYear, spinnerOrigin, 
            spinnerStyle, spinnerSeats, spinnerColor, spinnerFuel, spinnerGearbox, spinnerOwnerCount;
    private TextInputEditText etModel, etMileage, etPrice, etTitle, etDescription, etAddress;
    private RadioGroup rgSellerType;
    private MaterialButton btnSubmit, btnSaveDraft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        initViews();
        setupSpinners();
        initRetrofit();

        String catName = getIntent().getStringExtra("categoryName");
        tvSelectedCategory.setText(catName != null ? catName : "Ô tô");

        findViewById(R.id.cardSelectCategory).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddImage).setOnClickListener(v -> openGallery(PICK_IMAGE_REQUEST));
        findViewById(R.id.btnAddVideo).setOnClickListener(v -> openGallery(PICK_VIDEO_REQUEST));
        
        btnSubmit.setOnClickListener(v -> submitPost(false));
        btnSaveDraft.setOnClickListener(v -> submitPost(true));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        layoutImages = findViewById(R.id.layoutImages);
        tvVideoStatus = findViewById(R.id.tvVideoStatus);
        tvSelectedCategory = findViewById(R.id.tvSelectedCategory);
        
        spinnerCondition = findViewById(R.id.spinnerCondition);
        spinnerBrand = findViewById(R.id.spinnerBrand);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerOrigin = findViewById(R.id.spinnerOrigin);
        spinnerStyle = findViewById(R.id.spinnerStyle);
        spinnerSeats = findViewById(R.id.spinnerSeats);
        spinnerColor = findViewById(R.id.spinnerColor);
        spinnerFuel = findViewById(R.id.spinnerFuel);
        spinnerGearbox = findViewById(R.id.spinnerGearbox);
        spinnerOwnerCount = findViewById(R.id.spinnerOwnerCount);
        
        etModel = findViewById(R.id.etModel);
        etMileage = findViewById(R.id.etMileage);
        etPrice = findViewById(R.id.etPrice);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etAddress = findViewById(R.id.etAddress);
        rgSellerType = findViewById(R.id.rgSellerType);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnSaveDraft = findViewById(R.id.btnSaveDraft);
    }

    private void setupSpinners() {
        setSpinnerAdapter(spinnerCondition, new String[]{"Mới", "Đã qua sử dụng"});
        setSpinnerAdapter(spinnerBrand, new String[]{"Toyota", "Honda", "Mazda", "Hyundai", "Kia", "VinFast", "Mercedes Benz", "BMW", "Audi"});
        setSpinnerAdapter(spinnerYear, new String[]{"2025", "2024", "2023", "2022", "2021", "2020", "2019", "2018", "2017", "2016"});
        setSpinnerAdapter(spinnerOrigin, new String[]{"Việt Nam", "Thái Lan", "Nhật Bản", "Hàn Quốc", "Mỹ", "Đức"});
        setSpinnerAdapter(spinnerStyle, new String[]{"Sedan", "SUV / Crossover", "Hatchback", "Bán tải", "MPV"});
        setSpinnerAdapter(spinnerSeats, new String[]{"2", "4", "5", "7", "9", "16"});
        setSpinnerAdapter(spinnerColor, new String[]{"Trắng", "Đen", "Xám", "Bạc", "Đỏ", "Xanh dương", "Vàng"});
        setSpinnerAdapter(spinnerFuel, new String[]{"Xăng", "Dầu", "Điện", "Hybrid"});
        setSpinnerAdapter(spinnerGearbox, new String[]{"Số tự động", "Số sàn", "Bán tự động"});
        setSpinnerAdapter(spinnerOwnerCount, new String[]{"Chủ đầu tiên", "2 chủ", "3 chủ", "4 chủ trở lên"});
    }

    private void setSpinnerAdapter(AutoCompleteTextView spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, items);
        spinner.setAdapter(adapter);
    }

    private void initRetrofit() {
        // Tạo OkHttpClient với AuthInterceptor
        okhttp3.OkHttpClient.Builder httpClient = new okhttp3.OkHttpClient.Builder();
        httpClient.addInterceptor(new AuthInterceptor(this));

        apiService = new Retrofit.Builder()
                .baseUrl("http://vehiclemarket.runasp.net/")
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }

    private void openGallery(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(requestCode == PICK_IMAGE_REQUEST ? "image/*" : "video/*");
        if (requestCode == PICK_IMAGE_REQUEST) intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                if (data.getClipData() != null) {
                    for (int i = 0; i < data.getClipData().getItemCount(); i++) addImagePreview(data.getClipData().getItemAt(i).getUri());
                } else if (data.getData() != null) addImagePreview(data.getData());
            } else if (requestCode == PICK_VIDEO_REQUEST) {
                selectedVideoUri = data.getData();
                tvVideoStatus.setText("Đã chọn video");
            }
        }
    }

    private void addImagePreview(Uri uri) {
        if (selectedImageUris.size() >= 20) return;
        selectedImageUris.add(uri);
        ImageButton imgBtn = new ImageButton(this);
        imgBtn.setLayoutParams(new LinearLayout.LayoutParams(250, 250));
        imgBtn.setScaleType(ImageButton.ScaleType.CENTER_CROP);
        imgBtn.setImageURI(uri);
        layoutImages.addView(imgBtn, layoutImages.getChildCount() - 1);
    }

    private void submitPost(boolean isDraft) {
        // Debug authentication status
        AuthDebugger.debugAuthStatus(this);

        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 hình ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isDraft) {
            if (etTitle.getText().toString().isEmpty() || spinnerBrand.getText().toString().isEmpty() || etModel.getText().toString().isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ các trường bắt buộc (*)", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        btnSubmit.setEnabled(false);
        btnSaveDraft.setEnabled(false);
        btnSubmit.setText(isDraft ? "Đang lưu nháp..." : "Đang xử lý...");

        Map<String, RequestBody> fields = new HashMap<>();
        String itemTypeId = getIntent().getStringExtra("itemTypeId");
        fields.put("ItemTypeId", createPartFromString(itemTypeId != null ? itemTypeId : "e6aee7cb-dff5-41a5-8cce-55f174768daa"));
        fields.put("SerialNumber", createPartFromString("CAR_" + System.currentTimeMillis()));
        
        String titleValue = etTitle.getText().toString().trim();
        fields.put("Title", createPartFromString(titleValue.isEmpty() ? "Chưa cập nhật" : titleValue));
        
        String brandValue = spinnerBrand.getText().toString().trim();
        fields.put("Brand", createPartFromString(brandValue.isEmpty() ? "Khác" : brandValue));
        
        String modelValue = etModel.getText().toString().trim();
        fields.put("Model", createPartFromString(modelValue.isEmpty() ? "Chưa cập nhật" : modelValue));
        
        String yearValue = spinnerYear.getText().toString().trim();
        fields.put("Year", createPartFromString(yearValue.isEmpty() ? "2024" : yearValue));
        
        String conditionValue = spinnerCondition.getText().toString().trim();
        fields.put("Condition", createPartFromString(conditionValue.isEmpty() ? "Đã qua sử dụng" : conditionValue));
        
        String priceValue = etPrice.getText().toString().trim();
        String finalPrice = priceValue.isEmpty() ? "0" : priceValue;
        fields.put("Price", createPartFromString(finalPrice));
        fields.put("BuyNowPrice", createPartFromString(finalPrice));
        
        String mileageValue = etMileage.getText().toString().trim();
        fields.put("Mileage", createPartFromString(mileageValue.isEmpty() ? "0" : mileageValue));
        
        String originValue = spinnerOrigin.getText().toString().trim();
        fields.put("Origin", createPartFromString(originValue.isEmpty() ? "Việt Nam" : originValue));
        
        String styleValue = spinnerStyle.getText().toString().trim();
        fields.put("Style", createPartFromString(styleValue.isEmpty() ? "Sedan" : styleValue));
        
        String seatValue = spinnerSeats.getText().toString().trim();
        fields.put("Seat", createPartFromString(seatValue.isEmpty() ? "5" : seatValue));
        
        String colorValue = spinnerColor.getText().toString().trim();
        fields.put("Color", createPartFromString(colorValue.isEmpty() ? "Trắng" : colorValue));
        
        String fuelValue = spinnerFuel.getText().toString().trim();
        fields.put("Fuel", createPartFromString(fuelValue.isEmpty() ? "Xăng" : fuelValue));
        
        String gearboxValue = spinnerGearbox.getText().toString().trim();
        fields.put("Gearbox", createPartFromString(gearboxValue.isEmpty() ? "Số tự động" : gearboxValue));
        
        String ownerCountValue = spinnerOwnerCount.getText().toString().trim();
        fields.put("OwnerCount", createPartFromString(ownerCountValue.isEmpty() ? "Chủ đầu tiên" : ownerCountValue));
        
        fields.put("ListingType", createPartFromString("0"));
        
        String descriptionValue = etDescription.getText().toString().trim();
        fields.put("Detail", createPartFromString(descriptionValue.isEmpty() ? "Chưa cập nhật" : descriptionValue));
        
        String addressValue = etAddress.getText().toString().trim();
        fields.put("Address", createPartFromString(addressValue.isEmpty() ? "Chưa cập nhật" : addressValue));
        
        String sellerType = rgSellerType.getCheckedRadioButtonId() == R.id.rbIndividual ? "Cá nhân" : "Bán chuyên";
        fields.put("YouAre", createPartFromString(sellerType));

        List<MultipartBody.Part> imageParts = new ArrayList<>();
        for (Uri uri : selectedImageUris) {
            MultipartBody.Part part = prepareFilePart("Images", uri);
            if (part != null) imageParts.add(part);
        }

        if (imageParts.isEmpty()) {
            resetButtons();
            Toast.makeText(this, "Lỗi xử lý hình ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        MultipartBody.Part videoPart = null;
        if (selectedVideoUri != null) {
            videoPart = prepareFilePart("Video", selectedVideoUri);
        }

        apiService.createListing(fields, imageParts, videoPart).enqueue(new Callback<ApiResponse<Listing>>() {
            @Override
            public void onResponse(Call<ApiResponse<Listing>> call, Response<ApiResponse<Listing>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String listingId = response.body().getData().getListingId();
                    if (isDraft) {
                        finishSuccess("Đã lưu bản nháp thành công!");
                    } else {
                        activateListing(listingId);
                    }
                } else {
                    resetButtons();
                    ErrorHandler.handleErrorResponse(CreatePostActivity.this, response);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Listing>> call, Throwable t) {
                resetButtons();
                ErrorHandler.handleNetworkError(CreatePostActivity.this, t);
            }
        });
    }

    private void activateListing(String listingId) {
        apiService.toggleStatus(listingId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    finishSuccess("Đăng tin thành công!");
                } else {
                    finishSuccess("Bài đăng đã tạo ở chế độ Nháp (Lỗi kích hoạt)");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                finishSuccess("Bài đăng đã tạo ở chế độ Nháp (Lỗi kết nối)");
            }
        });
    }

    private void resetButtons() {
        btnSubmit.setEnabled(true);
        btnSaveDraft.setEnabled(true);
        btnSubmit.setText("ĐĂNG TIN NGAY");
    }

    private void finishSuccess(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        finish();
    }

    private RequestBody createPartFromString(String value) {
        return RequestBody.create(MultipartBody.FORM, value != null ? value : "");
    }

    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            if (inputStream == null) return null;
            
            File file = new File(getCacheDir(), "upload_" + System.currentTimeMillis());
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, read);
            outputStream.close();
            inputStream.close();
            
            RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(fileUri)), file);
            return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
