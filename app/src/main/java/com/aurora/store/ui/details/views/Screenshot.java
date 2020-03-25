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

package com.aurora.store.ui.details.views;

import android.content.Intent;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.model.items.ScreenshotItem;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.ui.single.activity.FullscreenImageActivity;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;

import butterknife.BindView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class Screenshot extends AbstractDetails {

    @BindView(R.id.recycler)
    RecyclerView recyclerView;

    public Screenshot(DetailsActivity activity, App app) {
        super(activity, app);
    }

    @Override
    public void draw() {
        if (app.getScreenshotUrls().size() > 0) {
            drawGallery();
        }
    }

    private void drawGallery() {
        FastItemAdapter<ScreenshotItem> fastItemAdapter = new FastItemAdapter<>();

        Observable.fromIterable(app.getScreenshotUrls())
                .subscribeOn(Schedulers.io())
                .map(ScreenshotItem::new)
                .toList()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(fastItemAdapter::add)
                .subscribe();

        fastItemAdapter.setOnClickListener((view, screenshotItemIAdapter, screenshotItem, position) -> {
            Intent intent = new Intent(context, FullscreenImageActivity.class);
            intent.putExtra(FullscreenImageActivity.INTENT_SCREENSHOT_NUMBER, position);
            context.startActivity(intent);
            return false;
        });

        recyclerView.setAdapter(fastItemAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
    }
}