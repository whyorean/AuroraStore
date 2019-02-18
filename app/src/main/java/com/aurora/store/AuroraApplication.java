package com.aurora.store;

import android.app.Application;

import com.aurora.store.utility.Log;

import io.reactivex.plugins.RxJavaPlugins;

public class AuroraApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        RxJavaPlugins.setErrorHandler(err -> Log.e(err.getMessage()));
    }
}
