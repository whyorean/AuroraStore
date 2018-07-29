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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.dragons.aurora.HistoryItemTouchHelper;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.DetailsActivity;
import com.dragons.aurora.adapters.SearchHistoryAdapter;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.task.playstore.SearchHistoryTask;
import com.dragons.custom.ClusterAppsCard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.dragons.aurora.activities.SearchActivity.PUB_PREFIX;

public class SearchFragment extends SearchHistoryTask implements HistoryItemTouchHelper.RecyclerItemTouchHelperListener {
    private SearchView searchToolbar;
    private ClusterAppsCard clusterAppsCard;
    private RecyclerView clusterRecycler;
    private RecyclerView recyclerView;
    private CardView search_layout;
    private TextView emptyView;
    private Button clearAll;
    private View view;
    private ArrayList<String> currList;
    private SearchHistoryAdapter searchHistoryAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view != null) {
            if ((ViewGroup) view.getParent() != null)
                ((ViewGroup) view.getParent()).removeView(view);
            return view;
        }
        view = inflater.inflate(R.layout.fragment_search, container, false);
        init();
        clearAll.setOnClickListener(v -> clearAll());
        search_layout.setOnClickListener(v -> {
            searchToolbar.setFocusable(true);
            searchToolbar.setIconified(false);
            searchToolbar.requestFocusFromTouch();
            searchToolbar.setQuery("", false);
        });
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addQueryTextListener(searchToolbar);
        setupSearchHistory();
        getHistoryApps(getRecentAppsList());
    }

    private void init() {
        searchToolbar = view.findViewById(R.id.search_apps);
        clusterAppsCard = view.findViewById(R.id.searchClusterApp);
        clusterRecycler = view.findViewById(R.id.m_apps_recycler);
        recyclerView = view.findViewById(R.id.searchHistory);
        search_layout = view.findViewById(R.id.search_layout);
        emptyView = view.findViewById(R.id.emptyView);
        clearAll = view.findViewById(R.id.clearAll);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSearchHistory();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if (search_layout != null && Prefs.getBoolean(view.getContext(), "SHOW_IME"))
                search_layout.performClick();
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
            addRecentApps(query);
            getContext().startActivity(DetailsActivity.getDetailsIntent(getContext(), query));
        } else {
            addHistory(query);
            getCategoryApps(query);
        }
    }

    private void setupSearchHistory() {
        currList = getHistoryList();
        toggleEmptyRecycle(currList);
        searchHistoryAdapter = new SearchHistoryAdapter(this, currList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(searchHistoryAdapter);
        new ItemTouchHelper(
                new HistoryItemTouchHelper(0, ItemTouchHelper.LEFT, this))
                .attachToRecyclerView(recyclerView);
    }

    private void updateSearchHistory() {
        if (searchHistoryAdapter != null) {
            currList = getHistoryList();
            toggleEmptyRecycle(currList);
            searchHistoryAdapter.queryHistory = currList;
            searchHistoryAdapter.notifyDataSetChanged();
        }
    }

    private void updateHistoryPref() {
        Set<String> updatedSet = new HashSet<>();
        updatedSet.addAll(currList);
        writeToPref("SEARCH_HISTORY", updatedSet);
        if (recyclerView.getAdapter().getItemCount() == 0) {
            toggleEmptyRecycle(currList);
        }
    }

    private void clearAll() {
        if (searchHistoryAdapter != null) {
            currList = new ArrayList<>();
            searchHistoryAdapter.queryHistory = currList;
            searchHistoryAdapter.notifyDataSetChanged();
        }
        writeToPref("SEARCH_HISTORY", new HashSet<>());
        writeToPref("APP_HISTORY", new HashSet<>());
        clusterAppsCard.setVisibility(View.GONE);
        toggleEmptyRecycle(currList);
    }

    private void toggleEmptyRecycle(ArrayList<String> currList) {
        if (currList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void getHistoryApps(ArrayList<String> appList) {
        if (!appList.isEmpty()) {
            TextView clusterTitle = clusterAppsCard.findViewById(R.id.m_apps_title);
            clusterTitle.setText(R.string.action_search_history_apps);
            clusterTitle.setTextSize(18);
            Observable.fromCallable(() -> getHistoryApps(new PlayStoreApiAuthenticator(this.getActivity()).getApi(), appList))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((appToAdd) -> {
                        if (view != null) {
                            setupRecyclerView(clusterRecycler, appToAdd);
                        }
                    }, this::processException);
        } else clusterAppsCard.setVisibility(View.GONE);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof SearchHistoryAdapter.ViewHolder) {
            searchHistoryAdapter.remove(position);
            currList = searchHistoryAdapter.queryHistory;
            updateHistoryPref();
        }
    }

    private void getCategoryApps(String query) {
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
        return query.startsWith(PUB_PREFIX)
                ? getString(R.string.apps_by, query.substring(PUB_PREFIX.length()))
                : getString(R.string.activity_title_search, query)
                ;
    }

}
