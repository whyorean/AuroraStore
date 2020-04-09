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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.manager.SpoofManager;
import com.aurora.store.task.DeviceInfoBuilder;
import com.aurora.store.ui.single.fragment.BaseFragment;
import com.aurora.store.ui.spoof.GenericSpoofActivity;
import com.aurora.store.util.ContextUtil;
import com.aurora.store.util.Log;
import com.aurora.store.util.PrefUtil;
import com.aurora.store.util.ViewUtil;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.apache.commons.lang3.StringUtils;

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
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isSpoofed())
            drawSpoofedDevice();
        else
            drawDevice();
    }

    @OnClick(R.id.card_device)
    public void openDeviceSpoof() {
        Intent intent = new Intent(requireContext(), GenericSpoofActivity.class);
        intent.putExtra(Constants.FRAGMENT_NAME, Constants.FRAGMENT_SPOOF_DEVICE);
        startActivity(intent, ViewUtil.getEmptyActivityBundle((AppCompatActivity) requireActivity()));
    }

    @OnClick(R.id.card_locale)
    public void openLocaleSpoof() {
        Intent intent = new Intent(requireContext(), GenericSpoofActivity.class);
        intent.putExtra(Constants.FRAGMENT_NAME, Constants.FRAGMENT_SPOOF_LOCALE);
        startActivity(intent, ViewUtil.getEmptyActivityBundle((AppCompatActivity) requireActivity()));
    }

    @OnClick(R.id.card_geolocation)
    public void openGeoSpoof() {
        Intent intent = new Intent(requireContext(), GenericSpoofActivity.class);
        intent.putExtra(Constants.FRAGMENT_NAME, Constants.FRAGMENT_SPOOF_GEOLOCATION);
        startActivity(intent, ViewUtil.getEmptyActivityBundle((AppCompatActivity) requireActivity()));
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
        deviceName = PrefUtil.getString(requireContext(), Constants.PREFERENCE_SPOOF_DEVICE);
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
        final Properties properties = new SpoofManager(this.requireContext()).getProperties(deviceName);

        txtDeviceModel.setText(StringUtils.joinWith(" | ",
                properties.getProperty("UserReadableName"),
                properties.getProperty(Constants.BUILD_DEVICE)));

        txtDeviceInfo.setText(StringUtils.joinWith(" | ",
                properties.getProperty(Constants.BUILD_MANUFACTURER),
                properties.getProperty(Constants.BUILD_HARDWARE)));
    }
}
