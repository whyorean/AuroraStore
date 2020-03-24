package com.aurora.store.ui.single.activity;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.aurora.store.AuroraApplication;
import com.aurora.store.R;
import com.aurora.store.events.Event;
import com.aurora.store.util.ThemeUtil;
import com.aurora.store.viewmodel.ConnectionLiveData;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Modifier;

public abstract class BaseActivity extends AppCompatActivity {

    protected int intExtra;
    protected String stringExtra;
    protected boolean awaiting = false;
    protected Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();

    private ThemeUtil themeUtil = new ThemeUtil();
    private ConnectionLiveData connectionLiveData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        themeUtil.onCreate(this);
        connectionLiveData = new ConnectionLiveData(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        themeUtil.onResume(this);
        connectionLiveData.observe(this, connectionModel -> {
            AuroraApplication
                    .rxNotify(new Event(connectionModel.isConnected()
                            ? Event.SubType.NETWORK_AVAILABLE
                            : Event.SubType.NETWORK_UNAVAILABLE));
        });
    }

    @Override
    protected void onDestroy() {
        connectionLiveData.removeObservers(this);
        super.onDestroy();
    }

    protected void showSnackBar(View view, @StringRes int message, int duration, View.OnClickListener onClickListener) {
        Snackbar snackbar = Snackbar.make(view, message, duration);
        snackbar.setAction(R.string.action_retry, onClickListener);
        snackbar.show();
    }

    protected void showSnackBar(View view, @StringRes int message, View.OnClickListener onClickListener) {
        showSnackBar(view, message, 0, onClickListener);
    }
}
