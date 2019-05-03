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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Filter;
import com.aurora.store.R;
import com.aurora.store.utility.ContextUtil;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TopTrendingApps extends TopFreeApps {

    @BindView(R.id.endless_apps_list)
    RecyclerView recyclerView;

    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_applist, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void init() {
        iterator = setupIterator(CategoryAppsFragment.categoryId, GooglePlayAPI.SUBCATEGORY.MOVERS_SHAKERS);
        if (iterator != null) {
            iterator.setFilter(new Filter(context).getFilterPreferences());
            iterator.setEnableFilter(true);
            setIterator(iterator);
        }
    }

    @Override
    protected View.OnClickListener errRetry() {
        return v -> ContextUtil.runOnUiThread(() -> fetchCategoryApps(false));
    }
}