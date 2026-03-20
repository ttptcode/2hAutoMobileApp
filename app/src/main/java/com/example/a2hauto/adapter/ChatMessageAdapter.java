package com.example.a2hauto.adapter;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a2hauto.R;
import com.example.a2hauto.model.Message;
import com.bumptech.glide.Glide;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private final List<Message> messages = new ArrayList<>();
    private String currentUserId = "";

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId == null ? "" : currentUserId;
    }

    public void setMessages(List<Message> data) {
        messages.clear();
        if (data != null) {
            messages.addAll(data);
        }
        notifyDataSetChanged();
    }

    public void addMessage(Message message) {
        if (message == null) {
            return;
        }
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        boolean isCurrentUser = TextUtils.equals(currentUserId, message.getSenderId());

        bindContent(holder, message, isCurrentUser);
        holder.tvMessageTime.setText(buildMetaText(message, isCurrentUser, holder));

        int rowGravity = isCurrentUser ? Gravity.END : Gravity.START;
        holder.container.setGravity(rowGravity);
        applyHorizontalGravity(holder.tvMessageTime, rowGravity);

        holder.tvMessageContent.setBackgroundResource(isCurrentUser
                ? R.drawable.bg_message_user
                : R.drawable.bg_message_other);
        holder.tvMessageContent.setTextColor(holder.itemView.getContext().getColor(
                isCurrentUser ? R.color.white : R.color.text_primary));

        holder.ivMessageImage.setBackgroundResource(isCurrentUser
                ? R.drawable.bg_message_user
                : R.drawable.bg_message_other);
        holder.vvMessageVideo.setBackgroundResource(isCurrentUser
                ? R.drawable.bg_message_user
                : R.drawable.bg_message_other);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private String formatTime(String isoDate) {
        if (TextUtils.isEmpty(isoDate)) {
            return "";
        }

        Date date = parseIsoDate(isoDate);
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
    }

    private String buildMetaText(Message message, boolean isCurrentUser, MessageViewHolder holder) {
        String timeText = formatTime(message.getCreatedAt());
        if (!isCurrentUser) {
            return timeText;
        }

        String statusText = holder.itemView.getContext().getString(
                message.isRead() ? R.string.chat_status_seen : R.string.chat_status_received
        );

        if (TextUtils.isEmpty(timeText)) {
            return statusText;
        }
        return timeText + "  " + statusText;
    }

    private Date parseIsoDate(String isoDate) {
        String[] formats = new String[] {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss"
        };

        for (String format : formats) {
            try {
                return new SimpleDateFormat(format, Locale.getDefault()).parse(isoDate);
            } catch (ParseException ignored) {
                // Continue trying the next ISO date format.
            }
        }
        return null;
    }

    private void bindContent(MessageViewHolder holder, Message message, boolean isCurrentUser) {
        String content = message.getContent() == null ? "" : message.getContent().trim();
        ParsedContent parsed = parseContent(content);
        int rowGravity = isCurrentUser ? Gravity.END : Gravity.START;

        holder.ivMessageImage.setVisibility(View.GONE);
        holder.vvMessageVideo.setVisibility(View.GONE);
        holder.tvMessageContent.setVisibility(View.GONE);

        if (parsed.type == ParsedType.IMAGE && !TextUtils.isEmpty(parsed.url)) {
            holder.ivMessageImage.setVisibility(View.VISIBLE);
            applyHorizontalGravity(holder.ivMessageImage, rowGravity);
            Glide.with(holder.itemView)
                    .load(parsed.url)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.ivMessageImage);
            if (!TextUtils.isEmpty(parsed.caption)) {
                holder.tvMessageContent.setVisibility(View.VISIBLE);
                applyHorizontalGravity(holder.tvMessageContent, rowGravity);
                holder.tvMessageContent.setText(parsed.caption);
            }
            return;
        }

        if (parsed.type == ParsedType.VIDEO && !TextUtils.isEmpty(parsed.url)) {
            holder.vvMessageVideo.setVisibility(View.VISIBLE);
            applyHorizontalGravity(holder.vvMessageVideo, rowGravity);
            holder.vvMessageVideo.setVideoPath(parsed.url);
            MediaController controller = new MediaController(holder.itemView.getContext());
            controller.setAnchorView(holder.vvMessageVideo);
            holder.vvMessageVideo.setMediaController(controller);
            holder.vvMessageVideo.seekTo(1);

            if (!TextUtils.isEmpty(parsed.caption)) {
                holder.tvMessageContent.setVisibility(View.VISIBLE);
                applyHorizontalGravity(holder.tvMessageContent, rowGravity);
                holder.tvMessageContent.setText(parsed.caption);
            }
            return;
        }

        holder.tvMessageContent.setVisibility(View.VISIBLE);
        applyHorizontalGravity(holder.tvMessageContent, rowGravity);
        holder.tvMessageContent.setText(parsed.caption);
    }

    private void applyHorizontalGravity(View view, int gravity) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).gravity = gravity;
            view.setLayoutParams(params);
        }
    }

    private ParsedContent parseContent(String content) {
        if (TextUtils.isEmpty(content)) {
            return new ParsedContent(ParsedType.TEXT, "", "");
        }

        try {
            JsonElement element = new JsonParser().parse(content);
            if (!element.isJsonObject()) {
                return new ParsedContent(ParsedType.TEXT, "", content);
            }

            JsonObject object = element.getAsJsonObject();
            String type = getAsString(object, "type");
            String caption = getAsString(object, "text");

            if ("image".equalsIgnoreCase(type)) {
                return new ParsedContent(ParsedType.IMAGE, getAsString(object, "url"), caption);
            }

            if ("video".equalsIgnoreCase(type)) {
                return new ParsedContent(ParsedType.VIDEO, getAsString(object, "url"), caption);
            }

            if ("multiple".equalsIgnoreCase(type) && object.has("media") && object.get("media").isJsonArray()) {
                JsonArray media = object.getAsJsonArray("media");
                if (media.size() > 0) {
                    JsonObject firstItem = media.get(0).getAsJsonObject();
                    String firstType = getAsString(firstItem, "type");
                    String firstUrl = getAsString(firstItem, "url");
                    String suffix = media.size() > 1 ? " (" + media.size() + " tep)" : "";

                    if ("video".equalsIgnoreCase(firstType)) {
                        return new ParsedContent(ParsedType.VIDEO, firstUrl, caption + suffix);
                    }
                    return new ParsedContent(ParsedType.IMAGE, firstUrl, caption + suffix);
                }
            }
        } catch (Exception ignored) {
            // Non-JSON content is handled as plain text.
        }

        return new ParsedContent(ParsedType.TEXT, "", content);
    }

    private String getAsString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private enum ParsedType {
        TEXT,
        IMAGE,
        VIDEO
    }

    private static class ParsedContent {
        private final ParsedType type;
        private final String url;
        private final String caption;

        private ParsedContent(ParsedType type, String url, String caption) {
            this.type = type;
            this.url = url;
            this.caption = caption == null ? "" : caption;
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout container;
        private final TextView tvMessageContent;
        private final TextView tvMessageTime;
        private final ImageView ivMessageImage;
        private final VideoView vvMessageVideo;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.layoutMessageRow);
            tvMessageContent = itemView.findViewById(R.id.tvMessageContent);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
            ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
            vvMessageVideo = itemView.findViewById(R.id.vvMessageVideo);
        }
    }
}

