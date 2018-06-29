/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Aurora Store (a fork of Yalp Store )is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.dragons.aurora.task;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.dragons.aurora.BuildConfig;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.activities.SpoofActivity;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static android.content.Context.LOCATION_SERVICE;
import static com.dragons.aurora.fragment.PreferenceFragment.PREFERENCE_REQUESTED_LOCATION_INDEX;

public class SpoofTask extends AsyncTask<Void, Void, Void> {

    private LocationManager locationManager;
    private String mocLocationProvider;
    private Location mockLocation;
    private List<Address> addresses;
    private Context context;
    private String geoLocation;
    private Boolean mockEnabled = false;
    private int position;

    public void setPosition(int position) {
        this.position = position;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setGeoLocation(String geoLocation) {
        this.geoLocation = geoLocation;
    }

    private void init() {
        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        mocLocationProvider = LocationManager.GPS_PROVIDER;
        mockLocation = new Location(mocLocationProvider);
    }

    private void mockLocation(String mockLocation) {
        addresses = getAddress(mockLocation);
        spoofLocation(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
        setGeoLocation(addresses.get(0).getAddressLine(0));
    }

    private List<Address> getAddress(String geoLocation) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocationName(geoLocation, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return addresses;
    }

    private void spoofLocation(double latitude, double longitude) {
        mocLocationProvider = LocationManager.GPS_PROVIDER;
        new Criteria().setAccuracy(Criteria.ACCURACY_FINE);
        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            try {
                locationManager.addTestProvider(mocLocationProvider,
                        false,
                        false,
                        false,
                        false,
                        true,
                        true,
                        true,
                        0,
                        5);
                locationManager.setTestProviderEnabled(mocLocationProvider, true);
                mockLocation.setLatitude(latitude);
                mockLocation.setLongitude(longitude);
                mockLocation.setAltitude(mockLocation.getAltitude());
                mockLocation.setTime(System.currentTimeMillis());
                mockLocation.setAccuracy(1);
                mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                locationManager.setTestProviderLocation(mocLocationProvider, mockLocation);
                mockEnabled = true;
                Util.putInteger(context, PREFERENCE_REQUESTED_LOCATION_INDEX, position);
            } catch (SecurityException e) {
                Log.e(getClass().getSimpleName(), e.getMessage());
                ((SpoofActivity) context).runOnUiThread(() -> setMockDialog(true));
            }
        } else
            Log.i(getClass().getSimpleName(), "No location provider found!");
    }

    @Override
    protected void onPreExecute() {
        init();
        super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (position == 0) {
            Util.putInteger(context, PREFERENCE_REQUESTED_LOCATION_INDEX, 0);
            setMockDialog(false);
        } else
            mockLocation(geoLocation);

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if (mockEnabled)
            createNotificationChannel(geoLocation);
        super.onPostExecute(aVoid);
    }

    private void setMockDialog(boolean set) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.pref_category_spoof_location)
                .setMessage(set ? R.string.pref_requested_location_enable : R.string.pref_requested_location_disable)
                .setPositiveButton(set ? R.string.action_enable : R.string.action_disable, (dialogInterface, i) -> {
                    context.startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                    dialogInterface.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void createNotificationChannel(String myLocation) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, BuildConfig.APPLICATION_ID)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_map_marker)
                .setBadgeIconType(R.mipmap.ic_launcher_round)
                .setColorized(true)
                .setColor(ContextCompat.getColor(context, R.color.colorOrange))
                .setContentTitle("Aurora Location Spoof")
                .setContentText("Current Location : " + myLocation)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    BuildConfig.APPLICATION_ID,
                    context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            notificationManager.notify(0, mBuilder.build());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(0, mBuilder.build());
        }
    }
}
