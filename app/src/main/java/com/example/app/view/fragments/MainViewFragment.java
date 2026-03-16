package com.example.app.view.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.content.Context;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app.R;
import com.example.app.databinding.FragmentMainViewBinding;
import com.example.app.utilities.VibrationHelper;
import com.example.app.view.activities.MainViewActivity;
import com.example.app.view.adapters.CustomAdapter;
import com.example.app.viewmodel.MainViewModel;

import java.util.Objects;

public class MainViewFragment extends Fragment {

    private FragmentMainViewBinding binding;
    private TextView noData, count;
    private ImageView empty_imageview;
    private RecyclerView recyclerView;
    private ImageButton buttonGenerate, buttonAdd, buttonSettings,
            buttonSearch, buttonCancel, buttonFilter;
    private MainViewModel mainViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentMainViewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint({ "SetTextI18n", "ClickableViewAccessibility" })
    @Override
    public void onViewCreated(@NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        initViews();
        setupObservers();
        mainViewModel.storeDataInArrays();

        Activity activity = this.getActivity();

        if (activity instanceof MainViewActivity) {

            buttonSettings.setOnClickListener(v -> {
                VibrationHelper.vibrate(v, VibrationHelper.VibrationType.Weak);
                ((MainViewActivity) activity).openFragment(new SettingsFragment());
            });

            buttonAdd.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        VibrationHelper.vibrate(v, VibrationHelper.VibrationType.Weak);
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.performClick();
                        VibrationHelper.vibrate(v, VibrationHelper.VibrationType.Strong);
                        ((MainViewActivity) activity).openFragment(new AddPasswordFragment());
                        return true;
                }
                return false;
            });

            buttonGenerate.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        VibrationHelper.vibrate(v, VibrationHelper.VibrationType.Weak);
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.performClick();
                        VibrationHelper.vibrate(v, VibrationHelper.VibrationType.Strong);
                        ((MainViewActivity) activity).openFragment(new GeneratePasswordFragment());
                        return true;
                }
                return false;
            });

            buttonSearch.setOnClickListener(v -> showInputDialog());

            buttonCancel.setOnClickListener(v -> {
                buttonCancel.setVisibility(View.GONE);
                mainViewModel.setSearchTerm("");
            });

            buttonFilter.setOnClickListener(this::showFilterMenu);
        }
    }

    private void setupObservers() {
        mainViewModel.getFilteredUserDataList()
                .observe(getViewLifecycleOwner(), userDataList -> {

                    CustomAdapter adapter = new CustomAdapter(getActivity(), getContext(), userDataList);

                    recyclerView.setAdapter(adapter);
                    recyclerView.setLayoutManager(
                            new LinearLayoutManager(requireContext()));

                    count.setText("[" + adapter.getItemCount() + "]");

                    if (adapter.getItemCount() == 0) {
                        empty_imageview.setVisibility(View.VISIBLE);
                        noData.setVisibility(View.VISIBLE);
                    } else {
                        empty_imageview.setVisibility(View.GONE);
                        noData.setVisibility(View.GONE);
                    }
                });
    }

    private void showFilterMenu(View v) {
        Context wrapper = new ContextThemeWrapper(requireContext(), R.style.SecureVaultPopupMenu);
        PopupMenu popup = new PopupMenu(wrapper, v);

        popup.getMenuInflater().inflate(R.menu.filter_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.filter_all) {
                mainViewModel.setFilter(MainViewModel.FilterType.ALL);
            } else if (id == R.id.filter_az) {
                mainViewModel.setFilter(MainViewModel.FilterType.AZ);
            } else if (id == R.id.filter_za) {
                mainViewModel.setFilter(MainViewModel.FilterType.ZA);
            } else if (id == R.id.filter_recently_added) {
                mainViewModel.setFilter(MainViewModel.FilterType.RECENTLY_ADDED);
            } else if (id == R.id.filter_recently_updated) {
                mainViewModel.setFilter(MainViewModel.FilterType.RECENTLY_UPDATED);
            }
            return true;
        });

        popup.show();
    }

    @SuppressLint("SetTextI18n")
    private void showInputDialog() {
        Log.d("MainViewFragment", "Search button clicked");

        // Guard: never call requireContext() when the fragment is not attached
        if (!isAdded() || getContext() == null) {
            Log.w("MainViewFragment", "showInputDialog: fragment is not attached, aborting");
            return;
        }

        Log.d("MainViewFragment", "Inflating dialog_search layout...");
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_search, null);
        Log.d("MainViewFragment", "Layout inflated successfully");

        EditText input = dialogView.findViewById(R.id.input);
        if (input == null) {
            Log.e("MainViewFragment", "R.id.input is null in dialog_search.xml — aborting dialog");
            return;
        }
        Log.d("MainViewFragment", "Input EditText found");

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.search_password)
                .setView(dialogView)
                .setPositiveButton(R.string.ok, (d, which) -> {
                    // Guard: fragment may have been detached before OK is pressed
                    if (!isAdded() || binding == null)
                        return;

                    String searchTerm = input.getText().toString().toLowerCase().trim();
                    buttonCancel.setVisibility(searchTerm.isEmpty() ? View.GONE : View.VISIBLE);

                    // Route through ViewModel — no inner observe() needed
                    mainViewModel.setSearchTerm(searchTerm);
                })
                .setNegativeButton(R.string.cancel, (d, which) -> d.cancel())
                .create();

        Log.d("MainViewFragment", "Calling dialog.show()");
        dialog.show();

        // Apply rounded background AFTER show() — Window is non-null only after show()
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.background_dialog);
            Log.d("MainViewFragment", "Rounded background applied");
        } else {
            Log.w("MainViewFragment", "dialog.getWindow() is null — background not applied");
        }
    }

    private void initViews() {
        recyclerView = binding.recyclerView;
        buttonGenerate = binding.buttonGenerate;
        buttonAdd = binding.buttonAdd;
        buttonSettings = binding.buttonSettings;
        count = binding.textViewCount;
        empty_imageview = binding.emptyImageview;
        noData = binding.noData;
        buttonSearch = binding.buttonSearch;
        buttonCancel = binding.buttonCancel;
        buttonFilter = binding.buttonFilter;
    }

    @Override
    public void onResume() {
        super.onResume();

        getParentFragmentManager()
                .setFragmentResultListener("requestKey",
                        this,
                        (requestKey, bundle) -> {

                            String result = bundle.getString("resultKey");

                            if (Objects.equals(result, "1")) {
                                mainViewModel.storeDataInArrays();
                            }
                        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
