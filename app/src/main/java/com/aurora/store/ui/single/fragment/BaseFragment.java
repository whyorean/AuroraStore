package com.aurora.store.ui.single.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.aurora.store.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Modifier;

public class BaseFragment extends Fragment {

    protected boolean awaiting = false;
    protected Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    protected void showSnackBar(View view, @StringRes int message, int duration, View.OnClickListener onClickListener) {
        Snackbar snackbar = Snackbar.make(view, message, duration);
        if (onClickListener != null)
            snackbar.setAction(R.string.action_retry, onClickListener);
        snackbar.show();
    }

    protected void showSnackBar(View view, @StringRes int message, View.OnClickListener onClickListener) {
        showSnackBar(view, message, 0, onClickListener);
    }
}
