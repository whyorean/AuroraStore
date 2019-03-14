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
