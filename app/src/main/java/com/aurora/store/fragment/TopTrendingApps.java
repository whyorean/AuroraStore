package com.aurora.store.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.R;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TopTrendingApps extends TopFreeApps implements BaseFragment.EventListenerImpl {

    @BindView(R.id.endless_apps_list)
    RecyclerView recyclerView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_applist, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void init() {
        setIterator(setupIterator(CategoryAppsFragment.categoryId, GooglePlayAPI.SUBCATEGORY.MOVERS_SHAKERS));
    }


    @Override
    public void onLoggedIn() {
        fetchCategoryApps(false);
    }

    @Override
    public void onLoginFailed() {

    }

    @Override
    public void onNetworkFailed() {

    }
}