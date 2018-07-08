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

import android.app.AlertDialog;
import android.content.Intent;
import android.location.Address;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.SpoofDeviceManager;
import com.dragons.aurora.Util;
import com.dragons.aurora.activities.DeviceInfoActivity;
import com.dragons.aurora.activities.LoginActivity;
import com.dragons.aurora.task.SpoofTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static com.dragons.aurora.fragment.PreferenceFragment.PREFERENCE_DEVICE_TO_PRETEND_TO_BE;
import static com.dragons.aurora.fragment.PreferenceFragment.PREFERENCE_DEVICE_TO_PRETEND_TO_BE_INDEX;
import static com.dragons.aurora.fragment.PreferenceFragment.PREFERENCE_REQUESTED_LANGUAGE;
import static com.dragons.aurora.fragment.PreferenceFragment.PREFERENCE_REQUESTED_LANGUAGE_INDEX;
import static com.dragons.aurora.fragment.PreferenceFragment.PREFERENCE_REQUESTED_LOCATION_INDEX;

public class SpoofFragment extends AccountsHelper {

    static String LineageURl = "https://wiki.lineageos.org/images/devices/";

    private String deviceName;
    private View view;
    private List<Address> addresses;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view != null) {
            if ((ViewGroup) view.getParent() != null)
                ((ViewGroup) view.getParent()).removeView(view);
            return view;
        }

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

    public boolean isSpoofed() {
        deviceName = PreferenceFragment.getString(getActivity(), PreferenceFragment.PREFERENCE_DEVICE_TO_PRETEND_TO_BE);
        return (deviceName.contains("device-"));
    }

    public void drawDevice() {
        Picasso
                .with(getContext())
                .load(LineageURl + Build.DEVICE + ".png")
                .placeholder(ContextCompat.getDrawable(getContext(), R.drawable.ic_device))
                .into((ImageView) view.findViewById(R.id.device_avatar));

        setText(R.id.device_model, R.string.device_model, Build.MODEL, Build.DEVICE);
        setText(R.id.device_manufacturer, Build.MANUFACTURER);
        setText(R.id.device_architect, Build.BOARD);
    }

    public void drawSpoofedDevice() {
        Properties properties = new SpoofDeviceManager(this.getActivity()).getProperties(deviceName);
        String Model = properties.getProperty("UserReadableName");

        Picasso
                .with(getContext())
                .load(LineageURl + properties.getProperty("Build.DEVICE") + ".png")
                .placeholder(ContextCompat.getDrawable(getContext(), R.drawable.ic_device))
                .into((ImageView) view.findViewById(R.id.device_avatar));

        setText(R.id.device_model, R.string.device_model, Model.substring(0, Model.indexOf('(')), properties.getProperty("Build.DEVICE"));
        setText(R.id.device_manufacturer, properties.getProperty("Build.MANUFACTURER"));
        setText(R.id.device_architect, properties.getProperty("Build.HARDWARE"));
    }

    void setupLanguage() {
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
        spinner.setSelection(Util.getInteger(getContext(), PREFERENCE_REQUESTED_LANGUAGE_INDEX), true);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    try {
                        new PlayStoreApiAuthenticator(getActivity()).getApi().setLocale(new Locale(localeKeys[position]));
                        Util.putString(getContext(), PREFERENCE_REQUESTED_LANGUAGE, localeKeys[position]);
                        Util.putInteger(getContext(), PREFERENCE_REQUESTED_LANGUAGE_INDEX, position);
                    } catch (IOException e) {
                        // Should be impossible to get to preferences with incorrect credentials
                    }
                }

                if (position == 0) {
                    Util.putString(getContext(), PREFERENCE_REQUESTED_LANGUAGE, "");
                    Util.putInteger(getContext(), PREFERENCE_REQUESTED_LANGUAGE_INDEX, 0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    void setupDevice() {
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
        spinner.setSelection(Util.getInteger(getContext(), PREFERENCE_DEVICE_TO_PRETEND_TO_BE_INDEX), true);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    Intent i = new Intent(getContext(), DeviceInfoActivity.class);
                    i.putExtra(DeviceInfoActivity.INTENT_DEVICE_NAME, deviceKeys[position]);
                    i.putExtra(DeviceInfoActivity.INTENT_DEVICE_INDEX, position);
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

    void setupLocations() {
        Spinner spinner = view.findViewById(R.id.spoof_location);
        String geoLocations[] = getContext().getResources().getStringArray(R.array.geoLocation);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                geoLocations
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(Util.getInteger(getContext(), PREFERENCE_REQUESTED_LOCATION_INDEX), true);
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

    protected void setText(int viewId, String text) {
        TextView textView = (TextView) view.findViewById(viewId);
        if (null != textView)
            textView.setText(text);
    }

    protected void setText(int viewId, int stringId, Object... text) {
        setText(viewId, this.getString(stringId, text));
    }

    protected Map<String, String> getDeviceKeyValueMap() {
        Map<String, String> devices = new SpoofDeviceManager(getContext()).getDevices();
        devices = Util.sort(devices);
        Util.addToStart(
                (LinkedHashMap<String, String>) devices,
                "",
                view.getContext().getString(R.string.pref_device_to_pretend_to_be_default)
        );
        return devices;
    }

    protected Map<String, String> getLanguageKeyValueMap() {
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
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.pref_device_to_pretend_to_be_toast)
                .setTitle(R.string.dialog_title_logout)
                .setPositiveButton(R.string.action_logout, (dialogInterface, i) -> {
                    Util.putString(getContext(), PREFERENCE_DEVICE_TO_PRETEND_TO_BE, "");
                    Util.putInteger(getContext(), PREFERENCE_DEVICE_TO_PRETEND_TO_BE_INDEX, 0);
                    Util.completeCheckout(getContext());
                    dialogInterface.dismiss();
                    startActivity(new Intent(getContext(), LoginActivity.class));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

}
