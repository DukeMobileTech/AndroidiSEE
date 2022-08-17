package org.adaptlab.chpir.android.survey;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import org.adaptlab.chpir.android.survey.adapters.DisplayPagerAdapter;
import org.adaptlab.chpir.android.survey.adapters.NavigationDrawerAdapter;
import org.adaptlab.chpir.android.survey.entities.Display;
import org.adaptlab.chpir.android.survey.entities.Instrument;
import org.adaptlab.chpir.android.survey.entities.Question;
import org.adaptlab.chpir.android.survey.entities.Response;
import org.adaptlab.chpir.android.survey.entities.Section;
import org.adaptlab.chpir.android.survey.entities.Survey;
import org.adaptlab.chpir.android.survey.relations.DisplayRelation;
import org.adaptlab.chpir.android.survey.relations.InstrumentRelation;
import org.adaptlab.chpir.android.survey.relations.QuestionTranslationRelation;
import org.adaptlab.chpir.android.survey.relations.SectionRelation;
import org.adaptlab.chpir.android.survey.relations.SurveyRelation;
import org.adaptlab.chpir.android.survey.repositories.SurveyRepository;
import org.adaptlab.chpir.android.survey.utils.AppUtil;
import org.adaptlab.chpir.android.survey.utils.LocaleManager;
import org.adaptlab.chpir.android.survey.utils.LocationManager;
import org.adaptlab.chpir.android.survey.utils.TranslationUtil;
import org.adaptlab.chpir.android.survey.viewmodelfactories.InstrumentRelationViewModelFactory;
import org.adaptlab.chpir.android.survey.viewmodelfactories.SectionViewModelFactory;
import org.adaptlab.chpir.android.survey.viewmodelfactories.SurveyRelationViewModelFactory;
import org.adaptlab.chpir.android.survey.viewmodelfactories.SurveyViewModelFactory;
import org.adaptlab.chpir.android.survey.viewmodels.InstrumentRelationViewModel;
import org.adaptlab.chpir.android.survey.viewmodels.SectionViewModel;
import org.adaptlab.chpir.android.survey.viewmodels.SettingsViewModel;
import org.adaptlab.chpir.android.survey.viewmodels.SurveyRelationViewModel;
import org.adaptlab.chpir.android.survey.viewmodels.SurveyViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.adaptlab.chpir.android.survey.utils.ConstantUtils.COMMA;
import static org.adaptlab.chpir.android.survey.utils.FormatUtils.styleTextWithHtmlWhitelist;

