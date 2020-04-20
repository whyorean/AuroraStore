package com.aurora.store.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.aurora.store.AuroraApplication;
import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.notification.QuickNotification;
import com.aurora.store.task.LiveUpdate;
import com.aurora.store.task.ObservableDeliveryData;
import com.aurora.store.task.UpdatableAppsTask;
import com.aurora.store.ui.main.AuroraActivity;
import com.aurora.store.util.ApiBuilderUtil;
import com.aurora.store.util.Log;
import com.aurora.store.util.TextUtil;
import com.aurora.store.util.Util;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class UpdatesReceiver extends BroadcastReceiver {

    private Context context;
    private CompositeDisposable disposable = new CompositeDisposable();

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        Log.i("Update check Started");

        disposable.add(Observable.fromCallable(() -> ApiBuilderUtil.getPlayApi(context))
                .subscribeOn(Schedulers.io())
                .map(api -> new UpdatableAppsTask(AuroraApplication.api, context).getUpdatableApps())
                .subscribe(apps -> {
                    if (apps.isEmpty()) {
                        Log.e(context.getString(R.string.list_empty_updates));
                    } else {
                        QuickNotification.show(context,
                                context.getString(R.string.action_updates),
                                StringUtils.joinWith(StringUtils.SPACE, apps.size(), context.getString(R.string.list_update_all_txt)),
                                getContentIntent(context));

                        if (Util.isAutoUpdatesEnabled(context))
                            updateAllApps(apps);
                    }
                }, throwable -> Log.e("Update check failed : %s", throwable.getMessage())));
    }

    private void updateAllApps(List<App> updatableAppList) {
        AuroraApplication.setOngoingUpdateList(updatableAppList);
        AuroraApplication.setBulkUpdateAlive(true);

        QuickNotification.show(context,
                context.getString(R.string.action_updates),
                context.getString(R.string.list_updating_background),
                getContentIntent(context));

        disposable.add(Observable.fromIterable(updatableAppList)
                .subscribeOn(Schedulers.io())
                .flatMap(app -> new ObservableDeliveryData(context).getDeliveryData(app))
                .doOnNext(bundle -> new LiveUpdate(context).enqueueUpdate(bundle.getApp(), bundle.getAndroidAppDeliveryData()))
                .doOnError(err -> {
                    QuickNotification.show(context,
                            context.getString(R.string.list_updating_failed),
                            TextUtil.emptyIfNull(err.getMessage()),
                            getContentIntent(context));
                })
                .subscribe());
    }

    private PendingIntent getContentIntent(Context context) {
        final Intent intent = new Intent(context, AuroraActivity.class);
        intent.putExtra(Constants.INTENT_FRAGMENT_POSITION, 1);
        return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
