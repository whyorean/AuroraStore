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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.activity.DetailsActivity;
import com.aurora.store.model.App;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class InstalledAppsAdapter extends RecyclerView.Adapter<InstalledAppsAdapter.ViewHolder> {

    public List<App> appsToAdd;
    private Context context;

    public InstalledAppsAdapter(Context context, List<App> appsToAdd) {
        this.context = context;
        this.appsToAdd = appsToAdd;
    }

    public void add(int position, App app) {
        appsToAdd.add(position, app);
        notifyItemInserted(position);
    }

    public void add(App app) {
        appsToAdd.add(app);
    }

    public void remove(int position) {
        appsToAdd.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_installed, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        App app = appsToAdd.get(position);
        List<String> Version = new ArrayList<>();
        List<String> Extra = new ArrayList<>();

        holder.AppTitle.setText(app.getDisplayName());
        getDetails(context, Version, Extra, app);
        setText(holder.AppVersion, TextUtils.join(" • ", Version));
        setText(holder.AppExtra, TextUtils.join(" • ", Extra));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailsActivity.class);
            intent.putExtra("INTENT_PACKAGE_NAME", app.getPackageName());
            context.startActivity(intent);
        });

        GlideApp
                .with(context)
                .load(app.getIconInfo().getUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transition(new DrawableTransitionOptions().crossFade())
                .into(holder.AppIcon);
    }

    public void getDetails(Context mContext, List<String> Version, List<String> Extra, App app) {
        Version.add("v" + app.getVersionName() + "." + app.getVersionCode());
        if (app.isSystem())
            Extra.add(mContext.getString(R.string.list_app_system));
        else
            Extra.add(mContext.getString(R.string.list_app_user));
    }

    protected void setText(TextView textView, String text) {
        if (!TextUtils.isEmpty(text)) {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return appsToAdd.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.app_icon)
        ImageView AppIcon;
        @BindView(R.id.app_title)
        TextView AppTitle;
        @BindView(R.id.app_version)
        TextView AppVersion;
        @BindView(R.id.app_extra)
        TextView AppExtra;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

}
