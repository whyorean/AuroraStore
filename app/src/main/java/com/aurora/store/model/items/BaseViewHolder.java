package com.aurora.store.model.items;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.util.Log;
import com.aurora.store.util.PackageUtil;
import com.aurora.store.util.Util;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.mikepenz.fastadapter.FastAdapter;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BaseViewHolder extends FastAdapter.ViewHolder<BaseItem> {

    @BindView(R.id.img_icon)
    ImageView imgIcon;
    @BindView(R.id.line1)
    TextView txtTitle;
    @BindView(R.id.line2)
    TextView txtVersion;
    @BindView(R.id.line3)
    TextView txtExtra;

    private Context context;

    public BaseViewHolder(@NonNull View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        context = itemView.getContext();
    }

    @Override
    public void bindView(@NotNull BaseItem item, @NotNull List<?> list) {
        final App app = item.getApp();
        final List<String> versionStringList = new ArrayList<>();
        final List<String> extraStringList = new ArrayList<>();

        fillAppDetails(context, versionStringList, extraStringList, item.getApp());

        txtTitle.setText(item.getApp().getDisplayName());
        txtVersion.setText(StringUtils.join(versionStringList.toArray(), " • "));
        txtExtra.setText(StringUtils.join(extraStringList.toArray(), " • "));

        GlideApp
                .with(context)
                .load(item.getApp().getIconUrl())
                .transition(new DrawableTransitionOptions().crossFade())
                .transforms(new CenterCrop(), new RoundedCorners(30))
                .into(imgIcon);
    }

    @Override
    public void unbindView(@NotNull BaseItem item) {
        txtTitle.setText(null);
        txtVersion.setText(null);
        txtExtra.setText(null);
        imgIcon.setImageDrawable(null);
    }

    public void fillAppDetails(Context context, List<String> versionStringList, List<String> extraStringList, App app) {
        versionStringList.add(Util.addSiPrefix(app.getSize()));
        Log.e("Rating : %f", app.getRating().getAverage());
        if (!app.isEarlyAccess())
            versionStringList.add(context.getString(R.string.details_rating, (app.getRating().getAverage())));

        if (PackageUtil.isInstalled(context, app.getPackageName()))
            versionStringList.add(context.getString(R.string.action_installed));

        extraStringList.add(app.getPrice());
        extraStringList.add(context.getString(app.isContainsAds() ? R.string.list_app_has_ads : R.string.list_app_no_ads));
        extraStringList.add(context.getString(app.getDependencies().isEmpty() ? R.string.list_app_independent_from_gsf : R.string.list_app_depends_on_gsf));

        if (!StringUtils.isEmpty(app.getUpdated()))
            extraStringList.add(app.getUpdated());
    }
}
