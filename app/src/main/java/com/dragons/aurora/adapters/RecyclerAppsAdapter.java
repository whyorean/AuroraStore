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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.activities.DetailsActivity;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.fragment.HomeFragment;
import com.dragons.aurora.fragment.SearchFragment;
import com.dragons.aurora.fragment.details.ButtonDownload;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.model.App;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import static com.dragons.aurora.fragment.SearchFragment.HISTORY_APP;

public class RecyclerAppsAdapter extends RecyclerView.Adapter<RecyclerAppsAdapter.ViewHolder> {

    private List<App> appsToAdd;
    private Fragment fragment;
    private Context context;

    public RecyclerAppsAdapter(Context context, List<App> appsToAdd) {
        this.context = context;
        this.appsToAdd = appsToAdd;
    }

    public RecyclerAppsAdapter(DetailsFragment fragment, List<App> appsToAdd) {
        this.fragment = fragment;
        this.context = fragment.getContext();
        this.appsToAdd = appsToAdd;
    }

    public RecyclerAppsAdapter(SearchFragment fragment, List<App> appsToAdd) {
        this.fragment = fragment;
        this.context = fragment.getContext();
        this.appsToAdd = appsToAdd;
    }

    public RecyclerAppsAdapter(HomeFragment fragment, List<App> appsToAdd) {
        this.fragment = fragment;
        this.context = fragment.getContext();
        this.appsToAdd = appsToAdd;
    }

    public void clear() {
        if (fragment instanceof SearchFragment)
            Prefs.putListString(context, HISTORY_APP, new ArrayList<>());
        appsToAdd.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerAppsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_recyclers, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAppsAdapter.ViewHolder holder, int position) {
        App app = appsToAdd.get(position);
        boolean isInstalled = Util.isAlreadyInstalled(context, app.getPackageName());

        holder.appName.setText(Util.getSimpleName(app.getDisplayName()));
        holder.appRatingBar.setRating(app.getRating().getAverage() / 5.0f);
        setText(holder.view, holder.appRating, R.string.details_rating, app.getRating().getAverage());

        holder.appContainer.setOnClickListener(v -> {
            if (fragment instanceof DetailsFragment) {
                DetailsFragment detailsFragment = new DetailsFragment();
                Bundle arguments = new Bundle();
                arguments.putString("PackageName", app.getPackageName());
                detailsFragment.setArguments(arguments);
                fragment.getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, detailsFragment)
                        .addToBackStack(app.getPackageName())
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
            } else
                context.startActivity(DetailsActivity.getDetailsIntent(context, app.getPackageName()));
        });

        if (isInstalled)
            holder.get_run.setImageResource(R.drawable.ic_featured_launch);

        holder.get_run.setOnClickListener(v -> {
            if (isInstalled) {
                Intent launchIntent = Util.getLaunchIntent((AuroraActivity) context, app);
                if (launchIntent != null) {
                    context.startActivity(launchIntent);
                }
            } else
                new ButtonDownload(context, fragment.getView(), app).checkAndDownload();
        });

        Glide
                .with(context)
                .load(app.getIconInfo().getUrl())
                .apply(new RequestOptions().placeholder(R.color.transparent))
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
        return appsToAdd.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private View view;
        private RelativeLayout appContainer;
        private TextView appName;
        private TextView appRating;
        private RatingBar appRatingBar;
        private ImageView appIcon;
        private ImageView get_run;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            appContainer = view.findViewById(R.id.app_container);
            appName = view.findViewById(R.id.app_name);
            appRating = view.findViewById(R.id.app_rating);
            appRatingBar = view.findViewById(R.id.app_ratingbar);
            appIcon = view.findViewById(R.id.app_icon);
            get_run = view.findViewById(R.id.get_run);
        }
    }
}
