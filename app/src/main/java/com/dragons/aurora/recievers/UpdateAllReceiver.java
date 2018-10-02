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

package com.dragons.aurora.recievers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import com.dragons.aurora.AuroraApplication;
import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.fragment.UpdatableAppsFragment;

public class UpdateAllReceiver extends BroadcastReceiver {

    static public final String ACTION_ALL_UPDATES_COMPLETE = "ACTION_ALL_UPDATES_COMPLETE";
    static public final String ACTION_APP_UPDATE_COMPLETE = "ACTION_APP_UPDATE_COMPLETE";

    static public final String EXTRA_PACKAGE_NAME = "EXTRA_PACKAGE_NAME";
    static public final String EXTRA_UPDATE_ACTUALLY_INSTALLED = "EXTRA_UPDATE_ACTUALLY_INSTALLED";

    private UpdatableAppsFragment fragment;

    public UpdateAllReceiver(UpdatableAppsFragment fragment) {
        this.fragment = fragment;

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ALL_UPDATES_COMPLETE);
        filter.addAction(ACTION_APP_UPDATE_COMPLETE);

        fragment.getContext().registerReceiver(this, filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ContextUtil.isAlive(fragment.getActivity()) || TextUtils.isEmpty(intent.getAction())) {
            return;
        }
        if (intent.getAction().equals(ACTION_ALL_UPDATES_COMPLETE)) {
            ((AuroraApplication) fragment.getActivity().getApplication()).setBackgroundUpdating(false);
        } else if (intent.getAction().equals(ACTION_APP_UPDATE_COMPLETE)) {
            processAppUpdate(
                    intent.getStringExtra(EXTRA_PACKAGE_NAME),
                    intent.getBooleanExtra(EXTRA_UPDATE_ACTUALLY_INSTALLED, false)
            );
        }
    }

    private void processAppUpdate(String packageName, boolean installedUpdate) {
        if (installedUpdate) {
            fragment.updatableAppsAdapter.remove(packageName);
        }
    }
}
