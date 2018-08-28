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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.dragons.aurora.CategoryManager;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.activities.CategoryAppsActivity;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.task.CategoryTaskHelper;
import com.dragons.aurora.task.FeaturedTaskHelper;
import com.dragons.custom.MoreAppsCard;
import com.dragons.custom.TagView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

import static com.dragons.aurora.Util.isConnected;

public class HomeFragment extends BaseFragment {

    @BindView(R.id.top_links)
    LinearLayout topLinks;

    private View view;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.fragment_home, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addTags();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Accountant.isLoggedIn(getContext())) {
            if (isConnected(getContext())) {
                if (topLinks.getVisibility() == View.GONE) {
                    setupTopFeatured();
                    drawCategories();
                }
            }
        }
    }

    private void addTags() {
        setupTag(view, R.id.tag_gamesAction, "GAME_ACTION");
        setupTag(view, R.id.tag_family, "FAMILY");
        setupTag(view, R.id.tag_gamesRacing, "GAME_RACING");
        setupTag(view, R.id.tag_travel, "TRAVEL_AND_LOCAL");
        setupTag(view, R.id.tag_social, "SOCIAL");
    }

    private void setupTag(View v, int viewID, String Category) {
        TagView tagView = v.findViewById(viewID);
        if (tagView.getStyle() == 0)
            tagView.setMono_title(new CategoryManager(getContext()).getCategoryName(Category));
        else {
            if (Category.contains("GAME_"))
                tagView.setDual_title0(getString(R.string.tagview_games));
            else
                tagView.setDual_title0(getString(R.string.tagview_family));
            tagView.setDual_title1(new CategoryManager(getContext()).getCategoryName(Category));
        }
        tagView.setOnClickListener(click -> getActivity()
                .startActivity(CategoryAppsActivity.start(v.getContext(), Category)));
    }

    private void setupTopFeatured() {
        RecyclerView topGrossingGames = view.findViewById(R.id.top_featured_games);
        RecyclerView topGrossingApps = view.findViewById(R.id.top_featured_apps);
        new FeaturedTaskHelper(this, topGrossingGames).getCategoryApps("GAME",
                Util.getSubCategory(getContext()));
        new FeaturedTaskHelper(this, topGrossingApps).getCategoryApps("APPLICATION",
                Util.getSubCategory(getContext()));
    }

    private void drawCategories() {
        GooglePlayAPI.SUBCATEGORY subcategory = Util.getSubCategory(getContext());
        LinearLayout topLinksLayout = view.findViewById(R.id.top_links);
        topLinksLayout.setVisibility(View.VISIBLE);
        topLinksLayout.addView(buildAppsCard("TOOLS", subcategory,
                new CategoryManager(getContext()).getCategoryName("TOOLS")));
        topLinksLayout.addView(buildAppsCard("COMMUNICATION", subcategory,
                new CategoryManager(getContext()).getCategoryName("COMMUNICATION")));
        topLinksLayout.addView(buildAppsCard("MUSIC_AND_AUDIO", subcategory,
                new CategoryManager(getContext()).getCategoryName("MUSIC_AND_AUDIO")));
    }

    private MoreAppsCard buildAppsCard(String categoryId, GooglePlayAPI.SUBCATEGORY subcategory, String label) {
        MoreAppsCard moreAppsCard = new MoreAppsCard(getContext(), categoryId, label);
        RecyclerView recyclerView = moreAppsCard.findViewById(R.id.m_apps_recycler);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        moreAppsCard.setLayoutParams(params);
        moreAppsCard.setGravity(Gravity.CENTER_VERTICAL);
        new CategoryTaskHelper(getActivity(), recyclerView).getCategoryApps(categoryId, subcategory);
        return moreAppsCard;
    }
}
