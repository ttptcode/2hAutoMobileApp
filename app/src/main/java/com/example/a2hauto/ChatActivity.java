package com.example.a2hauto;

import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a2hauto.adapter.ChatConversationAdapter;
import com.example.a2hauto.adapter.ChatMessageAdapter;
import com.example.a2hauto.api.ApiClient;
import com.example.a2hauto.auth.AuthSessionManager;
import com.example.a2hauto.chat.ChatRepository;
import com.example.a2hauto.model.Conversation;
import com.example.a2hauto.model.Message;
import com.example.a2hauto.model.UserBrief;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ChatActivity extends AppCompatActivity {

    private static final long POLLING_INTERVAL_MS = 7000L;
    public static final String EXTRA_OPEN_CONVERSATION_ID = "extra_open_conversation_id";

    private View chatListContainer;
    private View chatMessageContainer;
    private TextView tvChatHeaderName;
    private TextView tvChatHeaderStatus;
    private TextView tvChatListEmpty;
    private TextView tvChatMessageEmpty;
    private TextView tvSelectedMedia;
    private EditText etChatSearch;
    private EditText etChatMessage;
    private ProgressBar progressChatList;
    private ProgressBar progressChatMessages;

    private RecyclerView rvChatConversations;
    private RecyclerView rvChatMessages;

    private ChatConversationAdapter conversationAdapter;
    private ChatMessageAdapter messageAdapter;

    private ChatRepository chatRepository;
    private String currentUserId;
    private Conversation activeConversation;
    private String pendingOpenConversationId;
    private final List<Conversation> allConversations = new ArrayList<>();
    private final Map<String, String> draftByConversationId = new HashMap<>();
    private final Map<String, ArrayList<Uri>> mediaDraftByConversationId = new HashMap<>();
    private final Set<String> unreadConversationIds = new HashSet<>();

    private final android.os.Handler pollingHandler = new android.os.Handler();
    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            refreshConversations(false);
            refreshIncomingUnread();
            if (activeConversation != null) {
                loadMessages(activeConversation.getConversationId(), false);
            }
            pollingHandler.postDelayed(this, POLLING_INTERVAL_MS);
        }
    };

    private final ActivityResultLauncher<String[]> mediaPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), this::onMediaPicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatRepository = new ChatRepository(ApiClient.getApiService(), new AuthSessionManager(this));
        currentUserId = chatRepository.getCurrentUserId();
        pendingOpenConversationId = getIntent().getStringExtra(EXTRA_OPEN_CONVERSATION_ID);

        if (!chatRepository.isLoggedIn()) {
            Toast.makeText(this, R.string.chat_login_required, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupRecyclerViews();
        setupActions();
        setupBackHandling();

        showListScreen();
        refreshConversations(true);
        refreshIncomingUnread();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPolling();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPolling();
        saveDraftForActiveConversation();
    }

    private void bindViews() {
        chatListContainer = findViewById(R.id.chatListContainer);
        chatMessageContainer = findViewById(R.id.chatMessageContainer);
        tvChatHeaderName = findViewById(R.id.tvChatHeaderName);
        tvChatHeaderStatus = findViewById(R.id.tvChatHeaderStatus);
        tvChatListEmpty = findViewById(R.id.tvChatListEmpty);
        tvChatMessageEmpty = findViewById(R.id.tvChatMessageEmpty);
        tvSelectedMedia = findViewById(R.id.tvSelectedMedia);
        etChatSearch = findViewById(R.id.etChatSearch);
        etChatMessage = findViewById(R.id.etChatMessage);
        progressChatList = findViewById(R.id.progressChatList);
        progressChatMessages = findViewById(R.id.progressChatMessages);
        rvChatConversations = findViewById(R.id.rvChatConversations);
        rvChatMessages = findViewById(R.id.rvChatMessages);
    }

    private void setupRecyclerViews() {
        conversationAdapter = new ChatConversationAdapter(this::openConversation);
        conversationAdapter.setCurrentUserId(currentUserId);

        rvChatConversations.setLayoutManager(new LinearLayoutManager(this));
        rvChatConversations.setAdapter(conversationAdapter);

        messageAdapter = new ChatMessageAdapter();
        messageAdapter.setCurrentUserId(currentUserId);
        rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
        rvChatMessages.setAdapter(messageAdapter);
    }

    private void setupActions() {
        findViewById(R.id.btnChatBackHome).setOnClickListener(v -> finish());
        findViewById(R.id.btnChatBack).setOnClickListener(v -> showListScreen());
        findViewById(R.id.btnSendMessage).setOnClickListener(v -> sendMessage());
        findViewById(R.id.btnPickMedia).setOnClickListener(v -> mediaPickerLauncher.launch(new String[]{"image/*", "video/*"}));

        etChatSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterConversations();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op.
            }
        });
    }

    private void setupBackHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (chatMessageContainer != null && chatMessageContainer.getVisibility() == View.VISIBLE) {
                    showListScreen();
                    return;
                }

                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    private void refreshConversations(boolean showLoading) {
        if (showLoading) {
            progressChatList.setVisibility(View.VISIBLE);
        }

        chatRepository.getConversations(new ChatRepository.RepositoryCallback<List<Conversation>>() {
            @Override
            public void onSuccess(List<Conversation> data) {
                progressChatList.setVisibility(View.GONE);
                allConversations.clear();
                allConversations.addAll(data);
                filterConversations();

                if (!TextUtils.isEmpty(pendingOpenConversationId)) {
                    Conversation target = findConversationById(pendingOpenConversationId);
                    if (target != null) {
                        openConversation(target);
                        pendingOpenConversationId = null;
                    }
                }

                if (activeConversation != null) {
                    Conversation updated = findConversationById(activeConversation.getConversationId());
                    if (updated != null) {
                        activeConversation = updated;
                        bindConversationHeader(updated);
                    }
                }
            }

            @Override
            public void onError(String message) {
                progressChatList.setVisibility(View.GONE);
                if (allConversations.isEmpty()) {
                    tvChatListEmpty.setVisibility(View.VISIBLE);
                    tvChatListEmpty.setText(message);
                }
            }
        });
    }

    private void refreshIncomingUnread() {
        chatRepository.getIncomingUnread(new ChatRepository.RepositoryCallback<List<Message>>() {
            @Override
            public void onSuccess(List<Message> data) {
                unreadConversationIds.clear();
                for (Message message : data) {
                    if (!TextUtils.isEmpty(message.getConversationId())) {
                        unreadConversationIds.add(message.getConversationId());
                    }
                }
                conversationAdapter.setUnreadConversationIds(unreadConversationIds);
            }

            @Override
            public void onError(String message) {
                // Keep silent for unread polling.
            }
        });
    }

    private void filterConversations() {
        String query = etChatSearch.getText() == null ? "" : etChatSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<Conversation> filtered = new ArrayList<>();

        for (Conversation conversation : allConversations) {
            String name = getOtherUserName(conversation).toLowerCase(Locale.ROOT);
            String preview = conversation.getLastMessage() == null ? "" : conversation.getLastMessage().toLowerCase(Locale.ROOT);
            if (TextUtils.isEmpty(query) || name.contains(query) || preview.contains(query)) {
                filtered.add(conversation);
            }
        }

        conversationAdapter.setConversations(filtered);
        boolean isEmpty = filtered.isEmpty();
        tvChatListEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (isEmpty) {
            tvChatListEmpty.setText(getString(R.string.chat_list_empty));
        }
    }

    private void openConversation(@NonNull Conversation conversation) {
        saveDraftForActiveConversation();
        activeConversation = conversation;
        bindConversationHeader(conversation);

        etChatMessage.setError(null);
        etChatMessage.setText(getDraftForConversation(conversation.getConversationId()));
        etChatMessage.setSelection(etChatMessage.getText().length());
        updateSelectedMediaUi();

        chatListContainer.setVisibility(View.GONE);
        chatMessageContainer.setVisibility(View.VISIBLE);

        loadMessages(conversation.getConversationId(), true);
    }

    private void bindConversationHeader(Conversation conversation) {
        String name = getOtherUserName(conversation);
        tvChatHeaderName.setText(TextUtils.isEmpty(name)
                ? getString(R.string.chat_unknown_user)
                : name);
        tvChatHeaderStatus.setText(getString(R.string.chat_status_unknown));
    }

    private void loadMessages(String conversationId, boolean showLoading) {
        if (showLoading) {
            progressChatMessages.setVisibility(View.VISIBLE);
        }

        chatRepository.getMessages(conversationId, new ChatRepository.RepositoryCallback<List<Message>>() {
            @Override
            public void onSuccess(List<Message> data) {
                progressChatMessages.setVisibility(View.GONE);
                messageAdapter.setMessages(data);
                tvChatMessageEmpty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
                if (data.isEmpty()) {
                    tvChatMessageEmpty.setText(getString(R.string.chat_message_empty));
                } else {
                    scrollMessagesToBottom();
                }
            }

            @Override
            public void onError(String message) {
                progressChatMessages.setVisibility(View.GONE);
                messageAdapter.setMessages(new ArrayList<>());
                tvChatMessageEmpty.setVisibility(View.VISIBLE);
                tvChatMessageEmpty.setText(message);
            }
        });
    }

    private void sendMessage() {
        if (activeConversation == null) {
            return;
        }

        String text = etChatMessage.getText() == null ? "" : etChatMessage.getText().toString().trim();
        int mediaCount = getMediaDraftCount(activeConversation.getConversationId());

        String contentToSend = text;
        if (TextUtils.isEmpty(contentToSend) && mediaCount > 0) {
            contentToSend = getString(R.string.chat_media_only_placeholder, mediaCount);
        } else if (!TextUtils.isEmpty(contentToSend) && mediaCount > 0) {
            contentToSend = contentToSend + "\n" + getString(R.string.chat_media_note_inline, mediaCount);
        }

        if (TextUtils.isEmpty(contentToSend)) {
            etChatMessage.setError(getString(R.string.chat_message_or_media_required));
            return;
        }

        etChatMessage.setError(null);
        String conversationId = activeConversation.getConversationId();
        chatRepository.sendMessage(conversationId, contentToSend, new ChatRepository.RepositoryCallback<Message>() {
            @Override
            public void onSuccess(Message data) {
                etChatMessage.setText("");
                draftByConversationId.remove(conversationId);
                mediaDraftByConversationId.remove(conversationId);
                updateSelectedMediaUi();

                messageAdapter.addMessage(data);
                tvChatMessageEmpty.setVisibility(View.GONE);
                scrollMessagesToBottom();
                refreshConversations(false);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ChatActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showListScreen() {
        saveDraftForActiveConversation();
        hideKeyboardAndClearInputFocus();
        chatListContainer.setVisibility(View.VISIBLE);
        chatMessageContainer.setVisibility(View.GONE);
    }

    private void saveDraftForActiveConversation() {
        if (activeConversation == null || etChatMessage == null) {
            return;
        }

        String conversationId = activeConversation.getConversationId();
        if (TextUtils.isEmpty(conversationId)) {
            return;
        }

        String draft = etChatMessage.getText() == null ? "" : etChatMessage.getText().toString();
        if (TextUtils.isEmpty(draft.trim())) {
            draftByConversationId.remove(conversationId);
        } else {
            draftByConversationId.put(conversationId, draft);
        }
    }

    private String getDraftForConversation(String conversationId) {
        if (TextUtils.isEmpty(conversationId)) {
            return "";
        }
        String draft = draftByConversationId.get(conversationId);
        return draft == null ? "" : draft;
    }

    private void onMediaPicked(List<Uri> uris) {
        if (activeConversation == null || uris == null || uris.isEmpty()) {
            return;
        }

        String conversationId = activeConversation.getConversationId();
        if (TextUtils.isEmpty(conversationId)) {
            return;
        }

        ArrayList<Uri> mediaUris = getOrCreateMediaDraft(conversationId);
        for (Uri uri : uris) {
            if (uri != null && !mediaUris.contains(uri)) {
                mediaUris.add(uri);
            }
        }

        updateSelectedMediaUi();
        Toast.makeText(this, getString(R.string.chat_media_selected_count, mediaUris.size()), Toast.LENGTH_SHORT).show();
    }

    private ArrayList<Uri> getOrCreateMediaDraft(String conversationId) {
        ArrayList<Uri> uris = mediaDraftByConversationId.get(conversationId);
        if (uris == null) {
            uris = new ArrayList<>();
            mediaDraftByConversationId.put(conversationId, uris);
        }
        return uris;
    }

    private int getMediaDraftCount(String conversationId) {
        ArrayList<Uri> uris = mediaDraftByConversationId.get(conversationId);
        return uris == null ? 0 : uris.size();
    }

    private void updateSelectedMediaUi() {
        if (tvSelectedMedia == null || activeConversation == null) {
            return;
        }

        int count = getMediaDraftCount(activeConversation.getConversationId());
        if (count <= 0) {
            tvSelectedMedia.setVisibility(View.GONE);
            tvSelectedMedia.setText("");
            return;
        }

        tvSelectedMedia.setVisibility(View.VISIBLE);
        tvSelectedMedia.setText(getString(R.string.chat_media_selected_count, count));
    }

    private Conversation findConversationById(String conversationId) {
        if (TextUtils.isEmpty(conversationId)) {
            return null;
        }
        for (Conversation conversation : allConversations) {
            if (TextUtils.equals(conversationId, conversation.getConversationId())) {
                return conversation;
            }
        }
        return null;
    }

    private String getOtherUserName(Conversation conversation) {
        if (conversation == null) {
            return "";
        }

        UserBrief seller = conversation.getSeller();
        UserBrief buyer = conversation.getBuyer();
        boolean isCurrentSeller = seller != null && TextUtils.equals(currentUserId, seller.getUserId());
        UserBrief other = isCurrentSeller ? buyer : seller;
        if (other == null) {
            other = isCurrentSeller ? seller : buyer;
        }

        return other == null ? "" : other.getFullName();
    }

    private void scrollMessagesToBottom() {
        if (messageAdapter.getItemCount() > 0) {
            rvChatMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
        }
    }

    private void startPolling() {
        stopPolling();
        pollingHandler.postDelayed(pollingRunnable, POLLING_INTERVAL_MS);
    }

    private void stopPolling() {
        pollingHandler.removeCallbacks(pollingRunnable);
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

}

