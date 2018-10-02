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

package com.dragons.aurora.task;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;

import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.SearchActivity;
import com.dragons.aurora.builders.SystemRemountDialogBuilder;
import com.scottyab.rootbeer.RootBeer;

import androidx.appcompat.app.AlertDialog;
import eu.chainfire.libsuperuser.Shell;
import timber.log.Timber;

public class CheckShellTask extends TaskWithProgress<Boolean> {

    private SystemRemountTask primaryTask;
    private boolean isSUAvail = false;
    private boolean isBBAvail = false;

    public CheckShellTask(Context context) {
        setContext(context);
    }

    public void setPrimaryTask(SystemRemountTask primaryTask) {
        this.primaryTask = primaryTask;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        RootBeer mRootBeer = new RootBeer(context);

        isBBAvail = mRootBeer.checkForBusyBoxBinary();
        isSUAvail = mRootBeer.isRooted();

        if (!isBBAvail) {
            return false;
        }

        if (!isSUAvail)
            return false;

        if (!Shell.SU.available())
            return false;

        Timber.i("Root & Busybox is available ");
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (!result || !ContextUtil.isAlive(context)) {
            ContextUtil.toast(context.getApplicationContext(), R.string.pref_no_root);
        } else if (!isBBAvail) {
            showBusyboxDialog();
        } else {
            primaryTask.setBusybox(true);
            askAndExecute(primaryTask);
        }
    }

    private void showBusyboxDialog() {
        new AlertDialog.Builder(context, R.style.ThemeOverlay_MaterialComponents_Dialog)
                .setMessage(R.string.dialog_message_busybox_not_available)
                .setTitle(R.string.dialog_title_busybox_not_available)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    context.startActivity(getBusyboxSearchIntent());
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.dialog_two_factor_cancel, (dialog, which) -> dialog.dismiss())
                .show()
        ;
    }

    private Intent getBusyboxSearchIntent() {
        Intent i = new Intent(context, SearchActivity.class);
        i.setAction(Intent.ACTION_SEARCH);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra(SearchManager.QUERY, "busybox");
        return i;
    }

    private void askAndExecute(SystemRemountTask task) {
        new SystemRemountDialogBuilder(context, R.style.ThemeOverlay_MaterialComponents_Dialog)
                .setPrimaryTask(task)
                .show();
    }
}
