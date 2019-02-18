package com.aurora.store.task;

import android.content.Context;

import com.aurora.store.model.App;
import com.aurora.store.model.AppBuilder;
import com.dragons.aurora.playstoreapiv2.BulkDetailsEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BulkDetails extends BaseTask {

    public BulkDetails(Context context) {
        super(context);
    }

    public List<App> getRemoteAppList(List<String> packageNames) throws IOException {
        List<App> apps = new ArrayList<>();
        for (BulkDetailsEntry details : getApi().bulkDetails(packageNames).getEntryList()) {
            if (!details.hasDoc()) {
                continue;
            }
            apps.add(AppBuilder.build(details.getDoc()));
        }
        return apps;
    }
}
