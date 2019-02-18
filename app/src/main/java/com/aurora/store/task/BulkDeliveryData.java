package com.aurora.store.task;

import android.content.Context;

import com.aurora.store.model.App;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BulkDeliveryData extends DeliveryData {
    private List<AndroidAppDeliveryData> deliveryDataList = new ArrayList<>();

    public BulkDeliveryData(Context context) {
        super(context);
    }

    public List<AndroidAppDeliveryData> getDeliveryData(List<App> mAppList) throws IOException {
        GooglePlayAPI api = getApi();
        for (App app : mAppList) {
            purchase(api, app);
            delivery(api, app);
            deliveryDataList.add(deliveryData);
        }
        return deliveryDataList;
    }
}
