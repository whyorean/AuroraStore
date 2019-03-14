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

package com.aurora.store.fragment;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.HistoryItemTouchHelper;
import com.aurora.store.R;
import com.aurora.store.activity.DetailsActivity;
import com.aurora.store.adapter.SearchHistoryAdapter;
import com.aurora.store.utility.PrefUtil;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SearchFragment extends BaseFragment implements BaseFragment.EventListenerImpl, HistoryItemTouchHelper.RecyclerItemTouchHelperListener {

    @BindView(R.id.search_apps)
    SearchView searchView;
    @BindView(R.id.searchHistory)
    RecyclerView mRecyclerView;
    @BindView(R.id.emptyView)
    TextView emptyView;
    @BindView(R.id.clearAll)
    Button clearAll;

    private Context context;
    private ArrayList<String> mQueryList;
    private SearchHistoryAdapter searchHistoryAdapter;

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addQueryTextListener();
        setupHistory();
        clearAll.setOnClickListener(v -> clearAll());
    }

    private void addQueryTextListener() {
        SearchManager searchManager = (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
        ComponentName componentName = getActivity().getComponentName();
        if (null != searchManager && componentName != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName));
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
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
            context.startActivity(DetailsActivity.getDetailsIntent(getContext(), query));
        } else {
            String mDatedQuery = query
                    .concat(":")
                    .concat(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(new Date()));
            setQueryToPref(mDatedQuery);
            getQueriedApps(query);
        }
    }

    private void setQueryToPref(String query) {
        mQueryList = PrefUtil.getListString(context, Constants.RECENT_HISTORY);
        mQueryList.add(0, query);
        PrefUtil.putListString(context, Constants.RECENT_HISTORY, mQueryList);
        if (searchHistoryAdapter != null)
            searchHistoryAdapter.reload();
        else
            setupSearchHistory(mQueryList);
    }

    private void setupHistory() {
        mQueryList = PrefUtil.getListString(context, Constants.RECENT_HISTORY);
        if (!mQueryList.isEmpty()) {
            setupSearchHistory(mQueryList);
            toggleViews(false);
        } else {
            toggleViews(true);
        }
    }

    private void setupSearchHistory(ArrayList<String> mHistoryList) {
        searchHistoryAdapter = new SearchHistoryAdapter(this, mHistoryList);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(searchHistoryAdapter);
        new ItemTouchHelper(
                new HistoryItemTouchHelper(0, ItemTouchHelper.LEFT, this))
                .attachToRecyclerView(mRecyclerView);
    }

    private void clearAll() {
        if (searchHistoryAdapter != null)
            searchHistoryAdapter.clear();
        toggleViews(true);
    }

    private void toggleViews(Boolean shouldHide) {
        if (shouldHide) {
            mRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
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
        return query.startsWith(Constants.PUB_PREFIX)
                ? getString(R.string.apps_by, query.substring(Constants.PUB_PREFIX.length()))
                : getString(R.string.title_search_result, query)
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
