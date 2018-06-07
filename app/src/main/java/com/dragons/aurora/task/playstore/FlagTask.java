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

package com.dragons.aurora.task.playstore;

import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.R;
import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.io.IOException;

public class FlagTask extends PlayStorePayloadTask<Boolean> implements CloneableTask {

    private App app;
    private GooglePlayAPI.ABUSE reason;
    private String explanation;

    public FlagTask setApp(App app) {
        this.app = app;
        return this;
    }

    public FlagTask setReason(GooglePlayAPI.ABUSE reason) {
        this.reason = reason;
        return this;
    }

    public FlagTask setExplanation(String explanation) {
        this.explanation = explanation;
        return this;
    }

    @Override
    public FlagTask clone() {
        FlagTask task = new FlagTask();
        task.setContext(context);
        task.setApp(app);
        task.setReason(reason);
        task.setExplanation(explanation);
        return task;
    }

    @Override
    protected Boolean getResult(GooglePlayAPI api, String... arguments) throws IOException {
        return api.reportAbuse(app.getPackageName(), reason, explanation);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (success()) {
            ContextUtil.toast(context, R.string.content_flagged);
        }
    }
}
