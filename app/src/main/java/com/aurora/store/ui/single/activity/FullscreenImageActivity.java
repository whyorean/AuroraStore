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

package com.aurora.store.ui.single.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.adapter.BigScreenshotsAdapter;
import com.aurora.store.model.App;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FullscreenImageActivity extends BaseActivity {

    static public final String INTENT_SCREENSHOT_NUMBER = "INTENT_SCREENSHOT_NUMBER";

    @BindView(R.id.gallery)
    RecyclerView recyclerView;

    private App app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setContentView(R.layout.activity_fullscreen_screenshots);
        ButterKnife.bind(this);
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {

            stringExtra = intent.getStringExtra(Constants.STRING_EXTRA);
            intExtra = intent.getIntExtra(INTENT_SCREENSHOT_NUMBER, 0);

            if (stringExtra != null) {
                app = gson.fromJson(stringExtra, App.class);
                setupRecycler();
            }
        } else {
            finishAfterTransition();
        }
    }

    private void setupRecycler() {
        final SnapHelper snapHelper = new PagerSnapHelper();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        snapHelper.attachToRecyclerView(recyclerView);

        recyclerView.setAdapter(new BigScreenshotsAdapter(app.getScreenshotUrls(), this));
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.scrollToPosition(intExtra);
    }
}