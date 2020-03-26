package com.aurora.store.util.diff;

import com.aurora.store.model.items.DownloadItem;
import com.mikepenz.fastadapter.diff.DiffCallback;

import org.jetbrains.annotations.Nullable;

public class DownloadDiffCallback implements DiffCallback<DownloadItem> {

    @Override
    public boolean areContentsTheSame(DownloadItem oldItem, DownloadItem newItem) {
        return oldItem.equals(newItem);
    }

    @Override
    public boolean areItemsTheSame(DownloadItem oldItem, DownloadItem newItem) {
        return oldItem.getDownload().getProgress() == newItem.getDownload().getProgress();
    }

    @Nullable
    @Override
    public Object getChangePayload(DownloadItem oldItem, int oldPosition, DownloadItem newItem, int newPosition) {
        return null;
    }
}