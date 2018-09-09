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

package com.dragons.aurora.task;

import android.content.Context;
import android.view.animation.AnimationUtils;

import com.dragons.aurora.AppListIteratorHelper;
import com.dragons.aurora.Aurora;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.adapters.RecyclerAppsAdapter;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.GooglePlayException;
import com.dragons.aurora.playstoreapiv2.IteratorGooglePlayException;
import com.dragons.aurora.playstoreapiv2.UrlIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static com.dragons.aurora.task.playstore.PlayStoreTask.noNetwork;

public class ClusterTaskHelper {

    private DetailsFragment fragment;
    private Context context;
    private RecyclerView recyclerView;
    private Disposable disposable;

    public ClusterTaskHelper(DetailsFragment fragment, RecyclerView recyclerView) {
        this.fragment = fragment;
        this.context = fragment.getContext();
        this.recyclerView = recyclerView;
    }

    public void getClusterApps(String clusterUrl) {
        disposable = Observable.fromCallable(() -> getApps(clusterUrl))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((appList) -> {
                    if (!appList.isEmpty())
                        setupListView(recyclerView, appList);
                }, err -> Timber.e(err));
    }

    private List<App> getApps(String clusterUrl) throws IOException {
        List<App> apps = new ArrayList<>();
        AppListIteratorHelper iterator = new AppListIteratorHelper(new UrlIterator(
                new PlayStoreApiAuthenticator(context).getApi(), clusterUrl));

        try {
            iterator.setGooglePlayApi(new PlayStoreApiAuthenticator(context).getApi());
        } catch (IOException e) {
            Timber.e("Building an api object from preferences failed");
        }

        if (!iterator.hasNext()) {
            return new ArrayList<>();
        }

        while (iterator.hasNext() && apps.isEmpty()) {
            try {
                apps.addAll(iterator.next());
            } catch (IteratorGooglePlayException e) {
                if (null == e.getCause()) {
                    continue;
                }
                if (noNetwork(e.getCause())) {
                    throw (IOException) e.getCause();
                } else if (e.getCause() instanceof GooglePlayException
                        && ((GooglePlayException) e.getCause()).getCode() == 401
                        && PreferenceFragment.getBoolean(context, Aurora.PREFERENCE_APP_PROVIDED_EMAIL)
                ) {
                    PlayStoreApiAuthenticator authenticator = new PlayStoreApiAuthenticator(context);
                    authenticator.refreshToken();
                    iterator.setGooglePlayApi(authenticator.getApi());
                    apps.addAll(iterator.next());
                }
            }
        }
        return apps;
    }

    private void setupListView(RecyclerView recyclerView, List<App> appsToAdd) {
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(context, R.anim.anim_falldown));
        recyclerView.setAdapter(new RecyclerAppsAdapter(fragment, appsToAdd));
    }
}
