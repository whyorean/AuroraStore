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

package com.aurora.store.fragment;

import android.content.Context;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.aurora.store.Constants;
import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.activity.AccountsActivity;
import com.aurora.store.activity.DeviceInfoActivity;
import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.manager.CategoryManager;
import com.aurora.store.manager.SpoofManager;
import com.aurora.store.notification.QuickNotification;
import com.aurora.store.task.GeoSpoofTask;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.Util;
import com.aurora.store.utility.ViewUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class SpoofFragment extends Fragment {

    private static final String LineageURl = "https://wiki.lineageos.org/images/devices/";

    @BindView(R.id.device_avatar)
    ImageView imgDeviceAvatar;
    @BindView(R.id.spoof_device)
    Spinner mSpinnerDevice;
    @BindView(R.id.spoof_language)
    Spinner mSpinnerLanguage;
    @BindView(R.id.spoof_location)
    Spinner mSpinnerLocation;

    private Context context;
    private CompositeDisposable mDisposable = new CompositeDisposable();
    private String deviceName;
    private View view;

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.fragment_spoof, container, false);
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

    private boolean isSpoofed() {
        deviceName = PrefUtil.getString(context, Constants.PREFERENCE_DEVICE_TO_PRETEND_TO_BE);
        return (deviceName.contains("device-"));
    }

    private void drawDevice() {
        getDeviceImg(LineageURl + Build.DEVICE + ".png");
        ViewUtil.setText(view, R.id.device_model, R.string.device_model, Build.MODEL, Build.DEVICE);
        ViewUtil.setText(view, R.id.device_manufacturer, Build.MANUFACTURER);
        ViewUtil.setText(view, R.id.device_architect, Build.BOARD);
    }

    private void drawSpoofedDevice() {
        Properties properties = new SpoofManager(this.context).getProperties(deviceName);
        String Model = properties.getProperty("UserReadableName");
        getDeviceImg(LineageURl + properties.getProperty(Constants.BUILD_DEVICE) + ".png");
        ViewUtil.setText(view, R.id.device_model, R.string.device_model, Model.substring(0, Model.indexOf('(')), properties.getProperty(Constants.BUILD_DEVICE));
        ViewUtil.setText(view, R.id.device_manufacturer, properties.getProperty(Constants.BUILD_MANUFACTURER));
        ViewUtil.setText(view, R.id.device_architect, properties.getProperty(Constants.BUILD_HARDWARE));
    }

    private void setupDevice() {
        Map<String, String> devices = getDeviceKeyValueMap();

        String[] deviceList = devices.values().toArray(new String[0]);
        String[] deviceKeys = devices.keySet().toArray(new String[0]);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                deviceList
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerDevice.setAdapter(adapter);
        mSpinnerDevice.setSelection(PrefUtil.getInteger(context, Constants.PREFERENCE_DEVICE_TO_PRETEND_TO_BE_INDEX), true);
        mSpinnerDevice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    Intent i = new Intent(context, DeviceInfoActivity.class);
                    i.putExtra(Constants.INTENT_DEVICE_NAME, deviceKeys[position]);
                    i.putExtra(Constants.INTENT_DEVICE_INDEX, position);
                    context.startActivity(i);
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
                context,
                android.R.layout.simple_spinner_item,
                localeList
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerLanguage.setAdapter(adapter);
        mSpinnerLanguage.setSelection(PrefUtil.getInteger(context, Constants.PREFERENCE_REQUESTED_LANGUAGE_INDEX), true);
        mSpinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    try {
                        new PlayStoreApiAuthenticator(context).getApi().setLocale(new Locale(localeKeys[position]));
                        PrefUtil.putString(context, Constants.PREFERENCE_REQUESTED_LANGUAGE, localeKeys[position]);
                        PrefUtil.putInteger(context, Constants.PREFERENCE_REQUESTED_LANGUAGE_INDEX, position);
                    } catch (IOException e) {
                        Log.w(e.getMessage());
                        ContextUtil.runOnUiThread(() -> {
                            Toast.makeText(context, "You need to login first", Toast.LENGTH_LONG).show();
                        });
                    }
                }

                if (position == 0) {
                    PrefUtil.putString(context, Constants.PREFERENCE_REQUESTED_LANGUAGE, "");
                    PrefUtil.putInteger(context, Constants.PREFERENCE_REQUESTED_LANGUAGE_INDEX, 0);
                }

                new CategoryManager(context).clearAll();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setupLocations() {
        String[] geoLocations = context.getResources().getStringArray(R.array.geoLocation);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                geoLocations);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerLocation.setAdapter(adapter);
        mSpinnerLocation.setSelection(PrefUtil.getInteger(context, Constants.PREFERENCE_REQUESTED_LOCATION_INDEX), true);
        mSpinnerLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final String mLocation = geoLocations[position];
                GeoSpoofTask mGeoSpoofTask = new GeoSpoofTask(context, mLocation, position);
                mDisposable.add(Observable.fromCallable(mGeoSpoofTask::spoof)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((success) -> {
                            if (view != null) {
                                new QuickNotification(context).show(
                                        "Aurora Location Spoof",
                                        "Current Location : " + mLocation);
                            }
                        }, err -> {
                            Log.e(err.getMessage());
                        }));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void getDeviceImg(String url) {
        GlideApp
                .with(context)
                .load(url)
                .centerCrop()
                .placeholder(ContextCompat.getDrawable(context, R.drawable.ic_device_avatar))
                .into(imgDeviceAvatar);
    }

    private Map<String, String> getDeviceKeyValueMap() {
        Map<String, String> devices = new SpoofManager(context).getDevices();
        devices = Util.sort(devices);
        Util.addToStart((LinkedHashMap<String, String>) devices,
                "",
                context.getString(R.string.pref_device_to_pretend_to_be_default));
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
                context.getString(R.string.pref_requested_language_default));
        return languages;
    }

    private void showConfirmationDialog() {
        MaterialAlertDialogBuilder mBuilder = new MaterialAlertDialogBuilder(context)
                .setTitle(getString(R.string.dialog_title_logout))
                .setMessage(getString(R.string.pref_device_to_pretend_to_be_toast))
                .setPositiveButton(getString(R.string.action_logout), (dialog, which) -> {
                    PrefUtil.putString(context, Constants.PREFERENCE_DEVICE_TO_PRETEND_TO_BE, "");
                    PrefUtil.putInteger(context, Constants.PREFERENCE_DEVICE_TO_PRETEND_TO_BE_INDEX, 0);
                    Accountant.completeCheckout(context);
                    startActivity(new Intent(context, AccountsActivity.class));
                })
                .setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> {

                });
        mBuilder.create();
        mBuilder.show();
    }
}
