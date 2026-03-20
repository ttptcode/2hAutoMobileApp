package com.example.a2hauto.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.a2hauto.R;
import com.example.a2hauto.model.Item;
import com.example.a2hauto.model.Listing;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NewsListingAdapter extends RecyclerView.Adapter<NewsListingAdapter.NewsListingViewHolder> {

    public interface ListingActionListener {
        void onToggleVisibility(Listing listing, boolean hide);
        void onViewListing(Listing listing);
        void onDeleteListing(Listing listing);
        void onEditListing(Listing listing);
    }

    private final List<Listing> listings;
    private final ListingActionListener actionListener;

    public NewsListingAdapter(List<Listing> listings, ListingActionListener actionListener) {
        this.listings = listings == null ? new ArrayList<>() : listings;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public NewsListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news_listing, parent, false);
        return new NewsListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsListingViewHolder holder, int position) {
        Listing listing = listings.get(position);
        holder.tvListingTitle.setText(resolveTitle(listing));
        holder.tvListingPrice.setText(formatPrice(holder.itemView, listing.getBuyNowPrice()));
        holder.tvListingMeta.setText(resolveMeta(listing));
        holder.switchHideListing.setOnCheckedChangeListener(null);
        holder.switchHideListing.setChecked(!isHidden(listing));

        String imageUrl = resolveFirstImage(listing);
        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivListingImage);

        holder.switchHideListing.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (actionListener != null) {
                actionListener.onToggleVisibility(listing, !isChecked);
            }
        });
        holder.btnViewListing.setOnClickListener(view -> {
            if (actionListener != null) {
                actionListener.onViewListing(listing);
            }
        });
        holder.btnDeleteListing.setOnClickListener(view -> {
            if (actionListener != null) {
                actionListener.onDeleteListing(listing);
            }
        });

        String status = (listing != null && listing.getStatus() != null) ? listing.getStatus().trim().toLowerCase(Locale.ROOT) : "";
        boolean isDraft = status.contains("draft");
        holder.btnEditListing.setVisibility(isDraft ? View.VISIBLE : View.GONE);
        holder.btnEditListing.setOnClickListener(view -> {
            if (actionListener != null) {
                actionListener.onEditListing(listing);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    public void setData(List<Listing> data) {
        listings.clear();
        if (data != null) {
            listings.addAll(data);
        }
        notifyDataSetChanged();
    }

    private String resolveTitle(Listing listing) {
        String title = listing == null ? "" : listing.getDisplayTitle();
        if (TextUtils.isEmpty(title)) {
            return "Bài đăng không tiêu đề";
        }
        return title;
    }

    private String resolveMeta(Listing listing) {
        if (listing == null) {
            return "";
        }

        String status = listing.getStatus();
        String date = listing.getCreatedAt();
        if (TextUtils.isEmpty(status)) {
            return TextUtils.isEmpty(date) ? "" : date;
        }
        if (TextUtils.isEmpty(date)) {
            return status;
        }
        return status + " • " + date;
    }

    private String resolveFirstImage(Listing listing) {
        if (listing == null) {
            return null;
        }

        Item item = listing.getItem();
        if (item == null || item.getImageUrls() == null || item.getImageUrls().isEmpty()) {
            return null;
        }
        return item.getImageUrls().get(0);
    }

    private boolean isHidden(Listing listing) {
        if (listing == null || TextUtils.isEmpty(listing.getStatus())) {
            return false;
        }
        return "hidden".equalsIgnoreCase(listing.getStatus().trim());
    }

    private String formatPrice(View view, double price) {
        if (price <= 0d) {
            return view.getContext().getString(R.string.news_price_contact);
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(price);
    }

    static class NewsListingViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivListingImage;
        private final TextView tvListingTitle;
        private final TextView tvListingPrice;
        private final TextView tvListingMeta;
        private final SwitchCompat switchHideListing;
        private final ImageButton btnViewListing;
        private final ImageButton btnDeleteListing;
        private final ImageButton btnEditListing;

        NewsListingViewHolder(@NonNull View itemView) {
            super(itemView);
            ivListingImage = itemView.findViewById(R.id.ivListingImage);
            tvListingTitle = itemView.findViewById(R.id.tvListingTitle);
            tvListingPrice = itemView.findViewById(R.id.tvListingPrice);
            tvListingMeta = itemView.findViewById(R.id.tvListingMeta);
            switchHideListing = itemView.findViewById(R.id.switchHideListing);
            btnViewListing = itemView.findViewById(R.id.btnViewListing);
            btnDeleteListing = itemView.findViewById(R.id.btnDeleteListing);
            btnEditListing = itemView.findViewById(R.id.btnEditListing);
        }
    }
}
