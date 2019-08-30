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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.CardType;
import com.aurora.store.FeatureType;
import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.utility.PackageUtil;
import com.aurora.store.utility.Util;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FeaturedAppsAdapter extends RecyclerView.Adapter<FeaturedAppsAdapter.ViewHolder> {

    private onClickListener listener;
    private CardType cardType;
    private FeatureType featureType;

    private Context context;
    private List<App> appList = new ArrayList<>();

    public FeaturedAppsAdapter(Context context, FeatureType featureType) {
        this.context = context;
        this.featureType = featureType;
        this.cardType = Util.isLegacyCardEnabled(context) ? CardType.LEGACY : CardType.MODERN;
    }

    public List<App> getAppList() {
        return appList;
    }

    public void addData(List<App> appList) {
        this.appList.clear();
        this.appList = appList;
        notifyDataSetChanged();
    }

    public boolean isDataEmpty() {
        return appList.isEmpty();
    }

    @NonNull
    @Override
    public FeaturedAppsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(cardType == CardType.MODERN
                ? R.layout.item_featured
                : R.layout.item_cluster, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position) {
        final App app = appList.get(position);
        viewHolder.txtName.setText(app.getDisplayName());
        if (app.getPageBackgroundImage() != null)
            drawBackground(app, viewHolder);

        viewHolder.txtIndicator.setVisibility(PackageUtil.isInstalled(context, app)
                ? View.VISIBLE
                : View.GONE);

        if (viewHolder.txtSize != null)
            viewHolder.txtSize.setText(Util.humanReadableByteValue(app.getSize(), true));
    }

    private void drawBackground(App app, ViewHolder holder) {
        GlideApp
                .with(context)
                .asBitmap()
                .load(cardType == CardType.MODERN
                        ? app.getPageBackgroundImage().getUrl()
                        : app.getIconInfo().getUrl())
                .placeholder(R.color.colorTransparent)
                .transforms(new CenterCrop(), new RoundedCorners(15))
                .transition(new BitmapTransitionOptions().crossFade())
                .into(holder.imgIcon);
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public void setOnItemClickListener(onClickListener clickListener) {
        this.listener = clickListener;
    }

    public interface onClickListener {
        void onItemClick(int position, View v, FeatureType featureType);

        void onItemLongClick(int position, View v, FeatureType featureType);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        @BindView(R.id.app_icon)
        ImageView imgIcon;
        @BindView(R.id.app_name)
        TextView txtName;
        @Nullable
        @BindView(R.id.app_size)
        TextView txtSize;
        @BindView(R.id.txt_indicator)
        TextView txtIndicator;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            int orientation = Resources.getSystem().getConfiguration().orientation;
            if (cardType == CardType.MODERN) {
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    itemView.getLayoutParams().width = (Resources.getSystem()
                            .getDisplayMetrics().widthPixels) - 64/*Padding & Margins*/;
                    imgIcon.getLayoutParams().height = (Resources.getSystem()
                            .getDisplayMetrics().heightPixels) / 4;
                } else {
                    itemView.getLayoutParams().width = ((Resources.getSystem()
                            .getDisplayMetrics().widthPixels) / 2) - 64 /*Padding & Margins*/;
                    imgIcon.getLayoutParams().height = (Resources.getSystem()
                            .getDisplayMetrics().heightPixels) / 2;
                }
            }
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onItemClick(getAdapterPosition(), view, featureType);
        }

        @Override
        public boolean onLongClick(View view) {
            listener.onItemLongClick(getAdapterPosition(), view, featureType);
            return false;
        }
    }
}