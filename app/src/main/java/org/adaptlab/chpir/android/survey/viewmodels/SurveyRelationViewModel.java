package org.adaptlab.chpir.android.survey.viewmodels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import org.adaptlab.chpir.android.survey.relations.SurveyRelation;
import org.adaptlab.chpir.android.survey.repositories.SurveyRepository;

public class SurveyRelationViewModel extends AndroidViewModel {
    private LiveData<SurveyRelation> mSurveyRelation;

    public SurveyRelationViewModel(@NonNull Application application, String uuid) {
        super(application);
        SurveyRepository surveyRepository = new SurveyRepository(application);
        mSurveyRelation = surveyRepository.getSurveyDao().findSurveyRelationByUUID(uuid);
    }

    public LiveData<SurveyRelation> getSurveyRelation() {
        return mSurveyRelation;
    }

}
