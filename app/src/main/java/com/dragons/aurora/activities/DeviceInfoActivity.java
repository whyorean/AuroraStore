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

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.LinearLayout;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.R;
import com.dragons.aurora.SpoofDeviceManager;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.playstoreapiv2.PropertiesDeviceInfoProvider;
import com.dragons.custom.PropCard;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class DeviceInfoActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.incognito_fab)
    FloatingActionButton incognito_fab;
    @BindView(R.id.device_info)
    LinearLayout root;

    private String deviceName;
    private int deviceIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        ButterKnife.bind(this);
        mToolbar.setNavigationOnClickListener(v -> onBackPressed());
        onNewIntent(getIntent());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        deviceName = intent.getStringExtra(Aurora.INTENT_DEVICE_NAME);
        deviceIndex = intent.getIntExtra(Aurora.INTENT_DEVICE_INDEX, 0);
        if (TextUtils.isEmpty(deviceName)) {
            Timber.e("No device name given");
            finish();
            return;
        }

        Properties properties = new SpoofDeviceManager(this).getProperties(deviceName);
        mToolbar.setTitle(properties.getProperty("UserReadableName"));
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
        root.addView(new PropCard(this, key, value));
    }

    private void setupButtons() {
        incognito_fab = findViewById(R.id.incognito_fab);
        incognito_fab.show();
        incognito_fab.setOnClickListener(click -> showConfirmationDialog());
    }

    private boolean isDeviceDefinitionValid(String spoofDevice) {
        PropertiesDeviceInfoProvider deviceInfoProvider = new PropertiesDeviceInfoProvider();
        deviceInfoProvider.setProperties(new SpoofDeviceManager(this).getProperties(spoofDevice));
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
                        Prefs.putString(this, Aurora.PREFERENCE_DEVICE_TO_PRETEND_TO_BE, deviceName);
                        Prefs.putInteger(this, Aurora.PREFERENCE_DEVICE_TO_PRETEND_TO_BE_INDEX, deviceIndex);
                    }
                    Accountant.completeCheckout(this);
                    dialogInterface.dismiss();
                    startActivity(new Intent(this, LoginActivity.class));
                    this.finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
