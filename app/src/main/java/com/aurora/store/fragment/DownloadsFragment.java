package com.aurora.store.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.ErrorType;
import com.aurora.store.R;
import com.aurora.store.adapter.DownloadsAdapter;
import com.aurora.store.download.DownloadManager;
import com.aurora.store.sheet.DownloadMenuSheet;
import com.aurora.store.utility.PackageUtil;
import com.aurora.store.view.ErrorView;
import com.tonyodev.fetch2.AbstractFetchListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Status;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DownloadsFragment extends Fragment implements DownloadsAdapter.onClickListener {

    private static final long UNKNOWN_REMAINING_TIME = -1;
    private static final long UNKNOWN_DOWNLOADED_BYTES_PER_SECOND = 0;

    @BindView(R.id.recyclerDownloads)
    RecyclerView recyclerView;
    @BindView(R.id.view_switcher)
    ViewSwitcher viewSwitcher;
    @BindView(R.id.content_view)
    ViewGroup layoutContent;
    @BindView(R.id.err_view)
    ViewGroup layoutError;

    private Context context;
    private Fetch fetch;
    private DownloadsAdapter adapter;

    private final FetchListener fetchListener = new AbstractFetchListener() {
        @Override
        public void onAdded(@NotNull Download download) {
            adapter.addDownload(download);
        }

        @Override
        public void onQueued(@NotNull Download download, boolean waitingOnNetwork) {
            adapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onCompleted(@NotNull Download download) {
            adapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onError(@NotNull Download download, @NotNull Error error, @org.jetbrains.annotations.Nullable Throwable throwable) {
            super.onError(download, error, throwable);
            adapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onProgress(@NotNull Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {
            adapter.update(download, etaInMilliseconds, downloadedBytesPerSecond);
        }

        @Override
        public void onPaused(@NotNull Download download) {
            adapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onResumed(@NotNull Download download) {
            adapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onCancelled(@NotNull Download download) {
            adapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onRemoved(@NotNull Download download) {
            adapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onDeleted(@NotNull Download download) {
            adapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_downloads, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fetch = DownloadManager.getFetchInstance(context);
        adapter = new DownloadsAdapter(context);
        adapter.setOnItemClickListener(this);
        setupRecycler();
    }

    @Override
    public void onResume() {
        super.onResume();
        fetch.getDownloads(downloads -> {
            final ArrayList<Download> list = new ArrayList<>(downloads);
            Collections.sort(list, (first, second) -> Long.compare(first.getCreated(), second.getCreated()));
            if (list.isEmpty()) {
                setErrorView(ErrorType.NO_DOWNLOADS);
                switchViews(true);
                return;
            }
            for (Download download : list) {
                adapter.addDownload(download);
            }
        }).addListener(fetchListener);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        fetch.removeListener(fetchListener);
        super.onPause();
    }

    protected void setErrorView(ErrorType errorType) {
        layoutError.removeAllViews();
        layoutError.addView(new ErrorView(context, errorType, null));
    }

    protected void switchViews(boolean showError) {
        if (viewSwitcher.getCurrentView() == layoutContent && showError)
            viewSwitcher.showNext();
        else if (viewSwitcher.getCurrentView() == layoutError && !showError)
            viewSwitcher.showPrevious();
    }

    private void cancelAll() {
        fetch.cancelAll();
    }

    private void clearCompleted() {
        fetch.removeAllWithStatus(Status.COMPLETED);
    }

    private void forceClearAll() {
        fetch.deleteAllWithStatus(Status.ADDED);
        fetch.deleteAllWithStatus(Status.QUEUED);
        fetch.deleteAllWithStatus(Status.CANCELLED);
        fetch.deleteAllWithStatus(Status.COMPLETED);
        fetch.deleteAllWithStatus(Status.DOWNLOADING);
        fetch.deleteAllWithStatus(Status.FAILED);
        fetch.deleteAllWithStatus(Status.PAUSED);
    }

    private void setupRecycler() {
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        DividerItemDecoration itemDecorator = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecorator);
        recyclerView.setItemViewCacheSize(25);
    }

    @Override
    public void onItemClick(int position, View v) {
        Bundle bundle = new Bundle();
        bundle.putString("PACKAGE_NAME", adapter.getDownloads().get(position).download.getTag());
        NavHostFragment.findNavController(this).navigate(R.id.downloadlist_to_details, bundle);
    }

    @Override
    public void onItemLongClick(int position, View v) {
        DownloadMenuSheet downloadMenuSheet = new DownloadMenuSheet();
        Download download = adapter.getDownloads().get(position).download;
        downloadMenuSheet.setTitle(PackageUtil.getDisplayName(context, download.getTag()));
        downloadMenuSheet.setDownload(download);
        downloadMenuSheet.show(getChildFragmentManager(), "APP_MENU_SHEET");
    }
}
