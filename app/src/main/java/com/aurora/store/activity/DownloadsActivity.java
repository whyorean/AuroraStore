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
import com.aurora.store.model.App;
import com.aurora.store.task.BulkDetails;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.ThemeUtil;
import com.aurora.store.utility.ViewUtil;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.Status;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class DownloadsActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.recyclerDownloads)
    RecyclerView mRecyclerView;
    @BindView(R.id.placeholder)
    RelativeLayout placeholder;

    private Fetch mFetch;
    private ThemeUtil mThemeUtil = new ThemeUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mThemeUtil.onCreate(this);
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
        mThemeUtil.onResume(this);
    }

    private void init() {
        DownloadManager mDownloadManager = new DownloadManager(this);
        mFetch = mDownloadManager.getFetchInstance();
        mFetch.getDownloads(downloadList -> {
            if (downloadList.isEmpty()) {
                ViewUtil.hideWithAnimation(mRecyclerView);
                ViewUtil.showWithAnimation(placeholder);
            } else {
                ViewUtil.showWithAnimation(mRecyclerView);
                ViewUtil.hideWithAnimation(placeholder);
                getAppsList(downloadList);
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
        mFetch.cancelAll();
        init();
    }

    private void clearCompleted() {
        mFetch.removeAllWithStatus(Status.COMPLETED);
        init();
    }

    private void forceClearAll() {
        mFetch.removeAllWithStatus(Status.ADDED);
        mFetch.removeAllWithStatus(Status.CANCELLED);
        mFetch.removeAllWithStatus(Status.COMPLETED);
        mFetch.removeAllWithStatus(Status.DOWNLOADING);
        mFetch.removeAllWithStatus(Status.FAILED);
        mFetch.removeAllWithStatus(Status.PAUSED);
        mFetch.removeAllWithStatus(Status.QUEUED);
        init();
    }

    private void pauseAll() {
        if (mFetch != null) {
            mFetch.getDownloads(mDownloads -> {
                for (Download download : mDownloads)
                    if (download.getStatus() == Status.DOWNLOADING
                            || download.getStatus() == Status.QUEUED
                            || download.getStatus() == Status.ADDED)
                        mFetch.pause(download.getId());
            });
        }
    }

    private void resumeAll() {
        if (mFetch != null) {
            mFetch.getDownloads(mDownloads -> {
                for (Download download : mDownloads)
                    if (download.getStatus() == Status.PAUSED)
                        mFetch.resume(download.getId());
            });
        }
    }

    private void getAppsList(List<Download> downloadList) {
        List<String> packageList = getPackageNames(downloadList);
        CompositeDisposable mDisposable = new CompositeDisposable();
        mDisposable.add(Observable.fromCallable(() -> new BulkDetails(this).getRemoteAppList(packageList))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((appList) -> {
                    setupRecycler(downloadList, appList);
                }, err -> {
                    Log.e(err.getMessage());
                }));
    }

    private List<String> getPackageNames(List<Download> downloadList) {
        List<String> appList = new ArrayList<>();
        for (Download download : downloadList)
            appList.add(download.getTag());
        return appList;
    }

    private void setupRecycler(List<Download> downloadList, List<App> appList) {
        DownloadsAdapter mAdapter = new DownloadsAdapter(this, downloadList, appList);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        DividerItemDecoration itemDecorator = new DividerItemDecoration(mRecyclerView.getContext(), DividerItemDecoration.VERTICAL);
        mRecyclerView.addItemDecoration(itemDecorator);
    }

}
