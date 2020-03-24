package com.aurora.store.viewmodel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.lifecycle.LiveData;

import com.aurora.store.model.ConnectionModel;

import org.jetbrains.annotations.NotNull;

public class ConnectionLiveData extends LiveData<ConnectionModel> {

    private Context context;

    private BroadcastReceiver networkReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() != null) {
                final Object object = context.getSystemService(Context.CONNECTIVITY_SERVICE);
                final ConnectivityManager manager = (ConnectivityManager) object;

                if (manager != null) {
                    final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(@NotNull Network network) {
                            postValue(new ConnectionModel("ONLINE", true));
                        }

                        @Override
                        public void onLost(@NotNull Network network) {
                            postValue(new ConnectionModel("OFFLINE", false));
                        }
                    };

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        manager.registerDefaultNetworkCallback(networkCallback);
                    } else {
                        final NetworkRequest networkRequest = new NetworkRequest.Builder()
                                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
                        manager.registerNetworkCallback(networkRequest, networkCallback);
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
