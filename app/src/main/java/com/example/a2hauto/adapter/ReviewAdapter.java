package com.example.a2hauto.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a2hauto.R;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private final List<JsonElement> reviews;

    public ReviewAdapter(List<JsonElement> reviews) {
        this.reviews = reviews == null ? new ArrayList<>() : reviews;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        JsonObject review = asJsonObject(reviews.get(position));

        String reviewerName = getAsString(review, "reviewerName");
        if (TextUtils.isEmpty(reviewerName)) {
            reviewerName = getAsString(review, "reviewerFullName");
        }
        if (TextUtils.isEmpty(reviewerName)) {
            reviewerName = holder.itemView.getContext().getString(R.string.review_reviewer_fallback);
        }

        String comment = getAsString(review, "comment");
        if (TextUtils.isEmpty(comment)) {
            comment = holder.itemView.getContext().getString(R.string.review_comment_empty);
        }

        int rating = getAsInt(review, "rating");
        if (rating < 1) {
            rating = 5;
        }

        String createdAt = getAsString(review, "createdAt");
        if (TextUtils.isEmpty(createdAt)) {
            createdAt = getAsString(review, "timestamp");
        }

        holder.tvReviewerName.setText(reviewerName);
        holder.tvReviewRating.setText(holder.itemView.getContext().getString(R.string.review_rating_format, buildStars(rating), rating));
        holder.tvReviewComment.setText(comment);
        holder.tvReviewTime.setText(TextUtils.isEmpty(createdAt)
                ? holder.itemView.getContext().getString(R.string.review_time_unknown)
                : createdAt);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    public void setData(List<JsonElement> data) {
        reviews.clear();
        if (data != null) {
            reviews.addAll(data);
        }
        notifyDataSetChanged();
    }

    private String buildStars(int rating) {
        int safeRating = Math.max(1, Math.min(5, rating));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < safeRating; i++) {
            builder.append('★');
        }
        for (int i = safeRating; i < 5; i++) {
            builder.append('☆');
        }
        return builder.toString();
    }

    private JsonObject asJsonObject(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private String getAsString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }

        try {
            return object.get(key).getAsString();
        } catch (Exception exception) {
            return "";
        }
    }

    private int getAsInt(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return 0;
        }

        try {
            return object.get(key).getAsInt();
        } catch (Exception exception) {
            return 0;
        }
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvReviewerName;
        private final TextView tvReviewRating;
        private final TextView tvReviewComment;
        private final TextView tvReviewTime;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReviewerName = itemView.findViewById(R.id.tvReviewerName);
            tvReviewRating = itemView.findViewById(R.id.tvReviewRating);
            tvReviewComment = itemView.findViewById(R.id.tvReviewComment);
            tvReviewTime = itemView.findViewById(R.id.tvReviewTime);
        }
    }
}
