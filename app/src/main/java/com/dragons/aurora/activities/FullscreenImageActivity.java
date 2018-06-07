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

package com.dragons.aurora.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.dragons.aurora.R;
import com.dragons.aurora.adapters.BigScreenshotsAdapter;

import java.util.ArrayList;
import java.util.List;

import static com.dragons.aurora.fragment.DetailsFragment.app;

public class FullscreenImageActivity extends AppCompatActivity {

    static public final String INTENT_SCREENSHOT_NUMBER = "INTENT_SCREENSHOT_NUMBER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setContentView(R.layout.fullscreen_image_activity_layout);

        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (null == app) {
            Log.w(getClass().getSimpleName(), "No app stored");
            finish();
            return;
        }

        List<BigScreenshotsAdapter.Holder> BSAdapter = new ArrayList<>();
        RecyclerView gallery = this.findViewById(R.id.gallery);
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(gallery);
        BigScreenshotsAdapter adapter = new BigScreenshotsAdapter(BSAdapter, this);
        for (int i = 0; i < app.getScreenshotUrls().size(); i++)
            BSAdapter.add(new BigScreenshotsAdapter.Holder(app.getScreenshotUrls()));
        gallery.setAdapter(adapter);
        gallery.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        gallery.scrollToPosition(intent.getIntExtra(INTENT_SCREENSHOT_NUMBER, 0));
        adapter.notifyDataSetChanged();
    }
}