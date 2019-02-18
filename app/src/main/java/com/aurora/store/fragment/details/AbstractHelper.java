package com.aurora.store.fragment.details;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.TextView;

import com.aurora.store.activity.ManualDownloadActivity;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.model.App;

import butterknife.ButterKnife;

public abstract class AbstractHelper {

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

    protected boolean isInstalled() {
        try {
            context.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

}