public class SurveyActivity extends AppCompatActivity {
    public final static String EXTRA_INSTRUMENT_ID = "org.adaptlab.chpir.android.survey.EXTRA_INSTRUMENT_ID";
    public final static String EXTRA_SURVEY_UUID = "org.adaptlab.chpir.android.survey.EXTRA_SURVEY_UUID";
    public final static String EXTRA_DISPLAY_ID = "org.adaptlab.chpir.android.survey.EXTRA_DISPLAY_ID";
    public final static String EXTRA_DISPLAY_POSITION = "org.adaptlab.chpir.android.survey.EXTRA_DISPLAY_POSITION";
    private static final int REVIEW_CODE = 100;
    private final String TAG = this.getClass().getName();
    private DisplayPagerAdapter mDisplayPagerAdapter;
    private ViewPager mViewPager;
    private Instrument mInstrument;
    private Survey mSurvey;
    private SurveyViewModel mSurveyViewModel;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ExpandableListView mExpandableListView;
    private ActionBar mActionBar;
    private Spinner mSpinner;
    private int mLastPosition = -1;
    private List<String> mLanguageCodes;
    private Long mInstrumentId;
    private String mSurveyUUID;
    private LocationManager mLocationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);
        if (getIntent() != null) {
            mInstrumentId = getIntent().getLongExtra(EXTRA_INSTRUMENT_ID, -1);
            if (mInstrumentId == -1) return;
            mSurveyUUID = getIntent().getStringExtra(EXTRA_SURVEY_UUID);
        }
        if (savedInstanceState != null) {
            assignExtras(savedInstanceState);
        }
        if (TextUtils.isEmpty(mSurveyUUID)) {
            mSurveyUUID = UUID.randomUUID().toString();
            SurveyRepository surveyRepository = new SurveyRepository(getApplication());
            Survey survey = surveyRepository.initializeSurvey(mSurveyUUID, AppUtil.getProjectId(), mInstrumentId);
            if (!mSurveyUUID.equals(survey.getUUID())) return;
        }

        setActionBar();
        mDisplayPagerAdapter = new DisplayPagerAdapter(getSupportFragmentManager(), mSurveyUUID);
        setDisplayViewPagers();
        addOnPageChangeListener();
        setInstrumentViewModel(mInstrumentId);
        setSurveyViewModel(mSurveyUUID);
        setSectionViewModel(mInstrumentId);
        setSurveyRelationViewModel(mSurveyUUID);
        setLanguage();
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (mLocationManager == null) {
            mLocationManager = new LocationManager(this);
            mLocationManager.startLocationUpdates();
        }
    }

    private void setActionBar() {
        mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeButtonEnabled(true);
        }
    }

    private void setDisplayViewPagers() {
        mViewPager = findViewById(R.id.displayPager);
        mViewPager.setAdapter(mDisplayPagerAdapter);
    }

    private void addOnPageChangeListener() {
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                setActionBarTitle(position);

                if (position > mSurveyViewModel.getDisplayPosition()) {
                    mSurveyViewModel.getPreviousDisplays().add(mSurveyViewModel.getDisplayPosition());
                    mSurveyViewModel.incrementDisplayPosition();
                } else if (position < mSurveyViewModel.getDisplayPosition()) {
                    int current = mSurveyViewModel.getDisplayPosition();
                    if (current > 0 && current < mSurveyViewModel.getDisplays().size() && mSurveyViewModel.getPreviousDisplays().size() > 0) {
                        mSurveyViewModel.setDisplayPosition(mSurveyViewModel.getPreviousDisplays().remove(mSurveyViewModel.getPreviousDisplays().size() - 1));
                    } else {
                        mSurveyViewModel.decrementDisplayPosition();
                    }
                }
                invalidateOptionsMenu();
            }
        });
    }

    private void setActionBarTitle(int position) {
        if (mSurveyViewModel.getDisplays() == null) return;
        Display display = mSurveyViewModel.getDisplays().get(position);
        mActionBar.setTitle(mSurveyViewModel.getDisplayTitle(display.getRemoteId()));
    }

    private void setInstrumentViewModel(Long instrumentId) {
        InstrumentRelationViewModelFactory factory = new InstrumentRelationViewModelFactory(getApplication(), instrumentId);
        InstrumentRelationViewModel viewModel = ViewModelProviders.of(this, factory).get(InstrumentRelationViewModel.class);
        viewModel.getInstrumentRelation().observe(this, new Observer<InstrumentRelation>() {
            @Override
            public void onChanged(@Nullable InstrumentRelation relation) {
                if (relation != null) {
                    mInstrument = relation.instrument;
                    mSurveyViewModel.setInstrumentLanguage(mInstrument.getLanguage());
                    if (mSurveyViewModel.getSurvey() != null) {
                        if (mSurveyViewModel.getSurvey().getInstrumentTitle() == null) {
                            mSurveyViewModel.getSurvey().setInstrumentTitle(mInstrument.getTitle());
                        }
                        if (mSurveyViewModel.getSurvey().getInstrumentVersionNumber() == null) {
                            mSurveyViewModel.getSurvey().setInstrumentVersionNumber(String.valueOf(mInstrument.getVersionNumber()));
                        }
                    }
                    mSurveyViewModel.update();

                    List<Display> displayList = new ArrayList<>();
                    for (SectionRelation sectionRelation : relation.sections) {
                        List<Display> displays = new ArrayList<>();
                        for (DisplayRelation displayRelation : sectionRelation.displays) {
                            displays.add(displayRelation.display);
                        }
                        displayList.addAll(getSortedDisplays(displays));
                    }
                    Collections.sort(displayList, new Comparator<Display>() {
                        @Override
                        public int compare(Display display1, Display display2) {
                            return display1.getPosition() - display2.getPosition();
                        }
                    });
                    mSurveyViewModel.setDisplays(displayList);
                    mDisplayPagerAdapter.setDisplays(displayList);

                    List<Question> questions = new ArrayList<>();
                    for (QuestionTranslationRelation questionTranslationRelation : relation.questions) {
                        questions.add(questionTranslationRelation.question);
                    }
                    Collections.sort(questions, new Comparator<Question>() {
                        @Override
                        public int compare(Question question1, Question question2) {
                            return question1.getNumberInInstrument() - question2.getNumberInInstrument();
                        }
                    });
                    mSurveyViewModel.setQuestions(questions);

                    setActionBarTitle(mSurveyViewModel.getDisplayPosition());
                    invalidateOptionsMenu();
                }
            }
        });
    }

    private List<Display> getSortedDisplays(List<Display> displayList) {
        List<Display> displays = new ArrayList<>();
        for (Display display : displayList) {
            if (!display.isDeleted()) {
                displays.add(display);
            }
        }
        Collections.sort(displays, new Comparator<Display>() {
            @Override
            public int compare(Display display, Display display1) {
                return compareDisplays(display, display1);
            }
        });
        return displays;
    }

    private int compareDisplays(Display display1, Display display2) {
        if (display1.getInstrumentPosition() == display2.getInstrumentPosition()) {
            return display1.getTitle().compareTo(display2.getTitle());
        } else {
            return display1.getInstrumentPosition() - display2.getInstrumentPosition();
        }
    }

    private void setLanguage() {
        SettingsViewModel viewModel = ViewModelProviders.of(this).get(SettingsViewModel.class);
        viewModel.getLanguages().observe(this, new Observer<List<String>>() {
            @Override
            public void onChanged(@Nullable List<String> languages) {
                if (languages == null) return;
                initializeLanguages();
                mLanguageCodes.addAll(languages);
                invalidateOptionsMenu();
            }
        });
    }

    private void setSurveyViewModel(final String surveyUUID) {
        SurveyViewModelFactory factory = new SurveyViewModelFactory(getApplication(), surveyUUID);
        mSurveyViewModel = ViewModelProviders.of(this, factory).get(SurveyViewModel.class);
        mSurveyViewModel.getLiveDataSurvey().observe(this, new Observer<Survey>() {
            @Override
            public void onChanged(@Nullable Survey survey) {
                mSurvey = survey;
                if (survey != null && mSurveyViewModel.getSurvey() == null) {
                    mSurveyViewModel.setSurvey(mSurvey);
                    mSurveyViewModel.setSkipData();
                    mSurveyViewModel.setDisplayPosition(mSurvey.getLastDisplayPosition());
                    ArrayList<Integer> previousDisplays = new ArrayList<>();
                    if (mSurvey.getPreviousDisplays() != null) {
                        for (String str : mSurvey.getPreviousDisplays().split(COMMA)) {
                            if (!TextUtils.isEmpty(str)) previousDisplays.add(Integer.valueOf(str));
                        }
                    }
                    mSurveyViewModel.setPreviousDisplays(previousDisplays);
                    mSurveyViewModel.setSurveyLanguage();
                    mLocationManager.setSurveyViewModel(mSurveyViewModel);

                    setViewPagerPosition();
                    setActionBarTitle(mSurveyViewModel.getDisplayPosition());
                }
            }
        });
    }

    private void setSurveyRelationViewModel(String uuid) {
        SurveyRelationViewModelFactory factory = new SurveyRelationViewModelFactory(getApplication(), uuid);
        SurveyRelationViewModel viewModel = ViewModelProviders.of(this, factory).get(SurveyRelationViewModel.class);
        viewModel.getSurveyRelation().observe(this, new Observer<SurveyRelation>() {
            @Override
            public void onChanged(@Nullable SurveyRelation surveyRelation) {
                HashMap<String, Response> map = new HashMap<>();
                if (surveyRelation != null) {
                    for (Response response : surveyRelation.responses) {
                        map.put(response.getQuestionIdentifier(), response);
                    }
                }
                mSurveyViewModel.setResponses(map);
            }
        });
    }

    private void setSectionViewModel(Long instrumentId) {
        SectionViewModelFactory factory = new SectionViewModelFactory(getApplication(), instrumentId);
        SectionViewModel viewModel = ViewModelProviders.of(this, factory).get(SectionViewModel.class);
        viewModel.getSectionRelations().observe(this, new Observer<List<SectionRelation>>() {
            @Override
            public void onChanged(@Nullable List<SectionRelation> sectionRelations) {
                if (sectionRelations == null) return;
                LongSparseArray<Section> longSparseArray = new LongSparseArray<>();
                LinkedHashMap<String, List<String>> listData = new LinkedHashMap<>();
                for (SectionRelation relation : sectionRelations) {
                    longSparseArray.put(relation.section.getRemoteId(), relation.section);
                    List<String> displayTitles = new ArrayList<>();
                    for (DisplayRelation displayRelation : getSortedDisplayRelations(relation.displays)) {
                        String displayTitle = TranslationUtil.getText(displayRelation.display,
                                displayRelation.translations, mSurveyViewModel);
                        displayTitles.add(styleTextWithHtmlWhitelist(displayTitle).toString());
                        mSurveyViewModel.addDisplayTitle(displayRelation.display.getRemoteId(), styleTextWithHtmlWhitelist(displayTitle).toString());
                    }
                    String sectionTitle = TranslationUtil.getText(relation.section, relation.translations, mSurveyViewModel);
                    listData.put(styleTextWithHtmlWhitelist(sectionTitle).toString(), displayTitles);
                }
                setExtraItemLinks(listData);
                mSurveyViewModel.setSections(longSparseArray);
                mSurveyViewModel.setExpandableListData(listData);
                mSurveyViewModel.setExpandableListTitle(new ArrayList<>(listData.keySet()));
                setNavigationDrawer();
            }
        });
    }

    private List<DisplayRelation> getSortedDisplayRelations(List<DisplayRelation> displayRelations) {
        List<DisplayRelation> displays = new ArrayList<>();
        for (DisplayRelation relation : displayRelations) {
            if (!relation.display.isDeleted()) {
                displays.add(relation);
            }
        }
        Collections.sort(displays, new Comparator<DisplayRelation>() {
            @Override
            public int compare(DisplayRelation displayRelation, DisplayRelation displayRelation1) {
                return compareDisplays(displayRelation.display, displayRelation1.display);
            }
        });
        return displays;
    }

    private void setExtraItemLinks(LinkedHashMap<String, List<String>> listData) {
        List<String> notes = Collections.singletonList(getString(R.string.survey_notes));
        List<String> review = Collections.singletonList(getString(R.string.survey_review));
        listData.put(getString(R.string.survey_notes), notes);
        listData.put(getString(R.string.survey_review), review);
    }

    private void setNavigationDrawer() {
        mDrawerLayout = findViewById(R.id.drawerLayout);
        mExpandableListView = findViewById(R.id.navigation);
        int width = getResources().getDisplayMetrics().widthPixels / 3;
        ViewGroup.LayoutParams params = mExpandableListView.getLayoutParams();
        params.width = width;
        mExpandableListView.setLayoutParams(params);

        ExpandableListAdapter adapter = new NavigationDrawerAdapter(getApplicationContext(),
                mSurveyViewModel.getExpandableListTitle(), mSurveyViewModel.getExpandableListData());
        mExpandableListView.setAdapter(adapter);
        mExpandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                if (mLastPosition != -1 && groupPosition != mLastPosition) {
                    mExpandableListView.collapseGroup(mLastPosition);
                }
                mLastPosition = groupPosition;
            }
        });

        mExpandableListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int groupPosition) {
            }
        });

        mExpandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                if (groupPosition == mSurveyViewModel.getExpandableListTitle().size() - 1) {
                    beginSurveyCompletion();
                }
                if (groupPosition == mSurveyViewModel.getExpandableListTitle().size() - 2) {
                    showNotes();
                }

                String selectedItem = ((List) (mSurveyViewModel.getExpandableListData().get(mSurveyViewModel.getExpandableListTitle().get(groupPosition)))).get(childPosition).toString();
                int index = 0;
                for (Display display : mSurveyViewModel.getDisplays()) {
                    if (mSurveyViewModel.getDisplayTitle(display.getRemoteId()).equals(selectedItem)) {
                        moveToDisplay(index);
                        break;
                    }
                    index++;
                }
                mDrawerLayout.closeDrawer(GravityCompat.START);
                mExpandableListView.collapseGroup(groupPosition);
                return false;
            }
        });

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    private void showNotes() {
        Intent intent = new Intent(this, SurveyNoteActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(SurveyNoteFragment.EXTRA_SURVEY_UUID, mSurveyUUID);
        intent.putExtras(bundle);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        } else {
            startActivity(intent);
        }
    }

    private void setLanguageSelection() {
        ArrayList<String> displayLanguages = new ArrayList<>();
        for (String languageCode : mLanguageCodes) {
            if (!TextUtils.isEmpty(languageCode)) {
                displayLanguages.add(new Locale(languageCode).getDisplayLanguage());
            }
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, displayLanguages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != mLanguageCodes.indexOf(AppUtil.getSettings().getLanguage())) {
                    AppUtil.getSettings().setLanguage(mLanguageCodes.get(position));
                    mSurveyViewModel.setDeviceLanguage(mLanguageCodes.get(position));
                    if (mSurveyViewModel.getSurvey() != null) {
                        mSurveyViewModel.setSurveyLanguage();
                        mSurveyViewModel.update();
                    }
                    if (!TextUtils.isEmpty(mLanguageCodes.get(position))) {
                        LocaleManager.setNewLocale(SurveyActivity.this, mLanguageCodes.get(position));
                        saveData();
                        recreate();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        mSurveyViewModel.setDeviceLanguage(AppUtil.getSettings().getLanguage());
        mSpinner.setSelection(mLanguageCodes.indexOf(AppUtil.getSettings().getLanguage()));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(EXTRA_INSTRUMENT_ID, mInstrumentId);
        outState.putString(EXTRA_SURVEY_UUID, mSurveyUUID);
        outState.putInt(EXTRA_DISPLAY_POSITION, mSurveyViewModel.getDisplayPosition());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            assignExtras(savedInstanceState);
            int displayPosition = savedInstanceState.getInt(EXTRA_DISPLAY_POSITION);
            mSurveyViewModel.setDisplayPosition(displayPosition);
            setViewPagerPosition();
        }
    }

    private void assignExtras(Bundle savedInstanceState) {
        mInstrumentId = savedInstanceState.getLong(EXTRA_INSTRUMENT_ID);
        mSurveyUUID = savedInstanceState.getString(EXTRA_SURVEY_UUID);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.setLocale(base));
    }

    @Override
    public void onStop() {
        super.onStop();
        saveData();
        if (mLocationManager != null) mLocationManager.stopLocationUpdates();
    }

    private void saveData() {
        if (mSurveyViewModel == null || mSurveyViewModel.getSurvey() == null) return;
        mSurveyViewModel.persistSkipMaps();
        mSurveyViewModel.persistSkippedQuestions();
        mSurveyViewModel.persistPreviousDisplays();
        mSurveyViewModel.setSurveyLastUpdatedTime();
        mSurveyViewModel.setSurveyLastDisplayPosition();
        mSurveyViewModel.update();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.fragment_survey, menu);
        MenuItem item = menu.findItem(R.id.language_spinner);
        mSpinner = (Spinner) item.getActionView();
        initializeLanguages();
        setLanguageSelection();
        return true;
    }

    private void initializeLanguages() {
        if (mLanguageCodes == null) {
            mLanguageCodes = new ArrayList<>();
            mLanguageCodes.add(getString(R.string.english_iso_code));
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        int position = mViewPager.getCurrentItem();
        menu.findItem(R.id.menu_item_previous).setVisible(position != 0 && !mSurveyViewModel.getPreviousDisplays().isEmpty()).setEnabled(true);
        if (mSurveyViewModel.getDisplays() != null) {
            menu.findItem(R.id.menu_item_next).setVisible(position != mSurveyViewModel.getDisplays().size() - 1).setEnabled(true);
            menu.findItem(R.id.menu_item_finish).setVisible(position == mSurveyViewModel.getDisplays().size() - 1).setEnabled(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_item_previous:
                moveToPreviousDisplay();
                return true;
            case R.id.menu_item_next:
                moveToNextDisplay();
                return true;
            case R.id.menu_item_finish:
                beginSurveyCompletion();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void moveToPreviousDisplay() {
        int position = mSurveyViewModel.getDisplayPosition();
        if (position > 0 && position < mSurveyViewModel.getDisplays().size() && mSurveyViewModel.getPreviousDisplays().size() > 0) {
            mSurveyViewModel.setDisplayPosition(mSurveyViewModel.getPreviousDisplays().remove(mSurveyViewModel.getPreviousDisplays().size() - 1));
        } else {
            mSurveyViewModel.decrementDisplayPosition();
        }
        setViewPagerPosition();
    }

    private void setViewPagerPosition() {
        mViewPager.setCurrentItem(mSurveyViewModel.getDisplayPosition());
        invalidateOptionsMenu();
    }

    private void moveToNextDisplay() {
        mSurveyViewModel.getPreviousDisplays().add(mSurveyViewModel.getDisplayPosition());
        mSurveyViewModel.incrementDisplayPosition();
        setViewPagerPosition();
    }

    private void moveToDisplay(int position) {
        int displayPosition = mSurveyViewModel.getDisplayPosition();
        if (position > displayPosition) {
            mSurveyViewModel.getPreviousDisplays().add(displayPosition);
        }
        mSurveyViewModel.setDisplayPosition(position);
        setViewPagerPosition();
        mDrawerLayout.closeDrawer(mExpandableListView);
    }

    public void beginSurveyCompletion() {
        saveData();
        mSurveyViewModel.setQuestionsWithoutResponses();
        if (mSurveyViewModel.getQuestionsWithoutResponses().size() > 0) {
            Intent i = new Intent(this, SurveyReviewActivity.class);
            Bundle b = new Bundle();
            b.putString(SurveyReviewFragment.EXTRA_SURVEY_UUID, mSurveyUUID);
            b.putLong(SurveyReviewFragment.EXTRA_INSTRUMENT_ID, mInstrumentId);
            b.putString(SurveyReviewFragment.EXTRA_DEVICE_LANGUAGE, mSurveyViewModel.getDeviceLanguage());
            i.putExtras(b);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startActivityForResult(i, REVIEW_CODE, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
            } else {
                startActivityForResult(i, REVIEW_CODE);
            }
        } else {
            finishSurvey();
        }
    }

    private void finishSurvey() {
        mSurveyViewModel.setSurveyComplete();
        mSurveyViewModel.update();
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REVIEW_CODE) {
            Long displayId = 0L;
            if (data.getExtras() != null) {
                displayId = data.getExtras().getLong(EXTRA_DISPLAY_ID);
            }
            if (displayId.equals(-1L)) {
                finishSurvey();
            } else {
                int position = 0;
                for (Display display : mSurveyViewModel.getDisplays()) {
                    if (display.getRemoteId().equals(displayId)) {
                        mSurveyViewModel.setDisplayPosition(position);
                        setViewPagerPosition();
                    }
                    position++;
                }
            }
        }
    }

}