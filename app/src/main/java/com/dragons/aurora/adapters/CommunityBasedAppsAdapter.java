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
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dragons.aurora.R;
import com.dragons.aurora.activities.DetailsActivity;
import com.squareup.picasso.Picasso;

import java.util.List;

public class CommunityBasedAppsAdapter extends RecyclerView.Adapter<CommunityBasedAppsAdapter.MyViewHolderInst> {

    private List<FeaturedHolder> FeaturedAppsH;

    public CommunityBasedAppsAdapter(List<FeaturedHolder> FeaturedAppsH) {
        this.FeaturedAppsH = FeaturedAppsH;
    }

    @NonNull
    @Override
    public MyViewHolderInst onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.community_based_item, parent, false);
        return new MyViewHolderInst(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolderInst holder, int position) {
        Context context = holder.itemView.getContext();
        FeaturedHolder featuredHolder = FeaturedAppsH.get(position);
        holder.cbased_name.setText(featuredHolder.title);
        holder.cbased_layout.setOnClickListener(v -> {
            context.startActivity(DetailsActivity.getDetailsIntent(context, featuredHolder.id));
        });
        Picasso.with(context)
                .load(featuredHolder.icon)
                .placeholder(android.R.color.transparent)
                .into(holder.cbased_image);
    }

    @Override
    public int getItemCount() {
        return FeaturedAppsH.size();
    }

    public static class FeaturedHolder {
        String title;
        String id;
        String developer;
        String icon;
        double rating;
        String price;

        public FeaturedHolder(String title, String id, String developer, String icon,
                              double rating, String price) {
            this.title = title;
            this.id = id;
            this.developer = developer;
            this.icon = icon;
            this.rating = rating;
            this.price = price;
        }

    }

    class MyViewHolderInst extends RecyclerView.ViewHolder {
        TextView cbased_name;
        ImageView cbased_image;
        RelativeLayout cbased_layout;

        MyViewHolderInst(View view) {
            super(view);
            cbased_name = view.findViewById(R.id.cbased_name);
            cbased_image = view.findViewById(R.id.cbased_image);
            cbased_layout = view.findViewById(R.id.cbased_layout);
        }
    }
}

