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

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.SearchActivity;
import com.dragons.aurora.builders.SystemRemountDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.chainfire.libsuperuser.Shell;

public class CheckShellTask extends TaskWithProgress<Boolean> {

    static private final int RETURN_CODE_NOT_FOUND = 127;
    static private final String COMMAND_ECHO = "echo ";
    static private final String COMMAND_RETURNED = " returned ";
    static private final String COMMAND_CODE = "$?";
    static private final String COMMAND_MV = "mv";
    static private final String COMMAND_RM = "rm";
    static private final String COMMAND_MKDIR = "mkdir";
    static private final String COMMAND_CHMOD = "chmod";
    static private final String COMMAND_CHOWN = "chown";
    static private final String COMMAND_CHGRP = "chgrp";
    static private final String COMMAND_BUSYBOX = "busybox";
    static private final String[] COMMANDS = new String[]{
            COMMAND_MV,
            COMMAND_RM,
            COMMAND_MKDIR,
            COMMAND_CHMOD,
            COMMAND_CHOWN,
            COMMAND_CHGRP,
            COMMAND_BUSYBOX
    };

    private boolean availableCoreutils;
    private boolean availableBusybox;
    private SystemRemountTask primaryTask;

    public CheckShellTask(Context context) {
        setContext(context);
    }

    public void setPrimaryTask(SystemRemountTask primaryTask) {
        this.primaryTask = primaryTask;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        if (!Shell.SU.available()) {
            return false;
        }
        List<String> output = Shell.SU.run(getCommands());
        if (null == output) {
            return false;
        }
        Map<String, Boolean> flags = processOutput(output);
        availableCoreutils = flags.get(COMMAND_MV) && flags.get(COMMAND_RM) && flags.get(COMMAND_MKDIR) && flags.get(COMMAND_CHMOD);
        availableBusybox = flags.get(COMMAND_BUSYBOX);
        Log.i(getClass().getSimpleName(), "Coreutils available " + availableCoreutils);
        Log.i(getClass().getSimpleName(), "Busybox available " + availableBusybox);
        return true;
    }

    private List<String> getCommands() {
        List<String> commands = new ArrayList<>();
        for (String command : COMMANDS) {
            commands.add(command);
            commands.add(COMMAND_ECHO + command + COMMAND_RETURNED + COMMAND_CODE);
        }
        return commands;
    }

    private Map<String, Boolean> processOutput(List<String> output) {
        Map<String, Boolean> flags = new HashMap<>();
        for (String command : COMMANDS) {
            flags.put(command, false);
        }
        for (String line : output) {
            Log.d(getClass().getSimpleName(), line);
            for (String command : COMMANDS) {
                if (line.contains(command + COMMAND_RETURNED)) {
                    int returnCode = Integer.parseInt(line.substring((command + COMMAND_RETURNED).length()).trim());
                    flags.put(command, returnCode != RETURN_CODE_NOT_FOUND);
                }
            }
        }
        return flags;
    }

    @Override
    protected void onPreExecute() {
        prepareDialog(
                R.string.dialog_message_checking_busybox,
                R.string.dialog_title_checking_busybox
        );
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (!result || !ContextUtil.isAlive(context)) {
            ContextUtil.toast(context.getApplicationContext(), R.string.pref_no_root);
        } else if (!availableBusybox && !availableCoreutils) {
            showBusyboxDialog();
        } else {
            primaryTask.setBusybox(availableBusybox);
            askAndExecute(primaryTask);
        }
    }

    private void askAndExecute(SystemRemountTask task) {
        new SystemRemountDialogBuilder(context)
                .setPrimaryTask(task)
                .show()
        ;
    }

    private void showBusyboxDialog() {
        new AlertDialog.Builder(context)
                .setMessage(R.string.dialog_message_busybox_not_available)
                .setTitle(R.string.dialog_title_busybox_not_available)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        context.startActivity(getBusyboxSearchIntent());
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.dialog_two_factor_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
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
}
