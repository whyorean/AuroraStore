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
import android.content.Intent;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.activity.DetailsActivity;
import com.aurora.store.model.App;
import com.aurora.store.utility.ViewUtil;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FeaturedAppsAdapter extends RecyclerView.Adapter<FeaturedAppsAdapter.ViewHolder> {

    private Context context;
    private List<App> appList;

    public FeaturedAppsAdapter(Context context, List<App> appList) {
        this.context = context;
        this.appList = appList;
    }

    @NonNull
    @Override
    public FeaturedAppsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_featured, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position) {
        final App app = appList.get(position);
        viewHolder.appName.setText(app.getDisplayName());
        viewHolder.appRatingBar.setRating(app.getRating().getStars(1));
        ViewUtil.setText(viewHolder.itemView, viewHolder.appRating, R.string.details_rating, app.getRating().getAverage());

        if (app.getPageBackgroundImage() != null)
            drawBackground(app, viewHolder);

        viewHolder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailsActivity.class);
            intent.putExtra("INTENT_PACKAGE_NAME", app.getPackageName());
            context.startActivity(intent);
        });
    }

    private void drawBackground(App app, ViewHolder holder) {
        GlideApp
                .with(context)
                .asBitmap()
                .load(app.getPageBackgroundImage().getUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.color.colorTransparent)
                .transforms(new CenterCrop(), new RoundedCorners(15))
                .transition(new BitmapTransitionOptions().crossFade())
                .into(holder.appBackground);
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.app_background)
        ImageView appBackground;
        @BindView(R.id.app_name)
        TextView appName;
        @BindView(R.id.app_rating)
        TextView appRating;
        @BindView(R.id.app_ratingbar)
        RatingBar appRatingBar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            appBackground.getLayoutParams().height = (Resources.getSystem().getDisplayMetrics().heightPixels) / 4;
        }
    }
}