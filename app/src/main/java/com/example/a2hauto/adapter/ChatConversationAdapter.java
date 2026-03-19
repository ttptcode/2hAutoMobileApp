package com.example.a2hauto.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a2hauto.R;
import com.example.a2hauto.model.Conversation;
import com.example.a2hauto.model.UserBrief;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ChatConversationAdapter extends RecyclerView.Adapter<ChatConversationAdapter.ConversationViewHolder> {

    public interface ConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    private final List<Conversation> conversations = new ArrayList<>();
    private final Set<String> unreadConversationIds = new HashSet<>();
    private final ConversationClickListener clickListener;
    private String currentUserId = "";

    public ChatConversationAdapter(ConversationClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId == null ? "" : currentUserId;
    }

    public void setConversations(List<Conversation> data) {
        conversations.clear();
        if (data != null) {
            conversations.addAll(data);
        }
        notifyDataSetChanged();
    }

    public void setUnreadConversationIds(Set<String> unreadIds) {
        unreadConversationIds.clear();
        if (unreadIds != null) {
            unreadConversationIds.addAll(unreadIds);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);
        String displayName = getOtherUserName(conversation);
        holder.tvName.setText(TextUtils.isEmpty(displayName)
                ? holder.itemView.getContext().getString(R.string.chat_unknown_user)
                : displayName);

        String preview = TextUtils.isEmpty(conversation.getLastMessage())
                ? holder.itemView.getContext().getString(R.string.chat_no_messages_yet)
                : getPreviewText(conversation.getLastMessage());
        holder.tvPreview.setText(preview);

        holder.tvTime.setText(formatTime(conversation.getUpdatedAt()));
        holder.tvUnreadDot.setVisibility(unreadConversationIds.contains(conversation.getConversationId())
                ? View.VISIBLE
                : View.GONE);

        String avatarText = "?";
        if (!TextUtils.isEmpty(displayName)) {
            avatarText = String.valueOf(Character.toUpperCase(displayName.charAt(0)));
        }
        holder.tvAvatar.setText(avatarText);

        holder.itemView.setOnClickListener(v -> clickListener.onConversationClick(conversation));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    private String getOtherUserName(Conversation conversation) {
        if (conversation == null) {
            return "";
        }

        UserBrief seller = conversation.getSeller();
        UserBrief buyer = conversation.getBuyer();
        if (seller == null && buyer == null) {
            return "";
        }

        boolean isCurrentSeller = seller != null && TextUtils.equals(currentUserId, seller.getUserId());
        UserBrief other = isCurrentSeller ? buyer : seller;
        if (other == null) {
            other = isCurrentSeller ? seller : buyer;
        }

        return other == null ? "" : other.getFullName();
    }

    private String formatTime(String isoDate) {
        if (TextUtils.isEmpty(isoDate)) {
            return "";
        }

        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = input.parse(isoDate);
            if (date == null) {
                return "";
            }
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
        } catch (ParseException ignored) {
            return "";
        }
    }

    private String getPreviewText(String rawMessage) {
        if (TextUtils.isEmpty(rawMessage)) {
            return "";
        }

        try {
            JsonElement element = new JsonParser().parse(rawMessage);
            if (!element.isJsonObject()) {
                return rawMessage;
            }

            JsonObject object = element.getAsJsonObject();
            String type = getAsString(object, "type");
            String text = getAsString(object, "text");

            if ("image".equalsIgnoreCase(type)) {
                return TextUtils.isEmpty(text) ? "📷 Da gui anh" : "📷 " + text;
            }
            if ("video".equalsIgnoreCase(type)) {
                return TextUtils.isEmpty(text) ? "🎥 Da gui video" : "🎥 " + text;
            }
            if ("multiple".equalsIgnoreCase(type) && object.has("media") && object.get("media").isJsonArray()) {
                JsonArray mediaArray = object.getAsJsonArray("media");
                int count = mediaArray == null ? 0 : mediaArray.size();
                if (TextUtils.isEmpty(text)) {
                    return "📎 Da gui " + count + " tep";
                }
                return "📎 " + text;
            }
        } catch (Exception ignored) {
            // Keep raw text for non-JSON messages.
        }

        return rawMessage;
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

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAvatar;
        private final TextView tvName;
        private final TextView tvPreview;
        private final TextView tvTime;
        private final View tvUnreadDot;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvConversationAvatar);
            tvName = itemView.findViewById(R.id.tvConversationName);
            tvPreview = itemView.findViewById(R.id.tvConversationPreview);
            tvTime = itemView.findViewById(R.id.tvConversationTime);
            tvUnreadDot = itemView.findViewById(R.id.viewConversationUnreadDot);
        }
    }
}

