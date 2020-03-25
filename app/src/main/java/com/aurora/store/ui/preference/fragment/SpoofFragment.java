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

package com.aurora.store.ui.preference.fragment;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aurora.store.AuroraApplication;
import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.manager.CategoryManager;
import com.aurora.store.manager.SpoofManager;
import com.aurora.store.notification.QuickNotification;
import com.aurora.store.task.DeviceInfoBuilder;
import com.aurora.store.task.GeoSpoofTask;
import com.aurora.store.ui.single.activity.DeviceInfoActivity;
import com.aurora.store.ui.single.activity.GenericActivity;
import com.aurora.store.ui.single.fragment.BaseFragment;
import com.aurora.store.util.Accountant;
import com.aurora.store.util.ContextUtil;
import com.aurora.store.util.Log;
import com.aurora.store.util.PrefUtil;
import com.aurora.store.util.Util;
import com.aurora.store.util.ViewUtil;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SpoofFragment extends BaseFragment {

    @BindView(R.id.device_avatar)
    ImageView imgDeviceAvatar;
    @BindView(R.id.device_model)
    TextView txtDeviceModel;
    @BindView(R.id.device_info)
    TextView txtDeviceInfo;
    @BindView(R.id.spoof_device)
    Spinner spinnerDevice;
    @BindView(R.id.spoof_language)
    Spinner spinnerLanguage;
    @BindView(R.id.spoof_location)
    Spinner spinnerLocation;
    @BindView(R.id.export_fab)
    ExtendedFloatingActionButton exportFab;

    private String deviceName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_spoof, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (isSpoofed())
            drawSpoofedDevice();
        else
            drawDevice();

        setupDevice();
        setupLanguage();
        setupLocations();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isSpoofed())
            drawSpoofedDevice();
        else
            drawDevice();
    }

    @OnClick(R.id.export_fab)
    public void exportConfig() {
        Observable.fromCallable(() -> new DeviceInfoBuilder(requireContext())
                .build())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(success -> {
                    ContextUtil.toast(requireContext(), success
                            ? R.string.action_export_info
                            : R.string.action_export_info_failed);
                })
                .doOnError(throwable -> Log.e(throwable.getMessage()))
                .subscribe();
    }

    private boolean isSpoofed() {
        deviceName = PrefUtil.getString(requireContext(), Constants.PREFERENCE_DEVICE_TO_PRETEND_TO_BE);
        return (deviceName.contains("device-"));
    }

    private void drawDevice() {
        txtDeviceModel.setText(new StringBuilder()
                .append(Build.MODEL)
                .append(" | ")
                .append(Build.DEVICE));

        txtDeviceInfo.setText(new StringBuilder()
                .append(Build.MANUFACTURER)
                .append(" | ")
                .append(Build.BOARD));
    }

    private void drawSpoofedDevice() {
        Properties properties = new SpoofManager(this.requireContext()).getProperties(deviceName);
        String Model = properties.getProperty("UserReadableName");
        txtDeviceModel.setText(new StringBuilder()
                .append(Model.substring(0, Model.indexOf('(')))
                .append(" | ")
                .append(properties.getProperty(Constants.BUILD_DEVICE)));

        txtDeviceInfo.setText(new StringBuilder()
                .append(properties.getProperty(Constants.BUILD_MANUFACTURER))
                .append(" | ")
                .append(properties.getProperty(Constants.BUILD_HARDWARE)));
    }

    private void setupDevice() {
        Map<String, String> devices = getDeviceKeyValueMap();

        String[] deviceList = devices.values().toArray(new String[0]);
        String[] deviceKeys = devices.keySet().toArray(new String[0]);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                deviceList
        );

        adapter.setDropDownViewResource(R.layout.item_spinner);
        spinnerDevice.setAdapter(adapter);
        spinnerDevice.setSelection(PrefUtil.getInteger(requireContext(), Constants.PREFERENCE_DEVICE_TO_PRETEND_TO_BE_INDEX), true);
        spinnerDevice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    Intent i = new Intent(requireContext(), DeviceInfoActivity.class);
                    i.putExtra(Constants.INTENT_DEVICE_NAME, deviceKeys[position]);
                    i.putExtra(Constants.INTENT_DEVICE_INDEX, position);
                    requireContext().startActivity(i);
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

    private void setupLanguage() {
        Map<String, String> locales = getLanguageKeyValueMap();
        String[] localeList = locales.values().toArray(new String[0]);
        String[] localeKeys = locales.keySet().toArray(new String[0]);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                localeList
        );

        adapter.setDropDownViewResource(R.layout.item_spinner);
        spinnerLanguage.setAdapter(adapter);
        spinnerLanguage.setSelection(PrefUtil.getInteger(requireContext(),
                Constants.PREFERENCE_REQUESTED_LANGUAGE_INDEX), true);
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    try {
                        GooglePlayAPI api = AuroraApplication.api;
                        api.setLocale(new Locale(localeKeys[position]));
                        PrefUtil.putString(requireContext(), Constants.PREFERENCE_REQUESTED_LANGUAGE,
                                localeKeys[position]);
                        PrefUtil.putInteger(requireContext(), Constants.PREFERENCE_REQUESTED_LANGUAGE_INDEX,
                                position);
                    } catch (Exception e) {
                        Log.e(e.getMessage());
                    }
                }

                if (position == 0) {
                    PrefUtil.putString(requireContext(), Constants.PREFERENCE_REQUESTED_LANGUAGE, "");
                    PrefUtil.putInteger(requireContext(), Constants.PREFERENCE_REQUESTED_LANGUAGE_INDEX,
                            0);
                }
                CategoryManager.clear(requireContext());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setupLocations() {
        String[] geoLocations = requireContext().getResources().getStringArray(R.array.geoLocation);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                geoLocations);
        adapter.setDropDownViewResource(R.layout.item_spinner);
        spinnerLocation.setAdapter(adapter);
        spinnerLocation.setSelection(PrefUtil.getInteger(requireContext(),
                Constants.PREFERENCE_REQUESTED_LOCATION_INDEX), true);
        spinnerLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final String mLocation = geoLocations[position];
                GeoSpoofTask mGeoSpoofTask = new GeoSpoofTask(requireContext(), mLocation, position);
                Observable.fromCallable(mGeoSpoofTask::spoof)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(result -> {
                            if (result) {
                                QuickNotification.show(
                                        requireContext(),
                                        "Aurora Location Spoof",
                                        "Current Location : " + mLocation,
                                        null);
                                Util.clearCache(requireContext());
                            }
                        })
                        .doOnError(throwable -> Log.e(throwable.getMessage()))
                        .subscribe();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private Map<String, String> getDeviceKeyValueMap() {
        Map<String, String> devices = new SpoofManager(requireContext()).getDevices();
        devices = Util.sort(devices);
        Util.addToStart((LinkedHashMap<String, String>) devices,
                "",
                requireContext().getString(R.string.pref_device_to_pretend_to_be_default));
        return devices;
    }

    private Map<String, String> getLanguageKeyValueMap() {
        Map<String, String> languages = new HashMap<>();
        for (Locale locale : Locale.getAvailableLocales()) {
            languages.put(locale.toString(), locale.getDisplayName());
        }
        languages = Util.sort(languages);
        Util.addToStart((LinkedHashMap<String, String>) languages, "",
                requireContext().getString(R.string.pref_requested_language_default));
        return languages;
    }

    private void showConfirmationDialog() {
        MaterialAlertDialogBuilder mBuilder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_title_logout))
                .setMessage(getString(R.string.pref_device_to_pretend_to_be_toast))
                .setPositiveButton(getString(R.string.action_logout), (dialog, which) -> {
                    PrefUtil.putString(requireContext(),
                            Constants.PREFERENCE_DEVICE_TO_PRETEND_TO_BE, "");
                    PrefUtil.putInteger(requireContext(),
                            Constants.PREFERENCE_DEVICE_TO_PRETEND_TO_BE_INDEX, 0);
                    Accountant.completeCheckout(requireContext());
                    Util.clearCache(requireContext());
                    Intent intent = new Intent(requireContext(), GenericActivity.class);
                    intent.putExtra(Constants.FRAGMENT_NAME, Constants.FRAGMENT_ACCOUNTS);
                    startActivity(intent, ViewUtil.getEmptyActivityBundle((AppCompatActivity) requireContext()));
                })
                .setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> {

                });
        mBuilder.create();
        mBuilder.show();
    }
}
