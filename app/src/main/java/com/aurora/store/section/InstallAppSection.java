package com.aurora.store.section;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.ui.details.DetailsActivity;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;

public class InstallAppSection extends Section {

    protected Context context;
    protected List<App> appList = new ArrayList<>();
    private ClickListener clickListener;

    public InstallAppSection(Context context, ClickListener clickListener) {
        super(SectionParameters.builder()
                .itemResourceId(R.layout.item_installed)
                .loadingResourceId(R.layout.item_loading)
                .emptyResourceId(R.layout.item_empty)
                .build());
        this.context = context;
        this.clickListener = clickListener;
        setState(State.LOADING);
    }

    public void updateList(List<App> appList) {
        this.appList.clear();
        this.appList.addAll(appList);

        //Sort Apps by Names
        Collections.sort(this.appList, (App1, App2) ->
                App1.getDisplayName().compareToIgnoreCase(App2.getDisplayName()));

        if (appList.isEmpty())
            setState(State.EMPTY);
        else
            setState(State.LOADED);
    }

    public int removeApp(String packageName) {
        int i = 0;
        for (Iterator<App> iterator = appList.iterator(); iterator.hasNext(); ) {
            if (iterator.next().getPackageName().equals(packageName)) {
                iterator.remove();
                return i;
            }
            i++;
        }
        return -1;
    }

    public List<App> getList() {
        return appList;
    }

    @Override
    public int getContentItemsTotal() {
        return appList.size();
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        return new ContentHolder(view);
    }

    @Override
    public RecyclerView.ViewHolder getEmptyViewHolder(View view) {
        return new EmptyHolder(view);
    }

    @Override
    public RecyclerView.ViewHolder getLoadingViewHolder(View view) {
        return new LoadingHolder(view);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
        final ContentHolder contentHolder = (ContentHolder) holder;
        final App app = appList.get(position);
        List<String> Version = new ArrayList<>();
        List<String> Extra = new ArrayList<>();

        contentHolder.AppTitle.setText(app.getDisplayName());
        getDetails(Version, Extra, app);
        setText(contentHolder.AppExtra, TextUtils.join(" • ", Extra));
        setText(contentHolder.AppVersion, TextUtils.join(" • ", Version));

        GlideApp
                .with(context)
                .load(app.getIconInfo().getUrl())
                .transition(new DrawableTransitionOptions().crossFade())
                .transforms(new CenterCrop(), new RoundedCorners(30))
                .into(contentHolder.AppIcon);

        contentHolder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailsActivity.class);
            intent.putExtra("INTENT_PACKAGE_NAME", app.getPackageName());
            Bundle bundle = ActivityOptions.makeSceneTransitionAnimation((AppCompatActivity) context,
                    Pair.create(contentHolder.AppIcon, "icon"),
                    Pair.create(contentHolder.AppTitle, "displayName")).toBundle();
            context.startActivity(intent, bundle);
        });
        contentHolder.itemView.setOnLongClickListener(v -> {
            clickListener.onLongClick(app);
            return true;
        });
    }

    public void getDetails(List<String> Version, List<String> Extra, App app) {
        Version.add("v" + app.getVersionName() + "." + app.getVersionCode());
        if (app.isSystem())
            Extra.add(context.getString(R.string.list_app_system));
        else
            Extra.add(context.getString(R.string.list_app_user));
    }

    private void setText(TextView textView, String text) {
        if (!TextUtils.isEmpty(text)) {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    public interface ClickListener {
        void onClick(App app);

        void onLongClick(App app);
    }

    static class ContentHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.app_icon)
        ImageView AppIcon;
        @BindView(R.id.app_title)
        TextView AppTitle;
        @BindView(R.id.app_version)
        TextView AppVersion;
        @BindView(R.id.app_extra)
        TextView AppExtra;

        ContentHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    static class EmptyHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.img)
        ImageView img;
        @BindView(R.id.line1)
        TextView line1;

        EmptyHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            img.setImageDrawable(itemView.getResources().getDrawable(R.drawable.ic_apps));
            line1.setText(itemView.getContext().getString(R.string.list_empty_updates));
        }
    }

    static class LoadingHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.progress_bar)
        ProgressBar progressBar;
        @BindView(R.id.line1)
        TextView line1;

        LoadingHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
