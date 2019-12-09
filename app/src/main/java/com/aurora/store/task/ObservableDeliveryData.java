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

import com.aurora.store.AuroraApplication;
import com.aurora.store.Constants;
import com.aurora.store.exception.NotPurchasedException;
import com.aurora.store.model.App;
import com.aurora.store.util.Log;
import com.aurora.store.util.PrefUtil;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;
import com.dragons.aurora.playstoreapiv2.BuyResponse;
import com.dragons.aurora.playstoreapiv2.DeliveryResponse;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.io.IOException;

import io.reactivex.Observable;

public class ObservableDeliveryData extends BaseTask {

    public AndroidAppDeliveryData deliveryData;
    private String downloadToken;

    public ObservableDeliveryData(Context context) {
        super(context);
    }

    public Observable<DeliveryDataBundle> getDeliveryData(App app) {
        return Observable.create(emitter -> {
            GooglePlayAPI api = AuroraApplication.api;
            purchase(api, app);
            delivery(api, app);
            DeliveryDataBundle deliveryDataBundle = new DeliveryDataBundle(app, deliveryData);
            emitter.onNext(deliveryDataBundle);
            emitter.onComplete();
        });
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
            Log.d("Failed to purchase %s", app.getDisplayName());
        }
    }

    void delivery(GooglePlayAPI api, App app) throws IOException {
        DeliveryResponse deliveryResponse = api.delivery(
                app.getPackageName(),
                shouldDownloadDelta(app) ? app.getInstalledVersionCode() : 0,
                app.getVersionCode(),
                app.getOfferType(),
                downloadToken
        );
        if (deliveryResponse.hasAppDeliveryData()
                && deliveryResponse.getAppDeliveryData().hasDownloadUrl()) {
            deliveryData = deliveryResponse.getAppDeliveryData();
        } else if (deliveryData == null) {
            throw new NotPurchasedException(app.getDisplayName());
        }
    }

    private boolean shouldDownloadDelta(App app) {
        return PrefUtil.getBoolean(context, Constants.PREFERENCE_DOWNLOAD_DELTAS)
                && app.getInstalledVersionCode() < app.getVersionCode();
    }

    public class DeliveryDataBundle {
        private App app;
        private AndroidAppDeliveryData androidAppDeliveryData;

        public DeliveryDataBundle(App app, AndroidAppDeliveryData androidAppDeliveryData) {
            this.app = app;
            this.androidAppDeliveryData = androidAppDeliveryData;
        }

        public App getApp() {
            return app;
        }

        public AndroidAppDeliveryData getAndroidAppDeliveryData() {
            return androidAppDeliveryData;
        }
    }
}
