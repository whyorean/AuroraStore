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

package com.aurora.store.ui.main.fragment.updates;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.AppDiffCallback;
import com.aurora.store.AuroraApplication;
import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.download.DownloadManager;
import com.aurora.store.model.App;
import com.aurora.store.section.UpdateAppSection;
import com.aurora.store.sheet.AppMenuSheet;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.ui.main.AuroraActivity;
import com.aurora.store.ui.single.fragment.BaseFragment;
import com.aurora.store.ui.view.CustomSwipeToRefresh;
import com.aurora.store.util.Util;
import com.aurora.store.util.ViewUtil;
import com.google.android.material.button.MaterialButton;
import com.tonyodev.fetch2.Fetch;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;
import io.reactivex.disposables.CompositeDisposable;


public class UpdatesFragment extends BaseFragment implements UpdateAppSection.ClickListener {

    @BindView(R.id.swipe_layout)
    CustomSwipeToRefresh customSwipeToRefresh;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.txt_update_all)
    AppCompatTextView txtUpdateAll;
    @BindView(R.id.btn_action)
    MaterialButton btnAction;

    private Fetch fetch;
    private CompositeDisposable disposable = new CompositeDisposable();

    private UpdatableAppsModel model;
    private UpdateAppSection section;
    private SectionedRecyclerViewAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_updates, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fetch = DownloadManager.getFetchInstance(requireContext());
        setupRecycler();

        model = new ViewModelProvider(this).get(UpdatableAppsModel.class);
        model.getListMutableLiveData().observe(this, appList -> {
            dispatchAppsToAdapter(appList);
            customSwipeToRefresh.setRefreshing(false);
        });

        model.getError().observe(this, errorType -> {
            switch (errorType) {
                case NO_API:
                case SESSION_EXPIRED:
                    Util.validateApi(requireContext());
                    break;
            }
        });

        customSwipeToRefresh.setOnRefreshListener(() -> model.fetchUpdatableApps());

        disposable.add(AuroraApplication
                .getRxBus()
                .getBus()
                .subscribe(event -> {
                    switch (event.getSubType()) {
                        case BLACKLIST:
                        case INSTALLED:
                            removeAppFromAdapter(event.getPackageName());
                            break;
                        case API_SUCCESS:
                        case NETWORK_AVAILABLE:
                            model.fetchUpdatableApps();
                            break;
                        case BULK_UPDATE_NOTIFY:
                            drawButtons();
                            break;
                    }
                }));
    }

    @Override
    public void onPause() {
        super.onPause();
        customSwipeToRefresh.setRefreshing(false);
    }

    @Override
    public void onDestroy() {
        try {
            disposable.clear();
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    private void removeAppFromAdapter(String packageName) {
        int position = section.removeApp(packageName);
        adapter.notifyItemRemoved(position);
        AuroraApplication.removeFromOngoingUpdateList(packageName);
        drawButtons();
        updateCounter();
    }

    private void dispatchAppsToAdapter(List<App> newList) {
        List<App> oldList = section.getList();
        if (oldList.isEmpty()) {
            section.updateList(newList);
            adapter.notifyDataSetChanged();
        } else {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new AppDiffCallback(newList, oldList));
            diffResult.dispatchUpdatesTo(adapter);
            section.updateList(newList);
        }
        updateCounter();
        drawButtons();
    }

    private void setupRecycler() {
        customSwipeToRefresh.setRefreshing(false);
        adapter = new SectionedRecyclerViewAdapter();
        section = new UpdateAppSection(requireContext(), this);
        adapter.addSection(section);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        recyclerView.setAdapter(adapter);
    }

    private void drawButtons() {
        if (AuroraApplication.isBulkUpdateAlive()) {
            btnAction.setText(getString(R.string.action_cancel));
            btnAction.setOnClickListener(v -> {
                cancelAllRequests();
                AuroraApplication.setOngoingUpdateList(new ArrayList<>());
                Util.stopBulkUpdate(requireContext());
            });
        } else {
            btnAction.setText(getString(R.string.list_update_all));
            btnAction.setOnClickListener(v -> {
                AuroraApplication.setOngoingUpdateList(section.getList());
                Util.bulkUpdate(requireContext());
            });
        }
    }

    private void updateCounter() {
        int size = section.getList().size();
        if (size == 0) {
            btnAction.setVisibility(View.INVISIBLE);
            txtUpdateAll.setVisibility(View.INVISIBLE);
        } else {
            txtUpdateAll.setText(new StringBuilder()
                    .append(size)
                    .append(StringUtils.SPACE)
                    .append(size == 1 ? requireContext().getString(R.string.list_update_all_txt_one) :
                            requireContext().getString(R.string.list_update_all_txt)));
            btnAction.setVisibility(View.VISIBLE);
            txtUpdateAll.setVisibility(View.VISIBLE);
        }
    }

    private void cancelAllRequests() {
        List<App> ongoingUpdateList = AuroraApplication.getOngoingUpdateList();
        for (App app : ongoingUpdateList) {
            fetch.deleteGroup(app.getPackageName().hashCode());
        }
    }

    @Override
    public void onClick(App app) {
        DetailsActivity.app = app;
        Intent intent = new Intent(requireContext(), DetailsActivity.class);
        intent.putExtra(Constants.INTENT_PACKAGE_NAME, app.getPackageName());
        requireContext().startActivity(intent, ViewUtil.getEmptyActivityBundle((AuroraActivity) requireContext()));
    }

    @Override
    public void onLongClick(App app) {
        AppMenuSheet menuSheet = new AppMenuSheet();
        menuSheet.setApp(app);
        menuSheet.show(getChildFragmentManager(), "BOTTOM_MENU_SHEET");
    }
}
