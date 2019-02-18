package com.aurora.store;

import android.content.Context;
import android.content.SharedPreferences;

import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.Util;

public class Filter {

    private Context context;

    public Filter(Context context) {
        this.context = context;
    }

    public com.aurora.store.model.Filter getFilterPreferences() {
        com.aurora.store.model.Filter filter = new com.aurora.store.model.Filter();
        SharedPreferences prefs = Util.getPrefs(context);
        filter.setSystemApps(prefs.getBoolean(Constants.FILTER_SYSTEM_APPS, false));
        filter.setAppsWithAds(prefs.getBoolean(Constants.FILTER_APPS_WITH_ADS, true));
        filter.setPaidApps(prefs.getBoolean(Constants.FILTER_PAID_APPS, true));
        filter.setGsfDependentApps(prefs.getBoolean(Constants.FILTER_GSF_DEPENDENT_APPS, true));
        filter.setCategory(prefs.getString(Constants.FILTER_CATEGORY, Constants.TOP));
        filter.setRating(prefs.getFloat(Constants.FILTER_RATING, 0.0f));
        filter.setDownloads(prefs.getInt(Constants.FILTER_DOWNLOADS, 0));
        return filter;
    }

    public void resetFilterPreferences() {
        SharedPreferences prefs = Util.getPrefs(context);
        PrefUtil.putBoolean(context, Constants.FILTER_SYSTEM_APPS, false);
        PrefUtil.putBoolean(context, Constants.FILTER_APPS_WITH_ADS, true);
        PrefUtil.putBoolean(context, Constants.FILTER_PAID_APPS, true);
        PrefUtil.putBoolean(context, Constants.FILTER_GSF_DEPENDENT_APPS, true);
        PrefUtil.putString(context, Constants.FILTER_CATEGORY, Constants.TOP);
        PrefUtil.putFloat(context, Constants.FILTER_RATING, 0.0f);
        PrefUtil.putInteger(context, Constants.FILTER_DOWNLOADS, 0);
    }
}
