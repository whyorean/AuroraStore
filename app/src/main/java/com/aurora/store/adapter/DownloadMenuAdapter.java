package com.aurora.store.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.R;
import com.aurora.store.download.DownloadManager;
import com.aurora.store.model.MenuEntry;
import com.aurora.store.sheet.DownloadMenuSheet;
import com.aurora.store.utility.ViewUtil;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.Status;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DownloadMenuAdapter extends RecyclerView.Adapter<DownloadMenuAdapter.ViewHolder> {

    private DownloadMenuSheet menuSheet;
    private Context context;
    private List<MenuEntry> menuEntryList;
    private Fetch mFetch;
    private Download mDownload;

    public DownloadMenuAdapter(DownloadMenuSheet menuSheet, Download mDownload) {
        this.menuSheet = menuSheet;
        this.context = menuSheet.getContext();
        this.mDownload = mDownload;
        menuEntryList = ViewUtil.parseMenu(context, R.menu.menu_download_single);
        mFetch = new DownloadManager(context).getFetchInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sheet_menu_iconed, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final MenuEntry menuEntry = menuEntryList.get(position);
        holder.menu_icon.setImageDrawable(menuEntry.getIcon());
        holder.menu_title.setText(menuEntry.getTitle());
        attachMenuAction(menuEntry, holder.itemView);
    }

    private void attachMenuAction(MenuEntry menuEntry, View view) {
        switch (menuEntry.getResId()) {
            case R.id.action_copy:
                view.setOnClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Apk Url", mDownload.getUrl());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, context.getString(R.string.action_copied), Toast.LENGTH_LONG).show();
                    menuSheet.dismissAllowingStateLoss();
                });
                break;
            case R.id.action_pause:
                if (mDownload.getStatus() == Status.PAUSED
                        || mDownload.getStatus() == Status.COMPLETED) {
                    view.setEnabled(false);
                    view.setAlpha(.5f);
                } else
                    view.setOnClickListener(v -> {
                        mFetch.pause(mDownload.getId());
                        menuSheet.dismissAllowingStateLoss();
                    });
                break;
            case R.id.action_resume:
                if (mDownload.getStatus() == Status.DOWNLOADING
                        || mDownload.getStatus() == Status.COMPLETED
                        || mDownload.getStatus() == Status.QUEUED) {
                    view.setEnabled(false);
                    view.setAlpha(.5f);
                } else
                    view.setOnClickListener(v -> {
                        mFetch.resume(mDownload.getId());
                        menuSheet.dismissAllowingStateLoss();
                    });
                break;
            case R.id.action_cancel:
                if (mDownload.getStatus() == Status.COMPLETED) {
                    view.setAlpha(.5f);
                    view.setEnabled(false);
                } else
                    view.setOnClickListener(v -> {
                        mFetch.cancel(mDownload.getId());
                        menuSheet.dismissAllowingStateLoss();
                    });
                break;
            case R.id.action_clear:
                view.setOnClickListener(v -> {
                    mFetch.delete(mDownload.getId());
                    menuSheet.dismissAllowingStateLoss();
                });
                break;
        }
    }

    @Override
    public int getItemCount() {
        return menuEntryList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.menu_icon)
        ImageView menu_icon;
        @BindView(R.id.menu_title)
        TextView menu_title;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}

