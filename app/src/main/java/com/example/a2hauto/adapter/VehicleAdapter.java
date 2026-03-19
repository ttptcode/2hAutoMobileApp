package com.example.a2hauto.adapter;

import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {
    private List<Listing> listings;
    private final Map<String, Boolean> favoriteState = new HashMap<>();
    private final Set<String> pendingFavoriteRequests = new HashSet<>();
    private ApiService apiService;
    private AuthSessionManager authSessionManager;

    public VehicleAdapter(List<Listing> listings) {
        this.listings = listings;
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (apiService == null) {
            apiService = ApiClient.getApiService();
        }
        if (authSessionManager == null) {
            authSessionManager = new AuthSessionManager(parent.getContext());
        }
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

        holder.tvSpecs.setText(buildSpecs(item));
        holder.tvAddress.setText(buildAddress(listing));
        holder.tvSummary.setText(formatRelativeTime(listing.getCreatedAt()));

        String favoriteKey = getFavoriteKey(listing, position);
        boolean isFavorite = Boolean.TRUE.equals(favoriteState.get(favoriteKey));
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

<<<<<<< feature/chat_V3
        holder.ivFavoriteBadge.setOnClickListener(v -> {
=======
        holder.favoriteActionView.setOnClickListener(v -> {
>>>>>>> main
            String listingId = listing.getListingId();
            if (TextUtils.isEmpty(listingId)) {
                Toast.makeText(holder.itemView.getContext(), R.string.favorite_action_failed, Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = authSessionManager == null ? "" : authSessionManager.getUserId();
            if (TextUtils.isEmpty(userId)) {
                Toast.makeText(holder.itemView.getContext(), R.string.favorite_login_required, Toast.LENGTH_SHORT).show();
                return;
            }

            if (pendingFavoriteRequests.contains(listingId)) {
                return;
            }

            pendingFavoriteRequests.add(listingId);
            apiService.toggleFavorite(new ToggleFavoriteRequest(userId, listingId))
                    .enqueue(new Callback<ApiResponse<JsonElement>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<JsonElement>> call,
                                               @NonNull Response<ApiResponse<JsonElement>> response) {
                            pendingFavoriteRequests.remove(listingId);
                            if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                                Toast.makeText(holder.itemView.getContext(), R.string.favorite_action_failed, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            boolean currentFavorite = Boolean.TRUE.equals(favoriteState.get(listingId));
                            updateFavoriteState(listingId, !currentFavorite);
                        }

                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<JsonElement>> call, @NonNull Throwable t) {
                            pendingFavoriteRequests.remove(listingId);
                            Toast.makeText(holder.itemView.getContext(), R.string.favorite_action_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

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
                        && TextUtils.equals(oldItem.getUserName(), newItem.getUserName())
                        && TextUtils.equals(oldItem.getCreatedAt(), newItem.getCreatedAt());
            }
        });

        this.listings = newListings;
        diffResult.dispatchUpdatesTo(this);
    }

    public void setFavoriteListingIds(Set<String> listingIds) {
        favoriteState.clear();
        if (listingIds != null) {
            for (String listingId : listingIds) {
                if (!TextUtils.isEmpty(listingId)) {
                    favoriteState.put(listingId, true);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void updateFavoriteState(String listingId, boolean isFavorite) {
        if (TextUtils.isEmpty(listingId)) {
            return;
        }

        if (isFavorite) {
            favoriteState.put(listingId, true);
        } else {
            favoriteState.remove(listingId);
        }

        for (int i = 0; i < listings.size(); i++) {
            Listing listing = listings.get(i);
            if (listingId.equals(listing.getListingId())) {
                notifyItemChanged(i);
                return;
            }
        }
    }

    public static class VehicleViewHolder extends RecyclerView.ViewHolder {
        ImageView ivVehicle;
<<<<<<< feature/chat_V3
        ImageView ivFavoriteBadge;
=======
        ImageView favoriteActionView;
>>>>>>> main
        AppCompatCheckBox cbSelectFavorite;
        TextView tvName, tvPrice, tvSpecs, tvAddress, tvSummary;

        public VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            ivVehicle = itemView.findViewById(R.id.ivVehicle);
<<<<<<< feature/chat_V3
            ivFavoriteBadge = itemView.findViewById(R.id.ivFavoriteBadge);
=======
            favoriteActionView = itemView.findViewById(R.id.ivFavoriteBadge);
>>>>>>> main
            cbSelectFavorite = itemView.findViewById(R.id.cbSelectFavorite);
            tvName = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvSpecs = itemView.findViewById(R.id.tvSpecs);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvSummary = itemView.findViewById(R.id.tvSummary);
        }
    }

    private void bindFavoriteState(VehicleViewHolder holder, boolean isFavorite) {
<<<<<<< feature/chat_V3
        holder.ivFavoriteBadge.setSelected(isFavorite);
        holder.ivFavoriteBadge.setImageResource(isFavorite
                ? R.drawable.ic_favorite_heart_filled
                : R.drawable.ic_favorite_heart_outline);
        holder.ivFavoriteBadge.setContentDescription(holder.itemView.getContext().getString(
=======
        holder.favoriteActionView.setSelected(isFavorite);
        holder.favoriteActionView.setImageResource(isFavorite
                ? R.drawable.ic_favorite_heart_filled
                : R.drawable.ic_favorite_heart_outline);
        holder.favoriteActionView.setContentDescription(holder.itemView.getContext().getString(
>>>>>>> main
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

    private String buildAddress(Listing listing) {
        return !TextUtils.isEmpty(listing.getAddress()) ? listing.getAddress() : "Lien he nguoi ban";
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
