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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.R;
import com.aurora.store.model.MenuEntry;
import com.aurora.store.utility.ViewUtil;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Status;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DownloadMenuAdapter extends RecyclerView.Adapter<DownloadMenuAdapter.ViewHolder> {

    private Context context;
    private Download download;
    private List<MenuEntry> menuEntryList;
    private MenuClickListener listener;

    public DownloadMenuAdapter(Context context, Download download, MenuClickListener listener) {
        this.context = context;
        this.download = download;
        this.listener = listener;
        menuEntryList = ViewUtil.parseMenu(context, R.menu.menu_download_single);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sheet_menu_iconed, parent, false);
        return new ViewHolder(itemView, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final MenuEntry menuEntry = menuEntryList.get(position);
        holder.menu_icon.setImageDrawable(menuEntry.getIcon());
        holder.menu_title.setText(menuEntry.getTitle());
        attachMenuAction(menuEntry, holder.itemView);
    }

    private void attachMenuAction(MenuEntry menuEntry, View view) {
        switch (menuEntry.getResId()) {
            case R.id.action_pause:
                if (download.getStatus() == Status.PAUSED
                        || download.getStatus() == Status.COMPLETED
                        || download.getStatus() == Status.CANCELLED) {
                    view.setEnabled(false);
                    view.setAlpha(.5f);
                }
                break;
            case R.id.action_resume:
                if (download.getStatus() == Status.DOWNLOADING
                        || download.getStatus() == Status.COMPLETED
                        || download.getStatus() == Status.QUEUED) {
                    view.setEnabled(false);
                    view.setAlpha(.5f);
                }
                break;
            case R.id.action_cancel:
                if (download.getStatus() == Status.COMPLETED
                        || download.getStatus() == Status.CANCELLED) {
                    view.setAlpha(.5f);
                    view.setEnabled(false);
                }
                break;
        }
    }

    @Override
    public int getItemCount() {
        return menuEntryList.size();
    }

    public interface MenuClickListener {
        void onMenuClicked(int position);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.menu_icon)
        ImageView menu_icon;
        @BindView(R.id.menu_title)
        TextView menu_title;

        private MenuClickListener listener;

        ViewHolder(@NonNull View itemView, MenuClickListener listener) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.listener = listener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onMenuClicked(getAdapterPosition());
            }
        }
    }
}

