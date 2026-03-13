package com.example.a2hauto.adapter;

import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.a2hauto.DetailActivity;
import com.example.a2hauto.R;
import com.example.a2hauto.model.Item;
import com.example.a2hauto.model.Listing;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {
    private List<Listing> listings;

    public VehicleAdapter(List<Listing> listings) {
        this.listings = listings;
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vehicle, parent, false);
        return new VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        Listing listing = listings.get(position);
        Item item = listing.getItem();
        
        holder.tvName.setText(listing.getDisplayTitle());
        
        // Format Price
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.tvPrice.setText(formatter.format(listing.getBuyNowPrice()));

        // Status/Condition Tag
        String condition = "Đang mở bán";
        if (item != null && item.getCondition() != null && !item.getCondition().trim().isEmpty()) {
            condition = item.getCondition();
        }
        holder.tvConditionTag.setText(condition);
        
        // Specs: Year • Type • Fuel
        List<String> specs = new ArrayList<>();
        if (item != null) {
            if (item.getYear() != null) specs.add(String.valueOf(item.getYear()));
            if (!TextUtils.isEmpty(item.getItemTypeName())) specs.add(item.getItemTypeName());
            if (!TextUtils.isEmpty(item.getFuel())) specs.add(item.getFuel());
        }
        holder.tvSpecs.setText(specs.isEmpty() ? "Xe đang cập nhật thông số" : TextUtils.join(" • ", specs));

        String listingType = listing.getListingType();
        holder.tvListingType.setText(!TextUtils.isEmpty(listingType) ? listingType : holder.itemView.getContext().getString(R.string.listing_type_fallback));

        String sellerName = listing.getUserName();
        if (!TextUtils.isEmpty(sellerName)) {
            holder.tvSeller.setText(holder.itemView.getContext().getString(R.string.seller_prefix, sellerName));
        } else {
            holder.tvSeller.setText(R.string.seller_fallback);
        }

        List<String> summaryParts = new ArrayList<>();
        if (item != null) {
            if (!TextUtils.isEmpty(item.getBrand())) summaryParts.add(item.getBrand());
            if (!TextUtils.isEmpty(item.getMileage())) summaryParts.add(item.getMileage() + " km");
            if (!TextUtils.isEmpty(item.getGearbox())) summaryParts.add(item.getGearbox());
            if (!TextUtils.isEmpty(item.getColor())) summaryParts.add(item.getColor());
        }
        holder.tvSummary.setText(summaryParts.isEmpty() ? "Xe đang cập nhật thêm mô tả chi tiết cho mẫu này." : TextUtils.join(" • ", summaryParts));

        // Address
        holder.tvAddress.setText(listing.getAddress() != null ? listing.getAddress() : "Liên hệ người bán");

        // Image
        String imageUrl = null;
        if (item != null && item.getImageUrls() != null && !item.getImageUrls().isEmpty()) {
            imageUrl = item.getImageUrls().get(0);
        }

        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivVehicle);

        // Click event to show detail
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), DetailActivity.class);
            intent.putExtra("listing", listing);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return listings == null ? 0 : listings.size();
    }

    public void setListings(List<Listing> listings) {
        List<Listing> oldListings = this.listings == null ? new ArrayList<>() : new ArrayList<>(this.listings);
        List<Listing> newListings = listings == null ? new ArrayList<>() : new ArrayList<>(listings);

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldListings.size();
            }

            @Override
            public int getNewListSize() {
                return newListings.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                String oldId = oldListings.get(oldItemPosition).getListingId();
                String newId = newListings.get(newItemPosition).getListingId();
                return !TextUtils.isEmpty(oldId) && oldId.equals(newId);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Listing oldItem = oldListings.get(oldItemPosition);
                Listing newItem = newListings.get(newItemPosition);

                return TextUtils.equals(oldItem.getListingId(), newItem.getListingId())
                        && TextUtils.equals(oldItem.getDisplayTitle(), newItem.getDisplayTitle())
                        && oldItem.getBuyNowPrice() == newItem.getBuyNowPrice()
                        && TextUtils.equals(oldItem.getStatus(), newItem.getStatus())
                        && TextUtils.equals(oldItem.getAddress(), newItem.getAddress())
                        && TextUtils.equals(oldItem.getListingType(), newItem.getListingType())
                        && TextUtils.equals(oldItem.getUserName(), newItem.getUserName());
            }
        });

        this.listings = newListings;
        diffResult.dispatchUpdatesTo(this);
    }

    public static class VehicleViewHolder extends RecyclerView.ViewHolder {
        ImageView ivVehicle;
        TextView tvName, tvPrice, tvConditionTag, tvSpecs, tvAddress, tvSeller, tvListingType, tvSummary;

        public VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            ivVehicle = itemView.findViewById(R.id.ivVehicle);
            tvName = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvConditionTag = itemView.findViewById(R.id.tvConditionTag);
            tvSpecs = itemView.findViewById(R.id.tvSpecs);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvSeller = itemView.findViewById(R.id.tvSeller);
            tvListingType = itemView.findViewById(R.id.tvListingType);
            tvSummary = itemView.findViewById(R.id.tvSummary);
        }
    }
}
