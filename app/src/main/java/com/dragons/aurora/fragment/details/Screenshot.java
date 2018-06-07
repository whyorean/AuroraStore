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

package com.dragons.aurora.fragment.details;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.dragons.aurora.R;
import com.dragons.aurora.adapters.SmallScreenshotsAdapter;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.model.App;

import java.util.ArrayList;
import java.util.List;

public class Screenshot extends AbstractHelper {

    public Screenshot(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        if (app.getScreenshotUrls().size() > 0) {
            drawGallery();
        }
    }

    private void drawGallery() {
        List<SmallScreenshotsAdapter.Holder> SSAdapter = new ArrayList<>();
        RecyclerView gallery = fragment.getActivity().findViewById(R.id.screenshots_gallery);

        for (int i = 0; i < app.getScreenshotUrls().size(); i++)
            SSAdapter.add(new SmallScreenshotsAdapter.Holder(app.getScreenshotUrls()));

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(fragment.getActivity(), LinearLayoutManager.HORIZONTAL, false);
        gallery.setNestedScrollingEnabled(false);
        gallery.setAdapter(new SmallScreenshotsAdapter(SSAdapter, fragment.getActivity()));
        gallery.setLayoutManager(layoutManager);
    }
}