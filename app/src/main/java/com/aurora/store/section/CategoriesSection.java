package com.aurora.store.section;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.model.CategoryModel;
import com.aurora.store.util.ImageUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;

public class CategoriesSection extends Section {

    private Context context;
    private List<CategoryModel> categories;
    private String header;
    private ClickListener clickListener;

    public CategoriesSection(Context context, List<CategoryModel> categories, String header, ClickListener clickListener) {
        super(SectionParameters.builder()
                .itemResourceId(R.layout.item_category_list)
                .headerResourceId(R.layout.item_header)
                .loadingResourceId(R.layout.item_loading)
                .emptyResourceId(R.layout.item_empty)
                .build());
        this.context = context;
        this.clickListener = clickListener;
        this.categories = categories;
        this.header = header;

        if (categories.isEmpty())
            setState(State.LOADING);
        else
            setState(State.LOADED);
    }

    @Override
    public int getContentItemsTotal() {
        return categories.size();
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        return new ContentHolder(view);
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
        return new HeaderHolder(view);
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
        CategoryModel categoryModel = categories.get(position);
        contentHolder.topLabel.setText(categoryModel.getCategoryTitle());
        contentHolder.itemView.setOnClickListener(v -> clickListener.onClick(categoryModel.getCategoryId(), categoryModel.getCategoryTitle()));
        contentHolder.topImage.setBackground(ImageUtil.getDrawable(position, GradientDrawable.OVAL));

        GlideApp
                .with(context)
                .load(categoryModel.getCategoryImageUrl())
                .circleCrop()
                .into(contentHolder.topImage);
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder) {
        final HeaderHolder headerHolder = (HeaderHolder) holder;
        headerHolder.line1.setText(header);
    }

    public interface ClickListener {
        void onClick(String categoryId, String categoryName);
    }

    public static class ContentHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.all_cat_img)
        ImageView topImage;
        @BindView(R.id.all_cat_name)
        TextView topLabel;

        ContentHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.line1)
        TextView line1;

        HeaderHolder(@NonNull View itemView) {
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
