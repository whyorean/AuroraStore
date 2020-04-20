package com.aurora.store.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.aurora.store.AuroraApplication;
import com.aurora.store.exception.CredentialsEmptyException;
import com.aurora.store.exception.TooManyRequestsException;
import com.aurora.store.util.ApiBuilderUtil;
import com.aurora.store.util.Log;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.TocResponse;

import java.net.UnknownHostException;

public class ApiValidator extends Worker {

    public static final String TAG = "API_VALIDATOR";
    public static final String ERROR_KEY = "ERROR_KEY";

    /*
     * -1 - No error
     * 0 - Credentials Empty
     * 1 - Too many requests/Session expired
     * 2 - Network Error
     * 3 - Unknown
     */
    private int errorCode = -1;

    public ApiValidator(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        Log.i("API Validator started");
    }

    private GooglePlayAPI buildApi() throws Exception {
        try {
            return ApiBuilderUtil.getPlayApi(getApplicationContext());
        } catch (Exception e) {
            if (e instanceof CredentialsEmptyException) {
                errorCode = 0;
                return null;
            } else if (e instanceof AuthException || e instanceof TooManyRequestsException) {
                errorCode = 1;
                return ApiBuilderUtil.generateApiWithNewAuthToken(getApplicationContext());
            } else if (e instanceof UnknownHostException) {
                errorCode = 2;
                return null;
            } else {
                errorCode = 3;
                return null;
            }
        }
    }

    private boolean testApi(GooglePlayAPI api) throws Exception {
        final TocResponse tocResponse = api.toc();
        return tocResponse.hasTosToken();
    }

    private Data getOutputData() {
        return new Data.Builder()
                .putInt(ERROR_KEY, errorCode)
                .build();
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            GooglePlayAPI googlePlayAPI = buildApi();

            if (googlePlayAPI == null) {

                return Result.failure(getOutputData());
            }

            if (testApi(googlePlayAPI)) {
                AuroraApplication.api = googlePlayAPI;
                return Result.success();
            } else {
                return Result.failure(getOutputData());
            }
        } catch (Exception e) {
            return Result.failure(getOutputData());
        }
    }
}
