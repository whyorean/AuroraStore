package com.aurora.store.util.diff;

import com.aurora.store.model.items.InstalledItem;
import com.mikepenz.fastadapter.diff.DiffCallback;

import org.jetbrains.annotations.Nullable;

public class InstalledDiffCallback implements DiffCallback<InstalledItem> {

    @Override
    public boolean areContentsTheSame(InstalledItem oldItem, InstalledItem newItem) {
        return oldItem.getApp().getPackageName().equals(newItem.getApp().getPackageName());
    }

    @Override
    public boolean areItemsTheSame(InstalledItem oldItem, InstalledItem newItem) {
        return oldItem.getApp().getPackageName().equals(newItem.getApp().getPackageName());
    }

    @Nullable
    @Override
    public Object getChangePayload(InstalledItem oldItem, int oldPosition, InstalledItem newItem, int newPosition) {
        return null;
    }
}