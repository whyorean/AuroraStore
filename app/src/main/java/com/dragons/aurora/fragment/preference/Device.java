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

package com.dragons.aurora.fragment.preference;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.OnListPreferenceChangeListener;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.SpoofDeviceManager;
import com.dragons.aurora.Util;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.activities.DeviceInfoActivity;
import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.playstoreapiv2.PropertiesDeviceInfoProvider;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class Device extends List {

    private static final String PREFERENCE_DEVICE_DEFINITION_REQUESTED = "PREFERENCE_DEVICE_DEFINITION_REQUESTED";

    public Device(PreferenceFragment activity) {
        super(activity);
    }

    @Override
    public void draw() {
        super.draw();
        listPreference.setOnPreferenceClickListener(preference -> {
            ContextUtil.toast(
                    activity.getActivity().getApplicationContext(),
                    R.string.pref_device_to_pretend_to_be_notice,
                    PreferenceManager.getDefaultSharedPreferences(activity.getActivity()).getString(PreferenceFragment.PREFERENCE_DOWNLOAD_DIRECTORY, "")
            );
            ((AlertDialog) listPreference.getDialog()).getListView().setOnItemLongClickListener((parent, v, position, id) -> {
                if (position > 0) {
                    Intent i = new Intent(activity.getActivity(), DeviceInfoActivity.class);
                    i.putExtra(DeviceInfoActivity.INTENT_DEVICE_NAME, (String) keyValueMap.keySet().toArray()[position]);
                    activity.startActivity(i);
                }
                return false;
            });
            return false;
        });
    }

    @Override
    protected OnListPreferenceChangeListener getOnListPreferenceChangeListener() {
        OnListPreferenceChangeListener listener = new OnListPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!TextUtils.isEmpty((String) newValue) && !isDeviceDefinitionValid((String) newValue)) {
                    ContextUtil.toast(activity.getActivity().getApplicationContext(), R.string.error_invalid_device_definition);
                    return false;
                }
                showLogOutDialog();
                return super.onPreferenceChange(preference, newValue);
            }
        };
        listener.setDefaultLabel(activity.getString(R.string.pref_device_to_pretend_to_be_default));
        return listener;
    }

    @Override
    protected Map<String, String> getKeyValueMap() {
        Map<String, String> devices = new SpoofDeviceManager(activity.getActivity()).getDevices();
        devices = Util.sort(devices);
        Util.addToStart(
                (LinkedHashMap<String, String>) devices,
                "",
                activity.getString(R.string.pref_device_to_pretend_to_be_default)
        );
        return devices;
    }

    private boolean isDeviceDefinitionValid(String spoofDevice) {
        PropertiesDeviceInfoProvider deviceInfoProvider = new PropertiesDeviceInfoProvider();
        deviceInfoProvider.setProperties(new SpoofDeviceManager(activity.getActivity()).getProperties(spoofDevice));
        deviceInfoProvider.setLocaleString(Locale.getDefault().toString());
        return deviceInfoProvider.isValid();
    }

    private boolean showRequestDialog(boolean logOut) {
        PreferenceManager.getDefaultSharedPreferences(activity.getActivity())
                .edit()
                .putBoolean(PREFERENCE_DEVICE_DEFINITION_REQUESTED, true)
                .apply()
        ;
        return true;
    }

    private AlertDialog showLogOutDialog() {
        return new AlertDialog.Builder(activity.getActivity())
                .setMessage(R.string.pref_device_to_pretend_to_be_toast)
                .setTitle(R.string.dialog_title_logout)
                .setPositiveButton(android.R.string.yes, new RequestOnClickListener(activity.getActivity(), true))
                .setNegativeButton(R.string.dialog_two_factor_cancel, new RequestOnClickListener(activity.getActivity(), false))
                .show()
                ;
    }

    private void finishAll() {
        new PlayStoreApiAuthenticator(activity.getActivity().getApplicationContext()).logout();
        AuroraActivity.cascadeFinish();
        activity.getActivity().finish();
    }

    class RequestOnClickListener implements DialogInterface.OnClickListener {

        private boolean logOut;
        private boolean askedAlready;

        public RequestOnClickListener(Activity activity, boolean logOut) {
            askedAlready = PreferenceFragment.getBoolean(activity, PREFERENCE_DEVICE_DEFINITION_REQUESTED);
            this.logOut = logOut;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
            if (askedAlready) {
                if (logOut) {
                    finishAll();
                }
            } else {
                showRequestDialog(logOut);
            }
        }
    }

    class FinishingOnClickListener implements DialogInterface.OnClickListener {

        private boolean logOut;

        public FinishingOnClickListener(boolean logOut) {
            this.logOut = logOut;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
            if (logOut) {
                finishAll();
            }
        }
    }
}
