/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Aurora Store is free software: you can redistribute it and/or modify
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
 *
 *
 */

package com.aurora.store.task;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.util.ContextUtil;
import com.aurora.store.util.Log;
import com.aurora.store.util.PrefUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.content.Context.LOCATION_SERVICE;

public class GeoSpoofTask {

    private LocationManager locationManager;
    private String mocLocationProvider;
    private Location mockLocation;
    private Context context;
    private String geoLocation;
    private int position;

    public GeoSpoofTask(Context context, String geoLocation, int position) {
        this.context = context;
        this.geoLocation = geoLocation;
        this.position = position;
    }

    public boolean spoof() {
        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        mocLocationProvider = LocationManager.GPS_PROVIDER;
        mockLocation = new Location(mocLocationProvider);

        int lastValue = PrefUtil.getInteger(context, Constants.PREFERENCE_REQUESTED_LOCATION_INDEX);

        if (position == 0 && lastValue != 0) {
            PrefUtil.putInteger(context, Constants.PREFERENCE_REQUESTED_LOCATION_INDEX, 0);
            ContextUtil.runOnUiThread(() -> setMockDialog(false));
            return false;
        } else
            return mockLocation(geoLocation);
    }

    private boolean mockLocation(String mockLocation) {
        List<Address> addresses = getAddress(mockLocation);
        if (addresses.isEmpty()) {
            Log.i("Could not get Address");
            return false;
        } else {
            return spoofLocation(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
        }
    }

    private List<Address> getAddress(String geoLocation) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> AllAddress = geocoder.getFromLocationName(geoLocation, 5);
            List<Address> ValidAddress = new ArrayList<>(AllAddress.size());
            for (Address address : AllAddress) {
                if (address.hasLatitude() && address.hasLongitude()) {
                    ValidAddress.add(address);
                }
            }
            return ValidAddress;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private boolean spoofLocation(double latitude, double longitude) {
        mocLocationProvider = LocationManager.GPS_PROVIDER;
        new Criteria().setAccuracy(Criteria.ACCURACY_FINE);
        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
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
                    500);
            locationManager.setTestProviderEnabled(mocLocationProvider, true);
            mockLocation.setLatitude(latitude);
            mockLocation.setLongitude(longitude);
            mockLocation.setAltitude(mockLocation.getAltitude());
            mockLocation.setTime(System.currentTimeMillis());
            mockLocation.setAccuracy(1);
            mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            locationManager.setTestProviderLocation(mocLocationProvider, mockLocation);
            PrefUtil.putInteger(context, Constants.PREFERENCE_REQUESTED_LOCATION_INDEX, position);
            return true;
        } catch (SecurityException e) {
            ContextUtil.runOnUiThread(() -> setMockDialog(true));
            return false;
        } catch (Exception e) {
            Log.e(e.getMessage());
            return false;
        }
    }

    private void setMockDialog(boolean enable) {
        MaterialAlertDialogBuilder mBuilder = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.pref_category_spoof_location)
                .setCancelable(false)
                .setMessage(enable
                        ? R.string.pref_requested_location_enable
                        : R.string.pref_requested_location_disable)
                .setPositiveButton(enable
                        ? R.string.action_enable
                        : R.string.action_disable, (dialogInterface, i) -> {
                    context.startActivity(new Intent(
                            android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                    dialogInterface.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null);
        mBuilder.create();
        mBuilder.show();
    }
}
