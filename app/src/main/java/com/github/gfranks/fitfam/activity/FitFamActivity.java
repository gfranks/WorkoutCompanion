package com.github.gfranks.fitfam.activity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.github.gfranks.fitfam.data.api.FitFamService;
import com.github.gfranks.fitfam.util.Feature;
import com.github.gfranks.fitfam.R;
import com.github.gfranks.fitfam.activity.base.BaseActivity;
import com.github.gfranks.fitfam.data.model.FFUser;
import com.github.gfranks.fitfam.fragment.CompanionFiltersFragment;
import com.github.gfranks.fitfam.fragment.DiscoverFragment;
import com.github.gfranks.fitfam.fragment.SettingsFragment;
import com.github.gfranks.fitfam.manager.AccountManager;
import com.github.gfranks.fitfam.util.CropCircleTransformation;
import com.squareup.picasso.Picasso;
import com.urbanairship.UAirship;

import javax.inject.Inject;

import butterknife.InjectView;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FitFamActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    @Inject
    AccountManager mAccountManager;
    @Inject
    Picasso mPicasso;
    @Inject
    FitFamService mService;

    @InjectView(R.id.nav_view)
    NavigationView mNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fit_fam);

        mNavigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHeader();

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.activity_fragment_content);
        if (fragment != null) {
            if (fragment.isDetached()) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .attach(fragment)
                        .commit();
            } else if (fragment.getView() == null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.activity_fragment_content, fragment, fragment.getTag())
                        .commit();
            }
        } else {
            fragment = DiscoverFragment.newInstance();
            replaceMainFragment(fragment, DiscoverFragment.TAG);
        }

        FFUser user = mAccountManager.getUser();
        if (Feature.CRASHLYTICS.isEnabled(this)) {
            Crashlytics.setUserIdentifier(user.getId());
            Crashlytics.setUserEmail(user.getEmail());
            Crashlytics.setUserName(user.getFullName());
        }

        checkForPushNotificationRegistration();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_workout_companion, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /**
     * ***********************************************
     * NavigationView.OnNavigationItemSelectedListener
     * ***********************************************
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_discover) {
            DiscoverFragment fragment = (DiscoverFragment) getSupportFragmentManager().findFragmentByTag(DiscoverFragment.TAG);
            if (fragment == null || getSupportFragmentManager().findFragmentById(R.id.activity_fragment_content) != fragment) {
                if (fragment == null) {
                    fragment = DiscoverFragment.newInstance();
                }
                replaceMainFragment(fragment, DiscoverFragment.TAG);
            }
        } else if (id == R.id.nav_companion_filters) {
            CompanionFiltersFragment fragment = (CompanionFiltersFragment) getSupportFragmentManager().findFragmentByTag(CompanionFiltersFragment.TAG);
            if (fragment == null || getSupportFragmentManager().findFragmentById(R.id.activity_fragment_content) != fragment) {
                if (fragment == null) {
                    fragment = CompanionFiltersFragment.newInstance();
                }
                replaceMainFragment(fragment, CompanionFiltersFragment.TAG);
            }
        } else if (id == R.id.nav_settings) {
            SettingsFragment fragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag(SettingsFragment.TAG);
            if (fragment == null || getSupportFragmentManager().findFragmentById(R.id.activity_fragment_content) != fragment) {
                if (fragment == null) {
                    fragment = SettingsFragment.newInstance();
                }
                replaceMainFragment(fragment, SettingsFragment.TAG);
            }
        } else if (id == R.id.nav_logout) {
            mAccountManager.logout();
            startActivity(new Intent(this, LoginActivity.class));
            supportFinishAfterTransition();
        }

        delayCloseDrawerLayout();
        return true;
    }

    private void replaceMainFragment(Fragment fragment, String tag) {
        getSupportFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.activity_fragment_content, fragment, tag)
                .commit();
    }

    private void updateHeader() {
        FFUser user = mAccountManager.getUser();
        ((TextView) mNavigationView.getHeaderView(0).findViewById(R.id.user_name)).setText(user.getFullName());
        ((TextView) mNavigationView.getHeaderView(0).findViewById(R.id.user_email)).setText(mAccountManager.getEmail());
        Drawable defaultImage = ContextCompat.getDrawable(this, R.drawable.ic_avatar);
        if (user.getImage() != null && !user.getImage().isEmpty()) {
            mPicasso.load(user.getImage())
                    .placeholder(defaultImage)
                    .error(defaultImage)
                    .transform(new CropCircleTransformation())
                    .into(((ImageView) mNavigationView.getHeaderView(0).findViewById(R.id.user_image)));
        } else {
            ((ImageView) mNavigationView.getHeaderView(0).findViewById(R.id.user_image)).setImageDrawable(defaultImage);
        }
        mNavigationView.getHeaderView(0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FitFamActivity.this, UserProfileActivity.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(FitFamActivity.this,
                            mNavigationView.getHeaderView(0).findViewById(R.id.user_image), getString(R.string.transition_user_image));
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }
            }
        });
    }

    private void checkForPushNotificationRegistration() {
        if (!mAccountManager.isPushNotificationsRegisteredWithApi() && UAirship.shared().getPushManager().getChannelId() != null) {
            mService.registerPush(mAccountManager.getUser().getId(), UAirship.shared().getPushManager().getChannelId()).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    mAccountManager.setPushNotificationsRegisteredWithApi(true);
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    mAccountManager.setPushNotificationsRegisteredWithApi(false);
                }
            });
        }
    }
}