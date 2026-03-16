package com.example.app.autofill;

import android.app.assist.AssistStructure;
import android.os.Build;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.autofill.AutofillId;

import androidx.annotation.RequiresApi;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to parse AssistStructure and detect autofill fields.
 * Identifies username, password fields and extracts domain/package information.
 */
public class AutofillFieldsParser {

    private static final String TAG = "AutofillFieldsParser";

    private String username;
    private String password;
    private String domain; // Website domain or app package name
    private String webDomain; // Domain extracted from WebNodes (browsers)
    private String activityPackage; // Package name of the requesting activity
    private AutofillId usernameId;
    private AutofillId passwordId;
    private boolean isWebView = false; // Track if we're parsing WebView content

    /**
     * Parses the AssistStructure to find username and password fields.
     *
     * @param structure The AssistStructure from the autofill request
     * @return true if at least a username OR password field was found
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean parse(AssistStructure structure) {
        if (structure == null) {
            Log.d(TAG, "parse: structure is null");
            return false;
        }

        Log.d(TAG, "=== Starting AssistStructure Parsing ===");

        // Always extract the activity package name — critical for native apps
        // (Instagram, LinkedIn, etc.)
        if (structure.getActivityComponent() != null) {
            activityPackage = structure.getActivityComponent().getPackageName();
            Log.d(TAG, "[AUTOFILL] Activity package: " + activityPackage);
            if (activityPackage != null && activityPackage.contains("chrome")) {
                Log.d(TAG, "[AUTOFILL] Detected Chrome browser");
            }
        } else {
            Log.d(TAG, "[AUTOFILL] Activity component is null — cannot determine package");
        }

        int nodes = structure.getWindowNodeCount();
        Log.d(TAG, "Window node count: " + nodes);

        for (int i = 0; i < nodes; i++) {
            AssistStructure.ViewNode rootNode = structure.getWindowNodeAt(i).getRootViewNode();
            parseNode(rootNode);
        }

        // Extract domain from the structure title if not already found
        if (domain == null && nodes > 0) {
            String title = (String) structure.getWindowNodeAt(0).getTitle();
            Log.d(TAG, "Window title: " + title);
            if (title != null && !title.isEmpty()) {
                domain = extractDomain(title);
                Log.d(TAG, "Extracted domain from title: " + domain);
            }
        }

        // Log final parsing results
        Log.d(TAG, "=== Parsing Complete ===");
        Log.d(TAG, "[AUTOFILL] Username field found: " + (usernameId != null));
        Log.d(TAG, "[AUTOFILL] Password field found: " + (passwordId != null));
        Log.d(TAG, "[AUTOFILL] Activity package: " + activityPackage);
        Log.d(TAG, "[AUTOFILL] WebDomain: " + webDomain);
        Log.d(TAG, "[AUTOFILL] Domain: " + domain);
        Log.d(TAG, "[AUTOFILL] Final domain (getDomain): " + getDomain());
        Log.d(TAG, "[AUTOFILL] Is WebView: " + isWebView);

        // Return true if we found at least a username OR password field
        // (native apps like Instagram may hide password on the second login screen)
        return usernameId != null || passwordId != null;
    }

    /**
     * Recursively parses view nodes to find username and password fields.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void parseNode(AssistStructure.ViewNode node) {
        if (node == null) {
            return;
        }

        // Get autofill hints
        String[] autofillHints = node.getAutofillHints();
        String hint = node.getHint();
        String idEntry = node.getIdEntry();
        int inputType = node.getInputType();
        String className = node.getClassName();

        // Check for WebView
        if (className != null && className.contains("WebView")) {
            isWebView = true;
            Log.d(TAG, "WebView detected: " + className);
        }

        // Extract HTML attributes for Chrome/browser support
        String htmlAutocomplete = null;
        String htmlName = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Object htmlInfoObj = node.getHtmlInfo();
            if (htmlInfoObj != null) {
                try {
                    htmlAutocomplete = (String) htmlInfoObj.getClass().getMethod("getAttribute", String.class)
                            .invoke(htmlInfoObj, "autocomplete");
                    htmlName = (String) htmlInfoObj.getClass().getMethod("getAttribute", String.class)
                            .invoke(htmlInfoObj, "name");
                    String htmlType = (String) htmlInfoObj.getClass().getMethod("getAttribute", String.class)
                            .invoke(htmlInfoObj, "type");

                    if (htmlAutocomplete != null || htmlName != null || htmlType != null) {
                        Log.d(TAG, "HTML attributes found - autocomplete: " + htmlAutocomplete +
                                ", name: " + htmlName + ", type: " + htmlType);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Could not extract HTML attributes: " + e.getMessage());
                }
            }
        }

        // Log autofill hints if present
        if (autofillHints != null && autofillHints.length > 0) {
            StringBuilder hintsLog = new StringBuilder("Autofill hints: ");
            for (String h : autofillHints) {
                hintsLog.append(h).append(", ");
            }
            Log.d(TAG, hintsLog.toString());
        }

        // Check if this is a password field
        if (isPasswordField(autofillHints, inputType, idEntry, hint, htmlAutocomplete, htmlName)) {
            if (passwordId == null) {
                passwordId = node.getAutofillId();
                CharSequence text = node.getText();
                if (text != null) {
                    password = text.toString();
                }
                Log.d(TAG, "✓ Password field detected - ID: " + idEntry + ", hint: " + hint);
            }
        }
        // Check if this is a username/email field
        else if (isUsernameField(autofillHints, inputType, idEntry, hint, htmlAutocomplete, htmlName)) {
            if (usernameId == null) {
                usernameId = node.getAutofillId();
                CharSequence text = node.getText();
                if (text != null) {
                    username = text.toString();
                }
                Log.d(TAG, "✓ Username field detected - ID: " + idEntry + ", hint: " + hint);
            }
        }

        // Extract web domain — priority: WebDomain > Package name from node
        if (webDomain == null && node.getWebDomain() != null) {
            webDomain = node.getWebDomain();
            Log.d(TAG, "[AUTOFILL] WebDomain extracted from node: " + webDomain);
        }

        if (domain == null) {
            String packageName = node.getIdPackage();
            if (packageName != null && !packageName.isEmpty()) {
                domain = packageName;
                Log.d(TAG, "Package name extracted from node: " + domain);
            }
        }

        // Recursively parse child nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            parseNode(node.getChildAt(i));
        }
    }

    /**
     * Checks if a field is a password field based on various indicators.
     */
    private boolean isPasswordField(String[] autofillHints, int inputType, String idEntry,
            String hint, String htmlAutocomplete, String htmlName) {
        // Check autofill hints
        if (autofillHints != null) {
            for (String autofillHint : autofillHints) {
                if (View.AUTOFILL_HINT_PASSWORD.equals(autofillHint)) {
                    return true;
                }
            }
        }

        // Check HTML autocomplete attribute (critical for Chrome)
        if (htmlAutocomplete != null) {
            String lowerAutocomplete = htmlAutocomplete.toLowerCase();
            if (lowerAutocomplete.contains("current-password") ||
                    lowerAutocomplete.contains("new-password") ||
                    lowerAutocomplete.equals("password")) {
                Log.d(TAG, "Password detected via HTML autocomplete: " + htmlAutocomplete);
                return true;
            }
        }

        // Check HTML name attribute
        if (htmlName != null) {
            String lowerName = htmlName.toLowerCase();
            if (lowerName.contains("password") || lowerName.contains("passwd") ||
                    lowerName.contains("pwd")) {
                Log.d(TAG, "Password detected via HTML name: " + htmlName);
                return true;
            }
        }

        // Check input type
        int variation = inputType & InputType.TYPE_MASK_VARIATION;
        if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
            return true;
        }

