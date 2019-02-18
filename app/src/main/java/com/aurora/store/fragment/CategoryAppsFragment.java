package com.aurora.store.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.viewpager.widget.ViewPager;

import com.aurora.store.R;
import com.aurora.store.activity.CategoriesActivity;
import com.aurora.store.adapter.SubCategoryAdapter;
import com.aurora.store.sheet.FilterBottomSheet;
import com.aurora.store.utility.Log;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CategoryAppsFragment extends BaseFragment implements BaseFragment.EventListenerImpl {

    public static String categoryId;

    @BindView(R.id.view_pager)
    ViewPager viewPager;
    @BindView(R.id.category_tabs)
    TabLayout tabLayout;
    @BindView(R.id.filter_fab)
    FloatingActionButton filterFab;
    private ActionBar mActionBar;

    public FloatingActionButton getFilterFab() {
        return filterFab;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_category_container, container, false);
        ButterKnife.bind(this, view);
        Bundle arguments = getArguments();
        if (arguments != null) {
            categoryId = arguments.getString("CategoryId");
            if (getActivity() instanceof CategoriesActivity) {
                mActionBar = ((CategoriesActivity) getActivity()).getSupportActionBar();
                mActionBar.setTitle(arguments.getString("CategoryName"));
            }
        } else
            Log.e("No category id provided");
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SubCategoryAdapter categoryFilterAdapter = new SubCategoryAdapter(getContext(), getChildFragmentManager());
        viewPager.setAdapter(categoryFilterAdapter);
        viewPager.setOffscreenPageLimit(3);
        tabLayout.setupWithViewPager(viewPager);
        filterFab.setOnClickListener(v -> {
            getFilterDialog();
        });
    }

    @Override
    public void onDestroy() {
        Glide.with(this).pauseAllRequests();
        if (mActionBar != null)
            mActionBar.setTitle(getString(R.string.app_name));
        super.onDestroy();
    }

    private void getFilterDialog() {
        FilterBottomSheet filterSheet = new FilterBottomSheet();
        filterSheet.setOnApplyListener(v -> {
            filterSheet.dismiss();
            viewPager.setAdapter(new SubCategoryAdapter(getContext(), getChildFragmentManager()));
        });
        filterSheet.show(getChildFragmentManager(), "FILTER");
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