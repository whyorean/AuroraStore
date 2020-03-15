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

package com.aurora.store.ui.single.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.AppDiffCallback;
import com.aurora.store.R;
import com.aurora.store.manager.BlacklistManager;
import com.aurora.store.model.App;
import com.aurora.store.section.BlackListedAppSection;
import com.aurora.store.ui.view.CustomSwipeToRefresh;
import com.aurora.store.util.Log;
import com.aurora.store.util.ViewUtil;
import com.aurora.store.viewmodel.BlackListedAppsModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;


public class BlacklistFragment extends Fragment implements BlackListedAppSection.ClickListener {

    private static final String TAG_BLACKED = "TAG_BLACKED";

    @BindView(R.id.swipe_layout)
    CustomSwipeToRefresh customSwipeToRefresh;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.btn_clear_all)
    Button btnClearAll;
    @BindView(R.id.txt_blacklist)
    TextView txtBlacklist;

    private Context context;
    private BlacklistManager blacklistManager;
    private BlackListedAppsModel model;
    private BlackListedAppSection section;
    private SectionedRecyclerViewAdapter adapter;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        this.blacklistManager = new BlacklistManager(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_blacklist, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupClearAll();
        setupRecycler();
        model = new ViewModelProvider(this).get(BlackListedAppsModel.class);
        model.getAllApps().observe(getViewLifecycleOwner(), appList -> {
            appList = sortBlackListedApps(appList);
            dispatchAppsToAdapter(appList);
            customSwipeToRefresh.setRefreshing(false);
        });
        model.fetchBlackListedApps();
        customSwipeToRefresh.setOnRefreshListener(() -> model.fetchBlackListedApps());
    }

    private List<App> sortBlackListedApps(List<App> appList) {
        final List<App> blackListedApps = new ArrayList<>();
        final List<App> whiteListedApps = new ArrayList<>();
        final List<App> sortListedApps = new ArrayList<>();

        //Sort Apps by Names
        Collections.sort(appList, (App1, App2) -> App1.getDisplayName()
                .compareToIgnoreCase(App2.getDisplayName()));

        //Sort Apps by blacklist status
        for (App app : appList) {
            if (blacklistManager.contains(app.getPackageName()))
                blackListedApps.add(app);
            else
                whiteListedApps.add(app);
        }

        sortListedApps.addAll(blackListedApps);
        sortListedApps.addAll(whiteListedApps);
        return sortListedApps;
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
        updateCount();
    }

    private void setupRecycler() {
        customSwipeToRefresh.setRefreshing(false);
        section = new BlackListedAppSection(context, blacklistManager.get(), this);
        adapter = new SectionedRecyclerViewAdapter();
        adapter.addSection(TAG_BLACKED, section);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        recyclerView.setAdapter(adapter);
    }

    private void setupClearAll() {
        btnClearAll.setOnClickListener(v -> {
            blacklistManager.removeAll();
            section.getBlacklist().clear();
            adapter.notifyDataSetChanged();
            updateCount();
        });
    }

    private void updateCount() {
        int count = section.getBlacklist().size();
        String txtCount = new StringBuilder()
                .append(context.getResources().getString(R.string.list_blacklist))
                .append(" : ")
                .append(count).toString();
        txtBlacklist.setText(count > 0 ? txtCount : getString(R.string.list_blacklist_none));
        ViewUtil.setVisibility(btnClearAll, count > 0, true);
    }

    @Override
    public void onClick(int position, String packageName) {
        recyclerView.post(() -> {
            if (blacklistManager.contains(packageName) || section.getBlacklist().contains(packageName)) {
                blacklistManager.remove(packageName);
                section.remove(packageName);
            } else {
                blacklistManager.add(packageName);
                section.add(packageName);
                for (String s : section.getBlacklist()) {
                    Log.e(s);
                }
            }
            adapter.notifyItemChanged(position);
            updateCount();
        });
    }
}
