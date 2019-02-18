package com.aurora.store.download;

import android.content.Context;

import com.aurora.store.utility.Util;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2okhttp.OkHttpDownloader;

import okhttp3.OkHttpClient;

public class DownloadManager {

    private Context context;
    private Fetch fetch;

    public DownloadManager(Context context) {
        this.context = context;
        init();
    }

    private void init() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(context)
                .setDownloadConcurrentLimit(Util.getActiveDownloadCount(context))
                .setHttpDownloader(new OkHttpDownloader(okHttpClient))
                .build();
        fetch = Fetch.Impl.getInstance(fetchConfiguration);
        if (Util.isFetchDebugEnabled(context))
            fetch.enableLogging(true);
    }

    public Fetch getFetchInstance() {
        return fetch;
    }
}

