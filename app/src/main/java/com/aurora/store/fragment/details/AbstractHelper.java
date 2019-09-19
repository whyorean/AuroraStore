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

package com.aurora.store.fragment.details;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentTransaction;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.activity.ManualDownloadActivity;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.fragment.DevAppsFragment;
import com.aurora.store.model.App;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PackageUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import butterknife.ButterKnife;

public abstract class AbstractHelper {

    static private final String PLAY_STORE_PACKAGE_NAME = "com.android.vending";

    protected DetailsFragment fragment;
    protected App app;
    protected View view;
    protected Context context;

    public AbstractHelper(DetailsFragment fragment, App app) {
        this.fragment = fragment;
        this.app = app;
        this.view = fragment.getView();
        this.context = fragment.getContext();
        ButterKnife.bind(this, view);
    }

    public AbstractHelper(ManualDownloadActivity activity, App app) {
        this.app = app;
        this.context = activity;
        ButterKnife.bind(activity);
    }

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    abstract public void draw();

    protected void showDevApps() {
        DevAppsFragment devAppsFragment = new DevAppsFragment();
        Bundle arguments = new Bundle();
        arguments.putString("SearchQuery", Constants.PUB_PREFIX + app.getDeveloperName());
        arguments.putString("SearchTitle", app.getDeveloperName());
        devAppsFragment.setArguments(arguments);
        fragment.getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.coordinator, devAppsFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

    protected void setText(View v, int viewId, String text) {
        TextView textView = view.findViewById(viewId);
        if (null != textView)
            textView.setText(text);
    }

    protected void setText(View v, int viewId, int stringId, Object... text) {
        if (v != null)
            setText(v, viewId, v.getResources().getString(stringId, text));
    }

    protected void hide(View v, int viewID) {
        v.findViewById(viewID).setVisibility(View.GONE);
    }

    protected void show(View v, int viewID) {
        v.findViewById(viewID).setVisibility(View.VISIBLE);
    }

    protected boolean isPlayStoreInstalled() {
        return PackageUtil.isInstalled(context, PLAY_STORE_PACKAGE_NAME);
    }

    protected void showPurchaseDialog() {
        MaterialAlertDialogBuilder mBuilder = new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.dialog_purchase_title))
                .setMessage(context.getString(R.string.dialog_purchase_desc))
                .setPositiveButton(context.getString(R.string.dialog_purchase_positive), (dialog, which) -> {
                    openWebView(Constants.APP_DETAIL_URL + app.getPackageName());
                })
                .setNegativeButton(context.getString(R.string.action_later), (dialog, which) -> {
                    dialog.dismiss();
                });
        mBuilder.create();
        mBuilder.show();
    }

    protected void showGeoRestrictionDialog() {
        MaterialAlertDialogBuilder mBuilder = new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.dialog_geores_title))
                .setMessage(context.getString(R.string.dialog_geores_desc))
                .setPositiveButton(context.getString(R.string.action_close), (dialog, which) -> {
                    dialog.dismiss();
                });
        mBuilder.create();
        mBuilder.show();
    }

    protected void showIncompatibleDialog() {
        MaterialAlertDialogBuilder mBuilder = new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.dialog_incompat_title))
                .setMessage(context.getString(R.string.dialog_incompat_desc))
                .setPositiveButton(context.getString(R.string.action_close), (dialog, which) -> {
                    dialog.dismiss();
                });
        mBuilder.create();
        mBuilder.show();
    }

    private void openWebView(String URL) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
        } catch (Exception e) {
            Log.e("No WebView found !");
        }
    }

    protected void paintButton(@IdRes int buttonId, @ColorInt int color) {
        android.widget.Button button = view.findViewById(buttonId);
        if (button != null)
            ViewCompat.setBackgroundTintList(button, ColorStateList.valueOf(color));
    }

    protected void paintTextView(@IdRes int textViewId, @ColorInt int color) {
        TextView textView = view.findViewById(textViewId);
        if (textView != null)
            textView.setTextColor(color);
    }

    protected void paintTextViewBg(@IdRes int textViewId, @ColorInt int color, int alpha) {
        TextView textView = view.findViewById(textViewId);
        if (textView != null)
            textView.setBackgroundColor(ColorUtils.setAlphaComponent(color, alpha));
    }

    protected void paintLayout(@IdRes int viewId, @ColorInt int color) {
        ViewGroup viewGroup = view.findViewById(viewId);
        if (viewGroup != null)
            viewGroup.setBackgroundColor(color);
    }

    protected void paintLayout(@IdRes int viewId, @ColorInt int color, int alpha) {
        ViewGroup viewGroup = view.findViewById(viewId);
        if (viewGroup != null)
            viewGroup.setBackgroundColor(ColorUtils.setAlphaComponent(color, alpha));
    }

}
