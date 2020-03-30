package com.aurora.store.ui.spoof.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.manager.SpoofManager;
import com.aurora.store.model.items.DeviceItem;
import com.aurora.store.ui.single.fragment.BaseFragment;
import com.aurora.store.util.PrefUtil;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.mikepenz.fastadapter.select.SelectExtension;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class DeviceSpoofFragment extends BaseFragment {

    @BindView(R.id.recycler)
    RecyclerView recycler;
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;

    private SpoofManager spoofManager;
    private FastItemAdapter<DeviceItem> fastItemAdapter;
    private SelectExtension<DeviceItem> selectExtension;

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
        spoofManager = new SpoofManager(requireContext());
        setupRecycler();
        fetchSpoofDevices();
    }

    private void setupRecycler() {
        fastItemAdapter = new FastItemAdapter<>();
        selectExtension = new SelectExtension<>(fastItemAdapter);
        fastItemAdapter.addExtension(selectExtension);
        fastItemAdapter.addEventHook(new DeviceItem.CheckBoxClickEvent());

        fastItemAdapter.setOnClickListener((view, blacklistItemIAdapter, blacklistItem, position) -> {
            return false;
        });
        fastItemAdapter.setOnPreClickListener((view, blacklistItemIAdapter, blacklistItem, position) -> true);

        selectExtension.setSelectable(true);
        selectExtension.setMultiSelect(false);
        selectExtension.setSelectWithItemUpdate(true);
        selectExtension.setSelectionListener((item, selected) -> {
            if (selected) {
                for (DeviceItem deviceItem : selectExtension.getSelectedItems()) {
                    deviceItem.setSelected(false);
                }

                fastItemAdapter.notifyAdapterDataSetChanged();

                if (item.getProperties().getProperty("Build.MODEL").toLowerCase().equals("default"))
                    PrefUtil.putString(requireContext(), Constants.PREFERENCE_SPOOF_DEVICE, "");
                else
                    PrefUtil.putString(requireContext(), Constants.PREFERENCE_SPOOF_DEVICE,
                            item.getProperties().getProperty("CONFIG_NAME"));
                item.setSelected(true);
                Toast.makeText(requireContext(), getString(R.string.pref_dialog_to_apply_restart),
                        Toast.LENGTH_SHORT).show();
            }
        });

        recycler.setAdapter(fastItemAdapter);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
    }

    private void fetchSpoofDevices() {
        Observable.fromIterable(new SpoofManager(requireContext())
                .getAvailableDevice())
                .subscribeOn(Schedulers.io())
                .map(DeviceItem::new)
                .toList()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(deviceItems -> fastItemAdapter.add(deviceItems))
                .subscribe();
    }
}
