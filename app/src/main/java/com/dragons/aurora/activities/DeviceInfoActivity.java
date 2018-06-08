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

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.R;
import com.dragons.aurora.SpoofDeviceManager;
import com.dragons.aurora.Util;
import com.dragons.aurora.playstoreapiv2.PropertiesDeviceInfoProvider;
import com.dragons.aurora.view.PropCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static com.dragons.aurora.fragment.PreferenceFragment.PREFERENCE_DEVICE_TO_PRETEND_TO_BE;
import static com.dragons.aurora.fragment.PreferenceFragment.PREFERENCE_DEVICE_TO_PRETEND_TO_BE_INDEX;

public class DeviceInfoActivity extends AuroraActivity {

    public static final String INTENT_DEVICE_NAME = "INTENT_DEVICE_NAME";
    public static final String INTENT_DEVICE_INDEX = "INTENT_DEVICE_INDEX";
    private String deviceName;
    private int deviceIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.deviceinfo_activity_layout);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(getResources().getColor(R.color.semi_transparent));
        onNewIntent(getIntent());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        deviceName = intent.getStringExtra(INTENT_DEVICE_NAME);
        deviceIndex = intent.getIntExtra(INTENT_DEVICE_INDEX, 0);
        if (TextUtils.isEmpty(deviceName)) {
            Log.e(getClass().getSimpleName(), "No device name given");
            finish();
            return;
        }

        Properties properties = new SpoofDeviceManager(this).getProperties(deviceName);
        ((TextView) findViewById(R.id.aurora_title)).setText(properties.getProperty("UserReadableName"));
        List<String> keys = new ArrayList<>();
        for (Object key : properties.keySet()) {
            keys.add((String) key);
        }

        Collections.sort(keys);
        LinearLayout root = findViewById(R.id.device_info);
        for (String key : keys) {
            addCards(root, key, ((String) properties.get(key)).replace(",", ", "));
        }

        setupButtons();
    }

    private void addCards(LinearLayout root, String key, String value) {
        root.addView(new PropCard(this, key, value));
    }

    private void setupButtons() {
        ImageView toolbar_back = findViewById(R.id.toolbar_back);
        toolbar_back.setOnClickListener(click -> onBackPressed());

        FloatingActionButton incognito_fab = findViewById(R.id.incognito_fab);
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
                        Util.putString(this, PREFERENCE_DEVICE_TO_PRETEND_TO_BE, deviceName);
                        Util.putInteger(this, PREFERENCE_DEVICE_TO_PRETEND_TO_BE_INDEX, deviceIndex);
                    }
                    Util.completeCheckout(this);
                    dialogInterface.dismiss();
                    startActivity(new Intent(this, LoginActivity.class));
                    this.finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
