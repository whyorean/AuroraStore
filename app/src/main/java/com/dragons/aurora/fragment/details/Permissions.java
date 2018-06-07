package com.dragons.aurora.fragment.details;

import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dragons.aurora.R;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.fragment.widget.PermissionGroup;
import com.dragons.aurora.model.App;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Permissions extends AbstractHelper {

    private PackageManager pm;

    public Permissions(DetailsFragment fragment, App app) {
        super(fragment, app);
        pm = fragment.getActivity().getPackageManager();
    }

    @Override
    public void draw() {
        TextView viewHeader = fragment.getActivity().findViewById(R.id.permissions_header);
        LinearLayout viewContainer = fragment.getActivity().findViewById(R.id.permissions_container);

        viewHeader.setOnClickListener(v -> {
            boolean isExpanded = viewContainer.getVisibility() == View.VISIBLE;
            if (isExpanded) {
                viewContainer.setVisibility(View.GONE);
                ((TextView) v).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expand_more, 0);
            } else {
                addPermissionWidgets();
                viewContainer.setVisibility(View.VISIBLE);
                ((TextView) v).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expand_less, 0);
            }
        });
    }

    private void addPermissionWidgets() {
        Map<String, PermissionGroup> permissionGroupWidgets = new HashMap<>();
        for (String permissionName : app.getPermissions()) {
            PermissionInfo permissionInfo = getPermissionInfo(permissionName);
            if (null == permissionInfo) {
                continue;
            }
            PermissionGroup widget;
            PermissionGroupInfo permissionGroupInfo = getPermissionGroupInfo(permissionInfo);
            if (!permissionGroupWidgets.containsKey(permissionGroupInfo.name)) {
                widget = new PermissionGroup(fragment.getActivity());
                widget.setPermissionGroupInfo(permissionGroupInfo);
                widget.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                permissionGroupWidgets.put(permissionGroupInfo.name, widget);
            } else {
                widget = permissionGroupWidgets.get(permissionGroupInfo.name);
            }
            widget.addPermission(permissionInfo);
        }
        LinearLayout container = fragment.getActivity().findViewById(R.id.permissions_container_widgets);
        container.removeAllViews();
        List<String> permissionGroupLabels = new ArrayList<>(permissionGroupWidgets.keySet());
        Collections.sort(permissionGroupLabels);
        for (String permissionGroupLabel : permissionGroupLabels) {
            container.addView(permissionGroupWidgets.get(permissionGroupLabel));
        }
        fragment.getActivity().findViewById(R.id.permissions_none).setVisibility(permissionGroupWidgets.isEmpty() ? View.VISIBLE : View.GONE);
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
}