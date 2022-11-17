package org.adaptlab.chpir.android.survey.viewmodelfactories;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.adaptlab.chpir.android.survey.viewmodels.SurveyViewModel;

public class SurveyViewModelFactory implements ViewModelProvider.Factory {
    private final String mUUID;
    private final Application mApplication;

    public SurveyViewModelFactory(@NonNull Application application, String uuid) {
        this.mApplication = application;
        this.mUUID = uuid;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SurveyViewModel(mApplication, mUUID);
    }

}
