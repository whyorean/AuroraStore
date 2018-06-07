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
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.dragons.aurora.R;
import com.dragons.aurora.SpoofDeviceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class DeviceInfoActivity extends AuroraActivity {

    public static final String INTENT_DEVICE_NAME = "INTENT_DEVICE_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.deviceinfo_activity_layout);
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String deviceName = intent.getStringExtra(INTENT_DEVICE_NAME);
        if (TextUtils.isEmpty(deviceName)) {
            Log.e(getClass().getSimpleName(), "No device name given");
            finish();
            return;
        }

        Properties properties = new SpoofDeviceManager(this).getProperties(deviceName);
        setTitle(properties.getProperty("UserReadableName"));
        List<String> keys = new ArrayList<>();
        for (Object key : properties.keySet()) {
            keys.add((String) key);
        }
        Collections.sort(keys);

        TableLayout table = (TableLayout) findViewById(R.id.device_info);
        for (String key : keys) {
            addRow(table, key, ((String) properties.get(key)).replace(",", ", "));
        }
    }

    private void addRow(TableLayout parent, String key, String value) {
        TableRow.LayoutParams rowParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);

        TextView textViewKey = new TextView(this);
        textViewKey.setText(key);
        textViewKey.setLayoutParams(rowParams);

        TextView textViewValue = new TextView(this);
        textViewValue.setText(value);
        textViewValue.setLayoutParams(rowParams);

        TableRow tableRow = new TableRow(this);
        tableRow.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
        tableRow.addView(textViewKey);
        tableRow.addView(textViewValue);

        parent.addView(tableRow);
    }
}
