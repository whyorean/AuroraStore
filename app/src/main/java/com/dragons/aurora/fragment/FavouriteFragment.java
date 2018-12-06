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

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.FavouriteItemTouchHelper;
import com.dragons.aurora.FavouriteListManager;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.adapters.FavouriteAppsAdapter;
import com.dragons.aurora.adapters.SelectableViewHolder;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.model.App;
import com.dragons.aurora.model.FavouriteItem;
import com.dragons.aurora.task.playstore.AppDetailsTask;
import com.dragons.aurora.task.playstore.BackgroundBulkDownloadTask;
import com.dragons.custom.CustomSwipeToRefresh;
import com.percolate.caffeine.ToastUtils;

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
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static com.dragons.aurora.Util.isConnected;

public class FavouriteFragment extends BaseFragment implements FavouriteItemTouchHelper.RecyclerItemTouchHelperListener, SelectableViewHolder.OnItemSelectedListener {

    @BindView(R.id.swipe_refresh_layout)
    CustomSwipeToRefresh swipeRefreshLayout;
    @BindView(R.id.fav_apps_list)
    RecyclerView favRecyclerView;
    @BindView(R.id.empty_favourites)
    RelativeLayout emptyLayout;
    @BindView(R.id.export_list)
    Button buttonExport;
    @BindView(R.id.import_list)
    Button buttonImport;
    @BindView(R.id.install_list)
    Button buttonInstall;
    @BindView(R.id.count_selection)
    TextView txtCount;

    private FavouriteListManager manager;
    private AppDetailsTask mTask;
    private View view;
    private List<App> favouriteApps;
    private List<App> selectedApps;
    private ArrayList<String> favouriteList;
    private FavouriteAppsAdapter favouriteAppsAdapter;
    private CompositeDisposable mDisposable = new CompositeDisposable();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.fragment_favourites, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        manager = new FavouriteListManager(getContext());
        mTask = new AppDetailsTask(getContext());
        Util.setColors(getContext(), swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (Accountant.isLoggedIn(getContext()) && isConnected(getContext()))
                getFavApps();
            else
                swipeRefreshLayout.setRefreshing(false);
        });
        buttonInstall.setOnClickListener(v -> {
            BackgroundBulkDownloadTask mDownloadTask = new BackgroundBulkDownloadTask(getContext(), selectedApps);
            mDisposable.add(Observable.fromCallable(() -> mDownloadTask.start())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((success) -> {
                        if (success)
                            ToastUtils.quickToast(getContext(), "Bulk download initiated successfully");
                        else
                            ToastUtils.quickToast(getContext(), "Bulk download failed");
                    }, err -> Log.e(Aurora.TAG, err.getMessage())));
        });
        buttonExport.setOnClickListener(v -> {
            exportList();
        });
        buttonImport.setOnClickListener(v -> {
            favouriteList = importList();
            if (!favouriteList.isEmpty()) {
                manager.addAll(favouriteList);
                getFavApps();
            } else {
                ToastUtils.quickToast(getContext(), "Favourite AppList is empty");
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getFavApps();
    }

    @Override
    public void onStop() {
        super.onStop();
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onDestroy() {
        mDisposable.dispose();
        super.onDestroy();
    }

    private void exportList() {
        try {
            ArrayList<String> packageList = manager.get();
            File file = verifyAndGetFile();
            if (file != null) {
                OutputStream fileOutputStream = new FileOutputStream(file, false);
                for (String packageName : packageList)
                    fileOutputStream.write((packageName + System.lineSeparator()).getBytes());
                fileOutputStream.close();
                ToastUtils.quickToast(getContext(), "List exported to" + Util.getBaseDirectory() + Aurora.FILES);
            }
        } catch (NullPointerException e) {
            ToastUtils.quickToast(getContext(), "Could not create directory");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<String> importList() {
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
            ToastUtils.quickToast(getContext(), "Favourite AppList not found");
        }
        return packageList;
    }

    private boolean verifyDirectory() {
        Util.checkBaseDirectory();
        File directory = new File(Util.getBaseDirectory() + Aurora.FILES);
        if (!directory.exists())
            directory.mkdir();
        return (directory.exists());
    }

    private File verifyAndGetFile() {
        String fileExt = "fav_list.txt";
        boolean success = verifyDirectory();
        File file = new File(Util.getBaseDirectory() + Aurora.FILES + File.separator + fileExt);
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

    private void getFavApps() {
        favouriteList = manager.get();
        toggleEmptyLayout(favouriteList.isEmpty());
        if (!favouriteList.isEmpty())
            mDisposable.add(Observable.fromCallable(() -> mTask.getAppDetails(favouriteList))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(start -> swipeRefreshLayout.setRefreshing(true))
                    .doOnTerminate(() -> swipeRefreshLayout.setRefreshing(false))
                    .doOnComplete(() -> buttonExport.setEnabled(true))
                    .subscribe((appToAdd) -> {
                        favouriteApps = appToAdd;
                        setupFavourites(favouriteApps);
                    }, err -> Log.e(Aurora.TAG, err.getMessage())));
    }

    private void toggleEmptyLayout(boolean toggle) {
        emptyLayout.setVisibility(toggle ? View.VISIBLE : View.GONE);
        buttonExport.setEnabled(!toggle);
    }

    private void setupFavourites(List<App> appsToAdd) {
        favouriteAppsAdapter = new FavouriteAppsAdapter(this, this, appsToAdd);
        favRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        favRecyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.anim_falldown));
        DividerItemDecoration itemDecorator = new DividerItemDecoration(favRecyclerView.getContext(), DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(getResources().getDrawable(R.drawable.list_divider));
        favRecyclerView.addItemDecoration(itemDecorator);
        favRecyclerView.setAdapter(favouriteAppsAdapter);
        new ItemTouchHelper(
                new FavouriteItemTouchHelper(0, ItemTouchHelper.LEFT, this))
                .attachToRecyclerView(favRecyclerView);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof SelectableViewHolder) {
            favouriteAppsAdapter.remove(position);
            if (favouriteAppsAdapter.getItemCount() < 1) {
                toggleEmptyLayout(true);
            }
        }
    }

    @Override
    public void onItemSelected(FavouriteItem favouriteItem) {
        selectedApps = favouriteAppsAdapter.getSelectedItems();
        if (selectedApps.isEmpty()) {
            buttonInstall.setEnabled(false);
            txtCount.setText("");
        } else {
            buttonInstall.setEnabled(true);
            txtCount.setText(String.format(getString(R.string.list_selected), selectedApps.size()));
        }
    }
}
