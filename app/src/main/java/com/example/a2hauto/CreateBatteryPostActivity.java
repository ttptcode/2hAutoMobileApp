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

public class CreateBatteryPostActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_VIDEO_REQUEST = 2;
    
    private List<Uri> selectedImageUris = new ArrayList<>();
    private Uri selectedVideoUri = null;
    
    private LinearLayout layoutImages;
    private TextView tvVideoStatus, tvSelectedCategory;
    private ApiService apiService;

    private AutoCompleteTextView spinnerCondition, spinnerBrand, spinnerType, spinnerVoltage, 
            spinnerCapacity, spinnerOrigin, spinnerColor, spinnerWarranty;
    private TextInputEditText etPrice, etTitle, etDescription, etAddress;
    private RadioGroup rgSellerType;
    private MaterialButton btnSubmit, btnSaveDraft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_battery_post);

        initViews();
        setupSpinners();
        initRetrofit();

        String catName = getIntent().getStringExtra("categoryName");
        tvSelectedCategory.setText(catName != null ? catName : "Ắc quy/ Pin");

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
        spinnerType = findViewById(R.id.spinnerType);
        spinnerVoltage = findViewById(R.id.spinnerVoltage);
        spinnerCapacity = findViewById(R.id.spinnerCapacity);
        spinnerOrigin = findViewById(R.id.spinnerOrigin);
        spinnerColor = findViewById(R.id.spinnerColor);
        spinnerWarranty = findViewById(R.id.spinnerWarranty);
        
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
        setSpinnerAdapter(spinnerBrand, new String[]{"3K", "ACDelco", "Amaron", "Atlasbx", "Banner", "Bosch", "Camel", "Centra", "Century", "Chloride", "CSB", "Daewoo", "Delkor", "Enertec", "Exide", "Fiamm", "Furukawa", "GS Astra", "Hankook", "Hitachi", "Incoe", "Optima", "Panasonic", "Rocket", "Solite", "Tenergy", "Varta", "Vision", "Yuasa", "Hãng khác"});
        setSpinnerAdapter(spinnerType, new String[]{"Ắc quy khô (MF - Maintenance Free)", "Ắc quy nước", "Ắc quy Gel", "Ắc quy AGM", "Ắc quy Lithium", "Ắc quy Graphene", "Pin công nghiệp", "Loại khác"});
        setSpinnerAdapter(spinnerVoltage, new String[]{"6V", "12V", "24V", "36V", "48V", "60V", "72V", "Khác"});
        setSpinnerAdapter(spinnerCapacity, new String[]{"Dưới 20Ah", "20-35Ah", "36-45Ah", "46-60Ah", "61-75Ah", "76-100Ah", "101-150Ah", "151-200Ah", "Trên 200Ah"});
        setSpinnerAdapter(spinnerOrigin, new String[]{"Việt Nam", "Nhật Bản", "Hàn Quốc", "Thái Lan", "Trung Quốc", "Đài Loan", "Ấn Độ", "Đức", "Mỹ", "Nước khác"});
        setSpinnerAdapter(spinnerColor, new String[]{"Đen", "Trắng", "Xám", "Xanh dương", "Xanh lá", "Đỏ", "Vàng", "Cam", "Bạc", "Màu khác"});
        setSpinnerAdapter(spinnerWarranty, new String[]{"Không bảo hành", "3 tháng", "6 tháng", "12 tháng", "18 tháng", "24 tháng", "36 tháng", "48 tháng", "60 tháng"});
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
        if (data == null) return;
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                if (data.getClipData() != null) {
                    for (int i = 0; i < data.getClipData().getItemCount(); i++) addImagePreview(data.getClipData().getItemAt(i).getUri());
                } else if (data.getData() != null) addImagePreview(data.getData());
            } else if (requestCode == PICK_VIDEO_REQUEST) {
                Uri videoUri = data.getData();
                if (videoUri != null) {
                    if (isValidVideoFormat(videoUri)) {
                        selectedVideoUri = videoUri;
                        String videoName = getFileNameFromUri(videoUri);
                        tvVideoStatus.setText("✓ " + videoName);
                        tvVideoStatus.setTextColor(getResources().getColor(R.color.success_green, getTheme()));
                    } else {
                        Toast.makeText(this, "Định dạng video không hỗ trợ. Vui lòng chọn: MP4, AVI, MOV, WMV", Toast.LENGTH_LONG).show();
                        selectedVideoUri = null;
                        tvVideoStatus.setText("Chưa chọn video");
                        tvVideoStatus.setTextColor(getResources().getColor(R.color.text_muted, getTheme()));
                    }
                }
            }
        }
    }

    private boolean isValidVideoFormat(Uri videoUri) {
        String mimeType = getContentResolver().getType(videoUri);
        if (mimeType == null) {
            String fileName = getFileNameFromUri(videoUri);
            return fileName != null && isValidVideoExtension(fileName);
        }
        return mimeType.startsWith("video/");
    }

    private boolean isValidVideoExtension(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".mp4") || 
               lowerName.endsWith(".avi") || 
               lowerName.endsWith(".mov") || 
               lowerName.endsWith(".wmv") ||
               lowerName.endsWith(".mkv") ||
               lowerName.endsWith(".flv") ||
               lowerName.endsWith(".webm");
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME);
                    fileName = cursor.getString(nameIndex);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (fileName == null) {
            fileName = uri.getPath();
            int cut = fileName.lastIndexOf('/');
            if (cut != -1) {
                fileName = fileName.substring(cut + 1);
            }
        }
        return fileName;
    }

    private void addImagePreview(Uri uri) {
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
            if (etTitle.getText().toString().isEmpty() || spinnerBrand.getText().toString().isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ các trường bắt buộc (*)", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        btnSubmit.setEnabled(false);
        btnSaveDraft.setEnabled(false);
        btnSubmit.setText(isDraft ? "Đang lưu nháp..." : "Đang xử lý...");

        Map<String, RequestBody> fields = new HashMap<>();
        String itemTypeId = getIntent().getStringExtra("itemTypeId");
        fields.put("ItemTypeId", createPartFromString(itemTypeId != null ? itemTypeId : "1d653364-a296-4dc0-b8d7-97f6fd1f3a20"));
        fields.put("SerialNumber", createPartFromString("BATT_" + System.currentTimeMillis()));
        
        String titleValue = etTitle.getText().toString().trim();
        fields.put("Title", createPartFromString(titleValue.isEmpty() ? "Chưa cập nhật" : titleValue));
        
        String brandValue = spinnerBrand.getText().toString().trim();
        fields.put("Brand", createPartFromString(brandValue.isEmpty() ? "Khác" : brandValue));
        
        String batteryTypeValue = spinnerType.getText().toString().trim();
        fields.put("BatteryType", createPartFromString(batteryTypeValue.isEmpty() ? "Ắc quy khô (MF - Maintenance Free)" : batteryTypeValue));
        
        String voltageValue = spinnerVoltage.getText().toString().trim();
        fields.put("Voltage", createPartFromString(voltageValue.isEmpty() ? "12V" : voltageValue));
        
        String capacityValue = spinnerCapacity.getText().toString().trim();
        fields.put("Capacity", createPartFromString(capacityValue.isEmpty() ? "Dưới 20Ah" : capacityValue));
        
        String conditionValue = spinnerCondition.getText().toString().trim();
        fields.put("Condition", createPartFromString(conditionValue.isEmpty() ? "Đã qua sử dụng" : conditionValue));
        
        String originValue = spinnerOrigin.getText().toString().trim();
        fields.put("Origin", createPartFromString(originValue.isEmpty() ? "Việt Nam" : originValue));
        
        String colorValue = spinnerColor.getText().toString().trim();
        fields.put("Color", createPartFromString(colorValue.isEmpty() ? "Đen" : colorValue));
        
        String warrantyValue = spinnerWarranty.getText().toString().trim();
        fields.put("Warranty", createPartFromString(warrantyValue.isEmpty() ? "Không bảo hành" : warrantyValue));
        
        String priceValue = etPrice.getText().toString().trim();
        String finalPrice = priceValue.isEmpty() ? "0" : priceValue;
        fields.put("Price", createPartFromString(finalPrice));
        fields.put("BuyNowPrice", createPartFromString(finalPrice));
        
        String descriptionValue = etDescription.getText().toString().trim();
        fields.put("Detail", createPartFromString(descriptionValue.isEmpty() ? "Chưa cập nhật" : descriptionValue));
        
        String addressValue = etAddress.getText().toString().trim();
        fields.put("Address", createPartFromString(addressValue.isEmpty() ? "Chưa cập nhật" : addressValue));
        
        fields.put("ListingType", createPartFromString("0"));
        
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
                    ErrorHandler.handleErrorResponse(CreateBatteryPostActivity.this, response);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Listing>> call, Throwable t) {
                resetButtons();
                ErrorHandler.handleNetworkError(CreateBatteryPostActivity.this, t);
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
            
            // Get MIME type, fallback to extension-based detection
            String mimeType = getContentResolver().getType(fileUri);
            if (mimeType == null) {
                mimeType = getMimeTypeFromExtension(file.getName());
            }
            
            android.util.Log.d("VideoUpload", "File: " + file.getName() + ", MIME: " + mimeType);
            
            MediaType mediaType = MediaType.parse(mimeType != null ? mimeType : "application/octet-stream");
            RequestBody requestFile = RequestBody.create(mediaType, file);
            return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
        } catch (Exception e) {
            android.util.Log.e("VideoUpload", "Error preparing file", e);
            e.printStackTrace();
            return null;
        }
    }

    private String getMimeTypeFromExtension(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".mp4")) return "video/mp4";
        if (lowerName.endsWith(".avi")) return "video/x-msvideo";
        if (lowerName.endsWith(".mov")) return "video/quicktime";
        if (lowerName.endsWith(".wmv")) return "video/x-ms-wmv";
        if (lowerName.endsWith(".mkv")) return "video/x-matroska";
        if (lowerName.endsWith(".flv")) return "video/x-flv";
        if (lowerName.endsWith(".webm")) return "video/webm";
        return "video/mp4";
    }
}
