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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.GlideApp;
import com.aurora.store.ListType;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class InstalledAppsAdapter extends RecyclerView.Adapter<InstalledAppsAdapter.ViewHolder> {

    public Context context;
    private onClickListener listener;
    private List<App> appList = new ArrayList<>();
    private ListType listType;

    public InstalledAppsAdapter(Context context, ListType listType) {
        this.context = context;
        this.listType = listType;
    }

    public List<App> getAppList() {
        return appList;
    }

    public void add(int position, App app) {
        appList.add(position, app);
        notifyItemInserted(position);
    }

    public void add(App app) {
        appList.add(app);
    }

    public void remove(int position) {
        appList.remove(position);
        notifyItemRemoved(position);
    }

    public void remove(String packageName) {
        for (App app : appList) {
            if (app.getPackageName().equals(packageName)) {
                appList.remove(app);
                notifyDataSetChanged();
                break;
            }
        }
    }

    public void remove(App app) {
        appList.remove(app);
        notifyDataSetChanged();
    }

    public void addData(List<App> appList) {
        this.appList.clear();
        this.appList = appList;
        final Set<App> appSet = new LinkedHashSet<>(appList);
        appList.clear();
        appList.addAll(appSet);
        if (listType == ListType.INSTALLED || listType == ListType.UPDATES)
            Collections.sort(appList, (App1, App2) ->
                    App1.getDisplayName().compareToIgnoreCase(App2.getDisplayName()));
        notifyDataSetChanged();
    }

    public boolean isDataEmpty() {
        return appList.isEmpty();
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
        final App app = appList.get(position);
        List<String> Version = new ArrayList<>();
        List<String> Extra = new ArrayList<>();

        holder.AppTitle.setText(app.getDisplayName());
        getDetails(Version, Extra, app);
        setText(holder.AppExtra, TextUtils.join(" • ", Extra));
        setText(holder.AppVersion, TextUtils.join(" • ", Version));

        GlideApp
                .with(context)
                .load(app.getIconInfo().getUrl())
                .transition(new DrawableTransitionOptions().crossFade())
                .into(holder.AppIcon);
    }

    public void getDetails(List<String> Version, List<String> Extra, App app) {
        Version.add("v" + app.getVersionName() + "." + app.getVersionCode());
        if (app.isSystem())
            Extra.add(context.getString(R.string.list_app_system));
        else
            Extra.add(context.getString(R.string.list_app_user));
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
        return appList.size();
    }

    public void setOnItemClickListener(onClickListener clickListener) {
        this.listener = clickListener;
    }

    public interface onClickListener {
        void onItemClick(int position, View v);

        void onItemLongClick(int position, View v);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
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
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onItemClick(getAdapterPosition(), view);
        }

        @Override
        public boolean onLongClick(View view) {
            listener.onItemLongClick(getAdapterPosition(), view);
            return false;
        }
    }
}
