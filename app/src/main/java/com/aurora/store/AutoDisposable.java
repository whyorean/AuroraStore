package com.aurora.store;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import org.apache.commons.lang3.NotImplementedException;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class AutoDisposable implements LifecycleObserver {

    private CompositeDisposable compositeDisposable;

    public void bindTo(Lifecycle lifecycle) {
        lifecycle.addObserver(this);
        compositeDisposable = new CompositeDisposable();
    }

    public void add(Disposable disposable) {
        if (!compositeDisposable.isDisposed()) {
            compositeDisposable.add(disposable);
        } else {
            throw new NotImplementedException("Must bind AutoDisposable to a Lifecycle first");
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        compositeDisposable.dispose();
    }
}
