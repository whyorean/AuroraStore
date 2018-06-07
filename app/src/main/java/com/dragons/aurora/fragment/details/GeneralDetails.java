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
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dragons.aurora.CategoryManager;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.model.App;
import com.dragons.aurora.model.ImageSource;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GeneralDetails extends AbstractHelper {

    private TextView appInfo;
    private TextView appReviews;
    private TextView appExtras;

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
        ImageView appIcon = fragment.getActivity().findViewById(R.id.icon);
        RelativeLayout relativeLayout = fragment.getActivity().findViewById(R.id.details_header);
        ImageSource imageSource = app.getIconInfo();
        if (null != imageSource.getApplicationInfo()) {
            appIcon.setImageDrawable(fragment.getContext().getPackageManager().getApplicationIcon(imageSource.getApplicationInfo()));
            if (Util.getBoolean(fragment.getContext(), "COLOR_UI")) {
                Bitmap bitmap = getBitmapFromDrawable(appIcon.getDrawable());
                getPalette(bitmap);
            }
        } else {
            Picasso
                    .with(fragment.getActivity())
                    .load(imageSource.getUrl())
                    .placeholder(R.color.transparent)
                    .into(appIcon, new Callback() {
                        @Override
                        public void onSuccess() {
                            Bitmap bitmap = ((BitmapDrawable) appIcon.getDrawable()).getBitmap();
                            if (bitmap != null && Util.getBoolean(fragment.getContext(), "COLOR_UI"))
                                getPalette(bitmap);
                        }

                        @Override
                        public void onError() {
                            if (Util.isDark(fragment.getContext()))
                                relativeLayout.setBackgroundColor(Color.LTGRAY);
                            else
                                relativeLayout.setBackgroundColor(Color.DKGRAY);
                        }
                    });
        }

        setText(fragment.getView(), R.id.displayName, app.getDisplayName());
        setText(fragment.getView(), R.id.packageName, R.string.details_developer, app.getDeveloperName());
        drawVersion(fragment.getActivity().findViewById(R.id.versionString), app);
        drawBackground(fragment.getView().findViewById(R.id.app_background));


        appIcon.setOnClickListener(v -> {
            AuroraActivity activity = (AuroraActivity) fragment.getActivity();
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

    private void drawBackground(ImageView appBackground) {
        if (null != app.getPageBackgroundImage().getUrl())
            Picasso
                    .with(fragment.getActivity())
                    .load(app.getPageBackgroundImage().getUrl())
                    .placeholder(R.color.transparent)
                    .into(appBackground);
        else
            appBackground.setVisibility(View.GONE);
    }

    private void getPalette(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette ->
                paintEmAll(palette.getDarkVibrantColor(Util.isDark(fragment.getContext())
                        ? Color.LTGRAY
                        : Color.DKGRAY)
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
            paintTextView(color, R.id.exodus_title);
            paintTextView(color, R.id.changes_upper);
            paintTextView(color, R.id.showLessMoreTxt);
        }
        paintLLayout(color, R.id.changes_container);
        paintImageView(color, R.id.privacy_ico);
        paintImageViewBg(color, R.id.apps_similar);
        paintImageViewBg(color, R.id.apps_recommended);
        paintImageViewBg(color, R.id.apps_by_same_developer);
        paintImageViewBg(color, R.id.to_play_store);
        paintImageViewBg(color, R.id.share);
        paintImageViewBg(color, R.id.system_app_info);
    }

    private void drawGeneralDetails(App app) {
        if (app.isEarlyAccess()) {
            setText(fragment.getView(), R.id.rating, R.string.early_access);
        } else {
            setText(fragment.getView(), R.id.rating, R.string.details_rating, app.getRating().getAverage());
        }

        setText(fragment.getView(), R.id.installs, R.string.details_installs, Util.addDiPrefix(app.getInstalls()));
        setText(fragment.getView(), R.id.updated, R.string.details_updated, app.getUpdated());
        setText(fragment.getView(), R.id.size, R.string.details_size, Formatter.formatShortFileSize(fragment.getActivity(), app.getSize()));
        setText(fragment.getView(), R.id.category, R.string.details_category, new CategoryManager(fragment.getActivity()).getCategoryName(app.getCategoryId()));

        setText(fragment.getView(), R.id.google_dependencies, app.getDependencies().isEmpty()
                ? R.string.list_app_independent_from_gsf
                : R.string.list_app_depends_on_gsf);
        if (app.getPrice() != null && app.getPrice().isEmpty())
            setText(fragment.getView(), R.id.price, R.string.category_appFree);
        else
            setText(fragment.getView(), R.id.price, app.getPrice());
        setText(fragment.getView(), R.id.contains_ads, app.containsAds() ? R.string.details_contains_ads : R.string.details_no_ads);

        ImageView ratingImg = fragment.getView().findViewById(R.id.ratingLabelImg);
        Picasso
                .with(fragment.getActivity())
                .load(app.getRatingURL())
                .into(ratingImg);

        ImageView categoryImg = fragment.getView().findViewById(R.id.categoryImage);
        Picasso
                .with(fragment.getActivity())
                .load(app.getCategoryIconUrl())
                .placeholder(fragment.getContext().getDrawable(R.drawable.ic_categories))
                .into(categoryImg);

        drawOfferDetails(app);
        drawChanges(app);

        if (app.getVersionCode() == 0) {
            show(fragment.getView(), R.id.updated);
            show(fragment.getView(), R.id.size);
        }

        appInfo = fragment.getActivity().findViewById(R.id.d_app_info);
        appReviews = fragment.getActivity().findViewById(R.id.d_reviews);
        appExtras = fragment.getActivity().findViewById(R.id.d_additional_info);

        appInfo.setOnClickListener(v -> {
            textViewHopper(appInfo, appReviews, appExtras);
            contentViewHopper(R.id.app_info_layout, R.id.reviews_layout, R.id.additional_info_layout);
        });
        appReviews.setOnClickListener(v -> {
            textViewHopper(appReviews, appExtras, appInfo);
            contentViewHopper(R.id.reviews_layout, R.id.app_info_layout, R.id.additional_info_layout);
        });
        appExtras.setOnClickListener(v -> {
            textViewHopper(appExtras, appReviews, appInfo);
            contentViewHopper(R.id.additional_info_layout, R.id.app_info_layout, R.id.reviews_layout);
        });

        show(fragment.getView(), R.id.mainCard);
        show(fragment.getView(), R.id.app_detail);
        show(fragment.getView(), R.id.general_card);
        hide(fragment.getView(), R.id.progress);
    }

    private void drawChanges(App app) {
        String changes = app.getChanges();
        if (TextUtils.isEmpty(changes)) {
            hide(fragment.getView(), R.id.changes_container);
        } else {
            setText(fragment.getView(), R.id.changes_upper, Html.fromHtml(changes).toString());
            show(fragment.getView(), R.id.changes_container);
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
        TextView itemView = new TextView(fragment.getActivity());
        try {
            itemView.setAutoLinkMask(Linkify.ALL);
            itemView.setText(fragment.getActivity().getString(R.string.two_items, key, Html.fromHtml(value)));
        } catch (RuntimeException e) {
            Log.w(getClass().getSimpleName(), "System WebView missing: " + e.getMessage());
            itemView.setAutoLinkMask(0);
            itemView.setText(fragment.getActivity().getString(R.string.two_items, key, Html.fromHtml(value)));
        }
    }

    private void drawVersion(TextView textView, App app) {
        String versionName = app.getVersionName();
        if (TextUtils.isEmpty(versionName)) {
            return;
        }
        textView.setText(fragment.getActivity().getString(R.string.details_versionName, versionName));
        textView.setVisibility(View.VISIBLE);
        if (!app.isInstalled()) {
            return;
        }
        try {
            PackageInfo info = fragment.getActivity().getPackageManager().getPackageInfo(app.getPackageName(), 0);
            String currentVersion = info.versionName;
            if (info.versionCode == app.getVersionCode() || null == currentVersion) {
                return;
            }
            String newVersion = versionName;
            if (currentVersion.equals(newVersion)) {
                currentVersion += " (" + info.versionCode;
                newVersion = app.getVersionCode() + ")";
            }
            textView.setText(fragment.getActivity().getString(R.string.details_versionName_updatable, currentVersion, newVersion));
            setText(fragment.getView(), R.id.download, fragment.getActivity().getString(R.string.details_update));
        } catch (PackageManager.NameNotFoundException e) {
            // We've checked for that already
        }
    }

    private void drawDescription(App app) {
        CardView changelogLayout = fragment.getActivity().findViewById(R.id.changelog_container);
        ImageView showLessMore = fragment.getActivity().findViewById(R.id.showLessMore);
        TextView showLessMoreTxt = fragment.getActivity().findViewById(R.id.showLessMoreTxt);

        if (TextUtils.isEmpty(app.getDescription())) {
            hide(fragment.getView(), R.id.more_card);
            return;
        } else {
            show(fragment.getView(), R.id.more_card);
            setText(fragment.getView(), R.id.d_moreinf, Html.fromHtml(app.getDescription()).toString());
        }

        showLessMore.setOnClickListener(v -> {
            if (changelogLayout.getVisibility() == View.GONE) {
                show(fragment.getView(), R.id.changelog_container);
                showLessMoreTxt.setText(R.string.details_less);
                showLessMore.animate().rotation(180).start();
            } else {
                hide(fragment.getView(), R.id.changelog_container);
                showLessMoreTxt.setText(R.string.details_more);
                showLessMore.animate().rotation(0).start();
            }
        });
    }

    private void textViewHopper(TextView A, TextView B, TextView C) {
        A.setAlpha(1.0f);
        B.setAlpha(0.5f);
        C.setAlpha(0.5f);
    }

    private void contentViewHopper(int viewA, int viewB, int viewC) {
        show(fragment.getView(), viewA);
        hide(fragment.getView(), viewB);
        hide(fragment.getView(), viewC);
    }
}