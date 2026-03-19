package com.example.a2hauto.adapter;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.a2hauto.R;

import java.util.ArrayList;
import java.util.List;

public class DetailMediaAdapter extends RecyclerView.Adapter<DetailMediaAdapter.MediaViewHolder> {

    public static final int TYPE_IMAGE = 0;
    public static final int TYPE_VIDEO = 1;

    private List<MediaItem> mediaItems;

    public DetailMediaAdapter(List<MediaItem> mediaItems) {
        this.mediaItems = mediaItems == null ? new ArrayList<>() : mediaItems;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detail_media_page, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        MediaItem item = mediaItems.get(position);

        if (item.type == TYPE_VIDEO) {
            bindVideo(holder, item.url);
            return;
        }

        bindImage(holder, item.url);
    }

    @Override
    public int getItemCount() {
        return mediaItems == null ? 0 : mediaItems.size();
    }

    @Override
    public void onViewRecycled(@NonNull MediaViewHolder holder) {
        super.onViewRecycled(holder);
        holder.btnPlayVideo.setVisibility(View.GONE);
        holder.videoView.stopPlayback();
        holder.videoView.setVideoURI(null);
    }

    public void setMediaItems(List<MediaItem> mediaItems) {
        this.mediaItems = mediaItems == null ? new ArrayList<>() : mediaItems;
        notifyDataSetChanged();
    }

    private void bindImage(@NonNull MediaViewHolder holder, String url) {
        holder.videoView.stopPlayback();
        holder.videoView.setVisibility(View.GONE);
        holder.btnPlayVideo.setVisibility(View.GONE);
        holder.mediaLoading.setVisibility(View.GONE);
        holder.ivImage.setVisibility(View.VISIBLE);

        Glide.with(holder.itemView.getContext())
                .load(url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(holder.ivImage);
    }

    private void bindVideo(@NonNull MediaViewHolder holder, String url) {
        holder.ivImage.setVisibility(View.GONE);
        holder.videoView.setVisibility(View.VISIBLE);
        holder.btnPlayVideo.setVisibility(View.VISIBLE);
        holder.btnPlayVideo.setImageResource(android.R.drawable.ic_media_play);

        if (TextUtils.isEmpty(url)) {
            holder.mediaLoading.setVisibility(View.GONE);
            holder.btnPlayVideo.setVisibility(View.GONE);
            holder.videoView.setVideoURI(null);
            return;
        }

        holder.mediaLoading.setVisibility(View.VISIBLE);
        holder.videoView.stopPlayback();
        holder.videoView.setVideoURI(Uri.parse(url));
        holder.videoView.setOnPreparedListener(mediaPlayer -> {
            holder.mediaLoading.setVisibility(View.GONE);
            mediaPlayer.setLooping(true);
            // Render first frame as preview until user taps play.
            holder.videoView.seekTo(1);
            if (holder.videoView.isPlaying()) {
                holder.btnPlayVideo.setVisibility(View.GONE);
            } else {
                holder.btnPlayVideo.setVisibility(View.VISIBLE);
                holder.btnPlayVideo.setImageResource(android.R.drawable.ic_media_play);
            }
        });
        holder.videoView.setOnCompletionListener(mediaPlayer -> {
            holder.btnPlayVideo.setVisibility(View.VISIBLE);
            holder.btnPlayVideo.setImageResource(android.R.drawable.ic_media_play);
        });

        View.OnClickListener togglePlayback = v -> {
            if (holder.videoView.isPlaying()) {
                holder.videoView.pause();
                holder.btnPlayVideo.setVisibility(View.VISIBLE);
                holder.btnPlayVideo.setImageResource(android.R.drawable.ic_media_play);
            } else {
                holder.videoView.start();
                holder.btnPlayVideo.setVisibility(View.VISIBLE);
                holder.btnPlayVideo.setImageResource(android.R.drawable.ic_media_pause);
                holder.btnPlayVideo.postDelayed(() -> {
                    if (holder.videoView.isPlaying()) {
                        holder.btnPlayVideo.setVisibility(View.GONE);
                    }
                }, 650);
            }
        };
        holder.btnPlayVideo.setOnClickListener(togglePlayback);
        holder.videoView.setOnClickListener(togglePlayback);
    }

    public static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        VideoView videoView;
        ImageButton btnPlayVideo;
        ProgressBar mediaLoading;

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivDetailMedia);
            videoView = itemView.findViewById(R.id.videoDetailMedia);
            btnPlayVideo = itemView.findViewById(R.id.btnPlayVideo);
            mediaLoading = itemView.findViewById(R.id.progressMedia);
        }
    }

    public static class MediaItem {
        public final int type;
        public final String url;

        public MediaItem(int type, String url) {
            this.type = type;
            this.url = url;
        }
    }
}




