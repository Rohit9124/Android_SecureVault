package com.example.app.view.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.app.ContextWrapper.NewPassContextWrapper;
import com.example.app.SharedPreferences.SharedPreferencesHelper;
import com.example.app.database.DatabaseServiceLocator;
import com.example.app.databinding.ActivityMainViewBinding;

import com.example.app.R;
import com.example.app.utilities.AppLockManager;
import com.example.app.utilities.ShakeDetector;
import com.example.app.utilities.SystemBarColorHelper;
import com.example.app.view.fragments.LanguageDialogFragment;
import com.example.app.view.fragments.MainViewFragment;

import java.util.Locale;
import java.util.Objects;

public class MainViewActivity extends AppCompatActivity implements LanguageDialogFragment.LanguageListener {

    private ShakeDetector shakeDetector;
    private BroadcastReceiver screenOffReceiver;
    private static boolean isOpeningSystemSettings = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainViewBinding binding = ActivityMainViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Block screenshots and screen recording for security
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        DatabaseServiceLocator.init(getApplicationContext());

        SystemBarColorHelper.changeBarsColor(this, R.color.background_primary);

        // Initial fragment setup, showing the 'AddFragment' by default
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MainViewFragment())
                    .commit();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferencesHelper.toggleDarkLightModeUI(this);

        // Register shake detector if Emergency Lock is enabled
        if (SharedPreferencesHelper.isEmergencyLockEnabled(this)) {
            if (shakeDetector == null) {
                shakeDetector = new ShakeDetector(this);
                shakeDetector.setOnShakeListener(() -> {
                    // Lock app when shake is detected
                    AppLockManager.lockApp(MainViewActivity.this);
                });
            }
            shakeDetector.start();
        }

        // Register screen-off receiver if Emergency Lock is enabled
        if (SharedPreferencesHelper.isEmergencyLockEnabled(this)) {
            registerScreenOffReceiver();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop shake detector
        if (shakeDetector != null) {
            shakeDetector.stop();
        }

        // Unregister screen-off receiver
        unregisterScreenOffReceiver();

        // Lock app when going to background if Emergency Lock is enabled
        // BUT skip locking if we're opening system settings
        if (SharedPreferencesHelper.isEmergencyLockEnabled(this) && !isOpeningSystemSettings) {
            AppLockManager.handleBackgroundLock(this);
        }

        // Reset the flag
        isOpeningSystemSettings = false;
    }

    private void registerScreenOffReceiver() {
        if (screenOffReceiver == null) {
            screenOffReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                        // Lock app when screen turns off if Emergency Lock is enabled
                        if (SharedPreferencesHelper.isEmergencyLockEnabled(context)) {
                            AppLockManager.lockApp(MainViewActivity.this);
                        }
                    }
                }
            };
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            registerReceiver(screenOffReceiver, filter);
        }
    }

    private void unregisterScreenOffReceiver() {
        if (screenOffReceiver != null) {
            try {
                unregisterReceiver(screenOffReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver was not registered, ignore
            }
            screenOffReceiver = null;
        }
    }

    /**
     * Set flag to prevent Emergency Lock when opening system settings.
     * This is called before launching system settings to avoid unwanted app lock.
     */
    public static void setOpeningSystemSettings(boolean opening) {
        isOpeningSystemSettings = opening;
    }

    public void openFragment(Fragment fragment) {

        // Perform the fragment transaction and add it to the back stack
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.enter_right_to_left, R.anim.exit_right_to_left,
                        R.anim.enter_left_to_right, R.anim.exit_left_to_right)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {
        // If the fragment stack has more than one entry, pop the back stack
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            // Otherwise, defer to the system default behavior
            super.onBackPressed();
        }
    }

    @Override
    protected void attachBaseContext(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SharedPreferencesHelper.SHARED_PREF_FLAG,
                MODE_PRIVATE);
        String language = sharedPreferences.getString(SharedPreferencesHelper.LANG_PREF_FLAG, "en");

        super.attachBaseContext(NewPassContextWrapper.wrap(context, language));

        Locale locale = new Locale(language);
        Resources resources = getBaseContext().getResources();
        Configuration conf = resources.getConfiguration();

        conf.setLocale(locale);
        resources.updateConfiguration(conf, resources.getDisplayMetrics());
    }

    @Override
    public void onPositiveButtonClicked(String[] list, int position) {
        String selectedLanguage = list[position];
        SharedPreferencesHelper.setLanguage(this, selectedLanguage);
    }

    @Override
    public void onNegativeButtonClicked() {
    }
}