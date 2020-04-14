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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.OneTimeWorkRequest;

import com.aurora.store.AuroraApplication;
import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.download.DownloadManager;
import com.aurora.store.manager.IgnoreListManager;
import com.aurora.store.model.App;
import com.aurora.store.model.items.UpdatesItem;
import com.aurora.store.sheet.AppMenuSheet;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.ui.single.fragment.BaseFragment;
import com.aurora.store.ui.view.ViewFlipper2;
import com.aurora.store.util.Util;
import com.aurora.store.util.ViewUtil;
import com.aurora.store.util.WorkerUtil;
import com.aurora.store.util.diff.UpdatesDiffCallback;
import com.aurora.store.worker.ApiValidator;
import com.google.android.material.button.MaterialButton;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil;
import com.mikepenz.fastadapter.select.SelectExtension;
import com.tonyodev.fetch2.AbstractFetchGroupListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchGroup;
import com.tonyodev.fetch2.FetchListener;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;


public class UpdatesFragment extends BaseFragment {
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;
    @BindView(R.id.swipe_layout)
    SwipeRefreshLayout swipeLayout;
    @BindView(R.id.view_flipper)
    ViewFlipper2 viewFlipper;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.txt_update_all)
    AppCompatTextView txtUpdateAll;
    @BindView(R.id.btn_action)
    MaterialButton btnAction;

    private Fetch fetch;
    private Set<UpdatesItem> selectedItems = new HashSet<>();

    private UpdatableAppsModel model;
    private FastAdapter<UpdatesItem> fastAdapter;
    private ItemAdapter<UpdatesItem> itemAdapter;
    private SelectExtension<UpdatesItem> selectExtension;

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

        model.getData().observe(getViewLifecycleOwner(), updatesItems -> {
            dispatchAppsToAdapter(updatesItems);
            swipeLayout.setRefreshing(false);
        });

        model.getError().observe(getViewLifecycleOwner(), errorType -> {
            switch (errorType) {
                case NO_API:
                case SESSION_EXPIRED:
                    awaiting = true;
                    buildAndTestApi();
                    break;

                case NO_NETWORK:
                    awaiting = true;
                    break;
            }
        });

        swipeLayout.setRefreshing(true);
        swipeLayout.setOnRefreshListener(() -> model.fetchUpdatableApps());

        AuroraApplication
                .getRelayBus()
                .doOnNext(event -> {
                    //Handle list update events
                    switch (event.getSubType()) {
                        case BLACKLIST:
                            int adapterPosition = event.getIntExtra();
                            removeItemByAdapterPosition(adapterPosition);
                            break;
                        case INSTALLED:
                        case UNINSTALLED:
                            removeItemByPackageName(event.getStringExtra());
                            break;
                    }

                    //Handle Network & API events
                    switch (event.getSubType()) {
                        case API_SUCCESS:
                        case NETWORK_AVAILABLE:
                            if (awaiting) {
                                model.fetchUpdatableApps();
                                awaiting = false;
                            }
                            break;
                    }

                    //Handle misc events
                    switch (event.getSubType()) {
                        case BULK_UPDATE_NOTIFY:
                            updatePageData();
                            break;
                        case WHITELIST:
                            //TODO:Check for update and add app to list if update is available
                            break;
                    }
                })
                .subscribe();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        swipeLayout.setRefreshing(false);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void removeItemByPackageName(String packageName) {
        int adapterPosition = -1;
        for (UpdatesItem updatesItem : itemAdapter.getAdapterItems()) {
            if (updatesItem.getPackageName().equals(packageName)) {
                adapterPosition = itemAdapter.getAdapterPosition(updatesItem);
                break;
            }
        }

        if (adapterPosition >= 0 && itemAdapter != null) {
            itemAdapter.remove(adapterPosition);
            updateItemList(packageName);
        }
    }

    private void removeItemByAdapterPosition(int adapterPosition) {
        if (adapterPosition >= 0 && itemAdapter != null) {
            UpdatesItem updatesItem = itemAdapter.getAdapterItem(adapterPosition);
            updateItemList(updatesItem.getPackageName());
            itemAdapter.remove(adapterPosition);
        }
    }

    private void updateItemList(String packageName) {
        AuroraApplication.removeFromOngoingUpdateList(packageName);
        updatePageData();
    }

    private void buildAndTestApi() {
        final OneTimeWorkRequest workRequest = WorkerUtil.getWorkRequest(ApiValidator.TAG,
                WorkerUtil.getNetworkConstraints(),
                ApiValidator.class);

        WorkerUtil.enqueue(requireContext(), getViewLifecycleOwner(), workRequest, workInfo -> {
            switch (workInfo.getState()) {
                case FAILED:
                    showSnackBar(coordinator, R.string.toast_api_build_failed, null);
                    break;

                case SUCCEEDED:
                    model.getData();
                    break;
            }
        });
    }

    private void updatePageData() {
        updateText();
        updateButtons();
        updateButtonActions();

        if (itemAdapter != null && itemAdapter.getAdapterItems().size() > 0) {
            viewFlipper.switchState(ViewFlipper2.DATA);
        } else {
            viewFlipper.switchState(ViewFlipper2.EMPTY);
        }
    }

    private void dispatchAppsToAdapter(List<UpdatesItem> updatesItems) {
        final FastAdapterDiffUtil fastAdapterDiffUtil = FastAdapterDiffUtil.INSTANCE;
        final UpdatesDiffCallback diffCallback = new UpdatesDiffCallback();
        final DiffUtil.DiffResult diffResult = fastAdapterDiffUtil.calculateDiff(itemAdapter, updatesItems, diffCallback);
        fastAdapterDiffUtil.set(itemAdapter, diffResult);
        updatePageData();
    }

    private void setupRecycler() {
        fastAdapter = new FastAdapter<>();
        itemAdapter = new ItemAdapter<>();
        selectExtension = new SelectExtension<>(fastAdapter);

        fastAdapter.addAdapter(0, itemAdapter);

        fastAdapter.setOnClickListener((view, updatesItemIAdapter, updatesItem, integer) -> {
            final App app = updatesItem.getApp();
            final Intent intent = new Intent(requireContext(), DetailsActivity.class);
            intent.putExtra(Constants.INTENT_PACKAGE_NAME, app.getPackageName());
            intent.putExtra(Constants.STRING_EXTRA, gson.toJson(app));
            startActivity(intent, ViewUtil.getEmptyActivityBundle((AppCompatActivity) requireActivity()));
            return false;
        });

        fastAdapter.setOnLongClickListener((view, updatesItemIAdapter, updatesItem, position) -> {
            final AppMenuSheet menuSheet = new AppMenuSheet();
            final Bundle bundle = new Bundle();
            bundle.putInt(Constants.INT_EXTRA, position);
            bundle.putString(Constants.STRING_EXTRA, gson.toJson(updatesItem.getApp()));
            menuSheet.setArguments(bundle);
            menuSheet.show(getChildFragmentManager(), AppMenuSheet.TAG);
            return true;
        });

        fastAdapter.addExtension(selectExtension);
        fastAdapter.addEventHook(new UpdatesItem.CheckBoxClickEvent());

        selectExtension.setMultiSelect(true);
        selectExtension.setSelectionListener((item, selected) -> {
            if (selected) {
                selectedItems.add(item);
            } else {
                selectedItems.remove(item);
            }
            updatePageData();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(fastAdapter);
    }

    private void updateText() {
        if (selectExtension.getSelectedItems().size() > 0) {
            btnAction.setText(getString(R.string.list_update_selected));
        } else {
            btnAction.setText(getString(R.string.list_update_all));
        }
    }

    private void updateButtons() {
        final int size = itemAdapter.getAdapterItemCount();
        btnAction.setVisibility(size == 0 ? View.INVISIBLE : View.VISIBLE);
        txtUpdateAll.setVisibility(size == 0 ? View.INVISIBLE : View.VISIBLE);

        if (size > 0) {
            txtUpdateAll.setText(new StringBuilder()
                    .append(size)
                    .append(StringUtils.SPACE)
                    .append(size == 1
                            ? requireContext().getString(R.string.list_update_all_txt_one)
                            : requireContext().getString(R.string.list_update_all_txt)));
        }
    }

    private void attachFetchCancelListener() {
        boolean selectiveUpdate = selectExtension.getSelectedItems().size() > 0;
        Observable.fromIterable(selectiveUpdate
                ? selectedItems
                : itemAdapter.getAdapterItems())
                .map(updatesItem -> updatesItem.getPackageName().hashCode())
                .doOnNext(hashcode -> {
                    final FetchListener fetchListener = new AbstractFetchGroupListener() {
                        @Override
                        public void onAdded(int groupId, @NotNull Download download, @NotNull FetchGroup fetchGroup) {
                            super.onAdded(groupId, download, fetchGroup);
                            if (hashcode == groupId) {
                                fetch.cancelGroup(groupId);
                                fetch.removeListener(this);
                            }
                        }

                        @Override
                        public void onProgress(int groupId, @NotNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond, @NotNull FetchGroup fetchGroup) {
                            super.onProgress(groupId, download, etaInMilliSeconds, downloadedBytesPerSecond, fetchGroup);
                            if (hashcode == groupId) {
                                fetch.cancelGroup(groupId);
                                fetch.removeListener(this);
                            }
                        }

                        @Override
                        public void onQueued(int groupId, @NotNull Download download, boolean waitingNetwork, @NotNull FetchGroup fetchGroup) {
                            super.onQueued(groupId, download, waitingNetwork, fetchGroup);
                            if (hashcode == groupId) {
                                fetch.cancelGroup(groupId);
                                fetch.removeListener(this);
                            }
                        }
                    };
                    fetch.addListener(fetchListener);
                })
                .doOnComplete(() -> {
                    //Clear ongoing update list
                    AuroraApplication.setOngoingUpdateList(new ArrayList<>());
                    //Start BulkUpdate cancellation request
                    Util.stopBulkUpdateService(requireContext());
                })
                .subscribe();
    }

    private void updateButtonActions() {
        btnAction.setOnClickListener(null);
        btnAction.setEnabled(true);
        if (AuroraApplication.isBulkUpdateAlive()) {
            btnAction.setText(getString(R.string.action_cancel));
            btnAction.setOnClickListener(v -> {
                attachFetchCancelListener();
                btnAction.setEnabled(false);
            });
        } else {
            boolean selectiveUpdate = selectExtension.getSelectedItems().size() > 0;
            btnAction.setOnClickListener(v -> {
                btnAction.setEnabled(false);
                IgnoreListManager ignoreListManager = new IgnoreListManager(requireContext());
                Observable.fromIterable(selectiveUpdate
                        ? selectedItems
                        : itemAdapter.getAdapterItems())
                        .filter(updatesItem -> {
                            final App app = updatesItem.getApp();
                            return !ignoreListManager.isIgnored(app.getPackageName(), app.getVersionCode());
                        })
                        .map(UpdatesItem::getApp)
                        .toList()
                        .doOnSuccess(apps -> {
                            AuroraApplication.setOngoingUpdateList(apps);
                            Util.startBulkUpdateService(requireContext());
                        })
                        .subscribe();
            });
        }
    }
}
