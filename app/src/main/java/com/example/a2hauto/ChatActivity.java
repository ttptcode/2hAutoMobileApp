package com.example.a2hauto;

import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final String STATE_ACTIVE_CHAT_USER = "state_active_chat_user";
    private static final String STATE_DRAFTS = "state_drafts";
    private static final String STATE_MEDIA_DRAFTS = "state_media_drafts";

    private View chatListContainer;
    private View chatMessageContainer;
    private TextView tvChatHeaderName;
    private TextView tvChatHeaderStatus;
    private TextView tvSelectedMedia;
    private EditText etChatMessage;
    private String activeChatUser;
    private final Map<String, String> draftByUser = new HashMap<>();
    private final Map<String, ArrayList<Uri>> mediaDraftByUser = new HashMap<>();

    private final ActivityResultLauncher<String[]> mediaPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), this::onMediaPicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatListContainer = findViewById(R.id.chatListContainer);
        chatMessageContainer = findViewById(R.id.chatMessageContainer);
        tvChatHeaderName = findViewById(R.id.tvChatHeaderName);
        tvChatHeaderStatus = findViewById(R.id.tvChatHeaderStatus);
        tvSelectedMedia = findViewById(R.id.tvSelectedMedia);
        etChatMessage = findViewById(R.id.etChatMessage);

        if (savedInstanceState != null) {
            activeChatUser = savedInstanceState.getString(STATE_ACTIVE_CHAT_USER);
            Bundle drafts = savedInstanceState.getBundle(STATE_DRAFTS);
            if (drafts != null) {
                for (String key : drafts.keySet()) {
                    draftByUser.put(key, drafts.getString(key, ""));
                }
            }

            Bundle mediaDrafts = savedInstanceState.getBundle(STATE_MEDIA_DRAFTS);
            if (mediaDrafts != null) {
                for (String key : mediaDrafts.keySet()) {
                    ArrayList<String> serializedUris = mediaDrafts.getStringArrayList(key);
                    if (serializedUris == null || serializedUris.isEmpty()) {
                        continue;
                    }

                    ArrayList<Uri> parsedUris = new ArrayList<>();
                    for (String uriString : serializedUris) {
                        if (uriString != null && !uriString.trim().isEmpty()) {
                            parsedUris.add(Uri.parse(uriString));
                        }
                    }

                    if (!parsedUris.isEmpty()) {
                        mediaDraftByUser.put(key, parsedUris);
                    }
                }
            }
        }

        findViewById(R.id.itemChatUser1).setOnClickListener(v -> showMessageScreen("Tuananh"));
        findViewById(R.id.itemChatUser2).setOnClickListener(v -> showMessageScreen("Tran Le Tuan Anh (K17 HCM)"));

        findViewById(R.id.btnChatBackHome).setOnClickListener(v -> finish());
        findViewById(R.id.btnChatBack).setOnClickListener(v -> showListScreen());
        findViewById(R.id.btnSendMessage).setOnClickListener(v -> sendMessage());
        findViewById(R.id.btnPickMedia).setOnClickListener(v -> mediaPickerLauncher.launch(new String[]{"image/*", "video/*"}));

        bindQuickReply(R.id.chipSuggestionPrice, getString(R.string.chat_suggestion_price));
        bindQuickReply(R.id.chipSuggestionAvailable, getString(R.string.chat_suggestion_available));
        bindQuickReply(R.id.chipSuggestionAddress, getString(R.string.chat_suggestion_address));
        bindQuickReply(R.id.chipSuggestionFixPrice, getString(R.string.chat_suggestion_fix_price));
        bindQuickReply(R.id.chipSuggestionThanks, getString(R.string.chat_suggestion_thanks));

        showListScreen();
    }

    private void showListScreen() {
        saveDraftForActiveUser();
        hideKeyboardAndClearInputFocus();
        chatListContainer.setVisibility(View.VISIBLE);
        chatMessageContainer.setVisibility(View.GONE);
    }

    private void showMessageScreen(String userName) {
        saveDraftForActiveUser();

        tvChatHeaderName.setText(userName);
        tvChatHeaderStatus.setText(getString(R.string.chat_status_unknown));
        etChatMessage.setError(null);

        activeChatUser = userName;
        etChatMessage.setText(getDraftForUser(userName));
        etChatMessage.setSelection(etChatMessage.getText().length());
        updateSelectedMediaUi();

        chatListContainer.setVisibility(View.GONE);
        chatMessageContainer.setVisibility(View.VISIBLE);
    }

    private void bindQuickReply(int viewId, String quickReplyText) {
        findViewById(viewId).setOnClickListener(v -> insertIntoInput(quickReplyText));
    }

    private void insertIntoInput(String value) {
        if (etChatMessage == null) {
            return;
        }

        int start = Math.max(etChatMessage.getSelectionStart(), 0);
        int end = Math.max(etChatMessage.getSelectionEnd(), 0);
        etChatMessage.getText().replace(Math.min(start, end), Math.max(start, end), value);
        etChatMessage.requestFocus();
        saveDraftForActiveUser();
    }

    private void onMediaPicked(List<Uri> mediaUris) {
        appendMediaToActiveChat(mediaUris);
    }

    private void appendMediaToActiveChat(List<Uri> pickedUris) {
        if (activeChatUser == null || pickedUris == null || pickedUris.isEmpty()) {
            return;
        }

        ArrayList<Uri> mediaUris = getOrCreateMediaDraft(activeChatUser);
        int previousCount = mediaUris.size();

        for (Uri uri : pickedUris) {
            if (uri != null && !mediaUris.contains(uri)) {
                mediaUris.add(uri);
            }
        }

        updateSelectedMediaUi();
        int newCount = mediaUris.size() - previousCount;
        if (newCount > 0) {
            Toast.makeText(this, getString(R.string.chat_media_added, newCount), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage() {
        String message = etChatMessage.getText().toString().trim();
        ArrayList<Uri> mediaUris = activeChatUser == null
                ? new ArrayList<>()
                : mediaDraftByUser.getOrDefault(activeChatUser, new ArrayList<>());

        if (message.isEmpty() && mediaUris.isEmpty()) {
            etChatMessage.setError(getString(R.string.chat_message_or_media_required));
            return;
        }

        etChatMessage.setError(null);
        etChatMessage.setText("");
        if (activeChatUser != null) {
            draftByUser.remove(activeChatUser);
            mediaDraftByUser.remove(activeChatUser);
        }
        updateSelectedMediaUi();
    }

    private void updateSelectedMediaUi() {
        if (tvSelectedMedia == null) {
            return;
        }

        int count = 0;
        if (activeChatUser != null && mediaDraftByUser.containsKey(activeChatUser)) {
            count = mediaDraftByUser.get(activeChatUser).size();
        }

        if (count <= 0) {
            tvSelectedMedia.setVisibility(View.GONE);
            tvSelectedMedia.setText("");
            return;
        }

        tvSelectedMedia.setVisibility(View.VISIBLE);
        tvSelectedMedia.setText(getString(R.string.chat_media_selected_count, count));
    }

    private ArrayList<Uri> getOrCreateMediaDraft(String userName) {
        ArrayList<Uri> uris = mediaDraftByUser.get(userName);
        if (uris == null) {
            uris = new ArrayList<>();
            mediaDraftByUser.put(userName, uris);
        }
        return uris;
    }

    private void saveDraftForActiveUser() {
        if (etChatMessage == null || activeChatUser == null) {
            return;
        }

        String draft = etChatMessage.getText().toString();
        if (draft.trim().isEmpty()) {
            draftByUser.remove(activeChatUser);
            return;
        }

        draftByUser.put(activeChatUser, draft);
    }

    private String getDraftForUser(String userName) {
        if (userName == null) {
            return "";
        }
        String draft = draftByUser.get(userName);
        return draft == null ? "" : draft;
    }

    private void hideKeyboardAndClearInputFocus() {
        if (etChatMessage == null) {
            return;
        }

        etChatMessage.clearFocus();
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etChatMessage.getWindowToken(), 0);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN
                && etChatMessage != null
                && etChatMessage.hasFocus()) {
            Rect hitRect = new Rect();
            etChatMessage.getGlobalVisibleRect(hitRect);
            if (!hitRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                hideKeyboardAndClearInputFocus();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        saveDraftForActiveUser();
        outState.putString(STATE_ACTIVE_CHAT_USER, activeChatUser);

        Bundle drafts = new Bundle();
        for (Map.Entry<String, String> entry : draftByUser.entrySet()) {
            drafts.putString(entry.getKey(), entry.getValue());
        }
        outState.putBundle(STATE_DRAFTS, drafts);

        Bundle mediaDrafts = new Bundle();
        for (Map.Entry<String, ArrayList<Uri>> entry : mediaDraftByUser.entrySet()) {
            ArrayList<String> serializedUris = new ArrayList<>();
            for (Uri uri : entry.getValue()) {
                serializedUris.add(uri.toString());
            }
            mediaDrafts.putStringArrayList(entry.getKey(), serializedUris);
        }
        outState.putBundle(STATE_MEDIA_DRAFTS, mediaDrafts);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (chatMessageContainer != null && chatMessageContainer.getVisibility() == View.VISIBLE) {
            showListScreen();
            return;
        }
        super.onBackPressed();
    }
}

