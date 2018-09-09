/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store (a fork of Yalp Store )is free software: you can redistribute it and/or modify
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
 */

package com.dragons.aurora.adapters;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.R;
import com.dragons.aurora.fragment.SearchAppsFragment;
import com.dragons.aurora.fragment.SearchFragment;
import com.dragons.aurora.helpers.Prefs;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import static com.dragons.aurora.fragment.SearchFragment.HISTORY_LIST;

public class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder> {

    private ArrayList<String> mQueryList;
    private SearchFragment fragment;

    public SearchHistoryAdapter(SearchFragment fragment, ArrayList<String> mQueryList) {
        this.fragment = fragment;
        this.mQueryList = mQueryList;
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
        mQueryList = Prefs.getListString(fragment.getContext(), HISTORY_LIST);
        notifyDataSetChanged();
    }

    private void updatePrefList() {
        Prefs.putListString(fragment.getContext(), HISTORY_LIST, mQueryList);
    }

    private void clearPrefList() {
        Prefs.putListString(fragment.getContext(), HISTORY_LIST, new ArrayList<>());
    }

    @NonNull
    @Override
    public SearchHistoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchHistoryAdapter.ViewHolder holder, final int position) {
        String mDatedQuery[] = mQueryList.get(position).split(":");
        holder.query.setText(mDatedQuery[0]);
        holder.date.setText(getDiffString((int) getDiff(mDatedQuery[1])));
        holder.viewForeground.setOnClickListener(v -> {
            final String query = holder.query.getText().toString();
            SearchAppsFragment searchAppsFragment = new SearchAppsFragment();
            Bundle arguments = new Bundle();
            arguments.putString("SearchQuery", query);
            arguments.putString("SearchTitle", getTitleString(query));
            searchAppsFragment.setArguments(arguments);
            fragment.getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, searchAppsFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public int getItemCount() {
        return mQueryList.size();
    }

    private long getDiff(String queryDate) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date curDate = simpleDateFormat.parse(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
            return TimeUnit.DAYS.convert(curDate.getTime() - simpleDateFormat.parse(queryDate).getTime(), TimeUnit.MILLISECONDS);
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private String getDiffString(int diff) {
        if (diff == 0)
            return "Today";
        if (diff == 1)
            return "Yesterday";
        else if (diff > 1)
            return diff + " days before";
        return "";
    }

    private String getTitleString(String query) {
        return query.startsWith(Aurora.PUB_PREFIX)
                ? fragment.getString(R.string.apps_by, query.substring(Aurora.PUB_PREFIX.length()))
                : fragment.getString(R.string.activity_title_search, query)
                ;
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
