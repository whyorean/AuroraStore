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

import android.content.Context;

import com.dragons.aurora.R;
import com.dragons.aurora.model.App;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import eu.chainfire.libsuperuser.Shell;
import timber.log.Timber;

public abstract class SystemRemountTask extends TaskWithProgress<List<String>> {

    static private final String MOUNT_RW = "mount -o rw,remount,rw /system";
    static private final String MOUNT_RO = "mount -o ro,remount,ro /system";

    protected App app;
    protected boolean busybox;

    public SystemRemountTask(Context context, App app) {
        this.context = context;
        this.app = app;
    }

    abstract protected List<String> getCommands();

    public App getApp() {
        return app;
    }

    public void setBusybox(boolean busybox) {
        this.busybox = busybox;
    }

    @Override
    protected List<String> doInBackground(String... params) {
        List<String> commands = new ArrayList<>();
        commands.add(getBusyboxCommand(MOUNT_RW));
        commands.addAll(getCommands());
        commands.add(getBusyboxCommand(MOUNT_RO));
        return Shell.SU.run(commands);
    }

    @Override
    protected void onPostExecute(List<String> output) {
        super.onPostExecute(output);
        if (null != output) {
            for (String outputLine : output) {
                Timber.i(outputLine);
            }
            showRebootDialog();
        }
    }

    protected String getBusyboxCommand(String command) {
        return (busybox ? "busybox " : "") + command;
    }

    private void showRebootDialog() {
        new AlertDialog.Builder(context)
                .setMessage(R.string.dialog_message_reboot_required)
                .setTitle(R.string.dialog_title_reboot_required)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    new RebootTask().execute();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.dialog_two_factor_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
