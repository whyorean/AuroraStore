package com.aurora.store;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.events.Event;
import com.aurora.store.exception.CredentialsEmptyException;
import com.aurora.store.exception.TooManyRequestsException;
import com.aurora.store.util.ApiBuilderUtil;
import com.aurora.store.util.Log;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.TocResponse;

import java.net.UnknownHostException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ValidateApiService extends Service {
    public static ValidateApiService instance = null;

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
        buildAndTestApi();
    }

    private void buildAndTestApi() {
        Log.d(getString(R.string.toast_api_build_api));
        disposable.clear();
        disposable.add(Observable.fromCallable(() -> new PlayStoreApiAuthenticator()
                .getPlayApi(this))
                .flatMap(api -> {
                    AuroraApplication.api = api;
                    return getTocResponse(api);
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    Log.d(getString(R.string.toast_api_all_ok));
                    AuroraApplication.rxNotify(new Event(Event.SubType.API_SUCCESS));
                    stopSelf();
                }, err -> {
                    Log.d(getString(R.string.toast_api_build_failed));
                    processException(err);
                })
        );
    }

    private void processException(Throwable e) {
        if (e instanceof CredentialsEmptyException) {
            AuroraApplication.rxNotify(new Event(Event.SubType.API_ERROR));
        } else if (e instanceof AuthException | e instanceof TooManyRequestsException) {
            AuroraApplication.rxNotify(new Event(Event.SubType.API_FAILED));
            getNewAuthToken();
        } else if (e instanceof UnknownHostException) {
            AuroraApplication.rxNotify(new Event(Event.SubType.NETWORK_UNAVAILABLE));
        } else
            Log.e(e.getMessage());
        stopSelf();
    }

    public void getNewAuthToken() {
        disposable.clear();
        disposable.add(Observable.fromCallable(() -> ApiBuilderUtil
                .generateApiWithNewAuthToken(this))
                .flatMap(api -> {
                    AuroraApplication.api = api;
                    return getTocResponse(api);
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    AuroraApplication.rxNotify(new Event(Event.SubType.API_SUCCESS));
                    stopSelf();
                }, err -> {
                    Log.e(err.getMessage());
                    AuroraApplication.rxNotify(new Event(Event.SubType.API_ERROR));
                })
        );
    }

    public Observable<TocResponse> getTocResponse(GooglePlayAPI api) {
        return Observable.create(emitter -> {
            TocResponse tocResponse = api.toc();
            emitter.onNext(tocResponse);
            emitter.onComplete();
        });
    }


    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }
}
