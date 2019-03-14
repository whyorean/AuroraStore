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
