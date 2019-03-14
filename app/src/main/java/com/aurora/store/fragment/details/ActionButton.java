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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.aurora.store.R;
import com.aurora.store.activity.ManualDownloadActivity;
import com.aurora.store.download.DownloadManager;
import com.aurora.store.download.RequestBuilder;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.installer.Installer;
import com.aurora.store.model.App;
import com.aurora.store.notification.GeneralNotification;
import com.aurora.store.task.DeliveryData;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PathUtil;
import com.aurora.store.utility.Util;
import com.aurora.store.utility.ViewUtil;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;
import com.dragons.aurora.playstoreapiv2.Split;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.Status;
import com.tonyodev.fetch2core.DownloadBlock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static com.aurora.store.utility.ContextUtil.runOnUiThread;

public class ActionButton extends AbstractHelper {

    @BindView(R.id.btn_positive)
    Button btnPositive;
    @BindView(R.id.btn_negative)
    Button btnNegative;
    @BindView(R.id.viewSwitcher)
    ViewSwitcher mViewSwitcher;
    @BindView(R.id.view1)
    LinearLayout actions_layout;
    @BindView(R.id.view2)
    LinearLayout progress_layout;
    @BindView(R.id.progress_download)
    ProgressBar progressBar;
    @BindView(R.id.progress_txt)
    TextView progressTxt;
    @BindView(R.id.progress_status)
    TextView progressStatus;
    @BindView(R.id.btn_cancel)
    ImageButton btnCancel;

    private boolean isPaused;
    private boolean isSplit;
    private int requestId;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Fetch fetch;
    private FetchListener fetchListener;
    private GeneralNotification notification;

    private Request request;

    public ActionButton(DetailsFragment fragment, App app) {
        super(fragment, app);
        ButterKnife.bind(this, view);
    }

    public ActionButton(ManualDownloadActivity activity, App app) {
        super(activity, app);
        ButterKnife.bind(this, activity);
    }

    @Override
    public void draw() {
        ViewUtil.setVisibility(btnNegative, isInstalled());
        btnNegative.setOnClickListener(uninstallAppListener());
        btnPositive.setOnClickListener(downloadAppListener());
        btnCancel.setOnClickListener(cancelDownloadListener());

        if (!app.isFree()) {
            btnPositive.setText(R.string.details_purchase);
        }

        if (app.isInstalled())
            runOrUpdate();

        fetch = new DownloadManager(context).getFetchInstance();
        notification = new GeneralNotification(context, app);

        fetch.getDownloads(downloadList -> {
            for (Download download : downloadList) {
                if (download.getTag() != null && download.getTag().equals(app.getPackageName())) {
                    if (download.getStatus() == Status.NONE) {
                        btnPositive.setOnClickListener(downloadAppListener());
                    } else if (download.getStatus() == Status.COMPLETED && !app.isInstalled()) {
                        btnPositive.setOnClickListener(installAppListener());
                    } else if (download.getStatus() == Status.DOWNLOADING
                            || download.getStatus() == Status.QUEUED) {
                        switchViews(true);
                        request = download.getRequest();
                        fetch.resume(download.getId());
                        fetch.addListener(fetchListener = getFetchListener());
                    } else if (download.getStatus() == Status.PAUSED) {
                        btnPositive.setText(R.string.download_resume);
                        isPaused = true;
                        requestId = download.getId();
                    }
                }
            }
        });
    }

    private void switchViews(boolean showDownloads) {
        if (mViewSwitcher.getCurrentView() == actions_layout && showDownloads)
            mViewSwitcher.showNext();
        else if (mViewSwitcher.getCurrentView() == progress_layout && !showDownloads)
            mViewSwitcher.showPrevious();
    }

