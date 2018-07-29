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

package com.dragons.aurora.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.dragons.aurora.AuroraApplication;
import com.dragons.aurora.GridAutoFitLayoutManager;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.UpdateChecker;
import com.dragons.aurora.Util;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.adapters.UpdatableAppsGridAdapter;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.model.App;
import com.dragons.aurora.notification.CancelDownloadService;
import com.dragons.aurora.recievers.UpdateAllReceiver;
import com.dragons.aurora.task.playstore.UpdatableAppsTaskHelper;
import com.percolate.caffeine.ToastUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.dragons.aurora.Util.hide;
import static com.dragons.aurora.Util.isConnected;
import static com.dragons.aurora.Util.show;

public class UpdatableAppsFragment extends UpdatableAppsTaskHelper {

    private static final int sColumnWidth = 128;
    public static boolean recheck = false;
    public UpdatableAppsGridAdapter updatableAppsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private Button update;
    private Button cancel;
    private Button recheck_update;
    private Button retry_update;
    private TextView updates_txt;
    private List<App> updatableApps = new ArrayList<>(new HashSet<>());
    private UpdateAllReceiver updateAllReceiver;
    private View view;
    private Disposable loadApps;
    private OnUpdateListener mOnUpdateListener;

    public UpdatableAppsFragment() {
    }

    public static UpdatableAppsFragment newInstance() {
        return new UpdatableAppsFragment();
    }

    private Boolean isAlreadyUpdating() {
        return ((AuroraApplication) getActivity().getApplication()).isBackgroundUpdating();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);
        this.setRetainInstance(true);
        onAttachToParentFragment(getParentFragment());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (view != null) {
            if ((ViewGroup) view.getParent() != null)
                ((ViewGroup) view.getParent()).removeView(view);
            return view;
        }
        view = inflater.inflate(R.layout.fragment_updatable, container, false);
        init();
        Util.setColors(getContext(), swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (Accountant.isLoggedIn(getContext()) && isConnected(getContext()) && !isAlreadyUpdating())
                loadUpdatableApps();
            else
                swipeRefreshLayout.setRefreshing(false);
        });
        recheck_update.setOnClickListener(click -> {
            if (Accountant.isLoggedIn(getContext()) && isConnected(getContext())) {
                hide(view, R.id.unicorn);
                updates_txt.setText(R.string.list_update_chk_txt);
                loadUpdatableApps();
            }
        });
        retry_update.setOnClickListener(click -> {
            if (Accountant.isLoggedIn(getContext()) && isConnected(getContext())) {
                hide(view, R.id.ohhSnap);
                if (updatableAppsAdapter == null || updatableAppsAdapter.getItemCount() <= 0) {
                    updates_txt.setText(R.string.list_update_chk_txt);
                    loadUpdatableApps();
                }
            }
        });
        return view;
    }

    private void init() {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        recyclerView = view.findViewById(R.id.updatable_apps_list);
        update = view.findViewById(R.id.update_all);
        cancel = view.findViewById(R.id.update_cancel);
        recheck_update = view.findViewById(R.id.recheck_updates);
        retry_update = view.findViewById(R.id.ohhSnap_retry);
        updates_txt = view.findViewById(R.id.updates_txt);
    }

    private void onAttachToParentFragment(Fragment fragment) {
        try {
            mOnUpdateListener = (OnUpdateListener) fragment;
        } catch (ClassCastException e) {
            throw new ClassCastException("Methods not implemented");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Accountant.isLoggedIn(getContext()) && updatableApps.isEmpty() || recheck) {
            recheck = false;
            loadUpdatableApps();
        } else if (!Accountant.isLoggedIn(getContext()))
            ToastUtils.quickToast(getActivity(), "You need to Login First", true);
        updateAllReceiver = new UpdateAllReceiver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getContext().unregisterReceiver(updateAllReceiver);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void launchUpdateAll() {
        ((AuroraApplication) getActivity().getApplicationContext()).setBackgroundUpdating(true);
        new UpdateChecker().onReceive(getActivity(), getActivity().getIntent());
        hide(view, R.id.update_all);
        show(view, R.id.update_cancel);
    }

    private void loadUpdatableApps() {
        swipeRefreshLayout.setRefreshing(true);
        loadApps = Observable.fromCallable(() -> getUpdatableApps(new PlayStoreApiAuthenticator(this.getActivity()).getApi()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((appList) -> {
                    if (view != null) {
                        updatableApps.clear();
                        updatableApps.addAll(appList);
                        setupList(updatableApps);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }, err -> {
                    swipeRefreshLayout.setRefreshing(false);
                    processException(err);
                    show(view, R.id.ohhSnap);
                });
    }

    private void setupList(List<App> updatableApps) {
        if (mOnUpdateListener != null)
            mOnUpdateListener.setUpdateCount(updatableApps.size());
        if (updatableApps.isEmpty())
            removeButtons();
        else
            addButtons();

        if (recyclerView.getAdapter() == null)
            setupRecycler(updatableApps);
        else {
            updatableAppsAdapter.appsToAdd = updatableApps;
            Util.reloadRecycler(recyclerView);
        }
    }

    private void setupRecycler(List<App> appsToAdd) {
        updatableAppsAdapter = new UpdatableAppsGridAdapter(this, (AuroraActivity) getActivity(), appsToAdd);
        recyclerView.setLayoutManager(new GridAutoFitLayoutManager(getContext(), 128));
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_anim));
        recyclerView.setAdapter(updatableAppsAdapter);
    }

    private void addButtons() {
        if (update.getVisibility() == View.VISIBLE)
            return;
        hide(view, R.id.unicorn);
        update.setVisibility(View.VISIBLE);
        update.setOnClickListener(v -> {
            launchUpdateAll();
            update.setVisibility(View.GONE);
            cancel.setVisibility(View.VISIBLE);
            updates_txt.setText(R.string.list_updating);
        });
        cancel.setOnClickListener(v -> {
            for (App app : updatableApps) {
                getContext().startService(new Intent(getContext().getApplicationContext(), CancelDownloadService.class)
                        .putExtra(CancelDownloadService.PACKAGE_NAME, app.getPackageName()));
            }
            ((AuroraApplication) getActivity().getApplicationContext()).setBackgroundUpdating(false);
            update.setVisibility(View.VISIBLE);
            cancel.setVisibility(View.GONE);
            mOnUpdateListener.setUpdateCount(updatableApps.size());
        });
    }

    private void removeButtons() {
        show(view, R.id.unicorn);
        update.setVisibility(View.GONE);
        cancel.setVisibility(View.GONE);
    }

    public interface OnUpdateListener {
        void setUpdateCount(int count);
    }
}
