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

package com.dragons.custom;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.activities.AccountsActivity;
import com.dragons.aurora.activities.PreferenceActivity;
import com.google.android.material.appbar.AppBarLayout;

import androidx.appcompat.widget.Toolbar;

public class AdaptiveToolbar extends AppBarLayout {

    static int style;
    View root;
    Toolbar layout;
    ImageView profile_icon;
    ImageView prefs_icon;
    TextView title0;

    public AdaptiveToolbar(Context context) {
        super(context);
        init(context, null);
    }

    public AdaptiveToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AdaptiveToolbar, 0, 0);
        try {
            style = a.getInteger(R.styleable.AdaptiveToolbar_ToolbarStyle, 0);
        } finally {
            a.recycle();
        }
        root = inflate(context, R.layout.layout_toolbar, this);
        layout = root.findViewById(R.id.layout);
        profile_icon = root.findViewById(R.id.account_icon);
        prefs_icon = root.findViewById(R.id.prefs_icon);
        title0 = root.findViewById(R.id.app_title0);
        switch (getStyle()) {
            case 0:
                homeToolbar(context);
                break;
            case 1:
                detailsToolbar(context);
                break;
        }
        setupActions(context);
    }

    private void homeToolbar(Context context) {
        if (profile_icon.getVisibility() != VISIBLE) {
            profile_icon.setVisibility(VISIBLE);
        }
        setBackgroundColor(Util.getStyledAttribute(context, android.R.attr.colorPrimary));
        title0.setText(context.getString(R.string.app_name));
    }

    private void detailsToolbar(Context context) {
        if (profile_icon.getVisibility() != GONE) {
            profile_icon.setVisibility(GONE);
        }
        setBackgroundColor(Util.getStyledAttribute(context, android.R.attr.colorPrimary));
    }

    public void setupActions(Context context) {
        prefs_icon.setOnClickListener(v -> context.startActivity(new Intent(context, PreferenceActivity.class)));
        profile_icon.setOnClickListener(v -> context.startActivity(new Intent(context, AccountsActivity.class)));
    }

    public ImageView getProfileIcon() {
        return profile_icon;
    }

    public int getStyle() {
        return style;
    }

    public void setStyle(int style) {
        AdaptiveToolbar.style = style;
    }

}
