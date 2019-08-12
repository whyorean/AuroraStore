package com.aurora.store.task;

import android.content.Context;
import android.content.ContextWrapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NetworkTask extends ContextWrapper {
    public NetworkTask(Context base) {
        super(base);
    }

    public String get(String url) throws Exception {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }
}
