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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.dragons.aurora.CategoryManager;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.AccountsActivity;
import com.dragons.aurora.activities.CategoryAppsActivity;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.task.CategoryTaskHelper;
import com.dragons.aurora.task.FeaturedTaskHelper;
import com.dragons.aurora.view.AdaptiveToolbar;
import com.dragons.aurora.view.MoreAppsCard;
import com.dragons.custom.TagView;

import static com.dragons.aurora.Util.isConnected;

public class HomeFragment extends BaseFragment {

    private AdaptiveToolbar adtb;
    private View view;
    private LinearLayout topLinks;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view != null) {
            if ((ViewGroup) view.getParent() != null)
                ((ViewGroup) view.getParent()).removeView(view);
            return view;
        }
        view = inflater.inflate(R.layout.fragment_home, container, false);
        adtb = view.findViewById(R.id.adtb);
        adtb.getAvatar_icon().setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AccountsActivity.class);
            intent.putExtra("account_profile_animate", true);
            startActivity(intent);
        });

        initTags();

        topLinks = view.findViewById(R.id.top_links);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Accountant.isLoggedIn(getContext())) {
            if (isConnected(getContext())) {
                setUser();
                if (topLinks.getVisibility() == View.GONE) {
                    setupTopFeatured();
                    drawCategories();
                }
            }
        } else {
            resetUser();
            Accountant.LoginFirst(getContext());
        }
    }

    protected void setUser() {
        if (Accountant.isGoogle(getContext())) {
            Glide
                    .with(getContext())
                    .load(PreferenceFragment.getString(getActivity(), "GOOGLE_URL"))
                    .apply(new RequestOptions()
                            .placeholder(ContextCompat.getDrawable(getContext(), R.drawable.ic_user_placeholder))
                            .circleCrop())
                    .into(adtb.getAvatar_icon());
        } else {
            (adtb.getAvatar_icon()).setImageDrawable(getResources()
                    .getDrawable(R.drawable.ic_dummy_avatar));
        }
    }

    protected void resetUser() {
        (adtb.getAvatar_icon()).setImageDrawable(getResources()
                .getDrawable(R.drawable.ic_user_placeholder));
    }

    protected void initTags() {
        setupTag(view, R.id.tag_gamesAction, "GAME_ACTION");
        setupTag(view, R.id.tag_family, "FAMILY");
        setupTag(view, R.id.tag_gamesRacing, "GAME_RACING");
        setupTag(view, R.id.tag_travel, "TRAVEL_AND_LOCAL");
        setupTag(view, R.id.tag_social, "SOCIAL");
    }

    protected void setupTag(View v, int viewID, String Category) {
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

    protected void setupTopFeatured() {
        RecyclerView topGrossingGames = view.findViewById(R.id.top_featured_games);
        RecyclerView topGrossingApps = view.findViewById(R.id.top_featured_apps);

        new FeaturedTaskHelper(getContext(), topGrossingGames).getCategoryApps("GAME",
                getSubCategory());
        new FeaturedTaskHelper(getContext(), topGrossingApps).getCategoryApps("APPLICATION",
                getSubCategory());
    }

    protected void drawCategories() {
        GooglePlayAPI.SUBCATEGORY subcategory = getSubCategory();
        LinearLayout topLinksLayout = view.findViewById(R.id.top_links);
        topLinksLayout.setVisibility(View.VISIBLE);
        topLinksLayout.addView(buildAppsCard("TOOLS", subcategory,
                new CategoryManager(getContext()).getCategoryName("TOOLS")));
        topLinksLayout.addView(buildAppsCard("COMMUNICATION", subcategory,
                new CategoryManager(getContext()).getCategoryName("COMMUNICATION")));
        topLinksLayout.addView(buildAppsCard("MUSIC_AND_AUDIO", subcategory,
                new CategoryManager(getContext()).getCategoryName("MUSIC_AND_AUDIO")));
        topLinksLayout.addView(buildAppsCard("PERSONALIZATION", subcategory,
                new CategoryManager(getContext()).getCategoryName("PERSONALIZATION")));
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

    private GooglePlayAPI.SUBCATEGORY getSubCategory() {
        GooglePlayAPI.SUBCATEGORY subcategory = null;
        switch (PreferenceFragment.getString(getContext(), "PREFERENCE_SUBCATEGORY")) {
            case "1":
                subcategory = GooglePlayAPI.SUBCATEGORY.TOP_FREE;
                break;
            case "2":
                subcategory = GooglePlayAPI.SUBCATEGORY.TOP_GROSSING;
                break;
            case "3":
                subcategory = GooglePlayAPI.SUBCATEGORY.MOVERS_SHAKERS;
                break;
            default:
                subcategory = GooglePlayAPI.SUBCATEGORY.TOP_GROSSING;
        }
        return subcategory;
    }
}
