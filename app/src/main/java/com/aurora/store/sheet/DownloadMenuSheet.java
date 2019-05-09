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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.R;
import com.aurora.store.adapter.DownloadMenuAdapter;
import com.aurora.store.adapter.DownloadsAdapter;
import com.aurora.store.download.DownloadManager;
import com.aurora.store.utility.Util;
import com.aurora.store.view.CustomBottomSheetDialogFragment;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.Status;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DownloadMenuSheet extends CustomBottomSheetDialogFragment implements DownloadMenuAdapter.MenuClickListener {

    @BindView(R.id.menu_title)
    TextView downloadTitle;
    @BindView(R.id.menu_recycler)
    RecyclerView menuRecyclerView;

    private String title;
    private Context context;
    private Fetch fetch;
    private Download download;
    private DownloadsAdapter downloadsAdapter;

    public DownloadMenuSheet() {
    }

    public DownloadsAdapter getDownloadsAdapter() {
        return downloadsAdapter;
    }

    public void setDownloadsAdapter(DownloadsAdapter downloadsAdapter) {
        this.downloadsAdapter = downloadsAdapter;
    }

    public Download getDownload() {
        return download;
    }

    public void setDownload(Download download) {
        this.download = download;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_menu, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fetch = DownloadManager.getFetchInstance(context);
        downloadTitle.setText(getTitle());
        menuRecyclerView.setNestedScrollingEnabled(false);
        menuRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        menuRecyclerView.setAdapter(new DownloadMenuAdapter(context, download, this));
    }

    @Override
    public void onMenuClicked(int position) {
        switch (position) {
            case 0:
                Util.copyToClipBoard(context, download.getUrl());
                Toast.makeText(context, context.getString(R.string.action_copied), Toast.LENGTH_LONG).show();
                notifyAndDismiss();
                break;
            case 1:
                fetch.pause(download.getId());
                notifyAndDismiss();
                break;
            case 2:
                if (download.getStatus() == Status.FAILED
                        || download.getStatus() == Status.CANCELLED)
                    fetch.retry(download.getId());
                else
                    fetch.resume(download.getId());
                notifyAndDismiss();
                break;
            case 3:
                fetch.cancel(download.getId());
                notifyAndDismiss();
                break;
            case 4:
                fetch.delete(download.getId());
                notifyAndDismiss();
                break;
        }
    }

    private void notifyAndDismiss() {
        getDownloadsAdapter().refreshList();
        dismissAllowingStateLoss();
    }
}
