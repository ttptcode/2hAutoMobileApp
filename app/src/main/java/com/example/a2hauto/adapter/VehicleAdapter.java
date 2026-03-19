package com.example.a2hauto.adapter;

import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.a2hauto.DetailActivity;
import com.example.a2hauto.R;
import com.example.a2hauto.api.ApiClient;
import com.example.a2hauto.api.ApiService;
import com.example.a2hauto.auth.AuthSessionManager;
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.Item;
import com.example.a2hauto.model.Listing;
import com.example.a2hauto.model.ToggleFavoriteRequest;
import com.google.gson.JsonElement;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.Set;
import java.util.TimeZone;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {
    private List<Listing> listings;
    private final Map<String, Boolean> favoriteState = new HashMap<>();
    private final Set<String> pendingToggleIds = new HashSet<>();
    private ApiService apiService;
    private AuthSessionManager authSessionManager;
    private final Set<String> favoriteListingIds = new HashSet<>();

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

        String condition = !TextUtils.isEmpty(listing.getStatus()) ? listing.getStatus() : "Đang mở bán";
        if ("Active".equalsIgnoreCase(condition)) {
            condition = "Đang mở bán";
        }
        if (item != null && !TextUtils.isEmpty(item.getCondition())) {
            condition = item.getCondition();
        }
        holder.tvConditionTag.setText(condition);

        holder.tvSpecs.setText(buildSpecs(item));
        holder.tvMeta.setText(buildMeta(listing));

        String favoriteKey = getFavoriteKey(listing, position);
        boolean isFavorite = favoriteListingIds.contains(favoriteKey);
        bindFavoriteState(holder, isFavorite);

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

        bindFavoriteBadge(holder, listing);

        holder.btnFavorite.setOnClickListener(v -> {
            boolean currentlyFavorite = favoriteListingIds.contains(favoriteKey);
            if (currentlyFavorite) {
                favoriteListingIds.remove(favoriteKey);
            } else {
                favoriteListingIds.add(favoriteKey);
            }
            bindFavoriteState(holder, !currentlyFavorite);
        });

        // Click event to show detail
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), DetailActivity.class);
            intent.putExtra("listing", listing);
            v.getContext().startActivity(intent);
        });
    }

    private void bindFavoriteBadge(@NonNull VehicleViewHolder holder, @NonNull Listing listing) {
        String listingId = listing.getListingId();
        boolean isFavorite = !TextUtils.isEmpty(listingId) && favoriteState.getOrDefault(listingId, false);
        updateFavoriteIcon(holder.ivFavoriteBadge, isFavorite);

        holder.ivFavoriteBadge.setClickable(true);
        holder.ivFavoriteBadge.setFocusable(true);
        holder.ivFavoriteBadge.setOnClickListener(v -> {
            if (TextUtils.isEmpty(listingId)) {
                Toast.makeText(v.getContext(), R.string.favorite_toggle_failed, Toast.LENGTH_SHORT).show();
                return;
            }

            // Child click consumes the event to avoid triggering the root item click.
            v.getParent().requestDisallowInterceptTouchEvent(true);

            if (pendingToggleIds.contains(listingId)) {
                return;
            }

            ensureDependencies(v);
            String userId = authSessionManager.getUserId();
            if (TextUtils.isEmpty(userId)) {
                Toast.makeText(v.getContext(), R.string.favorite_userid_missing, Toast.LENGTH_SHORT).show();
                return;
            }

            boolean previousState = favoriteState.getOrDefault(listingId, false);
            boolean optimisticState = !previousState;

            favoriteState.put(listingId, optimisticState);
            updateFavoriteIcon(holder.ivFavoriteBadge, optimisticState);

            pendingToggleIds.add(listingId);
            holder.ivFavoriteBadge.setEnabled(false);

            apiService.toggleFavorite(new ToggleFavoriteRequest(userId, listingId)).enqueue(new Callback<ApiResponse<JsonElement>>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<JsonElement>> call, @NonNull Response<ApiResponse<JsonElement>> response) {
                    pendingToggleIds.remove(listingId);
                    holder.ivFavoriteBadge.setEnabled(true);

                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(holder.itemView.getContext(),
                                optimisticState ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Rollback optimistic state when API fails.
                    favoriteState.put(listingId, previousState);
                    notifyListingChanged(listingId);
                    Toast.makeText(holder.itemView.getContext(), R.string.favorite_toggle_failed, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse<JsonElement>> call, @NonNull Throwable t) {
                    pendingToggleIds.remove(listingId);
                    holder.ivFavoriteBadge.setEnabled(true);

                    favoriteState.put(listingId, previousState);
                    notifyListingChanged(listingId);
                    Toast.makeText(holder.itemView.getContext(), R.string.favorite_toggle_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void ensureDependencies(@NonNull View anchorView) {
        if (apiService == null) {
            apiService = ApiClient.getApiService();
        }
        if (authSessionManager == null) {
            authSessionManager = new AuthSessionManager(anchorView.getContext());
        }
    }

    private void updateFavoriteIcon(@NonNull ImageView favoriteView, boolean isFavorite) {
        favoriteView.setImageResource(isFavorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
    }

    private void notifyListingChanged(@NonNull String listingId) {
        if (listings == null || listings.isEmpty()) {
            return;
        }

        for (int i = 0; i < listings.size(); i++) {
            Listing listing = listings.get(i);
            if (listing != null && TextUtils.equals(listingId, listing.getListingId())) {
                notifyItemChanged(i);
                return;
            }
        }
    }

    @Override
    public int getItemCount() {
        return listings == null ? 0 : listings.size();
    }

    public void updateFavoriteIds(Set<String> newFavoriteIds) {
        favoriteState.clear();
        pendingToggleIds.clear();

        if (newFavoriteIds == null || newFavoriteIds.isEmpty()) {
            return;
        }

        for (String listingId : newFavoriteIds) {
            if (!TextUtils.isEmpty(listingId)) {
                favoriteState.put(listingId, true);
            }
        }
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
                        && TextUtils.equals(oldItem.getUserName(), newItem.getUserName())
                        && TextUtils.equals(oldItem.getCreatedAt(), newItem.getCreatedAt());
            }
        });

        this.listings = newListings;
        diffResult.dispatchUpdatesTo(this);
    }

    public static class VehicleViewHolder extends RecyclerView.ViewHolder {
        ImageButton btnFavorite;
        ImageView ivVehicle;
        ImageView ivFavoriteBadge;
        TextView tvName, tvPrice, tvConditionTag, tvSpecs, tvMeta;

        public VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            ivVehicle = itemView.findViewById(R.id.ivVehicle);
            ivFavoriteBadge = itemView.findViewById(R.id.ivFavoriteBadge);
            tvName = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvConditionTag = itemView.findViewById(R.id.tvConditionTag);
            tvSpecs = itemView.findViewById(R.id.tvSpecs);
            tvMeta = itemView.findViewById(R.id.tvMeta);
        }
    }

    private void bindFavoriteState(VehicleViewHolder holder, boolean isFavorite) {
        holder.btnFavorite.setSelected(isFavorite);
        holder.btnFavorite.setImageResource(isFavorite
                ? android.R.drawable.btn_star_big_on
                : android.R.drawable.btn_star_big_off);
        holder.btnFavorite.setContentDescription(holder.itemView.getContext().getString(
                isFavorite ? R.string.favorite_remove : R.string.favorite_add));
    }

    private String getFavoriteKey(Listing listing, int position) {
        if (!TextUtils.isEmpty(listing.getListingId())) {
            return listing.getListingId();
        }
        return listing.getDisplayTitle() + "_" + listing.getBuyNowPrice() + "_" + position;
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

    private String buildMeta(Listing listing) {
        String address = !TextUtils.isEmpty(listing.getAddress()) ? listing.getAddress() : "Lien he nguoi ban";
        String relativeTime = formatRelativeTime(listing.getCreatedAt());

        if (!TextUtils.isEmpty(relativeTime)) {
            return address + " • " + relativeTime;
        }

        return address;
    }

    private String formatRelativeTime(String createdAt) {
        if (TextUtils.isEmpty(createdAt)) {
            return "";
        }

        Date createdDate = parseCreatedAt(createdAt);
        if (createdDate == null) {
            return "";
        }

        long diffMillis = System.currentTimeMillis() - createdDate.getTime();
        if (diffMillis < 60_000L) {
            return "Vua dang";
        }

        long minutes = diffMillis / 60_000L;
        if (minutes < 60L) {
            return minutes + " phut truoc";
        }

        long hours = diffMillis / 3_600_000L;
        if (hours < 24L) {
            return hours + " gio truoc";
        }

        long days = diffMillis / 86_400_000L;
        if (days < 30L) {
            return days + " ngay truoc";
        }

        long months = days / 30L;
        if (months < 12L) {
            return months + " thang truoc";
        }

        long years = months / 12L;
        return years + " nam truoc";
    }

    private Date parseCreatedAt(String value) {
        String[] patterns = new String[]{
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setLenient(false);
                if (pattern.endsWith("'Z'")) {
                    format.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                return format.parse(value);
            } catch (ParseException ignored) {
                // Try next date pattern.
            }
        }
        return null;
    }
}
