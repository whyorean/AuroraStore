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
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aurora.store.R;
import com.aurora.store.activity.CategoriesActivity;
import com.aurora.store.view.FeaturedAppsView;
import com.google.android.material.chip.Chip;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class GamesFragment extends ContainerFragment {

    @BindView(R.id.bulk_layout)
    LinearLayout bulk_layout;
    @BindView(R.id.all_chip)
    Chip allChip;

    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_games, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        allChip.setOnClickListener(v -> {
            Intent intent = new Intent(context, CategoriesActivity.class);
            intent.putExtra("INTENT_CATEGORY", "GAME");
            context.startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bulk_layout.getChildCount() < 1)
            addViews();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposable.clear();
    }

    private void addViews() {
        bulk_layout.removeAllViews();
        disposable.add(Observable.just(
                new FeaturedAppsView(context, translator.getString("GAME_ACTION"), "GAME_ACTION"),
                new FeaturedAppsView(context, translator.getString("GAME_ADVENTURE"), "GAME_ADVENTURE"),
                new FeaturedAppsView(context, translator.getString("GAME_PUZZLE"), "GAME_PUZZLE"))
                .zipWith(Observable.interval(16, TimeUnit.MILLISECONDS),
                        (featuredAppsView, interval) -> featuredAppsView)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(featuredAppsView -> bulk_layout.addView(featuredAppsView))
                .subscribe()
        );
    }
}
