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
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;

import com.aurora.store.Constants;
import com.aurora.store.util.PrefUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.content.Context.LOCATION_SERVICE;

public class GeoSpoofTask {

    private LocationManager locationManager;
    private String locationProvider;
    private Location mockLocation;
    private Context context;

    public GeoSpoofTask(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        this.locationProvider = LocationManager.GPS_PROVIDER;
        this.mockLocation = new Location(locationProvider);
    }

    public boolean spoof(String geoLocation) throws Exception {
        final List<Address> addresses = getAddress(geoLocation);
        if (!addresses.isEmpty()) {
            spoofLocation(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
            PrefUtil.putString(context, Constants.PREFERENCE_SPOOF_GEOLOCATION, geoLocation);
            return true;
        } else {
            return false;
        }
    }

    private List<Address> getAddress(String geoLocation) throws Exception {
        final Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        final List<Address> AllAddress = geocoder.getFromLocationName(geoLocation, 5);
        final List<Address> ValidAddress = new ArrayList<>(AllAddress.size());
        for (Address address : AllAddress) {
            if (address.hasLatitude() && address.hasLongitude()) {
                ValidAddress.add(address);
            }
        }
        return ValidAddress;
    }

    private void spoofLocation(double latitude, double longitude) {
        mockLocation.setLatitude(latitude);
        mockLocation.setLongitude(longitude);
        mockLocation.setTime(System.currentTimeMillis());
        mockLocation.setAccuracy(Criteria.ACCURACY_HIGH);
        mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

        locationManager.addTestProvider(locationProvider,
                true,
                true,
                true,
                false,
                false,
                false,
                true,
                Criteria.POWER_MEDIUM,
                Criteria.ACCURACY_HIGH);
        locationManager.setTestProviderEnabled(locationProvider, true);
        locationManager.setTestProviderLocation(locationProvider, mockLocation);
    }
}
