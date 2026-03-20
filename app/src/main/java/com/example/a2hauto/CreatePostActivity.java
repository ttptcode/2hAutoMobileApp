package com.example.a2hauto;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
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
    private List<Uri> selectedImageUris = new ArrayList<>();
    private LinearLayout layoutImages;
    private TextView tvSelectedCategory;
    private ApiService apiService;

    private AutoCompleteTextView spinnerCondition, spinnerBrand, spinnerYear, spinnerOrigin, 
            spinnerStyle, spinnerSeats, spinnerColor, spinnerFuel, spinnerGearbox, spinnerOwnerCount;
    private TextInputEditText etModel, etMileage, etPrice, etTitle, etDescription, etAddress;
    private RadioGroup rgSellerType;
    private MaterialButton btnSubmit, btnSaveDraft;
    
    private boolean isEditMode = false;
    private Listing editingListing = null;

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
        findViewById(R.id.btnAddImage).setOnClickListener(v -> openGallery());
        
        btnSubmit.setOnClickListener(v -> submitPost(false));
        btnSaveDraft.setOnClickListener(v -> submitPost(true));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        isEditMode = getIntent().getBooleanExtra("isEditMode", false);
        if (isEditMode) {
            editingListing = (Listing) getIntent().getSerializableExtra("listingData");
            if (editingListing != null) {
                btnSaveDraft.setVisibility(android.view.View.GONE);
                btnSubmit.setText("CẬP NHẬT");
                loadExistingListingData(editingListing);
            }
        }
    }

    private void loadExistingListingData(Listing listing) {
        if (listing == null || listing.getItem() == null) return;
        com.example.a2hauto.model.Item item = listing.getItem();

        etTitle.setText(item.getTitle());
        setSpinnerValue(spinnerBrand, item.getBrand());
        etModel.setText(item.getModel());
        setSpinnerValue(spinnerYear, String.valueOf(item.getYear()));
        etMileage.setText(item.getMileage());
        setSpinnerValue(spinnerStyle, item.getStyle());
        setSpinnerValue(spinnerColor, item.getColor());
        setSpinnerValue(spinnerSeats, item.getSeat());
        setSpinnerValue(spinnerFuel, item.getFuel());
        setSpinnerValue(spinnerGearbox, item.getGearbox());
        setSpinnerValue(spinnerOwnerCount, item.getOwnerCount());
        setSpinnerValue(spinnerOrigin, item.getOrigin());
        setSpinnerValue(spinnerCondition, item.getCondition());

        if (listing.getBuyNowPrice() > 0) etPrice.setText(String.valueOf((long) listing.getBuyNowPrice()));
        etDescription.setText(listing.getDetail());
        etAddress.setText(listing.getAddress());
    }

    private void setSpinnerValue(AutoCompleteTextView spinner, String value) {
        if (value != null) spinner.setText(value, false);
    }

    private void initViews() {
        layoutImages = findViewById(R.id.layoutImages);
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

    private void setSpinnerAdapter(AutoCompleteTextView s, String[] items) {
        s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, items));
    }

    private void initRetrofit() {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder().addInterceptor(new AuthInterceptor(this)).build();
        apiService = new Retrofit.Builder().baseUrl("http://vehiclemarket.runasp.net/").client(client).addConverterFactory(GsonConverterFactory.create()).build().create(ApiService.class);
    }

    private void openGallery() {
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setType("image/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_REQUEST && data != null) {
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) addImagePreview(data.getClipData().getItemAt(i).getUri());
            } else if (data.getData() != null) addImagePreview(data.getData());
        }
    }

    private void addImagePreview(Uri uri) {
        if (selectedImageUris.size() >= 10) return;
        selectedImageUris.add(uri);
        ImageButton btn = new ImageButton(this);
        btn.setLayoutParams(new LinearLayout.LayoutParams(250, 250));
        btn.setScaleType(ImageButton.ScaleType.CENTER_CROP);
        btn.setImageURI(uri);
        layoutImages.addView(btn, layoutImages.getChildCount() - 1);
    }

    private void submitPost(boolean isDraft) {
        if (!isEditMode && selectedImageUris.isEmpty()) { Toast.makeText(this, "Chọn ít nhất 1 ảnh", Toast.LENGTH_SHORT).show(); return; }
        
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang lưu...");

        Map<String, RequestBody> fields = new HashMap<>();
        String typeId = getIntent().getStringExtra("itemTypeId");
        fields.put("ItemTypeId", createPart(typeId != null ? typeId : "e6aee7cb-dff5-41a5-8cce-55f174768daa"));

        if (isEditMode && editingListing != null) {
            fields.put("ListingId", createPart(editingListing.getListingId()));
            fields.put("ItemId", createPart(editingListing.getItem().getItemId()));
            fields.put("SerialNumber", createPart(editingListing.getItem().getSerialNumber()));
        } else {
            fields.put("SerialNumber", createPart("CAR_" + System.currentTimeMillis()));
        }

        fields.put("Title", createPart(etTitle.getText().toString()));
        fields.put("Brand", createPart(spinnerBrand.getText().toString()));
        fields.put("Model", createPart(etModel.getText().toString()));
        fields.put("Year", createPart(spinnerYear.getText().toString()));
        fields.put("Condition", createPart(spinnerCondition.getText().toString()));
        fields.put("Price", createPart(etPrice.getText().toString()));
        fields.put("BuyNowPrice", createPart(etPrice.getText().toString()));
        fields.put("Mileage", createPart(etMileage.getText().toString()));
        fields.put("Origin", createPart(spinnerOrigin.getText().toString()));
        fields.put("Style", createPart(spinnerStyle.getText().toString()));
        fields.put("Seat", createPart(spinnerSeats.getText().toString()));
        fields.put("Color", createPart(spinnerColor.getText().toString()));
        fields.put("Fuel", createPart(spinnerFuel.getText().toString()));
        fields.put("Gearbox", createPart(spinnerGearbox.getText().toString()));
        fields.put("OwnerCount", createPart(spinnerOwnerCount.getText().toString()));
        fields.put("Detail", createPart(etDescription.getText().toString()));
        fields.put("Address", createPart(etAddress.getText().toString()));
        fields.put("ListingType", createPart("0"));
        fields.put("YouAre", createPart(rgSellerType.getCheckedRadioButtonId() == R.id.rbIndividual ? "Cá nhân" : "Bán chuyên"));

        List<MultipartBody.Part> images = new ArrayList<>();
        for (Uri u : selectedImageUris) {
            MultipartBody.Part p = prepareFilePart("Images", u);
            if (p != null) images.add(p);
        }

        Callback<ApiResponse<Listing>> cb = new Callback<ApiResponse<Listing>>() {
            @Override
            public void onResponse(Call<ApiResponse<Listing>> call, Response<ApiResponse<Listing>> response) {
                if (response.isSuccessful()) { 
                    Toast.makeText(CreatePostActivity.this, "Thành công!", Toast.LENGTH_SHORT).show(); 
                    finish(); 
                } else { 
                    btnSubmit.setEnabled(true); btnSubmit.setText("CẬP NHẬT");
                    ErrorHandler.handleErrorResponse(CreatePostActivity.this, response); 
        final MultipartBody.Part finalVideoPart = videoPart;

        if (isDraft) {
            apiService.createListing(fields, imageParts, finalVideoPart).enqueue(new Callback<ApiResponse<Listing>>() {
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
            return;
        }

        PackageSelectionBottomSheet.show(this, packageId -> {
            fields.put("FeeCommissionId", createPartFromString(packageId));
            apiService.createListing(fields, imageParts, finalVideoPart).enqueue(new Callback<ApiResponse<Listing>>() {
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
            public void onFailure(Call<ApiResponse<Listing>> call, Throwable t) { 
                btnSubmit.setEnabled(true); 
                ErrorHandler.handleNetworkError(CreatePostActivity.this, t); 
            }
        };

        if (isEditMode) apiService.updateListingWithItemWithFiles(fields, images, null).enqueue(cb);
        else apiService.createListing(fields, images, null).enqueue(cb);
    }

    private RequestBody createPart(String v) { return RequestBody.create(MultipartBody.FORM, v != null ? v : ""); }

    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        try {
            InputStream is = getContentResolver().openInputStream(fileUri);
            File f = new File(getCacheDir(), "up_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream os = new FileOutputStream(f);
            byte[] buf = new byte[1024]; int r;
            while ((r = is.read(buf)) != -1) os.write(buf, 0, r);
            os.close(); is.close();
            return MultipartBody.Part.createFormData(partName, f.getName(), RequestBody.create(MediaType.parse("image/jpeg"), f));
        } catch (Exception e) { return null; }
    }
}
