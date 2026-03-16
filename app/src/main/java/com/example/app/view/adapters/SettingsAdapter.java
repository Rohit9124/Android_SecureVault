package com.example.app.view.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.app.R;
import com.example.app.SharedPreferences.SharedPreferencesHelper;
import com.example.app.model.SettingData;
import com.example.app.utilities.VibrationHelper;
import com.example.app.view.activities.MainViewActivity;

import java.util.ArrayList;

public class SettingsAdapter extends ArrayAdapter<SettingData> {

    private final Context mContext;
    private final int mResource;
    private final Activity mActivity;

    private Boolean isDarkModeSet;
    private final int DARK_THEME_SWITCH = 1;
    private final int SCREEN_LOCK_SWITCH = 2;
    private final int AUTOFILL_SWITCH = 4;
    private final int EMERGENCY_LOCK_SWITCH = 5;

    // Constructor
    public SettingsAdapter(@NonNull Context context, int resource, @NonNull ArrayList<SettingData> objects,
            Activity activity) {
        super(context, resource, objects);
        this.mContext = context;
        this.mResource = resource;
        this.mActivity = activity;
    }

    // View holder class
    private static class ViewHolder {
        ImageView imageView;
        TextView txtName;
        ImageButton switchView;
        ImageView arrowImage;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(mResource, parent, false);
            holder = new ViewHolder();
            holder.imageView = convertView.findViewById(R.id.image);
            holder.txtName = convertView.findViewById(R.id.txtName);
            holder.switchView = convertView.findViewById(R.id.switch1);
            holder.arrowImage = convertView.findViewById(R.id.arrow);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SettingData setting = getItem(position);
        if (setting != null) {
            holder.imageView.setImageResource(setting.getImage());
            holder.txtName.setText(setting.getName());

            if (setting.getSwitchPresence()) {
                holder.switchView.setVisibility(View.VISIBLE);

                // int imageResource = (SharedPreferencesHelper.isDarkModeSet(mContext)) ?
                // R.drawable.btn_yes : R.drawable.btn_no;
                // holder.switchView.setImageDrawable(ContextCompat.getDrawable(mContext,
                // imageResource));

                int switchID = setting.getSwitchID();
                int imageResource;

                if (switchID == DARK_THEME_SWITCH) {
                    imageResource = (SharedPreferencesHelper.isDarkModeSet(mContext)) ? R.drawable.btn_yes
                            : R.drawable.btn_no;

                } else if (switchID == SCREEN_LOCK_SWITCH) {
                    imageResource = (SharedPreferencesHelper.isScreenLockEnabled(mContext)) ? R.drawable.btn_yes
                            : R.drawable.btn_no;
                    // Log.i("switches", String.valueOf(imageResource));
                } else if (switchID == AUTOFILL_SWITCH) {
                    imageResource = (SharedPreferencesHelper.isAutofillEnabled(mContext))
                            ? R.drawable.btn_yes
                            : R.drawable.btn_no;
                } else if (switchID == EMERGENCY_LOCK_SWITCH) {
                    imageResource = (SharedPreferencesHelper.isEmergencyLockEnabled(mContext))
                            ? R.drawable.btn_yes
                            : R.drawable.btn_no;
                } else {
                    imageResource = R.drawable.btn_yes; // Imposta un valore predefinito nel caso in cui l'ID dello
                                                        // switch non sia valido
                }

                holder.switchView.setImageDrawable(ContextCompat.getDrawable(mContext, imageResource));

                holder.switchView.setOnClickListener(v -> {
                    VibrationHelper.vibrate(v, VibrationHelper.VibrationType.Weak);

                    switch (switchID) {

                        case DARK_THEME_SWITCH:
                            toggleDarkMode();
                            break;

                        case SCREEN_LOCK_SWITCH:
                            toggleScreenLock(holder);
                            break;

                        case AUTOFILL_SWITCH:
                            toggleAutofill(holder);
                            break;

                        case EMERGENCY_LOCK_SWITCH:
                            toggleEmergencyLock(holder);
                            break;

                    }
                });
            } else {
                holder.switchView.setVisibility(View.GONE);
            }

            if (setting.getImagePresence()) {
                holder.arrowImage.setVisibility(View.VISIBLE);
            } else {
                holder.arrowImage.setVisibility(View.GONE);
                holder.arrowImage.setVisibility(View.GONE);
            }

        }
        return convertView;
    }

