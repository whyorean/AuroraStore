package com.aurora.store.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.activity.AuroraActivity;
import com.aurora.store.notification.QuickNotification;
import com.aurora.store.task.UpdatableAppsTask;
import com.aurora.store.utility.Log;

import org.apache.commons.lang3.StringUtils;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class UpdatesReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("Update check Started");
        CompositeDisposable disposable = new CompositeDisposable();
        UpdatableAppsTask updatableAppTask = new UpdatableAppsTask(context);
        disposable.add(Observable.fromCallable(updatableAppTask::getUpdatableApps)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((appList) -> {
                    StringBuilder msg;
                    if (!appList.isEmpty()) {
                        msg = new StringBuilder()
                                .append(appList.size())
                                .append(StringUtils.SPACE)
                                .append(context.getString(R.string.list_update_all_txt));

                        QuickNotification.show(context,
                                context.getString(R.string.action_updates),
                                msg.toString(),
                                getContentIntent(context));
                    }
                }, err -> Log.e("Update check failed")));
    }

    private PendingIntent getContentIntent(Context context) {
        Intent intent = new Intent(context, AuroraActivity.class);
        intent.putExtra(Constants.INTENT_FRAGMENT_POSITION, 1);
        return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
