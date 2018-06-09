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
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.dragons.aurora.R;

public class CustomAppBar extends NestedScrollView {

    public static final String MORE_ICON_TAG = "more_icon_tag";
    private static final int sColumnWidth = 60;
    private BottomSheetBehavior bottomSheetBehavior;
    private ImageView showLessMore;
    private OnClickListener onMoreClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {

            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        }
    };
    private BottomSheetBehavior.BottomSheetCallback bottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                showLessMore.animate().rotation(180).start();
            } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                showLessMore.animate().rotation(0).start();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    };

    public CustomAppBar(Context context) {
        super(context);
        init();
    }

    public CustomAppBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setClipToPadding(true);
        setDrawingCacheQuality(DRAWING_CACHE_QUALITY_HIGH);
        LayoutInflater.from(getContext()).inflate(R.layout.content_app_bar, this, true);
        showLessMore = findViewById(R.id.show_LessMore);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        bottomSheetBehavior = BottomSheetBehavior.from(this);
        bottomSheetBehavior.setPeekHeight((int) getResources().getDimension(R.dimen.appbar_bar_height));
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setBottomSheetCallback(bottomSheetCallback);
    }

    public void setNavigationMenu(@MenuRes int menuRes, OnClickListener onClickListener) {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.nav_items_recycler);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        MenuNavigationItemsAdapter menuNavigationItemsAdapter = new MenuNavigationItemsAdapter(getContext(), menuRes, onClickListener);
        recyclerView.setAdapter(menuNavigationItemsAdapter);
    }

    public void setSecondaryMenu(@MenuRes int menuRes, OnClickListener onClickListener) {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.secondary_menu_items_recyler);
        recyclerView.setNestedScrollingEnabled(false);
        MenuSecondaryItemsAdapter menuSecondaryItemsAdapter = new MenuSecondaryItemsAdapter(getContext(), menuRes, onClickListener);
        recyclerView.setAdapter(menuSecondaryItemsAdapter);
    }

    public void collapse() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        }, 500);
    }

    public void collapseWithoutDelay() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    public void expand() {
        bottomSheetBehavior.setState(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
    }
}

