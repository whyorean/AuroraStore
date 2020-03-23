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
import com.aurora.store.model.App;
import com.aurora.store.util.Log;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

public class BaseViewModel extends AndroidViewModel {

    protected GooglePlayAPI api;
    protected MutableLiveData<ErrorType> errorData = new MutableLiveData<>();

    public BaseViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<ErrorType> getError() {
        return errorData;
    }

    public List<App> sortList(List<App> appList) {
        Collections.sort(appList, (App1, App2) -> App1.getDisplayName().compareToIgnoreCase(App2.getDisplayName()));
        return appList;
    }

    public void handleError(Throwable err) {
        if (err instanceof NullPointerException)
            errorData.setValue(ErrorType.NO_API);
        else if (err instanceof CredentialsEmptyException || err instanceof InvalidApiException)
            errorData.setValue(ErrorType.LOGOUT_ERR);
        else if (err instanceof AuthException | err instanceof TooManyRequestsException)
            errorData.setValue(ErrorType.SESSION_EXPIRED);
        else if (err instanceof UnknownHostException)
            errorData.setValue(ErrorType.NO_NETWORK);
        else
            errorData.setValue(ErrorType.UNKNOWN);
        Log.d(err.getMessage());
    }
}
