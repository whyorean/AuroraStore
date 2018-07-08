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

package com.dragons.aurora.fragment.details;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.dragons.aurora.CategoryManager;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.model.App;
import com.dragons.aurora.model.ImageSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dragons.aurora.AuroraApplication.COLOR_UI;

public class GeneralDetails extends AbstractHelper {

    public GeneralDetails(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        drawAppBadge(app);
        if (app.isInPlayStore()) {
            drawGeneralDetails(app);
            drawDescription(app);
        }
    }

    private void drawAppBadge(App app) {
        if (view != null) {
            ImageView appIcon = view.findViewById(R.id.icon);
            ImageView app_menu3dot = view.findViewById(R.id.app_menu3dot);
            ImageSource imageSource = app.getIconInfo();
            if (null != imageSource.getApplicationInfo()) {
                appIcon.setImageDrawable(context.getPackageManager().getApplicationIcon(imageSource.getApplicationInfo()));
                if (COLOR_UI) {
                    Bitmap bitmap = getBitmapFromDrawable(appIcon.getDrawable());
                    getPalette(bitmap);
                }
            } else {
                Glide.with(context)
                        .asBitmap()
                        .load(imageSource.getUrl())
                        .apply(new RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                .placeholder(R.color.transparent)
                                .priority(Priority.HIGH))
                        .transition(new BitmapTransitionOptions().crossFade())
                        .listener(new RequestListener<Bitmap>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                if (COLOR_UI)
                                    getPalette(resource);
                                return false;
                            }
                        })
                        .into(appIcon);
            }

            setText(view, R.id.displayName, app.getDisplayName());
            setText(view, R.id.packageName, R.string.details_developer, app.getDeveloperName());
            drawVersion(view.findViewById(R.id.versionString), app);
            drawBackground(view.findViewById(R.id.app_background));


