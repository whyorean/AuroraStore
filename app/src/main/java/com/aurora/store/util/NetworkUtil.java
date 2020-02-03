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

package com.aurora.store.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

public class NetworkUtil {
    public static boolean isConnected(Context context) {
        if (context == null)
            return false;

        Object object = context.getSystemService(Context.CONNECTIVITY_SERVICE);
        ConnectivityManager manager = (ConnectivityManager) object;

        if (manager != null) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                NetworkCapabilities capabilities = manager.getNetworkCapabilities(manager.getActiveNetwork());
                if (capabilities != null) {
                    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)
                            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN)
                            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE);
                }
            } else {
                try {
                    NetworkInfo activeNetworkInfo = manager.getActiveNetworkInfo();
                    if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                        return true;
                    }
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }
}
