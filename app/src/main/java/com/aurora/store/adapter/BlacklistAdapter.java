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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.model.App;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class BlacklistAdapter extends SelectableAdapter<BlacklistAdapter.ViewHolder> {

    private List<App> appList;
    private ItemClickListener itemClickListener;

    public BlacklistAdapter(Context context, List<App> appList, ItemClickListener itemClickListener) {
        super(context);
        this.itemClickListener = itemClickListener;
        this.appList = appList;
        Collections.sort(appList, (App1, App2) ->
                App1.getDisplayName().compareTo(App2.getDisplayName()));
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_blacklist, parent, false);
        return new ViewHolder(itemLayoutView, itemClickListener);
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder viewHolder, int position) {
        final App app = appList.get(position);
        viewHolder.label.setText(app.getDisplayName());
        viewHolder.packageName.setText(app.getPackageName());
        viewHolder.checkBox.setChecked(isSelected(app.getPackageName()));
        GlideApp
                .with(context)
                .load(app.getIconInfo().getUrl())
                .into(viewHolder.icon);
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    @Override
    public void toggleSelection(int position) {
        String packageName = appList.get(position).getPackageName();
        if (mSelections.contains(packageName)) {
            mSelections.remove(packageName);
            mBlacklistManager.remove(packageName);
        } else {
            mSelections.add(packageName);
        }
        notifyItemChanged(position);
    }

    public int getSelectedCount() {
        return mSelections.size();
    }

    public interface ItemClickListener {
        void onItemClicked(int position);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.label)
        TextView label;
        @BindView(R.id.packageName)
        TextView packageName;
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
