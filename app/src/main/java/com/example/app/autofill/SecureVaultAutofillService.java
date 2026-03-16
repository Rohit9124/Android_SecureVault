package com.example.app.autofill;

import android.app.assist.AssistStructure;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.FillCallback;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveInfo;
import android.service.autofill.SaveRequest;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.app.R;
import com.example.app.SharedPreferences.SharedPreferencesHelper;
import com.example.app.database.DatabaseHelper;
import com.example.app.database.DatabaseServiceLocator;
import com.example.app.encryption.EncryptionHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SecureVault's AutofillService implementation.
 * Provides password autofill and save functionality across apps and websites.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class SecureVaultAutofillService extends AutofillService {

    private static final String TAG = "AutofillService";

    /**
     * Browser package names — these use webDomain; native apps use package name.
     */
    private static final Set<String> BROWSER_PACKAGES = new HashSet<>();
    static {
        BROWSER_PACKAGES.add("com.android.chrome");
        BROWSER_PACKAGES.add("com.chrome.beta");
        BROWSER_PACKAGES.add("com.chrome.dev");
        BROWSER_PACKAGES.add("com.chrome.canary");
        BROWSER_PACKAGES.add("org.mozilla.firefox");
        BROWSER_PACKAGES.add("org.mozilla.firefox_beta");
        BROWSER_PACKAGES.add("com.microsoft.emmx"); // Edge
        BROWSER_PACKAGES.add("com.opera.browser");
        BROWSER_PACKAGES.add("com.opera.mini.native");
        BROWSER_PACKAGES.add("com.brave.browser");
        BROWSER_PACKAGES.add("com.duckduckgo.mobile.android");
        BROWSER_PACKAGES.add("com.UCMobile.intl");
        BROWSER_PACKAGES.add("com.sec.android.app.sbrowser"); // Samsung Internet
    }

    @Override
    public void onFillRequest(@NonNull FillRequest request,
            @NonNull CancellationSignal cancellationSignal,
            @NonNull FillCallback callback) {

        Log.d(TAG, "==========================================");
        Log.d(TAG, "onFillRequest triggered");
        Log.d(TAG, "==========================================");

        // Check if autofill is enabled in app settings
        if (!SharedPreferencesHelper.isAutofillEnabled(getApplicationContext())) {
            Log.d(TAG, "Autofill is disabled in app settings");
            callback.onSuccess(null);
            return;
        }

        // Get the structure from the request
        List<FillContext> fillContexts = request.getFillContexts();
        if (fillContexts == null || fillContexts.isEmpty()) {
            Log.d(TAG, "No fill contexts available");
            callback.onSuccess(null);
            return;
        }

        AssistStructure structure = fillContexts.get(fillContexts.size() - 1).getStructure();
        Log.d(TAG, "AssistStructure obtained, parsing fields...");

        // Parse the structure to find username and password fields
        AutofillFieldsParser parser = new AutofillFieldsParser();
        parser.parse(structure);

        // --- Determine final domain for credential lookup ---
        String activityPackage = parser.getActivityPackage();
        String webDomain = parser.getWebDomain();
        boolean isBrowser = activityPackage != null && BROWSER_PACKAGES.contains(activityPackage);

        String finalDomain;
        if (isBrowser && webDomain != null) {
            // Browser: use normalised web domain (unchanged behaviour)
            finalDomain = AutofillHelper.normalizeDomain(webDomain);
            Log.d(TAG, "[AUTOFILL] Browser path — using webDomain: " + finalDomain);
        } else if (activityPackage != null && !activityPackage.isEmpty()) {
            // Native app: use raw package name, do NOT normalise (no mangling)
            finalDomain = activityPackage;
            Log.d(TAG, "[AUTOFILL] Native app path — using activityPackage: " + finalDomain);
        } else {
            // Fallback: legacy getDomain() + normalisation
            finalDomain = AutofillHelper.normalizeDomain(parser.getDomain());
            Log.d(TAG, "[AUTOFILL] Fallback path — using getDomain(): " + finalDomain);
        }

        Log.d(TAG, "--- Domain Detection ---");
        Log.d(TAG, "[AUTOFILL] Activity package: " + activityPackage);
        Log.d(TAG, "[AUTOFILL] Web domain: " + webDomain);
        Log.d(TAG, "[AUTOFILL] Is browser: " + isBrowser);
        Log.d(TAG, "[AUTOFILL] Final domain for lookup: " + finalDomain);

        // Build fill response
        FillResponse.Builder responseBuilder = new FillResponse.Builder();

        // --- SaveInfo: attach if username OR password field is present ---
        if (parser.hasPassword() || parser.hasUsername()) {
            List<AutofillId> saveIdList = new ArrayList<>();
            if (parser.hasUsername())
                saveIdList.add(parser.getUsernameId());
            if (parser.hasPassword())
                saveIdList.add(parser.getPasswordId());

            AutofillId[] saveIds = saveIdList.toArray(new AutofillId[0]);

            SaveInfo saveInfo = new SaveInfo.Builder(
                    SaveInfo.SAVE_DATA_TYPE_PASSWORD, saveIds)
                    .build();
            responseBuilder.setSaveInfo(saveInfo);
            Log.d(TAG, "SaveInfo attached with " + saveIds.length + " field(s)");
        } else {
            Log.d(TAG, "No username or password field detected — SaveInfo not created");
        }

        // --- Credential lookup ---
        if (AutofillHelper.isValidDomain(finalDomain)) {
            Log.d(TAG, "Domain is valid, querying credentials...");
            List<CredentialData> credentials = getCredentialsForDomain(finalDomain, activityPackage);

            Log.d(TAG, "--- Credential Matching ---");
            Log.d(TAG, "[AUTOFILL] Matched credentials count: " + credentials.size());

            int datasetCount = 0;
            for (CredentialData credential : credentials) {
                android.service.autofill.Dataset.Builder datasetBuilder = new android.service.autofill.Dataset.Builder();

                // Presentation label
                RemoteViews presentation = new RemoteViews(getPackageName(),
                        android.R.layout.simple_list_item_1);
                String presentationText = credential.name + " (" + credential.email + ")";
                presentation.setTextViewText(android.R.id.text1, presentationText);

                boolean datasetHasValue = false;

                // Fill username field if present
                if (parser.hasUsername() && credential.email != null) {
                    datasetBuilder.setValue(parser.getUsernameId(),
                            AutofillValue.forText(credential.email),
                            presentation);
                    datasetHasValue = true;
                }

                // Fill password field if present
                // (native apps may not expose it on the second login screen — that's OK)
                if (parser.hasPassword() && credential.password != null) {
                    datasetBuilder.setValue(parser.getPasswordId(),
                            AutofillValue.forText(credential.password),
                            presentation);
                    datasetHasValue = true;
                }

                // Only add dataset if at least one field value was set
                if (datasetHasValue) {
                    responseBuilder.addDataset(datasetBuilder.build());
                    datasetCount++;
                    Log.d(TAG, "Dataset " + datasetCount + " created: " + presentationText);
                } else {
                    Log.d(TAG, "Skipping dataset — no field IDs available to fill");
                }
            }

            Log.d(TAG, "Total datasets added: " + datasetCount);
        } else {
            Log.d(TAG, "Invalid or missing domain, no credentials queried");
        }

        Log.d(TAG, "FillResponse built, sending to callback");
        Log.d(TAG, "==========================================");
        callback.onSuccess(responseBuilder.build());
    }

    @Override
    public void onSaveRequest(@NonNull SaveRequest request, @NonNull SaveCallback callback) {

        Log.d(TAG, "==========================================");
        Log.d(TAG, "onSaveRequest triggered");
        Log.d(TAG, "==========================================");

        // Check if autofill is enabled in app settings
        if (!SharedPreferencesHelper.isAutofillEnabled(getApplicationContext())) {
            Log.d(TAG, "Autofill is disabled in app settings");
            callback.onSuccess();
            return;
        }

        // Get the structure from the request
        List<FillContext> fillContexts = request.getFillContexts();
        if (fillContexts == null || fillContexts.isEmpty()) {
            Log.d(TAG, "No fill contexts available");
            callback.onSuccess();
            return;
        }

        AssistStructure structure = fillContexts.get(fillContexts.size() - 1).getStructure();

        // Parse the structure to extract credentials
        // Note: we do NOT gate on parse() return value — parse() now returns true for
        // username-only screens (Instagram second screen). We check fields explicitly.
        AutofillFieldsParser parser = new AutofillFieldsParser();
        parser.parse(structure);

        String username = parser.getUsername();
        String password = parser.getPassword();
        String activityPackage = parser.getActivityPackage();
        String webDomain = parser.getWebDomain();
        boolean isBrowser = activityPackage != null && BROWSER_PACKAGES.contains(activityPackage);

        // Determine save domain:
        // - Browser: normalise webDomain (unchanged)
        // - Native app: use raw activityPackage — do NOT normalise package names
        String saveDomain;
        if (isBrowser && webDomain != null) {
            saveDomain = AutofillHelper.normalizeDomain(webDomain);
            Log.d(TAG, "[AUTOFILL] Save: browser path — domain = " + saveDomain);
        } else if (activityPackage != null && !activityPackage.isEmpty()) {
            saveDomain = activityPackage; // raw, e.g. com.instagram.android
            Log.d(TAG, "[AUTOFILL] Save: native app path — domain = " + saveDomain);
        } else {
            saveDomain = AutofillHelper.normalizeDomain(parser.getDomain());
            Log.d(TAG, "[AUTOFILL] Save: fallback path — domain = " + saveDomain);
        }

        Log.d(TAG, "--- Extracted Credentials ---");
        Log.d(TAG, "[AUTOFILL] Activity package: " + activityPackage);
        Log.d(TAG, "[AUTOFILL] Save domain: " + saveDomain);
        Log.d(TAG, "Username: " + (username != null && !username.isEmpty() ? username : "(empty)"));
        Log.d(TAG, "Password: " + (password != null && !password.isEmpty() ? "***" : "(empty)"));

        // Validate password
        if (password == null || password.isEmpty()) {
            Log.d(TAG, "Password is empty, cannot save");
            callback.onSuccess();
            return;
        }

        if (!AutofillHelper.isValidDomain(saveDomain)) {
            Log.d(TAG, "Invalid domain, cannot save");
            callback.onSuccess();
            return;
        }

        // Save to database
        try {
            DatabaseHelper dbHelper = DatabaseServiceLocator.getDatabaseHelper();

            if (dbHelper == null) {
                Log.d(TAG, "DatabaseHelper was null, attempting lazy initialization");
                dbHelper = DatabaseServiceLocator.getInstance(getApplicationContext());
            }

            if (dbHelper == null) {
                Log.e(TAG, "Failed to get DatabaseHelper instance");
                callback.onSuccess();
                return;
            }

            String email = (username != null && !username.isEmpty()) ? username : "";
            String name = saveDomain; // Use raw domain (package name or web domain) as the name

            Log.d(TAG, "Attempting to save — Name: " + name + ", Email: " + email + ", Domain: " + saveDomain);

            // Check if credential already exists
            if (!dbHelper.checkIfAccountAlreadyExist(getApplicationContext(), name, email)) {
                dbHelper.addEntry(getApplicationContext(), name, email, password, saveDomain);
                Log.d(TAG, "✓ Credential saved successfully");
            } else {
                Log.d(TAG, "Credential already exists, not saving duplicate");
            }

            Log.d(TAG, "==========================================");
            callback.onSuccess();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save password: " + e.getMessage(), e);
            Log.d(TAG, "==========================================");
            callback.onFailure("Failed to save password: " + e.getMessage());
        }
    }

    /**
     * Queries the database for credentials matching the given domain.
     * For native apps: also queries by raw activityPackage to catch all saved
     * credentials.
     * Deduplicates results via a Set of unique keys.
     *
     * @param domain          The primary domain to look up (could be normalised web
     *                        domain or raw package name)
     * @param activityPackage The raw package name of the requesting app (may be
     *                        same as domain for native apps)
     */
    private List<CredentialData> getCredentialsForDomain(String domain, String activityPackage) {
        List<CredentialData> credentialsList = new ArrayList<>();
        Set<String> uniqueCredentialKeys = new HashSet<>();

        String normalizedDomain = AutofillHelper.normalizeDomain(domain);
        String baseDomain = AutofillHelper.getBaseDomain(normalizedDomain);

        Log.d(TAG, "[AUTOFILL] Querying by normalizedDomain: " + normalizedDomain);
        queryAndAddCredentials(normalizedDomain, credentialsList, uniqueCredentialKeys);

        if (baseDomain != null && !baseDomain.equals(normalizedDomain)) {
            Log.d(TAG, "[AUTOFILL] Querying by baseDomain: " + baseDomain);
            queryAndAddCredentials(baseDomain, credentialsList, uniqueCredentialKeys);
        }

        // For native apps: also query by the raw activityPackage name
        // This handles saved credentials stored as "com.instagram.android"
        if (activityPackage != null && !activityPackage.isEmpty()
                && !activityPackage.equals(normalizedDomain)
                && !activityPackage.equals(baseDomain)) {
            Log.d(TAG, "[AUTOFILL] Querying by activityPackage: " + activityPackage);
            queryAndAddCredentials(activityPackage, credentialsList, uniqueCredentialKeys);
        }

        Log.d(TAG, "[AUTOFILL] Total credential matches: " + credentialsList.size());
        return credentialsList;
    }

    private void queryAndAddCredentials(String domainToQuery, List<CredentialData> list, Set<String> keys) {
        try {
            DatabaseHelper dbHelper = DatabaseServiceLocator.getDatabaseHelper();

            if (dbHelper == null) {
                dbHelper = DatabaseServiceLocator.getInstance(getApplicationContext());
            }

            if (dbHelper == null) {
                return;
            }

            Cursor cursor = dbHelper.getCredentialsByDomain(getApplicationContext(), domainToQuery);

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int nameIndex = cursor.getColumnIndex("record_name");
                    int emailIndex = cursor.getColumnIndex("record_email");
                    int passwordIndex = cursor.getColumnIndex("record_password");

                    if (nameIndex >= 0 && emailIndex >= 0 && passwordIndex >= 0) {
                        String name = cursor.getString(nameIndex);
                        String email = cursor.getString(emailIndex);
                        String encryptedPassword = cursor.getString(passwordIndex);

                        // Deduplicate
                        String uniqueKey = email + ":" + name;
                        if (keys.contains(uniqueKey)) {
                            continue;
                        }

                        // Decrypt password
                        String password = null;
                        try {
                            password = EncryptionHelper.decrypt(encryptedPassword);
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to decrypt password for: " + name);
                            continue;
                        }

                        list.add(new CredentialData(name, email, password));
                        keys.add(uniqueKey);
                        Log.d(TAG, "[AUTOFILL] Credential matched: " + name + " (" + email + ")");
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in queryAndAddCredentials for: " + domainToQuery, e);
        }
    }

    /**
     * Simple data class to hold credential information.
     */
    private static class CredentialData {
        String name;
        String email;
        String password;

        CredentialData(String name, String email, String password) {
            this.name = name;
            this.email = email;
            this.password = password;
        }
    }
}
