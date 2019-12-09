package com.aurora.store.ui.main.fragment.updates;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.AuroraApplication;
import com.aurora.store.enums.ErrorType;
import com.aurora.store.exception.CredentialsEmptyException;
import com.aurora.store.exception.InvalidApiException;
import com.aurora.store.model.App;
import com.aurora.store.task.UpdatableAppsTask;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.net.UnknownHostException;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class UpdatableAppsModel extends AndroidViewModel {

    private Application application;
    private CompositeDisposable disposable = new CompositeDisposable();

    private GooglePlayAPI api;


    private MutableLiveData<List<App>> listMutableLiveData = new MutableLiveData<>();
    private MutableLiveData<ErrorType> errorTypeMutableLiveData = new MutableLiveData<>();

    public UpdatableAppsModel(@NonNull Application application) {
        super(application);
        this.application = application;
        this.api = AuroraApplication.api;
        fetchUpdatableApps();
    }

    public LiveData<ErrorType> getErrorTypeMutableLiveData() {
        return errorTypeMutableLiveData;
    }

    public LiveData<List<App>> getListMutableLiveData() {
        return listMutableLiveData;
    }

    public void fetchUpdatableApps() {
        disposable.add(Observable.fromCallable(() -> new UpdatableAppsTask(api, application)
                .getUpdatableApps())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((appList) -> {
                    listMutableLiveData.setValue(appList);
                }, err -> {
                    if (err instanceof CredentialsEmptyException || err instanceof InvalidApiException)
                        errorTypeMutableLiveData.setValue(ErrorType.LOGOUT_ERR);
                    else if (err instanceof AuthException)
                        errorTypeMutableLiveData.setValue(ErrorType.SESSION_EXPIRED);
                    else if (err instanceof UnknownHostException)
                        errorTypeMutableLiveData.setValue(ErrorType.NO_NETWORK);
                    else
                        errorTypeMutableLiveData.setValue(ErrorType.UNKNOWN);
                }));
    }

    @Override
    protected void onCleared() {
        disposable.dispose();
        super.onCleared();
    }
}
