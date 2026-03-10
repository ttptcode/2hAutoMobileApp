package com.example.a2hauto.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.a2hauto.DetailActivity;
import com.example.a2hauto.R;
import com.example.a2hauto.model.Listing;
import java.text.NumberFormat;
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
        
        holder.tvName.setText(listing.getDisplayTitle());
        
        // Format Price
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.tvPrice.setText(formatter.format(listing.getBuyNowPrice()));

        // Status/Condition Tag
        String condition = "N/A";
        if (listing.getItem() != null && listing.getItem().getCondition() != null) {
            condition = listing.getItem().getCondition();
        }
        holder.tvConditionTag.setText(condition);
        
        // Specs: Year • Type • Fuel
        StringBuilder specs = new StringBuilder();
        if (listing.getItem() != null) {
            if (listing.getItem().getYear() != null) specs.append(listing.getItem().getYear()).append(" • ");
            if (listing.getItem().getItemTypeName() != null) specs.append(listing.getItem().getItemTypeName()).append(" • ");
            if (listing.getItem().getFuel() != null) specs.append(listing.getItem().getFuel());
        }
        String specsStr = specs.toString();
        if (specsStr.endsWith(" • ")) specsStr = specsStr.substring(0, specsStr.length() - 3);
        holder.tvSpecs.setText(specsStr);

        // Address
        holder.tvAddress.setText(listing.getAddress() != null ? listing.getAddress() : "Liên hệ người bán");

        // Image
        String imageUrl = null;
        if (listing.getItem() != null && listing.getItem().getImageUrls() != null && !listing.getItem().getImageUrls().isEmpty()) {
            imageUrl = listing.getItem().getImageUrls().get(0);
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
        this.listings = listings;
        notifyDataSetChanged();
    }

    static class VehicleViewHolder extends RecyclerView.ViewHolder {
        ImageView ivVehicle;
        TextView tvName, tvPrice, tvConditionTag, tvSpecs, tvAddress;

        public VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            ivVehicle = itemView.findViewById(R.id.ivVehicle);
            tvName = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvConditionTag = itemView.findViewById(R.id.tvConditionTag);
            tvSpecs = itemView.findViewById(R.id.tvSpecs);
            tvAddress = itemView.findViewById(R.id.tvAddress);
        }
    }
}
