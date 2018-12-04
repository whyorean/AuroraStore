/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
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

package com.dragons.aurora.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import com.dragons.aurora.R;
import com.dragons.aurora.fragment.ContainerFragment;
import com.dragons.aurora.model.App;

import timber.log.Timber;

public class AuroraActivity extends BaseActivity {

    public static App app;
    public static boolean showUpdates;

    private ContainerFragment containerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        else
            getWindow().setStatusBarColor(getResources().getColor(R.color.semi_transparent));

        setContentView(R.layout.activity_main);

        Intent mIntent = getIntent();
        if (mIntent != null && mIntent.getAction() != null)
            showUpdates = (mIntent.getAction().equals(Intent.ACTION_VIEW));

        if (savedInstanceState == null)
            init();
        else
            containerFragment = (ContainerFragment) getSupportFragmentManager().getFragments().get(0);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }

    @Override
    public void onBackPressed() {
        if (!containerFragment.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        if (null == receiver) {
            return;
        }
        try {
            super.unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            Timber.e(e.getMessage());
        }
    }

    private void init() {
        containerFragment = new ContainerFragment();
        final FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, containerFragment)
                .commit();
    }
}
