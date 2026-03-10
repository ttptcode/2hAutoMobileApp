package com.example.a2hauto;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.a2hauto.model.Listing;
import com.example.a2hauto.model.Item;
import java.text.NumberFormat;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Listing listing = (Listing) getIntent().getSerializableExtra("listing");
        if (listing == null) {
            finish();
            return;
        }

        ImageView ivDetail = findViewById(R.id.ivDetail);
        TextView tvDetailName = findViewById(R.id.tvDetailName);
        TextView tvDetailPrice = findViewById(R.id.tvDetailPrice);
        TextView tvDetailSpecs = findViewById(R.id.tvDetailSpecs);
        TextView tvDetailDesc = findViewById(R.id.tvDetailDesc);
        TextView tvDetailSeller = findViewById(R.id.tvDetailSeller);

        tvDetailName.setText(listing.getDisplayTitle());
        
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvDetailPrice.setText(formatter.format(listing.getBuyNowPrice()));

        StringBuilder specs = new StringBuilder();
        Item item = listing.getItem();
        if (item != null) {
            specs.append("Loại: ").append(item.getItemTypeName()).append("\n");
            specs.append("Thương hiệu: ").append(item.getBrand()).append("\n");
            specs.append("Mẫu xe: ").append(item.getModel()).append("\n");
            specs.append("Năm sản xuất: ").append(item.getYear()).append("\n");
            specs.append("Tình trạng: ").append(item.getCondition()).append("\n");
            specs.append("Số KM: ").append(item.getMileage()).append("\n");
            specs.append("Nhiên liệu: ").append(item.getFuel()).append("\n");
            specs.append("Hộp số: ").append(item.getGearbox()).append("\n");
            specs.append("Màu sắc: ").append(item.getColor()).append("\n");
            specs.append("Số chỗ: ").append(item.getSeat()).append("\n");
            specs.append("Xuất xứ: ").append(item.getOrigin()).append("\n");
            specs.append("Biển số: ").append(item.getLicensePlate());
        }
        tvDetailSpecs.setText(specs.toString());

        tvDetailDesc.setText(listing.getDetail() != null ? listing.getDetail() : "Không có mô tả chi tiết.");
        tvDetailSeller.setText("Người đăng: " + listing.getUserName() + "\nĐịa chỉ: " + listing.getAddress());

        String imageUrl = null;
        if (item != null && item.getImageUrls() != null && !item.getImageUrls().isEmpty()) {
            imageUrl = item.getImageUrls().get(0);
        }

        Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(ivDetail);
    }
}
