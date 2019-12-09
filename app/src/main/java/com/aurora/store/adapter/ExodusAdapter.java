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
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.R;
import com.aurora.store.model.ExodusTracker;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ExodusAdapter extends RecyclerView.Adapter<ExodusAdapter.ViewHolder> {

    private Context context;
    private List<ExodusTracker> exodusTrackers;

    public ExodusAdapter(Context context, List<ExodusTracker> exodusTrackers) {
        this.context = context;
        this.exodusTrackers = exodusTrackers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exodus, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExodusTracker mExodusTracker = exodusTrackers.get(position);
        holder.TrackerName.setText(mExodusTracker.Name);
        holder.TrackerSignature.setText(mExodusTracker.Signature);
        holder.TrackerDate.setText(mExodusTracker.Date);
        holder.itemView.setOnClickListener(v -> context.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse(mExodusTracker.URL)))
        );
    }

    @Override
    public int getItemCount() {
        return exodusTrackers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tracker_name)
        TextView TrackerName;
        @BindView(R.id.tracker_signature)
        TextView TrackerSignature;
        @BindView(R.id.tracker_date)
        TextView TrackerDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
