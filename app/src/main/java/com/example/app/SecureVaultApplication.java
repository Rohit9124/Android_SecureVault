package com.example.app;

import android.app.Application;
import com.example.app.database.DatabaseServiceLocator;

/**
 * Custom Application class for SecureVault.
 * Ensures central services are initialized at app startup.
 */
public class SecureVaultApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize DatabaseServiceLocator once at app startup
        DatabaseServiceLocator.init(this);
    }
}
