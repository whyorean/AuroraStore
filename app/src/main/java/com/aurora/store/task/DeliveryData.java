package com.aurora.store.task;

import android.content.Context;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.exception.NotPurchasedException;
import com.aurora.store.model.App;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PrefUtil;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;
import com.dragons.aurora.playstoreapiv2.BuyResponse;
import com.dragons.aurora.playstoreapiv2.DeliveryResponse;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.io.IOException;

public class DeliveryData extends BaseTask {

    public AndroidAppDeliveryData deliveryData;
    private String downloadToken;

    public DeliveryData(Context context) {
        super(context);
    }

    public AndroidAppDeliveryData getDeliveryData(App app) throws IOException {
        GooglePlayAPI api = getApi();
        purchase(api, app);
        delivery(api, app);
        return deliveryData;
    }

    public void purchase(GooglePlayAPI api, App app) {
        try {
            BuyResponse buyResponse = api.purchase(app.getPackageName(), app.getVersionCode(), app.getOfferType());
            if (buyResponse.hasPurchaseStatusResponse()
                    && buyResponse.getPurchaseStatusResponse().hasAppDeliveryData()
                    && buyResponse.getPurchaseStatusResponse().getAppDeliveryData().hasDownloadUrl()) {
                deliveryData = buyResponse.getPurchaseStatusResponse().getAppDeliveryData();
            }
            if (buyResponse.hasDownloadToken()) {
                downloadToken = buyResponse.getDownloadToken();
            }
        } catch (IOException e) {
            Log.w("Purchase for " + app.getPackageName() + " failed with " + e.getClass().getName()
                    + ": " + e.getMessage());
        }
    }

    void delivery(GooglePlayAPI api, App app) throws IOException {
        DeliveryResponse deliveryResponse = api.delivery(
                app.getPackageName(),
                shouldDownloadDelta(app) ? app.getInstalledVersionCode() : 0,
                app.getVersionCode(),
                app.getOfferType(),
                GooglePlayAPI.PATCH_FORMAT.GZIPPED_GDIFF,
                downloadToken
        );
        if (deliveryResponse.hasAppDeliveryData()
                && deliveryResponse.getAppDeliveryData().hasDownloadUrl()) {
            deliveryData = deliveryResponse.getAppDeliveryData();
        } else {
            throw new NotPurchasedException();
        }
    }

    protected String getRestrictionString(App app) {
        switch (app.getRestriction()) {
            case GooglePlayAPI.AVAILABILITY_NOT_RESTRICTED:
                return null;
            case GooglePlayAPI.AVAILABILITY_RESTRICTED_GEO:
                return context.getString(R.string.availability_restriction_country);
            case GooglePlayAPI.AVAILABILITY_INCOMPATIBLE_DEVICE_APP:
                return context.getString(R.string.availability_restriction_hardware_app);
            default:
                return context.getString(R.string.availability_restriction_generic);
        }
    }

    private boolean shouldDownloadDelta(App app) {
        return PrefUtil.getBoolean(context, Constants.PREFERENCE_DOWNLOAD_DELTAS)
                && app.getInstalledVersionCode() < app.getVersionCode();
    }
}
