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

import android.content.Intent;
import android.net.Uri;
import android.view.View;

import com.dragons.aurora.R;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.model.App;
import com.dragons.aurora.task.CheckShellTask;
import com.dragons.aurora.task.UninstallSystemAppTask;

public class ButtonUninstall extends Button {

    public ButtonUninstall(AuroraActivity activity, App app) {
        super(activity, app);
    }

    @Override
    protected View getButton() {
        return activity.findViewById(R.id.uninstall);
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
        if (isSystemAndReadyForPermanentUninstall()) {
            askAndUninstall();
        } else {
            activity.startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.getPackageName())));
        }
        View buttonRun = activity.findViewById(R.id.run);
        if (buttonRun != null)
            buttonRun.setVisibility(View.GONE);
    }

    private boolean isSystemAndReadyForPermanentUninstall() {
        return app.isSystem()
                && null != app.getPackageInfo().applicationInfo
                && null != app.getPackageInfo().applicationInfo.sourceDir
                && app.getPackageInfo().applicationInfo.sourceDir.startsWith("/system/")
                ;
    }

    private void askAndUninstall() {
        CheckShellTask checkShellTask = new CheckShellTask(activity);
        checkShellTask.setPrimaryTask(new UninstallSystemAppTask(activity, app));
        checkShellTask.execute();
    }
}
