package com.aurora.store.model.items;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.aurora.store.Constants;
import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.util.Util;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Status;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lombok.Data;

@Data
public class DownloadItem extends AbstractItem<DownloadItem.ViewHolder> {

    private Download download;
    private String packageName;

    public DownloadItem(Download download) {
        this.download = download;
        this.packageName = download.getTag();
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_downloads;
    }

    @Override
    public long getIdentifier() {
        return download.getId();
    }

    @NotNull
    @Override
    public ViewHolder getViewHolder(@NotNull View view) {
        return new ViewHolder(view);
    }

    @Override
    public int getType() {
        return R.id.fastadapter_item;
    }

    public static class ViewHolder extends FastAdapter.ViewHolder<DownloadItem> {
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

        private Context context;

        ViewHolder(@NonNull View view) {
            super(view);
            ButterKnife.bind(this, view);
            this.context = view.getContext();
        }

        @Override
        public void bindView(@NotNull DownloadItem item, @NotNull List<?> list) {

            final Download download = item.getDownload();
            final String displayName = download.getHeaders().get(Constants.DOWNLOAD_DISPLAY_NAME);
            final String iconURL = download.getHeaders().get(Constants.DOWNLOAD_ICON_URL);
            final Status status = download.getStatus();

            GlideApp
                    .with(context)
                    .load(iconURL)
                    .transforms(new CenterCrop(), new RoundedCorners(30))
                    .into(imgDownload);

            txtTitle.setText(displayName);
            txtStatus.setText(Util.getStatus(status));
            txtPath.setText(download.getFile());
            txtSize.setText(new StringBuilder()
                    .append(Util.humanReadableByteValue(download.getDownloaded(), true))
                    .append("/")
                    .append(Util.humanReadableByteValue(download.getTotal(), true)));

            int progress = download.getProgress();
            if (progress == -1) {
                progress = 0;
            }

            txtProgress.setText(new StringBuilder().append(progress).append("%"));
            progressBar.setProgress(progress);

            if (item.getDownload().getEtaInMilliSeconds() == -1) {
                txtETA.setText("");
                txtSpeed.setText("");
            } else {
                txtETA.setText(Util.getETAString(context, item.getDownload().getEtaInMilliSeconds()));
                txtSpeed.setText("--/s");
            }

            if (item.getDownload().getDownloadedBytesPerSecond() == 0) {
                txtSpeed.setText("");
            } else {
                txtSpeed.setText(Util.getDownloadSpeedString(context, item.getDownload().getDownloadedBytesPerSecond()));
            }

            switch (status) {
                case FAILED:
                case CANCELLED:
                case COMPLETED: {
                    txtStatus.setText(Util.getStatus(status));
                    txtSpeed.setVisibility(View.INVISIBLE);
                    txtETA.setVisibility(View.INVISIBLE);
                    break;
                }
                case PAUSED:
                case DOWNLOADING:
                case QUEUED:
                case ADDED: {
                    txtStatus.setText(Util.getStatus(status));
                    break;
                }
                default: {
                    break;
                }
            }
        }

        @Override
        public void unbindView(@NotNull DownloadItem item) {
            txtTitle.setText(null);
            txtStatus.setText(null);
            txtPath.setText(null);
            txtSize.setText(null);
            txtProgress.setText(null);
            txtETA.setText(null);
            txtSpeed.setText(null);
        }
    }
}
