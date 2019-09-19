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

package com.aurora.store.fragment.details;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.util.Linkify;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.manager.CategoryManager;
import com.aurora.store.model.App;
import com.aurora.store.sheet.MoreInfoSheet;
import com.aurora.store.utility.ColorUtil;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.TextUtil;
import com.aurora.store.utility.ThemeUtil;
import com.aurora.store.utility.Util;
import com.aurora.store.utility.ViewUtil;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GeneralDetails extends AbstractHelper {
    @BindView(R.id.icon)
    ImageView appIcon;
    @BindView(R.id.devName)
    TextView txtDevName;
    @BindView(R.id.app_desc_short)
    TextView txtDescShort;
    @BindView(R.id.img_more)
    ImageButton imgMore;
    @BindView(R.id.versionString)
    TextView app_version;
    @BindView(R.id.txt_new)
    TextView txtNew;
    @BindView(R.id.txt_updated)
    Chip txtUpdated;
    @BindView(R.id.txt_google_dependencies)
    Chip txtDependencies;
    @BindView(R.id.txt_rating)
    Chip txtRating;
    @BindView(R.id.txt_installs)
    Chip txtInstalls;
    @BindView(R.id.txt_size)
    Chip txtSize;
    @BindView(R.id.category)
    Chip category;
    @BindView(R.id.developer_layout)
    LinearLayout developerLayout;
    @BindView(R.id.btn_positive)
    MaterialButton btnPositive;
    @BindView(R.id.btn_negative)
    MaterialButton btnNegative;

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
            GlideApp.with(context)
                    .asBitmap()
                    .load(app.getIconInfo().getUrl())
                    .transition(new BitmapTransitionOptions().crossFade())
                    .transforms(new CenterCrop(), new RoundedCorners(50))
                    .listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            getPalette(resource);
                            return false;
                        }
                    })
                    .into(appIcon);
            setText(view, R.id.displayName, app.getDisplayName());
            setText(view, R.id.packageName, app.getPackageName());
            setText(view, R.id.devName, app.getDeveloperName());
            txtDevName.setOnClickListener(v -> showDevApps());
            drawVersion();
        }
    }

    private void getPalette(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette != null)
                paintEmAll(palette);
        });
    }

    private void paintEmAll(Palette palette) {
        Palette.Swatch swatch = palette.getDarkVibrantSwatch();
        int colorPrimary = Color.GRAY;
        int colorPrimaryText = Color.BLACK;

        //Make sure we get a fallback swatch if DarkVibrantSwatch is not available
        if (swatch == null)
            swatch = palette.getVibrantSwatch();

        //Make sure we get another fallback swatch if VibrantSwatch is not available
        if (swatch == null)
            swatch = palette.getDominantSwatch();

        if (swatch != null) {
            colorPrimary = swatch.getRgb();
            colorPrimaryText = ColorUtil.manipulateColor(colorPrimary, 0.3f);
        }

        if (ColorUtil.isColorLight(colorPrimary))
            btnPositive.setTextColor(Color.BLACK);
        else
            btnPositive.setTextColor(Color.WHITE);

        btnPositive.setBackgroundColor(colorPrimary);
        btnPositive.setStrokeColor(ColorStateList.valueOf(colorPrimary));

        if (ThemeUtil.isLightTheme(context)) {
            txtDevName.setTextColor(colorPrimaryText);
            txtNew.setTextColor(colorPrimaryText);
            txtDescShort.setTextColor(colorPrimaryText);
            txtDescShort.setBackgroundTintList(ColorStateList.valueOf(ColorUtils.setAlphaComponent(colorPrimary, 60)));
        }
    }

    private void drawGeneralDetails() {
        if (context != null) {
            if (app.isEarlyAccess()) {
                setText(view, R.id.rating, R.string.early_access);
            } else {
                setText(view, R.id.rating, R.string.details_rating, app.getRating().getAverage());
            }

            final String category = new CategoryManager(context).getCategoryName(app.getCategoryId());
            if (TextUtil.isEmpty(category))
                hide(view, R.id.category);
            else
                setText(view, R.id.category, new CategoryManager(context).getCategoryName(app.getCategoryId()));

            if (app.getPrice() != null && app.getPrice().isEmpty())
                setText(view, R.id.price, R.string.category_appFree);
            else
                setText(view, R.id.price, app.getPrice());
            setText(view, R.id.contains_ads, app.containsAds() ? R.string.details_contains_ads : R.string.details_no_ads);

            txtUpdated.setText(app.getUpdated());
            txtDependencies.setText(app.getDependencies().isEmpty()
                    ? R.string.list_app_independent_from_gsf
                    : R.string.list_app_depends_on_gsf);
            txtRating.setText(app.getLabeledRating());
            txtInstalls.setText(app.getInstalls() <= 100 ? "Unknown" : Util.addDiPrefix(app.getInstalls()));
            txtSize.setText(app.getSize() == 0 ? "Unknown" : Formatter.formatShortFileSize(context, app.getSize()));
            setText(view, R.id.app_desc_short, TextUtil.emptyIfNull(app.getShortDescription()));

            drawOfferDetails();
            drawChanges();

            show(view, R.id.app_desc_short);
            show(view, R.id.layout_main);
        }
    }

    private void drawChanges() {
        String changes = app.getChanges();
        if (TextUtil.isEmpty(changes))
            setText(view, R.id.txt_changelog, context.getString(R.string.details_no_changes));
        else
            setText(view, R.id.txt_changelog, Html.fromHtml(changes).toString());
        show(view, R.id.changes_container);
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
            Log.w("System WebView missing: %s", e.getMessage());
            itemView.setAutoLinkMask(0);
            itemView.setText(context.getString(R.string.two_items, key, Html.fromHtml(value)));
        }
        itemView.setTextColor(ViewUtil.getStyledAttribute(context, android.R.attr.textColorPrimary));
        developerLayout.addView(itemView);
        ViewUtil.setVisibility(developerLayout, true);
    }

    private void drawVersion() {
        String versionName = app.getVersionName();
        DefaultArtifactVersion defaultArtifactVersion = new DefaultArtifactVersion(versionName);
        int versionCode = app.getVersionCode();

        if (TextUtils.isEmpty(versionName)) {
            return;
        }

        app_version.setText(new StringBuilder()
                .append(versionName)
                .append(".")
                .append(versionCode));
        app_version.setVisibility(View.VISIBLE);
        app_version.setSelected(true);

        if (!app.isInstalled()) {
            return;
        }

        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            String currentVersion = info.versionName;
            DefaultArtifactVersion currentVersionName = new DefaultArtifactVersion(info.versionName);

            int currentVersionCode = info.versionCode;
            boolean updatable = false;

            if (currentVersionName.compareTo(defaultArtifactVersion) < 0) {
                updatable = true;
            } else if (currentVersionName.compareTo(defaultArtifactVersion) == 0
                    && currentVersionCode < versionCode) {
                updatable = true;
            }

            if (updatable)
                app_version.setText(new StringBuilder()
                        .append(currentVersion)
                        .append(".")
                        .append(currentVersionCode)
                        .append(" >> ")
                        .append(versionName).append(".").append(versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            // We've checked for that already
        }
    }

    private void drawDescription() {
        if (context != null) {
            if (TextUtils.isEmpty(app.getDescription())) {
                hide(view, R.id.more_layout);
            } else {
                show(view, R.id.more_layout);
            }
        }
    }

    private void setupReadMore() {
        imgMore.setOnClickListener(v -> {
            MoreInfoSheet mDetailsFragmentMore = new MoreInfoSheet();
            mDetailsFragmentMore.setApp(app);
            mDetailsFragmentMore.show(fragment.getChildFragmentManager(), "MORE");
        });
    }
}