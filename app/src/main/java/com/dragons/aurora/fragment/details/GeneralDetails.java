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
import android.text.Html;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.util.Linkify;
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
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.dragons.aurora.CategoryManager;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.fragment.DetailsFragmentMore;
import com.dragons.aurora.model.App;
import com.dragons.aurora.model.ImageSource;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.palette.graphics.Palette;
import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static com.dragons.aurora.AuroraApplication.COLOR_UI;

public class GeneralDetails extends AbstractHelper {

    @BindView(R.id.app_background)
    ImageView appBackground;
    @BindView(R.id.icon)
    ImageView appIcon;
    @BindView(R.id.app_menu3dot)
    ImageView app_menu3dot;
    @BindView(R.id.img_category)
    ImageView categoryImg;
    @BindView(R.id.showLessMoreTxt)
    TextView showLessMoreTxt;
    @BindView(R.id.versionString)
    TextView app_version;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.collapsingToolbar)
    CollapsingToolbarLayout mCollapsingToolbarLayout;

    private int colorPrimary = Color.LTGRAY;
    private int colorPrimaryText = Color.DKGRAY;

    public GeneralDetails(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        ButterKnife.bind(this, view);
        drawAppBadge();
        if (app.isInPlayStore()) {
            drawGeneralDetails();
            drawDescription();
            setupReadMore();
        }
    }

    private void drawAppBadge() {
        if (view != null) {
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
            setText(view, R.id.packageName, app.getDeveloperName());
            mCollapsingToolbarLayout.setTitle(app.getDisplayName());
            mCollapsingToolbarLayout.setExpandedTitleColor(context.getResources().getColor(android.R.color.transparent));
            drawVersion(app);
            drawBackground();

            app_menu3dot.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.inflate(R.menu.menu_download);
                new DownloadOptions(context, view, app).inflate(popup.getMenu());
                popup.getMenu().findItem(R.id.action_download).setVisible(false);
                popup.getMenu().findItem(R.id.action_uninstall).setVisible(false);
                popup.getMenu().findItem(R.id.action_manual).setVisible(true);
                popup.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        default:
                            return new DownloadOptions(context, view, app).onContextItemSelected(item);
                    }
                });
                popup.show();
            });
        }
    }

    private void drawBackground() {
        if (null != app.getPageBackgroundImage().getUrl())
            Glide.with(context)
                    .load(app.getPageBackgroundImage().getUrl())
                    .apply(new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .placeholder(R.color.transparent))
                    .into(appBackground);
        else
            appBackground.setVisibility(View.GONE);
    }

    private void getPalette(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette != null)
                paintEmAll(palette);
        });
    }

    private void paintEmAll(Palette palette) {
        Palette.Swatch mSwatch = palette.getDarkVibrantSwatch();
        //Make sure we get a fallback swatch if DarkVibrantSwatch is not available
        if (mSwatch == null)
            mSwatch = palette.getVibrantSwatch();

        //Make sure we get another fallback swatch if VibrantSwatch is not available
        if (mSwatch == null)
            mSwatch = palette.getDominantSwatch();

        if (mSwatch != null) {
            colorPrimary = mSwatch.getRgb();
            colorPrimaryText = mSwatch.getBodyTextColor();
        }

        AbstractHelper.color = colorPrimary;
        AbstractHelper.colorText = colorPrimaryText;

        paintButton(R.id.download);
        paintButton(R.id.install);
        paintButton(R.id.run);
        paintButton(R.id.beta_subscribe_button);
        paintButton(R.id.beta_submit_button);
        paintButton(R.id.moreButton);
        if (!Util.isDark(fragment.getContext())) {
            paintTextView(R.id.beta_header);
            paintTextView(R.id.permissions_header);
            paintTextView(R.id.exodus_title);
            paintTextViewTxt(R.id.changes_upper);
            paintTextView(R.id.showLessMoreTxt);
            paintTextView(R.id.txt_readAll);
        }
        paintLLayout(R.id.changes_container);
        paintImageView(R.id.privacy_ico);
        paintImageViewBackground(R.id.img_star);
        paintImageViewBackground(R.id.img_downloads);
        paintImageViewBackground(R.id.img_storage);
        paintImageViewBackground(R.id.img_category);
    }

    private void drawGeneralDetails() {
        if (context != null) {
            if (app.isEarlyAccess()) {
                setText(view, R.id.rating, R.string.early_access);
            } else {
                setText(view, R.id.rating, R.string.details_rating, app.getRating().getAverage());
            }

            setText(view, R.id.installs, Util.addDiPrefix(app.getInstalls()));
            setText(view, R.id.size, Formatter.formatShortFileSize(context, app.getSize()));
            setText(view, R.id.category, new CategoryManager(context).getCategoryName(app.getCategoryId()));

            if (app.getPrice() != null && app.getPrice().isEmpty())
                setText(view, R.id.price, R.string.category_appFree);
            else
                setText(view, R.id.price, app.getPrice());
            setText(view, R.id.contains_ads, app.containsAds() ? R.string.details_contains_ads : R.string.details_no_ads);

            Glide.with(context)
                    .load(app.getCategoryIconUrl())
                    .apply(new RequestOptions()
                            .placeholder(ContextCompat.getDrawable(context, R.drawable.ic_categories))
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                    .into(categoryImg);

            drawOfferDetails();
            drawChanges();

            if (app.getVersionCode() == 0) {
                show(view, R.id.size);
            }

            show(view, R.id.layout_main);
            show(view, R.id.app_detail);
            show(view, R.id.related_links);
            hide(view, R.id.progress);
        }
    }

    private void drawChanges() {
        String changes = app.getChanges();
        if (TextUtils.isEmpty(changes)) {
            hide(view, R.id.changes_container);
        } else {
            setText(view, R.id.changes_upper, Html.fromHtml(changes).toString());
            show(view, R.id.changes_container);
        }
    }

    private void drawOfferDetails() {
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
            Timber.w("System WebView missing: %s", e.getMessage());
            itemView.setAutoLinkMask(0);
            itemView.setText(context.getString(R.string.two_items, key, Html.fromHtml(value)));
        }
    }

    private void drawVersion(App app) {
        String versionName = app.getVersionName();
        if (TextUtils.isEmpty(versionName)) {
            return;
        }
        app_version.setText(versionName);
        app_version.setVisibility(View.VISIBLE);
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
            app_version.setText(newVersion);
            setText(view, R.id.download, context.getString(R.string.details_update));
        } catch (PackageManager.NameNotFoundException e) {
            // We've checked for that already
        }
    }

    private void drawDescription() {
        if (context != null) {

            if (TextUtils.isEmpty(app.getDescription())) {
                hide(view, R.id.more_card);
                return;
            } else {
                show(view, R.id.more_card);
                setText(view, R.id.content_readMore, Html.fromHtml(app.getDescription()).toString());
            }
        }
    }

    private void setupReadMore() {
        showLessMoreTxt.setOnClickListener(v -> {
            DetailsFragmentMore mDetailsFragmentMore = new DetailsFragmentMore();
            mDetailsFragmentMore.setApp(app);
            fragment.getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, mDetailsFragmentMore)
                    .addToBackStack("MORE")
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        });

    }
}