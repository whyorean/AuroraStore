package com.dragons.aurora.fragment;

import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;

import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.LoginActivity;
import com.dragons.aurora.task.AppProvidedCredentialsTask;
import com.dragons.aurora.task.UserProvidedCredentialsTask;

public abstract class AccountsHelper extends Fragment {

    protected boolean isLoggedIn() {
        return PreferenceFragment.getBoolean(getContext(), "LOGGED_IN");
    }

    protected boolean isDummy() {
        return PreferenceFragment.getBoolean(getContext(), "DUMMY_ACC");
    }

    protected boolean isGoogle() {
        return PreferenceFragment.getBoolean(getContext(), "GOOGLE_ACC");
    }

    protected void checkOut() {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("LOGGED_IN", false).apply();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString("GOOGLE_NAME", "").apply();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString("GOOGLE_URL", "").apply();
    }

    protected void LoginFirst() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.action_login)
                .setMessage(R.string.header_usr_noEmail)
                .setPositiveButton(R.string.action_login, (dialogInterface, i) -> startActivity(new Intent(getActivity(), LoginActivity.class)))
                .setCancelable(false)
                .show();
    }

    public void switchDummy() {
        if (isLoggedIn())
            new PlayStoreApiAuthenticator(getContext()).logout();

        AppProvidedCredentialsTask.LoginTask task = new AppProvidedCredentialsTask.LoginTask(getContext());
        task.setContext(getContext());
        task.prepareDialog(R.string.dialog_message_switching_in_predefined, R.string.dialog_title_logging_in);
        task.execute();
    }

    public void switchGoogle() {
        new UserProvidedCredentialsTask(getContext()).logInWithGoogleAccount();
    }

    public void loginWithDummy() {
        if (isLoggedIn())
            new PlayStoreApiAuthenticator(getContext()).logout();
        new AppProvidedCredentialsTask(getContext()).logInWithPredefinedAccount();
    }

    protected void refreshMyToken() {
        new AppProvidedCredentialsTask(getContext()).refreshToken();
    }

}
