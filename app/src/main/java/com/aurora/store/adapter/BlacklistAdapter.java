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
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.R;
import com.aurora.store.model.Packages;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;


public class BlacklistAdapter extends SelectableAdapter<BlacklistAdapter.ViewHolder> {

    private List<Packages> packagesList;
    private ItemClickListener itemClickListener;

    public BlacklistAdapter(Context context, List<ResolveInfo> resolveInfos, ItemClickListener itemClickListener) {
        super(context);
        this.itemClickListener = itemClickListener;

        PackageManager mPackageManager = context.getPackageManager();
        Set<String> mBlackListSet = new HashSet<>();
        mBlackListSet.add("com.aurora.store");
        mBlackListSet.add("com.google.android.gms");
        mBlackListSet.add("Services Framework Proxy");
        mBlackListSet.add("com.android.vending");
        mBlackListSet.add("");

        packagesList = new ArrayList<>();
        for (int i = 0; i < resolveInfos.size(); i++) {
            ResolveInfo mResolveInfo = resolveInfos.get(i);
            String mPackageName = mResolveInfo.activityInfo.packageName;
            if (!mBlackListSet.contains(mPackageName)) {
                packagesList.add(new Packages(mResolveInfo, mPackageName, mPackageManager));
            }
        }
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_blacklist, parent, false);
        return new ViewHolder(itemLayoutView, itemClickListener);
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder viewHolder, int position) {
        viewHolder.label.setText(packagesList.get(position).getLabel());
        viewHolder.icon.setImageDrawable(packagesList.get(position).getIcon());
        viewHolder.checkBox.setChecked(isSelected(packagesList.get(position).getPackageName()));
    }

    @Override
    public int getItemCount() {
        return packagesList.size();
    }

    @Override
    public void toggleSelection(int position) {
        String packageName = packagesList.get(position).getPackageName();
        if (mSelections.contains(packageName)) {
            mSelections.remove(packageName);
        } else {
            mSelections.add(packageName);
        }
        notifyItemChanged(position);
    }


    public interface ItemClickListener {
        void onItemClicked(int position);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.label)
        TextView label;
        @BindView(R.id.icon)
        ImageView icon;
        @BindView(R.id.check)
        CheckBox checkBox;

        private ItemClickListener listener;

        ViewHolder(View itemLayoutView, ItemClickListener listener) {
            super(itemLayoutView);
            this.listener = listener;
            ButterKnife.bind(this, itemView);
            itemLayoutView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onItemClicked(getAdapterPosition());
            }
        }
    }
}
