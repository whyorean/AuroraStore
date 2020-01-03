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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.AppDiffCallback;
import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.exception.MalformedRequestException;
import com.aurora.store.manager.FavouriteListManager;
import com.aurora.store.model.App;
import com.aurora.store.section.FavouriteAppSection;
import com.aurora.store.task.LiveUpdate;
import com.aurora.store.task.ObservableDeliveryData;
import com.aurora.store.ui.view.CustomSwipeToRefresh;
import com.aurora.store.util.Log;
import com.aurora.store.util.PathUtil;
import com.aurora.store.util.ViewUtil;
import com.aurora.store.viewmodel.FavouriteAppsModel;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class FavouriteFragment extends Fragment implements FavouriteAppSection.ClickListener {


    private static final String TAG_FAV = "TAG_FAV";

    @BindView(R.id.swipe_refresh_layout)
    CustomSwipeToRefresh customSwipeToRefresh;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.export_list)
    MaterialButton buttonExport;
    @BindView(R.id.install_list)
    MaterialButton buttonInstall;
    @BindView(R.id.count_selection)
    TextView txtCount;

    private Context context;
    private CompositeDisposable disposable = new CompositeDisposable();

    private FavouriteListManager favouriteListManager;
    private FavouriteAppsModel model;
    private FavouriteAppSection section;
    private SectionedRecyclerViewAdapter adapter;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        favouriteListManager = new FavouriteListManager(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_favourites, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        buttonInstall.setOnClickListener(bulkInstallListener());
        buttonExport.setOnClickListener(v -> {
            exportList();
        });
        setupRecycler();
        model = ViewModelProviders.of(this).get(FavouriteAppsModel.class);
        model.getFavouriteApps().observe(this, appList -> {
            dispatchAppsToAdapter(appList);
            customSwipeToRefresh.setRefreshing(false);
        });
        model.fetchFavouriteApps();
        customSwipeToRefresh.setOnRefreshListener(() -> {
            model.fetchFavouriteApps();
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        customSwipeToRefresh.setRefreshing(false);
    }

    private View.OnClickListener bulkInstallListener() {
        return v -> {
            buttonInstall.setText(getString(R.string.details_installing));
            buttonInstall.setEnabled(false);
            disposable.add(Observable.fromIterable(section.getSelectedList())
                    .flatMap(app -> new ObservableDeliveryData(context).getDeliveryData(app))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(deliveryDataBundle -> new LiveUpdate(context)
                            .enqueueUpdate(deliveryDataBundle.getApp(),
                                    deliveryDataBundle.getAndroidAppDeliveryData()))
                    .doOnError(throwable -> {
                        if (throwable instanceof MalformedRequestException) {
                            Toast.makeText(context, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        } else
                            Log.e(throwable.getMessage());
                    })
                    .subscribe());
        };
    }

    private void exportList() {
        try {
            ArrayList<String> packageList = favouriteListManager.get();
            File file = verifyAndGetFile();
            if (file != null) {
                OutputStream fileOutputStream = new FileOutputStream(file, false);
                for (String packageName : packageList)
                    fileOutputStream.write((packageName + System.lineSeparator()).getBytes());
                fileOutputStream.close();
                Toast.makeText(context, "List exported to" + PathUtil.getRootApkPath(context),
                        Toast.LENGTH_SHORT).show();
            }
        } catch (NullPointerException e) {
            Toast.makeText(context, "Could not create directory", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void importList() {
        ArrayList<String> packageList = new ArrayList<>();
        File file = verifyAndGetFile();
        if (file != null) {
            try {
                InputStream in = new FileInputStream(file);
                Scanner sc = new Scanner(in);
                while (sc.hasNext()) {
                    packageList.add(sc.nextLine());
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(context, "Favourite AppList not found", Toast.LENGTH_SHORT).show();
        }
        new FavouriteListManager(context).addAll(packageList);
        model.fetchFavouriteApps();
    }

    private boolean verifyDirectory() {
        PathUtil.checkBaseDirectory(context);
        File directory = new File(PathUtil.getBaseDirectory(context) + Constants.FILES);
        if (!directory.exists())
            directory.mkdir();
        return (directory.exists());
    }

    private File verifyAndGetFile() {
        String fileExt = "fav_list.txt";
        boolean success = verifyDirectory();
        File file = new File(PathUtil.getBaseDirectory(context) + "/" + Constants.FILES + "/" + fileExt);
        try {
            success = file.exists() || file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (success)
            return file;
        else
            return null;
    }

    private void dispatchAppsToAdapter(List<App> newList) {
        List<App> oldList = section.getAppList();
        if (oldList.isEmpty()) {
            section.updateList(newList);
            adapter.notifyDataSetChanged();
        } else {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new AppDiffCallback(newList, oldList));
            diffResult.dispatchUpdatesTo(adapter);
            section.updateList(newList);
        }
        updateCount(section.getSelections().size());
    }

    private void updateCount(int count) {
        String ss = new StringBuilder()
                .append(context.getResources().getString(R.string.list_selected))
                .append(" : ")
                .append(count).toString();
        txtCount.setText(count > 0 ? ss : getString(R.string.list_empty_fav));
        ViewUtil.setVisibility(buttonInstall, count > 0, true);
    }

    private void setupRecycler() {
        customSwipeToRefresh.setRefreshing(false);
        section = new FavouriteAppSection(context, this);
        adapter = new SectionedRecyclerViewAdapter();
        adapter.addSection(TAG_FAV, section);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onClick(int position, String packageName) {
        recyclerView.post(() -> {
            if (section.getSelections().contains(packageName)) {
                section.remove(packageName);
            } else {
                section.add(packageName);
            }
            adapter.notifyItemChanged(position);
            updateCount(section.getSelections().size());
        });
    }

    @Override
    public void onClickError() {
        importList();
    }
}
