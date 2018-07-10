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
import android.support.v4.app.ActivityCompat;
import android.view.View;

import com.dragons.aurora.R;
import com.dragons.aurora.adapters.ViewPagerAdapter;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.model.App;
import com.dragons.aurora.view.CustomViewPager;
import com.dragons.custom.CustomAppBar;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AuroraActivity extends BaseActivity implements View.OnClickListener {

    public static App app;
    public static int static_pos = -9;
    @BindView(R.id.view_pager)
    CustomViewPager viewPager;
    @BindView(R.id.bottom_bar)
    CustomAppBar bottm_bar;

    public static void setPosition(int item) {
        static_pos = item;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(getThemeFromPref());
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        else
            getWindow().setStatusBarColor(getResources().getColor(R.color.semi_transparent));
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        bottm_bar.setNavigationMenu(R.menu.main_menu, this);
        bottm_bar.setSecondaryMenu(R.menu.nav_menu, this);
        viewPager.setAdapter(new ViewPagerAdapter(this, getSupportFragmentManager()));
        viewPager.setOffscreenPageLimit(3);
        if (Prefs.getBoolean(this, "SWIPE_PAGES"))
            viewPager.setScroll(true);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
        if (static_pos != -9) {
            viewPager.setCurrentItem(static_pos, true);
            static_pos = -9;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (viewPager != null && viewPager.getCurrentItem() > 0) {
            viewPager.setCurrentItem(0, true);
        } else
            super.onBackPressed();
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        if (null == receiver) {
            return;
        }
        try {
            super.unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            // Ignoring
        }
    }

    @Override
    public void onClick(View v) {
        switch ((int) v.getTag()) {
            case R.id.action_home:
                viewPager.setCurrentItem(0, true);
                break;
            case R.id.action_myapps:
                viewPager.setCurrentItem(1, true);
                break;
            case R.id.action_updates:
                viewPager.setCurrentItem(2, true);
                break;
            case R.id.action_categories:
                viewPager.setCurrentItem(3, true);
                break;
            case R.id.action_search:
                viewPager.setCurrentItem(4, true);
                break;
            case R.id.action_accounts:
                startActivity(new Intent(getApplicationContext(), AccountsActivity.class));
                break;
            case R.id.action_about:
                startActivity(new Intent(getApplicationContext(), AboutActivity.class));
                break;
            case R.id.action_settings:
                startActivity(new Intent(getApplicationContext(), PreferenceActivity.class));
                break;
            case R.id.action_spoof:
                startActivity(new Intent(getApplicationContext(), SpoofActivity.class));
                break;
        }
    }
}
