package com.example.app.repository;

import android.content.Context;

import androidx.annotation.StringRes;

import com.example.app.SharedPreferences.SharedPreferencesHelper;
import com.example.app.utilities.LocaleHelper;

public class ResourceRepository {
    private final Context context;

    public ResourceRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public String getString(@StringRes int resId) {
        String currentLanguage = getCurrentLanguage();

        return LocaleHelper.getStringByLocale(context, resId, currentLanguage);
    }

    public String getCurrentLanguage() {
        return SharedPreferencesHelper.getCurrentLanguage(context);
    }
}
