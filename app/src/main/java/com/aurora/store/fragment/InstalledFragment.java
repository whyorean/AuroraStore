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

package com.aurora.store.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.ErrorType;
import com.aurora.store.ListType;
import com.aurora.store.R;
import com.aurora.store.activity.AuroraActivity;
import com.aurora.store.adapter.InstalledAppsAdapter;
import com.aurora.store.task.InstalledAppsTask;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.ViewUtil;
import com.aurora.store.view.CustomSwipeToRefresh;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.jetbrains.annotations.NotNull;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class InstalledFragment extends BaseFragment {

    @BindView(R.id.swipe_layout)
    CustomSwipeToRefresh mSwipeRefreshLayout;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.switch_system)
    SwitchMaterial switchSystem;

    private Context context;
    private BottomNavigationView bottomNavigationView;
    private View view;
    private InstalledAppsAdapter adapter;
    private InstalledAppsTask installedAppTask;

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_installed, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setErrorView(ErrorType.UNKNOWN);

        switchSystem.setChecked(PrefUtil.getBoolean(context, Constants.PREFERENCE_INCLUDE_SYSTEM));
        switchSystem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                PrefUtil.putBoolean(context, Constants.PREFERENCE_INCLUDE_SYSTEM, true);
            else
                PrefUtil.putBoolean(context, Constants.PREFERENCE_INCLUDE_SYSTEM, false);
            fetchData();
        });

        mSwipeRefreshLayout.setOnRefreshListener(() -> fetchData());
        if (getActivity() instanceof AuroraActivity) {
            bottomNavigationView = ((AuroraActivity) getActivity()).getBottomNavigation();
            setBaseBottomNavigationView(bottomNavigationView);
        }

        setupRecycler();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter == null || adapter.isDataEmpty())
            fetchData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposable.clear();
        disposableBus.clear();
        installedAppTask = null;
        adapter = null;
    }

    @Override
    protected void fetchData() {
        installedAppTask = new InstalledAppsTask(context);
        disposable.add(Observable.fromCallable(() -> installedAppTask
                .getInstalledApps(switchSystem.isChecked()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscription -> mSwipeRefreshLayout.setRefreshing(true))
                .doOnTerminate(() -> mSwipeRefreshLayout.setRefreshing(false))
                .subscribe((appList) -> {
                    if (view != null) {
                        if (appList.isEmpty()) {
                            setErrorView(ErrorType.NO_APPS);
                            switchViews(true);
                        } else {
                            switchViews(false);
                            if (adapter != null)
                                adapter.addData(appList);
                        }
                    }
                }, err -> {
                    Log.d(err.getMessage());
                    processException(err);
                }));
    }

    private void setupRecycler() {
        adapter = new InstalledAppsAdapter(context, ListType.INSTALLED);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(context, R.anim.anim_falldown));
        recyclerView.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                if (velocityY < 0) {
                    if (bottomNavigationView != null)
                        ViewUtil.showBottomNav(bottomNavigationView, true);
                } else if (velocityY > 0) {
                    if (bottomNavigationView != null)
                        ViewUtil.hideBottomNav(bottomNavigationView, true);
                }
                return false;
            }
        });
    }

    @Override
    protected View.OnClickListener errRetry() {
        return v -> {
            fetchData();
            ((Button) v).setText(getString(R.string.action_retry_ing));
            ((Button) v).setEnabled(false);
        };
    }
}
