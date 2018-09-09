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

import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.HistoryItemTouchHelper;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.DetailsActivity;
import com.dragons.aurora.adapters.RecyclerAppsAdapter;
import com.dragons.aurora.adapters.SearchHistoryAdapter;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.model.App;
import com.dragons.aurora.task.playstore.SearchHistoryTask;
import com.dragons.custom.ClusterAppsCard;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class SearchFragment extends BaseFragment implements HistoryItemTouchHelper.RecyclerItemTouchHelperListener {

    public static String HISTORY_LIST = "HISTORY_LIST";
    public static String HISTORY_APP = "HISTORY_APP";

    @BindView(R.id.search_apps)
    SearchView searchToolbar;
    @BindView(R.id.search_layout)
    CardView search_layout;
    @BindView(R.id.searchHistory)
    RecyclerView searchHistoryRecyclerView;
    @BindView(R.id.m_apps_recycler)
    RecyclerView searchHistoryAppRecyclerView;
    @BindView(R.id.searchClusterApp)
    ClusterAppsCard clusterAppsCard;
    @BindView(R.id.emptyView)
    TextView emptyView;
    @BindView(R.id.clearAll)
    Button clearAll;

    private View view;
    private ArrayList<String> mQueryList;
    private ArrayList<String> mAppList;
    private SearchHistoryAdapter searchHistoryAdapter;
    private RecyclerAppsAdapter searchHistoryAppAdapter;
    private CompositeDisposable mDisposable = new CompositeDisposable();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.fragment_search, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addQueryTextListener(searchToolbar);
        setupHistory();
        setupAppHistory();

        clearAll.setOnClickListener(v -> clearAll());
        search_layout.setOnClickListener(v -> {
            searchToolbar.setFocusable(true);
            searchToolbar.setIconified(false);
            searchToolbar.requestFocusFromTouch();
            searchToolbar.setQuery("", false);
        });
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if (search_layout != null && Prefs.getBoolean(view.getContext(), "SHOW_IME"))
                search_layout.performClick();

            if (searchHistoryAdapter == null)
                setupHistory();
            else
                searchHistoryAdapter.reload();
        }
    }

    private void addQueryTextListener(SearchView searchView) {
        SearchManager searchManager = (SearchManager) getContext().getSystemService(Context.SEARCH_SERVICE);
        if (null != searchManager) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.setQuery("", false);
                searchView.setIconified(true);
                searchView.clearFocus();
                setQuery(query);
                return true;
            }
        });

        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return true;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                Cursor cursor = searchView.getSuggestionsAdapter().getCursor();
                cursor.moveToPosition(position);
                String suggestion = cursor.getString(2);
                searchView.setQuery(suggestion, true);
                return true;
            }
        });
    }

    private void setQuery(String query) {
        if (looksLikeAPackageId(query)) {
            setAppToPref(query);
            getContext().startActivity(DetailsActivity.getDetailsIntent(getContext(), query));
        } else {
            String mDatedQuerry = query.concat(":").concat(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
            setQueryToPref(mDatedQuerry);
            getQueriedApps(query);
        }
    }

    private void setQueryToPref(String query) {
        mQueryList = Prefs.getListString(getContext(), HISTORY_LIST);
        mQueryList.add(0, query);
        Prefs.putListString(getContext(), HISTORY_LIST, mQueryList);
        if (searchHistoryAdapter != null)
            searchHistoryAdapter.reload();
        else
            setupSearchHistory(mQueryList);
    }

    private void setAppToPref(String query) {
        mAppList = Prefs.getListString(getContext(), HISTORY_APP);
        mAppList.add(0, query);
        //Removes dupes
        Set<String> mAppSet = new HashSet<>(mAppList);
        mAppList.clear();
        mAppList.addAll(mAppSet);
        Prefs.putListString(getContext(), HISTORY_APP, mAppList);
    }

    private void setupHistory() {
        mQueryList = Prefs.getListString(getContext(), HISTORY_LIST);
        if (!mQueryList.isEmpty()) {
            setupSearchHistory(mQueryList);
            toggleViews(false);
        } else {
            toggleViews(true);
        }
    }

    private void setupAppHistory() {
        mAppList = Prefs.getListString(getContext(), HISTORY_APP);
        if (!mAppList.isEmpty()) {
            getHistoryApps(mAppList);
        } else
            clusterAppsCard.setVisibility(View.GONE);
    }

    private void setupSearchHistory(ArrayList<String> mHistoryList) {
        searchHistoryAdapter = new SearchHistoryAdapter(this, mHistoryList);
        searchHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        searchHistoryRecyclerView.setItemAnimator(new DefaultItemAnimator());
        DividerItemDecoration itemDecorator = new DividerItemDecoration(searchHistoryRecyclerView.getContext(), DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(getResources().getDrawable(R.drawable.list_divider));
        searchHistoryRecyclerView.addItemDecoration(itemDecorator);
        searchHistoryRecyclerView.setAdapter(searchHistoryAdapter);
        new ItemTouchHelper(
                new HistoryItemTouchHelper(0, ItemTouchHelper.LEFT, this))
                .attachToRecyclerView(searchHistoryRecyclerView);
    }

    private void setupAppSearchHistory(List<App> appsToAdd) {
        searchHistoryAppAdapter = new RecyclerAppsAdapter(this, appsToAdd);
        searchHistoryAppRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        searchHistoryAppRecyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.anim_slideright));
        searchHistoryAppRecyclerView.setAdapter(searchHistoryAppAdapter);
    }

    private void clearAll() {
        if (searchHistoryAdapter != null)
            searchHistoryAdapter.clear();
        if (searchHistoryAppAdapter != null)
            searchHistoryAppAdapter.clear();
        toggleViews(true);
        clusterAppsCard.setVisibility(View.GONE);
    }

    private void toggleViews(Boolean shouldHide) {
        if (shouldHide) {
            searchHistoryRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            searchHistoryRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof SearchHistoryAdapter.ViewHolder) {
            searchHistoryAdapter.remove(position);
            if (searchHistoryAdapter.getItemCount() < 1)
                clearAll();
        }
    }

    private void getQueriedApps(String query) {
        SearchAppsFragment searchAppsFragment = new SearchAppsFragment();
        Bundle arguments = new Bundle();
        arguments.putString("SearchQuery", query);
        arguments.putString("SearchTitle", getTitleString(query));
        searchAppsFragment.setArguments(arguments);
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.container, searchAppsFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

    private String getTitleString(String query) {
        return query.startsWith(Aurora.PUB_PREFIX)
                ? getString(R.string.apps_by, query.substring(Aurora.PUB_PREFIX.length()))
                : getString(R.string.activity_title_search, query)
                ;
    }

    private boolean looksLikeAPackageId(String query) {
        if (TextUtils.isEmpty(query)) {
            return false;
        }
        String pattern = "([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)+[\\p{L}_$][\\p{L}\\p{N}_$]*";
        Pattern r = Pattern.compile(pattern);
        return r.matcher(query).matches();
    }

    private void getHistoryApps(ArrayList<String> appList) {
        TextView clusterTitle = clusterAppsCard.findViewById(R.id.m_apps_title);
        clusterTitle.setText(R.string.action_search_history_apps);
        SearchHistoryTask mTask = new SearchHistoryTask(getContext());
        mDisposable.add(Observable.fromCallable(() -> mTask.getHistoryApps(appList))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> clusterAppsCard.setVisibility(View.VISIBLE))
                .doOnError(err -> clusterAppsCard.setVisibility(View.GONE))
                .subscribe((appToAdd) -> {
                    setupAppSearchHistory(appToAdd);
                }, err -> Timber.e(err.getMessage())));
    }
}
