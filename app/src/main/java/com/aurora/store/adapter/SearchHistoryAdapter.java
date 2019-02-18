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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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
        return query.startsWith(Constants.PUB_PREFIX)
                ? fragment.getString(R.string.apps_by, query.substring(Constants.PUB_PREFIX.length()))
                : fragment.getString(R.string.title_search_result, query)
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
