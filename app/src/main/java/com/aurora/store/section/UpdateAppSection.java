package com.aurora.store.section;

import android.content.Context;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.util.ViewUtil;
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

public class UpdateAppSection extends Section {

    private Context context;
    private List<App> appList = new ArrayList<>();
    private ClickListener clickListener;

    public UpdateAppSection(Context context, ClickListener clickListener) {
        super(SectionParameters.builder()
                .itemResourceId(R.layout.item_updatable)
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

        contentHolder.txtTitle.setText(app.getDisplayName());
        getDetails(Version, Extra, app);
        setText(contentHolder.txtExtra, TextUtils.join(" • ", Extra));
        setText(contentHolder.txtVersion, TextUtils.join(" • ", Version));
        setText(contentHolder.txtChanges, app.getChanges().isEmpty()
                ? context.getString(R.string.details_no_changes)
                : Html.fromHtml(app.getChanges()).toString());
        GlideApp
                .with(context)
                .load(app.getIconInfo().getUrl())
                .transition(new DrawableTransitionOptions().crossFade())
                .transforms(new CenterCrop(), new RoundedCorners(30))
                .into(contentHolder.imgIcon);

        contentHolder.imgExpand.setOnClickListener(v -> {
            if (contentHolder.layoutChanges.getHeight() == 0) {
                ViewUtil.rotateView(v, false);
                ViewUtil.expandView(contentHolder.layoutChanges,
                        contentHolder.txtChanges.getHeight()
                                + contentHolder.txtChangesTitle.getHeight()
                                + 120 /*Padding & Margins*/);
            } else {
                ViewUtil.rotateView(v, true);
                ViewUtil.collapseView(contentHolder.layoutChanges, 0);
            }
        });

        contentHolder.itemView.setOnClickListener(v -> clickListener.onClick(app));
        contentHolder.itemView.setOnLongClickListener(v -> {
            clickListener.onLongClick(app);
            return true;
        });
    }

    private void getDetails(List<String> Version, List<String> Extra, App app) {
        Version.add(app.getUpdated());
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

    public interface ClickListener {
        void onClick(App app);

        void onLongClick(App app);
    }

    public static class ContentHolder extends RecyclerView.ViewHolder {
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
