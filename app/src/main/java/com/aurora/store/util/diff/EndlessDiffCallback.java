package com.aurora.store.util.diff;

import com.aurora.store.model.items.EndlessItem;
import com.mikepenz.fastadapter.diff.DiffCallback;

import org.jetbrains.annotations.Nullable;

public class EndlessDiffCallback implements DiffCallback<EndlessItem> {

    @Override
    public boolean areContentsTheSame(EndlessItem oldItem, EndlessItem newItem) {
        return oldItem.getApp().getPackageName().equals(newItem.getApp().getPackageName());
    }

    @Override
    public boolean areItemsTheSame(EndlessItem oldItem, EndlessItem newItem) {
        return oldItem.getApp().getPackageName().equals(newItem.getApp().getPackageName());
    }

    @Nullable
    @Override
    public Object getChangePayload(EndlessItem oldItem, int oldPosition, EndlessItem newItem, int newPosition) {
        return null;
    }
}