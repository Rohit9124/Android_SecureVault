package com.example.app.viewmodel;

import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.app.model.UserData;
import com.example.app.database.DatabaseHelper;
import com.example.app.database.DatabaseServiceLocator;
import java.util.ArrayList;


import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainViewModel extends ViewModel {

    public enum FilterType {
        ALL, AZ, ZA, RECENTLY_ADDED, RECENTLY_UPDATED
    }

    private final MutableLiveData<ArrayList<UserData>> filteredUserDataList = new MutableLiveData<>();
    private final ArrayList<ArrayList<UserData>> cachedFullList = new ArrayList<>(); // Using ArrayList of ArrayList to
                                                                                     // match current pattern if needed,
                                                                                     // but simple ArrayList is better.
    private ArrayList<UserData> allEntries = new ArrayList<>();
    private final DatabaseHelper myDB;
    private String currentSearchTerm = "";
    private FilterType currentFilter = FilterType.ALL;

    public MainViewModel() {
        myDB = DatabaseServiceLocator.getDatabaseHelper();
    }

    public void storeDataInArrays() {
        ArrayList<UserData> localList = new ArrayList<>();
        Cursor cursor = myDB.readAllData();

        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                UserData userData = new UserData(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4) // website
                );
                localList.add(userData);
            }
            cursor.close();
        }
        allEntries = localList;
        applyFilterAndSearch();
    }

    public void setFilter(FilterType filterType) {
        currentFilter = filterType;
        applyFilterAndSearch();
    }

    public void setSearchTerm(String searchTerm) {
        currentSearchTerm = searchTerm.toLowerCase().trim();
        applyFilterAndSearch();
    }

    private void applyFilterAndSearch() {
        ArrayList<UserData> resultList = new ArrayList<>();

        // Search
        for (UserData entry : allEntries) {
            if (currentSearchTerm.isEmpty() || entry.getName().toLowerCase().contains(currentSearchTerm)) {
                resultList.add(entry);
            }
        }

        // Filter (Sort)
        switch (currentFilter) {
            case AZ:
                Collections.sort(resultList, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
                break;
            case ZA:
                Collections.sort(resultList, (o1, o2) -> o2.getName().compareToIgnoreCase(o1.getName()));
                break;
            case RECENTLY_ADDED:
            case RECENTLY_UPDATED:
                // Using ID as proxy for recently added/updated since we don't have timestamps
                Collections.sort(resultList, (o1, o2) -> {
                    try {
                        return Integer.compare(Integer.parseInt(o2.getId()), Integer.parseInt(o1.getId()));
                    } catch (NumberFormatException e) {
                        return o2.getId().compareTo(o1.getId());
                    }
                });
                break;
            case ALL:
            default:
                // Default order (as returned by DB, usually ID ascending)
                break;
        }

        filteredUserDataList.postValue(resultList);
    }

    public LiveData<ArrayList<UserData>> getFilteredUserDataList() {
        return filteredUserDataList;
    }

    // Deprecated but kept for compatibility during transition if needed
    public LiveData<ArrayList<UserData>> getUserDataList() {
        return filteredUserDataList;
    }

    public void storeSearchedDataInArrays(String searchedData) {
        setSearchTerm(searchedData);
    }

    public LiveData<ArrayList<UserData>> getSearchedDataList() {
        return filteredUserDataList;
    }
}
