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

    private Context mContext;
    private List<ExodusTracker> mExodusTrackers;

    public ExodusAdapter(Context mContext, List<ExodusTracker> mExodusTrackers) {
        this.mContext = mContext;
        this.mExodusTrackers = mExodusTrackers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exodus, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExodusTracker mExodusTracker = mExodusTrackers.get(position);
        holder.TrackerName.setText(mExodusTracker.Name);
        holder.TrackerSignature.setText(mExodusTracker.Signature);
        holder.TrackerDate.setText(mExodusTracker.Date);
        holder.itemView.setOnClickListener(v ->
                mContext.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(mExodusTracker.URL)))
        );
    }

    @Override
    public int getItemCount() {
        return mExodusTrackers.size();
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
