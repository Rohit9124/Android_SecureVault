package com.example.app.autofill;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.example.app.R;

/**
 * Helper utilities for Autofill functionality.
 */
public class AutofillHelper {

    private static final String TAG = "AutofillHelper";

    /**
     * Checks if autofill is available on this device.
     * Autofill requires API level 26 (Android 8.0) or higher.
     */
    public static boolean isAutofillAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    /**
     * Shows a dialog explaining autofill and prompts user to enable it in system
     * settings.
     * 
     * @param context The context to show the dialog
     */
    public static void showEnableAutofillDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Enable Autofill")
                .setMessage("SecureVault can autofill your passwords in apps and websites.\n\n" +
                        "To enable this feature:\n" +
                        "1. Tap 'Open Settings' below\n" +
                        "2. Select 'SecureVault' as your autofill service\n\n" +
                        "You can disable this anytime from SecureVault Settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    openAutofillSettings(context);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Opens Android's autofill settings page.
     * 
     * @param context The context to start the activity
     */
    public static void openAutofillSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE);
                intent.setData(android.net.Uri.parse("package:com.example.securevault"));
                context.startActivity(intent);
            } catch (Exception e) {
                // Fallback to general autofill settings
                try {
                    Intent intent = new Intent(Settings.ACTION_SETTINGS);
                    context.startActivity(intent);
                } catch (Exception ex) {
                    // Unable to open settings
                }
            }
        }
    }

    /**
     * Extracts domain from a URL and normalizes it.
     * 
     * @param url The URL to extract domain from
     * @return The normalized domain, or the original URL if extraction fails
     */
    public static String extractDomain(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        String domain = url;
        try {
            java.net.URI uri = new java.net.URI(url);
            String host = uri.getHost();
            if (host != null && !host.isEmpty()) {
                domain = host;
            }
        } catch (Exception e) {
            // Not a valid URI, might be just a domain
        }

        return normalizeDomain(domain);
    }

    /**
     * Normalizes a domain by removing protocol and www prefix.
     */
    public static String normalizeDomain(String domain) {
        if (domain == null)
            return null;

        String original = domain;
        String normalized = domain.toLowerCase().trim();

        // Remove protocol
        if (normalized.startsWith("http://")) {
            normalized = normalized.substring(7);
        } else if (normalized.startsWith("https://")) {
            normalized = normalized.substring(8);
        }

        // Remove port number (e.g., :8080, :443)
        int portIndex = normalized.indexOf(':');
        if (portIndex > 0 && !normalized.substring(portIndex).contains("/")) {
            // Only remove if there's no path after the port
            normalized = normalized.substring(0, portIndex);
        }

        // Remove path and query parameters (everything after first /)
        int slashIndex = normalized.indexOf('/');
        if (slashIndex > 0) {
            normalized = normalized.substring(0, slashIndex);
        }

        // Remove www variants (www, www2, www3, etc.)
        if (normalized.startsWith("www")) {
            int dotIndex = normalized.indexOf('.');
            if (dotIndex > 0 && dotIndex <= 5) { // www.domain or www2.domain
                normalized = normalized.substring(dotIndex + 1);
            }
        }

        // Remove mobile subdomains
        if (normalized.startsWith("m.")) {
            normalized = normalized.substring(2);
        } else if (normalized.startsWith("mobile.")) {
            normalized = normalized.substring(7);
        }

        // Remove trailing slash if any
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (!original.equals(normalized)) {
            Log.d(TAG, "Domain normalized: '" + original + "' -> '" + normalized + "'");
        }

        return normalized;
    }

    /**
     * Extracts the base domain from a full domain/subdomain.
     * e.g., login.example.com -> example.com
     */
    public static String getBaseDomain(String domain) {
        if (domain == null || domain.isEmpty())
            return domain;

        String normalized = normalizeDomain(domain);
        String[] parts = normalized.split("\\.");

        if (parts.length > 2) {
            // Check for common two-part TLDs (co.uk, com.au, etc.)
            String lastTwo = parts[parts.length - 2] + "." + parts[parts.length - 1];

            // More comprehensive two-part TLD check
            boolean isTwoPartTLD = lastTwo.matches(".+\\.(co|com|org|net|ac|gov|edu)\\.[a-z]{2,3}");

            if (isTwoPartTLD) {
                if (parts.length > 3) {
                    String baseDomain = parts[parts.length - 3] + "." + lastTwo;
                    Log.d(TAG, "Base domain extracted (with two-part TLD): " + baseDomain);
                    return baseDomain;
                }
                Log.d(TAG, "Base domain (two-part TLD): " + normalized);
                return normalized;
            }

            // Standard TLD - return last two parts (domain.tld)
            String baseDomain = parts[parts.length - 2] + "." + parts[parts.length - 1];
            Log.d(TAG, "Base domain extracted: " + baseDomain + " from " + normalized);
            return baseDomain;
        }

        return normalized;
    }

    /**
     * Checks if a string is a valid domain or package name.
     */
    public static boolean isValidDomain(String domain) {
        return domain != null && !domain.isEmpty() && domain.length() > 2;
    }
}
