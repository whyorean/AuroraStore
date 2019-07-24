/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Split APK Installer (SAI)
 * Copyright (C) 2018, Aefyr
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

package com.aurora.store.installer;

import com.aurora.store.model.App;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.Root;

public class AppUninstallerRooted {

    private Root root;

    public AppUninstallerRooted() {
        root = new Root();
    }

    protected void uninstall(App app) {
        try {
            if (root.isTerminated() || !root.isAcquired()) {
                root = new Root();
                if (!root.isAcquired()) {
                    return;
                }
            }
            Log.d(ensureCommandSucceeded(root.exec("pm clear " + app.getPackageName())));
            Log.d(ensureCommandSucceeded(root.exec("pm uninstall " + app.getPackageName())));
        } catch (Exception e) {
            Log.w(e.getMessage());
        }
    }

    private String ensureCommandSucceeded(String result) {
        if (result == null || result.length() == 0)
            throw new RuntimeException(root.readError());
        return result;
    }
}
