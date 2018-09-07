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

package com.dragons.aurora.fragment.details;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.dragons.aurora.BlackWhiteListManager;
import com.dragons.aurora.BuildConfig;
import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.InstalledApkCopier;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.activities.ManualDownloadActivity;
import com.dragons.aurora.builders.FlagDialogBuilder;
import com.dragons.aurora.fragment.ManualFragment;
import com.dragons.aurora.model.App;
import com.dragons.aurora.task.CheckShellTask;
import com.dragons.aurora.task.ConvertToNormalTask;
import com.dragons.aurora.task.ConvertToSystemTask;
import com.dragons.aurora.task.SystemRemountTask;

public class DownloadOptions extends Abstract {


    public DownloadOptions(Context context, View view, App app) {
        super(context, view, app);
    }

    @Override
    public void draw() {
    }

    public void inflate(Menu menu) {
        MenuInflater inflater = ((AuroraActivity) context).getMenuInflater();
        inflater.inflate(R.menu.menu_download, menu);
        if (!isInstalled(app)) {
            return;
        }
        menu.findItem(R.id.action_get_local_apk).setVisible(true);
        menu.findItem(R.id.action_manual).setVisible(true);
        BlackWhiteListManager manager = new BlackWhiteListManager(context);
        boolean isContained = manager.contains(app.getPackageName());
        if (manager.isBlack()) {
            menu.findItem(R.id.action_unignore).setVisible(isContained);
            menu.findItem(R.id.action_ignore).setVisible(!isContained);
        } else {
            menu.findItem(R.id.action_unwhitelist).setVisible(isContained);
            menu.findItem(R.id.action_whitelist).setVisible(!isContained);
        }
        if (isConvertible(app)) {
            menu.findItem(R.id.action_make_system).setVisible(!app.isSystem());
            menu.findItem(R.id.action_make_normal).setVisible(app.isSystem());
        }
        menu.findItem(R.id.action_flag).setVisible(app.isInstalled());
    }

    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_ignore:
            case R.id.action_whitelist:
                new BlackWhiteListManager(context).add(app.getPackageName());
                draw();
                return true;
            case R.id.action_unignore:
            case R.id.action_unwhitelist:
                new BlackWhiteListManager(context).remove(app.getPackageName());
                draw();
                return true;
            case R.id.action_manual:
                ManualFragment.app = app;
                context.startActivity(new Intent(context, ManualDownloadActivity.class));
                return true;
            case R.id.action_get_local_apk:
                new CopyTask((AuroraActivity) context).execute(app);
                return true;
            case R.id.action_make_system:
                checkAndExecute(new ConvertToSystemTask(context, app));
                return true;
            case R.id.action_make_normal:
                checkAndExecute(new ConvertToNormalTask(context, app));
                return true;
            case R.id.action_flag:
                new FlagDialogBuilder().setActivity((AuroraActivity) context).setApp(app).build().show();
                return true;
            default:
                return ((AuroraActivity) context).onContextItemSelected(item);
        }
    }

    private void checkAndExecute(SystemRemountTask primaryTask) {
        CheckShellTask checkShellTask = new CheckShellTask(context);
        checkShellTask.setPrimaryTask(primaryTask);
        checkShellTask.execute();
    }

    private boolean isConvertible(App app) {
        return isInstalled(app)
                && !app.getPackageName().equals(BuildConfig.APPLICATION_ID)
                && null != app.getPackageInfo().applicationInfo
                && null != app.getPackageInfo().applicationInfo.sourceDir
                && !app.getPackageInfo().applicationInfo.sourceDir.endsWith("pkg.apk")
                ;
    }

    private boolean isInstalled(App app) {
        try {
            context.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static class CopyTask extends AsyncTask<App, Void, Boolean> {

        @SuppressLint("StaticFieldLeak")
        private Activity activity;
        private App app;

        CopyTask(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            ContextUtil.toastLong(
                    activity.getApplicationContext(),
                    activity.getString(result
                            ? R.string.details_saved_in_downloads
                            : R.string.details_could_not_copy_apk
                    )
            );
        }

        @Override
        protected Boolean doInBackground(App... apps) {
            app = apps[0];
            return InstalledApkCopier.copy(activity, app);
        }
    }
}
