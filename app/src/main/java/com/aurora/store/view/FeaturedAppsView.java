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

package com.aurora.store.view;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.R;
import com.aurora.store.activity.LeaderBoardActivity;
import com.aurora.store.adapter.ClusterAppsAdapter;
import com.aurora.store.adapter.FeaturedAppsAdapter;
import com.aurora.store.model.App;
import com.aurora.store.task.FeaturedApps;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.Util;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class FeaturedAppsView extends RelativeLayout {

    Context context;
    String label;
    String categoryId;

    private TextView categoryName;
    private RecyclerView recyclerView;
    private Button buttonMore;
    private CompositeDisposable mDisposable = new CompositeDisposable();

    public FeaturedAppsView(Context context, String label, String categoryId) {
        super(context);
        this.context = context;
        this.label = label;
        this.categoryId = categoryId;
        init(context);
    }

    public FeaturedAppsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        View view = inflate(context, R.layout.view_featured_apps, this);
        categoryName = view.findViewById(R.id.category_name);
        recyclerView = view.findViewById(R.id.category_recycler);
        buttonMore = view.findViewById(R.id.btn_more);
        categoryName.setText(label);
        buttonMore.setOnClickListener(openLeaderBoardActivity());
        fetchCategoryApps();
    }

    public void fetchCategoryApps() {
        mDisposable.add(Observable.fromCallable(() ->
                new FeaturedApps(getContext()).getApps(categoryId, Util.getSubCategory(getContext())))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((appList) -> {
                    if (!appList.isEmpty())
                        setupRecycler(appList);
                }, err -> {
                    Log.e(err.getMessage());
                }));
    }

    private void setupRecycler(List<App> appList) {
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.anim_slideright));
        recyclerView.setAdapter(Util.isLegacyCardEnabled(context)
                ? new ClusterAppsAdapter(context, appList)
                : new FeaturedAppsAdapter(context, appList));
        if (Util.snapPagerEnabled(context) && !Util.isLegacyCardEnabled(context)) {
            PagerSnapHelper mSnapHelper = new PagerSnapHelper();
            mSnapHelper.attachToRecyclerView(recyclerView);
        }
    }

    private View.OnClickListener openLeaderBoardActivity() {
        return v -> {
            Intent intent = new Intent(getContext(), LeaderBoardActivity.class);
            intent.putExtra("INTENT_CATEGORY", categoryId);
            intent.putExtra("INTENT_SUBCATEGORY", "");
            intent.putExtra("INTENT_TITLE", label);
            getContext().startActivity(intent);
        };
    }
}
