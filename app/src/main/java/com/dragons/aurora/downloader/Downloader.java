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

import android.content.Context;
import android.os.StatFs;

import com.dragons.aurora.InstalledApkCopier;
import com.dragons.aurora.Paths;
import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;
import com.dragons.aurora.playstoreapiv2.AppFileMetadata;

import java.io.File;

import timber.log.Timber;

public class Downloader {

    private Context context;
    private DownloadManagerInterface dm;

    public Downloader(Context context) {
        this.context = context;
        this.dm = DownloadManagerFactory.get(context);
    }

    static private void prepare(File file, long expectedSize) {
        Timber.i("file.exists()=" + file.exists() + " file.length()=" + file.length() + " metadata.getSize()=" + expectedSize);
        if (file.exists() && file.length() != expectedSize) {
            Timber.i("Deleted old file: %s", file.delete());
        }
        file.getParentFile().mkdirs();
    }

    static private boolean shouldDownloadDelta(App app, AndroidAppDeliveryData deliveryData) {
        File currentApk = InstalledApkCopier.getCurrentApk(app);
        return app.getVersionCode() > app.getInstalledVersionCode()
                && deliveryData.hasPatchData()
                && null != currentApk
                && currentApk.exists()
                ;
    }

    public void download(App app, AndroidAppDeliveryData deliveryData) {
        DownloadState state = DownloadState.get(app.getPackageName());
        state.setApp(app);
        DownloadManagerInterface.Type type = shouldDownloadDelta(app, deliveryData)
                ? DownloadManagerInterface.Type.DELTA
                : DownloadManagerInterface.Type.APK;
        prepare(Paths.getApkPath(context, app.getPackageName(), app.getVersionCode()), deliveryData.getDownloadSize());
        state.setStarted(dm.enqueue(app, deliveryData, type));
        if (deliveryData.getAdditionalFileCount() > 0) {
            checkAndStartObbDownload(state, deliveryData, true);
        }
        if (deliveryData.getAdditionalFileCount() > 1) {
            checkAndStartObbDownload(state, deliveryData, false);
        }
    }

    public boolean enoughSpace(AndroidAppDeliveryData deliveryData) {
        long bytesNeeded = deliveryData.getDownloadSize();
        if (deliveryData.getAdditionalFileCount() > 0) {
            bytesNeeded += deliveryData.getAdditionalFile(0).getSize();
        }
        if (deliveryData.getAdditionalFileCount() > 1) {
            bytesNeeded += deliveryData.getAdditionalFile(1).getSize();
        }
        StatFs stat = new StatFs(Paths.getDownloadPath(context).getPath());
        return (long) stat.getBlockSize() * (long) stat.getAvailableBlocks() >= bytesNeeded;
    }

    private void checkAndStartObbDownload(DownloadState state, AndroidAppDeliveryData deliveryData, boolean main) {
        App app = state.getApp();
        AppFileMetadata metadata = deliveryData.getAdditionalFile(main ? 0 : 1);
        File file = Paths.getObbPath(app.getPackageName(), metadata.getVersionCode(), main);
        prepare(file, metadata.getSize());
        if (!file.exists()) {
            state.setStarted(dm.enqueue(
                    app,
                    deliveryData,
                    main ? DownloadManagerInterface.Type.OBB_MAIN : DownloadManagerInterface.Type.OBB_PATCH
            ));
        }
    }
}
