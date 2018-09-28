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
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.DetailsActivity;
import com.dragons.aurora.activities.ManualDownloadActivity;
import com.dragons.aurora.fragment.ManualFragment;
import com.dragons.aurora.fragment.details.ButtonDownload;
import com.dragons.aurora.fragment.details.ButtonUninstall;
import com.dragons.aurora.model.App;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;

public class FeaturedAppsAdapter extends RecyclerView.Adapter<FeaturedAppsAdapter.ViewHolder> {

    private List<App> appsToAdd;
    private Fragment fragment;
    private Context context;

    public FeaturedAppsAdapter(Fragment fragment, List<App> appsToAdd) {
        this.fragment = fragment;
        this.context = fragment.getContext();
        this.appsToAdd = appsToAdd;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_featured, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        App app = appsToAdd.get(position);
        holder.appName.setText(app.getDisplayName());
        holder.appRatingBar.setRating(app.getRating().getStars(1));
        setText(holder.view, holder.appRating, R.string.details_rating, app.getRating().getAverage());

        holder.appCard.setOnClickListener(v -> context
                .startActivity(DetailsActivity.getDetailsIntent(context, app.getPackageName())));

        if (app.getPageBackgroundImage() != null)
            drawBackground(app, holder);

        drawIcon(app, holder);

        holder.appMenu3Dot.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.inflate(R.menu.menu_download);
            popup.getMenu().findItem(R.id.action_download).setVisible(new ButtonDownload(context, fragment.getView(), app).shouldBeVisible());
            popup.getMenu().findItem(R.id.action_uninstall).setVisible(app.isInstalled());
            popup.getMenu().findItem(R.id.action_manual).setVisible(true);
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.action_download:
                        new ButtonDownload(context, fragment.getView(), app).checkAndDownload();
                        break;
                    case R.id.action_uninstall:
                        new ButtonUninstall(context, fragment.getView(), app).uninstall();
                        break;
                    case R.id.action_manual:
                        ManualFragment.app = app;
                        context.startActivity(new Intent(context, ManualDownloadActivity.class));
                        break;
                }
                return false;
            });
            popup.show();
        });
    }

    private void drawBackground(App app, ViewHolder holder) {
        Glide.with(context)
                .asBitmap()
                .load(app.getPageBackgroundImage().getUrl())
                .apply(new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.color.transparent))
                .transition(new BitmapTransitionOptions().crossFade())
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        getPalette(resource, holder);
                        return false;
                    }
                })
                .into(holder.appBackground);
    }

    private void drawIcon(App app, ViewHolder holder) {
        Glide.with(context)
                .load(app.getIconInfo().getUrl())
                .apply(new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.color.transparent))
                .into(holder.appIcon);
    }

    private void getPalette(Bitmap bitmap, ViewHolder holder) {
        Palette.from(bitmap)
                .generate(palette -> paintEmAll(palette, holder));
    }

    private void paintEmAll(Palette palette, ViewHolder holder) {
        Palette.Swatch swatch = palette.getDominantSwatch();
        if (swatch != null) {
            holder.appData.setBackgroundColor(swatch.getRgb());
            holder.appName.setTextColor(swatch.getBodyTextColor());
            holder.appRating.setTextColor(swatch.getTitleTextColor());
            holder.appMenu3Dot.setColorFilter(swatch.getBodyTextColor());
            holder.appRatingBar.setProgressTintList(ColorStateList.valueOf(swatch.getTitleTextColor()));
            holder.appRatingBar.getProgressDrawable().setTint(swatch.getBodyTextColor());
        }
    }

    protected void setText(TextView textView, String text) {
        if (null != textView)
            textView.setText(text);
    }

    protected void setText(View v, TextView textView, int stringId, Object... text) {
        setText(textView, v.getResources().getString(stringId, text));
    }

    @Override
    public int getItemCount() {
        return appsToAdd.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private View view;
        private RelativeLayout appCard;
        private RelativeLayout appData;
        private TextView appName;
        private TextView appRating;
        private RatingBar appRatingBar;
        private ImageView appIcon;
        private ImageView appBackground;
        private ImageView appMenu3Dot;

        ViewHolder(View view) {
            super(view);
            this.view = view;
            appCard = view.findViewById(R.id.app_container);
            appData = view.findViewById(R.id.app_data);
            appName = view.findViewById(R.id.app_name);
            appRating = view.findViewById(R.id.app_rating);
            appRatingBar = view.findViewById(R.id.app_ratingbar);
            appIcon = view.findViewById(R.id.app_icon);
            appBackground = view.findViewById(R.id.app_background);
            appMenu3Dot = view.findViewById(R.id.app_menu3dot);
        }
    }
}