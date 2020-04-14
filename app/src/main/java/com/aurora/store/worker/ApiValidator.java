package com.aurora.store.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.aurora.store.AuroraApplication;
import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.util.Log;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.TocResponse;

public class ApiValidator extends Worker {

    public static final String TAG = "API_VALIDATOR";

    public ApiValidator(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        Log.i("API Validator started");
    }

    private GooglePlayAPI buildApi() throws Exception {
        final PlayStoreApiAuthenticator apiAuthenticator = new PlayStoreApiAuthenticator();
        return apiAuthenticator.getPlayApi(getApplicationContext());
    }

    private boolean testApi(GooglePlayAPI api) throws Exception {
        final TocResponse tocResponse = api.toc();
        return tocResponse.hasTosToken();
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            GooglePlayAPI googlePlayAPI = buildApi();
            if (testApi(googlePlayAPI)) {
                AuroraApplication.api = googlePlayAPI;
                return Result.success();
            } else
                return Result.failure();
        } catch (Exception e) {
            Log.e(e.getMessage());
            return Result.failure();
        }
    }
}
