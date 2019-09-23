package com.aurora.store;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.events.Event;
import com.aurora.store.events.Events;
import com.aurora.store.events.RxBus;
import com.aurora.store.utility.Log;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class AnonymousRefreshService extends Service {

    public static AnonymousRefreshService instance = null;

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
        refreshToken();
    }

    private void refreshToken() {
        disposable.add(Flowable.fromCallable(() -> PlayStoreApiAuthenticator
                .refreshToken(this))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscription -> {
                    RxBus.publish(new Event(Events.TOKEN_EXPIRED));
                })
                .subscribe((success) -> {
                    if (success) {
                        Log.i("Token Refreshed");
                        RxBus.publish(new Event(Events.TOKEN_REFRESHED));
                    } else {
                        Log.e("Token Refresh Failed Permanently");
                        RxBus.publish(new Event(Events.NET_DISCONNECTED));
                    }
                    destroyService();
                }, err -> {
                    Log.e("Token Refresh Login failed %s", err.getMessage());
                    RxBus.publish(new Event(Events.PERMANENT_FAIL));
                    destroyService();
                }));
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    private void destroyService() {
        Log.e("Self-update service destroyed");
        stopSelf();
    }
}
