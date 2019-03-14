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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.manager.BlacklistManager;
import com.aurora.store.R;
import com.aurora.store.adapter.BlacklistAdapter;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;


public class BlacklistFragment extends Fragment implements BlacklistAdapter.ItemClickListener {

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;
    @BindView(R.id.btn_clear_all)
    Button btnClearAll;
    @BindView(R.id.txt_blacklist)
    TextView txtBlacklist;

    private Context context;
    private BlacklistManager mBlacklistManager;
    private BlacklistAdapter mAdapter;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_blacklist, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBlacklistManager = new BlacklistManager(context);
        setupRecycler();
        setupClearAll();
    }

    private void updateBlackListedApps() {
        mAdapter.addSelectionsToBlackList();
    }

    private void clearBlackListedApps() {
        if (mAdapter != null) {
            mAdapter.removeSelectionsToBlackList();
            mAdapter.notifyDataSetChanged();
            txtBlacklist.setText(getString(R.string.list_blacklist_none));
        }
    }

    private void setupRecycler() {
        List<ResolveInfo> mInstalledPackages = getInstalledApps();
        mAdapter = new BlacklistAdapter(context, mInstalledPackages, this);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        mRecyclerView.setAdapter(mAdapter);
    }

    private void setupClearAll() {
        btnClearAll.setOnClickListener(v -> {
            clearBlackListedApps();
        });
    }

    @Override
    public void onItemClicked(int position) {
        mAdapter.toggleSelection(position);
        updateBlackListedApps();
        int count = mBlacklistManager.getBlacklistedApps().size();
        String txtCount = new StringBuilder()
                .append(getResources().getString(R.string.list_blacklist))
                .append(" : ")
                .append(mBlacklistManager.getBlacklistedApps().size()).toString();
        txtBlacklist.setText(count > 0 ? txtCount : getString(R.string.list_blacklist_none));
    }

    private List<ResolveInfo> getInstalledApps() {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> installedApps = packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA);
        Collections.sort(installedApps, new ResolveInfo.DisplayNameComparator(packageManager));
        return installedApps;
    }
}
