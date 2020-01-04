package com.aurora.store.section;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.R;
import com.aurora.store.util.Util;
import com.dragons.aurora.playstoreapiv2.FileMetadata;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;

public class FilesSection extends Section {

    private Context context;
    private List<FileMetadata> fileMetadataList = new ArrayList<>();
    private ClickListener clickListener;

    public FilesSection(Context context, ClickListener clickListener) {
        super(SectionParameters.builder()
                .itemResourceId(R.layout.item_three_rows)
                .build());
        this.context = context;
        this.clickListener = clickListener;
    }

    public void addData(List<FileMetadata> fileMetadataList) {
        this.fileMetadataList.clear();
        this.fileMetadataList.addAll(fileMetadataList);
    }

    @Override
    public int getContentItemsTotal() {
        return fileMetadataList.size();
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
        final FileMetadata fileMetadata = fileMetadataList.get(position);
        if (fileMetadata.hasSplitId()) {
            contentHolder.line1.setText(fileMetadata.getSplitId());
            contentHolder.img.setImageDrawable(context.getDrawable(R.drawable.ic_file_patch));
        } else if (fileMetadata.hasFileType()) {
            switch (fileMetadata.getFileType()) {
                case 0:
                    contentHolder.line1.setText("Base");
                    contentHolder.img.setImageDrawable(context.getDrawable(R.drawable.ic_file_apk));
                    break;
                case 1:
                    contentHolder.line1.setText("Obb");
                    contentHolder.img.setImageDrawable(context.getDrawable(R.drawable.ic_file_obb));
                    break;
                default:
                    contentHolder.line1.setText("Patch");
                    contentHolder.img.setImageDrawable(context.getDrawable(R.drawable.ic_file_patch));
            }
        }
        contentHolder.line2.setText(String.valueOf(fileMetadata.getVersionCode()));
        contentHolder.line3.setText(Util.humanReadableByteValue(fileMetadata.getSize(), true));
        contentHolder.itemView.setOnClickListener(v -> clickListener.onClick(position));
    }

    public interface ClickListener {
        void onClick(int position);
    }

    public static class ContentHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.img)
        ImageView img;
        @BindView(R.id.line1)
        TextView line1;
        @BindView(R.id.line2)
        TextView line2;
        @BindView(R.id.line3)
        TextView line3;

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
