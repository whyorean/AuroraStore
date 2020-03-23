package com.aurora.store;

import com.aurora.store.model.items.SearchSuggestionItem;
import com.mikepenz.fastadapter.diff.DiffCallback;

import org.jetbrains.annotations.Nullable;

public class SuggestionDiffCallback implements DiffCallback<SearchSuggestionItem> {

    @Override
    public boolean areContentsTheSame(SearchSuggestionItem oldItem, SearchSuggestionItem newItem) {
        return oldItem.getSuggestEntry().getTitle().equals(newItem.getSuggestEntry().getTitle());
    }

    @Override
    public boolean areItemsTheSame(SearchSuggestionItem oldItem, SearchSuggestionItem newItem) {
        return oldItem.getSuggestEntry().getTitle().equals(newItem.getSuggestEntry().getTitle());
    }

    @Nullable
    @Override
    public Object getChangePayload(SearchSuggestionItem oldItem, int oldPosition, SearchSuggestionItem newItem, int newPosition) {
        return null;
    }
}