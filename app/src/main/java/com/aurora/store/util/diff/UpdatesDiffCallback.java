package com.aurora.store.util.diff;

import com.aurora.store.model.items.UpdatesItem;
import com.mikepenz.fastadapter.diff.DiffCallback;

import org.jetbrains.annotations.Nullable;

public class UpdatesDiffCallback implements DiffCallback<UpdatesItem> {

    @Override
    public boolean areContentsTheSame(UpdatesItem oldItem, UpdatesItem newItem) {
        return oldItem.getApp().getPackageName().equals(newItem.getApp().getPackageName());
    }

    @Override
    public boolean areItemsTheSame(UpdatesItem oldItem, UpdatesItem newItem) {
        return oldItem.getApp().getPackageName().equals(newItem.getApp().getPackageName());
    }

    @Nullable
    @Override
    public Object getChangePayload(UpdatesItem oldItem, int oldPosition, UpdatesItem newItem, int newPosition) {
        return null;
    }
}