package com.dragons.aurora.task;

import android.content.Context;

import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.activities.AccountsActivity;
import com.dragons.aurora.helpers.Prefs;

import java.io.IOException;

import timber.log.Timber;

public class RefreshTokenTask extends AppProvidedCredentialsTask {

    private Context context;

    public RefreshTokenTask(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void payload() throws IOException {
        new PlayStoreApiAuthenticator(context).refreshToken();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Prefs.putBoolean(context, "REFRESH_ASKED", true);
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        if (success()) {
            Prefs.putBoolean(context, "REFRESH_ASKED", false);
            if (context instanceof AccountsActivity)
                ((AccountsActivity) context).notifyTokenRefreshed();
            else
                Timber.i("Token Refreshed");
        }
    }
}
