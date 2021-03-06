package com.github.gfranks.fitfam.manager;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;

import com.github.gfranks.fitfam.data.model.FFGymGeometry;
import com.github.gfranks.fitfam.data.model.FFGymGeometryLocation;
import com.github.gfranks.fitfam.data.model.FFErrorResponse;
import com.github.gfranks.fitfam.data.model.FFLocation;
import com.github.gfranks.fitfam.data.model.FFLocations;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import info.metadude.android.typedpreferences.DoublePreference;

public class GoogleApiManager implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final int REQUEST_LOCATION_PERMISSION = 1;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAILURE = 2;

    private static final String KEY_LAT = "lat";
    private static final String KEY_LNG = "lng";

    private GoogleApiClient mGoogleApiClient;
    private Geocoder mGeocoder;
    private DoublePreference mLatitudePreference;
    private DoublePreference mLongitudePreference;

    private boolean mConnected;

    public GoogleApiManager(SharedPreferences prefs, Application app) {
        mLatitudePreference = new DoublePreference(prefs, KEY_LAT, 0.0);
        mLongitudePreference = new DoublePreference(prefs, KEY_LNG, 0.0);

        mGoogleApiClient = new GoogleApiClient
                .Builder(app)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
        mGeocoder = new Geocoder(app, Locale.US);
    }

    /**
     * ***********************************
     * GoogleApiClient.ConnectionCallbacks
     * ***********************************
     */
    @Override
    public void onConnected(Bundle bundle) {
        mConnected = true;
    }

    @Override
    public void onConnectionSuspended(int i) {
        mConnected = false;
    }

    /**
     * ******************************************
     * GoogleApiClient.OnConnectionFailedListener
     * ******************************************
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mConnected = false;
    }

    public boolean isConnected() {
        return mConnected;
    }

    public boolean connect(Activity activity, GoogleApiClient.ConnectionCallbacks onConnectedCallback,
                           GoogleApiClient.OnConnectionFailedListener onFailedToConnectListener) {
        if (hasLocationPermission(activity)) {
            connect(onConnectedCallback, onFailedToConnectListener);
            return true;
        } else {
            activity.requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return false;
        }
    }

    public boolean connect(Fragment fragment, GoogleApiClient.ConnectionCallbacks onConnectedCallback,
                           GoogleApiClient.OnConnectionFailedListener onFailedToConnectListener) {
        if (hasLocationPermission(fragment.getActivity())) {
            connect(onConnectedCallback, onFailedToConnectListener);
            return true;
        } else {
            fragment.requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return false;
        }
    }

    public void disconnect() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    public LatLng getLastKnownLocation() {
        if (mLatitudePreference.get() == 0.0 || mLongitudePreference.get() == 0.0) {
            LatLng latLng = getLastKnownGpsLocation();
            mLatitudePreference.set(latLng.latitude);
            mLongitudePreference.set(latLng.longitude);
            return latLng;
        } else {
            return new LatLng(mLatitudePreference.get(), mLongitudePreference.get());
        }
    }

    public LatLng getLastKnownGpsLocation() {
        LatLng latLng = new LatLng(33.744473, -84.389886);
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (location != null) {
                return new LatLng(location.getLatitude(), location.getLongitude());
            }
        }
        return latLng;
    }

    public void setLastKnownLocation(LatLng latLng) {
        mLatitudePreference.set(latLng.latitude);
        mLongitudePreference.set(latLng.longitude);
    }

    public boolean hasLocationPermission(Activity activity) {
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean setLastLocationFromQuery(String query, final Handler handler) {
        getLocationFromQuery(query, new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Message message = Message.obtain();
                message.setTarget(handler);
                message.what = msg.what;
                Bundle bundle = new Bundle();
                switch (msg.what) {
                    case STATUS_SUCCESS:
                        message.what = STATUS_SUCCESS;
                        bundle.putAll(msg.getData());
                        break;
                    case STATUS_FAILURE:
                        message.what = STATUS_FAILURE;
                        bundle.putParcelable(FFErrorResponse.EXTRA, new FFErrorResponse.Builder()
                                .setMessage("Unable to use the selected location")
                                .build());
                        break;
                }
                message.setData(bundle);
                message.sendToTarget();
                return true;
            }
        }));

        return false;
    }

    public void getLocationFromQuery(final String query, final Handler handler) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                Bundle bundle = new Bundle();
                Message message = Message.obtain();
                message.setTarget(handler);
                String bundleKey;
                Parcelable bundleExtra;
                try {
                    List<Address> addresses = restrictAddressResultsToLocale(mGeocoder.getFromLocationName(query, 10));
                    if (addresses.size() > 0) {
                        message.what = STATUS_SUCCESS;
                        bundleKey = FFLocation.EXTRA;
                        bundleExtra = getLocationFromAddress(addresses.get(0));
                    } else {
                        throw new Exception("");
                    }
                } catch (Throwable t) {
                    message.what = STATUS_FAILURE;
                    bundleKey = FFErrorResponse.EXTRA;
                    bundleExtra = new FFErrorResponse.Builder()
                            .setMessage("Unable to find locations")
                            .build();
                }

                bundle.putParcelable(bundleKey, bundleExtra);
                message.setData(bundle);
                message.sendToTarget();
            }
        };
        thread.start();
    }

    public void getLocationsFromQuery(final String query, final Handler handler) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                Bundle bundle = new Bundle();
                Message message = Message.obtain();
                message.setTarget(handler);
                try {
                    List<Address> addresses = restrictAddressResultsToLocale(mGeocoder.getFromLocationName(query, 10));

                    FFLocations locations = new FFLocations();
                    locations.setResults(getLocationArrayFromAddresses(addresses));
                    message.what = STATUS_SUCCESS;
                    bundle.putParcelable(FFLocations.EXTRA, locations);
                } catch (Throwable t) {
                    message.what = STATUS_FAILURE;
                    bundle.putParcelable(FFErrorResponse.EXTRA, new FFErrorResponse.Builder()
                            .setMessage("Unable to find locations")
                            .build());
                }

                message.setData(bundle);
                message.sendToTarget();
            }
        };
        thread.start();
    }

    private List<Address> restrictAddressResultsToLocale(List<Address> addresses) {
        List<Address> restrictedAddresses = new ArrayList<>();
        Locale locale = Locale.US;
        for (Address address : addresses) {
            if (!locale.getCountry().equals(address.getCountryCode())) {
                continue;
            }
            restrictedAddresses.add(address);
        }

        return restrictedAddresses;
    }

    private List<FFLocation> getLocationArrayFromAddresses(List<Address> addresses) {
        List<FFLocation> locations = new ArrayList<>();
        for (Address address : addresses) {
            locations.add(getLocationFromAddress(address));
        }

        return locations;
    }

    private FFLocation getLocationFromAddress(Address address) {
        FFLocation location = new FFLocation();
        StringBuilder formattedAddress = new StringBuilder();
        for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
            if (i > 0) {
                formattedAddress.append(", ");
            }
            formattedAddress.append(address.getAddressLine(i));
        }
        location.setFormatted_address(formattedAddress.toString());
        FFGymGeometry geometry = new FFGymGeometry();
        FFGymGeometryLocation geometryLocation = new FFGymGeometryLocation();
        geometryLocation.setLat(address.getLatitude());
        geometryLocation.setLng(address.getLongitude());
        geometry.setLocation(geometryLocation);
        location.setGeometry(geometry);

        return location;
    }

    private void connect(GoogleApiClient.ConnectionCallbacks onConnectedCallback,
                         GoogleApiClient.OnConnectionFailedListener onFailedToConnectListener) {
        if (mGoogleApiClient != null) {
            if (onConnectedCallback != null) {
                mGoogleApiClient.registerConnectionCallbacks(onConnectedCallback);
            }
            if (onFailedToConnectListener != null) {
                mGoogleApiClient.registerConnectionFailedListener(onFailedToConnectListener);
            }
            mGoogleApiClient.connect();
        }
    }
}