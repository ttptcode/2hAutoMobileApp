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

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private final List<JsonElement> conversations;

    public ConversationAdapter(List<JsonElement> conversations) {
        this.conversations = conversations == null ? new ArrayList<>() : conversations;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        JsonObject conversation = asJsonObject(conversations.get(position));

        String title = extractListingTitle(conversation);
        if (TextUtils.isEmpty(title)) {
            title = holder.itemView.getContext().getString(R.string.news_title_fallback);
        }

        String lastMessage = getAsString(conversation, "lastMessage");
        if (TextUtils.isEmpty(lastMessage)) {
            lastMessage = holder.itemView.getContext().getString(R.string.news_message_fallback);
        }

        String status = getAsString(conversation, "status");
        if (TextUtils.isEmpty(status)) {
            status = holder.itemView.getContext().getString(R.string.news_status_fallback);
        }

        String updatedAt = getAsString(conversation, "updatedAt");
        String updatedText = TextUtils.isEmpty(updatedAt)
                ? ""
                : holder.itemView.getContext().getString(R.string.news_updated_prefix, updatedAt);

        holder.tvConversationTitle.setText(title);
        holder.tvConversationMessage.setText(lastMessage);
        holder.tvConversationMeta.setText(TextUtils.isEmpty(updatedText) ? status : status + " • " + updatedText);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public void setData(List<JsonElement> data) {
        conversations.clear();
        if (data != null) {
            conversations.addAll(data);
        }
        notifyDataSetChanged();
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

    private String extractListingTitle(JsonObject conversation) {
        if (conversation == null || !conversation.has("listing") || conversation.get("listing").isJsonNull()) {
            return "";
        }

        JsonElement listingElement = conversation.get("listing");
        if (!listingElement.isJsonObject()) {
            return "";
        }

        JsonObject listingObject = listingElement.getAsJsonObject();
        String directTitle = getAsString(listingObject, "itemTitle");
        if (!TextUtils.isEmpty(directTitle)) {
            return directTitle;
        }

        if (listingObject.has("item") && listingObject.get("item").isJsonObject()) {
            JsonObject itemObject = listingObject.getAsJsonObject("item");
            String nestedTitle = getAsString(itemObject, "title");
            if (!TextUtils.isEmpty(nestedTitle)) {
                return nestedTitle;
            }
        }

        return "";
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvConversationTitle;
        private final TextView tvConversationMessage;
        private final TextView tvConversationMeta;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvConversationTitle = itemView.findViewById(R.id.tvConversationTitle);
            tvConversationMessage = itemView.findViewById(R.id.tvConversationMessage);
            tvConversationMeta = itemView.findViewById(R.id.tvConversationMeta);
        }
    }
}
