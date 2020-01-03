package com.aurora.store.section;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.util.PackageUtil;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;

public class FavouriteAppSection extends Section {

    private Context context;
    private List<App> appList = new ArrayList<>();
    private List<String> selectedPackages = new ArrayList<>();
    private ClickListener clickListener;

    public FavouriteAppSection(Context context, ClickListener clickListener) {
        super(SectionParameters.builder()
                .itemResourceId(R.layout.item_favorite)
                .loadingResourceId(R.layout.item_loading)
                .emptyResourceId(R.layout.item_empty)
                .build());
        this.context = context;
        this.clickListener = clickListener;
        setState(State.LOADING);
    }

    public List<App> getAppList() {
        return appList;
    }

    public void updateList(List<App> appList) {
        this.appList.clear();
        this.appList.addAll(appList);
        if (appList.isEmpty())
            setState(State.EMPTY);
        else
            setState(State.LOADED);
    }

    public void remove(String packageName) {
        selectedPackages.remove(packageName);
    }

    public void add(String packageName) {
        selectedPackages.add(packageName);
    }

    public List<String> getSelections() {
        return selectedPackages;
    }

    public List<App> getSelectedList() {
        List<App> selectedList = new ArrayList<>();
        for (App app : appList) {
            if (selectedPackages.contains(app.getPackageName()))
                selectedList.add(app);
        }
        return selectedList;
    }

    public int getSize() {
        return appList.size();
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

        contentHolder.line1.setText(app.getDisplayName());
        contentHolder.checkBox.setChecked(selectedPackages.contains(app.getPackageName()));

        GlideApp
                .with(context)
                .load(app.getIconUrl())
                .transforms(new CenterCrop(), new RoundedCorners(30))
                .into(contentHolder.img);

        contentHolder.itemView.setOnClickListener(v -> {
            clickListener.onClick(position, app.getPackageName());
        });

        if (PackageUtil.isInstalled(context, app)) {
            contentHolder.line2.setText(context.getText(R.string.list_installed));
            contentHolder.checkBox.setEnabled(false);
            contentHolder.checkBox.setOnCheckedChangeListener(null);
        } else {
            contentHolder.line2.setText(context.getText(R.string.list_not_installd));
        }
    }

    @Override
    public void onBindEmptyViewHolder(RecyclerView.ViewHolder holder) {
        super.onBindEmptyViewHolder(holder);
        final EmptyHolder emptyViewHolder = (EmptyHolder) holder;
        emptyViewHolder.button.setText(context.getString(R.string.action_import));
        emptyViewHolder.button.setOnClickListener(v -> clickListener.onClickError());
    }

    public interface ClickListener {
        void onClick(int position, String packageName);

        void onClickError();
    }

    static class ContentHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.img)
        ImageView img;
        @BindView(R.id.line1)
        TextView line1;
        @BindView(R.id.line2)
        TextView line2;
        @BindView(R.id.check)
        MaterialCheckBox checkBox;

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
        @BindView(R.id.action)
        MaterialButton button;

        EmptyHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            img.setImageDrawable(itemView.getResources().getDrawable(R.drawable.ic_menu_fav));
            line1.setText(itemView.getContext().getString(R.string.list_empty_fav));
            button.setVisibility(View.VISIBLE);
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
