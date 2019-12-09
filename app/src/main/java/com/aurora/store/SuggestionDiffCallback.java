package com.aurora.store;

import androidx.recyclerview.widget.DiffUtil;

import com.dragons.aurora.playstoreapiv2.SearchSuggestEntry;

import java.util.List;

public class SuggestionDiffCallback extends DiffUtil.Callback {
    private List<SearchSuggestEntry> newList;
    private List<SearchSuggestEntry> oldList;

    public SuggestionDiffCallback(List<SearchSuggestEntry> newList, List<SearchSuggestEntry> oldList) {
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