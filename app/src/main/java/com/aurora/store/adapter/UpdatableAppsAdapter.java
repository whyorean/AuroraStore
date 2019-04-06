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
import android.text.Html;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.GlideApp;
import com.aurora.store.ListType;
import com.aurora.store.R;
import com.aurora.store.activity.AuroraActivity;
import com.aurora.store.activity.DetailsActivity;
import com.aurora.store.model.App;
import com.aurora.store.sheet.AppMenuSheet;
import com.aurora.store.utility.ViewUtil;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class UpdatableAppsAdapter extends RecyclerView.Adapter<UpdatableAppsAdapter.ViewHolder> {

    private List<App> appList;
    private Context context;
    private ListType listType;

    public UpdatableAppsAdapter(Context context, List<App> appsToAdd, ListType listType) {
        this.context = context;
        this.appList = appsToAdd;
        this.listType = listType;
        Collections.sort(appsToAdd, (App1, App2) ->
                App1.getDisplayName().compareTo(App2.getDisplayName()));
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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_updatable, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        App app = appList.get(position);
        List<String> Version = new ArrayList<>();
        List<String> Extra = new ArrayList<>();

        viewHolder.txtTitle.setText(app.getDisplayName());
        getDetails(Version, Extra, app);
        setText(viewHolder.txtVersion, TextUtils.join(" • ", Version));
        setText(viewHolder.txtExtra, TextUtils.join(" • ", Extra));
        setText(viewHolder.txtChanges, app.getChanges().isEmpty()
                ? context.getString(R.string.details_no_changes)
                : Html.fromHtml(app.getChanges()).toString());

        viewHolder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailsActivity.class);
            intent.putExtra("INTENT_PACKAGE_NAME", app.getPackageName());
            context.startActivity(intent);
        });

        viewHolder.itemView.setOnLongClickListener(v -> {
            final AppMenuSheet menuSheet = new AppMenuSheet();
            menuSheet.setApp(app);
            menuSheet.setListType(listType);
            menuSheet.show(((AuroraActivity) context).getSupportFragmentManager(),
                    "BOTTOM_MENU_SHEET");
            return false;
        });

        GlideApp
                .with(context)
                .load(app.getIconInfo().getUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transition(new DrawableTransitionOptions().crossFade())
                .into(viewHolder.imgIcon);

        viewHolder.imgExpand.setOnClickListener(v -> {
            if (viewHolder.layoutChanges.getHeight() == 0) {
                ViewUtil.rotateView(v, false);
                ViewUtil.expandView(viewHolder.layoutChanges,
                        viewHolder.txtChanges.getHeight()
                                + viewHolder.txtChangesTitle.getHeight()
                                + 120 /*Padding & Margins*/);
            } else {
                ViewUtil.rotateView(v, true);
                ViewUtil.collapseView(viewHolder.layoutChanges, 0);
            }
        });
    }

    private void getDetails(List<String> Version, List<String> Extra, App app) {
        Version.add("v" + app.getVersionName() + "." + app.getVersionCode());
        Extra.add(app.getUpdated());
        Extra.add(app.getSize() == 0 ? "N/A" : Formatter.formatShortFileSize(context, app.getSize()));
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

    public class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.layout_info)
        RelativeLayout layoutInfo;
        @BindView(R.id.layout_changes)
        RelativeLayout layoutChanges;
        @BindView(R.id.app_icon)
        ImageView imgIcon;
        @BindView(R.id.app_title)
        TextView txtTitle;
        @BindView(R.id.app_version)
        TextView txtVersion;
        @BindView(R.id.app_extra)
        TextView txtExtra;
        @BindView(R.id.img_expand)
        ImageView imgExpand;
        @BindView(R.id.txt_title)
        TextView txtChangesTitle;
        @BindView(R.id.txt_changes)
        TextView txtChanges;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

}
