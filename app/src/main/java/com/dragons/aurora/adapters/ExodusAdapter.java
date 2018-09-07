package com.dragons.aurora.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dragons.aurora.R;
import com.dragons.aurora.model.ExodusTracker;
import com.percolate.caffeine.ViewUtils;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

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
        holder.TrackerCard.setOnClickListener(v ->
                mContext.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(mExodusTracker.URL)))
        );
    }

    @Override
    public int getItemCount() {
        return mExodusTrackers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView TrackerName;
        TextView TrackerSignature;
        TextView TrackerDate;
        RelativeLayout TrackerCard;

        ViewHolder(View v) {
            super(v);
            TrackerName = ViewUtils.findViewById(v, R.id.tracker_name);
            TrackerSignature = ViewUtils.findViewById(v, R.id.tracker_signature);
            TrackerDate = ViewUtils.findViewById(v, R.id.tracker_date);
            TrackerCard = ViewUtils.findViewById(v, R.id.tracker_card);
        }
    }
}
