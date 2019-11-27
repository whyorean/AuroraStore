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

package com.aurora.store.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.aurora.store.R;
import com.aurora.store.adapter.ViewPagerAdapter;
import com.aurora.store.fragment.AccountsFragment;
import com.aurora.store.fragment.intro.LoginFragment;
import com.aurora.store.fragment.intro.PermissionFragment;
import com.aurora.store.fragment.intro.WelcomeFragment;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.ThemeUtil;
import com.aurora.store.view.CustomViewPager;

import butterknife.BindView;
import butterknife.ButterKnife;

public class IntroActivity extends AppCompatActivity {

    @BindView(R.id.viewpager)
    CustomViewPager viewPager;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private ActionBar actionBar;
    private ThemeUtil themeUtil = new ThemeUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        themeUtil.onCreate(this);
        setContentView(R.layout.activity_intro);
        ButterKnife.bind(this);
        setupActionbar();
        setupViewPager();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_intro, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_setting:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void setupActionbar() {
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setElevation(0f);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        themeUtil.onResume(this);
        if (Accountant.isLoggedIn(this)) {
            startActivity(new Intent(this, AuroraActivity.class));
            finish();
        }
    }

    private void setupViewPager() {
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFragment(0, new WelcomeFragment());
        viewPagerAdapter.addFragment(1, new PermissionFragment());
        viewPagerAdapter.addFragment(2, new LoginFragment());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setScroll(false);
    }

    public void moveForward() {
        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
    }
}
