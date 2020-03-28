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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aurora.store.R;
import com.aurora.store.download.DownloadManager;
import com.aurora.store.util.Util;
import com.google.android.material.navigation.NavigationView;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.Status;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DownloadMenuSheet extends BaseBottomSheet {

    public static final String TAG = "DOWNLOAD_MENU_SHEET";

    public static final String DOWNLOAD_ID = "DOWNLOAD_ID";
    public static final String DOWNLOAD_STATUS = "DOWNLOAD_STATUS";
    public static final String DOWNLOAD_URL = "DOWNLOAD_URL";

    @BindView(R.id.navigation_view)
    NavigationView navigationView;

    private Fetch fetch;

    private int id;
    private int status;
    private String url;

    public DownloadMenuSheet() {
    }

    @Override
    public View onCreateContentView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_download_menu, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onContentViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            Bundle bundle = getArguments();
            id = bundle.getInt(DOWNLOAD_ID);
            status = bundle.getInt(DOWNLOAD_STATUS);
            url = bundle.getString(DOWNLOAD_URL);
            fetch = DownloadManager.getFetchInstance(requireContext());
            setupNavigation();
        } else {
            dismissAllowingStateLoss();
        }
    }

    private void setupNavigation() {
        if (status == Status.PAUSED.getValue()
                || status == Status.COMPLETED.getValue()
                || status == Status.CANCELLED.getValue()) {
            navigationView.getMenu().findItem(R.id.action_pause).setVisible(false);
        }

        if (status == Status.DOWNLOADING.getValue()
                || status == Status.COMPLETED.getValue()
                || status == Status.QUEUED.getValue()) {
            navigationView.getMenu().findItem(R.id.action_resume).setVisible(false);
        }

        if (status == Status.COMPLETED.getValue()
                || status == Status.CANCELLED.getValue()) {
            navigationView.getMenu().findItem(R.id.action_cancel).setVisible(false);
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_copy:
                    Util.copyToClipBoard(requireContext(), url);
                    Toast.makeText(requireContext(), requireContext().getString(R.string.action_copied), Toast.LENGTH_LONG).show();
                    break;
                case R.id.action_pause:
                    fetch.pause(id);
                    break;
                case R.id.action_resume:
                    if (status == Status.FAILED.getValue()
                            || status == Status.CANCELLED.getValue())
                        fetch.retry(id);
                    else
                        fetch.resume(id);
                    break;
                case R.id.action_cancel:
                    fetch.cancel(id);
                    break;
                case R.id.action_clear:
                    fetch.delete(id);
                    break;
            }
            dismissAllowingStateLoss();
            return false;
        });
    }
}
