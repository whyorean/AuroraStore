package com.aurora.store.section;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.dragons.aurora.playstoreapiv2.SearchSuggestEntry;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;

public class SearchSuggestionSection extends Section {

    private Context context;
    private List<SearchSuggestEntry> suggestEntryList = new ArrayList<>();
    private ClickListener clickListener;

    public SearchSuggestionSection(Context context, ClickListener clickListener) {
        super(SectionParameters.builder()
                .itemResourceId(R.layout.item_suggestion)
                .build());
        this.context = context;
        this.clickListener = clickListener;
    }

    public void addData(List<SearchSuggestEntry> suggestEntryList) {
        this.suggestEntryList.clear();
        this.suggestEntryList.addAll(suggestEntryList);
    }

    @Override
    public int getContentItemsTotal() {
        return suggestEntryList.size();
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
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
        final ContentHolder contentHolder = (ContentHolder) holder;
        SearchSuggestEntry suggestEntry = suggestEntryList.get(position);
        String title = suggestEntry.getTitle();
        String packageName = suggestEntry.getPackageNameContainer().getPackageName();
        contentHolder.line1.setText(title);
        GlideApp
                .with(context)
                .load(suggestEntry.getImageContainer().getImageUrl())
                .placeholder(R.drawable.ic_round_search)
                .transforms(new CenterCrop(), new RoundedCorners(30))
                .into(contentHolder.img);
        holder.itemView.setOnClickListener(v -> clickListener.onClick(packageName.isEmpty() ? title : packageName));
    }

    public List<SearchSuggestEntry> getList() {
        return suggestEntryList;
    }

    public interface ClickListener {
        void onClick(String query);
    }

    public static class ContentHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.img)
        ImageView img;
        @BindView(R.id.line1)
        TextView line1;

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
}
