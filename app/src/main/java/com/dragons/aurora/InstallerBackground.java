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

import android.content.Context;

import com.dragons.aurora.activities.DetailsActivity;
import com.dragons.aurora.model.App;
import com.dragons.aurora.notification.NotificationManagerWrapper;

import timber.log.Timber;

abstract public class InstallerBackground extends InstallerAbstract {

    private boolean wasInstalled;

    public InstallerBackground(Context context) {
        super(context);
    }

    @Override
    public boolean verify(App app) {
        if (!super.verify(app)) {
            return false;
        }
        if (background && !new PermissionsComparator(context).isSame(app)) {
            Timber.i("New permissions for %s", app.getPackageName());
            ((AuroraApplication) context.getApplicationContext()).removePendingUpdate(app.getPackageName());
            notifyNewPermissions(app);
            return false;
        }
        wasInstalled = app.isInstalled();
        return true;
    }

    protected void postInstallationResult(App app, boolean success) {
        String resultString = context.getString(
                success
                        ? (wasInstalled ? R.string.notification_installation_complete : R.string.details_installed)
                        : (wasInstalled ? R.string.notification_installation_failed : R.string.details_install_failure)
        );
        if (background) {
            new NotificationManagerWrapper(context).show(DetailsActivity.getDetailsIntent(context, app.getPackageName()), app.getDisplayName(), resultString);
        } else {
            ContextUtil.toastLong(context, resultString);
        }
        app.setInstalled(true);
    }

    private void notifyNewPermissions(App app) {
        notifyAndToast(
                R.string.notification_download_complete_new_permissions,
                R.string.notification_download_complete_new_permissions_toast,
                app
        );
    }
}
