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

package com.aurora.store.ui.details.views;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.ui.devapps.DevAppsActivity;
import com.aurora.store.ui.single.activity.ManualDownloadActivity;
import com.aurora.store.util.Log;
import com.aurora.store.util.PackageUtil;
import com.aurora.store.util.ViewUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import butterknife.ButterKnife;

public abstract class AbstractDetails {

    static private final String PLAY_STORE_PACKAGE_NAME = "com.android.vending";

    protected DetailsActivity activity;
    protected Context context;
    protected App app;

    public AbstractDetails(DetailsActivity activity, App app) {
        this.activity = activity;
        this.context = activity;
        this.app = app;
        ButterKnife.bind(this, activity);
    }

    public AbstractDetails(ManualDownloadActivity activity, App app) {
        this.app = app;
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
        Intent intent = new Intent(context, DevAppsActivity.class);
        intent.putExtra("SearchQuery", Constants.PUB_PREFIX + app.getDeveloperName());
        intent.putExtra("SearchTitle", app.getDeveloperName());
        context.startActivity(intent, ViewUtil.getEmptyActivityBundle(activity));
    }

    protected void setText(int viewId, String text) {
        TextView textView = activity.findViewById(viewId);
        if (null != textView) {
            textView.setText(text);
        }
    }

    protected void setText(int viewId, int stringId, Object... text) {
        setText(viewId, activity.getResources().getString(stringId, text));
    }

    protected void hide(int viewID) {
        activity.findViewById(viewID).setVisibility(View.GONE);
    }

    protected void show(ViewGroup viewGroup, int... viewIds) {
        TransitionManager.beginDelayedTransition(viewGroup);
        for (int viewId : viewIds) {
            activity.findViewById(viewId).setVisibility(View.VISIBLE);
        }
    }

    protected void show(int... viewIds) {
        for (int viewId : viewIds) {
            activity.findViewById(viewId).setVisibility(View.VISIBLE);
        }
    }

    protected boolean isPlayStoreInstalled() {
        return PackageUtil.isInstalled(activity, PLAY_STORE_PACKAGE_NAME);
    }

    protected void showPurchaseDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.dialog_purchase_title))
                .setMessage(context.getString(R.string.dialog_purchase_desc))
                .setPositiveButton(context.getString(R.string.dialog_purchase_positive), (dialog, which) -> {
                    openWebView(Constants.APP_DETAIL_URL + app.getPackageName());
                })
                .setNegativeButton(context.getString(R.string.action_later), (dialog, which) -> {
                    dialog.dismiss();
                });
        builder.create();
        builder.show();
    }

    protected void showGeoRestrictionDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.dialog_geores_title))
                .setMessage(context.getString(R.string.dialog_geores_desc))
                .setPositiveButton(context.getString(R.string.action_close), (dialog, which) -> {
                    dialog.dismiss();
                });
        builder.create();
        builder.show();
    }

    protected void showIncompatibleDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.dialog_incompat_title))
                .setMessage(context.getString(R.string.dialog_incompat_desc))
                .setPositiveButton(context.getString(R.string.action_close), (dialog, which) -> {
                    dialog.dismiss();
                });
        builder.create();
        builder.show();
    }

    private void openWebView(String URL) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
        } catch (Exception e) {
            Log.e("No WebView found !");
        }
    }
}
