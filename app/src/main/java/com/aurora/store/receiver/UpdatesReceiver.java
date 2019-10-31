package com.aurora.store.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.aurora.store.AuroraApplication;
import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.activity.AuroraActivity;
import com.aurora.store.model.App;
import com.aurora.store.notification.QuickNotification;
import com.aurora.store.task.LiveUpdate;
import com.aurora.store.task.ObservableDeliveryData;
import com.aurora.store.task.UpdatableAppsTask;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.Util;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class UpdatesReceiver extends BroadcastReceiver {

    private Context context;
    private CompositeDisposable disposable = new CompositeDisposable();

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
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
                        if (Util.isAutoUpdatesEnabled(context))
                            updateAllApps(appList);
                    }
                }, err -> Log.e("Update check failed")));
    }

    private void updateAllApps(List<App> updatableAppList) {
        AuroraApplication.setOngoingUpdateList(updatableAppList);
        AuroraApplication.setOnGoingUpdate(true);
        disposable.add(Observable.fromIterable(updatableAppList)
                .flatMap(app -> new ObservableDeliveryData(context).getDeliveryData(app))
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(disposable -> {
                    QuickNotification.show(context,
                            context.getString(R.string.action_updates),
                            context.getString(R.string.list_updating_background),
                            getContentIntent(context));
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(deliveryDataBundle -> new LiveUpdate(context)
                        .enqueueUpdate(deliveryDataBundle.getApp(),
                                deliveryDataBundle.getAndroidAppDeliveryData()))
                .doOnError(err -> {
                    QuickNotification.show(context,
                            context.getString(R.string.list_updating_failed),
                            err.getMessage() != null ? err.getMessage() : "",
                            getContentIntent(context));
                })
                .subscribe());
    }


    private PendingIntent getContentIntent(Context context) {
        Intent intent = new Intent(context, AuroraActivity.class);
        intent.putExtra(Constants.INTENT_FRAGMENT_POSITION, 1);
        return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
