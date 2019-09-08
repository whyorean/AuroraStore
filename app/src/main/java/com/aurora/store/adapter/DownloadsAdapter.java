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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.activity.DetailsActivity;
import com.aurora.store.activity.DownloadsActivity;
import com.aurora.store.sheet.DownloadMenuSheet;
import com.aurora.store.utility.PackageUtil;
import com.aurora.store.utility.Util;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Status;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DownloadsAdapter extends RecyclerView.Adapter<DownloadsAdapter.ViewHolder> {

    private final List<DownloadData> downloads = new ArrayList<>();
    private final DownloadMenuSheet menuSheet = new DownloadMenuSheet();
    private Context context;

    public DownloadsAdapter(Context context) {
        this.context = context;
    }

    public void addDownload(@NonNull final Download download) {
        boolean found = false;
        DownloadData data = null;
        int dataPosition = -1;
        for (int i = 0; i < downloads.size(); i++) {
            final DownloadData downloadData = downloads.get(i);
            if (downloadData.id == download.getId()) {
                data = downloadData;
                dataPosition = i;
                found = true;
                break;
            }
        }
        if (!found) {
            final DownloadData downloadData = new DownloadData();
            downloadData.id = download.getId();
            downloadData.download = download;
            downloads.add(downloadData);
            notifyItemInserted(downloads.size() - 1);
        } else {
            data.download = download;
            notifyItemChanged(dataPosition);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_downloads, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        final DownloadData downloadData = downloads.get(position);
        final String packageName = downloadData.download.getTag();
        final String displayName = PackageUtil.getDisplayName(context, packageName);
        final String iconURL = PackageUtil.getIconURL(context, packageName);
        final Status status = downloadData.download.getStatus();

        GlideApp
                .with(context)
                .load(iconURL)
                .into(viewHolder.imgDownload);

        viewHolder.txtTitle.setText(displayName);
        viewHolder.txtStatus.setText(Util.getStatus(status));
        viewHolder.txtPath.setText(downloadData.download.getFile());
        viewHolder.txtSize.setText(new StringBuilder()
                .append(Util.humanReadableByteValue(downloadData.download.getDownloaded(), true))
                .append("/")
                .append(Util.humanReadableByteValue(downloadData.download.getTotal(), true)));

        int progress = downloadData.download.getProgress();
        if (progress == -1) {
            progress = 0;
        }
        viewHolder.txtProgress.setText(new StringBuilder().append(progress).append("%"));
        viewHolder.progressBar.setProgress(progress);

        if (downloadData.eta == -1) {
            viewHolder.txtETA.setText("");
            viewHolder.txtSpeed.setText("");
        } else {
            viewHolder.txtETA.setText(Util.getETAString(context, downloadData.eta));
            viewHolder.txtSpeed.setText("--/s");
        }

        if (downloadData.downloadedBytesPerSecond == 0) {
            viewHolder.txtSpeed.setText("");
        } else {
            viewHolder.txtSpeed.setText(Util.getDownloadSpeedString(context, downloadData.downloadedBytesPerSecond));
        }

        viewHolder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailsActivity.class);
            intent.putExtra("INTENT_PACKAGE_NAME", packageName);
            context.startActivity(intent);
        });

        viewHolder.itemView.setOnLongClickListener(v -> {
            menuSheet.setTitle(displayName);
            menuSheet.setDownload(downloadData.download);
            menuSheet.show(((DownloadsActivity) context).getSupportFragmentManager(), "DOWNLOAD_SHEET");
            return false;
        });

        switch (status) {
            case FAILED:
            case CANCELLED:
            case COMPLETED: {
                viewHolder.txtStatus.setText(Util.getStatus(status));
                viewHolder.txtSpeed.setVisibility(View.INVISIBLE);
                viewHolder.txtETA.setVisibility(View.INVISIBLE);
                break;
            }
            case PAUSED:
            case DOWNLOADING:
            case QUEUED:
            case ADDED: {
                viewHolder.txtStatus.setText(Util.getStatus(status));
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return downloads.size();
    }

    public void update(@NonNull final Download download, long eta, long downloadedBytesPerSecond) {
        for (int position = 0; position < downloads.size(); position++) {
            final DownloadData downloadData = downloads.get(position);
            if (downloadData.id == download.getId()) {
                switch (download.getStatus()) {
                    case REMOVED:
                    case DELETED: {
                        downloads.remove(position);
                        notifyItemRemoved(position);
                        break;
                    }
                    default: {
                        downloadData.download = download;
                        downloadData.eta = eta;
                        downloadData.downloadedBytesPerSecond = downloadedBytesPerSecond;
                        notifyDataSetChanged();
                    }
                }
                return;
            }
        }
    }

    public static class DownloadData {
        public int id;
        @Nullable
        public Download download;
        long eta = -1;
        long downloadedBytesPerSecond = 0;

        @Override
        public int hashCode() {
            return id;
        }

        @NotNull
        @Override
        public String toString() {
            if (download == null) {
                return "";
            }
            return download.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this
                    || obj instanceof DownloadData
                    && ((DownloadData) obj).id == id;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.img_download)
        ImageView imgDownload;
        @BindView(R.id.txt_title)
        TextView txtTitle;
        @BindView(R.id.txt_status)
        TextView txtStatus;
        @BindView(R.id.txt_path)
        TextView txtPath;
        @BindView(R.id.txt_size)
        TextView txtSize;
        @BindView(R.id.txt_progress)
        TextView txtProgress;
        @BindView(R.id.txt_eta)
        TextView txtETA;
        @BindView(R.id.txt_speed)
        TextView txtSpeed;
        @BindView(R.id.progress_download)
        ProgressBar progressBar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
