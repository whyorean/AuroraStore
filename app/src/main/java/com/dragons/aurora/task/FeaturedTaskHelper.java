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

package com.dragons.aurora.task;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.animation.AnimationUtils;

import com.dragons.aurora.R;
import com.dragons.aurora.adapters.FeaturedAppsAdapter;
import com.dragons.aurora.model.App;

import java.util.List;

public class FeaturedTaskHelper extends CategoryTaskHelper {

    private Context context;
    private RecyclerView recyclerView;

    public FeaturedTaskHelper(Context context, RecyclerView recyclerView) {
        super(context, recyclerView);
        this.context = context;
        this.recyclerView = recyclerView;
    }

    @Override
    public void setupListView(RecyclerView recyclerView, List<App> appsToAdd) {
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(context, R.anim.layout_anim));
        recyclerView.setAdapter(new FeaturedAppsAdapter(context, appsToAdd));
    }
}