    private void runOrUpdate() {
        String versionName = app.getVersionName();
        if (TextUtils.isEmpty(versionName)) {
            return;
        }
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            String currentVersion = info.versionName;
            if (info.versionCode == app.getVersionCode() || null == currentVersion) {
                btnPositive.setText(R.string.details_run);
                btnPositive.setOnClickListener(openAppListener());
                return;
            } else if (new File(PathUtil.getLocalApkPath(context, app.getPackageName(),
                    app.getVersionCode())).exists()) {
                btnPositive.setOnClickListener(installAppListener());
            }
            btnPositive.setText(R.string.details_update);
        } catch (PackageManager.NameNotFoundException ignored) {
        }
    }

    private View.OnClickListener uninstallAppListener() {
        return v -> {
            Uri uri = Uri.fromParts("package", app.getPackageName(), null);
            Intent intent = new Intent();
            intent.setData(uri);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                intent.setAction(Intent.ACTION_DELETE);
            } else {
                intent.setAction(Intent.ACTION_UNINSTALL_PACKAGE);
                intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            }
            context.startActivity(intent);
        };
    }

    private View.OnClickListener installAppListener() {
        btnPositive.setText(R.string.details_install);
        return v -> {
            btnPositive.setText(R.string.details_installing);
            btnPositive.setEnabled(false);
            new Installer(context).install(app);
        };
    }

    private View.OnClickListener downloadAppListener() {
        btnPositive.setText(R.string.details_download);
        return v -> {
            switchViews(true);
            compositeDisposable.add(Observable.fromCallable(() -> new DeliveryData(context)
                    .getDeliveryData(app))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(deliveryData -> {
                        initiateDownload(deliveryData);
                    }, err -> {
                        Log.e(err.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(context, "App not available", Toast.LENGTH_LONG).show();
                            draw();
                            switchViews(false);
                        });
                    }));
        };
    }

    private View.OnClickListener openAppListener() {
        btnPositive.setText(R.string.details_run);
        return v -> {
            Intent i = getLaunchIntent();
            if (null != i) {
                try {
                    context.startActivity(i);
                } catch (ActivityNotFoundException e) {
                    Log.e(e.getMessage());
                }
            }
        };
    }

    private View.OnClickListener cancelDownloadListener() {
        return v -> {
            if (fetch != null && request != null)
                fetch.cancel(request.getId());
        };
    }

    private Intent getLaunchIntent() {
        Intent mIntent = context.getPackageManager().getLaunchIntentForPackage(app.getPackageName());
        boolean isTv = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isTv();
        if (isTv) {
            Intent l = context.getPackageManager()
                    .getLeanbackLaunchIntentForPackage(app.getPackageName());
            if (null != l) {
                mIntent = l;
            }
        }
        if (mIntent == null) {
            return null;
        }
        mIntent.addCategory(isTv ? Intent.CATEGORY_LEANBACK_LAUNCHER : Intent.CATEGORY_LAUNCHER);
        return mIntent;
    }

    private boolean isTv() {
        int uiMode = context.getResources().getConfiguration().uiMode;
        return (uiMode & Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    private void initiateDownload(AndroidAppDeliveryData deliveryData) {
        List<Split> splitList = deliveryData.getSplitList();
        List<Request> requestList = new ArrayList<>();

        if (!splitList.isEmpty()) {
            isSplit = true;
            requestList = RequestBuilder.buildSplitRequestList(context, app, splitList);
        }

        request = RequestBuilder.buildRequest(context, app, deliveryData.getDownloadUrl());
        fetchListener = getFetchListener();
        fetch.addListener(fetchListener);

        if (isPaused)
            fetch.resume(requestId);
        else
            fetch.enqueue(request, updatedRequest -> {
                Log.i("Downloading : %s", app.getPackageName());
            }, error -> {
                Log.e(Objects.requireNonNull(error.getThrowable()).getMessage() != null
                        ? error.getThrowable().getMessage()
                        : "Unknown error occurred while fetching APK");
            });

        if (isSplit) {
            fetch.enqueue(requestList, updatedRequestList -> {
                Log.i("Downloading Splits : %s", app.getPackageName());
            });
        }
    }

    private FetchListener getFetchListener() {
        return new FetchListener() {

            @Override
            public void onWaitingNetwork(@NotNull Download download) {

            }

            @Override
            public void onStarted(@NotNull Download download,
                                  @NotNull List<? extends DownloadBlock> list, int i) {
                if (download.getId() == request.getId()) {
                    progressBar.setIndeterminate(false);
                    switchViews(true);
                    progressStatus.setText(R.string.download_queued);
                }
            }

            @Override
            public void onResumed(@NotNull Download download) {
                if (download.getId() == request.getId()) {
                    notification.notifyProgress(download.getProgress(), 0,
                            request.getId());
                    progressBar.setIndeterminate(false);
                }
            }

            @Override
            public void onRemoved(@NotNull Download download) {
            }

            @Override
            public void onQueued(@NotNull Download download, boolean waitingOnNetwork) {
                if (waitingOnNetwork)
                    Log.d("Waiting on network");
                if (download.getId() == request.getId()) {
                    notification.notifyQueued();
                    progressStatus.setText(R.string.download_queued);
                }
            }

            @Override
            public void onProgress(@NotNull Download download, long etaInMilliSeconds,
                                   long downloadedBytesPerSecond) {
                if (download.getId() == request.getId()) {
                    btnCancel.setVisibility(View.VISIBLE);
                    notification.notifyProgress(download.getProgress(), downloadedBytesPerSecond,
                            request.getId());
                    //Set intermediate to false, just in case xD
                    if (progressBar.isIndeterminate())
                        progressBar.setIndeterminate(false);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        progressBar.setProgress(download.getProgress(), true);
                    } else
                        progressBar.setProgress(download.getProgress());
                    progressStatus.setText(R.string.download_progress);
                    progressTxt.setText(new StringBuilder().append(download.getProgress()).append("%"));
                }
            }

            @Override
            public void onPaused(@NotNull Download download) {
                if (download.getId() == request.getId()) {
                    notification.notifyResume(request.getId());
                    progressStatus.setText(R.string.download_paused);
                }
            }

            @Override
            public void onError(@NotNull Download download, @NotNull Error error, @Nullable Throwable throwable) {
                notification.notifyFailed();
            }

            @Override
            public void onDownloadBlockUpdated(@NotNull Download download, @NotNull DownloadBlock downloadBlock, int i) {

            }

            @Override
            public void onDeleted(@NotNull Download download) {

            }

            @Override
            public void onCompleted(@NotNull Download download) {
                if (download.getId() == request.getId()) {
                    notification.notifyCompleted();
                    progressStatus.setText(R.string.download_completed);
                    switchViews(false);
                    btnPositive.setOnClickListener(installAppListener());

                    // Check for AutoInstall & Disable InstallButton
                    if (Util.shouldAutoInstallApk(context)) {
                        btnPositive.setText(R.string.details_installing);
                        btnPositive.setEnabled(false);
                        new Installer(context).install(app);
                    }

                    // Finally Remove FetchListener
                    fetch.removeListener(fetchListener);
                }
            }

            @Override
            public void onCancelled(@NotNull Download download) {
                if (download.getId() == request.getId()) {
                    notification.notifyCancelled();
                    progressBar.setIndeterminate(true);
                    progressStatus.setText(R.string.download_canceled);
                    fetch.remove(download.getId());
                    fetch.delete(download.getId());
                    fetch.removeListener(fetchListener);
                    switchViews(false);
                }
            }

            @Override
            public void onAdded(@NotNull Download download) {

            }
        };
    }
}
