package com.pocketledger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pocketledger.R;
import com.pocketledger.customviews.SegmentedControl;
import com.pocketledger.network.SupabaseClient;
import com.pocketledger.network.SupabaseConfig;
import com.pocketledger.repositories.AuthRepository;
import com.pocketledger.utils.AppExecutors;
import com.pocketledger.utils.UiUtils;
import com.pocketledger.utils.Validators;

import java.io.IOException;

public class AuthActivity extends AppCompatActivity {

    private SegmentedControl modeControl;
    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmLayout;
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmInput;
    private TextView authTitle;
    private TextView authSubtitle;
    private TextView forgotButton;
    private TextView authError;
    private TextView offlineButton;
    private MaterialButton authButton;
    private ProgressBar progress;
    private AuthRepository authRepository;
    private boolean registerMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        authRepository = new AuthRepository(this);
        bindViews();
        modeControl.setItems(new String[]{"Log in", "Create account"}, new String[]{"login", "register"});
        modeControl.setOnSelectionChanged((index, value) -> {
            registerMode = index == 1;
            updateMode();
        });
        authButton.setOnClickListener(v -> submit());
        forgotButton.setOnClickListener(v -> resetPassword());
        offlineButton.setOnClickListener(v -> continueOffline());
        updateMode();
    }

    private void bindViews() {
        modeControl = findViewById(R.id.authMode);
        nameLayout = findViewById(R.id.nameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmLayout = findViewById(R.id.confirmLayout);
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmInput = findViewById(R.id.confirmInput);
        authTitle = findViewById(R.id.authTitle);
        authSubtitle = findViewById(R.id.authSubtitle);
        forgotButton = findViewById(R.id.forgotButton);
        authError = findViewById(R.id.authError);
        offlineButton = findViewById(R.id.offlineButton);
        authButton = findViewById(R.id.authButton);
        progress = findViewById(R.id.authProgress);
    }

    private void updateMode() {
        authTitle.setText(registerMode ? R.string.register : R.string.welcome_back);
        authSubtitle.setText(registerMode ? R.string.offline_mode_note : R.string.login_subtitle);
        nameLayout.setVisibility(registerMode ? View.VISIBLE : View.GONE);
        confirmLayout.setVisibility(registerMode ? View.VISIBLE : View.GONE);
        forgotButton.setVisibility(registerMode ? View.GONE : View.VISIBLE);
        authButton.setText(registerMode ? R.string.register : R.string.login);
        clearErrors();
    }

    private void submit() {
        clearErrors();
        String email = text(emailInput);
        String password = raw(passwordInput);
        String name = text(nameInput);
        String confirm = raw(confirmInput);
        boolean valid = true;
        if (!Validators.isEmail(email)) {
            emailLayout.setError(getString(R.string.error_email));
            valid = false;
        }
        if (!Validators.isPassword(password)) {
            passwordLayout.setError(getString(R.string.error_password));
            valid = false;
        }
        if (registerMode) {
            if (!Validators.notBlank(name)) {
                nameLayout.setError(getString(R.string.error_name));
                valid = false;
            }
            if (!password.equals(confirm)) {
                confirmLayout.setError(getString(R.string.error_password_match));
                valid = false;
            }
        }
        if (!valid) {
            return;
        }
        if (!SupabaseConfig.isConfigured()) {
            showError(getString(R.string.cloud_not_configured_desc));
            return;
        }
        UiUtils.hideKeyboard(this);
        setLoading(true, registerMode ? R.string.creating_account : R.string.logging_in);
        AppExecutors.io().execute(() -> {
            try {
                if (registerMode) {
                    SupabaseClient.AuthResult result = authRepository.signUp(email, password, name);
                    AppExecutors.main(() -> {
                        setLoading(false, 0);
                        if (result.session != null) {
                            openMain();
                        } else {
                            showError(getString(R.string.check_email));
                            modeControl.setSelectedValue("login");
                            registerMode = false;
                            updateMode();
                        }
                    });
                } else {
                    authRepository.signIn(email, password);
                    AppExecutors.main(() -> {
                        setLoading(false, 0);
                        openMain();
                    });
                }
            } catch (SupabaseClient.ApiException e) {
                AppExecutors.main(() -> {
                    setLoading(false, 0);
                    showError(e.getMessage() == null ? getString(R.string.error_generic) : e.getMessage());
                });
            } catch (IOException e) {
                AppExecutors.main(() -> {
                    setLoading(false, 0);
                    showError(getString(R.string.saved_offline));
                });
            }
        });
    }

    private void resetPassword() {
        String email = text(emailInput);
        if (!Validators.isEmail(email)) {
            emailLayout.setError(getString(R.string.error_email));
            return;
        }
        if (!SupabaseConfig.isConfigured()) {
            showError(getString(R.string.password_reset_unavailable));
            return;
        }
        setLoading(true, R.string.reset_password);
        AppExecutors.io().execute(() -> {
            try {
                authRepository.requestPasswordReset(email);
                AppExecutors.main(() -> {
                    setLoading(false, 0);
                    showError(getString(R.string.reset_link_sent));
                });
            } catch (Exception e) {
                AppExecutors.main(() -> {
                    setLoading(false, 0);
                    showError(e.getMessage() == null ? getString(R.string.error_generic) : e.getMessage());
                });
            }
        });
    }

    private void continueOffline() {
        authRepository.continueOffline(null);
        openMain();
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void setLoading(boolean loading, int label) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        authButton.setEnabled(!loading);
        if (loading && label != 0) {
            authButton.setText(label);
        } else {
            authButton.setText(registerMode ? R.string.register : R.string.login);
        }
    }

    private void clearErrors() {
        nameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        confirmLayout.setError(null);
        authError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        authError.setText(message);
        authError.setVisibility(View.VISIBLE);
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private String raw(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
