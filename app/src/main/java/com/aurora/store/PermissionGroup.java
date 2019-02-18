package com.aurora.store;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.aurora.store.utility.ViewUtil;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionGroup extends LinearLayout {

    static private final String[] permissionPrefixes = new String[]{
            "android"
    };
    static private final String permissionSuffix = ".permission.";

    private PermissionGroupInfo permissionGroupInfo;
    private Map<String, String> permissionMap = new HashMap<>();
    private PackageManager pm;

    public PermissionGroup(Context context) {
        super(context);
        init();
    }

    public PermissionGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PermissionGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PermissionGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    static private String getReadableLabel(String label, String packageName) {
        if (TextUtils.isEmpty(label)) {
            return "";
        }
        List<String> permissionPrefixesList = new ArrayList<>(Arrays.asList(permissionPrefixes));
        permissionPrefixesList.add(packageName);
        for (String permissionPrefix : permissionPrefixesList) {
            if (label.startsWith(permissionPrefix + permissionSuffix)) {
                label = label.substring((permissionPrefix + permissionSuffix).length());
                label = label.replace("_", " ").toLowerCase();
                return StringUtils.capitalize(label);
            }
        }
        return StringUtils.capitalize(label);
    }

    public void setPermissionGroupInfo(final PermissionGroupInfo permissionGroupInfo) {
        this.permissionGroupInfo = permissionGroupInfo;
        ImageView imageView = (ImageView) findViewById(R.id.permission_group_icon);
        imageView.setImageDrawable(getPermissionGroupIcon(permissionGroupInfo));
        imageView.setColorFilter(ViewUtil.getStyledAttribute(imageView.getContext(), android.R.attr.colorAccent));
    }

    public void addPermission(PermissionInfo permissionInfo) {
        CharSequence label = permissionInfo.loadLabel(pm);
        CharSequence description = permissionInfo.loadDescription(pm);
        permissionMap.put(getReadableLabel(label.toString(), permissionInfo.packageName), TextUtils.isEmpty(description) ? "" : description.toString());
        List<String> permissionLabels = new ArrayList<>(permissionMap.keySet());
        Collections.sort(permissionLabels);
        LinearLayout permissionLabelsView = findViewById(R.id.permission_labels);
        permissionLabelsView.removeAllViews();
        for (String permissionLabel : permissionLabels) {
            addPermissionLabel(permissionLabelsView, permissionLabel, permissionMap.get(permissionLabel));
        }
    }

    private void init() {
        inflate(getContext(), R.layout.layout_permission, this);
        pm = getContext().getPackageManager();
    }

    private void addPermissionLabel(LinearLayout permissionLabelsView, String label, String description) {
        TextView textView = new TextView(getContext());
        textView.setText(label);
        textView.setOnClickListener(getOnClickListener(description));
        permissionLabelsView.addView(textView);
    }

    private Drawable getPermissionGroupIcon(PermissionGroupInfo permissionGroupInfo) {
        try {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1
                    ? getContext().getResources().getDrawable(permissionGroupInfo.icon, getContext().getTheme())
                    : getContext().getResources().getDrawable(permissionGroupInfo.icon);
        } catch (Resources.NotFoundException e) {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1
                    ? permissionGroupInfo.loadUnbadgedIcon(pm)
                    : permissionGroupInfo.loadIcon(pm);
        }
    }

    private OnClickListener getOnClickListener(final String message) {
        if (TextUtils.isEmpty(message)) {
            return null;
        }
        CharSequence label = null == permissionGroupInfo ? "" : permissionGroupInfo.loadLabel(pm);
        final String title = TextUtils.isEmpty(label) ? "" : label.toString();
        return v -> new AlertDialog.Builder(getContext())
                .setIcon(getPermissionGroupIcon(permissionGroupInfo))
                .setTitle((title.equals(permissionGroupInfo.name) || title.equals(permissionGroupInfo.packageName)) ? "" : title)
                .setMessage(message)
                .show();
    }
}
