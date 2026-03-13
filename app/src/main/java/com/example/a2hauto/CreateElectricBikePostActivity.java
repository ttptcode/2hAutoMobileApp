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
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.Listing;
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

public class CreateElectricBikePostActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_VIDEO_REQUEST = 2;
    
    private List<Uri> selectedImageUris = new ArrayList<>();
    private Uri selectedVideoUri = null;
    
    private LinearLayout layoutImages;
    private TextView tvVideoStatus, tvSelectedCategory;
    private ApiService apiService;

    private AutoCompleteTextView spinnerCondition, spinnerBrand, spinnerVehicleType, spinnerMotor, spinnerOrigin, spinnerWarranty;
    private TextInputEditText etBatteryCapacity, etColor, etPrice, etTitle, etDescription, etAddress;
    private RadioGroup rgSellerType;
    private MaterialButton btnSubmit, btnSaveDraft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_electric_bike_post);

        initViews();
        setupSpinners();
        initRetrofit();

        String catName = getIntent().getStringExtra("categoryName");
        tvSelectedCategory.setText(catName != null ? catName : "Xe điện");

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
        spinnerVehicleType = findViewById(R.id.spinnerVehicleType);
        spinnerMotor = findViewById(R.id.spinnerMotor);
        spinnerOrigin = findViewById(R.id.spinnerOrigin);
        spinnerWarranty = findViewById(R.id.spinnerWarranty);
        
        etBatteryCapacity = findViewById(R.id.etBatteryCapacity);
        etColor = findViewById(R.id.etColor);
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
        setSpinnerAdapter(spinnerBrand, new String[]{"Aima", "Anbico", "Asama", "BMX", "Bridgestone", "Detech", "Dibao", "DKBike", "Giant", "Hitasa", "HK bike", "Honda", "Hyundai", "Jett", "Kingda", "MBI", "Nijia", "Nioshima", "Pega", "Sonsu", "Suzika", "Trinx", "VinFast", "Xiaomi", "Xmen", "Yadea", "Yamaha", "Zoomer", "Khác"});
        setSpinnerAdapter(spinnerVehicleType, new String[]{"Xe đạp điện", "Xe máy điện", "Xe điện khác"});
        setSpinnerAdapter(spinnerMotor, new String[]{"< 200W", "200 - 250W", "251 - 350 W", "351 - 500 W", "501 - 1000 W", "> 1000 W"});
        setSpinnerAdapter(spinnerOrigin, new String[]{"Nhật Bản", "Đài Loan", "Trung Quốc", "Mỹ", "Pháp", "Đức", "Ý", "Lắp ráp tại Việt Nam", "Thái Lan"});
        setSpinnerAdapter(spinnerWarranty, new String[]{"Hết bảo hành", "< 3 tháng", "3 tháng", "6 tháng", "12 tháng", "18 tháng", "24 tháng", "> 24 tháng"});
    }

    private void setSpinnerAdapter(AutoCompleteTextView spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, items);
        spinner.setAdapter(adapter);
    }

    private void initRetrofit() {
        apiService = new Retrofit.Builder()
                .baseUrl("http://vehiclemarket.runasp.net/")
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
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 hình ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isDraft) {
            if (etTitle.getText().toString().isEmpty() || etPrice.getText().toString().isEmpty() || spinnerBrand.getText().toString().isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ các trường bắt buộc (*)", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        btnSubmit.setEnabled(false);
        btnSaveDraft.setEnabled(false);
        btnSubmit.setText(isDraft ? "Đang lưu nháp..." : "Đang xử lý...");

        Map<String, RequestBody> fields = new HashMap<>();
        String itemTypeId = getIntent().getStringExtra("itemTypeId");
        fields.put("ItemTypeId", createPartFromString(itemTypeId != null ? itemTypeId : "9b4e23e4-c621-480f-96b8-4a04194af47f"));
        fields.put("SerialNumber", createPartFromString("EBIKE_" + System.currentTimeMillis()));
        fields.put("Title", createPartFromString(etTitle.getText().toString()));
        fields.put("Brand", createPartFromString(spinnerBrand.getText().toString()));
        fields.put("VehicleType", createPartFromString(spinnerVehicleType.getText().toString()));
        fields.put("MotorPower", createPartFromString(spinnerMotor.getText().toString()));
        fields.put("BatteryCapacity", createPartFromString(etBatteryCapacity.getText().toString()));
        fields.put("Condition", createPartFromString(spinnerCondition.getText().toString()));
        fields.put("Origin", createPartFromString(spinnerOrigin.getText().toString()));
        fields.put("Color", createPartFromString(etColor.getText().toString()));
        fields.put("Warranty", createPartFromString(spinnerWarranty.getText().toString()));
        fields.put("Price", createPartFromString(etPrice.getText().toString()));
        fields.put("BuyNowPrice", createPartFromString(etPrice.getText().toString()));
        fields.put("Detail", createPartFromString(etDescription.getText().toString()));
        fields.put("Address", createPartFromString(etAddress.getText().toString()));
        fields.put("ListingType", createPartFromString("0"));
        
        String sellerType = rgSellerType.getCheckedRadioButtonId() == R.id.rbIndividual ? "Cá nhân" : "Bán chuyên";
        fields.put("YouAre", createPartFromString(sellerType));

        List<MultipartBody.Part> imageParts = new ArrayList<>();
        for (Uri uri : selectedImageUris) imageParts.add(prepareFilePart("Images", uri));
        MultipartBody.Part videoPart = selectedVideoUri != null ? prepareFilePart("Video", selectedVideoUri) : null;

        apiService.createListing(fields, imageParts, videoPart).enqueue(new Callback<ApiResponse<Listing>>() {
            @Override
            public void onResponse(Call<ApiResponse<Listing>> call, Response<ApiResponse<Listing>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String listingId = response.body().getData().getListingId();
                    if (isDraft) {
                        finishSuccess("Đã lưu bản nháp xe điện thành công!");
                    } else {
                        activateListing(listingId);
                    }
                } else {
                    resetButtons();
                    Toast.makeText(CreateElectricBikePostActivity.this, "Lỗi tạo bài đăng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Listing>> call, Throwable t) {
                resetButtons();
                Toast.makeText(CreateElectricBikePostActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void activateListing(String listingId) {
        apiService.toggleStatus(listingId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    finishSuccess("Đăng tin xe điện thành công!");
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
            File file = new File(getCacheDir(), "upload_" + System.currentTimeMillis());
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, read);
            outputStream.close();
            RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(fileUri)), file);
            return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
        } catch (Exception e) { return null; }
    }
}
