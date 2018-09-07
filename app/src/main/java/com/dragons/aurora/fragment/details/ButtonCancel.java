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

package com.dragons.aurora.fragment.details;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.dragons.aurora.R;
import com.dragons.aurora.downloader.DownloadState;
import com.dragons.aurora.model.App;
import com.dragons.aurora.notification.CancelDownloadService;
import com.percolate.caffeine.ViewUtils;

public class ButtonCancel extends Button {


    public ButtonCancel(Context context, View view, App app) {
        super(context, view, app);
    }

    @Override
    protected android.widget.Button getButton() {
        return (android.widget.Button) view.findViewById(R.id.cancel);
    }

    @Override
    protected boolean shouldBeVisible() {
        boolean isVisible = !DownloadState.get(app.getPackageName()).isEverythingFinished();
        if (isVisible) {
            if (mViewSwitcher.getCurrentView() == actions_layout)
                mViewSwitcher.showNext();
        }
        return isVisible;
    }

    @Override
    protected void onButtonClick(View button) {
        Intent intentCancel = new Intent(context, CancelDownloadService.class);
        intentCancel.putExtra(CancelDownloadService.PACKAGE_NAME, app.getPackageName());
        context.startService(intentCancel);
        button.setVisibility(View.GONE);
        switchViews();
        android.widget.Button buttonDownload = ViewUtils.findViewById(view, R.id.download);
        buttonDownload.setVisibility(View.VISIBLE);
        buttonDownload.setEnabled(true);
    }
}
