package com.example.a2hauto.adapter;

import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.a2hauto.DetailActivity;
import com.example.a2hauto.R;
import com.example.a2hauto.model.Item;
import com.example.a2hauto.model.Listing;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {

    public interface FavoriteActionListener {
        void onSelectionChanged(int selectedCount);
        void onFavoriteRemoved(String listingId);
    }

    private final List<Listing> listings = new ArrayList<>();
    private final Set<String> selectedListingIds = new HashSet<>();
    private boolean selectionMode;
    private final FavoriteActionListener listener;

    public FavoritesAdapter(FavoriteActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vehicle, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        Listing listing = listings.get(position);
        Item item = listing.getItem();
        String listingId = listing.getListingId();

        holder.tvName.setText(listing.getDisplayTitle());
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.tvPrice.setText(formatter.format(listing.getBuyNowPrice()));
        holder.tvSpecs.setText(buildSpecs(item));
        holder.tvAddress.setText(!TextUtils.isEmpty(listing.getAddress()) ? listing.getAddress() : "Lien he nguoi ban");
        holder.tvSummary.setText(!TextUtils.isEmpty(listing.getCreatedAt()) ? listing.getCreatedAt() : "");

        String imageUrl = null;
        if (item != null && item.getImageUrls() != null && !item.getImageUrls().isEmpty()) {
            imageUrl = item.getImageUrls().get(0);
        }

        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivVehicle);

        bindSelectionState(holder, listingId);

        holder.ivFavoriteBadge.setOnClickListener(v -> {
            if (selectionMode || TextUtils.isEmpty(listingId)) {
                return;
            }
            if (listener != null) {
                listener.onFavoriteRemoved(listingId);
            }
        });

        holder.cbSelectFavorite.setOnClickListener(v -> {
            if (TextUtils.isEmpty(listingId)) {
                holder.cbSelectFavorite.setChecked(false);
                return;
            }
            if (holder.cbSelectFavorite.isChecked()) {
                selectedListingIds.add(listingId);
            } else {
                selectedListingIds.remove(listingId);
            }
            if (listener != null) {
                listener.onSelectionChanged(selectedListingIds.size());
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                if (TextUtils.isEmpty(listingId)) {
                    return;
                }
                boolean willSelect = !selectedListingIds.contains(listingId);
                if (willSelect) {
                    selectedListingIds.add(listingId);
                } else {
                    selectedListingIds.remove(listingId);
                }
                holder.cbSelectFavorite.setChecked(willSelect);
                if (listener != null) {
                    listener.onSelectionChanged(selectedListingIds.size());
                }
                return;
            }

            Intent intent = new Intent(v.getContext(), DetailActivity.class);
            intent.putExtra("listing", listing);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    public void setListings(List<Listing> newListings) {
        listings.clear();
        if (newListings != null) {
            listings.addAll(newListings);
        }

        Set<String> validIds = new HashSet<>();
        for (Listing listing : listings) {
            if (!TextUtils.isEmpty(listing.getListingId())) {
                validIds.add(listing.getListingId());
            }
        }
        selectedListingIds.retainAll(validIds);

        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(selectedListingIds.size());
        }
    }

    public void setSelectionMode(boolean enabled) {
        selectionMode = enabled;
        if (!selectionMode) {
            selectedListingIds.clear();
            if (listener != null) {
                listener.onSelectionChanged(0);
            }
        }
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public Set<String> getSelectedListingIds() {
        return new HashSet<>(selectedListingIds);
    }

    public void clearSelection() {
        selectedListingIds.clear();
        if (listener != null) {
            listener.onSelectionChanged(0);
        }
        notifyDataSetChanged();
    }

    public void removeListingIds(Set<String> listingIds) {
        if (listingIds == null || listingIds.isEmpty()) {
            return;
        }

        List<Listing> filtered = new ArrayList<>();
        for (Listing listing : listings) {
            if (TextUtils.isEmpty(listing.getListingId()) || !listingIds.contains(listing.getListingId())) {
                filtered.add(listing);
            }
        }

        listings.clear();
        listings.addAll(filtered);
        selectedListingIds.removeAll(listingIds);

        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(selectedListingIds.size());
        }
    }

    private void bindSelectionState(FavoriteViewHolder holder, String listingId) {
        if (selectionMode) {
            holder.cbSelectFavorite.setVisibility(View.VISIBLE);
            holder.ivFavoriteBadge.setVisibility(View.GONE);
            holder.cbSelectFavorite.setChecked(!TextUtils.isEmpty(listingId) && selectedListingIds.contains(listingId));
            return;
        }

        holder.cbSelectFavorite.setVisibility(View.GONE);
        holder.ivFavoriteBadge.setVisibility(View.VISIBLE);
        holder.ivFavoriteBadge.setImageResource(R.drawable.ic_heart_filled);
        holder.ivFavoriteBadge.setImageTintList(null);
    }

    private String buildSpecs(Item item) {
        List<String> specs = new ArrayList<>();
        if (item != null) {
            if (item.getYear() != null) {
                specs.add(String.valueOf(item.getYear()));
            }
            if (!TextUtils.isEmpty(item.getMileage())) {
                specs.add(item.getMileage() + " km");
            }
            if (!TextUtils.isEmpty(item.getGearbox())) {
                specs.add(item.getGearbox());
            }
        }

        return specs.isEmpty() ? "Xe dang cap nhat thong so" : TextUtils.join(" • ", specs);
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        ImageView ivVehicle;
        ImageView ivFavoriteBadge;
        AppCompatCheckBox cbSelectFavorite;
        TextView tvName;
        TextView tvPrice;
        TextView tvSpecs;
        TextView tvAddress;
        TextView tvSummary;

        FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            ivVehicle = itemView.findViewById(R.id.ivVehicle);
            ivFavoriteBadge = itemView.findViewById(R.id.ivFavoriteBadge);
            cbSelectFavorite = itemView.findViewById(R.id.cbSelectFavorite);
            tvName = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvSpecs = itemView.findViewById(R.id.tvSpecs);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvSummary = itemView.findViewById(R.id.tvSummary);
        }
    }
}
