package com.example.app.view.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.security.crypto.EncryptedSharedPreferences;

import com.example.app.ContextWrapper.NewPassContextWrapper;
import com.example.app.R;
import com.example.app.SharedPreferences.SharedPreferencesHelper;
import com.example.app.databinding.ActivityLoginBinding;
import com.example.app.encryption.EncryptionHelper;
import com.example.app.factory.ViewMoldelsFactory;
import com.example.app.repository.ResourceRepository;
import com.example.app.utilities.AnimationsUtility;
import com.example.app.utilities.StringHelper;
import com.example.app.utilities.SystemBarColorHelper;
import com.example.app.utilities.VibrationHelper;
import com.example.app.viewmodel.LoginViewModel;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private EditText passwordEntry;
    private ImageButton buttonRegisterOrUnlock, buttonPasswordVisibility;
    private ImageView passwordBox, bgImage;
    private TextView welcomeTextView, textViewRegisterOrUnlock;
    private EncryptedSharedPreferences encryptedSharedPreferences;
    private LoginViewModel loginViewModel;
    private Boolean isPasswordVisible = false;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLoginBinding binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Block screenshots and screen recording for security
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        SystemBarColorHelper.changeBarsColor(this, R.color.background_primary);

        initViews(binding);

        textViewRegisterOrUnlock.setText(getString(R.string.create_password_button_text));
        welcomeTextView.setText(getString(R.string.welcome_newpass_text));

        loginViewModel = new ViewModelProvider(
                this,
                new ViewMoldelsFactory(new ResourceRepository(getApplicationContext()))).get(LoginViewModel.class);

        loginViewModel.getLoginMessageLiveData().observe(this, message -> {
            // intentionally left empty
        });

        loginViewModel.getLoginSuccessLiveData().observe(this, success -> {
            String hashedPassword = encryptedSharedPreferences.getString("password", "");

            if (success) {
                Intent intent = new Intent(LoginActivity.this, MainViewActivity.class);
                StringHelper.setSharedString(hashedPassword);
                startActivity(intent);
                finish();
            } else {
                AnimationsUtility.errorAnimation(buttonRegisterOrUnlock, textViewRegisterOrUnlock);
            }
        });

        encryptedSharedPreferences = EncryptionHelper.getEncryptedSharedPreferences(getApplicationContext());

        // Toggle dark/light mode
        SharedPreferencesHelper.toggleDarkLightModeUI(this);

        String hashedPassword = encryptedSharedPreferences.getString("password", "");
        Boolean isPasswordEmpty = hashedPassword.isEmpty();

        if (!isPasswordEmpty) {
            textViewRegisterOrUnlock.setText(getString(R.string.unlock_newpass_button_text));
            welcomeTextView.setText(getString(R.string.welcome_back_newpass_text));
        }

        buttonPasswordVisibility.setOnClickListener(v -> {
            if (isPasswordVisible) {
                buttonPasswordVisibility.setImageDrawable(
                        ContextCompat.getDrawable(this, R.drawable.icon_visibility_on));
                passwordEntry.setTransformationMethod(
                        PasswordTransformationMethod.getInstance());
            } else {
                buttonPasswordVisibility.setImageDrawable(
                        ContextCompat.getDrawable(this, R.drawable.icon_visibility_off));
                passwordEntry.setTransformationMethod(
                        HideReturnsTransformationMethod.getInstance());
            }
            isPasswordVisible = !isPasswordVisible;
        });

        buttonRegisterOrUnlockListener(buttonRegisterOrUnlock, isPasswordEmpty);
    }

    public void buttonRegisterOrUnlockListener(View view, Boolean isPasswordEmpty) {
        if (!isPasswordEmpty) {
            loginUser(view);
        } else {
            registerUser();
        }
    }

    private void loginUser(View view) {
        Log.d("LOGIN_VM", "Already launched before");

        if (SharedPreferencesHelper.isScreenLockEnabled(this)) {
            BiometricManager biometricManager = BiometricManager.from(this);

            int canAuthenticate = biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
                            | BiometricManager.Authenticators.DEVICE_CREDENTIAL);

            if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                hideUI(true);
                loginViewModel.loginUserWithBiometricAuth(this);

                loginViewModel.getLoginSuccessLiveData().observe(this, state -> {
                    if (!state) {
                        hideUI(false);
                        loginWithPassword(view);
                    }
                });
            } else {
                loginWithPassword(view);
            }
        } else {
            loginWithPassword(view);
        }
    }

    private void registerUser() {
        Log.d("LOGIN_VM", "First launch");

        buttonRegisterOrUnlock.setOnClickListener(v -> {
            String passwordInput = passwordEntry.getText().toString();
            try {
                loginViewModel.createUser(passwordInput, encryptedSharedPreferences);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new RuntimeException(e);
            }
            VibrationHelper.vibrate(v, VibrationHelper.VibrationType.Strong);
        });
    }

    private void hideUI(boolean hide) {
        int visibility = hide ? View.GONE : View.VISIBLE;
        buttonRegisterOrUnlock.setVisibility(visibility);
        passwordEntry.setVisibility(visibility);
        passwordBox.setVisibility(visibility);
        welcomeTextView.setVisibility(visibility);
        bgImage.setVisibility(visibility);
        buttonPasswordVisibility.setVisibility(visibility);
    }

    private void loginWithPassword(View view) {
        view.setOnTouchListener((v, event) -> {
            String passwordInput = passwordEntry.getText().toString();

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                VibrationHelper.vibrate(v, VibrationHelper.VibrationType.Weak);
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
                try {
                    loginViewModel.loginUserWithPassword(
                            passwordInput, encryptedSharedPreferences);
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    throw new RuntimeException(e);
                }
                VibrationHelper.vibrate(v, VibrationHelper.VibrationType.Strong);
                return true;
            }
            return false;
        });
    }

    private void initViews(ActivityLoginBinding binding) {
        passwordEntry = binding.loginTwPassword;
        welcomeTextView = binding.welcomeLoginTw;
        buttonRegisterOrUnlock = binding.registerOrUnlockButton;
        textViewRegisterOrUnlock = binding.registerOrUnlockTextView;
        passwordBox = binding.backgroundInputbox2;
        bgImage = binding.logoLogin;
        buttonPasswordVisibility = binding.passwordVisibilityButton;
    }

    @Override
    protected void attachBaseContext(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                SharedPreferencesHelper.SHARED_PREF_FLAG, MODE_PRIVATE);
        String language = sharedPreferences.getString(
                SharedPreferencesHelper.LANG_PREF_FLAG, "en");

        super.attachBaseContext(NewPassContextWrapper.wrap(context, language));

        Locale locale = new Locale(language);
        Resources resources = getBaseContext().getResources();
        Configuration conf = resources.getConfiguration();
        conf.setLocale(locale);
        resources.updateConfiguration(conf, resources.getDisplayMetrics());
    }
}
