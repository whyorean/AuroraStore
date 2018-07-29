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
import android.content.Intent;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.FullscreenImageActivity;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SmallScreenshotsAdapter extends RecyclerView.Adapter<SmallScreenshotsAdapter.ViewHolder> {

    private List<String> URLs;
    private Context context;

    public SmallScreenshotsAdapter(List<String> URLs, Context context) {
        this.URLs = URLs;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_screenshots_small, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        Glide
                .with(context)
                .load(URLs.get(position))
                .apply(new RequestOptions()
                        .transforms(new CenterCrop(), new RoundedCorners(15))
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                .transition(new DrawableTransitionOptions().crossFade())
                .into(holder.ss_image);

        holder.ss_image.setOnClickListener(v -> {
            Intent intent = new Intent(context, FullscreenImageActivity.class);
            intent.putExtra(FullscreenImageActivity.INTENT_SCREENSHOT_NUMBER, position);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return URLs.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ss_image;

        ViewHolder(View view) {
            super(view);
            ss_image = view.findViewById(R.id.scrn_itm_s);
            ss_image.getLayoutParams().height = (Resources.getSystem().getDisplayMetrics().heightPixels) / 3;
            ss_image.getLayoutParams().width = (Resources.getSystem().getDisplayMetrics().widthPixels) / 3;
        }
    }
}