            app_menu3dot.setOnClickListener(v -> {
                AuroraActivity activity = (AuroraActivity) context;
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.inflate(R.menu.menu_download);
                new DownloadOptions(activity, app).inflate(popup.getMenu());
                popup.getMenu().findItem(R.id.action_download).setVisible(false);
                popup.getMenu().findItem(R.id.action_uninstall).setVisible(false);
                popup.getMenu().findItem(R.id.action_manual).setVisible(true);
                popup.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        default:
                            return new DownloadOptions(activity, app).onContextItemSelected(item);
                    }
                });
                popup.show();
            });
        }
    }

    private void drawBackground(ImageView appBackground) {
        if (null != app.getPageBackgroundImage().getUrl())
            Glide
                    .with(context)
                    .load(app.getPageBackgroundImage().getUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.color.transparent)
                            .priority(Priority.HIGH))
                    .into(appBackground);
        else
            appBackground.setVisibility(View.GONE);
    }

    private void getPalette(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette ->
                paintEmAll(palette.getDarkVibrantColor(Util.isDark(fragment.getContext())
                        ? palette.getDominantColor(Color.LTGRAY)
                        : palette.getDominantColor(Color.DKGRAY))
                ));
    }

    private void paintEmAll(int color) {
        paintRLayout(color, R.id.details_header);
        paintButton(color, R.id.download);
        paintButton(color, R.id.install);
        paintButton(color, R.id.run);
        paintButton(color, R.id.beta_subscribe_button);
        paintButton(color, R.id.beta_submit_button);
        if (!Util.isDark(fragment.getContext())) {
            paintTextView(color, R.id.beta_header);
            paintTextView(color, R.id.permissions_header);
            paintTextView(color, R.id.review_header);
            paintTextView(color, R.id.exodus_title);
            paintTextView(color, R.id.changes_upper);
            paintTextView(color, R.id.showLessMoreTxt);
        }
        paintLLayout(color, R.id.changes_container);
        paintImageView(color, R.id.privacy_ico);
    }

    private void drawGeneralDetails(App app) {
        if (context != null) {
            if (app.isEarlyAccess()) {
                setText(view, R.id.rating, R.string.early_access);
            } else {
                setText(view, R.id.rating, R.string.details_rating, app.getRating().getAverage());
            }

            setText(view, R.id.installs, R.string.details_installs, Util.addDiPrefix(app.getInstalls()));
            setText(view, R.id.updated, R.string.details_updated, app.getUpdated());
            setText(view, R.id.size, R.string.details_size, Formatter.formatShortFileSize(context, app.getSize()));
            setText(view, R.id.category, R.string.details_category, new CategoryManager(context).getCategoryName(app.getCategoryId()));
            setText(view, R.id.rating_lable, R.string.details_updated, app.getLabeledRating());
            setText(view, R.id.google_dependencies, app.getDependencies().isEmpty()
                    ? R.string.list_app_independent_from_gsf
                    : R.string.list_app_depends_on_gsf);
            if (app.getPrice() != null && app.getPrice().isEmpty())
                setText(view, R.id.price, R.string.category_appFree);
            else
                setText(view, R.id.price, app.getPrice());
            setText(view, R.id.contains_ads, app.containsAds() ? R.string.details_contains_ads : R.string.details_no_ads);

            ImageView categoryImg = view.findViewById(R.id.categoryImage);
            ImageView ratingImg = view.findViewById(R.id.rating_img);

            Glide.with(context)
                    .load(app.getCategoryIconUrl())
                    .apply(new RequestOptions()
                            .placeholder(ContextCompat.getDrawable(context, R.drawable.ic_categories))
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                    .into(categoryImg);

            Glide.with(context)
                    .load(app.getRatingURL())
                    .apply(new RequestOptions()
                            .placeholder(ContextCompat.getDrawable(context,R.drawable.ic_audience))
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                    .into(ratingImg);

            drawOfferDetails(app);
            drawChanges(app);

            if (app.getVersionCode() == 0) {
                show(view, R.id.updated);
                show(view, R.id.size);
            }

            show(view, R.id.mainCard);
            show(view, R.id.app_detail);
            show(view, R.id.general_card);
            show(view, R.id.related_links);
            hide(view, R.id.progress);
        }
    }

    private void drawChanges(App app) {
        String changes = app.getChanges();
        if (TextUtils.isEmpty(changes)) {
            hide(view, R.id.changes_container);
        } else {
            setText(view, R.id.changes_upper, Html.fromHtml(changes).toString());
            show(view, R.id.changes_container);
        }
    }

    private void drawOfferDetails(App app) {
        List<String> keyList = new ArrayList<>(app.getOfferDetails().keySet());
        Collections.reverse(keyList);
        for (String key : keyList) {
            addOfferItem(key, app.getOfferDetails().get(key));
        }
    }

    private void addOfferItem(String key, String value) {
        if (null == value) {
            return;
        }
        TextView itemView = new TextView(context);
        try {
            itemView.setAutoLinkMask(Linkify.ALL);
            itemView.setText(context.getString(R.string.two_items, key, Html.fromHtml(value)));
        } catch (RuntimeException e) {
            Log.w(getClass().getSimpleName(), "System WebView missing: " + e.getMessage());
            itemView.setAutoLinkMask(0);
            itemView.setText(context.getString(R.string.two_items, key, Html.fromHtml(value)));
        }
    }

    private void drawVersion(TextView textView, App app) {
        String versionName = app.getVersionName();
        if (TextUtils.isEmpty(versionName)) {
            return;
        }
        textView.setText(context.getString(R.string.details_versionName, versionName));
        textView.setVisibility(View.VISIBLE);
        if (!app.isInstalled()) {
            return;
        }
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            String currentVersion = info.versionName;
            if (info.versionCode == app.getVersionCode() || null == currentVersion) {
                return;
            }
            String newVersion = versionName;
            if (currentVersion.equals(newVersion)) {
                newVersion = String.valueOf(app.getVersionCode());
            }
            textView.setText(newVersion);
            setText(view, R.id.download, context.getString(R.string.details_update));
        } catch (PackageManager.NameNotFoundException e) {
            // We've checked for that already
        }
    }

    private void drawDescription(App app) {
        if (context != null) {
            CardView changelogLayout = view.findViewById(R.id.changelog_container);
            TextView showLessMoreTxt = view.findViewById(R.id.showLessMoreTxt);

            if (TextUtils.isEmpty(app.getDescription())) {
                hide(view, R.id.more_card);
                return;
            } else {
                show(view, R.id.more_card);
                setText(view, R.id.d_moreinf, Html.fromHtml(app.getDescription()).toString());
            }

            showLessMoreTxt.setOnClickListener(v -> {
                if (changelogLayout.getVisibility() == View.GONE) {
                    show(view, R.id.changelog_container);
                    showLessMoreTxt.setText(R.string.details_less);
                } else {
                    hide(view, R.id.changelog_container);
                    showLessMoreTxt.setText(R.string.details_more);
                }
            });
        }
    }
}