package com.example.a2hauto;

import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.GestureDetector;
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
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import androidx.core.content.ContextCompat;

public class ChatActivity extends AppCompatActivity {

    private static final long POLLING_INTERVAL_MS = 7000L;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    public static final String EXTRA_OPEN_CONVERSATION_ID = "extra_open_conversation_id";
    public static final String EXTRA_OPEN_CONVERSATION_JSON = "extra_open_conversation_json";

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
    private Conversation pendingOpenConversationSeed;
    private final List<Conversation> allConversations = new ArrayList<>();
    private final Map<String, String> draftByConversationId = new HashMap<>();
    private final Map<String, ArrayList<Uri>> mediaDraftByConversationId = new HashMap<>();
    private final Set<String> unreadConversationIds = new HashSet<>();
    
    private GestureDetector gestureDetector;
    private int currentNavItem = 3; // Chat position
    private int previousNavItem = 2; // Track previous position
    
    // Navbar items
    private LinearLayout navHome, navFavorites, navPost, navChat, navAccount;

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
        pendingOpenConversationSeed = parseConversationSeedFromIntent();

        if (!chatRepository.isLoggedIn()) {
            Toast.makeText(this, R.string.chat_login_required, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupRecyclerViews();
        setupActions();
        setupBackHandling();
        setupGestureDetection();
        setupBottomNavigation();

        showListScreen();
        tryOpenSeedConversation();
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
                        if (activeConversation == null
                                || !TextUtils.equals(activeConversation.getConversationId(), target.getConversationId())) {
                            openConversation(target);
                        } else {
                            activeConversation = target;
                            bindConversationHeader(target);
                        }
                        pendingOpenConversationId = null;
                        pendingOpenConversationSeed = null;
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

    private void tryOpenSeedConversation() {
        if (pendingOpenConversationSeed == null) {
            return;
        }

        if (TextUtils.isEmpty(pendingOpenConversationSeed.getConversationId())) {
            pendingOpenConversationSeed = null;
            return;
        }

        openConversation(pendingOpenConversationSeed);
    }

    private Conversation parseConversationSeedFromIntent() {
        String rawJson = getIntent().getStringExtra(EXTRA_OPEN_CONVERSATION_JSON);
        if (TextUtils.isEmpty(rawJson)) {
            return null;
        }

        try {
            return new Gson().fromJson(rawJson, Conversation.class);
        } catch (Exception ignored) {
            return null;
        }
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
        // Handle gesture detection for swipe
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(ev);
        }
        
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

    private void setupBottomNavigation() {
        // Get references to all nav items
        navHome = findViewById(R.id.navHome);
        navFavorites = findViewById(R.id.navFavorites);
        navPost = findViewById(R.id.navPost);
        navChat = findViewById(R.id.navChat);
        navAccount = findViewById(R.id.navAccount);

        // Set up navigation click listeners
        navHome.setOnClickListener(v -> {
            selectNavItem(0);
            startActivity(new Intent(this, MainActivity.class));
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
        
        navFavorites.setOnClickListener(v -> {
            selectNavItem(1);
            startActivity(new Intent(this, FavoritesActivity.class));
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
        
        navPost.setOnClickListener(v -> {
            selectNavItem(2);
            startActivity(new Intent(this, NewsListingsActivity.class));
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
    
        navChat.setOnClickListener(v -> {
            // Already on chat screen
            selectNavItem(3);
        });
        
        navAccount.setOnClickListener(v -> {
            selectNavItem(4);
            Toast.makeText(this, "Account page coming soon", Toast.LENGTH_SHORT).show();
        });
        
        // Set initial highlight to Chat
        selectNavItem(3);
    }

    private void selectNavItem(int navIndex) {
        // Remove highlight from all items
        resetAllNavItems();
        
        // Highlight selected item
        currentNavItem = navIndex;
        LinearLayout selectedNav = null;
        
        switch (navIndex) {
            case 0:
                selectedNav = navHome;
                break;
            case 1:
                selectedNav = navFavorites;
                break;
            case 2:
                selectedNav = navPost;
                break;
            case 3:
                selectedNav = navChat;
                break;
            case 4:
                selectedNav = navAccount;
                break;
        }
        
        if (selectedNav != null) {
            highlightNavItem(selectedNav);
        }
    }

    private void resetAllNavItems() {
        if (navHome != null) unhighlightNavItem(navHome);
        if (navFavorites != null) unhighlightNavItem(navFavorites);
        if (navPost != null) unhighlightNavItem(navPost);
        if (navChat != null) unhighlightNavItem(navChat);
        if (navAccount != null) unhighlightNavItem(navAccount);
    }

    private void highlightNavItem(LinearLayout navItem) {
        // Add scale animation for transition
        navItem.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(200)
                .start();
        
        // Set background and update colors
        navItem.setBackgroundResource(R.drawable.bg_nav_active);
        
        // Update icon and text color for highlighted state
        for (int i = 0; i < navItem.getChildCount(); i++) {
            android.view.View child = navItem.getChildAt(i);
            if (child instanceof ImageView) {
                ((ImageView) child).setColorFilter(ContextCompat.getColor(this, R.color.primary_teal_dark), android.graphics.PorterDuff.Mode.SRC_IN);
            } else if (child instanceof TextView) {
                ((TextView) child).setTextColor(ContextCompat.getColor(this, R.color.primary_teal_dark));
                ((TextView) child).setTypeface(((TextView) child).getTypeface(), android.graphics.Typeface.BOLD);
            } else if (child instanceof FrameLayout) {
                // Handle FrameLayout (which contains the badge)
                for (int j = 0; j < ((FrameLayout) child).getChildCount(); j++) {
                    android.view.View grandChild = ((FrameLayout) child).getChildAt(j);
                    if (grandChild instanceof ImageView) {
                        ((ImageView) grandChild).setColorFilter(ContextCompat.getColor(this, R.color.primary_teal_dark), android.graphics.PorterDuff.Mode.SRC_IN);
                    }
                }
            }
        }
    }

    private void unhighlightNavItem(LinearLayout navItem) {
        // Reset scale animation
        navItem.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .start();
        
        // Remove background
        navItem.setBackground(null);
        
        // Reset icon and text color for unhighlighted state
        for (int i = 0; i < navItem.getChildCount(); i++) {
            android.view.View child = navItem.getChildAt(i);
            if (child instanceof ImageView) {
                ((ImageView) child).setColorFilter(ContextCompat.getColor(this, R.color.text_muted), android.graphics.PorterDuff.Mode.SRC_IN);
            } else if (child instanceof TextView) {
                ((TextView) child).setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                ((TextView) child).setTypeface(((TextView) child).getTypeface(), android.graphics.Typeface.NORMAL);
            } else if (child instanceof FrameLayout) {
                // Handle FrameLayout (which contains the badge)
                for (int j = 0; j < ((FrameLayout) child).getChildCount(); j++) {
                    android.view.View grandChild = ((FrameLayout) child).getChildAt(j);
                    if (grandChild instanceof ImageView) {
                        ((ImageView) grandChild).setColorFilter(ContextCompat.getColor(this, R.color.text_muted), android.graphics.PorterDuff.Mode.SRC_IN);
                    }
                }
            }
        }
    }

    private void setupGestureDetection() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                try {
                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();
                    
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                // Swipe Right: Chat(3) → Post(2)
                                onSwipeRight();
                            } else {
                                // Swipe Left: Chat(3) → Account(4)
                                onSwipeLeft();
                            }
                            return true;
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return false;
            }
        });
    }

    private void onSwipeRight() {
        // Swipe Right: Chat(3) → Post(2)
        previousNavItem = currentNavItem;
        currentNavItem = 2;
        startActivity(new Intent(this, NewsListingsActivity.class));
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void onSwipeLeft() {
        // Swipe Left: Chat(3) → Account(4)
        // Account page doesn't exist yet, so navigate to home for now
        previousNavItem = currentNavItem;
        currentNavItem = 4;
        Toast.makeText(this, "Account page coming soon", Toast.LENGTH_SHORT).show();
        // Commented out until Account page is implemented:
        // startActivity(new Intent(this, AccountActivity.class));
        // finish();
        // overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

}

