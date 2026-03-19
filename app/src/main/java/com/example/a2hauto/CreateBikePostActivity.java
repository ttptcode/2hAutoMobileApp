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

public class CreateBikePostActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_VIDEO_REQUEST = 2;
    
    private List<Uri> selectedImageUris = new ArrayList<>();
    private Uri selectedVideoUri = null;
    
    private LinearLayout layoutImages;
    private TextView tvVideoStatus, tvSelectedCategory;
    private ApiService apiService;

    private AutoCompleteTextView spinnerCondition, spinnerType, spinnerBrand, spinnerOrigin, 
            spinnerColor, spinnerFrameMaterial, spinnerFrameSize, spinnerWarranty;
    private TextInputEditText etPrice, etTitle, etDescription, etAddress;
    private RadioGroup rgSellerType;
    private MaterialButton btnSubmit, btnSaveDraft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_bike_post);

        initViews();
        setupSpinners();
        initRetrofit();

        String catName = getIntent().getStringExtra("categoryName");
        tvSelectedCategory.setText(catName != null ? catName : "Xe đạp");

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
        spinnerType = findViewById(R.id.spinnerType);
        spinnerBrand = findViewById(R.id.spinnerBrand);
        spinnerOrigin = findViewById(R.id.spinnerOrigin);
        spinnerColor = findViewById(R.id.spinnerColor);
        spinnerFrameMaterial = findViewById(R.id.spinnerFrameMaterial);
        spinnerFrameSize = findViewById(R.id.spinnerFrameSize);
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
        setSpinnerAdapter(spinnerType, new String[]{"Xe đạp thể thao", "Xe đạp địa hình (Mountain bike)", "Xe đạp đua (Road bike)", "Xe đạp touring", "Xe đạp đạp thành phố", "Xe đạp gấp", "Xe đạp điện", "Xe đạp trẻ em", "Loại khác"});
        setSpinnerAdapter(spinnerBrand, new String[]{"Ander", "Asama", "Baileys", "Bianchi", "Birdy", "BMC", "Brompton", "Bulls", "Cannondale", "Canyon", "Cervelo", "Colnago", "Cube", "Dahon", "De Rosa", "Felt", "Focus", "Fornix", "Fuji", "Fury", "Galaxy", "Ghost", "Giant", "Gitane", "GT", "Haibike", "Ibis", "Jamis", "Jett", "Kalkhoff", "Kona", "Lapierre", "Look", "Marin", "Martin", "Maruishi", "Merida", "Momentum", "Mongoose", "Niner", "Orbea", "Pacific", "Peugeot", "Phoenix", "Phượng Hoàng", "Pinarello", "Pivot", "Puch", "Raleigh", "Riese & Müller", "Ridley", "Royal baby", "Salsa", "Santa Cruz", "Schwinn", "Scott", "Specialized", "Sportlink", "Starider", "Stitch", "Strongman", "Stromer", "Surly", "Tacke", "Tern", "Thống Nhất", "Time", "Totem", "Trek", "Trinx", "Twitter", "Wilier", "Yeti", "Khác"});
        setSpinnerAdapter(spinnerOrigin, new String[]{"Việt Nam", "Trung Quốc", "Đài Loan", "Nhật Bản", "Hàn Quốc", "Thái Lan", "Mỹ", "Đức", "Pháp", "Ý", "Anh", "Nước khác"});
        setSpinnerAdapter(spinnerColor, new String[]{"Đen", "Trắng", "Đỏ", "Xanh dương", "Xanh lá", "Vàng", "Cam", "Tím", "Hồng", "Xám", "Bạc", "Nâu", "Nhiều màu", "Màu khác"});
        setSpinnerAdapter(spinnerFrameSize, new String[]{"XS (43 cm)", "S (46.5 cm)", "M (50 cm)", "L (55.5 cm)", "XL (58.5 cm)"});
        setSpinnerAdapter(spinnerFrameMaterial, new String[]{"Thép (Steel)", "Hợp kim nhôm (Aluminum)", "Carbon", "Titan (Titanium)", "Hợp kim khác"});
        setSpinnerAdapter(spinnerWarranty, new String[]{"Không bảo hành", "1 tháng", "3 tháng", "6 tháng", "12 tháng", "24 tháng", "Trọn đời khung"});
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
            if (etTitle.getText().toString().isEmpty() || spinnerBrand.getText().toString().isEmpty() || spinnerType.getText().toString().isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ các trường bắt buộc (*)", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        btnSubmit.setEnabled(false);
        btnSaveDraft.setEnabled(false);
        btnSubmit.setText(isDraft ? "Đang lưu nháp..." : "Đang xử lý...");

        Map<String, RequestBody> fields = new HashMap<>();
        String itemTypeId = getIntent().getStringExtra("itemTypeId");
        fields.put("ItemTypeId", createPartFromString(itemTypeId != null ? itemTypeId : "fb1da32a-c211-42f4-b8a0-a03c5b82d584"));
        fields.put("SerialNumber", createPartFromString("BIKE_" + System.currentTimeMillis()));
        
        String titleValue = etTitle.getText().toString().trim();
        fields.put("Title", createPartFromString(titleValue.isEmpty() ? "Chưa cập nhật" : titleValue));
        
        String brandValue = spinnerBrand.getText().toString().trim();
        fields.put("Brand", createPartFromString(brandValue.isEmpty() ? "Khác" : brandValue));
        
        String conditionValue = spinnerCondition.getText().toString().trim();
        fields.put("Condition", createPartFromString(conditionValue.isEmpty() ? "Đã qua sử dụng" : conditionValue));
        
        String originValue = spinnerOrigin.getText().toString().trim();
        fields.put("Origin", createPartFromString(originValue.isEmpty() ? "Việt Nam" : originValue));
        
        String colorValue = spinnerColor.getText().toString().trim();
        fields.put("Color", createPartFromString(colorValue.isEmpty() ? "Đen" : colorValue));
        
        String frameMaterialValue = spinnerFrameMaterial.getText().toString().trim();
        fields.put("FrameMaterial", createPartFromString(frameMaterialValue.isEmpty() ? "Thép (Steel)" : frameMaterialValue));
        
        String frameSizeValue = spinnerFrameSize.getText().toString().trim();
        fields.put("FrameSize", createPartFromString(frameSizeValue.isEmpty() ? "M (50 cm)" : frameSizeValue));
        
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
                        finishSuccess("Đã lưu bản nháp xe đạp thành công!");
                    } else {
                        activateListing(listingId);
                    }
                } else {
                    resetButtons();
                    ErrorHandler.handleErrorResponse(CreateBikePostActivity.this, response);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Listing>> call, Throwable t) {
                resetButtons();
                ErrorHandler.handleNetworkError(CreateBikePostActivity.this, t);
            }
        });
    }

    private void activateListing(String listingId) {
        apiService.toggleStatus(listingId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    finishSuccess("Đăng tin xe đạp thành công!");
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
