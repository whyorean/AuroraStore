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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.dragons.aurora.R;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.dragons.aurora.Util.hide;
import static com.dragons.aurora.Util.isConnected;

public class TopGrossingApps extends TopFreeApps {
    @BindView(R.id.endless_apps_list)
    RecyclerView recyclerView;
    @BindView(R.id.unicorn)
    RelativeLayout unicorn;
    @BindView(R.id.ohhSnap)
    RelativeLayout ohhSnap;
    @BindView(R.id.progress)
    RelativeLayout progress;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.app_endless_inc, container, false);
        ButterKnife.bind(this, view);
        setIterator(setupIterator(CategoryAppsFragment.categoryId, GooglePlayAPI.SUBCATEGORY.TOP_GROSSING));
        setRecyclerView(recyclerView);
        fetchCategoryApps(false);
        Button ohhSnap_retry = view.findViewById(R.id.ohhSnap_retry);
        ohhSnap_retry.setOnClickListener(click -> {
            if (isLoggedIn() && isConnected(getContext())) {
                hide(view, R.id.ohhSnap);
                fetchCategoryApps(false);
            }
        });
        Button retry_querry = view.findViewById(R.id.recheck_query);
        retry_querry.setOnClickListener(click -> {
            if (isLoggedIn() && isConnected(getContext())) {
                hide(view, R.id.unicorn);
                fetchCategoryApps(false);
            }
        });
        return view;
    }
}