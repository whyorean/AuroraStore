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

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SelectableViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    @BindView(R.id.view_foreground)
    public RelativeLayout viewForeground;
    @BindView(R.id.view_background)
    RelativeLayout viewBackground;
    @BindView(R.id.app_icon)
    ImageView AppIcon;
    @BindView(R.id.app_title)
    TextView AppTitle;
    @BindView(R.id.app_extra)
    TextView AppExtra;
    @BindView(R.id.app_checkbox)
    CheckBox AppCheckbox;

    private ItemClickListener listener;

    SelectableViewHolder(View view, ItemClickListener listener) {
        super(view);
        ButterKnife.bind(this, view);
        this.listener = listener;
        view.setOnClickListener(this);
    }

    public void setChecked(boolean value) {
        AppCheckbox.setChecked(value);
    }

    @Override
    public void onClick(View v) {
        if (listener != null) {
            listener.onItemClicked(getAdapterPosition());
        }
    }

    public interface ItemClickListener {
        void onItemClicked(int position);
    }
}
