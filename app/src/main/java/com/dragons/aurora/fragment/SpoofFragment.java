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

package com.dragons.aurora.fragment;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.dragons.aurora.Aurora;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.SpoofDeviceManager;
import com.dragons.aurora.Util;
import com.dragons.aurora.activities.DeviceInfoActivity;
import com.dragons.aurora.activities.LoginActivity;
import com.dragons.aurora.dialogs.GenericDialog;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.task.SpoofTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class SpoofFragment extends BaseFragment {

    public static final String LineageURl = "https://wiki.lineageos.org/images/devices/";

    private String deviceName;
    private View view;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.fragment_spoof, container, false);
        ScrollView disclaimer = view.findViewById(R.id.disclaimer);
        ImageView showLessMore = view.findViewById(R.id.show_LessMore);
        showLessMore.setOnClickListener(v -> {
            if (disclaimer.getVisibility() == View.GONE) {
                disclaimer.setVisibility(View.VISIBLE);
                showLessMore.animate().rotation(180).start();
            } else {
                disclaimer.setVisibility(View.GONE);
                showLessMore.animate().rotation(0).start();
            }
        });

        if (isSpoofed())
            drawSpoofedDevice();
        else
            drawDevice();

        setupLanguage();
        setupDevice();
        setupLocations();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isSpoofed())
            drawSpoofedDevice();
        else
            drawDevice();
    }

    private boolean isSpoofed() {
        deviceName = PreferenceFragment.getString(getActivity(), Aurora.PREFERENCE_DEVICE_TO_PRETEND_TO_BE);
        return (deviceName.contains("device-"));
    }

    private void drawDevice() {
        getDeviceImg(LineageURl + Build.DEVICE + ".png");
        Util.setText(view, R.id.device_model, R.string.device_model, Build.MODEL, Build.DEVICE);
        Util.setText(view, R.id.device_manufacturer, Build.MANUFACTURER);
        Util.setText(view, R.id.device_architect, Build.BOARD);
    }

    private void drawSpoofedDevice() {
        Properties properties = new SpoofDeviceManager(this.getActivity()).getProperties(deviceName);
        String Model = properties.getProperty("UserReadableName");
        getDeviceImg(LineageURl + properties.getProperty(Aurora.BUILD_DEVICE) + ".png");
        Util.setText(view, R.id.device_model, R.string.device_model, Model.substring(0, Model.indexOf('(')), properties.getProperty(Aurora.BUILD_DEVICE));
        Util.setText(view, R.id.device_manufacturer, properties.getProperty(Aurora.BUILD_MANUFACTURER));
        Util.setText(view, R.id.device_architect, properties.getProperty(Aurora.BUILD_HARDWARE));
    }

    private void setupLanguage() {
        Spinner spinner = (Spinner) view.findViewById(R.id.spoof_language);
        Map<String, String> locales = getLanguageKeyValueMap();
        String[] localeList = locales.values().toArray(new String[0]);
        String[] localeKeys = locales.keySet().toArray(new String[0]);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                localeList
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(Prefs.getInteger(getContext(), Aurora.PREFERENCE_REQUESTED_LANGUAGE_INDEX), true);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    try {
                        new PlayStoreApiAuthenticator(getActivity()).getApi().setLocale(new Locale(localeKeys[position]));
                        Prefs.putString(getContext(), Aurora.PREFERENCE_REQUESTED_LANGUAGE, localeKeys[position]);
                        Prefs.putInteger(getContext(), Aurora.PREFERENCE_REQUESTED_LANGUAGE_INDEX, position);
                    } catch (IOException e) {
                        // Should be impossible to get to preferences with incorrect credentials
                    }
                }

                if (position == 0) {
                    Prefs.putString(getContext(), Aurora.PREFERENCE_REQUESTED_LANGUAGE, "");
                    Prefs.putInteger(getContext(), Aurora.PREFERENCE_REQUESTED_LANGUAGE_INDEX, 0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setupDevice() {
        Spinner spinner = view.findViewById(R.id.spoof_device);
        Map<String, String> devices = getDeviceKeyValueMap();

        String[] deviceList = devices.values().toArray(new String[0]);
        String[] deviceKeys = devices.keySet().toArray(new String[0]);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                deviceList
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(Prefs.getInteger(getContext(), Aurora.PREFERENCE_DEVICE_TO_PRETEND_TO_BE_INDEX), true);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    Intent i = new Intent(getContext(), DeviceInfoActivity.class);
                    i.putExtra(Aurora.INTENT_DEVICE_NAME, deviceKeys[position]);
                    i.putExtra(Aurora.INTENT_DEVICE_INDEX, position);
                    getContext().startActivity(i);
                }
                if (position == 0) {
                    showConfirmationDialog();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setupLocations() {
        Spinner spinner = view.findViewById(R.id.spoof_location);
        String geoLocations[] = getContext().getResources().getStringArray(R.array.geoLocation);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                geoLocations
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(Prefs.getInteger(getContext(), Aurora.PREFERENCE_REQUESTED_LOCATION_INDEX), true);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SpoofTask spoofTask = new SpoofTask();
                spoofTask.setContext(getContext());
                spoofTask.setGeoLocation(geoLocations[position]);
                spoofTask.setPosition(position);
                spoofTask.execute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void getDeviceImg(String url) {
        Glide
                .with(getContext())
                .load(url)
                .apply(new RequestOptions().placeholder(ContextCompat.getDrawable(getContext(), R.drawable.ic_device)))
                .into((ImageView) view.findViewById(R.id.device_avatar));
    }

    private Map<String, String> getDeviceKeyValueMap() {
        Map<String, String> devices = new SpoofDeviceManager(getContext()).getDevices();
        devices = Util.sort(devices);
        Util.addToStart(
                (LinkedHashMap<String, String>) devices,
                "",
                view.getContext().getString(R.string.pref_device_to_pretend_to_be_default)
        );
        return devices;
    }

    private Map<String, String> getLanguageKeyValueMap() {
        Map<String, String> languages = new HashMap<>();
        for (Locale locale : Locale.getAvailableLocales()) {
            String displayName = locale.getDisplayName();
            displayName = displayName.substring(0, 1).toUpperCase(Locale.getDefault()) + displayName.substring(1);
            languages.put(locale.toString(), displayName);
        }
        languages = Util.sort(languages);
        Util.addToStart((LinkedHashMap<String, String>) languages, "",
                getActivity().getString(R.string.pref_requested_language_default));
        return languages;
    }

    private void showConfirmationDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        GenericDialog mDialog = new GenericDialog();
        mDialog.setDialogTitle(getString(R.string.dialog_title_logout));
        mDialog.setDialogMessage(getString(R.string.pref_device_to_pretend_to_be_toast));
        mDialog.setPositiveButton(getString(R.string.action_logout), v -> {
            Prefs.putString(getContext(), Aurora.PREFERENCE_DEVICE_TO_PRETEND_TO_BE, "");
            Prefs.putInteger(getContext(), Aurora.PREFERENCE_DEVICE_TO_PRETEND_TO_BE_INDEX, 0);
            Accountant.completeCheckout(getContext());
            mDialog.dismiss();
            startActivity(new Intent(getContext(), LoginActivity.class));
        });
        mDialog.setNegativeButton(getString(android.R.string.cancel), v -> {
            mDialog.dismiss();
        });
        mDialog.show(ft, "dialog");
    }

}
