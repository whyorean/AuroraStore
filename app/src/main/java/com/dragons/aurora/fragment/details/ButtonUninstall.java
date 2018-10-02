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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.R;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.model.App;
import com.dragons.aurora.task.CheckShellTask;
import com.dragons.aurora.task.UninstallSystemAppTask;
import com.dragons.aurora.task.UninstallTask;

public class ButtonUninstall extends Button {

    public ButtonUninstall(Context context, View view, App app) {
        super(context, view, app);
    }

    @Override
    protected View getButton() {
        return view.findViewById(R.id.uninstall);
    }

    @Override
    public boolean shouldBeVisible() {
        return isInstalled();
    }

    @Override
    protected void onButtonClick(View v) {
        uninstall();
    }

    public void uninstall() {
        if (Aurora.INSTALLATION_METHOD_AURORA.equals(Prefs.getString(context, Aurora.PREFERENCE_INSTALLATION_METHOD)))
            uncheckedUninstall();
        else
            checkedUninstall();
    }

    private void uncheckedUninstall() {
        new UninstallTask(context, app).execute();
    }

    private void checkedUninstall() {
        if (isSystemAndReadyForPermanentUninstall()) {
            askAndUninstall();
        } else {
            context.startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.getPackageName())));
        }
    }

    private boolean isSystemAndReadyForPermanentUninstall() {
        return null != app.getPackageInfo().applicationInfo
                && null != app.getPackageInfo().applicationInfo.sourceDir
                && app.getPackageInfo().applicationInfo.sourceDir.startsWith("/system/");
    }

    private void askAndUninstall() {
        CheckShellTask checkShellTask = new CheckShellTask(context);
        checkShellTask.setPrimaryTask(new UninstallSystemAppTask(context, app));
        checkShellTask.execute();
    }
}
