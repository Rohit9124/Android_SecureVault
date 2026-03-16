package com.example.app.factory;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.app.repository.ResourceRepository;
import com.example.app.viewmodel.AddViewModel;
import com.example.app.viewmodel.LoginViewModel;
import com.example.app.viewmodel.UpdateViewModel;

public class ViewMoldelsFactory implements ViewModelProvider.Factory {
    private ResourceRepository resourceRepository;

    public ViewMoldelsFactory(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AddViewModel.class)) {
            return (T) new AddViewModel(resourceRepository);
        }

        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            return (T) new LoginViewModel(resourceRepository);
        }

        if (modelClass.isAssignableFrom(UpdateViewModel.class)) {
            return (T) new UpdateViewModel(resourceRepository);
        }

        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}

