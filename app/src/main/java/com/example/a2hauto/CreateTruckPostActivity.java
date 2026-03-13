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

public class CreateTruckPostActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_VIDEO_REQUEST = 2;
    
    private List<Uri> selectedImageUris = new ArrayList<>();
    private Uri selectedVideoUri = null;
    
    private LinearLayout layoutImages;
    private TextView tvVideoStatus, tvSelectedCategory;
    private ApiService apiService;

    private AutoCompleteTextView spinnerCondition, spinnerBrand, spinnerPayload, spinnerYear, spinnerFuel, spinnerOrigin, spinnerColor;
    private TextInputEditText etCapacity, etLicensePlate, etMileage, etPrice, etTitle, etDescription, etAddress;
    private RadioGroup rgSellerType;
    private MaterialButton btnSubmit, btnSaveDraft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_truck_post);

        initViews();
        setupSpinners();
        initRetrofit();

        String catName = getIntent().getStringExtra("categoryName");
        tvSelectedCategory.setText(catName != null ? catName : "Xe tải, xe ben");

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
        spinnerPayload = findViewById(R.id.spinnerPayload);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerFuel = findViewById(R.id.spinnerFuel);
        spinnerOrigin = findViewById(R.id.spinnerOrigin);
        spinnerColor = findViewById(R.id.spinnerColor);
        
        etCapacity = findViewById(R.id.etCapacity);
        etLicensePlate = findViewById(R.id.etLicensePlate);
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
        setSpinnerAdapter(spinnerBrand, new String[]{"Chenglong", "Chiến Thắng", "Cửu Long", "Daewoo", "Dongben", "Dongfeng", "Đô Thành", "FAW", "Forcia", "Fusin", "Fuso", "Hino", "Hoa Mai", "Howo", "Hyundai", "Isuzu", "JAC", "Kamaz", "KIA", "Mitsubishi", "Samco", "Shacman", "Sinotruk", "Suzuki", "SYM", "TATA", "Teraco", "Thaco", "Thành Hưng", "TMT", "Trường Giang", "UD Trucks", "Veam", "Vinamotor", "Vinaxuki", "Hãng Khác"});
        setSpinnerAdapter(spinnerPayload, new String[]{"Dưới 500 kg", "500 - 990 kg", "1 - 1.9 tấn", "2 - 2.9 tấn", "3 - 4.9 tấn", "5 - 6.9 tấn", "7 - 9.9 tấn", "10 - 14.9 tấn", "15 - 19.9 tấn", "Trên 20 tấn"});
        setSpinnerAdapter(spinnerYear, new String[]{"2025", "2024", "2023", "2022", "2021", "2020", "2019", "2018", "2017", "2016", "2015", "2014", "2013", "2012", "2011", "2010", "2009", "2008", "2007", "2006", "2005", "2004", "2003", "2002", "2001", "2000", "1999", "1998", "1997", "1996", "1995", "1994", "1993", "1992", "1991", "1990", "Trước năm 1990"});
        setSpinnerAdapter(spinnerFuel, new String[]{"Xăng", "Dầu", "Hybrid"});
        setSpinnerAdapter(spinnerOrigin, new String[]{"Việt Nam", "Ấn Độ", "Hàn Quốc", "Thái Lan", "Nhật Bản", "Trung Quốc", "Mỹ", "Đức", "Thụy Điển", "Nga", "Đài Loan", "Nước khác"});
        setSpinnerAdapter(spinnerColor, new String[]{"Đen", "Trắng", "Đỏ", "Xanh dương", "Xanh lá", "Vàng", "Cam", "Tím", "Hồng", "Xám", "Bạc", "Nâu", "Nhiều màu", "Màu khác"});
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
        if (selectedImageUris.size() >= 20) return;
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
            if (etTitle.getText().toString().isEmpty() || etPrice.getText().toString().isEmpty() || 
                spinnerBrand.getText().toString().isEmpty() || spinnerPayload.getText().toString().isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ các trường bắt buộc (*)", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        btnSubmit.setEnabled(false);
        btnSaveDraft.setEnabled(false);
        btnSubmit.setText(isDraft ? "Đang lưu nháp..." : "Đang xử lý...");

        Map<String, RequestBody> fields = new HashMap<>();
        String itemTypeId = getIntent().getStringExtra("itemTypeId");
        fields.put("ItemTypeId", createPartFromString(itemTypeId != null ? itemTypeId : "805bc39a-6a16-4faf-bca8-2204395eae8f"));
        fields.put("SerialNumber", createPartFromString("TRUCK_" + System.currentTimeMillis()));
        fields.put("Title", createPartFromString(etTitle.getText().toString()));
        fields.put("Brand", createPartFromString(spinnerBrand.getText().toString()));
        fields.put("Weight", createPartFromString(spinnerPayload.getText().toString()));
        fields.put("Capacity", createPartFromString(etCapacity.getText().toString()));
        fields.put("Year", createPartFromString(spinnerYear.getText().toString()));
        fields.put("Fuel", createPartFromString(spinnerFuel.getText().toString()));
        fields.put("Condition", createPartFromString(spinnerCondition.getText().toString()));
        fields.put("Origin", createPartFromString(spinnerOrigin.getText().toString()));
        fields.put("Color", createPartFromString(spinnerColor.getText().toString()));
        fields.put("LicensePlate", createPartFromString(etLicensePlate.getText().toString()));
        fields.put("Price", createPartFromString(etPrice.getText().toString()));
        fields.put("BuyNowPrice", createPartFromString(etPrice.getText().toString()));
        fields.put("Mileage", createPartFromString(etMileage.getText().toString()));
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
                        finishSuccess("Đã lưu bản nháp thành công!");
                    } else {
                        activateListing(listingId);
                    }
                } else {
                    resetButtons();
                    Toast.makeText(CreateTruckPostActivity.this, "Lỗi tạo bài đăng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Listing>> call, Throwable t) {
                resetButtons();
                Toast.makeText(CreateTruckPostActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
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
