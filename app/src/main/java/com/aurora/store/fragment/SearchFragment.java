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
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.HistoryItemTouchHelper;
import com.aurora.store.R;
import com.aurora.store.activity.AuroraActivity;
import com.aurora.store.adapter.SearchHistoryAdapter;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.Util;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SearchFragment extends Fragment implements HistoryItemTouchHelper.RecyclerItemTouchHelperListener, SearchHistoryAdapter.onClickListener {

    @BindView(R.id.search_apps)
    SearchView searchView;
    @BindView(R.id.searchHistory)
    RecyclerView recyclerView;
    @BindView(R.id.emptyView)
    TextView emptyView;
    @BindView(R.id.clearAll)
    Button clearAll;

    private Context context;
    private ArrayList<String> queryList;
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
        setupSearch();
        setupHistory();
        clearAll.setOnClickListener(v -> clearAll());
    }

    @Override
    public void onResume() {
        super.onResume();
        Util.toggleSoftInput(context, true);
        if (searchView != null && Util.isIMEEnabled(context))
            searchView.requestFocus();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Util.toggleSoftInput(context, false);
    }

    @Override
    public void onStop() {
        super.onStop();
        Util.toggleSoftInput(context, false);
    }

    private void setupSearch() {
        SearchManager searchManager = (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
        ComponentName componentName = getActivity().getComponentName();

        if (null != searchManager && componentName != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName));
        }

        if (!StringUtils.isEmpty(AuroraActivity.externalQuery))
            setQuery(AuroraActivity.externalQuery);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String query) {
                return false;
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
                if (position == 0) {
                    searchView.setQuery(cursor.getString(2), true);
                    searchView.setQuery(cursor.getString(1), false);
                    saveQuery(cursor.getString(1));
                } else
                    searchView.setQuery(cursor.getString(1), true);
                return true;
            }
        });
    }

    private void setQuery(String query) {
        if (looksLikeAPackageId(query)) {
            Bundle bundle = new Bundle();
            bundle.putString("PACKAGE_NAME", query);
            NavHostFragment.findNavController(this).navigate(R.id.detailsFragment, bundle);
        } else {
            getQueriedApps(query);
            saveQuery(query);
        }
    }

    private void saveQuery(String query) {
        String mDatedQuery = query + ":" + Calendar.getInstance().getTimeInMillis();
        setQueryToPref(mDatedQuery);
    }

    private void setQueryToPref(String query) {
        queryList = PrefUtil.getListString(context, Constants.RECENT_HISTORY);
        queryList.add(0, query);
        PrefUtil.putListString(context, Constants.RECENT_HISTORY, queryList);
        if (searchHistoryAdapter != null)
            searchHistoryAdapter.reload();
        else
            setupSearchHistory(queryList);
    }

    private void setupHistory() {
        queryList = PrefUtil.getListString(context, Constants.RECENT_HISTORY);
        if (!queryList.isEmpty()) {
            setupSearchHistory(queryList);
            toggleViews(false);
        } else {
            toggleViews(true);
        }
    }

    private void setupSearchHistory(ArrayList<String> searchQueryList) {
        searchHistoryAdapter = new SearchHistoryAdapter(context, searchQueryList);
        searchHistoryAdapter.setOnItemClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(searchHistoryAdapter);
        new ItemTouchHelper(
                new HistoryItemTouchHelper(0, ItemTouchHelper.LEFT, this))
                .attachToRecyclerView(recyclerView);
    }

    private void clearAll() {
        if (searchHistoryAdapter != null)
            searchHistoryAdapter.clear();
        toggleViews(true);
    }

    private void toggleViews(Boolean shouldHide) {
        if (shouldHide) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
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
        if (searchView != null)
            searchView.setQuery("", false);
        Bundle arguments = new Bundle();
        arguments.putString("SearchQuery", query);
        arguments.putString("SearchTitle", getTitleString(query));
        NavHostFragment.findNavController(this).navigate(R.id.search_to_applist, arguments);
    }

    private String getTitleString(String query) {
        return query.startsWith(Constants.PUB_PREFIX)
                ? context.getString(R.string.apps_by) + StringUtils.SPACE + query.substring(Constants.PUB_PREFIX.length())
                : context.getString(R.string.title_search_result) + StringUtils.SPACE + query;
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
    public void onItemClick(int position, View v) {
        String query = searchHistoryAdapter.getQueryList().get(position);
        String[] splitQuery = query.split(":");
        getQueriedApps(splitQuery[0]);
    }

    @Override
    public void onItemLongClick(int position, View v) {

    }
}
