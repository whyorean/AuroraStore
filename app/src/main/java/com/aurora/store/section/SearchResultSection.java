package com.aurora.store.section;

import android.content.Context;

import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.util.PackageUtil;
import com.aurora.store.util.Util;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class SearchResultSection extends InstallAppSection {

    public SearchResultSection(Context context, ClickListener clickListener) {
        super(context, clickListener);
    }

    public void add(App app) {
        appList.add(app);
    }

    public int getCount() {
        return appList.size();
    }

    public void purgeData() {
        appList.clear();
    }

    @Override
    public void updateList(List<App> appList) {
        this.appList.clear();
        for (App app : appList) {
            if (this.appList.contains(app)) {
                continue;
            }
            this.appList.add(app);
        }

        if (appList.isEmpty())
            setState(State.EMPTY);
        else
            setState(State.LOADED);
    }

    @Override
    public void getDetails(List<String> Version, List<String> Extra, App app) {
        Version.add(Util.addSiPrefix(app.getSize()));
        if (!app.isEarlyAccess())
            Version.add(context.getString(R.string.details_rating, (app.getRating().getAverage())));
        if (PackageUtil.isInstalled(context, app.getPackageName()))
            Version.add(context.getString(R.string.action_installed));
        Extra.add(app.getPrice());
        Extra.add(context.getString(app.isContainsAds() ? R.string.list_app_has_ads : R.string.list_app_no_ads));
        Extra.add(context.getString(app.getDependencies().isEmpty() ? R.string.list_app_independent_from_gsf : R.string.list_app_depends_on_gsf));
        if (!StringUtils.isEmpty(app.getUpdated()))
            Extra.add(app.getUpdated());
    }
}
