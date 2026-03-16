package com.example.app.utilities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.example.app.SharedPreferences.SharedPreferencesHelper;
import com.example.app.view.activities.LoginActivity;

/**
 * Manages app locking behavior based on user preferences.
 * Handles automatic locking when screen turns off or app goes to background.
 */
public class AppLockManager {

    private static boolean skipNextLock = false;

    /**
     * Checks if App Lock feature is enabled in settings.
     *
     * @param context The application context
     * @return True if App Lock is enabled, false otherwise
     */
    public static boolean isAppLockEnabled(Context context) {
        return SharedPreferencesHelper.isAppLockEnabled(context);
    }

    /**
     * Sets a flag to skip the next lock operation.
     * Used when launching external intents (file picker, share sheet, etc.)
     * to prevent Emergency Lock from triggering.
     *
     * @param skip True to skip the next lock, false otherwise
     */
    public static void setSkipNextLock(boolean skip) {
        skipNextLock = skip;
    }

    /**
     * Locks the app by navigating to the LoginActivity.
     * Clears the activity stack to prevent back navigation.
     *
     * @param activity The current activity
     */
    public static void lockApp(Activity activity) {
        if (activity == null) {
            return;
        }

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    /**
     * Checks if app should lock when going to background and locks if enabled.
     * Respects the skipNextLock flag for external intent operations.
     *
     * @param activity The current activity
     */
    public static void handleBackgroundLock(Activity activity) {
        if (skipNextLock) {
            skipNextLock = false; // Reset the flag
            return;
        }
        if (isAppLockEnabled(activity)) {
            lockApp(activity);
        }
    }
}
