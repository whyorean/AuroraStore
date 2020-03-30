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

package com.aurora.store.ui.single.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.LinearLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.manager.SpoofManager;
import com.aurora.store.ui.accounts.AccountsActivity;
import com.aurora.store.ui.view.PropertyView;
import com.aurora.store.util.Accountant;
import com.aurora.store.util.ContextUtil;
import com.aurora.store.util.Log;
import com.aurora.store.util.PrefUtil;
import com.dragons.aurora.playstoreapiv2.PropertiesDeviceInfoProvider;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DeviceInfoActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.incognito_fab)
    ExtendedFloatingActionButton incognito_fab;
    @BindView(R.id.device_info)
    LinearLayout root;

    private String deviceName;
    private int deviceIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        onNewIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        deviceName = intent.getStringExtra(Constants.INTENT_DEVICE_NAME);
        deviceIndex = intent.getIntExtra(Constants.INTENT_DEVICE_INDEX, 0);
        if (TextUtils.isEmpty(deviceName)) {
            Log.d("No device name given");
            finish();
            return;
        }

        Properties properties = new SpoofManager(this).getProperties(deviceName);
        toolbar.setTitle(properties.getProperty("UserReadableName"));
        List<String> keys = new ArrayList<>();
        for (Object key : properties.keySet()) {
            keys.add((String) key);
        }

        Collections.sort(keys);
        for (String key : keys) {
            addCards(root, key, ((String) properties.get(key)).replace(",", ", "));
        }

        setupButtons();
    }

    private void addCards(LinearLayout root, String key, String value) {
        root.addView(new PropertyView(this, key, value));
    }

    private void setupButtons() {
        incognito_fab = findViewById(R.id.incognito_fab);
        incognito_fab.show();
        incognito_fab.setOnClickListener(click -> showConfirmationDialog());
    }

    private boolean isDeviceDefinitionValid(String deviceName) {
        final PropertiesDeviceInfoProvider deviceInfoProvider = new PropertiesDeviceInfoProvider();
        deviceInfoProvider.setProperties(new SpoofManager(this).getProperties(deviceName));
        deviceInfoProvider.setLocaleString(Locale.getDefault().toString());
        return deviceInfoProvider.isValid();
    }

    private void showConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.pref_device_to_pretend_to_be_toast)
                .setTitle(R.string.dialog_title_logout)
                .setPositiveButton(R.string.action_logout, (dialogInterface, i) -> {
                    if (!TextUtils.isEmpty(deviceName) && !isDeviceDefinitionValid(deviceName)) {
                        ContextUtil.toast(this, R.string.error_invalid_device_definition);
                    } else {
                        PrefUtil.putString(this, Constants.PREFERENCE_SPOOF_DEVICE, deviceName);
                        //PrefUtil.putInteger(this, Constants.PREFERENCE_DEVICE_TO_PRETEND_TO_BE_INDEX, deviceIndex);
                    }
                    Accountant.completeCheckout(this);
                    dialogInterface.dismiss();
                    startActivity(new Intent(this, AccountsActivity.class));
                    this.finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
