package com.github.gfranks.fitfam.application;

import android.app.Application;
import android.content.Context;
import android.support.v4.content.ContextCompat;

import com.crashlytics.android.Crashlytics;
import com.github.gfranks.fitfam.BuildConfig;
import com.github.gfranks.fitfam.util.Feature;
import com.github.gfranks.fitfam.R;
import com.github.gfranks.fitfam.module.Modules;
import com.github.gfranks.fitfam.notification.FFNotificationFactory;
import com.github.gfranks.fitfam.util.Utils;
import com.urbanairship.AirshipConfigOptions;
import com.urbanairship.UAirship;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;

import dagger.ObjectGraph;
import io.fabric.sdk.android.Fabric;

public class FitFamApplication extends Application {

    public static final String TAG = "FitFam";

    private ObjectGraph mObjectGraph;

    public static FitFamApplication get(Context context) {
        return (FitFamApplication) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        buildObjectGraph();

        if (Feature.CRASHLYTICS.isEnabled(this)) {
            Fabric.with(this, new Crashlytics());
        }

        UAirship.takeOff(this, new AirshipConfigOptions.Builder()
                .setDevelopmentAppKey(getResources().getString(R.string.airship_dev_app_key))
                .setDevelopmentAppSecret(getResources().getString(R.string.airship_dev_app_secret))
                .setProductionAppKey(getResources().getString(R.string.airship_prod_app_key))
                .setProductionAppSecret(getResources().getString(R.string.airship_prod_app_secret))
                .setInProduction(!BuildConfig.DEBUG)
                .setAnalyticsEnabled(false)
                .setGcmSender(getResources().getString(R.string.gcm_sender_id))
                .build(), new UAirship.OnReadyCallback() {
            @Override
            public void onAirshipReady(UAirship uAirship) {
                FFNotificationFactory notificationFactory = new FFNotificationFactory(getApplicationContext());
                notificationFactory.setColor(ContextCompat.getColor(getApplicationContext(), R.color.theme_accent));
                uAirship.getPushManager().setNotificationFactory(notificationFactory);

                uAirship.getPushManager().setUserNotificationsEnabled(true);

                if (!uAirship.getPushManager().isQuietTimeEnabled()) {
                    DateTimeFormatter formatter = Utils.getNotificationQuietTimeIntervalFormatter();
                    Date startDate = DateTime.parse("07:30 PM", formatter).toDateTime(DateTimeZone.getDefault()).toDate();
                    Date endDate = DateTime.parse("07:30 AM", formatter).toDateTime(DateTimeZone.getDefault()).toDate();
                    uAirship.getPushManager().setQuietTimeInterval(startDate, endDate);
                    uAirship.getPushManager().setQuietTimeEnabled(true);
                }

                uAirship.getInAppMessageManager().setDisplayAsapEnabled(true);
            }
        });
    }

    public void inject(Object o) {
        mObjectGraph.inject(o);
    }

    public ObjectGraph getApplicationObjectGraph() {
        return mObjectGraph;
    }

    private void buildObjectGraph() {
        mObjectGraph = ObjectGraph.create(Modules.list(this));
    }
}
