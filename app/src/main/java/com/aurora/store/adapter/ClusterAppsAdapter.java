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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.activity.DetailsActivity;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.model.App;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ClusterAppsAdapter extends RecyclerView.Adapter<ClusterAppsAdapter.ViewHolder> {

    private List<App> appList = new ArrayList<>();
    private Fragment fragment;
    private Context context;

    public ClusterAppsAdapter(Context context) {
        this.context = context;
    }

    public ClusterAppsAdapter(DetailsFragment fragment) {
        this.fragment = fragment;
        this.context = fragment.getContext();
    }

    public void addData(List<App> appList) {
        this.appList.clear();
        this.appList = appList;
        Collections.sort(appList, (App1, App2) ->
                App1.getDisplayName().compareTo(App2.getDisplayName()));
        notifyDataSetChanged();
    }

    public boolean isDataEmpty() {
        return appList.isEmpty();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_cluster, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        App app = appList.get(position);
        holder.appName.setText(app.getDisplayName());
        holder.appRating.setRating(app.getRating().getAverage());
        holder.itemView.setOnClickListener(v -> {
            if (fragment instanceof DetailsFragment) {
                DetailsFragment detailsFragment = new DetailsFragment();
                Bundle arguments = new Bundle();
                arguments.putString("PackageName", app.getPackageName());
                detailsFragment.setArguments(arguments);
                fragment.getChildFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, detailsFragment)
                        .addToBackStack(app.getPackageName())
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
            } else
                context.startActivity(DetailsActivity.getDetailsIntent(context, app.getPackageName()));
        });
        GlideApp
                .with(context)
                .load(app.getIconInfo().getUrl())
                .apply(new RequestOptions().placeholder(R.color.colorTransparent))
                .into(holder.appIcon);
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
        return appList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.app_icon)
        ImageView appIcon;
        @BindView(R.id.app_name)
        TextView appName;
        @BindView(R.id.app_ratingbar)
        RatingBar appRating;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
