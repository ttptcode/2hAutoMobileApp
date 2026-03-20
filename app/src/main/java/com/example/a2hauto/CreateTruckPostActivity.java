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
    
    private boolean isEditMode = false;
    private Listing editingListing = null;

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

        // Check if in edit mode
        isEditMode = getIntent().getBooleanExtra("isEditMode", false);
        if (isEditMode) {
              editingListing = (Listing) getIntent().getSerializableExtra("listingData");
            if (editingListing != null) {
                findViewById(R.id.cardSelectCategory).setEnabled(false);
                tvSelectedCategory.setAlpha(0.5f);
                btnSaveDraft.setVisibility(android.view.View.GONE);
                btnSubmit.setText("CẬP NHẬT");
                loadExistingListingData(editingListing);
            }
        }
    }

    private void loadExistingListingData(Listing listing) {
        if (listing == null || listing.getItem() == null) return;

        com.example.a2hauto.model.Item item = listing.getItem();

        // Load basic fields
        if (item.getTitle() != null) etTitle.setText(item.getTitle());

        // Load truck-specific fields
        if (item.getBrand() != null) setSpinnerValue(spinnerBrand, item.getBrand());

        String weight = getItemStringField(item, "getWeight");
        if (weight != null) setSpinnerValue(spinnerPayload, weight);

        String capacity = getItemStringField(item, "getCapacity");
        if (capacity != null) etCapacity.setText(capacity);

        if (item.getFuel() != null) setSpinnerValue(spinnerFuel, item.getFuel());
        if (item.getLicensePlate() != null) etLicensePlate.setText(item.getLicensePlate());
        if (item.getMileage() != null) etMileage.setText(item.getMileage());
        if (item.getYear() != null) setSpinnerValue(spinnerYear, String.valueOf(item.getYear()));
        if (item.getOrigin() != null) setSpinnerValue(spinnerOrigin, item.getOrigin());
        if (item.getColor() != null) setSpinnerValue(spinnerColor, item.getColor());
        if (item.getCondition() != null) setSpinnerValue(spinnerCondition, item.getCondition());

        // Load price
        if (listing.getBuyNowPrice() > 0) {
            etPrice.setText(String.valueOf((long) listing.getBuyNowPrice()));
        }

        // Load description and address
        if (listing.getDetail() != null) etDescription.setText(listing.getDetail());
        if (listing.getAddress() != null) etAddress.setText(listing.getAddress());
    }

    private void setSpinnerValue(AutoCompleteTextView spinner, String value) {
        if (value != null) {
            spinner.setText(value, false);
        }
    }

    private String getItemStringField(com.example.a2hauto.model.Item item, String getterName) {
        try {
            java.lang.reflect.Method method = item.getClass().getMethod(getterName);
            Object value = method.invoke(item);
            return value != null ? String.valueOf(value) : null;
        } catch (Exception e) {
            return null;
        }
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

        if (!isEditMode && selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 hình ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isDraft) {
            if (etTitle.getText().toString().isEmpty() || spinnerBrand.getText().toString().isEmpty() || spinnerPayload.getText().toString().isEmpty()) {
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

        // Add IDs for edit mode
        if (isEditMode && editingListing != null) {
            if (editingListing.getListingId() != null) {
                fields.put("ListingId", createPartFromString(editingListing.getListingId()));
            }
            if (editingListing.getItem() != null && editingListing.getItem().getItemId() != null) {
                fields.put("ItemId", createPartFromString(editingListing.getItem().getItemId()));
            }
        }

        fields.put("SerialNumber", createPartFromString("TRUCK_" + System.currentTimeMillis()));
        
        String titleValue = etTitle.getText().toString().trim();
        fields.put("Title", createPartFromString(titleValue.isEmpty() ? "Chưa cập nhật" : titleValue));
        
        String brandValue = spinnerBrand.getText().toString().trim();
        fields.put("Brand", createPartFromString(brandValue.isEmpty() ? "Khác" : brandValue));
        
        String weightValue = spinnerPayload.getText().toString().trim();
        fields.put("Weight", createPartFromString(weightValue.isEmpty() ? "Dưới 500 kg" : weightValue));
        
        String capacityValue = etCapacity.getText().toString().trim();
        fields.put("Capacity", createPartFromString(capacityValue.isEmpty() ? "0" : capacityValue));
        
        String yearValue = spinnerYear.getText().toString().trim();
        fields.put("Year", createPartFromString(yearValue.isEmpty() ? "2024" : yearValue));
        
        String fuelValue = spinnerFuel.getText().toString().trim();
        fields.put("Fuel", createPartFromString(fuelValue.isEmpty() ? "Dầu" : fuelValue));
        
        String conditionValue = spinnerCondition.getText().toString().trim();
        fields.put("Condition", createPartFromString(conditionValue.isEmpty() ? "Đã qua sử dụng" : conditionValue));
        
        String originValue = spinnerOrigin.getText().toString().trim();
        fields.put("Origin", createPartFromString(originValue.isEmpty() ? "Việt Nam" : originValue));
        
        String colorValue = spinnerColor.getText().toString().trim();
        fields.put("Color", createPartFromString(colorValue.isEmpty() ? "Đen" : colorValue));
        
        String licensePlateValue = etLicensePlate.getText().toString().trim();
        fields.put("LicensePlate", createPartFromString(licensePlateValue.isEmpty() ? "Chưa cập nhật" : licensePlateValue));
        
        String priceValue = etPrice.getText().toString().trim();
        String finalPrice = priceValue.isEmpty() ? "0" : priceValue;
        fields.put("Price", createPartFromString(finalPrice));
        fields.put("BuyNowPrice", createPartFromString(finalPrice));
        
        String mileageValue = etMileage.getText().toString().trim();
        fields.put("Mileage", createPartFromString(mileageValue.isEmpty() ? "0" : mileageValue));
        
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

        if (!isEditMode && imageParts.isEmpty()) {
            resetButtons();
            Toast.makeText(this, "Lỗi xử lý hình ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        MultipartBody.Part videoPart = null;
        if (selectedVideoUri != null) {
            videoPart = prepareFilePart("Video", selectedVideoUri);
        }

        if (isEditMode) {
            apiService.updateListingWithItemWithFiles(fields, imageParts, videoPart).enqueue(new Callback<ApiResponse<Listing>>() {
                @Override
                public void onResponse(Call<ApiResponse<Listing>> call, Response<ApiResponse<Listing>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        finishSuccess("Cập nhật bài đăng thành công!");
                    } else {
                        resetButtons();
                        ErrorHandler.handleErrorResponse(CreateTruckPostActivity.this, response);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Listing>> call, Throwable t) {
                    resetButtons();
                    ErrorHandler.handleNetworkError(CreateTruckPostActivity.this, t);
                }
            });
        } else {
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
                        ErrorHandler.handleErrorResponse(CreateTruckPostActivity.this, response);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Listing>> call, Throwable t) {
                    resetButtons();
                    ErrorHandler.handleNetworkError(CreateTruckPostActivity.this, t);
                }
            });
        }
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
