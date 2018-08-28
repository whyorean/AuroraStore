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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.dragons.aurora.R;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class TopGrossingApps extends TopFreeApps {

    @BindView(R.id.endless_apps_list)
    RecyclerView recyclerView;
    @BindView(R.id.unicorn)
    RelativeLayout unicorn;
    @BindView(R.id.ohhSnap)
    RelativeLayout ohhSnap;
    @BindView(R.id.progress)
    RelativeLayout progress;
    @BindView(R.id.ohhSnap_retry)
    Button ohhSnap_retry;
    @BindView(R.id.recheck_query)
    Button retry_query;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_endless_categorized, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void init() {
        setIterator(setupIterator(CategoryAppsFragment.categoryId, GooglePlayAPI.SUBCATEGORY.TOP_GROSSING));
    }
}