        // Check ID and hint for common password indicators
        if (idEntry != null) {
            String lowerIdEntry = idEntry.toLowerCase();
            if (lowerIdEntry.contains("password") || lowerIdEntry.contains("passwd") ||
                    lowerIdEntry.contains("pwd")) {
                return true;
            }
        }

        if (hint != null) {
            String lowerHint = hint.toLowerCase();
            if (lowerHint.contains("password") || lowerHint.contains("passwd") ||
                    lowerHint.contains("pwd")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a field is a username/email field based on various indicators.
     */
    private boolean isUsernameField(String[] autofillHints, int inputType, String idEntry,
            String hint, String htmlAutocomplete, String htmlName) {
        // Check autofill hints
        if (autofillHints != null) {
            for (String autofillHint : autofillHints) {
                if (View.AUTOFILL_HINT_USERNAME.equals(autofillHint) ||
                        View.AUTOFILL_HINT_EMAIL_ADDRESS.equals(autofillHint)) {
                    return true;
                }
            }
        }

        // Check HTML autocomplete attribute (critical for Chrome)
        if (htmlAutocomplete != null) {
            String lowerAutocomplete = htmlAutocomplete.toLowerCase();
            if (lowerAutocomplete.equals("username") ||
                    lowerAutocomplete.equals("email") ||
                    lowerAutocomplete.contains("email")) {
                Log.d(TAG, "Username/Email detected via HTML autocomplete: " + htmlAutocomplete);
                return true;
            }
        }

        // Check HTML name attribute
        if (htmlName != null) {
            String lowerName = htmlName.toLowerCase();
            if (lowerName.contains("username") || lowerName.contains("user") ||
                    lowerName.contains("email") || lowerName.contains("login")) {
                Log.d(TAG, "Username/Email detected via HTML name: " + htmlName);
                return true;
            }
        }

        // Check input type
        int variation = inputType & InputType.TYPE_MASK_VARIATION;
        if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) {
            return true;
        }

        // Check ID and hint for common username/email indicators
        if (idEntry != null) {
            String lowerIdEntry = idEntry.toLowerCase();
            if (lowerIdEntry.contains("username") || lowerIdEntry.contains("user") ||
                    lowerIdEntry.contains("email") || lowerIdEntry.contains("login")) {
                return true;
            }
        }

        if (hint != null) {
            String lowerHint = hint.toLowerCase();
            if (lowerHint.contains("username") || lowerHint.contains("user") ||
                    lowerHint.contains("email") || lowerHint.contains("login")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extracts domain from a URL or returns the input if it's not a URL.
     */
    private String extractDomain(String urlOrDomain) {
        if (urlOrDomain == null || urlOrDomain.isEmpty()) {
            return null;
        }

        try {
            URI uri = new URI(urlOrDomain);
            String host = uri.getHost();
            if (host != null && !host.isEmpty()) {
                return host;
            }
        } catch (Exception e) {
            // Not a valid URI, might be a package name or simple string
        }

        // Return as-is if not a URL
        return urlOrDomain;
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Returns the best available domain for credential lookup.
     * Prefers webDomain (browsers), then falls back to domain (node package or
     * title).
     */
    public String getDomain() {
        if (webDomain != null) {
            return webDomain;
        }
        return domain;
    }

    /**
     * Returns the raw web domain extracted from view nodes (browsers only).
     */
    public String getWebDomain() {
        return webDomain;
    }

    /**
     * Returns the activity package name of the requesting app (e.g.
     * com.instagram.android).
     * Never null-safe normalised — always the raw package name.
     */
    public String getActivityPackage() {
        return activityPackage;
    }

    public AutofillId getUsernameId() {
        return usernameId;
    }

    public AutofillId getPasswordId() {
        return passwordId;
    }

    public boolean hasUsername() {
        return usernameId != null;
    }

    public boolean hasPassword() {
        return passwordId != null;
    }
}
