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

import com.dragons.aurora.Paths;
import com.dragons.aurora.R;
import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;

import java.io.File;

public class DownloadRequestBuilderObb extends DownloadRequestBuilder {

    private boolean main;

    public DownloadRequestBuilderObb(Context context, App app, AndroidAppDeliveryData deliveryData) {
        super(context, app, deliveryData);
    }

    public DownloadRequestBuilderObb setMain(boolean main) {
        this.main = main;
        return this;
    }

    @Override
    protected File getDestinationFile() {
        return Paths.getObbPath(
                app.getPackageName(),
                deliveryData.getAdditionalFile(main ? 0 : 1).getVersionCode(),
                main
        );
    }

    @Override
    protected String getDownloadUrl() {
        return deliveryData.getAdditionalFile(main ? 0 : 1).getDownloadUrl();
    }

    @Override
    protected String getNotificationTitle() {
        return context.getString(main ? R.string.expansion_file_main : R.string.expansion_file_patch, app.getDisplayName());
    }
}
