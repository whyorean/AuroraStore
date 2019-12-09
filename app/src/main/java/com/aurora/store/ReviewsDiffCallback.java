package com.aurora.store;

import androidx.recyclerview.widget.DiffUtil;

import com.aurora.store.model.Review;

import java.util.List;

public class ReviewsDiffCallback extends DiffUtil.Callback {
    private List<Review> newList;
    private List<Review> oldList;

    public ReviewsDiffCallback(List<Review> newList, List<Review> oldList) {
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