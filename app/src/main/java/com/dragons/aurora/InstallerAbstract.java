/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store (a fork of Yalp Store )is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.dragons.aurora;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import com.dragons.aurora.model.App;
import com.dragons.aurora.notification.IgnoreUpdatesService;
import com.dragons.aurora.notification.NotificationBuilder;
import com.dragons.aurora.notification.NotificationManagerWrapper;
import com.dragons.aurora.recievers.DetailsInstallReceiver;

import java.io.File;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import timber.log.Timber;

public abstract class InstallerAbstract {

    protected Context context;
    protected boolean background;

    public InstallerAbstract(Context context) {
        Timber.i("Installer chosen");
        this.context = context;
        background = !(context instanceof Activity);
    }

    static public Intent getOpenApkIntent(Context context, File file) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", file));
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    abstract protected void install(App app);

    public void setBackground(boolean background) {
        this.background = background;
    }

    public void verifyAndInstall(App app) {
        if (verify(app)) {
            Timber.i("Installing %s", app.getPackageName());
            install(app);
        } else {
            sendBroadcast(app.getPackageName(), false);
        }
    }

    protected boolean verify(App app) {
        File apkPath = Paths.getApkPath(context, app.getPackageName(), app.getVersionCode());
        if (!apkPath.exists()) {
            Timber.w("%s does not exist", apkPath.getAbsolutePath());
            return false;
        }
        if (!new ApkSignatureVerifier(context).match(app.getPackageName(), apkPath)) {
            Timber.i("Signature mismatch for %s", app.getPackageName());
            ((AuroraApplication) context.getApplicationContext()).removePendingUpdate(app.getPackageName());
            if (ContextUtil.isAlive(context)) {
                ((AppCompatActivity) context).runOnUiThread(() -> getSignatureMismatchDialog(app).show());
            } else {
                notifySignatureMismatch(app);
            }
            return false;
        }
        return true;
    }

    protected void notifyAndToast(int notificationStringId, int toastStringId, App app) {
        showNotification(notificationStringId, app);
        if (!background) {
            ContextUtil.toast(context, toastStringId, app.getDisplayName());
        }
    }

    protected void sendBroadcast(String packageName, boolean success) {
        Intent intent = new Intent(
                success
                        ? DetailsInstallReceiver.ACTION_PACKAGE_REPLACED_NON_SYSTEM
                        : DetailsInstallReceiver.ACTION_PACKAGE_INSTALLATION_FAILED
        );
        intent.setData(new Uri.Builder().scheme("package").opaquePart(packageName).build());
        context.sendBroadcast(intent);
    }

    private AlertDialog getSignatureMismatchDialog(final App app) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setMessage(R.string.details_signature_mismatch)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.cancel());
        if (new BlackWhiteListManager(context).isUpdatable(app.getPackageName())) {
            builder.setNegativeButton(
                    R.string.action_ignore, (dialog, id) -> {
                        context.startService(getIgnoreIntent(app));
                        dialog.cancel();
                    });
        }
        return builder.create();
    }

    private void notifySignatureMismatch(App app) {
        notifyAndToast(
                R.string.notification_download_complete_signature_mismatch,
                R.string.notification_download_complete_signature_mismatch_toast,
                app
        );
    }

    private void showNotification(int notificationStringId, App app) {
        File file = Paths.getApkPath(context, app.getPackageName(), app.getVersionCode());
        Intent openApkIntent = getOpenApkIntent(context, file);
        NotificationBuilder builder = NotificationManagerWrapper.getBuilder(context)
                .setIntent(openApkIntent)
                .setTitle(app.getDisplayName())
                .setMessage(context.getString(notificationStringId));
        if (new BlackWhiteListManager(context).isUpdatable(app.getPackageName())) {
            builder.addAction(
                    R.drawable.ic_cancel,
                    R.string.action_ignore,
                    PendingIntent.getService(context, 0, getIgnoreIntent(app), PendingIntent.FLAG_UPDATE_CURRENT)
            );
        }
        new NotificationManagerWrapper(context).show(app.getDisplayName(), builder.build());
    }

    private Intent getIgnoreIntent(App app) {
        Intent intentIgnore = new Intent(context, IgnoreUpdatesService.class);
        intentIgnore.putExtra(IgnoreUpdatesService.PACKAGE_NAME, app.getPackageName());
        intentIgnore.putExtra(IgnoreUpdatesService.VERSION_CODE, app.getVersionCode());
        return intentIgnore;
    }
}
