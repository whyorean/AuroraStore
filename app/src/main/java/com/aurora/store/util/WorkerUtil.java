package com.aurora.store.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

public class WorkerUtil {

    public static Constraints getNetworkConstraints() {
        return new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
    }

    public static OneTimeWorkRequest getWorkRequest(@NonNull String tag, @NonNull Constraints constraints,
                                                    @NonNull Class workerClass) {
        return new OneTimeWorkRequest.Builder(workerClass)
                .setConstraints(constraints)
                .addTag(tag)
                .build();
    }

    public static void enqueue(@NonNull Context context, @NonNull LifecycleOwner lifecycleOwner,
                               @NonNull OneTimeWorkRequest workRequest, @NonNull Observer<WorkInfo> workInfoObserver) {
        WorkManager.getInstance(context).enqueue(workRequest);
        WorkManager.getInstance(context).getWorkInfoByIdLiveData(workRequest.getId())
                .observe(lifecycleOwner, workInfoObserver);
    }
}
