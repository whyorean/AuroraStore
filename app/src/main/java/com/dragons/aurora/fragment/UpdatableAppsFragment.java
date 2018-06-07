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
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.dragons.aurora.AuroraApplication;
import com.dragons.aurora.BlackWhiteListManager;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.UpdatableRecyclerItemTouchHelper;
import com.dragons.aurora.UpdateAllReceiver;
import com.dragons.aurora.UpdateChecker;
import com.dragons.aurora.Util;
import com.dragons.aurora.adapters.UpdatableAppsAdapter;
import com.dragons.aurora.model.App;
import com.dragons.aurora.notification.CancelDownloadService;
import com.dragons.aurora.task.playstore.UpdatableAppsTaskHelper;
import com.github.florent37.shapeofview.shapes.RoundRectView;
import com.percolate.caffeine.ToastUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class UpdatableAppsFragment extends UpdatableAppsTaskHelper implements UpdatableRecyclerItemTouchHelper.UpdatableRecyclerItemTouchListener {

    public static int updates = 0;
    public static boolean recheck = false;
    public UpdatableAppsAdapter updatableAppsAdapter;
    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.updatable_apps_list)
    RecyclerView recyclerView;
    @BindView(R.id.update_all)
    Button update;
    @BindView(R.id.update_cancel)
    Button cancel;
    @BindView(R.id.recheck_updates)
    Button recheck_update;
    @BindView(R.id.ohhSnap_retry)
    Button retry_update;
    @BindView(R.id.updates_txt)
    TextView updates_txt;
    @BindView(R.id.updates_setting)
    TextView deltaTextView;
    private List<App> updatableApps = new ArrayList<>(new HashSet<>());
    private UpdateAllReceiver updateAllReceiver;
    private View view;
    private Disposable loadApps;

    public static UpdatableAppsFragment newInstance() {
        return new UpdatableAppsFragment();
    }

    public Boolean isAlreadyUpdating() {
        return ((AuroraApplication) getActivity().getApplication()).isBackgroundUpdating();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);
        this.setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (view != null) {
            if ((ViewGroup) view.getParent() != null)
                ((ViewGroup) view.getParent()).removeView(view);
            return view;
        }

        view = inflater.inflate(R.layout.app_updatable_inc, container, false);
        ButterKnife.bind(this, view);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isLoggedIn() && isConnected(getContext()) && !isAlreadyUpdating())
                loadUpdatableApps();
            else
                swipeRefreshLayout.setRefreshing(false);
        });

        recheck_update.setOnClickListener(click -> {
            if (isLoggedIn() && isConnected(getContext())) {
                hide(view, R.id.unicorn);
                updates_txt.setText(R.string.list_update_chk_txt);
                loadUpdatableApps();
            }
        });

        retry_update.setOnClickListener(click -> {
            if (isLoggedIn() && isConnected(getContext())) {
                hide(view, R.id.ohhSnap);
                if (updatableAppsAdapter == null || updatableAppsAdapter.getItemCount() <= 0) {
                    updates_txt.setText(R.string.list_update_chk_txt);
                    loadUpdatableApps();
                }
            }
        });

        setupAutoUpdate();
        setupDelta();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isLoggedIn() && updatableApps.isEmpty() || recheck) {
            recheck = false;
            loadUpdatableApps();
        } else if (updatableApps.size() > 0)
            setText(view, R.id.updates_txt, R.string.list_update_all_txt, updatableApps.size());
        else if (!isLoggedIn())
            ToastUtils.quickToast(getActivity(), "You need to Login First", true);
        else {
            new UpdateAllReceiver(this);
        }

        updateAllReceiver = new UpdateAllReceiver(this);
        setupDelta();
    }

    @Override
    public void onStop() {
        super.onStop();
        (getActivity()).unregisterReceiver(updateAllReceiver);
        swipeRefreshLayout.setRefreshing(false);
    }

    public void launchUpdateAll() {
        ((AuroraApplication) getActivity().getApplicationContext()).setBackgroundUpdating(true);
        new UpdateChecker().onReceive(UpdatableAppsFragment.this.getActivity(), getActivity().getIntent());
        hide(view, R.id.update_all);
        show(view, R.id.update_cancel);
    }

    public void addButtons() {
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
            setUpdates(updatableApps.size());
        });
    }

    public void removeButtons() {
        show(view, R.id.unicorn);
        update.setVisibility(View.GONE);
        cancel.setVisibility(View.GONE);
    }

    public void setupDelta() {
        deltaTextView.setText(PreferenceFragment.getBoolean(getContext(), "PREFERENCE_DOWNLOAD_DELTAS")
                ? R.string.delta_enabled
                : R.string.delta_disabled);
    }

    public void updateInteger(int count) {
        if (count >= 99) {
            updates = 99;
        } else updates = count;
    }

    public void setupAutoUpdate() {
        CardView autoUpdatesCard = view.findViewById(R.id.autoUpdatesCard);
        ImageView autoUpdatesClose = view.findViewById(R.id.autoUpdatesClose);
        Button autoUpdatesSwitch = view.findViewById(R.id.autoUpdatesSwitch);
        boolean shouldAsk = PreferenceFragment.getBoolean(getContext(), "PROMPT_UPDATE_INTERVAL");
        if (PreferenceFragment.getString(getContext(), "PREFERENCE_BACKGROUND_UPDATE_INTERVAL").equals("-1") && !shouldAsk) {
            autoUpdatesCard.setVisibility(View.VISIBLE);
        }

        autoUpdatesClose.setOnClickListener(click -> {
            autoUpdatesCard.setVisibility(View.GONE);
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putBoolean("PROMPT_UPDATE_INTERVAL", true)
                    .apply();
        });

        autoUpdatesSwitch.setOnClickListener(click -> {
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putString("PREFERENCE_BACKGROUND_UPDATE_INTERVAL", "86400000")
                    .apply();
            autoUpdatesCard.setVisibility(View.GONE);
        });
    }

    public void loadUpdatableApps() {
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
        setUpdates(updatableApps.size());
        updateInteger(updatableApps.size());

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

    protected void setupRecycler(List<App> appsToAdd) {
        updatableAppsAdapter = new UpdatableAppsAdapter(getActivity(), appsToAdd);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getActivity()));
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_anim));
        recyclerView.setAdapter(updatableAppsAdapter);
        new ItemTouchHelper(
                new UpdatableRecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this))
                .attachToRecyclerView(recyclerView);
    }

    private void setUpdates(int count) {
        setText(view, R.id.updates_txt, R.string.list_update_all_txt, count);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof UpdatableAppsAdapter.ViewHolder) {
            new BlackWhiteListManager(getActivity())
                    .add(((UpdatableAppsAdapter.ViewHolder) viewHolder).app.getPackageName());

            updatableAppsAdapter.remove(position);
            setUpdates(updatableAppsAdapter.getItemCount());
            updateInteger(updatableAppsAdapter.getItemCount());

            if (updatableAppsAdapter.getItemCount() == 0)
                removeButtons();
        }
    }
}
