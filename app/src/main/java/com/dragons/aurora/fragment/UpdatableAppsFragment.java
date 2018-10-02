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
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.dragons.aurora.AuroraApplication;
import com.dragons.aurora.GridAutoFitLayoutManager;
import com.dragons.aurora.R;
import com.dragons.aurora.UpdateChecker;
import com.dragons.aurora.Util;
import com.dragons.aurora.adapters.UpdatableAppsGridAdapter;
import com.dragons.aurora.database.Jessie;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.model.App;
import com.dragons.aurora.notification.CancelDownloadService;
import com.dragons.aurora.recievers.UpdateAllReceiver;
import com.dragons.aurora.task.playstore.UpdatableAppsTaskHelper;
import com.percolate.caffeine.ToastUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static com.dragons.aurora.Util.hide;
import static com.dragons.aurora.Util.isConnected;
import static com.dragons.aurora.Util.show;

public class UpdatableAppsFragment extends BaseFragment {

    public UpdatableAppsGridAdapter updatableAppsAdapter;
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

    private View view;
    private Jessie mJessie;
    private List<App> updatableApps = new ArrayList<>(new HashSet<>());
    private CompositeDisposable mDisposable = new CompositeDisposable();
    private UpdateAllReceiver updateAllReceiver;
    private OnUpdateListener mOnUpdateListener;
    private UpdatableAppsTaskHelper mTaskHelper;

    public UpdatableAppsFragment() {
    }

    private Boolean isAlreadyUpdating() {
        return ((AuroraApplication) getActivity().getApplication()).isBackgroundUpdating();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onAttachToParentFragment(getParentFragment());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.fragment_updatable, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mJessie = new Jessie(getContext());
        mTaskHelper = new UpdatableAppsTaskHelper(getContext());
        Util.setColors(getContext(), swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (Accountant.isLoggedIn(getContext()) && isConnected(getContext()) && !isAlreadyUpdating())
                fetchFromServer();
            else
                swipeRefreshLayout.setRefreshing(false);
        });
        recheck_update.setOnClickListener(click -> {
            if (Accountant.isLoggedIn(getContext()) && isConnected(getContext())) {
                hide(view, R.id.unicorn);
                fetchFromServer();
            }
        });
        retry_update.setOnClickListener(click -> {
            if (Accountant.isLoggedIn(getContext()) && isConnected(getContext())) {
                hide(view, R.id.ohhSnap);
                if (updatableAppsAdapter == null || updatableAppsAdapter.getItemCount() <= 0) {
                    fetchFromServer();
                }
            }
        });
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
        if (Accountant.isLoggedIn(getContext()) && updatableApps.isEmpty()) {
            loadUpdatableApps();
        } else if (!Accountant.isLoggedIn(getContext()))
            ToastUtils.quickToast(getActivity(), "You need to Login First", true);
        updateAllReceiver = new UpdateAllReceiver(this);
    }

    @Override
    public void onPause() {
        swipeRefreshLayout.setRefreshing(false);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        getContext().unregisterReceiver(updateAllReceiver);
        updateAllReceiver = null;
        mDisposable.dispose();
        super.onDestroy();
    }

    private void launchUpdateAll() {
        ((AuroraApplication) getActivity().getApplication()).setBackgroundUpdating(true);
        new UpdateChecker().onReceive(getActivity(), getActivity().getIntent());
        hide(view, R.id.update_all);
        show(view, R.id.update_cancel);
    }

    private void loadUpdatableApps() {
        swipeRefreshLayout.setRefreshing(true);
        if (mJessie.isJsonAvailable(Jessie.JSON_UPDATES) && mJessie.isJasonValid(Jessie.JSON_UPDATES)) {
            JSONArray mJsonArray = mJessie.readJsonArrayFromFile(Jessie.JSON_UPDATES);
            List<App> mApps = mJessie.getAppsFromJsonArray(mJsonArray);
            updatableApps.clear();
            updatableApps.addAll(mApps);
            setupList(updatableApps);
        } else
            fetchFromServer();
    }

    private void fetchFromServer() {
        mDisposable.add(Observable.fromCallable(() -> mTaskHelper.getUpdatableApps())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(start -> {
                    swipeRefreshLayout.setRefreshing(true);
                    updates_txt.setText(R.string.list_update_chk_txt);
                })
                .doOnTerminate(() -> swipeRefreshLayout.setRefreshing(false))
                .doOnError(err -> show(view, R.id.ohhSnap))
                .subscribe((appList) -> {
                    if (view != null) {
                        updatableApps.clear();
                        updatableApps.addAll(appList);
                        addToDatabase(updatableApps);
                    }
                }, err -> Timber.e(err.getMessage())));
    }

    private void addToDatabase(List<App> mAppList) {
        List<JSONObject> mJsonObjects = mJessie.getJsonObjects(mAppList);
        JSONArray mJsonArray = mJessie.getJsonArray(mJsonObjects);
        mJessie.writeJsonToFile(Jessie.JSON_UPDATES, mJsonArray);
        setupList(mAppList);
    }

    private void setupList(List<App> updatableApps) {
        if (mOnUpdateListener != null)
            mOnUpdateListener.setUpdateCount(updatableApps.size());

        if (updatableApps.isEmpty()) {
            removeButtons();
            updates_txt.setText("");
        } else {
            addButtons();
            updates_txt.setText(R.string.list_update_all_txt);
        }

        if (recyclerView.getAdapter() == null)
            setupRecycler(updatableApps);
        else {
            updatableAppsAdapter.appsToAdd = updatableApps;
            Util.reloadRecycler(recyclerView);
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    private void setupRecycler(List<App> appsToAdd) {
        if (getDisplayDensity() >= 400) {
            recyclerView.setLayoutManager(new GridAutoFitLayoutManager(getContext(), 128));
            updatableAppsAdapter = new UpdatableAppsGridAdapter(this, appsToAdd, true);
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
            updatableAppsAdapter = new UpdatableAppsGridAdapter(this, appsToAdd, false);
        }
        recyclerView.setAdapter(updatableAppsAdapter);
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.anim_falldown));
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
                getContext().startService(new Intent(getContext(), CancelDownloadService.class)
                        .putExtra(CancelDownloadService.PACKAGE_NAME, app.getPackageName()));
            }
            ((AuroraApplication) getActivity().getApplication()).setBackgroundUpdating(false);
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

    private float getDisplayDensity() {
        return (Resources.getSystem().getConfiguration().screenWidthDp);
    }

    public interface OnUpdateListener {
        void setUpdateCount(int count);
    }
}
