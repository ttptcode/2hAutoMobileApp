package com.example.a2hauto.auth;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterDialogFragment extends DialogFragment {

    private static final long REGISTER_TIMEOUT_MS = 20_000L;

    public interface RegisterDialogListener {
        void onRegisterSuccess(String displayName);

        void onOpenLoginRequested();
    }

    public static final String TAG = "RegisterDialogFragment";

    private AuthSessionManager authSessionManager;
    private AuthRepository authRepository;
    private TextInputLayout tilName;
    private TextInputLayout tilPhone;
    private TextInputLayout tilPassword;
    private TextInputEditText etName;
    private TextInputEditText etPhone;
    private TextInputEditText etPassword;
    private MaterialButton btnRegister;
    private View contentView;
    private ImageButton btnClose;
    private TextView tvOpenLogin;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable registerTimeoutRunnable;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        authSessionManager = new AuthSessionManager(requireContext());
        authRepository = new AuthRepository();

        FrameLayout container = new FrameLayout(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_register, container, false);
        contentView = view;
        bindViews(view);
        setupActions();
        updateRegisterButtonState();

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
        btnClose = view.findViewById(R.id.btnCloseRegisterDialog);
        tilName = view.findViewById(R.id.tilRegisterName);
        tilPhone = view.findViewById(R.id.tilRegisterPhone);
        tilPassword = view.findViewById(R.id.tilRegisterPassword);
        etName = view.findViewById(R.id.etRegisterName);
        etPhone = view.findViewById(R.id.etRegisterPhone);
        etPassword = view.findViewById(R.id.etRegisterPassword);
        btnRegister = view.findViewById(R.id.btnRegister);
        tvOpenLogin = view.findViewById(R.id.tvOpenLogin);
    }

    private void setupActions() {
        btnClose.setOnClickListener(v -> dismiss());
        tvOpenLogin.setOnClickListener(v -> {
            dismiss();
            if (getActivity() instanceof RegisterDialogListener) {
                ((RegisterDialogListener) getActivity()).onOpenLoginRequested();
            }
        });
        btnRegister.setOnClickListener(v -> attemptRegister());

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearErrors();
                updateRegisterButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        etName.addTextChangedListener(textWatcher);
        etPhone.addTextChangedListener(textWatcher);
        etPassword.addTextChangedListener(textWatcher);
    }

    private void attemptRegister() {
        String fullName = getText(etName);
        String phone = getText(etPhone);
        String password = getText(etPassword);
        boolean isValid = true;
        View firstInvalidField = null;

        clearErrors();

        if (!AuthValidator.isValidFullName(fullName)) {
            tilName.setError(getString(R.string.validation_name_required));
            isValid = false;
            firstInvalidField = etName;
        }

        if (!AuthValidator.isValidPhone(phone)) {
            tilPhone.setError(getString(R.string.validation_phone_required));
            isValid = false;
            if (firstInvalidField == null) {
                firstInvalidField = etPhone;
            }
        }

        if (!AuthValidator.isValidPassword(password)) {
            tilPassword.setError(getString(R.string.validation_password_register));
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
        startRegisterTimeoutWatchdog();
        authRepository.register(fullName, phone, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String token, String message) {
                if (!isAdded()) {
                    stopRegisterTimeoutWatchdog();
                    return;
                }

                stopRegisterTimeoutWatchdog();
                try {
                    String displayName = fullName.trim();
                    authSessionManager.savePendingRegistration(displayName, phone);
                    Toast.makeText(requireContext(), getString(R.string.register_success, displayName), Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof RegisterDialogListener) {
                        ((RegisterDialogListener) getActivity()).onRegisterSuccess(displayName);
                    }
                    setLoading(false);
                    dismissAllowingStateLoss();

                    if (getActivity() instanceof RegisterDialogListener) {
                        etPhone.post(() -> {
                            if (getActivity() instanceof RegisterDialogListener) {
                                ((RegisterDialogListener) getActivity()).onOpenLoginRequested();
                            }
                        });
                    }
                } catch (Exception exception) {
                    setLoading(false);
                    String safeMessage = TextUtils.isEmpty(exception.getMessage())
                            ? getString(R.string.auth_unknown_error)
                            : exception.getMessage();
                    tilPhone.setError(safeMessage);
                    Toast.makeText(requireContext(), safeMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    stopRegisterTimeoutWatchdog();
                    return;
                }

                stopRegisterTimeoutWatchdog();
                setLoading(false);
                tilPhone.setError(message);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearErrors() {
        tilName.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
    }

    private void updateRegisterButtonState() {
        btnRegister.setEnabled(true);
    }

    private void setLoading(boolean isLoading) {
        if (contentView != null) {
            contentView.setEnabled(!isLoading);
        }
        btnClose.setEnabled(!isLoading);
        etName.setEnabled(!isLoading);
        etPhone.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        tvOpenLogin.setEnabled(!isLoading);
        btnRegister.setEnabled(!isLoading);
        btnRegister.setText(isLoading ? R.string.action_registering : R.string.action_register);
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setCancelable(!isLoading);
            dialog.setCanceledOnTouchOutside(!isLoading);
        }
    }

    @Override
    public void onDestroyView() {
        stopRegisterTimeoutWatchdog();
        super.onDestroyView();
    }

    private void startRegisterTimeoutWatchdog() {
        stopRegisterTimeoutWatchdog();
        registerTimeoutRunnable = () -> {
            if (!isAdded()) {
                return;
            }
            setLoading(false);
            String timeoutMessage = getString(R.string.auth_network_error);
            tilPhone.setError(timeoutMessage);
            Toast.makeText(requireContext(), timeoutMessage, Toast.LENGTH_SHORT).show();
        };
        timeoutHandler.postDelayed(registerTimeoutRunnable, REGISTER_TIMEOUT_MS);
    }

    private void stopRegisterTimeoutWatchdog() {
        if (registerTimeoutRunnable != null) {
            timeoutHandler.removeCallbacks(registerTimeoutRunnable);
            registerTimeoutRunnable = null;
        }
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}





