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
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

import com.aurora.store.R;
import com.aurora.store.fragment.AccountsFragment;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.ThemeUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AccountsActivity extends AppCompatActivity {

    private static final String URL_TOS = "https://www.google.com/mobile/android/market-tos.html";
    private static final String URL_LICENSE = "https://gitlab.com/AuroraOSS/AuroraStore/raw/master/LICENSE";
    private static final String URL_DISCLAIMER = "https://gitlab.com/AuroraOSS/AuroraStore/raw/master/DISCLAIMER";

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    private ActionBar mActionBar;
    private ThemeUtil mThemeUtil = new ThemeUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mThemeUtil.onCreate(this);
        setContentView(R.layout.activity_accounts);
        ButterKnife.bind(this);
        setupActionbar();
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_accounts, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_setting:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_terms:
                openWebView(URL_TOS);
                return true;
            case R.id.action_disclaimer:
                openWebView(URL_DISCLAIMER);
                return true;
            case R.id.action_license:
                openWebView(URL_LICENSE);
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mThemeUtil.onResume(this);
    }

    private void setupActionbar() {
        setSupportActionBar(mToolbar);
        mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setDisplayShowCustomEnabled(true);
            mActionBar.setElevation(0f);
        }
    }

    private void init() {
        AccountsFragment fragment = new AccountsFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commitAllowingStateLoss();
    }

    private void openWebView(String URL){
        try{
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
        }catch (Exception e){
            Log.e("No WebView found !");
        }
    }
}
