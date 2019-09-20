/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Aurora Store is free software: you can redistribute it and/or modify
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
 *
 *
 */

package com.aurora.store.sheet;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aurora.store.R;
import com.aurora.store.download.DownloadManager;
import com.aurora.store.utility.Util;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.navigation.NavigationView;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.Status;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DownloadMenuSheet extends BottomSheetDialogFragment {

    @BindView(R.id.navigation_view)
    NavigationView navigationView;

    private Context context;
    private Fetch fetch;
    private Download download;

    public DownloadMenuSheet() {
    }

    public Download getDownload() {
        return download;
    }

    public void setDownload(Download download) {
        this.download = download;
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_download_menu, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fetch = DownloadManager.getFetchInstance(context);

        if (download.getStatus() == Status.PAUSED
                || download.getStatus() == Status.COMPLETED
                || download.getStatus() == Status.CANCELLED) {
            navigationView.getMenu().findItem(R.id.action_pause).setVisible(false);
        }

        if (download.getStatus() == Status.DOWNLOADING
                || download.getStatus() == Status.COMPLETED
                || download.getStatus() == Status.QUEUED) {
            navigationView.getMenu().findItem(R.id.action_resume).setVisible(false);
        }

        if (download.getStatus() == Status.COMPLETED
                || download.getStatus() == Status.CANCELLED) {
            navigationView.getMenu().findItem(R.id.action_cancel).setVisible(false);
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_copy:
                    Util.copyToClipBoard(context, download.getUrl());
                    Toast.makeText(context, context.getString(R.string.action_copied), Toast.LENGTH_LONG).show();
                    break;
                case R.id.action_pause:
                    fetch.pause(download.getId());
                    break;
                case R.id.action_resume:
                    if (download.getStatus() == Status.FAILED
                            || download.getStatus() == Status.CANCELLED)
                        fetch.retry(download.getId());
                    else
                        fetch.resume(download.getId());
                    break;
                case R.id.action_cancel:
                    fetch.cancel(download.getId());
                    break;
                case R.id.action_clear:
                    fetch.delete(download.getId());
                    break;
            }
            dismissAllowingStateLoss();
            return false;
        });
    }
}
