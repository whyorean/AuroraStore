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

package com.dragons.aurora.view;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.support.annotation.ColorInt;
import android.support.design.widget.AppBarLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dragons.aurora.R;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.fragment.UpdatableAppsFragment;

public class AdaptiveToolbar extends AppBarLayout {

    static int style;
    View root;
    LinearLayout adtoolbarlayout;
    ImageView action_icon, avatar_icon;
    ImageButton download_section;
    RelativeLayout bubble_layout, download_container;
    TextView title0, title1, updates_counter;


    public AdaptiveToolbar(Context context) {
        super(context);
        init(context, null);
    }

    public AdaptiveToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    @ColorInt
    public static int getColorAttr(Context context, int attr) {
        TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
        @ColorInt int colorAccent = ta.getColor(0, 0);
        ta.recycle();
        return colorAccent;
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AdaptiveToolbar, 0, 0);
        try {
            style = a.getInteger(R.styleable.AdaptiveToolbar_ToolbarStyle, 0);
        } finally {
            a.recycle();
        }
        root = inflate(context, R.layout.adaptive_toolbar, this);
        adtoolbarlayout = root.findViewById(R.id.adtoolbar_layout);
        action_icon = root.findViewById(R.id.action_icon);
        avatar_icon = root.findViewById(R.id.account_avatar);
        download_section = root.findViewById(R.id.download_section);
        download_container = root.findViewById(R.id.dwn_container);
        title0 = root.findViewById(R.id.app_title0);
        title1 = root.findViewById(R.id.app_title1);
        bubble_layout = root.findViewById(R.id.updates_bubble);
        updates_counter = root.findViewById(R.id.updates_counter);
        switch (getStyle()) {
            case 0:
                homeToolbar(context);
                break;
            case 1:
                detailsToolbar(context);
                break;
        }
    }

    private void homeToolbar(Context context) {
        if (avatar_icon.getVisibility() != VISIBLE) {
            avatar_icon.setVisibility(VISIBLE);
        }
        download_container.setVisibility(GONE);
        action_icon.setImageResource(R.mipmap.ic_launcher);
        action_icon.setPadding(0, 0, 0, 0);
        action_icon.setContentDescription("home");
        setBackgroundColor(getColorAttr(context, android.R.attr.colorPrimary));
        title0.setText("Aurora");
        title1.setText("Store");
        title1.setVisibility(VISIBLE);
    }

    private void detailsToolbar(Context context) {
        if (avatar_icon.getVisibility() != GONE) {
            avatar_icon.setVisibility(GONE);
        }
        download_container.setVisibility(VISIBLE);
        download_section.setOnClickListener((v -> {
            context.startActivity(new Intent(context, AuroraActivity.class));
            AuroraActivity.setPosition(2);
        }));
        if (UpdatableAppsFragment.updates > 0) {
            bubble_layout.setVisibility(VISIBLE);
            updates_counter.setText(String.valueOf(UpdatableAppsFragment.updates));
        } else {
            bubble_layout.setVisibility(GONE);
        }
        action_icon.setImageResource(R.drawable.ic_chevron_left);
        action_icon.setPadding(6, 6, 6, 6);
        action_icon.setContentDescription("details");
        setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }

    public ImageButton getDownload_section() {
        return download_section;
    }

    public TextView getTitle0() {
        return title0;
    }

    public TextView getTitle1() {
        return title1;
    }

    public ImageView getAction_icon() {
        return action_icon;
    }

    public ImageView getAvatar_icon() {
        return avatar_icon;
    }

    public int getStyle() {
        return style;
    }

    public void setStyle(int style) {
        AdaptiveToolbar.style = style;
    }
}
