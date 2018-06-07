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

import com.dragons.aurora.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class BigScreenshotsAdapter extends RecyclerView.Adapter<BigScreenshotsAdapter.ViewHolder> {

    private BigScreenshotsAdapter.Holder bsholder;
    private List<BigScreenshotsAdapter.Holder> ssholder;
    private Context context;

    public BigScreenshotsAdapter(List<BigScreenshotsAdapter.Holder> FeaturedAppsH, Context context) {
        this.ssholder = FeaturedAppsH;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.screenshots_item_big, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        bsholder = this.ssholder.get(position);
        String url = bsholder.url.get(position);
        Picasso.with(context)
                .load(url)
                .placeholder(android.R.color.transparent)
                .into(holder.ss_image);
    }

    @Override
    public int getItemCount() {
        return ssholder.size();
    }

    public static class Holder {
        List<String> url;

        public Holder(List<String> url) {
            this.url = url;
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ss_image;

        ViewHolder(View view) {
            super(view);
            ss_image = view.findViewById(R.id.scrn_itm_b);
        }
    }
}

