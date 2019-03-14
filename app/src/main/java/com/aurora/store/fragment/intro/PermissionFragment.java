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

package com.aurora.store.fragment.intro;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.aurora.store.PermissionGroup;
import com.aurora.store.R;
import com.aurora.store.activity.IntroActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PermissionFragment extends IntroBaseFragment {

    @BindView(R.id.permissions_container)
    LinearLayout container;
    @BindView(R.id.btn_next)
    Button btnNext;

    private PackageManager pm;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_intro_permission, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pm = context.getPackageManager();
        addPermissionWidgets();
        setupNext();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupNext();
    }

    private void addPermissionWidgets() {
        Map<String, PermissionGroup> permissionGroupWidgets = new HashMap<>();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] allPermissionInfo = packageInfo.requestedPermissions;
            for (String permissionName : allPermissionInfo) {
                PermissionInfo permissionInfo = getPermissionInfo(permissionName);
                if (null == permissionInfo) {
                    continue;
                }
                PermissionGroup widget;
                PermissionGroupInfo permissionGroupInfo = getPermissionGroupInfo(permissionInfo);
                if (!permissionGroupWidgets.containsKey(permissionGroupInfo.name)) {
                    widget = new PermissionGroup(getContext());
                    widget.setPermissionGroupInfo(permissionGroupInfo);
                    widget.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    permissionGroupWidgets.put(permissionGroupInfo.name, widget);
                } else {
                    widget = permissionGroupWidgets.get(permissionGroupInfo.name);
                }
                if (widget != null) {
                    widget.addPermission(permissionInfo);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        container.removeAllViews();
        List<String> permissionGroupLabels = new ArrayList<>(permissionGroupWidgets.keySet());
        Collections.sort(permissionGroupLabels);
        for (String permissionGroupLabel : permissionGroupLabels) {
            container.addView(permissionGroupWidgets.get(permissionGroupLabel));
        }
    }

    private PermissionInfo getPermissionInfo(String permissionName) {
        try {
            return pm.getPermissionInfo(permissionName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private PermissionGroupInfo getPermissionGroupInfo(PermissionInfo permissionInfo) {
        PermissionGroupInfo permissionGroupInfo;
        if (null == permissionInfo.group) {
            permissionGroupInfo = getFakePermissionGroupInfo(permissionInfo.packageName);
        } else {
            try {
                permissionGroupInfo = pm.getPermissionGroupInfo(permissionInfo.group, 0);
            } catch (PackageManager.NameNotFoundException e) {
                permissionGroupInfo = getFakePermissionGroupInfo(permissionInfo.packageName);
            }
        }
        if (permissionGroupInfo.icon == 0) {
            permissionGroupInfo.icon = R.drawable.ic_permission_android;
        }
        return permissionGroupInfo;
    }

    private PermissionGroupInfo getFakePermissionGroupInfo(String packageName) {
        PermissionGroupInfo permissionGroupInfo = new PermissionGroupInfo();
        switch (packageName) {
            case "android":
                permissionGroupInfo.icon = R.drawable.ic_permission_android;
                permissionGroupInfo.name = "android";
                break;
            case "com.google.android.gsf":
            case "com.android.vending":
                permissionGroupInfo.icon = R.drawable.ic_permission_google;
                permissionGroupInfo.name = "google";
                break;
            default:
                permissionGroupInfo.icon = R.drawable.ic_permission_unknown;
                permissionGroupInfo.name = "unknown";
                break;
        }
        return permissionGroupInfo;

    }

    private void setupNext() {
        if (isPermissionGranted())
            btnNext.setOnClickListener(forwardListener());
        else {
            btnNext.setOnClickListener(askPermListener());
        }
    }

    private View.OnClickListener forwardListener() {
        btnNext.setText(context.getString(R.string.action_next));
        return v -> {
            if (getActivity() instanceof IntroActivity)
                ((IntroActivity) getActivity()).moveForward();
        };
    }

    private View.OnClickListener askPermListener() {
        btnNext.setText(R.string.action_ask);
        return v -> {
            checkPermissions();
        };
    }

    private boolean isPermissionGranted() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkPermissions() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1337);
    }
}
