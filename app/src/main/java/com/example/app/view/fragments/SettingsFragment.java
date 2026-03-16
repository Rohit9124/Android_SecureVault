package com.example.app.view.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.security.crypto.EncryptedSharedPreferences;

import com.example.app.R;
import com.example.app.databinding.FragmentSettingsBinding;
import com.example.app.encryption.EncryptionHelper;
import com.example.app.model.SettingData;
import com.example.app.utilities.AppLockManager;
import com.example.app.utilities.DialogHelper;
import com.example.app.utilities.VibrationHelper;
import com.example.app.view.activities.MainViewActivity;
import com.example.app.view.adapters.SettingsAdapter;

import java.util.ArrayList;

public class SettingsFragment extends Fragment {
    private static final int REQUEST_CODE_IMPORT_DOCUMENT = 2;
    private ImageButton buttonBack;
    private FragmentSettingsBinding binding;
    private ListView listView;
    private String url;
    private Intent intent;
    private EncryptedSharedPreferences encryptedSharedPreferences;
    static final int DARK_THEME = 0;
    static final int LOCK_SCREEN = 1;
    static final int CHANGE_LANGUAGE = 2;
    static final int CHANGE_PASSWORD = 3;
    static final int EXPORT = 4;
    static final int IMPORT = 5;
    static final int SHARE = 8;
    static final int APP_VERSION = 10;
    public static final int AUTO_CLEAR_CLIPBOARD = 11;
    public static final int AUTO_CLEAR_DELAY = 12;
    static final int ENABLE_AUTOFILL = 13;
    static final int EMERGENCY_LOCK = 14;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(binding);
        Activity activity = this.getActivity();

        ArrayList<SettingData> arrayList = new ArrayList<>();
        encryptedSharedPreferences = EncryptionHelper.getEncryptedSharedPreferences(requireContext());

        buttonBack.setOnClickListener(v -> {
            if (activity instanceof MainViewActivity) {
                ((MainViewActivity) activity).onBackPressed();
            }
        });

        createSettingsList(arrayList);

        SettingsAdapter settingsAdapter = new SettingsAdapter(requireContext(), R.layout.list_row, arrayList,
                getActivity());

        listView.setAdapter(settingsAdapter);

