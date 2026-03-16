package com.example.app.view.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.app.R;
import com.example.app.SharedPreferences.SharedPreferencesHelper;

import java.util.Objects;

public class LanguageDialogFragment extends DialogFragment {

    private int position;

    public interface LanguageListener {
        void onPositiveButtonClicked(String[] list, int position);
        void onNegativeButtonClicked();
    }

    private LanguageListener mListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (LanguageListener) context;
        } catch (Exception e) {
            throw new ClassCastException(getActivity() + " LanguageListener must be implemented!");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        String currentLanguage = SharedPreferencesHelper.getCurrentLanguage(requireContext());
        String[] languageList = requireActivity()
                .getResources()
                .getStringArray(R.array.language_options);

        position = getLanguagePosition(languageList, currentLanguage.substring(0, 2));

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        builder.setTitle(R.string.languagedialog_select_your_choice)
                .setSingleChoiceItems(languageList, position, (dialog, which) -> position = which)
                .setPositiveButton("Ok", (dialog, which) -> {
                    mListener.onPositiveButtonClicked(languageList, position);
                    Toast.makeText(requireContext(),
                            getString(R.string.language_changed),
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel,
                        (dialog, which) -> mListener.onNegativeButtonClicked());

        AlertDialog dialog = builder.create();

        // ✅ Apply rounded dialog background
        applyDialogBackground(dialog);

        return dialog;
    }

    private void applyDialogBackground(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.background_dialog);
        }
    }

    private int getLanguagePosition(String[] languageList, String currentLanguage) {
        for (int i = 0; i < languageList.length; i++) {
            if (Objects.equals(
                    languageList[i].toLowerCase().substring(0, 2),
                    currentLanguage)) {
                position = i;
                break;
            }
        }
        return position;
    }
}
