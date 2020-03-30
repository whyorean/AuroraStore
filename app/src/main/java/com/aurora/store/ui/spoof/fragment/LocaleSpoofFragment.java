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

import com.aurora.store.AuroraApplication;
import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.manager.CategoryManager;
import com.aurora.store.model.items.LocaleItem;
import com.aurora.store.ui.single.fragment.BaseFragment;
import com.aurora.store.util.Log;
import com.aurora.store.util.PrefUtil;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.mikepenz.fastadapter.select.SelectExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class LocaleSpoofFragment extends BaseFragment {

    @BindView(R.id.recycler)
    RecyclerView recycler;
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;

    private FastItemAdapter<LocaleItem> fastItemAdapter;
    private SelectExtension<LocaleItem> selectExtension;

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
        fetchAvailableLocales();
    }

    private void setupRecycler() {
        fastItemAdapter = new FastItemAdapter<>();
        selectExtension = new SelectExtension<>(fastItemAdapter);

        fastItemAdapter.addExtension(selectExtension);
        fastItemAdapter.addEventHook(new LocaleItem.CheckBoxClickEvent());
        fastItemAdapter.setOnPreClickListener((view, blacklistItemIAdapter, blacklistItem, position) -> true);

        selectExtension.setSelectable(true);
        selectExtension.setMultiSelect(false);
        selectExtension.setSelectWithItemUpdate(true);
        selectExtension.setSelectionListener((item, selected) -> {
            if (selected) {
                for (LocaleItem selectedItems : selectExtension.getSelectedItems()) {
                    selectedItems.setSelected(false);
                    selectedItems.setChecked(false);
                }

                fastItemAdapter.notifyAdapterDataSetChanged();

                if (item.getLocale().getLanguage().toLowerCase().equals("default")) {
                    PrefUtil.putString(requireContext(), Constants.PREFERENCE_SPOOF_LOCALE, "");
                } else {
                    final GooglePlayAPI api = AuroraApplication.api;
                    api.setLocale(item.getLocale());
                    String localeString = item.getLocale().toString();
                    Log.e(localeString);
                    if (localeString.contains("#"))
                        localeString = localeString.substring(0, localeString.indexOf('#') - 1);
                    Log.e(localeString);
                    PrefUtil.putString(requireContext(), Constants.PREFERENCE_SPOOF_LOCALE, localeString);
                }

                CategoryManager.clear(requireContext());
                item.setSelected(true);
                Toast.makeText(requireContext(), getString(R.string.pref_dialog_to_apply_restart),
                        Toast.LENGTH_SHORT).show();
            }
        });

        recycler.setAdapter(fastItemAdapter);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
    }

    private void fetchAvailableLocales() {
        final Locale[] locales = Locale.getAvailableLocales();
        final Locale defaultLocale = new Locale("Default", "Default");
        final List<Locale> localeList = new ArrayList<>();

        localeList.add(0, defaultLocale);
        localeList.addAll(Arrays.asList(locales));

        Observable.fromIterable(localeList)
                .subscribeOn(Schedulers.io())
                .map(LocaleItem::new)
                .toList()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(localeItems -> fastItemAdapter.add(localeItems))
                .subscribe();
    }
}
