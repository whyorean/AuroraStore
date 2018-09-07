package com.dragons.aurora.task.playstore;

import android.util.Log;

import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.fragment.details.ButtonDownload;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;

public class LocalPurchaseTask extends PurchaseTask {

    private ButtonDownload buttonDownload;

    public LocalPurchaseTask setFragment(ButtonDownload fragment) {
        this.buttonDownload = fragment;
        return this;
    }

    @Override
    public LocalPurchaseTask clone() {
        LocalPurchaseTask task = new LocalPurchaseTask();
        task.setDownloadProgressBarUpdater(progressBarUpdater);
        task.setTriggeredBy(triggeredBy);
        task.setApp(app);
        task.setContext(context);
        task.setFragment(buttonDownload);
        return task;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(AndroidAppDeliveryData deliveryData) {
        super.onPostExecute(deliveryData);
        if (!success()) {
            buttonDownload.draw();
            if (null != getRestrictionString()) {
                ContextUtil.toastLong(context, getRestrictionString());
                Log.i(getClass().getSimpleName(), "No download link returned, app restriction is " + app.getRestriction());
            }
        }
    }
}