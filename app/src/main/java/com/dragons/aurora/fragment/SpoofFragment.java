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

import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.ImageViewCompat;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dragons.aurora.R;
import com.dragons.aurora.SpoofDeviceManager;

import java.util.Properties;
import java.util.TimeZone;

public class SpoofFragment extends UtilFragment {

    private String deviceName;
    private ImageView spoofed;
    private Display mDisplay;
    private View v;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (v != null) {
            if ((ViewGroup) v.getParent() != null)
                ((ViewGroup) v.getParent()).removeView(v);
            return v;
        }

        v = inflater.inflate(R.layout.app_device_inc, container, false);

        deviceName = PreferenceFragment.getString(getActivity(), PreferenceFragment.PREFERENCE_DEVICE_TO_PRETEND_TO_BE);
        spoofed = (ImageView) v.findViewById(R.id.spoofed_indicator);
        mDisplay = (this).getActivity().getWindowManager().getDefaultDisplay();

        if (isSpoofed())
            drawSpoofedDevice();
        else
            drawDevice();
        return v;
    }

    public boolean isSpoofed() {
        return (deviceName.contains("device-"));
    }

    public void drawDevice() {
        ImageViewCompat.setImageTintList(spoofed, ColorStateList.valueOf((getResources().getColor(R.color.colorGreen))));
        setText(R.id.device_model, R.string.device_model, Build.MODEL, Build.DEVICE);
        setText(R.id.device_manufacturer, R.string.device_manufacturer, Build.MANUFACTURER);
        setText(R.id.device_architect, R.string.device_board, Build.BOARD);
        setText(R.id.device_timezone, R.string.device_timezone, (CharSequence) TimeZone.getDefault().getDisplayName());
        setText(R.id.device_resolution, R.string.device_res, mDisplay.getWidth(), mDisplay.getHeight());
        setText(R.id.device_api, R.string.device_api, Build.VERSION.SDK);
        setText(R.id.device_cpu, R.string.device_cpu, Build.CPU_ABI);
    }

    public void drawSpoofedDevice() {
        ImageViewCompat.setImageTintList(spoofed, ColorStateList.valueOf((getResources().getColor(R.color.colorRed))));

        Properties properties = new SpoofDeviceManager(this.getActivity()).getProperties(deviceName);
        String Model = properties.getProperty("UserReadableName");

        setText(R.id.device_model, R.string.device_model, Model.substring(0, Model.indexOf('(')), properties.getProperty("Build.DEVICE"));
        setText(R.id.device_manufacturer, R.string.device_manufacturer, properties.getProperty("Build.MANUFACTURER"));
        setText(R.id.device_architect, R.string.device_board, properties.getProperty("Build.HARDWARE"));
        setText(R.id.device_timezone, R.string.device_timezone, properties.getProperty("TimeZone"));
        setText(R.id.device_resolution, R.string.device_res, properties.getProperty("Screen.Width"), properties.getProperty("Screen.Height"));
        setText(R.id.device_api, R.string.device_api, properties.getProperty("Build.VERSION.SDK_INT"));
        String Platforms = properties.getProperty("Platforms");
        setText(R.id.device_cpu, R.string.device_cpu, Platforms.substring(0, Platforms.indexOf(',')));
    }


    protected void setText(int viewId, String text) {
        TextView textView = (TextView) v.findViewById(viewId);
        if (null != textView)
            textView.setText(text);
    }

    protected void setText(int viewId, int stringId, Object... text) {
        setText(viewId, this.getString(stringId, text));
    }
}
