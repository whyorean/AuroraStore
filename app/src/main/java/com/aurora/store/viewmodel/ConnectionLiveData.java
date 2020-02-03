package com.aurora.store.viewmodel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.lifecycle.LiveData;

import com.aurora.store.model.ConnectionModel;

public class ConnectionLiveData extends LiveData<ConnectionModel> {

    private Context context;

    private BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() != null) {

                Object object = context.getSystemService(Context.CONNECTIVITY_SERVICE);
                ConnectivityManager manager = (ConnectivityManager) object;

                if (manager != null) {
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        NetworkCapabilities capabilities = manager.getNetworkCapabilities(manager.getActiveNetwork());
                        if (capabilities != null) {
                            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)
                                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN)
                                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE)) {
                                postValue(new ConnectionModel("ONLINE", true));
                            } else {

                                postValue(new ConnectionModel("OFFLINE", false));
                            }
                        }
                    } else {
                        NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
                        try {
                            if (activeNetwork != null) {
                                boolean isConnected = activeNetwork.isConnectedOrConnecting();
                                postValue(new ConnectionModel(activeNetwork.getTypeName(), isConnected));
                            }
                        } catch (Exception e) {
                            postValue(new ConnectionModel(activeNetwork.getTypeName(), false));
                        }
                    }
                }
            }
        }
    };

    public ConnectionLiveData(Context context) {
        this.context = context;
    }

    @Override
    protected void onActive() {
        super.onActive();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(networkReceiver, filter);
    }

    @Override
    protected void onInactive() {
        context.unregisterReceiver(networkReceiver);
        super.onInactive();
    }
}
