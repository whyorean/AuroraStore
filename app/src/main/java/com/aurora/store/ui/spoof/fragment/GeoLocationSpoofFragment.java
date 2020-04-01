package com.aurora.store.ui.spoof.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.R;
import com.aurora.store.model.items.GeoItem;
import com.aurora.store.notification.QuickNotification;
import com.aurora.store.task.GeoSpoofTask;
import com.aurora.store.ui.single.fragment.BaseFragment;
import com.aurora.store.util.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.mikepenz.fastadapter.select.SelectExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class GeoLocationSpoofFragment extends BaseFragment {

    @BindView(R.id.recycler)
    RecyclerView recycler;
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;

    private FastItemAdapter<GeoItem> fastItemAdapter;
    private SelectExtension<GeoItem> selectExtension;
    private CompositeDisposable disposable = new CompositeDisposable();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_spoof_container, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupRecycler();
        fetchAvailableGeoLocations();
    }

    @Override
    public void onDestroy() {
        disposable.dispose();
        super.onDestroy();
    }

    private void setupRecycler() {
        fastItemAdapter = new FastItemAdapter<>();
        selectExtension = new SelectExtension<>(fastItemAdapter);

        fastItemAdapter.addExtension(selectExtension);
        fastItemAdapter.addEventHook(new GeoItem.CheckBoxClickEvent());
        fastItemAdapter.setOnPreClickListener((view, blacklistItemIAdapter, blacklistItem, position) -> true);

        selectExtension.setSelectable(true);
        selectExtension.setMultiSelect(false);
        selectExtension.setSelectWithItemUpdate(true);
        selectExtension.setSelectionListener((item, selected) -> {
            if (selected) {
                for (GeoItem selectedItems : selectExtension.getSelectedItems()) {
                    selectedItems.setSelected(false);
                    selectedItems.setChecked(false);
                }

                if (item.getLocation().toLowerCase().contains("default")) {
                    setMockDialog(false);
                } else {
                    applyGeoSpoof(item.getLocation(), item);
                }
            }
        });

        recycler.setAdapter(fastItemAdapter);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
    }

    private void fetchAvailableGeoLocations() {
        final String[] geoLocations = requireContext().getResources().getStringArray(R.array.geoLocation);
        final List<String> stringList = new ArrayList<>(Arrays.asList(geoLocations));
        Collections.sort(stringList);
        Observable.fromIterable(stringList)
                .subscribeOn(Schedulers.io())
                .map(GeoItem::new)
                .toList()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(localeItems -> fastItemAdapter.add(localeItems))
                .subscribe();
    }

    private void applyGeoSpoof(String geoLocation, GeoItem item) {
        disposable.clear();
        disposable.add(Observable.fromCallable(() -> new GeoSpoofTask(requireContext())
                .spoof(geoLocation))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result) {
                        QuickNotification.show(
                                requireContext(),
                                getString(R.string.pref_notification_title_geo_spoof),
                                geoLocation,
                                null);
                        Util.clearCache(requireContext());
                    }
                    item.setSelected(result);
                    fastItemAdapter.notifyAdapterDataSetChanged();
                }, throwable -> {
                    if (throwable instanceof SecurityException) {
                        setMockDialog(true);
                    }
                }));
    }

    private void setMockDialog(boolean isEnabled) {
        final MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pref_category_spoof_location)
                .setCancelable(false)
                .setMessage(isEnabled
                        ? R.string.pref_requested_location_enable
                        : R.string.pref_requested_location_disable)
                .setPositiveButton(isEnabled
                        ? R.string.action_enable
                        : R.string.action_disable, (dialogInterface, i) -> {
                    requireContext().startActivity(new Intent(
                            android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                    dialogInterface.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null);
        dialogBuilder.create();
        dialogBuilder.show();
    }
}
