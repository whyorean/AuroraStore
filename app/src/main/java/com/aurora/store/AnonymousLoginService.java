package com.aurora.store;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.events.Event;
import com.aurora.store.events.Events;
import com.aurora.store.events.RxBus;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.Log;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class AnonymousLoginService extends Service {

    public static AnonymousLoginService instance = null;

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
        login();
    }

    private void login() {
        disposable.add(Observable.fromCallable(() -> PlayStoreApiAuthenticator
                .login(this))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((api) -> {
                    if (api != null) {
                        Log.i("Anonymous Login Successful");
                        Accountant.setAnonymous(this, true);
                        RxBus.publish(new Event(Events.LOGGED_IN));
                    } else
                        Log.e("Anonymous Login Failed Permanently");
                    destroyService();
                }, err -> {
                    Log.e(err.getMessage());
                    destroyService();
                }));
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    private void destroyService() {
        Log.e("Anonymous login service destroyed");
        stopSelf();
    }
}