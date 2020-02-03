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

package com.aurora.store.ui.details;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.EndlessScrollListener;
import com.aurora.store.R;
import com.aurora.store.model.Review;
import com.aurora.store.section.ReviewsSection;
import com.aurora.store.ui.single.activity.BaseActivity;
import com.aurora.store.viewmodel.ReviewsModel;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class ReviewsActivity extends BaseActivity {

    private static final String TAG_REVIEWS = "TAG_REVIEWS";

    @BindView(R.id.reviews_recycler)
    RecyclerView recyclerView;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.chip_group)
    ChipGroup chipGroup;

    private String packageName;
    private ReviewsModel reviewsModel;
    private ReviewsSection reviewsSection;
    private SectionedRecyclerViewAdapter viewAdapter;

    private GooglePlayAPI.REVIEW_SORT reviewSort = GooglePlayAPI.REVIEW_SORT.HIGHRATING;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);
        ButterKnife.bind(this);
        setupActionBar();
        setupRecycler();

        Intent intent = getIntent();
        if (intent != null) {
            packageName = intent.getStringExtra(Constants.INTENT_PACKAGE_NAME);
            if (packageName != null && !packageName.isEmpty()) {
                reviewsModel = new ViewModelProvider(this).get(ReviewsModel.class);
                reviewsModel.getReviews().observe(this, reviewList -> {
                    dispatchToAdapter(reviewList);
                });
                reviewsModel.fetchReviews(packageName, reviewSort, true);
            }
        }

        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.chip_high:
                    reviewSort = GooglePlayAPI.REVIEW_SORT.HIGHRATING;
                    break;
                case R.id.chip_helpful:
                    reviewSort = GooglePlayAPI.REVIEW_SORT.HELPFUL;
                    break;
                case R.id.chip_new:
                    reviewSort = GooglePlayAPI.REVIEW_SORT.NEWEST;
                    break;
            }
            removeAllReviews();
            reviewsModel.fetchReviews(packageName, reviewSort, true);
        });
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void setupActionBar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(getString(R.string.details_reviews));
        }
    }

    private void dispatchToAdapter(List<Review> newList) {
        List<Review> oldList = reviewsSection.getList();
        if (oldList.isEmpty()) {
            reviewsSection.updateList(newList);
            viewAdapter.notifyDataSetChanged();
        } else {
            if (!newList.isEmpty()) {
                for (Review review : newList)
                    reviewsSection.add(review);
                viewAdapter.notifyItemInserted(reviewsSection.getCount() - 1);
            }
        }
    }

    private void removeAllReviews() {
        reviewsSection.clearReviews();
        viewAdapter.notifyDataSetChanged();
    }

    private void setupRecycler() {
        reviewsSection = new ReviewsSection(this);
        viewAdapter = new SectionedRecyclerViewAdapter();
        viewAdapter.addSection(TAG_REVIEWS, reviewsSection);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        EndlessScrollListener endlessScrollListener = new EndlessScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                reviewsModel.fetchReviews(packageName, reviewSort, false);
            }
        };
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addOnScrollListener(endlessScrollListener);
        recyclerView.setAdapter(viewAdapter);
    }
}
