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

package com.aurora.store.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.R;
import com.aurora.store.adapter.DownloadsAdapter;
import com.aurora.store.download.DownloadManager;
import com.aurora.store.utility.ThemeUtil;
import com.aurora.store.utility.ViewUtil;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.Status;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DownloadsActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.recyclerDownloads)
    RecyclerView mRecyclerView;
    @BindView(R.id.placeholder)
    RelativeLayout placeholder;

    private Fetch fetch;
    private DownloadsAdapter downloadsAdapter;
    private ThemeUtil themeUtil = new ThemeUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        themeUtil.onCreate(this);
        setContentView(R.layout.activity_downloads);
        ButterKnife.bind(this);

        init();
        setupActionbar();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_download_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_pause_all:
                pauseAll();
                return true;
            case R.id.action_resume_all:
                resumeAll();
                return true;
            case R.id.action_cancel_all:
                cancelAll();
                return true;
            case R.id.action_clear_completed:
                clearCompleted();
                return true;
            case R.id.action_force_clear_all:
                forceClearAll();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        themeUtil.onResume(this);
    }

    private void init() {
        fetch = DownloadManager.getFetchInstance(this);
        fetch.getDownloads(downloadList -> {
            if (downloadList.isEmpty()) {
                ViewUtil.hideWithAnimation(mRecyclerView);
                ViewUtil.showWithAnimation(placeholder);
            } else {
                ViewUtil.showWithAnimation(mRecyclerView);
                ViewUtil.hideWithAnimation(placeholder);
                setupRecycler(downloadList);
            }
        });
    }

    private void setupActionbar() {
        setSupportActionBar(mToolbar);
        ActionBar mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayShowCustomEnabled(true);
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setElevation(0f);
            mActionBar.setTitle(R.string.menu_downloads);
        }
    }

    private void cancelAll() {
        fetch.cancelAll();
        downloadsAdapter.refreshList();
    }

    private void clearCompleted() {
        fetch.removeAllWithStatus(Status.COMPLETED);
        downloadsAdapter.refreshList();
    }

    private void forceClearAll() {
        fetch.deleteAllWithStatus(Status.ADDED);
        fetch.deleteAllWithStatus(Status.CANCELLED);
        fetch.deleteAllWithStatus(Status.COMPLETED);
        fetch.deleteAllWithStatus(Status.DOWNLOADING);
        fetch.deleteAllWithStatus(Status.FAILED);
        fetch.deleteAllWithStatus(Status.PAUSED);
        fetch.deleteAllWithStatus(Status.QUEUED);
        init();
    }

    private void pauseAll() {
        if (fetch != null) {
            fetch.getDownloads(downloadList -> {
                for (Download download : downloadList)
                    if (download.getStatus() == Status.DOWNLOADING
                            || download.getStatus() == Status.QUEUED
                            || download.getStatus() == Status.ADDED)
                        fetch.pause(download.getId());
            });
            downloadsAdapter.refreshList();
        }
    }

    private void resumeAll() {
        if (fetch != null) {
            fetch.getDownloads(downloadList -> {
                for (Download download : downloadList)
                    if (download.getStatus() == Status.PAUSED
                            || download.getStatus() == Status.FAILED)
                        fetch.resume(download.getId());
            });
            downloadsAdapter.refreshList();
        }
    }

    private void setupRecycler(List<Download> downloadList) {
        downloadsAdapter = new DownloadsAdapter(this, downloadList);
        mRecyclerView.setAdapter(downloadsAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        DividerItemDecoration itemDecorator = new DividerItemDecoration(mRecyclerView.getContext(), DividerItemDecoration.VERTICAL);
        mRecyclerView.addItemDecoration(itemDecorator);
    }
}
