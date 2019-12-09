package com.aurora.store;

import androidx.recyclerview.widget.DiffUtil;

import com.aurora.store.model.App;

import java.util.List;

public class AppDiffCallback extends DiffUtil.Callback {
    private List<App> newList;
    private List<App> oldList;

    public AppDiffCallback(List<App> newList, List<App> oldList) {
        this.newList = newList;
        this.oldList = oldList;
    }

    @Override
    public int getOldListSize() {
        return oldList != null ? oldList.size() : 0;
    }

    @Override
    public int getNewListSize() {
        return newList != null ? newList.size() : 0;
    }

    @Override
    public boolean areItemsTheSame(int oldIndex, int newIndex) {
        return true;
    }

    @Override
    public boolean areContentsTheSame(int oldIndex, int newIndex) {
        return oldList.get(oldIndex).equals(newList.get(newIndex));
    }
}