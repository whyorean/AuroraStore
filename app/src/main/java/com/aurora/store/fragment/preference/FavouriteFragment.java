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

package com.aurora.store.fragment.preference;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.ErrorType;
import com.aurora.store.FavouriteItemTouchHelper;
import com.aurora.store.R;
import com.aurora.store.adapter.FavouriteAppsAdapter;
import com.aurora.store.adapter.SelectableViewHolder;
import com.aurora.store.exception.MalformedRequestException;
import com.aurora.store.fragment.BaseFragment;
import com.aurora.store.manager.FavouriteListManager;
import com.aurora.store.model.App;
import com.aurora.store.task.BulkDetails;
import com.aurora.store.task.LiveUpdate;
import com.aurora.store.task.ObservableDeliveryData;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.NetworkUtil;
import com.aurora.store.utility.PathUtil;
import com.aurora.store.view.CustomSwipeToRefresh;

import org.apache.commons.lang3.StringUtils;

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

public class FavouriteFragment extends BaseFragment implements SelectableViewHolder.ItemClickListener,
        FavouriteItemTouchHelper.RecyclerItemTouchHelperListener {

    private static final int FAV_GROUP_ID = 1338;

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

    private Context context;
    private FavouriteListManager manager;
    private List<App> favouriteApps;
    private List<App> selectedApps;
    private ArrayList<String> favouriteList;
    private FavouriteAppsAdapter favouriteAppsAdapter;
    private CompositeDisposable disposable = new CompositeDisposable();

    @Override
    protected View.OnClickListener errRetry() {
        return v -> {
            favouriteList = importList();
            if (!favouriteList.isEmpty()) {
                manager.addAll(favouriteList);
                fetchData();
            } else {
                Toast.makeText(context, "No favourite list to import", Toast.LENGTH_SHORT).show();
            }
        };
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        manager = new FavouriteListManager(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_favourites, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setErrorView(ErrorType.IMPORT);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (Accountant.isLoggedIn(context) && NetworkUtil.isConnected(context))
                fetchData();
            else
                swipeRefreshLayout.setRefreshing(false);
        });

        buttonInstall.setOnClickListener(bulkInstallListener());
        buttonExport.setOnClickListener(v -> {
            exportList();
        });
        fetchData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (favouriteAppsAdapter != null && favouriteAppsAdapter.isEmpty())
            fetchData();
    }

    @Override
    public void onPause() {
        super.onPause();
        swipeRefreshLayout.setRefreshing(false);
    }

    private View.OnClickListener bulkInstallListener() {
        return v -> {
            buttonInstall.setText(getString(R.string.details_installing));
            buttonInstall.setEnabled(false);
            disposable.add(Observable.fromIterable(selectedApps)
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
            ArrayList<String> packageList = manager.get();
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
            Toast.makeText(context, "Favourite AppList not found", Toast.LENGTH_SHORT).show();
        }
        return packageList;
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

    @Override
    protected void fetchData() {
        favouriteList = manager.get();
        if (favouriteList.isEmpty()) {
            setErrorView(ErrorType.IMPORT);
            switchViews(true);
        } else
            disposable.add(Observable.fromCallable(() -> new BulkDetails(context)
                    .getRemoteAppList(favouriteList))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(start -> swipeRefreshLayout.setRefreshing(true))
                    .doOnTerminate(() -> swipeRefreshLayout.setRefreshing(false))
                    .doOnComplete(() -> buttonExport.setEnabled(true))
                    .subscribe((appList) -> {
                        if (appList.isEmpty()) {
                            setErrorView(ErrorType.NO_APPS);
                            switchViews(true);
                        } else {
                            switchViews(false);
                            favouriteApps = appList;
                            setupFavourites(favouriteApps);
                        }
                    }, err -> Log.e(Constants.TAG, err.getMessage())));
    }

    private void setupFavourites(List<App> appsToAdd) {
        favouriteAppsAdapter = new FavouriteAppsAdapter(context, this, appsToAdd);
        favRecyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        favRecyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(context, R.anim.anim_falldown));
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
                setErrorView(ErrorType.IMPORT);
                switchViews(true);
            }
        }
    }

    @Override
    public void onItemClicked(int position) {
        favouriteAppsAdapter.toggleSelection(position);
        selectedApps = favouriteAppsAdapter.getSelectedList();
        if (selectedApps.isEmpty()) {
            buttonInstall.setEnabled(false);
            txtCount.setText("");
        } else {
            buttonInstall.setEnabled(true);
            txtCount.setText(new StringBuilder()
                    .append(getString(R.string.list_selected))
                    .append(StringUtils.SPACE)
                    .append(selectedApps.size())
            );
        }
    }
}
