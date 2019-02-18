package com.aurora.store.adapter;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.manager.BlacklistManager;

import java.util.HashSet;
import java.util.Set;

abstract class SelectableAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    protected Set<String> mSelections;
    protected Context context;
    private BlacklistManager mBlacklistManager;

    SelectableAdapter(Context context) {
        this.context = context;
        mBlacklistManager = new BlacklistManager(context);
        Set<String> blacklistedApps = mBlacklistManager.getBlacklistedApps();
        mSelections = new HashSet<>();

        if (blacklistedApps != null && !blacklistedApps.isEmpty()) {
            mSelections.addAll(blacklistedApps);
        }
    }

    boolean isSelected(String packageName) {
        return mSelections.contains(packageName);
    }

    void toggleSelection(int position) {
    }

    public void addSelectionsToBlackList() {
        mBlacklistManager.addSelectionsToBlackList(mSelections);
    }

    public void removeSelectionsToBlackList() {
        Set<String> blacklistedApps = mBlacklistManager.getBlacklistedApps();
        if (blacklistedApps != null && !blacklistedApps.isEmpty()) {
            mSelections.removeAll(blacklistedApps);
        }
        mBlacklistManager.removeSelectionsFromBlackList(mSelections);
    }
}
