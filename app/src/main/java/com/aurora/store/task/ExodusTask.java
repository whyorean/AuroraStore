package com.aurora.store.task;

import android.content.Context;
import android.content.ContextWrapper;

import com.aurora.store.BuildConfig;
import com.aurora.store.util.NetworkInterceptor;
import com.aurora.store.util.Util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ExodusTask extends ContextWrapper {
    /*This is Yalp Store's EXODUS API key, will replace with mine once I get it.*/
    private static final String EXODUS_API_KEY = "Token bf1108aec9c28c5c286c63e89230a71f77b35a5d";
    private Context context;

    public ExodusTask(Context context) {
        super(context);
        this.context = context;
    }

    private static OkHttpClient getOkHttpClient(Context context) {
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (Util.isNetworkProxyEnabled(context))
            builder.proxy(Util.getNetworkProxy(context));
        if (BuildConfig.DEBUG) {
            builder.addNetworkInterceptor(new NetworkInterceptor());
        }
        return builder.build();
    }

    public String get(String url) throws Exception {
        OkHttpClient client = getOkHttpClient(context);
        Request request = new Request.Builder()
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", EXODUS_API_KEY)
                .url(url).build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }
}
