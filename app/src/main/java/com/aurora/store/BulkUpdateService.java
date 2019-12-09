package com.aurora.store;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.aurora.store.events.Event;
import com.aurora.store.exception.CredentialsEmptyException;
import com.aurora.store.exception.MalformedRequestException;
import com.aurora.store.exception.NotPurchasedException;
import com.aurora.store.exception.TooManyRequestsException;
import com.aurora.store.model.App;
import com.aurora.store.notification.QuickNotification;
import com.aurora.store.task.LiveUpdate;
import com.aurora.store.task.ObservableDeliveryData;
import com.aurora.store.util.Log;
import com.dragons.aurora.playstoreapiv2.AuthException;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class BulkUpdateService extends Service {
    public static BulkUpdateService instance = null;

    private List<App> appList = new ArrayList<>();
    private CompositeDisposable disposable = new CompositeDisposable();

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
        appList = AuroraApplication.getOngoingUpdateList();
        updateAllApps();
    }

    private void updateAllApps() {
        AuroraApplication.setBulkUpdateAlive(true);
        AuroraApplication.rxNotify(new Event(Event.SubType.BULK_UPDATE_NOTIFY));
        disposable.add(Observable.fromIterable(appList)
                .flatMap(app -> new ObservableDeliveryData(getApplicationContext()).getDeliveryData(app))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(deliveryDataBundle -> new LiveUpdate(getApplicationContext())
                        .enqueueUpdate(deliveryDataBundle.getApp(),
                                deliveryDataBundle.getAndroidAppDeliveryData()))
                .subscribe(deliveryDataBundle -> {
                }, err -> {
                    if (err instanceof MalformedRequestException || err instanceof NotPurchasedException) {
                        QuickNotification.show(getApplication(),
                                getString(R.string.action_updates),
                                err.getMessage(),
                                null);
                    }
                    processException(err);
                    Log.e(err.getMessage());
                }));
    }

    private void processException(Throwable e) {
        if (e instanceof CredentialsEmptyException) {
            AuroraApplication.rxNotify(new Event(Event.SubType.API_ERROR));
        } else if (e instanceof AuthException | e instanceof TooManyRequestsException) {
            AuroraApplication.rxNotify(new Event(Event.SubType.API_FAILED));
        } else if (e instanceof UnknownHostException) {
            AuroraApplication.rxNotify(new Event(Event.SubType.NETWORK_UNAVAILABLE));
        } else
            Log.e(e.getMessage());
        stopSelf();
    }

    @Override
    public void onDestroy() {
        AuroraApplication.setBulkUpdateAlive(false);
        AuroraApplication.rxNotify(new Event(Event.SubType.BULK_UPDATE_NOTIFY));
        instance = null;
        super.onDestroy();
    }
}
