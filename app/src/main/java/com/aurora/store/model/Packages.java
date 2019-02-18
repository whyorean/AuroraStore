package com.aurora.store.model;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

public class Packages {
    private String packageName;
    private CharSequence label;
    private Drawable icon;

    public Packages(ResolveInfo info, String packageName, PackageManager packageManager) {
        this.packageName = packageName;
        label = info.loadLabel(packageManager);
        icon = info.loadIcon(packageManager);
    }

    public String getPackageName() {
        return packageName;
    }

    public CharSequence getLabel() {
        return label;
    }

    public Drawable getIcon() {
        return icon;
    }
}
