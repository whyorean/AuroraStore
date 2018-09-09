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
import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.CredentialsEmptyException;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.adapters.RecyclerAppsAdapter;
import com.dragons.aurora.database.Jessie;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.CategoryAppsIterator;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.GooglePlayException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static com.dragons.aurora.task.playstore.PlayStoreTask.noNetwork;

public class CategoryTaskHelper {

    protected Context context;
    private RecyclerView recyclerView;
    private CompositeDisposable mDisposable = new CompositeDisposable();
    private String categoryID;
    private Jessie mJessie;

    public CategoryTaskHelper(Context context, RecyclerView recyclerView) {
        this.context = context;
        this.recyclerView = recyclerView;
        mJessie = new Jessie(context);
    }

    public String getCategoryID() {
        return categoryID;
    }

    public void setCategoryID(String categoryID) {
        this.categoryID = categoryID;
    }

    public void getCategoryApps(String categoryId, GooglePlayAPI.SUBCATEGORY subCategory) {
        setCategoryID(categoryId);
        if (mJessie.isJsonAvailable(categoryId) && mJessie.isJasonValid(categoryId)) {
            JSONArray mJsonArray = mJessie.readJsonArrayFromFile(categoryId);
            List<App> mApps = mJessie.getAppsFromJsonArray(mJsonArray);
            setupListView(recyclerView, mApps);
        } else {
            mDisposable.add(Observable.fromCallable(() -> getApps(categoryId, subCategory))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((appList) -> {
                        if (!appList.isEmpty())
                            addToDatabase(appList);
                    }, err -> {
                        if (err instanceof AuthException)
                            processAuthException((AuthException) err);
                        Timber.e(err);
                    }));
        }
    }

    private void addToDatabase(List<App> mAppList) {
        List<JSONObject> mJsonObjects = mJessie.getJsonObjects(mAppList);
        JSONArray mJsonArray = mJessie.getJsonArray(mJsonObjects);
        mJessie.writeJsonToFile(getCategoryID(), mJsonArray);
        setupListView(recyclerView, mAppList);
    }

    private List<App> getApps(String categoryId, GooglePlayAPI.SUBCATEGORY subCategory) throws IOException {
        List<App> apps = new ArrayList<>();
        AppListIteratorHelper iterator = null;

        try {
            iterator = new AppListIteratorHelper(new CategoryAppsIterator(
                    new PlayStoreApiAuthenticator(context).getApi(), categoryId, subCategory));
            iterator.setGooglePlayApi(new PlayStoreApiAuthenticator(context).getApi());
        } catch (IOException e) {
            if (e instanceof CredentialsEmptyException)
                ContextUtil.toastShort(context, "You are logged out");
            else
                Timber.e("Building an api object from preferences failed");
        }

        if (iterator != null && !iterator.hasNext()) {
            return new ArrayList<>();
        }

        while (iterator != null && iterator.hasNext() && apps.isEmpty()) {
            try {
                apps.addAll(iterator.next());
            } catch (Exception e) {
                if (null == e.getCause()) {
                    continue;
                }
                if (noNetwork(e.getCause())) {
                    throw (IOException) e.getCause();
                } else if (e.getCause() instanceof GooglePlayException
                        && ((GooglePlayException) e.getCause()).getCode() == 401
                        && Accountant.isDummy(context)) {
                    PlayStoreApiAuthenticator authenticator = new PlayStoreApiAuthenticator(context);
                    authenticator.refreshToken();
                    iterator.setGooglePlayApi(authenticator.getApi());
                    apps.addAll(iterator.next());
                }
                Timber.i(e.getMessage());
            }
        }
        return apps;
    }

    public void setupListView(RecyclerView recyclerView, List<App> appsToAdd) {
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(context, R.anim.anim_slideright));
        recyclerView.setAdapter(new RecyclerAppsAdapter(context, appsToAdd));
    }

    private void processAuthException(AuthException e) {
        if (e instanceof CredentialsEmptyException) {
            Timber.i("Credentials empty");
            Accountant.loginWithDummy(context);
        } else if (e.getCode() == 401 && Accountant.isDummy(context)) {
            Timber.i("Token is stale");
            Accountant.refreshMyToken(context);
        } else {
            ContextUtil.toast(context, R.string.error_incorrect_password);
            Timber.e(e);
            new PlayStoreApiAuthenticator(context).logout();
        }
    }
}
