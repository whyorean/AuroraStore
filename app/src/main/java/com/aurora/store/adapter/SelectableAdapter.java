/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Aurora Store is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package com.aurora.store.adapter;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.manager.BlacklistManager;

import java.util.ArrayList;

abstract class SelectableAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    protected ArrayList<String> mSelections;
    protected Context context;
    protected BlacklistManager mBlacklistManager;

    SelectableAdapter(Context context) {
        this.context = context;
        mBlacklistManager = new BlacklistManager(context);
        ArrayList<String> blacklistedApps = mBlacklistManager.get();
        mSelections = new ArrayList<>();
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
        mBlacklistManager.addAll(mSelections);
    }

    public void removeSelectionsFromBlackList() {
        mBlacklistManager.removeAll(mSelections);
        mSelections = new ArrayList<>();
    }
}
