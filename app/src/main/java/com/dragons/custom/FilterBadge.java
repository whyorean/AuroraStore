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

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.github.florent37.shapeofview.shapes.CircleView;

public class FilterBadge extends RelativeLayout {
    private Context context;
    private boolean badgeChecked;
    private RelativeLayout badgeContainer;
    private View badgeDot;
    private CircleView badgeCancel;
    private TextView badgeText;
    private String title;
    private String key;
    private Integer dotColor;

    public FilterBadge(Context context) {
        super(context);
        init(context);
    }

    public FilterBadge(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(context);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.FilterBadge, 0, 0);

        try {
            title = a.getString(R.styleable.FilterBadge_badge_title);
            dotColor = a.getInteger(R.styleable.FilterBadge_badge_dotColor, context.getResources().getColor(R.color.colorAccent));
            key = a.getString(R.styleable.FilterBadge_badge_filter_key);
        } finally {
            a.recycle();
        }

        if (!title.isEmpty())
            setTitle(title);

        setDotColor(dotColor);
        setBadgeChecked();
        setupBadge();
    }

    public FilterBadge(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FilterBadge(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public boolean isBadgeChecked() {
        return badgeChecked;
    }

    public void setBadgeChecked() {
        badgeChecked = getFilterPreferences();
    }

    public void setTitle(String title) {
        badgeText.setText(title);
    }

    public void setDotColor(Integer color) {
        badgeDot.setBackgroundColor(color);
    }

    public void animateBadge() {
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), ColorUtils.setAlphaComponent(dotColor, 50), dotColor);
        colorAnimation.setDuration(350);
        colorAnimation.addUpdateListener(animator ->
                ViewCompat.setBackgroundTintList(badgeContainer, ColorStateList.valueOf(
                        ColorUtils.setAlphaComponent((int) animator.getAnimatedValue(),
                                200))));
        colorAnimation.start();
    }

    private void init(Context context) {
        View view = inflate(context, R.layout.filter_badge, this);
        badgeContainer = view.findViewById(R.id.badge_container);
        badgeDot = view.findViewById(R.id.badge_dot);
        badgeText = view.findViewById(R.id.badge_text);
        badgeCancel = view.findViewById(R.id.badge_cancel);
    }

    private void setupBadge() {
        if (!isBadgeChecked()) {
            badgeDot.setVisibility(VISIBLE);
            badgeText.setTextColor(Util.getStyledAttribute(context, android.R.attr.textColorPrimary));
            badgeContainer.setBackgroundTintList(null);
            badgeCancel.setVisibility(GONE);
            badgeChecked = false;
        } else {
            badgeDot.setVisibility(GONE);
            badgeText.setTextColor(Color.WHITE);
            animateBadge();
            badgeCancel.setVisibility(VISIBLE);
            badgeChecked = true;
        }

        badgeContainer.setOnClickListener(v -> {
            if (isBadgeChecked()) {
                setFilterPreferences(false);
                setupBadge();
            } else {
                setFilterPreferences(true);
                setupBadge();
            }
        });
    }

    public boolean getFilterPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(key, true);
    }

    public void setFilterPreferences(boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).apply();
        badgeChecked = value;
    }
}
