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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.dragons.aurora.ContextUtil;

abstract public class TaskWithProgress<T> extends AsyncTask<String, Void, T> {

    protected Context context;
    protected ProgressDialog progressDialog;

    public void setContext(Context context) {
        this.context = context;
    }

    public void prepareDialog(int messageId, int titleId) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setTitle(context.getString(titleId));
        dialog.setMessage(context.getString(messageId));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        this.progressDialog = dialog;
    }

    @Override
    protected void onPreExecute() {
        if (null != this.progressDialog && ContextUtil.isAlive(context)) {
            this.progressDialog.show();
        }
    }

    @Override
    protected void onPostExecute(T result) {
        if (null != this.progressDialog && ContextUtil.isAlive(context) && progressDialog.isShowing()) {
            this.progressDialog.dismiss();
        }
    }
}
