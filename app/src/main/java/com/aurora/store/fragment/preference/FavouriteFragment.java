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
import com.aurora.store.FavouriteItemTouchHelper;
import com.aurora.store.R;
import com.aurora.store.adapter.FavouriteAppsAdapter;
import com.aurora.store.adapter.SelectableViewHolder;
import com.aurora.store.download.DownloadManager;
import com.aurora.store.download.RequestBuilder;
import com.aurora.store.fragment.BaseFragment;
import com.aurora.store.manager.FavouriteListManager;
import com.aurora.store.model.App;
import com.aurora.store.notification.QuickNotification;
import com.aurora.store.task.BulkDeliveryData;
import com.aurora.store.task.BulkDetails;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.NetworkUtil;
import com.aurora.store.utility.PathUtil;
import com.aurora.store.view.CustomSwipeToRefresh;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.Request;

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
        FavouriteItemTouchHelper.RecyclerItemTouchHelperListener, BaseFragment.EventListenerImpl {

    private static final int BULK_GROUP_ID = 1996;

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
    private List<Request> requestList;
    private ArrayList<String> favouriteList;
    private FavouriteAppsAdapter favouriteAppsAdapter;
    private CompositeDisposable disposable = new CompositeDisposable();
    private Fetch fetch;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
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
        manager = new FavouriteListManager(context);
        fetch = new DownloadManager(context).getFetchInstance();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (Accountant.isLoggedIn(context) && NetworkUtil.isConnected(context))
                getFavApps();
            else
                swipeRefreshLayout.setRefreshing(false);
        });

        buttonInstall.setOnClickListener(bulkInstallListener());
        buttonExport.setOnClickListener(v -> {
            exportList();
        });
        buttonImport.setOnClickListener(v -> {
            favouriteList = importList();
            if (!favouriteList.isEmpty()) {
                manager.addAll(favouriteList);
                getFavApps();
            } else {
                Toast.makeText(context, "Favourite AppList is empty", Toast.LENGTH_SHORT).show();
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
        disposable.dispose();
        super.onDestroy();
    }

    private View.OnClickListener bulkInstallListener() {
        return v -> {
            disposable.add(Observable.fromCallable(() -> new BulkDeliveryData(context)
                    .getDeliveryData(selectedApps))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((deliveryDataList) -> {
                        requestList = RequestBuilder.getBulkRequestList(context, deliveryDataList,
                                selectedApps, BULK_GROUP_ID);
                        if (!requestList.isEmpty())
                            fetch.enqueue(requestList, updatedRequestList -> {
                                String bulkInstallText = new StringBuilder()
                                        .append(selectedApps.size())
                                        .append(StringUtils.SPACE)
                                        .append(context.getString(R.string.list_bulk_install)).toString();
                                new QuickNotification(context).show(
                                        context.getString(R.string.app_name),
                                        bulkInstallText,
                                        null);
                            });
                    }, err -> Log.e(err.getMessage())));
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
        File file = new File(PathUtil.getBaseDirectory(context) + Constants.FILES + File.separator + fileExt);
        try {
            Log.e(file.getPath());
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
            disposable.add(Observable.fromCallable(() -> new BulkDetails(context).getRemoteAppList(favouriteList))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(start -> swipeRefreshLayout.setRefreshing(true))
                    .doOnTerminate(() -> swipeRefreshLayout.setRefreshing(false))
                    .doOnComplete(() -> buttonExport.setEnabled(true))
                    .subscribe((appToAdd) -> {
                        favouriteApps = appToAdd;
                        setupFavourites(favouriteApps);
                    }, err -> Log.e(Constants.TAG, err.getMessage())));
    }

    private void toggleEmptyLayout(boolean toggle) {
        emptyLayout.setVisibility(toggle ? View.VISIBLE : View.GONE);
        buttonExport.setEnabled(!toggle);
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
                toggleEmptyLayout(true);
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
            txtCount.setText(String.format(getString(R.string.list_selected), selectedApps.size()));
        }
    }

    @Override
    public void onLoggedIn() {

    }

    @Override
    public void onLoginFailed() {

    }

    @Override
    public void onNetworkFailed() {

    }
}
