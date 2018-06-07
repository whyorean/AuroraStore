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

package com.dragons.custom;

import android.content.Context;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dragons.aurora.R;

import java.util.ArrayList;
import java.util.List;

class MenuNavigationItemsAdapter extends RecyclerView.Adapter<MenuNavigationItemsAdapter.MenuNavItem> {

    private Context context;
    private View.OnClickListener onClickListener;

    private List<MenuEntry> navItems;

    MenuNavigationItemsAdapter(Context context, @MenuRes int menuRes, View.OnClickListener onClickListener) {
        this.context = context;
        this.onClickListener = onClickListener;
        this.navItems = new ArrayList<>();
        populateNavigationItems(menuRes);
    }

    @NonNull
    @Override
    public MenuNavItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_nav_item, parent, false);
        v.getLayoutParams().width = parent.getMeasuredWidth() / navItems.size();
        return new MenuNavItem(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuNavItem holder, int position) {
        MenuEntry item = navItems.get(position);
        holder.label.setText(item.getTitle());
        holder.icon.setImageDrawable(item.getIcon());
        holder.itemView.setTag(item.getResId());
        holder.itemView.setOnClickListener(onClickListener);
    }

    @Override
    public int getItemCount() {
        return navItems.size();
    }

    private void populateNavigationItems(int menuRes) {
        MenuParserHelper.parseMenu(context, menuRes, navItems);
    }

    class MenuNavItem extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView label;

        MenuNavItem(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.nav_item_icon);
            label = (TextView) itemView.findViewById(R.id.nav_item_label);
        }
    }
}
