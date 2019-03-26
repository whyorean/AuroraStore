package com.aurora.store.fragment;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.aurora.store.SharedPreferencesTranslator;
import com.aurora.store.utility.Util;

import io.reactivex.disposables.CompositeDisposable;

public class ContainerFragment extends Fragment {

    CompositeDisposable disposable = new CompositeDisposable();
    SharedPreferencesTranslator translator;
    Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        translator = new SharedPreferencesTranslator(Util.getPrefs(context));
    }

}
