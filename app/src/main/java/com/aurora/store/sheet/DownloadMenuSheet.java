package com.aurora.store.sheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.R;
import com.aurora.store.adapter.DownloadMenuAdapter;
import com.aurora.store.view.CustomBottomSheetDialogFragment;
import com.tonyodev.fetch2.Download;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DownloadMenuSheet extends CustomBottomSheetDialogFragment {

    @BindView(R.id.download_title)
    TextView downloadTitle;
    @BindView(R.id.download_menu_recycler)
    RecyclerView menuRecyclerView;
    private Download download;
    private String title;

    public DownloadMenuSheet() {
    }

    public Download getDownload() {
        return download;
    }

    public void setDownload(Download download) {
        this.download = download;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_download_menu, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        downloadTitle.setText(getTitle());
        menuRecyclerView.setNestedScrollingEnabled(false);
        menuRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        menuRecyclerView.setAdapter(new DownloadMenuAdapter(this, getDownload()));
    }
}
