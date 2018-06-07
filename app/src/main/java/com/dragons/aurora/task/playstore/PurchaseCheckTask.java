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

import android.view.View;
import android.widget.Button;

import com.dragons.aurora.R;
import com.dragons.aurora.fragment.details.DownloadOrInstall;
import com.dragons.aurora.playstoreapiv2.AndroidAppDeliveryData;

import java.util.Timer;

public class PurchaseCheckTask extends DeliveryDataTask {

    private DownloadOrInstall downloadOrInstallFragment;
    private Button downloadButton;
    private Timer timer;

    public void setDownloadOrInstallFragment(DownloadOrInstall downloadOrInstallFragment) {
        this.downloadOrInstallFragment = downloadOrInstallFragment;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public void setDownloadButton(Button downloadButton) {
        this.downloadButton = downloadButton;
    }

    @Override
    protected void onPostExecute(AndroidAppDeliveryData deliveryData) {
        boolean success = success() && null != deliveryData;
        downloadOrInstallFragment.draw();
        if (null == downloadButton) {
            return;
        }
        downloadButton.setText(success ? R.string.details_download : R.string.details_download_not_available);
        downloadButton.setEnabled(success);
        downloadButton.setVisibility(View.VISIBLE);
        timer.cancel();
    }
}
