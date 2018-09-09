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

package com.dragons.aurora.task.playstore;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.NotPurchasedException;
import com.dragons.aurora.R;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;
import com.dragons.aurora.playstoreapiv2.BuyResponse;
import com.dragons.aurora.playstoreapiv2.DeliveryResponse;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.io.IOException;

import timber.log.Timber;

public class DeliveryDataTask extends PlayStorePayloadTask<AndroidAppDeliveryData> {

    protected App app;
    protected AndroidAppDeliveryData deliveryData;
    private String downloadToken;

    public void setApp(App app) {
        this.app = app;
    }

    @Override
    protected AndroidAppDeliveryData getResult(GooglePlayAPI api, String... arguments) throws IOException {
        purchase(api);
        delivery(api);
        return deliveryData;
    }

    private void purchase(GooglePlayAPI api) {
        try {
            BuyResponse buyResponse = api.purchase(app.getPackageName(), app.getVersionCode(), app.getOfferType());
            if (buyResponse.hasPurchaseStatusResponse()
                    && buyResponse.getPurchaseStatusResponse().hasAppDeliveryData()
                    && buyResponse.getPurchaseStatusResponse().getAppDeliveryData().hasDownloadUrl()
            ) {
                deliveryData = buyResponse.getPurchaseStatusResponse().getAppDeliveryData();
            }
            if (buyResponse.hasDownloadToken()) {
                downloadToken = buyResponse.getDownloadToken();
            }
        } catch (IOException e) {
            Timber.w("Purchase for " + app.getPackageName() + " failed with " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private void delivery(GooglePlayAPI api) throws IOException {
        DeliveryResponse deliveryResponse = api.delivery(
                app.getPackageName(),
                shouldDownloadDelta() ? app.getInstalledVersionCode() : 0,
                app.getVersionCode(),
                app.getOfferType(),
                GooglePlayAPI.PATCH_FORMAT.GZIPPED_GDIFF,
                downloadToken
        );
        if (deliveryResponse.hasAppDeliveryData()
                && deliveryResponse.getAppDeliveryData().hasDownloadUrl()
        ) {
            deliveryData = deliveryResponse.getAppDeliveryData();
        } else {
            throw new NotPurchasedException();
        }
    }

    protected String getRestrictionString() {
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

    private boolean shouldDownloadDelta() {
        return Prefs.getBoolean(context, Aurora.PREFERENCE_DOWNLOAD_DELTAS)
                && app.getInstalledVersionCode() < app.getVersionCode()
                ;
    }
}
