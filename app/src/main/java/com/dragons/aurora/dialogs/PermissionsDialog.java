package com.dragons.aurora.dialogs;

import android.app.Dialog;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.fragment.widget.PermissionGroup;
import com.dragons.aurora.model.App;
import com.dragons.custom.AuroraDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;

public class PermissionsDialog extends AppCompatDialogFragment {

    @BindView(R.id.permissions_header)
    TextView viewHeader;
    @BindView(R.id.permissions_container)
    LinearLayout viewContainer;
    @BindView(R.id.permissions_container_widgets)
    LinearLayout container;
    @BindView(R.id.permissions_none)
    TextView permissions_none;

    private View view;
    private App app;
    private PackageManager pm;

    public void setApp(App app) {
        this.app = app;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.dialog_permissions, container, false);
        pm = getContext().getPackageManager();
        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AuroraDialog(getContext(), Util.isDark(getContext()) ? R.style.Theme_Aurora_Dialog_Dark : R.style.Theme_Aurora_Dialog);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        addPermissionWidgets();
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

        container.removeAllViews();
        List<String> permissionGroupLabels = new ArrayList<>(permissionGroupWidgets.keySet());
        Collections.sort(permissionGroupLabels);
        for (String permissionGroupLabel : permissionGroupLabels) {
            container.addView(permissionGroupWidgets.get(permissionGroupLabel));
        }
        permissions_none.setVisibility(permissionGroupWidgets.isEmpty() ? View.VISIBLE : View.GONE);
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
