package com.aurora.store.model.items;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.util.PackageUtil;
import com.aurora.store.util.Util;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EndlessItem extends BaseItem {

    public EndlessItem(App app) {
        super(app);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_installed;
    }

    @NotNull
    @Override
    public BaseViewHolder getViewHolder(@NotNull View view) {
        return new ViewHolder(view);
    }

    public static class ViewHolder extends BaseViewHolder {

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        public void fillAppDetails(Context context, List<String> versionStringList, List<String> extraStringList, App app) {
            versionStringList.add(Util.addSiPrefix(app.getSize()));
            if (!app.isEarlyAccess())
                versionStringList.add(context.getString(R.string.details_rating, (app.getRating().getAverage())));
            if (PackageUtil.isInstalled(context, app.getPackageName()))
                versionStringList.add(context.getString(R.string.action_installed));
            if (!app.isFree())
                extraStringList.add(app.getPrice());
            extraStringList.add(context.getString(app.getDependencies().isEmpty() ? R.string.list_app_independent_from_gsf : R.string.list_app_depends_on_gsf));
            extraStringList.add(context.getString(app.isContainsAds() ? R.string.list_app_has_ads : R.string.list_app_no_ads));
            if (!StringUtils.isEmpty(app.getUpdated()))
                extraStringList.add(app.getUpdated());
        }
    }
}
