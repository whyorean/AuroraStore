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

package com.dragons.aurora.downloader;

import android.util.Pair;

import com.dragons.aurora.model.App;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DownloadState {

    static private Map<String, DownloadState> state = new HashMap<>();
    static private Map<Long, String> downloadIds = new HashMap<>();
    private App app;
    private TriggeredBy triggeredBy = TriggeredBy.DOWNLOAD_BUTTON;
    private Map<Long, Pair<Float, Float>> progress = new HashMap<>();
    private Map<Long, Status> status = new HashMap<>();

    static public DownloadState get(String packageName) {
        if (!state.containsKey(packageName)) {
            state.put(packageName, new DownloadState());
        }
        return state.get(packageName);
    }

    static public DownloadState get(long downloadId) {
        if (downloadIds.containsKey(downloadId)) {
            return get(downloadIds.get(downloadId));
        }
        return null;
    }

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    public boolean isEverythingFinished() {
        boolean isEverythingFinished = true;
        for (Long downloadId : status.keySet()) {
            if (status.get(downloadId).equals(Status.STARTED)) {
                isEverythingFinished = false;
                break;
            }
        }
        return isEverythingFinished;
    }

    public boolean isEverythingSuccessful() {
        boolean isEverythingSuccessful = true;
        for (Long downloadId : status.keySet()) {
            if (!status.get(downloadId).equals(Status.SUCCESSFUL)) {
                isEverythingSuccessful = false;
                break;
            }
        }
        return isEverythingSuccessful;
    }

    public void setStarted(long downloadId) {
        status.put(downloadId, Status.STARTED);
        downloadIds.put(downloadId, app.getPackageName());
    }

    public void setFinished(long downloadId) {
        status.put(downloadId, Status.FINISHED);
    }

    public void setSuccessful(long downloadId) {
        status.put(downloadId, Status.SUCCESSFUL);
    }

    public void setCancelled(long downloadId) {
        status.put(downloadId, Status.CANCELLED);
    }

    public boolean isCancelled(long downloadId) {
        Status status = this.status.get(downloadId);
        return null != status && status.equals(Status.CANCELLED);
    }

    public List<Long> getDownloadIds() {
        List<Long> ids = new ArrayList<>();
        for (Long id : downloadIds.keySet()) {
            if (null != app && null != app.getPackageName() && app.getPackageName().equals(downloadIds.get(id))) {
                ids.add(id);
            }
        }
        return ids;
    }

    public TriggeredBy getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(TriggeredBy triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public Pair<Float, Float> getProgress() {
        float complete = 0;
        float total = 0;
        for (long downloadId : status.keySet()) {
            Pair<Float, Float> current = progress.get(downloadId);
            if (null == current) {
                continue;
            }
            complete += current.first;
            total += current.second;
        }
        return new Pair<>(complete, total);
    }

    public void setProgress(long downloadId, float complete, float total) {
        progress.put(downloadId, new Pair<>(complete, total));
    }

    public enum TriggeredBy {
        DOWNLOAD_BUTTON,
        UPDATE_ALL_BUTTON,
        SCHEDULED_UPDATE,
        MANUAL_DOWNLOAD_BUTTON
    }

    enum Status {
        STARTED,
        FINISHED,
        SUCCESSFUL,
        CANCELLED,
    }
}
