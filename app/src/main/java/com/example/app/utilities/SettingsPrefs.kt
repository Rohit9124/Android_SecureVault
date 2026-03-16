package com.example.app.utilities

import android.content.Context
import android.content.SharedPreferences

object SettingsPrefs {
    private const val PREFS_NAME = "SecureVaultPrefs"
    private const val KEY_AUTO_CLEAR_ENABLED = "auto_clear_enabled"
    private const val KEY_AUTO_CLEAR_DELAY = "auto_clear_delay"

    // Default delay: 30 seconds (30000 ms)
    const val DELAY_30_SECONDS = 30000L
    const val DELAY_1_MINUTE = 60000L
    const val DELAY_5_MINUTES = 300000L

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAutoClearEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_CLEAR_ENABLED, false)
    }

    fun setAutoClearEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_CLEAR_ENABLED, enabled).apply()
    }

    fun getAutoClearDelay(context: Context): Long {
        return getPrefs(context).getLong(KEY_AUTO_CLEAR_DELAY, DELAY_30_SECONDS)
    }

    fun setAutoClearDelay(context: Context, delayMs: Long) {
        getPrefs(context).edit().putLong(KEY_AUTO_CLEAR_DELAY, delayMs).apply()
    }
}
