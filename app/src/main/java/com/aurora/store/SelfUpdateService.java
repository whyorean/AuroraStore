package com.aurora.store;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.aurora.store.download.DownloadManager;
import com.aurora.store.download.RequestBuilder;
import com.aurora.store.model.App;
import com.aurora.store.model.Update;
import com.aurora.store.task.NetworkTask;
import com.aurora.store.util.CertUtil;
import com.aurora.store.util.ContextUtil;
import com.aurora.store.util.Log;
import com.aurora.store.util.PackageUtil;
import com.google.gson.Gson;
import com.tonyodev.fetch2.AbstractFetchGroupListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.EnqueueAction;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchGroup;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Request;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class SelfUpdateService extends Service {

    public static SelfUpdateService instance = null;

    private CompositeDisposable disposable = new CompositeDisposable();
    private int hashCode = BuildConfig.APPLICATION_ID.hashCode();
    private App app;
    private Fetch fetch;
    private FetchListener fetchListener;
    private Gson gson;

    public static boolean isServiceRunning() {
        try {
            return instance != null && instance.isRunning();
        } catch (NullPointerException e) {
            return false;
        }
    }

    private boolean isRunning() {
        return true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForeground(1, getNotification());
        } else {
            Notification notification = getNotification(new NotificationCompat.Builder(this,
                    Constants.NOTIFICATION_CHANNEL_GENERAL));
            startForeground(1, notification);
        }
        startUpdate();
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    private void destroyService() {
        Log.d("Self-update service destroyed");
        if (fetchListener != null) {
            fetch.removeListener(fetchListener);
            fetchListener = null;
        }
        stopForeground(true);
        stopSelf();
    }

    private void startUpdate() {
        disposable.add(Observable.fromCallable(() -> new NetworkTask(this)
                .get(Constants.UPDATE_URL))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    try {
                        gson = new Gson();
                        Update update = gson.fromJson(response, Update.class);
                        if (update.getVersionCode() <= BuildConfig.VERSION_CODE) {
                            ContextUtil.toastLong(this, getString(R.string.list_empty_updates));
                            destroyService();
                        } else
                            downloadAndUpdate(update);
                    } catch (Exception e) {
                        Log.e("Error checking update");
                        destroyService();
                    }
                }));
    }

    private void downloadAndUpdate(Update update) {
        app = new App();
        app.setPackageName(BuildConfig.APPLICATION_ID);
        app.setDisplayName(getString(R.string.app_name));
        app.setVersionName(update.getVersionName());
        app.setVersionCode(update.getVersionCode());

        Request request = RequestBuilder.buildRequest(this, app, isFDroidVariant() ? update.getFdroidBuild() : update.getAuroraBuild());
        request.setEnqueueAction(EnqueueAction.REPLACE_EXISTING);
        List<Request> requestList = new ArrayList<>();
        requestList.add(request);

        fetch = DownloadManager.getFetchInstance(this);
        fetch.enqueue(requestList, result -> {
            Log.d("Downloading latest update");
        });

        fetchListener = getFetchListener();
        fetch.addListener(fetchListener);

        //Add <PackageName,DisplayName> and <PackageName,IconURL> to PseudoMaps
        PackageUtil.addToPseudoPackageMap(this, app.getPackageName(), app.getDisplayName());
        PackageUtil.addToPseudoURLMap(this, app.getPackageName(), Constants.APP_ICON_URL);
    }

    private boolean isFDroidVariant() {
        return CertUtil.isFDroidApp(this, BuildConfig.APPLICATION_ID);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private Notification getNotification() {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_GENERAL);
        return getNotification(notificationBuilder);
    }

    private Notification getNotification(NotificationCompat.Builder builder) {
        return builder
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setColor(getResources().getColor(R.color.colorAccent))
                .setContentTitle("Self update")
                .setContentText("Updating Aurora Store in background")
                .setOngoing(false)
                .setSmallIcon(R.drawable.ic_notification)
                .build();
    }

    private FetchListener getFetchListener() {
        return new AbstractFetchGroupListener() {
            @Override
            public void onError(int groupId, @NotNull Download download, @NotNull Error error,
                                @org.jetbrains.annotations.Nullable Throwable throwable, @NotNull FetchGroup fetchGroup) {
                if (groupId == hashCode) {
                    Log.e("Error self-updating %s", app.getDisplayName());
                    destroyService();
                }
            }

            @Override
            public void onCompleted(int groupId, @NotNull Download download, @NotNull FetchGroup fetchGroup) {
                if (groupId == hashCode && fetchGroup.getGroupDownloadProgress() == 100) {
                    AuroraApplication.getInstaller().install(app);
                    destroyService();
                }
            }

            @Override
            public void onCancelled(int groupId, @NotNull Download download, @NotNull FetchGroup fetchGroup) {
                if (groupId == hashCode) {
                    Log.d("Self-update cancelled %s", app.getDisplayName());
                    destroyService();
                }
            }
        };
    }
}
