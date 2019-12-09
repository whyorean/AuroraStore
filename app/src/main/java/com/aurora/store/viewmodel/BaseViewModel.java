package com.aurora.store.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurora.store.enums.ErrorType;
import com.aurora.store.exception.CredentialsEmptyException;
import com.aurora.store.exception.InvalidApiException;
import com.aurora.store.exception.TooManyRequestsException;
import com.aurora.store.util.Log;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.net.UnknownHostException;

import io.reactivex.disposables.CompositeDisposable;

public class BaseViewModel extends AndroidViewModel {

    protected GooglePlayAPI api;
    protected CompositeDisposable disposable = new CompositeDisposable();
    protected MutableLiveData<ErrorType> errorTypeMutableLiveData = new MutableLiveData<>();

    public BaseViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<ErrorType> getError() {
        return errorTypeMutableLiveData;
    }

    public void handleError(Throwable err) {
        if (err instanceof NullPointerException)
            errorTypeMutableLiveData.setValue(ErrorType.NO_API);
        else if (err instanceof CredentialsEmptyException || err instanceof InvalidApiException)
            errorTypeMutableLiveData.setValue(ErrorType.LOGOUT_ERR);
        else if (err instanceof AuthException | err instanceof TooManyRequestsException)
            errorTypeMutableLiveData.setValue(ErrorType.SESSION_EXPIRED);
        else if (err instanceof UnknownHostException)
            errorTypeMutableLiveData.setValue(ErrorType.NO_NETWORK);
        else
            errorTypeMutableLiveData.setValue(ErrorType.UNKNOWN);
        Log.d(err.getMessage());
    }
}
