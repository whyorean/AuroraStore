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

package com.aurora.store.fragment.details;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.fragment.app.FragmentTransaction;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.fragment.DevAppsFragment;
import com.aurora.store.model.App;
import com.aurora.store.utility.Log;
import com.aurora.store.view.ClusterAppsView;

import butterknife.BindView;

public class ClusterDetails extends AbstractHelper {

    @BindView(R.id.apps_by_same_developer)
    ImageView imgDev;
    @BindView(R.id.cluster_links)
    LinearLayout relatedLinksLayout;

    public ClusterDetails(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        /*relatedLinksLayout.setVisibility(View.VISIBLE);
        for (String label : app.getRelatedLinks().keySet()) {
            relatedLinksLayout.addView(new ClusterAppsView(context, app.getRelatedLinks().get(label), label));
        }*/
        addAppsByThisDeveloper();
    }

    private void addAppsByThisDeveloper() {
        imgDev.setVisibility(View.VISIBLE);
        imgDev.setOnClickListener(v -> {
            DevAppsFragment devAppsFragment = new DevAppsFragment();
            Bundle arguments = new Bundle();
            arguments.putString("SearchQuery", Constants.PUB_PREFIX + app.getDeveloperName());
            arguments.putString("SearchTitle", app.getDeveloperName());
            devAppsFragment.setArguments(arguments);
            fragment.getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, devAppsFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(null)
                    .commit();
        });
    }
}
