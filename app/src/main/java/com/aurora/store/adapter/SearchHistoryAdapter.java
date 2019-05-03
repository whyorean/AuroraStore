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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.fragment.SearchAppsFragment;
import com.aurora.store.fragment.SearchFragment;
import com.aurora.store.utility.PrefUtil;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder> {

    private ArrayList<String> mQueryList;
    private SearchFragment fragment;
    private Context context;

    public SearchHistoryAdapter(SearchFragment fragment, ArrayList<String> mQueryList) {
        this.fragment = fragment;
        this.mQueryList = mQueryList;
        this.context = fragment.getContext();
    }

    public void add(int position, String mHistory) {
        mQueryList.add(mHistory);
        notifyItemInserted(position);
    }

    public void remove(int position) {
        mQueryList.remove(position);
        updatePrefList();
        notifyItemRemoved(position);
    }

    public void clear() {
        mQueryList.clear();
        clearPrefList();
        notifyDataSetChanged();
    }

    public void reload() {
        mQueryList = PrefUtil.getListString(context, Constants.RECENT_HISTORY);
        notifyDataSetChanged();
    }

    private void updatePrefList() {
        PrefUtil.putListString(context, Constants.RECENT_HISTORY, mQueryList);
    }

    private void clearPrefList() {
        PrefUtil.putListString(context, Constants.RECENT_HISTORY, new ArrayList<>());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        String[] mDatedQuery = mQueryList.get(position).split(":");
        holder.query.setText(mDatedQuery[0]);
        holder.date.setText(getQueryDate(mDatedQuery[1]));
        holder.viewForeground.setOnClickListener(v -> {
            final String query = holder.query.getText().toString();
            SearchAppsFragment searchAppsFragment = new SearchAppsFragment();
            Bundle arguments = new Bundle();
            arguments.putString("SearchQuery", query);
            arguments.putString("SearchTitle", getTitleString(query));
            searchAppsFragment.setArguments(arguments);
            fragment.getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.coordinator, searchAppsFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public int getItemCount() {
        return mQueryList.size();
    }

    private String getQueryDate(String queryDate) {
        try {
            final long timeInMilli = Long.parseLong(queryDate);
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
            return simpleDateFormat.format(new Date(timeInMilli));
        } catch (NumberFormatException e) {
            return StringUtils.EMPTY;
        }
    }

    private String getTitleString(String query) {
        return query.startsWith(Constants.PUB_PREFIX)
                ? new StringBuilder().append(context.getString(R.string.apps_by))
                .append(StringUtils.SPACE)
                .append(query.substring(Constants.PUB_PREFIX.length())).toString()
                : new StringBuilder()
                .append(context.getString(R.string.title_search_result))
                .append(StringUtils.SPACE)
                .append(query).toString();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public RelativeLayout viewForeground;
        RelativeLayout viewBackground;
        TextView query;
        TextView date;

        ViewHolder(View view) {
            super(view);
            query = view.findViewById(R.id.query);
            date = view.findViewById(R.id.queryTime);
            viewBackground = view.findViewById(R.id.view_background);
            viewForeground = view.findViewById(R.id.view_foreground);
        }
    }
}
