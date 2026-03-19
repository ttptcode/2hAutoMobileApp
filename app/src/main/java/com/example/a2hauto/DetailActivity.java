package com.example.a2hauto;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
import com.example.a2hauto.model.ToggleFavoriteRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonElement;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailActivity extends AppCompatActivity implements
        LoginDialogFragment.LoginDialogListener,
        RegisterDialogFragment.RegisterDialogListener {

    private static final int SINGLE_COLUMN_BREAKPOINT_DP = 360;

    private ApiService apiService;
    private VehicleAdapter relatedAdapter;
    private Listing currentListing;
    private AuthSessionManager authSessionManager;
    private ChatRepository chatRepository;

    private View btnHeaderUpgrade;
    private View btnHeaderLogin;
    private ImageView btnHeaderFavorite;
    private TextView tvHeaderAvatar;

    private TextView tvDetailSpecsLeft;
    private TextView tvDetailSpecsRight;
    private TextView tvRelatedEmpty;
    private ProgressBar progressRelated;
    private MaterialButton btnDetailChat;
    private MaterialButton btnDetailContact;
    private boolean isFavorite = false;
    private boolean isFavoriteRequestInFlight = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        currentListing = (Listing) getIntent().getSerializableExtra("listing");
        if (currentListing == null) {
            finish();
            return;
        }

        authSessionManager = new AuthSessionManager(this);
        chatRepository = new ChatRepository(ApiClient.getApiService(), authSessionManager);
        bindHeaderViews();
        setupHeaderActions();
        refreshAuthHeaderUi();
        apiService = ApiClient.getApiService();
        updateHeaderFavoriteUi();
        fetchInitialFavoriteState();
        setupRelatedSection();

        ImageView ivDetail = findViewById(R.id.ivDetail);
        TextView tvDetailName = findViewById(R.id.tvDetailName);
        TextView tvDetailPrice = findViewById(R.id.tvDetailPrice);
        tvDetailSpecsLeft = findViewById(R.id.tvDetailSpecsLeft);
        tvDetailSpecsRight = findViewById(R.id.tvDetailSpecsRight);
        TextView tvDetailDesc = findViewById(R.id.tvDetailDesc);
        TextView tvDetailSeller = findViewById(R.id.tvDetailSeller);

        tvDetailName.setText(currentListing.getDisplayTitle());
        
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvDetailPrice.setText(formatter.format(currentListing.getBuyNowPrice()));

        Item item = currentListing.getItem();
        bindSpecs(item);

        String detail = currentListing.getDetail();
        tvDetailDesc.setText(!TextUtils.isEmpty(detail)
                ? detail
                : getString(R.string.detail_description_fallback));

        String seller = !TextUtils.isEmpty(currentListing.getUserName())
                ? currentListing.getUserName()
                : getString(R.string.seller_fallback);
        String address = !TextUtils.isEmpty(currentListing.getAddress())
                ? currentListing.getAddress()
                : getString(R.string.detail_address_fallback);
        tvDetailSeller.setText(getString(R.string.detail_seller_and_address, seller, address));

        String imageUrl = null;
        if (item != null) {
            if (item.getImageUrls() != null && !item.getImageUrls().isEmpty()) {
                imageUrl = item.getImageUrls().get(0);
            }
        }

        Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(ivDetail);

        fetchRelatedListings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAuthHeaderUi();
        updateDetailActionButtonsVisibility();
    }

    private void bindHeaderViews() {
        btnHeaderUpgrade = findViewById(R.id.btnHeaderUpgrade);
        btnHeaderLogin = findViewById(R.id.btnHeaderLogin);
        btnHeaderFavorite = findViewById(R.id.btnHeaderFavorite);
        tvHeaderAvatar = findViewById(R.id.tvHeaderAvatar);
    }

    private void setupHeaderActions() {
        findViewById(R.id.btnDetailBackSmall).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.btnMenu).setOnClickListener(v -> showCategoryMenuDialog());
        findViewById(R.id.btnHeaderFavorite).setOnClickListener(v -> onFavoriteClicked());
        btnDetailChat = findViewById(R.id.btnDetailChat);
        btnDetailContact = findViewById(R.id.btnDetailContact);
        btnDetailChat.setOnClickListener(v -> openChatFromDetail());
        btnHeaderUpgrade.setOnClickListener(v -> showUpgradeDialog());
        btnHeaderLogin.setOnClickListener(v -> handleAccountAction());
        tvHeaderAvatar.setOnClickListener(v -> handleAccountAction());
        updateDetailActionButtonsVisibility();
    }

    private void openChatFromDetail() {
        if (!chatRepository.isLoggedIn()) {
            showLoginDialog();
            return;
        }

        String listingId = currentListing == null ? "" : currentListing.getListingId();
        String buyerId = authSessionManager.getUserId();
        String sellerId = currentListing == null ? "" : currentListing.getUserId();
        if (TextUtils.isEmpty(listingId) || TextUtils.isEmpty(buyerId)) {
            Toast.makeText(this, R.string.chat_open_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TextUtils.isEmpty(sellerId) && TextUtils.equals(sellerId, buyerId)) {
            return;
        }

        chatRepository.createConversation(listingId, buyerId, new ChatRepository.RepositoryCallback<com.example.a2hauto.model.Conversation>() {
            @Override
            public void onSuccess(com.example.a2hauto.model.Conversation data) {
                Intent intent = new Intent(DetailActivity.this, ChatActivity.class);
                if (data != null && !TextUtils.isEmpty(data.getConversationId())) {
                    intent.putExtra(ChatActivity.EXTRA_OPEN_CONVERSATION_ID, data.getConversationId());
                }
                startActivity(intent);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(DetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDetailActionButtonsVisibility() {
        if (btnDetailChat == null || btnDetailContact == null || currentListing == null) {
            return;
        }

        String currentUserId = authSessionManager == null ? "" : authSessionManager.getUserId();
        String sellerId = currentListing.getUserId();
        boolean isOwnerViewingOwnPost = !TextUtils.isEmpty(currentUserId)
                && !TextUtils.isEmpty(sellerId)
                && TextUtils.equals(currentUserId, sellerId);

        btnDetailChat.setVisibility(isOwnerViewingOwnPost ? View.GONE : View.VISIBLE);

        ViewGroup.LayoutParams baseParams = btnDetailContact.getLayoutParams();
        if (!(baseParams instanceof LinearLayout.LayoutParams)) {
            return;
        }

        LinearLayout.LayoutParams contactParams = (LinearLayout.LayoutParams) baseParams;
        int defaultSpacing = (int) (8 * getResources().getDisplayMetrics().density);
        contactParams.setMarginStart(isOwnerViewingOwnPost ? 0 : defaultSpacing);
        btnDetailContact.setLayoutParams(contactParams);
    }

    private void fetchInitialFavoriteState() {
        String userId = authSessionManager.getUserId();
        String listingId = currentListing.getListingId();
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(listingId)) {
            isFavorite = false;
            updateHeaderFavoriteUi();
            return;
        }

        apiService.getFavoritesByUser(userId).enqueue(new Callback<ApiResponse<List<FavoriteItem>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<FavoriteItem>>> call, Response<ApiResponse<List<FavoriteItem>>> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    isFavorite = false;
                    updateHeaderFavoriteUi();
                    return;
                }

                List<FavoriteItem> favorites = response.body().getData();
                boolean matched = false;
                if (favorites != null) {
                    for (FavoriteItem item : favorites) {
                        if (item != null && listingId.equals(item.getListingId())) {
                            matched = true;
                            break;
                        }
                    }
                }

                isFavorite = matched;
                updateHeaderFavoriteUi();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<FavoriteItem>>> call, Throwable t) {
                isFavorite = false;
                updateHeaderFavoriteUi();
            }
        });
    }

    private void onFavoriteClicked() {
        if (!authSessionManager.isLoggedIn()) {
            Toast.makeText(this, R.string.favorite_login_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = authSessionManager.getUserId();
        String listingId = currentListing.getListingId();
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(listingId)) {
            Toast.makeText(this, R.string.favorite_action_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        if (isFavoriteRequestInFlight) {
            return;
        }

        isFavoriteRequestInFlight = true;
        apiService.toggleFavorite(new ToggleFavoriteRequest(userId, listingId))
                .enqueue(new Callback<ApiResponse<JsonElement>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<JsonElement>> call, Response<ApiResponse<JsonElement>> response) {
                        isFavoriteRequestInFlight = false;
                        if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                            Toast.makeText(DetailActivity.this, R.string.favorite_action_failed, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        isFavorite = !isFavorite;
                        updateHeaderFavoriteUi();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<JsonElement>> call, Throwable t) {
                        isFavoriteRequestInFlight = false;
                        Toast.makeText(DetailActivity.this, R.string.favorite_action_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateHeaderFavoriteUi() {
        if (btnHeaderFavorite == null) {
            return;
        }
        btnHeaderFavorite.setImageResource(isFavorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
    }

    private void setupRelatedSection() {
        RecyclerView rvRelatedListings = findViewById(R.id.rvRelatedListings);
        tvRelatedEmpty = findViewById(R.id.tvRelatedEmpty);
        progressRelated = findViewById(R.id.progressRelated);

        rvRelatedListings.setLayoutManager(new LinearLayoutManager(this));
        rvRelatedListings.setNestedScrollingEnabled(false);
        relatedAdapter = new VehicleAdapter(new ArrayList<>());
        rvRelatedListings.setAdapter(relatedAdapter);
    }

    private void bindSpecs(Item item) {
        List<String> specRows = new ArrayList<>();
        appendSpec(specRows, getString(R.string.detail_spec_item_type), item != null ? item.getItemTypeName() : null);
        appendSpec(specRows, getString(R.string.detail_spec_brand), item != null ? item.getBrand() : null);
        appendSpec(specRows, getString(R.string.detail_spec_model), item != null ? item.getModel() : null);
        appendSpec(specRows, getString(R.string.detail_spec_year), item != null && item.getYear() != null ? String.valueOf(item.getYear()) : null);
        appendSpec(specRows, getString(R.string.detail_spec_condition), item != null ? item.getCondition() : null);
        appendSpec(specRows, getString(R.string.detail_spec_mileage), item != null ? item.getMileage() : null);
        appendSpec(specRows, getString(R.string.detail_spec_fuel), item != null ? item.getFuel() : null);
        appendSpec(specRows, getString(R.string.detail_spec_gearbox), item != null ? item.getGearbox() : null);
        appendSpec(specRows, getString(R.string.detail_spec_color), item != null ? item.getColor() : null);
        appendSpec(specRows, getString(R.string.detail_spec_seat), item != null ? item.getSeat() : null);
        appendSpec(specRows, getString(R.string.detail_spec_origin), item != null ? item.getOrigin() : null);
        appendSpec(specRows, getString(R.string.detail_spec_plate), item != null ? item.getLicensePlate() : null);

        if (specRows.isEmpty()) {
            tvDetailSpecsLeft.setText(getString(R.string.detail_specs_fallback));
            tvDetailSpecsRight.setText("");
            tvDetailSpecsRight.setVisibility(View.GONE);
            return;
        }

        if (shouldUseSingleColumnSpecs()) {
            tvDetailSpecsLeft.setText(TextUtils.join("\n", specRows));
            tvDetailSpecsRight.setText("");
            tvDetailSpecsRight.setVisibility(View.GONE);
            return;
        }

        int midPoint = (specRows.size() + 1) / 2;
        tvDetailSpecsLeft.setText(TextUtils.join("\n", specRows.subList(0, midPoint)));
        tvDetailSpecsRight.setText(TextUtils.join("\n", specRows.subList(midPoint, specRows.size())));
        tvDetailSpecsRight.setVisibility(View.VISIBLE);
    }

    private boolean shouldUseSingleColumnSpecs() {
        return getResources().getConfiguration().screenWidthDp < SINGLE_COLUMN_BREAKPOINT_DP;
    }

    private void appendSpec(List<String> specs, String label, String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }

        specs.add("- " + label + ": " + value);
    }

    private void handleAccountAction() {
        if (authSessionManager.isLoggedIn()) {
            showAccountDialog();
            return;
        }
        showLoginDialog();
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

    private void showAccountDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.account_dialog_title)
                .setMessage(getString(
                        R.string.account_dialog_message,
                        authSessionManager.getDisplayName(),
                        authSessionManager.getPhoneNumber()
                ))
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.action_upgrade, (dialog, which) -> showUpgradeDialog())
                .setPositiveButton(R.string.action_logout, (dialog, which) -> {
                    authSessionManager.logout();
                    refreshAuthHeaderUi();
                    Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();
                })
                .show();
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

    private void showCategoryMenuDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_category_menu, null, false);
        LinearLayout optionContainer = dialogView.findViewById(R.id.menuOptionContainer);

        List<CategoryMenuItem> menuItems = new ArrayList<>();
        menuItems.add(new CategoryMenuItem(getString(R.string.menu_option_car), R.drawable.ic_menu_category_car));
        menuItems.add(new CategoryMenuItem(getString(R.string.menu_option_motorbike), R.drawable.ic_menu_category_motorbike));
        menuItems.add(new CategoryMenuItem(getString(R.string.menu_option_truck), R.drawable.ic_menu_category_truck));
        menuItems.add(new CategoryMenuItem(getString(R.string.menu_option_electric), R.drawable.ic_menu_category_electric));
        menuItems.add(new CategoryMenuItem(getString(R.string.menu_option_bicycle), R.drawable.ic_menu_category_bicycle));
        menuItems.add(new CategoryMenuItem(getString(R.string.menu_option_other_vehicle), android.R.drawable.ic_menu_mapmode));
        menuItems.add(new CategoryMenuItem(getString(R.string.menu_option_spare_parts), android.R.drawable.ic_menu_manage));

        final androidx.appcompat.app.AlertDialog[] menuDialogRef = new androidx.appcompat.app.AlertDialog[1];
        for (CategoryMenuItem item : menuItems) {
            View optionView = inflater.inflate(R.layout.item_menu_option, optionContainer, false);
            ImageView ivIcon = optionView.findViewById(R.id.ivMenuOptionIcon);
            TextView tvTitle = optionView.findViewById(R.id.tvMenuOptionTitle);

            ivIcon.setImageResource(item.iconResId);
            tvTitle.setText(item.title);
            optionView.setOnClickListener(v -> {
                showComingSoon(item.title);
                if (menuDialogRef[0] != null) {
                    menuDialogRef[0].dismiss();
                }
            });
            optionContainer.addView(optionView);
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();
        menuDialogRef[0] = dialog;
        dialog.show();
    }

    private void refreshAuthHeaderUi() {
        boolean isLoggedIn = authSessionManager.isLoggedIn();
        String initials = getAvatarInitials(authSessionManager.getDisplayName(), authSessionManager.getPhoneNumber());

        btnHeaderLogin.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        tvHeaderAvatar.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        tvHeaderAvatar.setText(initials);
    }

    private String getAvatarInitials(String displayName, String phoneNumber) {
        if (!TextUtils.isEmpty(displayName)) {
            String[] parts = displayName.trim().split("\\s+");
            if (parts.length >= 2) {
                return "" + Character.toUpperCase(parts[0].charAt(0))
                        + Character.toUpperCase(parts[parts.length - 1].charAt(0));
            }
            if (parts.length == 1 && !parts[0].isEmpty()) {
                return "" + Character.toUpperCase(parts[0].charAt(0));
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

    private void fetchRelatedListings() {
        progressRelated.setVisibility(ProgressBar.VISIBLE);
        tvRelatedEmpty.setVisibility(TextView.GONE);

        apiService.getListings().enqueue(new Callback<ApiResponse<List<Listing>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Listing>>> call, Response<ApiResponse<List<Listing>>> response) {
                progressRelated.setVisibility(ProgressBar.GONE);

                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess() || response.body().getData() == null) {
                    showRelatedEmpty();
                    return;
                }

                List<Listing> filtered = buildRelatedListings(response.body().getData());
                relatedAdapter.setListings(filtered);
                tvRelatedEmpty.setVisibility(filtered.isEmpty() ? TextView.VISIBLE : TextView.GONE);
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Listing>>> call, Throwable t) {
                progressRelated.setVisibility(ProgressBar.GONE);
                showRelatedEmpty();
            }
        });
    }

    private void showRelatedEmpty() {
        relatedAdapter.setListings(Collections.emptyList());
        tvRelatedEmpty.setVisibility(TextView.VISIBLE);
    }

    private List<Listing> buildRelatedListings(List<Listing> allListings) {
        List<Listing> candidates = new ArrayList<>();
        for (Listing listing : allListings) {
            if (listing == null) {
                continue;
            }

            if (!"Active".equalsIgnoreCase(listing.getStatus())) {
                continue;
            }

            if (!TextUtils.isEmpty(currentListing.getListingId())
                    && currentListing.getListingId().equals(listing.getListingId())) {
                continue;
            }
            candidates.add(listing);
        }

        candidates.sort(Comparator.comparingInt(this::relatedScore).reversed());
        if (candidates.size() > 6) {
            return new ArrayList<>(candidates.subList(0, 6));
        }
        return candidates;
    }

    private int relatedScore(Listing candidate) {
        int score = 0;
        Item currentItem = currentListing.getItem();
        Item candidateItem = candidate.getItem();

        if (currentItem != null && candidateItem != null) {
            if (!TextUtils.isEmpty(currentItem.getItemTypeName())
                    && currentItem.getItemTypeName().equalsIgnoreCase(candidateItem.getItemTypeName())) {
                score += 2;
            }
            if (!TextUtils.isEmpty(currentItem.getBrand())
                    && currentItem.getBrand().equalsIgnoreCase(candidateItem.getBrand())) {
                score += 1;
            }
        }
        return score;
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

    private static class CategoryMenuItem {
        private final String title;
        private final int iconResId;

        private CategoryMenuItem(String title, int iconResId) {
            this.title = title;
            this.iconResId = iconResId;
        }
    }
}
