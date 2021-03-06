package com.github.gfranks.fitfam.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SearchView;

import com.github.gfranks.fitfam.activity.FragmentActivity;
import com.github.gfranks.fitfam.adapter.SearchSuggestionsAdapter;
import com.github.gfranks.fitfam.data.model.FFCompanionFilters;
import com.github.gfranks.fitfam.data.model.FFGym;
import com.github.gfranks.fitfam.data.model.FFUser;
import com.github.gfranks.fitfam.dialog.SelectGymDialog;
import com.github.gfranks.fitfam.fragment.base.BaseFragment;
import com.github.gfranks.minimal.notification.GFMinimalNotification;
import com.github.gfranks.fitfam.R;
import com.github.gfranks.fitfam.data.model.FFErrorResponse;
import com.github.gfranks.fitfam.data.model.FFLocation;
import com.github.gfranks.fitfam.data.model.FFLocations;
import com.github.gfranks.fitfam.manager.FilterManager;
import com.github.gfranks.fitfam.manager.GoogleApiManager;

import java.util.List;

import javax.inject.Inject;

import butterknife.InjectView;
import butterknife.OnClick;

public class CompanionFiltersFragment extends BaseFragment implements SearchView.OnQueryTextListener,
        SearchView.OnSuggestionListener, CompoundButton.OnCheckedChangeListener, AgeSelectFragment.OnAgeChangedListener,
        WeightSelectFragment.OnWeightChangeListener, ExerciseTypeFragment.OnExercisesChangedListener {

    public static final String TAG = "companion_filters_fragment";

    @Inject
    FilterManager mFilterManager;
    @Inject
    GoogleApiManager mGoogleApiManager;

    @InjectView(R.id.filter_location)
    SearchView mSearchView;
    @InjectView(R.id.filter_male)
    CheckBox mMale;
    @InjectView(R.id.filter_female)
    CheckBox mFemale;
    @InjectView(R.id.filter_gym)
    Button mGym;

    private FFCompanionFilters mFilterOptions;
    private boolean mFiltersChanged;
    private SearchSuggestionsAdapter mSearchViewAdapter;

    public static CompanionFiltersFragment newInstance() {
        return new CompanionFiltersFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mSearchViewAdapter = new SearchSuggestionsAdapter(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_companion_filters, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initFilterOptions();
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnSuggestionListener(this);
        mSearchView.setSuggestionsAdapter(mSearchViewAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.nav_companion_filters);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_companion_filters, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_save).setVisible(mFiltersChanged);
        menu.findItem(R.id.action_cancel).setVisible(mFiltersChanged);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                save();
                break;
            case R.id.action_cancel:
                initFilterOptions();
                setFiltersChanged(false);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * ******************************
     * SearchView.OnQueryTextListener
     * ******************************
     */
    @Override
    public boolean onQueryTextSubmit(String query) {
        mGoogleApiManager.getLocationFromQuery(query, new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case GoogleApiManager.STATUS_SUCCESS:
                        FFLocation location = msg.getData().getParcelable(FFLocation.EXTRA);
                        mSearchView.setQuery(location.getFormatted_address(), false);
                        mFilterOptions.setLocation(location);
                        setFiltersChanged(true);
                        break;
                    case GoogleApiManager.STATUS_FAILURE:
                        if (!isDetached() && getActivity() != null) {
                            Throwable t = msg.getData().getParcelable(FFErrorResponse.EXTRA);
                            GFMinimalNotification.make(getView(), t.getMessage(), GFMinimalNotification.LENGTH_LONG, GFMinimalNotification.TYPE_ERROR).show();
                        }
                        break;
                }
                return false;
            }
        }));
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (query.length() >= 3) {
            mGoogleApiManager.getLocationsFromQuery(query, new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    switch (msg.what) {
                        case GoogleApiManager.STATUS_SUCCESS:
                            mSearchViewAdapter.updateWithLocationResults(((FFLocations) msg.getData().getParcelable(FFLocations.EXTRA)).getResults());
                            break;
                        case GoogleApiManager.STATUS_FAILURE:
                            if (!isDetached() && getActivity() != null) {
                                Throwable t = msg.getData().getParcelable(FFErrorResponse.EXTRA);
                                GFMinimalNotification.make(getView(), t.getMessage(), GFMinimalNotification.LENGTH_LONG, GFMinimalNotification.TYPE_ERROR).show();
                            }
                            break;
                    }
                    return true;
                }
            }));
        }
        return false;
    }

    /**
     * *******************************
     * SearchView.OnSuggestionListener
     * *******************************
     */
    @Override
    public boolean onSuggestionClick(int position) {
        FFLocation location = mSearchViewAdapter.getResultItem(position);
        mSearchView.setQuery(location.getFormatted_address(), false);
        mFilterOptions.setLocation(location);
        setFiltersChanged(true);
        return false;
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    /**
     * ********************
     * View.OnClickListener
     * ********************
     */
    @OnClick({R.id.filter_gym})
    void onClick(View v) {
        switch (v.getId()) {
            case R.id.filter_gym:
                SelectGymDialog.newInstance(getContext(), false, mFilterOptions.getLocation(), new SelectGymDialog.OnGymSelectedListener() {
                    @Override
                    public void onGymSelected(SelectGymDialog dialog, FFGym gym) {
                        mFilterOptions.setGym(gym);
                        mGym.setText(gym.getName());
                        setFiltersChanged(true);
                    }
                }).show();
                break;
        }
    }

    /**
     * **************************************
     * CompoundButton.OnCheckedChangeListener
     * **************************************
     */
    @Override
    public void onCheckedChanged(CompoundButton button, boolean isChecked) {
        mMale.setOnCheckedChangeListener(null);
        mFemale.setOnCheckedChangeListener(null);
        if (button.getId() == mMale.getId() && isChecked) {
            mFilterOptions.setSex(FFUser.MALE);
            mFemale.setChecked(false);
            setFiltersChanged(true);
        } else if (button.getId() == mFemale.getId() && isChecked) {
            mMale.setChecked(false);
            mFilterOptions.setSex(FFUser.FEMALE);
            setFiltersChanged(true);
        } else {
            mFilterOptions.setSex(null);
        }

        mMale.setOnCheckedChangeListener(this);
        mFemale.setOnCheckedChangeListener(this);
    }

    /**
     * *************************************
     * AgeSelectFragment.OnAgeChangeListener
     * *************************************
     */
    @Override
    public void onAgeChanged(AgeSelectFragment fragment, int age) {
        mFilterOptions.setAge(age);
        setFiltersChanged(true);
    }

    /**
     * *******************************************
     * WeightSelectFragment.OnWeightChangeListener
     * *******************************************
     */
    @Override
    public void onWeightChanged(WeightSelectFragment fragment, int weight) {
        mFilterOptions.setWeight(weight);
        setFiltersChanged(true);
    }

    /**
     * ***********************************************
     * ExerciseTypeFragment.OnExercisesChangedListener
     * ***********************************************
     */
    @Override
    public void onExercisesChanged(List<String> exercises) {
        mFilterOptions.setExercises(exercises);
        setFiltersChanged(true);
    }

    private void initFilterOptions() {
        mFilterOptions = mFilterManager.getFilterOptions();

        // reset listeners so we do not receive callbacks
        mMale.setOnCheckedChangeListener(null);
        mFemale.setOnCheckedChangeListener(null);

        if (mFilterOptions.getLocation() != null) {
            mSearchView.setQuery(mFilterOptions.getLocation().getFormatted_address(), false);
        } else {
            mSearchView.setQuery("", false);
        }

        if (FFUser.MALE.equals(mFilterOptions.getSex())) {
            mMale.setChecked(true);
            mFemale.setChecked(false);
        } else if (FFUser.FEMALE.equals(mFilterOptions.getSex())) {
            mMale.setChecked(false);
            mFemale.setChecked(true);
        } else {
            mMale.setChecked(false);
            mFemale.setChecked(false);
        }

        if (mFilterOptions.getGym() != null) {
            mGym.setText(mFilterOptions.getGym().getName());
        } else {
            mGym.setText(R.string.filter_by_select);
        }

        AgeSelectFragment ageSelectFragment = (AgeSelectFragment) getChildFragmentManager().findFragmentById(R.id.filter_age_select_fragment);
        if (ageSelectFragment != null) {
            ageSelectFragment.setAge(mFilterOptions.getAge());
            if (ageSelectFragment.isDetached()) {
                getChildFragmentManager()
                        .beginTransaction()
                        .attach(ageSelectFragment)
                        .commit();
            } else if (ageSelectFragment.getView() == null) {
                getChildFragmentManager()
                        .beginTransaction()
                        .replace(R.id.filter_age_select_fragment, ageSelectFragment)
                        .commit();
            }
        } else {
            ageSelectFragment = AgeSelectFragment.newInstance(mFilterOptions.getAge(), true, this);
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.filter_age_select_fragment, ageSelectFragment)
                    .commit();
        }

        WeightSelectFragment weightSelectFragment = (WeightSelectFragment) getChildFragmentManager().findFragmentById(R.id.filter_weight_select_fragment);
        if (weightSelectFragment != null) {
            weightSelectFragment.setWeight(mFilterOptions.getWeight());
            if (weightSelectFragment.isDetached()) {
                getChildFragmentManager()
                        .beginTransaction()
                        .attach(weightSelectFragment)
                        .commit();
            } else if (weightSelectFragment.getView() == null) {
                getChildFragmentManager()
                        .beginTransaction()
                        .replace(R.id.filter_weight_select_fragment, weightSelectFragment)
                        .commit();
            }
        } else {
            weightSelectFragment = WeightSelectFragment.newInstance(mFilterOptions.getWeight(), true, this);
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.filter_weight_select_fragment, weightSelectFragment)
                    .commit();
        }

        ExerciseTypeFragment exerciseTypeFragment= (ExerciseTypeFragment) getChildFragmentManager().findFragmentById(R.id.filter_exercises_fragment);
        if (exerciseTypeFragment != null) {
            exerciseTypeFragment.setExercises(mFilterOptions.getExercises(), true);
            if (exerciseTypeFragment.isDetached()) {
                getChildFragmentManager()
                        .beginTransaction()
                        .attach(exerciseTypeFragment)
                        .commit();
            } else if (exerciseTypeFragment.getView() == null) {
                getChildFragmentManager()
                        .beginTransaction()
                        .replace(R.id.filter_exercises_fragment, exerciseTypeFragment)
                        .commit();
            }
        } else {
            exerciseTypeFragment = ExerciseTypeFragment.newInstance(mFilterOptions.getExercises(), true, true, this);
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.filter_exercises_fragment, exerciseTypeFragment)
                    .commit();
        }

        mMale.setOnCheckedChangeListener(this);
        mFemale.setOnCheckedChangeListener(this);
    }

    private void setFiltersChanged(boolean changed) {
        if (mFiltersChanged != changed) {
            mFiltersChanged = changed;
            getActivity().supportInvalidateOptionsMenu();
        }
    }

    private void save() {
        mFilterManager.setFilterOptions(mFilterOptions);
        setFiltersChanged(false);
        GFMinimalNotification notification = GFMinimalNotification.make(getView(), R.string.filters_saved, GFMinimalNotification.LENGTH_LONG);
        if (getActivity() instanceof FragmentActivity) {
            getActivity().setResult(Activity.RESULT_OK); // notify the caller activity that filters have been changed
            notification.setCallback(new GFMinimalNotification.Callback() {
                @Override
                public void onDismissed(GFMinimalNotification notification, int event) {
                    getActivity().supportFinishAfterTransition();
                }
            }).show();
        }
        notification.show();
    }
}
