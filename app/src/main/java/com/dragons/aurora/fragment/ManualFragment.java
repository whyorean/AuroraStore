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

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.dragons.aurora.R;
import com.dragons.aurora.fragment.details.DownloadOrInstall;
import com.dragons.aurora.model.App;
import com.dragons.aurora.task.playstore.PurchaseCheckTask;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class ManualFragment extends BaseFragment {

    public static App app;

    @BindView(R.id.compatibility)
    TextView compatibility;
    @BindView(R.id.price)
    TextView price;
    @BindView(R.id.contains_ads)
    TextView contains_ads;
    @BindView(R.id.text_input)
    TextInputEditText text_input;
    @BindView(R.id.download)
    Button download;
    @BindView(R.id.install)
    Button install;
    @BindView(R.id.disclaimer)
    ScrollView disclaimer;
    @BindView(R.id.show_LessMore)
    ImageView showLessMore;
    @BindView(R.id.icon)
    ImageView appIcon;

    private int latestVersionCode;
    private DownloadOrInstall downloadOrInstallFragment;
    private View view;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.fragment_manual, container, false);
        ButterKnife.bind(this, view);
        latestVersionCode = app.getVersionCode();
        draw(app);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (null != downloadOrInstallFragment) {
            downloadOrInstallFragment.unregisterReceivers();
        }
        app.setVersionCode(latestVersionCode);
    }

    private void draw(App app) {
        drawDetails();

        if (app.getOfferType() == 0) {
            app.setOfferType(1);
        }

        if (app.getVersionCode() > 0) {
            text_input.setHint(String.valueOf(latestVersionCode));
        }

        compatibility.setText(app.getVersionCode() > 0
                ? R.string.manual_download_compatible
                : R.string.manual_download_incompatible);
        downloadOrInstallFragment = new DownloadOrInstall(getContext(), view, app);
        ManualDownloadTextWatcher textWatcher = new ManualDownloadTextWatcher(app, download, install,
                downloadOrInstallFragment);

        String versionCode = Integer.toString(app.getVersionCode());
        textWatcher.onTextChanged(versionCode, 0, 0, versionCode.length());

        text_input.addTextChangedListener(textWatcher);
        downloadOrInstallFragment.registerReceivers();
        downloadOrInstallFragment.draw();
    }

    private void drawDetails() {
        ((TextView) view.findViewById(R.id.displayName)).setText(app.getDisplayName());
        ((TextView) view.findViewById(R.id.packageName)).setText(app.getPackageName());
        ((TextView) view.findViewById(R.id.versionString)).setText(String.valueOf(app.getVersionCode()));

        if (app.getPrice() != null && app.getPrice().isEmpty())
            price.setText(R.string.category_appFree);
        else
            price.setText(app.getPrice());

        contains_ads.setText(app.containsAds() ? R.string.details_contains_ads : R.string.details_no_ads);
        view.findViewById(R.id.app_menu3dot).setVisibility(View.GONE);

        showLessMore.setOnClickListener(v -> {
            if (disclaimer.getVisibility() == View.GONE) {
                disclaimer.setVisibility(View.VISIBLE);
                showLessMore.animate().rotation(180).start();
            } else {
                disclaimer.setVisibility(View.GONE);
                showLessMore.animate().rotation(0).start();
            }
        });

        Glide
                .with(getContext())
                .load(app.getIconInfo().getUrl())
                .into(appIcon);
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
                Timber.w("%s is not a number", s.toString());
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