    private void toggleDarkMode() {
        boolean isDarkModeSet = SharedPreferencesHelper.isDarkModeSet(mContext);
        if (isDarkModeSet) {
            SharedPreferencesHelper.setAndEditSharedPrefForLightMode(mContext);
        } else {
            SharedPreferencesHelper.setAndEditSharedPrefForDarkMode(mContext);
        }
        if (mActivity instanceof MainViewActivity) {
            SharedPreferencesHelper.updateNavigationBarColor(isDarkModeSet, mActivity);
        }
    }

    private void toggleScreenLock(ViewHolder holder) {

        SharedPreferencesHelper.setUseScreenLockToUnlock(mContext);
        int imageResource = (SharedPreferencesHelper.isScreenLockEnabled(mContext)) ? R.drawable.btn_yes
                : R.drawable.btn_no;
        holder.switchView.setImageDrawable(ContextCompat.getDrawable(mContext, imageResource));
    }

    private void toggleAutofill(ViewHolder holder) {
        // Show info dialog explaining Autofill
        new androidx.appcompat.app.AlertDialog.Builder(mContext)
                .setTitle(mContext.getString(R.string.autofill_dialog_title))
                .setMessage(mContext.getString(R.string.autofill_dialog_message))
                .setPositiveButton(mContext.getString(R.string.autofill_enable_button), (dialog, which) -> {
                    // Enable autofill preference
                    SharedPreferencesHelper.setAutofillEnabled(mContext, true);
                    holder.switchView.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.btn_yes));

                    // Redirect to Android Autofill settings
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        try {
                            // Set flag to prevent Emergency Lock from triggering
                            if (mContext instanceof com.example.app.view.activities.MainViewActivity) {
                                com.example.app.view.activities.MainViewActivity.setOpeningSystemSettings(true);
                            }

                            android.content.Intent intent = new android.content.Intent(
                                    android.provider.Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE);

                            // Create ComponentName for SecureVaultAutofillService
                            android.content.ComponentName componentName = new android.content.ComponentName(
                                    mContext.getPackageName(),
                                    "com.example.app.autofill.SecureVaultAutofillService");

                            // Add the autofill service component as data URI
                            intent.setData(android.net.Uri.parse("package:" + componentName.flattenToString()));

                            // Ensure we're using an Activity context
                            if (mContext instanceof android.app.Activity) {
                                mContext.startActivity(intent);
                            } else {
                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                                mContext.startActivity(intent);
                            }
                        } catch (Exception e) {
                            // Fallback to general autofill settings
                            try {
                                android.content.Intent intent = new android.content.Intent(
                                        android.provider.Settings.ACTION_SETTINGS);
                                if (!(mContext instanceof android.app.Activity)) {
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                                }
                                mContext.startActivity(intent);
                            } catch (Exception ex) {
                                // Unable to open settings
                            }
                        }
                    }
                })
                .setNegativeButton(mContext.getString(R.string.cancel), (dialog, which) -> {
                    // Keep autofill disabled
                    SharedPreferencesHelper.setAutofillEnabled(mContext, false);
                    holder.switchView.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.btn_no));
                })
                .setCancelable(false)
                .show();
    }

    private void toggleEmergencyLock(ViewHolder holder) {
        boolean current = SharedPreferencesHelper.isEmergencyLockEnabled(mContext);

        if (!current) {
            // User is enabling Emergency Lock - show info dialog
            new androidx.appcompat.app.AlertDialog.Builder(mContext)
                    .setTitle(mContext.getString(R.string.emergency_lock_dialog_title))
                    .setMessage(mContext.getString(R.string.emergency_lock_dialog_message))
                    .setPositiveButton(mContext.getString(R.string.ok), (dialog, which) -> {
                        // Enable Emergency Lock
                        SharedPreferencesHelper.setEmergencyLockEnabled(mContext, true);
                        holder.switchView.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.btn_yes));
                    })
                    .setNegativeButton(mContext.getString(R.string.cancel), (dialog, which) -> {
                        // Keep Emergency Lock disabled
                        holder.switchView.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.btn_no));
                    })
                    .setCancelable(false)
                    .show();
        } else {
            // User is disabling Emergency Lock
            SharedPreferencesHelper.setEmergencyLockEnabled(mContext, false);
            holder.switchView.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.btn_no));
        }
    }
}
