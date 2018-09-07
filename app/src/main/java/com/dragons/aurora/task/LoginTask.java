package com.dragons.aurora.task;

import android.content.Context;
import android.preference.PreferenceManager;

import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.activities.AccountsActivity;

import java.io.IOException;

public class LoginTask extends AppProvidedCredentialsTask {

    private Context context;

    public LoginTask(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void payload() throws IOException {
        new PlayStoreApiAuthenticator(context).login();
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        if (success()) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("LOGGED_IN", true).apply();
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("DUMMY_ACC", true).apply();
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("GOOGLE_ACC", false).apply();
            if (context instanceof AccountsActivity)
                ((AccountsActivity) context).userChanged();
        }
    }
}
