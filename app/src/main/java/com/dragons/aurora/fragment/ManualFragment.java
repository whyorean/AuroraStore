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

package com.dragons.aurora.fragment;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.fragment.details.DownloadOrInstall;
import com.dragons.aurora.model.App;
import com.dragons.aurora.task.playstore.PurchaseCheckTask;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;

import static com.dragons.aurora.AuroraApplication.COLOR_UI;

public class ManualFragment extends BaseFragment {

    public static App app;
    private int latestVersionCode;
    private View view;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.fragment_manual, container, false);
        latestVersionCode = app.getVersionCode();
        draw(app);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        app.setVersionCode(latestVersionCode);
    }

    private void draw(App app) {
        drawDetails();

        if (app.getOfferType() == 0) {
            app.setOfferType(1);
        }

        ((TextView) view.findViewById(R.id.compatibility)).setText(app.getVersionCode() > 0
                ? R.string.manual_download_compatible
                : R.string.manual_download_incompatible);

        if (app.getVersionCode() > 0) {
            ((EditText) view.findViewById(R.id.version_code)).setHint(String.valueOf(latestVersionCode));
        }

        DownloadOrInstall downloadOrInstallFragment = new DownloadOrInstall(getContext(), view, app);
        ManualDownloadTextWatcher textWatcher = new ManualDownloadTextWatcher(app,
                view.findViewById(R.id.download),
                view.findViewById(R.id.install),
                downloadOrInstallFragment
        );
        String versionCode = Integer.toString(app.getVersionCode());
        textWatcher.onTextChanged(versionCode, 0, 0, versionCode.length());
        ((EditText) view.findViewById(R.id.version_code)).addTextChangedListener(textWatcher);
        downloadOrInstallFragment.registerReceivers();
        downloadOrInstallFragment.draw();
    }

    private void drawDetails() {
        ((TextView) view.findViewById(R.id.displayName)).setText(app.getDisplayName());
        ((TextView) view.findViewById(R.id.packageName)).setText(app.getPackageName());
        ((TextView) view.findViewById(R.id.versionString)).setText(String.valueOf(app.getVersionCode()));
        if (app.getPrice() != null && app.getPrice().isEmpty())
            ((TextView) view.findViewById(R.id.price)).setText(R.string.category_appFree);
        else
            ((TextView) view.findViewById(R.id.price)).setText(app.getPrice());
        ((TextView) view.findViewById(R.id.contains_ads)).setText(app.containsAds() ? R.string.details_contains_ads : R.string.details_no_ads);
        view.findViewById(R.id.app_menu3dot).setVisibility(View.GONE);
        ScrollView disclaimer = view.findViewById(R.id.disclaimer);
        ImageView showLessMore = view.findViewById(R.id.show_LessMore);
        showLessMore.setOnClickListener(v -> {
            if (disclaimer.getVisibility() == View.GONE) {
                disclaimer.setVisibility(View.VISIBLE);
                showLessMore.animate().rotation(180).start();
            } else {
                disclaimer.setVisibility(View.GONE);
                showLessMore.animate().rotation(0).start();
            }
        });

        ImageView appIcon = view.findViewById(R.id.icon);

        Glide.with(getContext())
                .asBitmap()
                .load(app.getIconInfo().getUrl())
                .apply(new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .placeholder(R.color.transparent))
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

    private void getPalette(Bitmap bitmap) {
        Palette.from(bitmap).generate(this::drawGradients);
    }

    private void drawGradients(Palette myPalette) {
        (view.findViewById(R.id.diagonalView1))
                .setBackground(Util.getGradient(myPalette.getLightVibrantColor(Color.LTGRAY),
                        myPalette.getDominantColor(Color.GRAY)));
        (view.findViewById(R.id.diagonalView2))
                .setBackground(Util.getGradient(myPalette.getLightVibrantColor(Color.GRAY),
                        myPalette.getVibrantColor(Color.DKGRAY)));
    }

    static private class ManualDownloadTextWatcher implements TextWatcher {

        static private final int TIMEOUT = 1000;

        private final App app;
        private final Button downloadButton;
        private final Button installButton;
        private DownloadOrInstall downloadOrInstallFragment;
        private Timer timer;

        ManualDownloadTextWatcher(App app,
                                  Button downloadButton,
                                  Button installButton,
                                  DownloadOrInstall downloadOrInstallFragment) {
            this.app = app;
            this.downloadButton = downloadButton;
            this.installButton = installButton;
            this.downloadOrInstallFragment = downloadOrInstallFragment;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            try {
                app.setVersionCode(Integer.parseInt(s.toString()));
                installButton.setVisibility(View.GONE);
                downloadButton.setText(R.string.details_download_checking);
                downloadButton.setEnabled(false);
                downloadButton.setVisibility(View.VISIBLE);
                restartTimer();
            } catch (NumberFormatException e) {
                Log.w(getClass().getSimpleName(), s.toString() + " is not a number");
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }

        private void restartTimer() {
            if (null != timer) {
                timer.cancel();
            }
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    getTask(timer).execute();
                }
            }, TIMEOUT);
        }

        private PurchaseCheckTask getTask(Timer timer) {
            PurchaseCheckTask task = new PurchaseCheckTask();
            task.setContext(downloadButton.getContext());
            task.setTimer(timer);
            task.setApp(app);
            task.setDownloadOrInstallFragment(downloadOrInstallFragment);
            task.setDownloadButton(downloadButton);
            return task;
        }
    }

}