        listView.setOnItemClickListener((parent, view1, position, id) -> {

            SettingData setting = (SettingData) parent.getItemAtPosition(position);
            if (setting == null)
                return;

            switch (setting.getPosition()) {

                case AUTO_CLEAR_CLIPBOARD:
                    VibrationHelper.vibrate(binding.getRoot(), VibrationHelper.VibrationType.Weak);
                    showAutoClearDelayDialog();
                    break;

                case CHANGE_LANGUAGE:
                    VibrationHelper.vibrate(binding.getRoot(), VibrationHelper.VibrationType.Weak);
                    DialogFragment languageDialogFragment = new LanguageDialogFragment();
                    languageDialogFragment.setCancelable(false);
                    languageDialogFragment.show(
                            requireActivity().getSupportFragmentManager(),
                            "Language Dialog");
                    break;

                case CHANGE_PASSWORD:
                    VibrationHelper.vibrate(binding.getRoot(), VibrationHelper.VibrationType.Weak);
                    DialogHelper.showChangePasswordDialog(requireContext(), encryptedSharedPreferences);
                    break;

                case EXPORT:
                    VibrationHelper.vibrate(binding.getRoot(), VibrationHelper.VibrationType.Weak);
                    DialogHelper.showExportingDialog(requireContext());
                    break;

                case IMPORT:
                    VibrationHelper.vibrate(binding.getRoot(), VibrationHelper.VibrationType.Weak);
                    AppLockManager.setSkipNextLock(true); // Bypass Emergency Lock for file picker
                    Intent intentImport = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intentImport.addCategory(Intent.CATEGORY_OPENABLE);
                    intentImport.setType("*/*");
                    startActivityForResult(intentImport, REQUEST_CODE_IMPORT_DOCUMENT);
                    break;

                case SHARE:
                    VibrationHelper.vibrate(binding.getRoot(), VibrationHelper.VibrationType.Weak);

                    // Bypass Emergency Lock for external share sheet
                    AppLockManager.setSkipNextLock(true);

                    String driveLink = "https://drive.google.com/drive/folders/1huZXH7TgIxfSLwmnyC6NuQwXIiX8PZiA?usp=drive_link";

                    String shareMessage = "SecureVault – Google Drive\n\n"
                            + "Hey! You can download the SecureVault APK from this link:\n"
                            + driveLink;

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "SecureVault – Download");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

                    try {
                        startActivity(Intent.createChooser(shareIntent, "Share SecureVault"));
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "No app available to share.", Toast.LENGTH_SHORT).show();
                    }

                    break;


                case APP_VERSION:
                    Toast.makeText(requireContext(), "\uD83D\uDE80⚡", Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private void createSettingsList(ArrayList<SettingData> arrayList) {
        arrayList.add(new SettingData(DARK_THEME, R.drawable.settings_icon_dark_theme,
                getString(R.string.settings_dark_theme), false, true, 1));
        arrayList.add(new SettingData(LOCK_SCREEN, R.drawable.icon_open_lock,
                getString(R.string.use_screen_lock_to_unlock), false, true, 2));
        arrayList.add(new SettingData(EMERGENCY_LOCK, R.drawable.settings_icon_lock,
                getString(R.string.settings_emergency_lock), false, true, 5));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            arrayList.add(new SettingData(ENABLE_AUTOFILL, R.drawable.settings_icon_lock, "Enable Autofill",
                    false, true, 4));
        }
        arrayList.add(new SettingData(AUTO_CLEAR_CLIPBOARD, R.drawable.settings_icon_lock, "Auto-clear clipboard",
                true));
        arrayList.add(new SettingData(CHANGE_LANGUAGE, R.drawable.settings_icon_language,
                getString(R.string.settings_change_language)));
        arrayList.add(new SettingData(CHANGE_PASSWORD, R.drawable.settings_icon_lock,
                getString(R.string.settings_change_password)));
        arrayList.add(new SettingData(EXPORT, R.drawable.icon_export, getString(R.string.settings_export_db)));
        arrayList.add(new SettingData(IMPORT, R.drawable.icon_import, getString(R.string.settings_import_db)));

        // Add autofill setting only on Android 8.0+ (API 26+)

        arrayList.add(
                new SettingData(SHARE, R.drawable.settings_icon_share, getString(R.string.settings_share_newpass),
                        true));
        arrayList.add(new SettingData(APP_VERSION, R.drawable.settings_icon_version,
                getString(R.string.app_version) + getAppVersion()));
    }

    private String getAppVersion() {
        String versionName = "";

        try {
            PackageManager packageManager = requireActivity().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(requireActivity().getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("AppVersion", "Error getting app version", e);
        }
        return versionName;
    }

    private void initViews(FragmentSettingsBinding binding) {
        buttonBack = binding.backButton;
        listView = binding.listView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri fileURL;

        if (resultCode == Activity.RESULT_OK) {

            if (requestCode == REQUEST_CODE_IMPORT_DOCUMENT) {
                if (data != null) {
                    fileURL = data.getData();

                    DialogHelper.showImportingDialog(requireContext(), fileURL);
                }
            }
        }
    }

    private void showAutoClearDelayDialog() {

        final CharSequence[] items = {"Off", "30 seconds", "1 minute", "5 minutes"};

        boolean isAutoClearEnabled = com.example.app.utilities.SettingsPrefs.INSTANCE
                .isAutoClearEnabled(requireContext());
        long currentDelay = com.example.app.utilities.SettingsPrefs.INSTANCE
                .getAutoClearDelay(requireContext());

        int checkedItem = 0; // Default "Off"

        if (isAutoClearEnabled) {
            if (currentDelay == com.example.app.utilities.SettingsPrefs.DELAY_30_SECONDS)
                checkedItem = 1;
            else if (currentDelay == com.example.app.utilities.SettingsPrefs.DELAY_1_MINUTE)
                checkedItem = 2;
            else if (currentDelay == com.example.app.utilities.SettingsPrefs.DELAY_5_MINUTES)
                checkedItem = 3;
        }

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext());

        builder.setTitle("Auto-clear clipboard")
                .setSingleChoiceItems(items, checkedItem, (dialog, which) -> {

                    if (which == 0) {
                        com.example.app.utilities.SettingsPrefs.INSTANCE
                                .setAutoClearEnabled(requireContext(), false);
                    } else {

                        com.example.app.utilities.SettingsPrefs.INSTANCE
                                .setAutoClearEnabled(requireContext(), true);

                        long delay = com.example.app.utilities.SettingsPrefs.DELAY_30_SECONDS;

                        if (which == 2)
                            delay = com.example.app.utilities.SettingsPrefs.DELAY_1_MINUTE;
                        else if (which == 3)
                            delay = com.example.app.utilities.SettingsPrefs.DELAY_5_MINUTES;

                        com.example.app.utilities.SettingsPrefs.INSTANCE
                                .setAutoClearDelay(requireContext(), delay);
                    }

                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        // ✅ Apply rounded dialog background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.background_dialog);
        }
    }
}