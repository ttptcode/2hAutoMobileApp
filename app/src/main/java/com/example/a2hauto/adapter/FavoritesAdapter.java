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
import androidx.recyclerview.widget.DiffUtil;
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
        void onToggleFavorite(Listing listing);

        void onSelectionChanged(int selectedCount, boolean isSelectionMode);
    }

    private final FavoriteActionListener listener;
    private List<Listing> listings;
    private final Set<String> selectedListingIds = new HashSet<>();
    private boolean selectionMode = false;

    public FavoritesAdapter(List<Listing> listings, FavoriteActionListener listener) {
        this.listings = listings == null ? new ArrayList<>() : listings;
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

        String condition = "Đang mở bán";
        if (item != null && !TextUtils.isEmpty(item.getCondition())) {
            condition = item.getCondition();
        }
        holder.tvConditionTag.setText(condition);

        List<String> specs = new ArrayList<>();
        if (item != null) {
            if (item.getYear() != null) {
                specs.add(String.valueOf(item.getYear()));
            }
            if (!TextUtils.isEmpty(item.getItemTypeName())) {
                specs.add(item.getItemTypeName());
            }
            if (!TextUtils.isEmpty(item.getFuel())) {
                specs.add(item.getFuel());
            }
        }
        holder.tvSpecs.setText(specs.isEmpty() ? "Xe đang cập nhật thông số" : TextUtils.join(" • ", specs));

        String listingType = listing.getListingType();
        holder.tvListingType.setText(!TextUtils.isEmpty(listingType)
                ? listingType
                : holder.itemView.getContext().getString(R.string.listing_type_fallback));

        String sellerName = listing.getUserName();
        if (!TextUtils.isEmpty(sellerName)) {
            holder.tvSeller.setText(holder.itemView.getContext().getString(R.string.seller_prefix, sellerName));
        } else {
            holder.tvSeller.setText(R.string.seller_fallback);
        }

        List<String> summaryParts = new ArrayList<>();
        if (item != null) {
            if (!TextUtils.isEmpty(item.getBrand())) {
                summaryParts.add(item.getBrand());
            }
            if (!TextUtils.isEmpty(item.getMileage())) {
                summaryParts.add(item.getMileage() + " km");
            }
            if (!TextUtils.isEmpty(item.getGearbox())) {
                summaryParts.add(item.getGearbox());
            }
            if (!TextUtils.isEmpty(item.getColor())) {
                summaryParts.add(item.getColor());
            }
        }
        holder.tvSummary.setText(summaryParts.isEmpty()
                ? "Xe đang cập nhật thêm mô tả chi tiết cho mẫu này."
                : TextUtils.join(" • ", summaryParts));

        holder.tvAddress.setText(!TextUtils.isEmpty(listing.getAddress()) ? listing.getAddress() : "Liên hệ người bán");

        String imageUrl = null;
        if (item != null && item.getImageUrls() != null && !item.getImageUrls().isEmpty()) {
            imageUrl = item.getImageUrls().get(0);
        }

        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivVehicle);

        boolean isSelected = !TextUtils.isEmpty(listingId) && selectedListingIds.contains(listingId);
        holder.cbSelectFavorite.setOnCheckedChangeListener(null);
        holder.cbSelectFavorite.setChecked(isSelected);

        if (selectionMode) {
            holder.cbSelectFavorite.setVisibility(View.VISIBLE);
            holder.ivFavoriteBadge.setVisibility(View.GONE);
        } else {
            holder.cbSelectFavorite.setVisibility(View.GONE);
            holder.ivFavoriteBadge.setVisibility(View.VISIBLE);
        }

        holder.ivFavoriteBadge.setImageResource(R.drawable.ic_heart_filled);
        holder.ivFavoriteBadge.setOnClickListener(v -> {
            if (!selectionMode && listener != null) {
                listener.onToggleFavorite(listing);
            }
        });

        holder.cbSelectFavorite.setOnCheckedChangeListener((buttonView, checked) -> {
            if (TextUtils.isEmpty(listingId)) {
                return;
            }
            if (checked) {
                selectedListingIds.add(listingId);
            } else {
                selectedListingIds.remove(listingId);
            }
            dispatchSelectionChanged();
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                selectionMode = true;
                if (!TextUtils.isEmpty(listingId)) {
                    selectedListingIds.add(listingId);
                }
                notifyDataSetChanged();
                dispatchSelectionChanged();
                return true;
            }
            return false;
        });

        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                holder.cbSelectFavorite.setChecked(!holder.cbSelectFavorite.isChecked());
                return;
            }

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
        selectedListingIds.retainAll(getListingIdSet(newListings));
        diffResult.dispatchUpdatesTo(this);
        dispatchSelectionChanged();
    }

    public void setSelectionMode(boolean enabled) {
        selectionMode = enabled;
        if (!enabled) {
            selectedListingIds.clear();
        }
        notifyDataSetChanged();
        dispatchSelectionChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public Set<String> getSelectedListingIds() {
        return new HashSet<>(selectedListingIds);
    }

    private void dispatchSelectionChanged() {
        if (listener != null) {
            listener.onSelectionChanged(selectedListingIds.size(), selectionMode);
        }
    }

    private Set<String> getListingIdSet(List<Listing> source) {
        Set<String> ids = new HashSet<>();
        for (Listing listing : source) {
            if (listing != null && !TextUtils.isEmpty(listing.getListingId())) {
                ids.add(listing.getListingId());
            }
        }
        return ids;
    }

    public static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        ImageView ivVehicle;
        ImageView ivFavoriteBadge;
        AppCompatCheckBox cbSelectFavorite;
        TextView tvName;
        TextView tvPrice;
        TextView tvConditionTag;
        TextView tvSpecs;
        TextView tvAddress;
        TextView tvSeller;
        TextView tvListingType;
        TextView tvSummary;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            ivVehicle = itemView.findViewById(R.id.ivVehicle);
            ivFavoriteBadge = itemView.findViewById(R.id.ivFavoriteBadge);
            cbSelectFavorite = itemView.findViewById(R.id.cbSelectFavorite);
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
