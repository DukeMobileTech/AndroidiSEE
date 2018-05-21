package org.adaptlab.chpir.android.survey;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ViewGroup;

public abstract class QuestionFragment extends Fragment {
    protected final static String LIST_DELIMITER = ",";
    protected SurveyFragment mSurveyFragment;
    protected abstract void unSetResponse();
    protected abstract void createQuestionComponent(ViewGroup questionComponent);
    protected abstract void deserialize(String responseText);
    protected abstract String serialize();
    protected abstract void setSpecialResponse(String specialResponse);
    protected abstract String getSpecialResponse();
    protected abstract void setDisplayInstructions();
    protected abstract void hideIndeterminateProgressBar();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mSurveyFragment = (SurveyFragment) getParentFragment();
    }

    @Override
    public void onStart() {
        super.onStart();
        hideIndeterminateProgressBar();
    }

    /*
    This is needed to hide the indeterminate progress bar when showing fragments that have previously been
    added and hidden. The onStart lifecycle event is not called when using the method FragmentTransaction.show(fragment)
     */
    @Override
    public void onHiddenChanged (boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            hideIndeterminateProgressBar();
        }
    }

}
