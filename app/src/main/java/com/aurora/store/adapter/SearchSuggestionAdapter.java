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

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.dragons.aurora.playstoreapiv2.SearchSuggestEntry;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SearchSuggestionAdapter extends RecyclerView.Adapter<SearchSuggestionAdapter.ViewHolder> {

    public List<SearchSuggestEntry> appList;
    public Context context;
    private ClickListener clickListener;

    public SearchSuggestionAdapter(Context context, ClickListener clickListener) {
        this.context = context;
        this.clickListener = clickListener;
        this.appList = new ArrayList<>();
    }

    public void addData(List<SearchSuggestEntry> appList) {
        this.appList.clear();
        this.appList.addAll(appList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchSuggestEntry suggestEntry = appList.get(position);
        String title = suggestEntry.getTitle();
        String packageName = suggestEntry.getPackageNameContainer().getPackageName();
        holder.line1.setText(title);
        GlideApp
                .with(context)
                .load(suggestEntry.getImageContainer().getImageUrl())
                .placeholder(R.drawable.ic_round_search)
                .transforms(new CenterCrop(), new RoundedCorners(30))
                .into(holder.img);
        holder.itemView.setOnClickListener(v -> clickListener.onClickedSuggestion(packageName.isEmpty() ? title : packageName));
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public interface ClickListener {
        void onClickedSuggestion(String query);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.img)
        ImageView img;
        @BindView(R.id.line1)
        TextView line1;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
