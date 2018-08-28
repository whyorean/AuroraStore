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

import com.dragons.aurora.HistoryItemTouchHelper;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.DetailsActivity;
import com.dragons.aurora.adapters.RecyclerAppsAdapter;
import com.dragons.aurora.adapters.SearchHistoryAdapter;
import com.dragons.aurora.database.Jessie;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.model.App;
import com.dragons.aurora.model.AppBuilder;
import com.dragons.aurora.model.History;
import com.dragons.aurora.playstoreapiv2.DetailsResponse;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.task.playstore.ExceptionTask;
import com.dragons.custom.ClusterAppsCard;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

import static com.dragons.aurora.activities.SearchActivity.PUB_PREFIX;

public class SearchFragment extends BaseFragment implements HistoryItemTouchHelper.RecyclerItemTouchHelperListener {

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
    private Jessie mJessie;
    private SearchHistoryAdapter searchHistoryAdapter;
    private RecyclerAppsAdapter searchHistoryAppAdapter;

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
        mJessie = new Jessie(getContext());
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
    public void onResume() {
        super.onResume();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if (search_layout != null && Prefs.getBoolean(view.getContext(), "SHOW_IME"))
                search_layout.performClick();

            if (searchHistoryAdapter != null)
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
            getAppDetail(query);
            getContext().startActivity(DetailsActivity.getDetailsIntent(getContext(), query));
        } else {
            History mHistory = new History();
            mHistory.setQuery(query);
            mHistory.setDate(new SimpleDateFormat("dd/MM/yyyy",
                    Locale.getDefault()).format(new Date()));
            if (searchHistoryAdapter != null)
                searchHistoryAdapter.add(searchHistoryAdapter.getItemCount() + 1, mHistory);
            else
                mJessie.addSingleHistory(mHistory);
            getQuerriedApps(query);
        }
    }

    private void setupHistory() {
        if (mJessie.isJsonAvailable(Jessie.JSON_HISTORY)) {
            JSONArray mJsonArray = mJessie.readJsonArrayFromFile(Jessie.JSON_HISTORY);
            List<History> mHistoryList = mJessie.getHistoryFromJsonArray(mJsonArray);
            setupSearchHistory(mHistoryList);
            toggleViews(false);
        } else {
            toggleViews(true);
        }
    }

    private void setupAppHistory() {
        if (mJessie.isJsonAvailable(Jessie.JSON_APP_HISTORY)) {
            JSONArray mJsonArray = mJessie.readJsonArrayFromFile(Jessie.JSON_APP_HISTORY);
            List<App> mAppList = mJessie.getAppsFromJsonArray(mJsonArray);
            setupAppSearchHistory(mAppList);
            clusterAppsCard.setVisibility(View.VISIBLE);
        } else
            clusterAppsCard.setVisibility(View.GONE);
    }

    private void setupSearchHistory(List<History> mHistoryList) {
        searchHistoryAdapter = new SearchHistoryAdapter(this, mHistoryList);
        searchHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        searchHistoryRecyclerView.setItemAnimator(new DefaultItemAnimator());
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

    private void getQuerriedApps(String query) {
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

    private boolean looksLikeAPackageId(String query) {
        if (TextUtils.isEmpty(query)) {
            return false;
        }
        String pattern = "([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)+[\\p{L}_$][\\p{L}\\p{N}_$]*";
        Pattern r = Pattern.compile(pattern);
        return r.matcher(query).matches();
    }

    private void getAppDetail(String packageName) {
        try {
            GooglePlayAPI api = new PlayStoreApiAuthenticator(this.getActivity()).getApi();
            DetailsResponse response = api.details(packageName);
            App mApp = AppBuilder.build(response);
            mJessie.addSingleApp(mApp);
        } catch (Exception e) {
            new ExceptionTask(getContext()).processException(e);
        }
    }
}
