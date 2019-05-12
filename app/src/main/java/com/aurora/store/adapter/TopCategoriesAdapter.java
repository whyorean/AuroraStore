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

package com.aurora.store.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.R;
import com.aurora.store.SharedPreferencesTranslator;
import com.aurora.store.fragment.CategoriesFragment;
import com.aurora.store.fragment.CategoryAppsFragment;
import com.aurora.store.fragment.HomeFragment;
import com.aurora.store.utility.Util;
import com.aurora.store.utility.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TopCategoriesAdapter extends RecyclerView.Adapter<TopCategoriesAdapter.ViewHolder> {

    private Context context;
    private Fragment fragment;
    private String[] topCategoryIDs;
    private SharedPreferencesTranslator translator;
    private boolean isTransparent;

    private Integer[] categoriesImg = {
            R.drawable.ic_cat_communication,
            R.drawable.ic_cat_family,
            R.drawable.ic_cat_games,
            R.drawable.ic_cat_music,
            R.drawable.ic_cat_personalization,
            R.drawable.ic_cat_photography,
            R.drawable.ic_cat_shopping,
            R.drawable.ic_cat_social_alt,
    };

    public TopCategoriesAdapter(HomeFragment fragment) {
        this.fragment = fragment;
        this.context = fragment.getContext();
        this.topCategoryIDs = context.getResources().getStringArray(R.array.topCategories);
        this.isTransparent = Util.isTransparentStyle(context);
        this.translator = new SharedPreferencesTranslator(Util.getPrefs(context));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_top, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        @ColorInt int color = ViewUtil.getSolidColors(position);
        holder.itemView.setBackgroundTintList(ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(color, isTransparent ? 60 : 255)));
        holder.imgCat.setImageDrawable(context.getResources().getDrawable(categoriesImg[position]));
        holder.imgCat.setColorFilter(isTransparent ? color : Color.WHITE);
        holder.txtCat.setText(translator.getString(topCategoryIDs[position]));
        holder.txtCat.setTextColor(isTransparent ? color : Color.WHITE);

        if (topCategoryIDs[position].equals(CategoriesFragment.FAMILY)) {
            holder.itemView.setOnClickListener(v -> getSubCategoryFragment(CategoriesFragment.FAMILY));
        } else if (topCategoryIDs[position].equals(CategoriesFragment.GAME)) {
            holder.itemView.setOnClickListener(v -> getSubCategoryFragment(CategoriesFragment.GAME));
        } else
            holder.itemView.setOnClickListener(v -> getCategoryAppsFragment(position));
    }

    private void getSubCategoryFragment(String subCategory) {
        CategoriesFragment categoryAppsFragment = new CategoriesFragment();
        Bundle arguments = new Bundle();
        arguments.putString("CATEGORY_TYPE", subCategory);
        categoryAppsFragment.setArguments(arguments);
        fragment.getChildFragmentManager().beginTransaction()
                .replace(R.id.coordinator, categoryAppsFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

    private void getCategoryAppsFragment(int position) {
        CategoryAppsFragment categoryAppsFragment = new CategoryAppsFragment();
        Bundle arguments = new Bundle();
        arguments.putString("CategoryId", topCategoryIDs[position]);
        arguments.putString("CategoryName", translator.getString(topCategoryIDs[position]));
        categoryAppsFragment.setArguments(arguments);
        FragmentManager manager = fragment.getFragmentManager();
        fragment.getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.coordinator, categoryAppsFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public int getItemCount() {
        return topCategoryIDs.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.cat_icon)
        ImageView imgCat;
        @BindView(R.id.cat_txt)
        TextView txtCat;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
