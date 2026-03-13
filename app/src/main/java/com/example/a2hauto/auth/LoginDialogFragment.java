package com.example.a2hauto.auth;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.a2hauto.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginDialogFragment extends DialogFragment {

    public interface LoginDialogListener {
        void onLoginSuccess(String displayName);

        void onOpenRegisterRequested();
    }

    public static final String TAG = "LoginDialogFragment";

    private AuthSessionManager authSessionManager;
    private AuthRepository authRepository;
    private TextInputLayout tilPhone;
    private TextInputLayout tilPassword;
    private TextInputEditText etPhone;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private View contentView;
    private ImageButton btnClose;
    private TextView tvForgotPassword;
    private TextView tvOpenRegister;
    private MaterialCardView cardGoogleLogin;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        authSessionManager = new AuthSessionManager(requireContext());
        authRepository = new AuthRepository();

        FrameLayout container = new FrameLayout(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_login, container, false);
        contentView = view;
        bindViews(view);
        setupActions();
        prefillPendingRegistrationData();
        updateLoginButtonState();

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.94f);
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void bindViews(View view) {
        btnClose = view.findViewById(R.id.btnCloseLoginDialog);
        tilPhone = view.findViewById(R.id.tilLoginPhone);
        tilPassword = view.findViewById(R.id.tilLoginPassword);
        etPhone = view.findViewById(R.id.etLoginPhone);
        etPassword = view.findViewById(R.id.etLoginPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        tvForgotPassword = view.findViewById(R.id.tvForgotPassword);
        tvOpenRegister = view.findViewById(R.id.tvOpenRegister);
        cardGoogleLogin = view.findViewById(R.id.cardGoogleLogin);
    }

    private void setupActions() {
        btnClose.setOnClickListener(v -> dismiss());
        tvForgotPassword.setOnClickListener(v -> Toast.makeText(requireContext(), R.string.forgot_password_placeholder, Toast.LENGTH_SHORT).show());
        cardGoogleLogin.setOnClickListener(v -> Toast.makeText(requireContext(), R.string.google_login_placeholder, Toast.LENGTH_SHORT).show());
        tvOpenRegister.setOnClickListener(v -> {
            dismiss();
            if (getActivity() instanceof LoginDialogListener) {
                ((LoginDialogListener) getActivity()).onOpenRegisterRequested();
            }
        });

        btnLogin.setOnClickListener(v -> attemptLogin());

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearErrors();
                updateLoginButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        etPhone.addTextChangedListener(textWatcher);
        etPassword.addTextChangedListener(textWatcher);
    }

    private void attemptLogin() {
        String phone = getText(etPhone);
        String password = getText(etPassword);
        boolean isValid = true;
        View firstInvalidField = null;

        clearErrors();

        if (!AuthValidator.isValidPhone(phone)) {
            tilPhone.setError(getString(R.string.validation_phone_required));
            isValid = false;
            firstInvalidField = etPhone;
        }

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.validation_password_required));
            isValid = false;
            if (firstInvalidField == null) {
                firstInvalidField = etPassword;
            }
        }

        if (!isValid) {
            if (firstInvalidField != null) {
                firstInvalidField.requestFocus();
            }
            return;
        }

        setLoading(true);
        authRepository.login(phone, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String token, String message) {
                if (!isAdded()) {
                    return;
                }

                String displayName = resolveDisplayNameAfterLogin(phone, token);
                authSessionManager.saveSession(displayName, phone, token);
                displayName = authSessionManager.getDisplayName();
                Toast.makeText(requireContext(), getString(R.string.login_success, displayName), Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof LoginDialogListener) {
                    ((LoginDialogListener) getActivity()).onLoginSuccess(displayName);
                }
                dismissAllowingStateLoss();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }

                setLoading(false);
                tilPassword.setError(message);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearErrors() {
        tilPhone.setError(null);
        tilPassword.setError(null);
    }

    private void updateLoginButtonState() {
        btnLogin.setEnabled(true);
    }

    private void prefillPendingRegistrationData() {
        String pendingPhone = authSessionManager.getPendingPhoneNumber();
        if (!TextUtils.isEmpty(pendingPhone) && TextUtils.isEmpty(getText(etPhone))) {
            etPhone.setText(pendingPhone);
            etPhone.setSelection(pendingPhone.length());
        }
    }

    private String resolveDisplayNameAfterLogin(String phone, String token) {
        String displayNameFromToken = JwtUtils.extractDisplayName(token);
        if (!TextUtils.isEmpty(displayNameFromToken)) {
            return displayNameFromToken;
        }

        String normalizedPhone = AuthValidator.normalizePhone(phone);
        String pendingPhone = authSessionManager.getPendingPhoneNumber();
        String pendingFullName = authSessionManager.getPendingFullName();
        if (!TextUtils.isEmpty(pendingFullName) && normalizedPhone.equals(pendingPhone)) {
            return pendingFullName;
        }

        return "";
    }

    private void setLoading(boolean isLoading) {
        if (contentView != null) {
            contentView.setEnabled(!isLoading);
        }
        btnClose.setEnabled(!isLoading);
        etPhone.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        tvForgotPassword.setEnabled(!isLoading);
        tvOpenRegister.setEnabled(!isLoading);
        cardGoogleLogin.setEnabled(!isLoading);
        btnLogin.setEnabled(!isLoading);
        btnLogin.setText(isLoading ? R.string.action_logging_in : R.string.action_login);
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setCancelable(!isLoading);
            dialog.setCanceledOnTouchOutside(!isLoading);
        }
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}






