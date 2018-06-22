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

package com.dragons.aurora.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.fragment.details.DownloadOrInstall;
import com.dragons.aurora.model.App;
import com.dragons.aurora.task.playstore.PurchaseCheckTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.Timer;
import java.util.TimerTask;

public class ManualDownloadActivity extends DetailsActivity {

    public static App app;
    private int latestVersionCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(getResources().getColor(R.color.semi_transparent));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (null == app) {
            Log.e(getClass().getSimpleName(), "No app stored");
            finish();
            return;
        }
        latestVersionCode = app.getVersionCode();
        draw(app);
    }

    @Override
    protected void onPause() {
        super.onPause();
        app.setVersionCode(latestVersionCode);
    }

    private void draw(App app) {
        setTitle(app.getDisplayName());
        setContentView(R.layout.manual_download_activity_layout);
        drawDetails();

        if (app.getOfferType() == 0) {
            app.setOfferType(1);
        }

        ((TextView) findViewById(R.id.compatibility)).setText(app.getVersionCode() > 0
                ? R.string.manual_download_compatible
                : R.string.manual_download_incompatible);

        if (app.getVersionCode() > 0) {
            ((EditText) findViewById(R.id.version_code)).setHint(String.valueOf(latestVersionCode));
        }

        DownloadOrInstall downloadOrInstallFragment = new DownloadOrInstall(this, app);
        ManualDownloadTextWatcher textWatcher = new ManualDownloadTextWatcher(app,
                findViewById(R.id.download),
                findViewById(R.id.install),
                downloadOrInstallFragment
        );
        String versionCode = Integer.toString(app.getVersionCode());
        textWatcher.onTextChanged(versionCode, 0, 0, versionCode.length());
        ((EditText) findViewById(R.id.version_code)).addTextChangedListener(textWatcher);
        downloadOrInstallFragment.registerReceivers();
        downloadOrInstallFragment.draw();
    }

    private void drawDetails() {
        ((TextView) findViewById(R.id.displayName)).setText(app.getDisplayName());
        ((TextView) findViewById(R.id.packageName)).setText(app.getPackageName());
        ((TextView) findViewById(R.id.versionString)).setText(String.valueOf(app.getVersionCode()));
        if (app.getPrice() != null && app.getPrice().isEmpty())
            ((TextView) findViewById(R.id.price)).setText(R.string.category_appFree);
        else
            ((TextView) findViewById(R.id.price)).setText(app.getPrice());
        ((TextView) findViewById(R.id.contains_ads)).setText(app.containsAds() ? R.string.details_contains_ads : R.string.details_no_ads);
        findViewById(R.id.app_menu3dot).setVisibility(View.GONE);
        ScrollView disclaimer = findViewById(R.id.disclaimer);
        ImageView showLessMore = findViewById(R.id.show_LessMore);
        showLessMore.setOnClickListener(v -> {
            if (disclaimer.getVisibility() == View.GONE) {
                disclaimer.setVisibility(View.VISIBLE);
                showLessMore.animate().rotation(180).start();
            } else {
                disclaimer.setVisibility(View.GONE);
                showLessMore.animate().rotation(0).start();
            }
        });

        ImageView appIcon = findViewById(R.id.icon);
        Picasso
                .with(this)
                .load(app.getIconInfo().getUrl())
                .placeholder(R.color.transparent)
                .into((ImageView) findViewById(R.id.icon), new Callback() {
                    @Override
                    public void onSuccess() {
                        Bitmap bitmap = ((BitmapDrawable) appIcon.getDrawable()).getBitmap();
                        if (bitmap != null && Util.getBoolean(appIcon.getContext(), "COLOR_UI"))
                            getPalette(bitmap);
                    }

                    @Override
                    public void onError() {

                    }
                });
    }

    private void getPalette(Bitmap bitmap) {
        Palette.from(bitmap).generate(this::drawGradients);
    }

    private void drawGradients(Palette myPalette) {
        (findViewById(R.id.diagonalView1))
                .setBackground(Util.getGradient(myPalette.getLightVibrantColor(Color.LTGRAY),
                        myPalette.getDominantColor(Color.GRAY)));
        (findViewById(R.id.diagonalView2))
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
