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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.EndlessScrollListener;
import com.aurora.store.ErrorType;
import com.aurora.store.Filter;
import com.aurora.store.R;
import com.aurora.store.activity.AuroraActivity;
import com.aurora.store.adapter.EndlessAppsAdapter;
import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.iterator.CustomAppListIterator;
import com.aurora.store.model.App;
import com.aurora.store.sheet.FilterBottomSheet;
import com.aurora.store.task.SearchTask;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.NetworkUtil;
import com.aurora.store.utility.ViewUtil;
import com.aurora.store.view.ErrorView;
import com.bumptech.glide.Glide;
import com.dragons.aurora.playstoreapiv2.SearchIterator;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class SearchAppsFragment extends BaseFragment implements BaseFragment.EventListenerImpl {

    @BindView(R.id.view_switcher)
    ViewSwitcher mViewSwitcher;
    @BindView(R.id.content_view)
    LinearLayout layoutContent;
    @BindView(R.id.err_view)
    LinearLayout layoutError;
    @BindView(R.id.search_apps_list)
    RecyclerView recyclerView;
    @BindView(R.id.filter_fab)
    FloatingActionButton filterFab;
    @BindView(R.id.searchQuery)
    EditText searchQuery;

    private Context context;
    private View view;
    private String query;
    private BottomNavigationView mBottomNavigationView;
    private CustomAppListIterator iterator;
    private CompositeDisposable mDisposable = new CompositeDisposable();
    private EndlessAppsAdapter endlessAppsAdapter;
    private SearchTask mSearchTask;

    private String getQuery() {
        return query;
    }

    private void setQuery(String query) {
        this.query = query;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        mSearchTask = new SearchTask(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_search_applist, container, false);
        ButterKnife.bind(this, view);
        Bundle arguments = getArguments();
        if (arguments != null) {
            setQuery(arguments.getString("SearchQuery"));
            searchQuery.setText(getQuery());
            if (NetworkUtil.isConnected(context)) {
                fetchSearchAppsList(false);
            } else
                onNetworkFailed();
        } else
            Log.e("No category id provided");
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        filterFab.show();
        filterFab.setOnClickListener(v -> getFilterDialog());
        setupQueryEdit();
        setErrorView(ErrorType.UNKNOWN);
        if (getActivity() instanceof AuroraActivity)
            mBottomNavigationView = ((AuroraActivity) getActivity()).getBottomNavigation();
        if (mBottomNavigationView != null)
            ViewUtil.hideBottomNav(mBottomNavigationView, true);
    }

    @Override
    public void onDestroy() {
        Glide.with(this).pauseAllRequests();
        mDisposable.dispose();
        if (mBottomNavigationView != null)
            ViewUtil.showBottomNav(mBottomNavigationView, true);
        super.onDestroy();
    }

    private void setupQueryEdit() {
        searchQuery.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                searchQuery.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_search, 0);
            else
                searchQuery.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_edit, 0);
        });
        searchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                query = searchQuery.getText().toString();
                iterator = setupIterator(getQuery());
                fetchSearchAppsList(false);
                searchQuery.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_edit, 0);
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            } else
                return false;
        });
    }

    private void getFilterDialog() {
        FilterBottomSheet filterSheet = new FilterBottomSheet();
        filterSheet.setOnApplyListener(v -> {
            filterSheet.dismiss();
            recyclerView.removeAllViewsInLayout();
            fetchSearchAppsList(false);
        });
        filterSheet.show(getChildFragmentManager(), "FILTER");
    }

    private CustomAppListIterator setupIterator(String query) {
        CustomAppListIterator iterator;
        try {
            iterator = new CustomAppListIterator(new SearchIterator(new PlayStoreApiAuthenticator(getContext()).getApi(), query));
            iterator.setEnableFilter(true);
            iterator.setFilter(new Filter(getContext()).getFilterPreferences());
            return iterator;
        } catch (Exception e) {
            processException(e);
            return null;
        }
    }

    private void fetchSearchAppsList(boolean shouldIterate) {
        if (!shouldIterate)
            iterator = setupIterator(getQuery());
        mDisposable.add(Observable.fromCallable(() -> mSearchTask.getSearchResults(iterator))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(appList -> {
                    if (view != null) {
                        if (shouldIterate) {
                            addApps(appList);
                        } else if (appList.isEmpty()) {
                            setErrorView(ErrorType.NO_SEARCH);
                            switchViews(true);
                        } else {
                            switchViews(false);
                            setupRecycler(appList);
                        }
                    }
                }, err -> {
                    Log.e(err.getMessage());
                    processException(err);
                }));
    }

    private void addApps(List<App> appsToAdd) {
        if (!appsToAdd.isEmpty()) {
            for (App app : appsToAdd)
                endlessAppsAdapter.add(app);
            endlessAppsAdapter.notifyItemInserted(endlessAppsAdapter.getItemCount() - 1);
        }
        if (iterator.hasNext() && endlessAppsAdapter.getItemCount() < 10) {
            iterator.next();
        }
    }

    private void setupRecycler(List<App> appsToAdd) {
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        endlessAppsAdapter = new EndlessAppsAdapter(context, appsToAdd);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this.getActivity(), R.anim.anim_falldown));
        recyclerView.setAdapter(endlessAppsAdapter);
        EndlessScrollListener mScrollListener = new EndlessScrollListener(mLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                fetchSearchAppsList(true);
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
        recyclerView.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                if (velocityY < 0) {
                    filterFab.show();
                } else if (velocityY > 0) {
                    filterFab.hide();
                }
                return false;
            }
        });
    }

    private void switchViews(boolean showError) {
        if (mViewSwitcher.getCurrentView() == layoutContent && showError)
            mViewSwitcher.showNext();
        else if (mViewSwitcher.getCurrentView() == layoutError && !showError)
            mViewSwitcher.showPrevious();
    }

    private void setErrorView(ErrorType errorType) {
        layoutError.removeAllViews();
        layoutError.addView(new ErrorView(context, errorType, retry()));
    }

    private View.OnClickListener retry() {
        return v -> {
            if (NetworkUtil.isConnected(context)) {
                fetchSearchAppsList(false);
            } else {
                setErrorView(ErrorType.NO_NETWORK);
            }
            ((Button) v).setText(getString(R.string.action_retry_ing));
            ((Button) v).setEnabled(false);
        };
    }

    @Override
    public void onLoggedIn() {
        fetchSearchAppsList(false);
    }

    @Override
    public void onLoginFailed() {
        setErrorView(ErrorType.UNKNOWN);
        switchViews(true);

    }

    @Override
    public void onNetworkFailed() {
        setErrorView(ErrorType.NO_NETWORK);
        switchViews(true);
    }
}