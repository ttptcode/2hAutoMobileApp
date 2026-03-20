package com.example.a2hauto;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a2hauto.adapter.VehicleAdapter;
import com.example.a2hauto.api.ApiClient;
import com.example.a2hauto.api.ApiService;
import com.example.a2hauto.auth.AuthSessionManager;
import com.example.a2hauto.chat.ChatRepository;
import com.example.a2hauto.auth.LoginDialogFragment;
import com.example.a2hauto.auth.RegisterDialogFragment;
import com.example.a2hauto.model.ApiResponse;
import com.example.a2hauto.model.FavoriteItem;
import com.example.a2hauto.model.Item;
import com.example.a2hauto.model.Listing;
import com.example.a2hauto.model.Message;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements LoginDialogFragment.LoginDialogListener, RegisterDialogFragment.RegisterDialogListener {

    private enum HomeCategoryFilter {
        ALL,
        CAR,
        MOTORBIKE,
        ELECTRIC,
        ACCESSORIES
    }

    private static final String TAG = "MainActivity";
    private static final float MINI_HEADER_FADE_START = 0.38f;
    private static final float MINI_HEADER_FADE_END = 0.72f;
    private static final long UNREAD_POLLING_INTERVAL_MS = 10000L;
    private RecyclerView rvVehicles;
    private VehicleAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvListingCount;
    private TextView tvMiniListingCount;
    private TextView tvSectionSubtitle;
    private TextView tvEmptyState;
    private TextView tvFilterStatus;
    private android.widget.EditText etHeroSearch;
    private android.widget.EditText etMiniSearch;
    private View btnHeroClearSearch;
    private View btnMiniClearSearch;
    private TextView chipHomeAll;
    private TextView chipHomeCar;
    private TextView chipHomeMotorbike;
    private TextView chipHomeElectric;
    private TextView chipHomeAccessories;
    private View btnHeaderUpgrade;
    private View btnHeaderLogin;
    private View btnMiniHeaderLogin;
    private TextView tvHeaderAvatar;
    private TextView tvMiniHeaderAvatar;
    private ImageView ivNavAccountIcon;
    private TextView tvNavAccountLabel;
    private TextView tvNavChatBadge;
    private View miniHeaderCard;
    private AppBarLayout appBarLayout;
    private FrameLayout bottomNavContainer;
    
    // Navbar items
    private LinearLayout navHome, navFavorites, navPost, navChat, navAccount;
    private int currentNavItem = 0; // 0=Home, 1=Favorites, 2=Post, 3=Chat, 4=Account
    private int previousNavItem = 0; // Track previous position for smart transitions
    
    private AuthSessionManager authSessionManager;
    private ApiService apiService;
    private ChatRepository chatRepository;
    private final Set<String> cachedFavoriteListingIds = new HashSet<>();
    private final android.os.Handler unreadHandler = new android.os.Handler();
    private final Runnable unreadPollingRunnable = new Runnable() {
        @Override
        public void run() {
            refreshUnreadBadge();
            unreadHandler.postDelayed(this, UNREAD_POLLING_INTERVAL_MS);
        }
    };

    // Gesture detection
    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    private final List<Listing> allActiveListings = new ArrayList<>();
    private String homeSearchQuery = "";
    private HomeCategoryFilter selectedHomeCategory = HomeCategoryFilter.ALL;
    private boolean isSyncingSearchInputs = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvVehicles = findViewById(R.id.rvVehicles);
        progressBar = findViewById(R.id.progressBar);
        tvListingCount = findViewById(R.id.tvListingCount);
        tvMiniListingCount = findViewById(R.id.tvMiniListingCount);
        tvSectionSubtitle = findViewById(R.id.tvSectionSubtitle);
        tvFilterStatus = findViewById(R.id.tvFilterStatus);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        etHeroSearch = findViewById(R.id.etHeroSearch);
        etMiniSearch = findViewById(R.id.etMiniSearch);
        btnHeroClearSearch = findViewById(R.id.btnHeroFilter);
        btnMiniClearSearch = findViewById(R.id.btnMiniFilter);
        chipHomeAll = findViewById(R.id.chipHomeAll);
        chipHomeCar = findViewById(R.id.chipHomeCar);
        chipHomeMotorbike = findViewById(R.id.chipHomeMotorbike);
        chipHomeElectric = findViewById(R.id.chipHomeElectric);
        chipHomeAccessories = findViewById(R.id.chipHomeAccessories);
        btnHeaderUpgrade = findViewById(R.id.btnHeaderUpgrade);
        btnHeaderLogin = findViewById(R.id.btnHeaderLogin);
        btnMiniHeaderLogin = findViewById(R.id.btnMiniHeaderLogin);
        tvHeaderAvatar = findViewById(R.id.tvHeaderAvatar);
        tvMiniHeaderAvatar = findViewById(R.id.tvMiniHeaderAvatar);
        ivNavAccountIcon = findViewById(R.id.ivNavAccountIcon);
        tvNavAccountLabel = findViewById(R.id.tvNavAccountLabel);
        tvNavChatBadge = findViewById(R.id.tvNavChatBadge);
        miniHeaderCard = findViewById(R.id.miniHeaderCard);
        appBarLayout = findViewById(R.id.appBarLayout);
        bottomNavContainer = findViewById(R.id.bottomNavContainer);
        authSessionManager = new AuthSessionManager(this);
        chatRepository = new ChatRepository(ApiClient.getApiService(), authSessionManager);

        rvVehicles.setLayoutManager(new LinearLayoutManager(this));
        rvVehicles.setHasFixedSize(true);
        adapter = new VehicleAdapter(new ArrayList<>());
        rvVehicles.setAdapter(adapter);
        updateHomeFilterUi();
        attachSearchInputListeners();

        setupActions();
        setupMiniHeaderBehavior();
        setupBottomNavigation();
        setupGestureDetection();
        refreshAuthHeaderUi();
        initRetrofit();
        fetchListings();
        syncFavoritesFromServer();
    }

       private void setupActions() {
        findViewById(R.id.btnMenu).setOnClickListener(v -> showCategoryMenuDialog());
        findViewById(R.id.btnHeaderFavorite).setOnClickListener(v -> openFavoritesScreen());
        findViewById(R.id.btnMiniMenu).setOnClickListener(v -> showCategoryMenuDialog());
        findViewById(R.id.btnMiniFavorite).setOnClickListener(v -> openFavoritesScreen());
        btnHeroClearSearch.setOnClickListener(v -> clearSearchQuery());
        btnMiniClearSearch.setOnClickListener(v -> clearSearchQuery());
        chipHomeAll.setOnClickListener(v -> setHomeCategoryFilter(HomeCategoryFilter.ALL));
        chipHomeCar.setOnClickListener(v -> setHomeCategoryFilter(HomeCategoryFilter.CAR));
        chipHomeMotorbike.setOnClickListener(v -> setHomeCategoryFilter(HomeCategoryFilter.MOTORBIKE));
        chipHomeElectric.setOnClickListener(v -> setHomeCategoryFilter(HomeCategoryFilter.ELECTRIC));
        chipHomeAccessories.setOnClickListener(v -> setHomeCategoryFilter(HomeCategoryFilter.ACCESSORIES));
        btnHeaderUpgrade.setOnClickListener(v -> showUpgradeDialog());
        findViewById(R.id.miniSearchBar).setOnClickListener(v -> showComingSoon(getString(R.string.search_hint)));
        btnHeaderUpgrade.setOnClickListener(v -> startActivity(new Intent(this, PlanActivity.class)));
        btnHeaderLogin.setOnClickListener(v -> handleAccountAction());
        btnMiniHeaderLogin.setOnClickListener(v -> handleAccountAction());
        tvHeaderAvatar.setOnClickListener(v -> handleAccountAction());
        tvMiniHeaderAvatar.setOnClickListener(v -> handleAccountAction());
        findViewById(R.id.navHome).setOnClickListener(v -> rvVehicles.smoothScrollToPosition(0));
        findViewById(R.id.navFavorites).setOnClickListener(v -> showComingSoon(getString(R.string.nav_favorites)));
        findViewById(R.id.navChat).setOnClickListener(v -> openChatScreen());
        findViewById(R.id.navPost).setOnClickListener(v -> handlePostAction());
        findViewById(R.id.navAccount).setOnClickListener(v -> handleAccountAction());
    }


    @Override
    protected void onResume() {
        super.onResume();
        refreshAuthHeaderUi();
        syncFavoritesFromServer();
        startUnreadPolling();
        refreshUnreadBadge();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopUnreadPolling();
    }

    private void openChatScreen() {
        if (!chatRepository.isLoggedIn()) {
            showLoginDialog();
            return;
        }
        startActivity(new Intent(this, ChatActivity.class));
    }

    private void startUnreadPolling() {
        stopUnreadPolling();
        unreadHandler.postDelayed(unreadPollingRunnable, UNREAD_POLLING_INTERVAL_MS);
    }

    private void stopUnreadPolling() {
        unreadHandler.removeCallbacks(unreadPollingRunnable);
    }

    private void refreshUnreadBadge() {
        if (tvNavChatBadge == null) {
            return;
        }

        if (!chatRepository.isLoggedIn()) {
            tvNavChatBadge.setVisibility(View.GONE);
            return;
        }

        chatRepository.getIncomingUnread(new ChatRepository.RepositoryCallback<List<Message>>() {
            @Override
            public void onSuccess(List<Message> data) {
                int count = data == null ? 0 : data.size();
                if (count <= 0) {
                    tvNavChatBadge.setVisibility(View.GONE);
                    return;
                }
                tvNavChatBadge.setText(String.valueOf(Math.min(count, 99)));
                tvNavChatBadge.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                tvNavChatBadge.setVisibility(View.GONE);
            }
        });
        // Restore navigation highlight
        if (currentNavItem != 0) {
            selectNavItem(currentNavItem);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Handle gesture detection for swipe
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(ev);
        }

        // Handle search input focus
        if (ev != null && ev.getAction() == MotionEvent.ACTION_DOWN) {
            View focusedView = getCurrentFocus();
            boolean focusedSearchInput = focusedView == etHeroSearch || focusedView == etMiniSearch;
            if (focusedSearchInput) {
                boolean touchInsideSearch = isTouchInsideView(etHeroSearch, ev) || isTouchInsideView(etMiniSearch, ev);
                if (!touchInsideSearch) {
                    finishHomeSearchInteraction(focusedView);
                }
            }
        }
        
        return super.dispatchTouchEvent(ev);
    }

    private boolean isTouchInsideView(View targetView, MotionEvent ev) {
        if (targetView == null || ev == null || targetView.getVisibility() != View.VISIBLE) {
            return false;
        }

        Rect hitRect = new Rect();
        targetView.getGlobalVisibleRect(hitRect);
        return hitRect.contains((int) ev.getRawX(), (int) ev.getRawY());
    }

    private void openFavoritesScreen() {
        if (!authSessionManager.isLoggedIn()) {
            showLoginDialog();
            return;
        }
        previousNavItem = currentNavItem;
        currentNavItem = 1;
        startActivity(new Intent(this, FavoritesActivity.class));
        // Home(0) → Favorites(1): left to right
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void setupMiniHeaderBehavior() {
        updateMiniHeaderProgress(0f);
        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            if (appBarLayout.getTotalScrollRange() == 0) {
                return;
            }

            float collapseRatio = Math.abs(verticalOffset) / (float) appBarLayout.getTotalScrollRange();
            float progress = (collapseRatio - MINI_HEADER_FADE_START) / (MINI_HEADER_FADE_END - MINI_HEADER_FADE_START);
            updateMiniHeaderProgress(Math.max(0f, Math.min(1f, progress)));
        });
    }

    private void updateMiniHeaderProgress(float progress) {
        miniHeaderCard.animate().cancel();

        miniHeaderCard.setVisibility(progress > 0.01f ? View.VISIBLE : View.INVISIBLE);
        miniHeaderCard.setAlpha(progress);
        miniHeaderCard.setTranslationY((1f - progress) * -20f);
    }

    private void setupBottomNavigation() {
        // Get references to all nav items from activity_main.xml
        navHome = findViewById(R.id.navHome);
        navFavorites = findViewById(R.id.navFavorites);
        navPost = findViewById(R.id.navPost);
        navChat = findViewById(R.id.navChat);
        navAccount = findViewById(R.id.navAccount);

        // Set up navigation click listeners with highlight
        navHome.setOnClickListener(v -> {
            selectNavItem(0);
            rvVehicles.smoothScrollToPosition(0);
        });
        
        navFavorites.setOnClickListener(v -> {
            selectNavItem(1);
            openFavoritesScreen();
        });
        
        navPost.setOnClickListener(v -> {
            selectNavItem(2);
            handlePostAction();
        });
    
        navChat.setOnClickListener(v -> {
            selectNavItem(3);
            openChatScreen();
        });
        
        navAccount.setOnClickListener(v -> {
            selectNavItem(4);
            handleAccountAction();
        });
        
        // Set initial highlight
        selectNavItem(0);
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
            }
        }
    }

    private void handleAccountAction() {
        if (!authSessionManager.isLoggedIn()) {
            showLoginDialog();
            return;
        }

        showAccountBottomSheet();
    }

    private void showAccountBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View contentView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_account_actions, findViewById(android.R.id.content), false);
        dialog.setContentView(contentView);

        View optionProfile = contentView.findViewById(R.id.optionProfile);
        View optionLogout = contentView.findViewById(R.id.optionLogout);

        optionProfile.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, ProfileActivity.class));
        });

        optionLogout.setOnClickListener(v -> {
            dialog.dismiss();
            authSessionManager.logout();
            refreshAuthHeaderUi();
            syncFavoritesFromServer();
            Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();
            showLoginDialog();
        });

        dialog.show();
    }

    private void showLoginDialog() {
        if (getSupportFragmentManager().findFragmentByTag(LoginDialogFragment.TAG) == null) {
            new LoginDialogFragment().show(getSupportFragmentManager(), LoginDialogFragment.TAG);
        }
    }

    private void showRegisterDialog() {
        if (getSupportFragmentManager().findFragmentByTag(RegisterDialogFragment.TAG) == null) {
            new RegisterDialogFragment().show(getSupportFragmentManager(), RegisterDialogFragment.TAG);
        }
    }

    private void showUpgradeDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_upgrade_freemium, null, false);
        new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_upgrade_now,
                        (dialog, which) -> showComingSoon(getString(R.string.action_upgrade)))
                .show();
    }

    private void handlePostAction() {
        if (!authSessionManager.isLoggedIn()) {
            showLoginDialog();
            return;
        }

        previousNavItem = currentNavItem;
        currentNavItem = 2;
        startActivity(new Intent(this, NewsListingsActivity.class));
        // Home(0) → Post(2): left to right
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void showCategoryMenuDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_category_menu, null, false);
        LinearLayout optionContainer = dialogView.findViewById(R.id.menuOptionContainer);

        List<CategoryMenuItem> menuItems = new ArrayList<>();
        menuItems.add(new CategoryMenuItem(getString(R.string.home_filter_all), android.R.drawable.ic_menu_sort_alphabetically, HomeCategoryFilter.ALL));
        menuItems.add(new CategoryMenuItem(getString(R.string.category_car), R.drawable.ic_menu_category_car, HomeCategoryFilter.CAR));
        menuItems.add(new CategoryMenuItem(getString(R.string.category_motorbike), R.drawable.ic_menu_category_motorbike, HomeCategoryFilter.MOTORBIKE));
        menuItems.add(new CategoryMenuItem(getString(R.string.category_electric), R.drawable.ic_menu_category_electric, HomeCategoryFilter.ELECTRIC));
        menuItems.add(new CategoryMenuItem(getString(R.string.category_accessories), android.R.drawable.ic_menu_manage, HomeCategoryFilter.ACCESSORIES));

        final androidx.appcompat.app.AlertDialog[] menuDialogRef = new androidx.appcompat.app.AlertDialog[1];
        for (CategoryMenuItem item : menuItems) {
            View optionView = inflater.inflate(R.layout.item_menu_option, optionContainer, false);
            ImageView ivIcon = optionView.findViewById(R.id.ivMenuOptionIcon);
            TextView tvTitle = optionView.findViewById(R.id.tvMenuOptionTitle);

            ivIcon.setImageResource(item.iconResId);
            tvTitle.setText(item.title);
            optionView.setOnClickListener(v -> {
                setHomeCategoryFilter(item.categoryFilter);
                if (menuDialogRef[0] != null) {
                    menuDialogRef[0].dismiss();
                }
            });

            optionContainer.addView(optionView);
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.home_filter_menu_title)
                .setView(dialogView)
                .create();
        menuDialogRef[0] = dialog;
        dialog.show();
    }

    private static class CategoryMenuItem {
        private final String title;
        private final int iconResId;
        private final HomeCategoryFilter categoryFilter;

        private CategoryMenuItem(String title, int iconResId, HomeCategoryFilter categoryFilter) {
            this.title = title;
            this.iconResId = iconResId;
            this.categoryFilter = categoryFilter;
        }
    }

    private void setHomeCategoryFilter(HomeCategoryFilter categoryFilter) {
        selectedHomeCategory = categoryFilter;
        applyHomeFilters();
    }

    private void clearSearchQuery() {
        if (!TextUtils.isEmpty(homeSearchQuery)) {
            homeSearchQuery = "";
            syncSearchInputs();
            applyHomeFilters();
        }
        finishHomeSearchInteraction(null);
    }

    private void attachSearchInputListeners() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No-op.
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (isSyncingSearchInputs) {
                    return;
                }

                String latestQuery = editable == null ? "" : editable.toString();
                if (TextUtils.equals(homeSearchQuery, latestQuery)) {
                    return;
                }

                homeSearchQuery = latestQuery;
                syncSearchInputs();
                applyHomeFilters();
            }
        };

        etHeroSearch.addTextChangedListener(watcher);
        etMiniSearch.addTextChangedListener(watcher);

        android.widget.TextView.OnEditorActionListener editorActionListener = (view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                finishHomeSearchInteraction(view);
                return true;
            }
            return false;
        };
        etHeroSearch.setOnEditorActionListener(editorActionListener);
        etMiniSearch.setOnEditorActionListener(editorActionListener);
    }

    private void finishHomeSearchInteraction(View sourceView) {
        etHeroSearch.clearFocus();
        etMiniSearch.clearFocus();

        View keyboardAnchor = sourceView != null ? sourceView : getCurrentFocus();
        if (keyboardAnchor == null) {
            keyboardAnchor = etHeroSearch;
        }

        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && keyboardAnchor.getWindowToken() != null) {
            inputMethodManager.hideSoftInputFromWindow(keyboardAnchor.getWindowToken(), 0);
        }
    }

    private void syncSearchInputs() {
        isSyncingSearchInputs = true;
        if (!TextUtils.equals(etHeroSearch.getText(), homeSearchQuery)) {
            etHeroSearch.setText(homeSearchQuery);
            etHeroSearch.setSelection(etHeroSearch.getText() == null ? 0 : etHeroSearch.getText().length());
        }
        if (!TextUtils.equals(etMiniSearch.getText(), homeSearchQuery)) {
            etMiniSearch.setText(homeSearchQuery);
            etMiniSearch.setSelection(etMiniSearch.getText() == null ? 0 : etMiniSearch.getText().length());
        }
        isSyncingSearchInputs = false;
    }

    private void applyHomeFilters() {
        List<Listing> filteredListings = new ArrayList<>();
        for (Listing listing : allActiveListings) {
            if (matchesCategory(listing) && matchesSearch(listing)) {
                filteredListings.add(listing);
            }
        }

        updateHomeFilterUi();
        updateListingUi(filteredListings);
    }

    private boolean matchesCategory(Listing listing) {
        if (selectedHomeCategory == HomeCategoryFilter.ALL) {
            return true;
        }

        String content = createSearchableText(listing);
        switch (selectedHomeCategory) {
            case CAR:
                return content.contains("oto") || content.contains("o to") || content.contains("car") || content.contains("sedan") || content.contains("suv");
            case MOTORBIKE:
                return content.contains("xe may") || content.contains("xemay") || content.contains("motor") || content.contains("moto");
            case ELECTRIC:
                return content.contains("xe dien") || content.contains("xedien") || content.contains("electric") || content.contains("ev");
            case ACCESSORIES:
                return content.contains("phu kien") || content.contains("phu tung") || content.contains("accessor") || content.contains("part");
            default:
                return true;
        }
    }

    private boolean matchesSearch(Listing listing) {
        String normalizedQuery = normalize(homeSearchQuery);
        if (TextUtils.isEmpty(normalizedQuery)) {
            return true;
        }
        return createSearchableText(listing).contains(normalizedQuery);
    }

    private String createSearchableText(Listing listing) {
        StringBuilder builder = new StringBuilder();
        builder.append(nonNull(listing.getDisplayTitle())).append(' ')
                .append(nonNull(listing.getAddress())).append(' ')
                .append(nonNull(listing.getDetail())).append(' ')
                .append(nonNull(listing.getListingType())).append(' ')
                .append(nonNull(listing.getUserName()));

        Item item = listing.getItem();
        if (item != null) {
            builder.append(' ')
                    .append(nonNull(item.getBrand())).append(' ')
                    .append(nonNull(item.getModel())).append(' ')
                    .append(nonNull(item.getItemTypeName()));
        }
        return normalize(builder.toString());
    }

    private String nonNull(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        return normalized.replace('đ', 'd');
    }

    private void updateHomeFilterUi() {
        syncSearchInputs();
        boolean hasSearchQuery = !TextUtils.isEmpty(homeSearchQuery);
        btnHeroClearSearch.setVisibility(hasSearchQuery ? View.VISIBLE : View.GONE);
        btnMiniClearSearch.setVisibility(hasSearchQuery ? View.VISIBLE : View.GONE);

        if (hasActiveHomeFilters()) {
            if (!TextUtils.isEmpty(homeSearchQuery) && selectedHomeCategory != HomeCategoryFilter.ALL) {
                tvFilterStatus.setText(getString(R.string.home_filter_result_with_both, getSelectedCategoryLabel(), homeSearchQuery));
            } else if (!TextUtils.isEmpty(homeSearchQuery)) {
                tvFilterStatus.setText(getString(R.string.home_filter_result_with_search, homeSearchQuery));
            } else {
                tvFilterStatus.setText(getString(R.string.home_filter_result_with_category, getSelectedCategoryLabel()));
            }
            tvFilterStatus.setVisibility(View.VISIBLE);
        } else {
            tvFilterStatus.setVisibility(View.GONE);
        }

        updateCategoryChipStyles();
    }

    private void updateCategoryChipStyles() {
        styleCategoryChip(chipHomeAll, selectedHomeCategory == HomeCategoryFilter.ALL);
        styleCategoryChip(chipHomeCar, selectedHomeCategory == HomeCategoryFilter.CAR);
        styleCategoryChip(chipHomeMotorbike, selectedHomeCategory == HomeCategoryFilter.MOTORBIKE);
        styleCategoryChip(chipHomeElectric, selectedHomeCategory == HomeCategoryFilter.ELECTRIC);
        styleCategoryChip(chipHomeAccessories, selectedHomeCategory == HomeCategoryFilter.ACCESSORIES);
    }

    private void styleCategoryChip(TextView chip, boolean isSelected) {
        if (chip == null) {
            return;
        }

        chip.setBackgroundResource(isSelected ? R.drawable.bg_nav_active : R.drawable.bg_filter_chip);
        chip.setTextColor(ContextCompat.getColor(this, isSelected ? R.color.primary_teal_dark : R.color.white));
        chip.setTypeface(chip.getTypeface(), isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        chip.setAlpha(isSelected ? 1f : 0.86f);
    }

    private boolean hasActiveHomeFilters() {
        return selectedHomeCategory != HomeCategoryFilter.ALL || !TextUtils.isEmpty(homeSearchQuery);
    }

    private String getSelectedCategoryLabel() {
        switch (selectedHomeCategory) {
            case CAR:
                return getString(R.string.category_car);
            case MOTORBIKE:
                return getString(R.string.category_motorbike);
            case ELECTRIC:
                return getString(R.string.category_electric);
            case ACCESSORIES:
                return getString(R.string.category_accessories);
            default:
                return getString(R.string.home_filter_all);
        }
    }

    private void refreshAuthHeaderUi() {
        boolean isLoggedIn = authSessionManager.isLoggedIn();
        String initials = getAvatarInitials(authSessionManager.getDisplayName(), authSessionManager.getPhoneNumber());

        btnHeaderLogin.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        btnMiniHeaderLogin.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        tvHeaderAvatar.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        tvMiniHeaderAvatar.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);

        tvHeaderAvatar.setText(initials);
        tvMiniHeaderAvatar.setText(initials);
        ivNavAccountIcon.setImageResource(R.drawable.ic_profile_outline);
        ivNavAccountIcon.setColorFilter(ContextCompat.getColor(this, isLoggedIn ? R.color.success_green : R.color.text_muted));
        tvNavAccountLabel.setTextColor(ContextCompat.getColor(this, isLoggedIn ? R.color.success_green : R.color.text_secondary));
    }

    private String getAvatarInitials(String displayName, String phoneNumber) {
        if (!TextUtils.isEmpty(displayName)) {
            String[] parts = displayName.trim().split("\\s+");
            if (parts.length >= 2) {
                return ("" + Character.toUpperCase(parts[0].charAt(0))
                        + Character.toUpperCase(parts[parts.length - 1].charAt(0)));
            }
            if (parts.length == 1 && !parts[0].isEmpty()) {
                return ("" + Character.toUpperCase(parts[0].charAt(0)));
            }
        }

        String normalizedPhone = phoneNumber == null ? "" : phoneNumber.replaceAll("\\D", "");
        if (normalizedPhone.length() >= 2) {
            return normalizedPhone.substring(normalizedPhone.length() - 2);
        }
        return "2H";
    }

    private void showComingSoon(String featureName) {
        Toast.makeText(this, getString(R.string.coming_soon, featureName), Toast.LENGTH_SHORT).show();
    }

    private void initRetrofit() {
        apiService = ApiClient.getApiService();
    }

    private void fetchListings() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        Log.d(TAG, "fetchListings: Starting fetch");
        apiService.getListings().enqueue(new Callback<ApiResponse<List<Listing>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Listing>>> call, Response<ApiResponse<List<Listing>>> response) {
                progressBar.setVisibility(View.GONE);
                Log.d(TAG, "onResponse: response code = " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Listing>> apiResponse = response.body();
                    Log.d(TAG, "onResponse: isSuccess = " + apiResponse.isSuccess());
                    
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        List<Listing> allListings = apiResponse.getData();
                        Log.d(TAG, "onResponse: Total listings = " + allListings.size());
                        
                        // Log listing statuses
                        for (int i = 0; i < Math.min(3, allListings.size()); i++) {
                            Log.d(TAG, "onResponse: Listing " + i + " status = " + allListings.get(i).getStatus());
                        }
                        
                        // Filter listings with status "Active"
                        // TODO: Uncomment when backend returns proper status
                        List<Listing> activeListings = allListings; // Comment this out to see all listings regardless of status
                        /*List<Listing> activeListings = allListings.stream()
                                .filter(listing -> "Active".equalsIgnoreCase(listing.getStatus()))
                                .collect(Collectors.toList());*/

                        Log.d(TAG, "onResponse: Active listings = " + activeListings.size());
                        
                        allActiveListings.clear();
                        allActiveListings.addAll(activeListings);
                        applyHomeFilters();

                        if (activeListings.isEmpty()) {
                            Log.w(TAG, "onResponse: No active listings found. All statuses: ");
                            for (Listing listing : allListings) {
                                Log.w(TAG, "  - Status: " + listing.getStatus());
                            }
                            Toast.makeText(MainActivity.this, "Không có bài đăng Active. Tất cả: " + allListings.size(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        allActiveListings.clear();
                        applyHomeFilters();
                        Toast.makeText(MainActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    allActiveListings.clear();
                    applyHomeFilters();
                    Log.e(TAG, "Error: " + response.code());
                    Toast.makeText(MainActivity.this, "Error " + response.code() + ": " + response.message(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Listing>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                allActiveListings.clear();
                applyHomeFilters();
                Log.e(TAG, "Failure: " + t.getMessage(), t);
                Toast.makeText(MainActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateListingUi(List<Listing> listings) {
        adapter.setListings(listings);
        adapter.setFavoriteListingIds(cachedFavoriteListingIds);
        tvListingCount.setText(getString(R.string.listing_count_format, listings.size()));
        tvMiniListingCount.setText(getString(R.string.listing_count_format, listings.size()));

        if (!hasActiveHomeFilters()) {
            tvSectionSubtitle.setText(getString(R.string.featured_section_subtitle));
        } else {
            tvSectionSubtitle.setText(getString(R.string.featured_section_subtitle));
        }

        boolean isEmpty = listings.isEmpty();
        tvEmptyState.setText(hasActiveHomeFilters() ? R.string.home_empty_filtered : R.string.empty_listings);
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvVehicles.setVisibility(isEmpty ? View.INVISIBLE : View.VISIBLE);

    }

    private void syncFavoritesFromServer() {
        String userId = authSessionManager.getUserId();
        if (TextUtils.isEmpty(userId)) {
            if (!cachedFavoriteListingIds.isEmpty()) {
                for (String listingId : new HashSet<>(cachedFavoriteListingIds)) {
                    adapter.updateFavoriteState(listingId, false);
                }
                cachedFavoriteListingIds.clear();
            }
            return;
        }

        apiService.getFavoritesByUser(userId).enqueue(new Callback<ApiResponse<List<FavoriteItem>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<FavoriteItem>>> call,
                                   Response<ApiResponse<List<FavoriteItem>>> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    return;
                }

                List<FavoriteItem> favoriteItems = response.body().getData() == null
                        ? new ArrayList<>()
                        : response.body().getData();
                Set<String> latestFavoriteIds = favoriteItems.stream()
                        .map(FavoriteItem::getListingId)
                        .filter(id -> !TextUtils.isEmpty(id))
                        .collect(Collectors.toSet());

                Set<String> staleIds = new HashSet<>(cachedFavoriteListingIds);
                staleIds.removeAll(latestFavoriteIds);
                for (String listingId : staleIds) {
                    adapter.updateFavoriteState(listingId, false);
                }

                Set<String> addedIds = new HashSet<>(latestFavoriteIds);
                addedIds.removeAll(cachedFavoriteListingIds);
                for (String listingId : addedIds) {
                    adapter.updateFavoriteState(listingId, true);
                }

                cachedFavoriteListingIds.clear();
                cachedFavoriteListingIds.addAll(latestFavoriteIds);
                adapter.setFavoriteListingIds(cachedFavoriteListingIds);
            }

            @Override
            public void onFailure(Call<ApiResponse<List<FavoriteItem>>> call, Throwable t) {
                Log.w(TAG, "Favorite sync failed: " + t.getMessage());
            }
        });
    }

    @Override
    public void onLoginSuccess(String displayName) {
        refreshAuthHeaderUi();
    }

    @Override
    public void onOpenRegisterRequested() {
        showRegisterDialog();
    }

    @Override
    public void onRegisterSuccess(String displayName) {
        refreshAuthHeaderUi();
    }

    @Override
    public void onOpenLoginRequested() {
        showLoginDialog();
    }

    private void setupGestureDetection() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                try {
                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();
                    
                    // Swipe sensitivity check
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                // Swipe Right - go to previous
                                onSwipeRight();
                            } else {
                                // Swipe Left - go to next
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
        // Swipe Right: Home(0) → back is no-op
        // Do nothing on home screen
    }

    private void onSwipeLeft() {
        // Swipe Left: Home(0) → Favorites(1)
        if (currentNavItem == 0) {
            selectNavItem(1);
            openFavoritesScreen();
        }
    }
}
