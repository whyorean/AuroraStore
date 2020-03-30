package com.aurora.store.sheet.spoof;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aurora.store.R;
import com.aurora.store.manager.SpoofManager;
import com.aurora.store.sheet.BaseBottomSheet;
import com.aurora.store.util.Util;

import java.util.LinkedHashMap;
import java.util.Map;

import butterknife.ButterKnife;

public class DeviceSpoofSheet extends BaseBottomSheet {

    @Nullable
    @Override
    public View onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_exodus, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onContentViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


    }

    private Map<String, String> getDeviceKeyValueMap() {
        Map<String, String> devices = new SpoofManager(requireContext()).getDevices();
        devices = Util.sort(devices);
        Util.addToStart((LinkedHashMap<String, String>) devices,
                "",
                requireContext().getString(R.string.pref_device_to_pretend_to_be_default));
        return devices;
    }
}
