package com.example.app.database;

import android.annotation.SuppressLint;
import android.content.Context;

/**
 * Class responsible for managing the DatabaseHelper instance.
 * Provides a centralized, context-safe access point.
 */
public class DatabaseServiceLocator {

    @SuppressLint("StaticFieldLeak")
    private static DatabaseHelper databaseHelper;

    /**
     * Initialize the DatabaseHelper.
     * Should be called from Application or first Activity.
     */
    public static void init(Context context) {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper(context.getApplicationContext());
        }
    }

    /**
     * Get DatabaseHelper instance (no context).
     * Returns null if init() was not called.
     */
    public static DatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }

    /**
     * Get DatabaseHelper instance with context fallback.
     * SAFE for services like AutofillService.
     */
    public static DatabaseHelper getInstance(Context context) {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper(context.getApplicationContext());
        }
        return databaseHelper;
    }

    /**
     * For testing or manual injection.
     */
    public static void setDatabaseHelper(DatabaseHelper dbHelper) {
        databaseHelper = dbHelper;
    }
}
