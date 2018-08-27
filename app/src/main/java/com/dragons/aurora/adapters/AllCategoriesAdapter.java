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

package com.dragons.aurora.adapters;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dragons.aurora.R;
import com.dragons.aurora.SharedPreferencesTranslator;
import com.dragons.aurora.fragment.CategoryAppsFragment;
import com.dragons.aurora.fragment.CategoryListFragment;
import com.percolate.caffeine.ViewUtils;

import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

public class AllCategoriesAdapter extends RecyclerView.Adapter<AllCategoriesAdapter.ViewHolder> {

    private CategoryListFragment fragment;
    private Context context;
    private Map<String, String> categories;

    private Integer[] categoriesImg = {
            R.drawable.ic_android_wear,
            R.drawable.ic_art_design,
            R.drawable.ic_auto_vehicles,
            R.drawable.ic_beauty,
            R.drawable.ic_books_reference,
            R.drawable.ic_business,
            R.drawable.ic_comics,
            R.drawable.ic_communication,
            R.drawable.ic_dating,
            R.drawable.ic_education,
            R.drawable.ic_entertainment,
            R.drawable.ic_events,
            R.drawable.ic_family,
            R.drawable.ic_finance,
            R.drawable.ic_food_drink,
            R.drawable.ic_games,
            R.drawable.ic_health_fitness,
            R.drawable.ic_house_home,
            R.drawable.ic_libraries_demo,
            R.drawable.ic_lifestyle,
            R.drawable.ic_maps_navigation,
            R.drawable.ic_medical,
            R.drawable.ic_music__audio,
            R.drawable.ic_news_magazines,
            R.drawable.ic_parenting,
            R.drawable.ic_personalization,
            R.drawable.ic_photography,
            R.drawable.ic_productivity,
            R.drawable.ic_shopping,
            R.drawable.ic_social,
            R.drawable.ic_sports,
            R.drawable.ic_tools,
            R.drawable.ic_travel_local,
            R.drawable.ic_video_editors,
            R.drawable.ic_weather,
    };

    private SharedPreferencesTranslator translator;

    public AllCategoriesAdapter(CategoryListFragment fragment, Map<String, String> categories) {
        this.fragment = fragment;
        this.categories = categories;
        this.context = fragment.getContext();
        translator = new SharedPreferencesTranslator(PreferenceManager.getDefaultSharedPreferences(context));
    }

    @NonNull
    @Override
    public AllCategoriesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        holder.topLabel.setText(translator.getString(new ArrayList<>(categories.keySet()).get(position)));
        holder.topImage.setImageDrawable(context.getResources().getDrawable(categoriesImg[position]));
        holder.topContainer.setOnClickListener(v -> {
            CategoryAppsFragment categoryAppsFragment = new CategoryAppsFragment();
            Bundle arguments = new Bundle();
            arguments.putString("CategoryId", new ArrayList<>(categories.keySet()).get(position));
            arguments.putString("CategoryName", translator.getString(new ArrayList<>(categories.keySet()).get(position)));
            categoryAppsFragment.setArguments(arguments);
            fragment.getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, categoryAppsFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView topLabel;
        ImageView topImage;
        CardView topContainer;

        ViewHolder(View v) {
            super(v);
            topLabel = ViewUtils.findViewById(v, R.id.all_cat_name);
            topImage = ViewUtils.findViewById(v, R.id.all_cat_img);
            topContainer = ViewUtils.findViewById(v, R.id.all_cat_container);
        }
    }
}
