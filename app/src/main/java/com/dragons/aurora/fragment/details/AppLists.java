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

import android.app.SearchManager;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.SearchActivity;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.model.App;
import com.dragons.aurora.task.ClusterTaskHelper;
import com.dragons.custom.ClusterAppsCard;
import com.percolate.caffeine.ViewUtils;

import androidx.recyclerview.widget.RecyclerView;

public class AppLists extends AbstractHelper {

    public AppLists(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        LinearLayout relatedLinksLayout = view.findViewById(R.id.cluster_links);
        relatedLinksLayout.setVisibility(View.VISIBLE);
        for (String label : app.getRelatedLinks().keySet()) {
            relatedLinksLayout.addView(buildClusterAppsCard(app.getRelatedLinks().get(label), label));
        }
        addAppsByThisDeveloper();
    }

    private ClusterAppsCard buildClusterAppsCard(String URL, String label) {
        ClusterAppsCard clusterAppsCard = new ClusterAppsCard(fragment.getActivity(), label);
        RecyclerView recyclerView = clusterAppsCard.findViewById(R.id.m_apps_recycler);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        clusterAppsCard.setLayoutParams(params);
        clusterAppsCard.setGravity(Gravity.CENTER_VERTICAL);

        new ClusterTaskHelper(fragment, recyclerView).getClusterApps(URL);
        return clusterAppsCard;
    }

    private void addAppsByThisDeveloper() {
        ViewUtils.findViewById(view, R.id.apps_by_same_developer).setVisibility(View.VISIBLE);
        ImageView imageView = view.findViewById(R.id.apps_by_same_developer);
        imageView.setVisibility(View.VISIBLE);
        imageView.setOnClickListener(v -> {
            Intent intent = new Intent(fragment.getActivity(), SearchActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Intent.ACTION_SEARCH);
            intent.putExtra(SearchManager.QUERY, Aurora.PUB_PREFIX + app.getDeveloperName());
            context.startActivity(intent);
        });
    }
}
