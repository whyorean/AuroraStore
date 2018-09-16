/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store (a fork of Yalp Store )is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.dragons.aurora.task;

import android.content.Context;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.dragons.aurora.Aurora;
import com.dragons.aurora.CredentialsEmptyException;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.AccountsActivity;
import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.playstoreapiv2.AuthException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

import timber.log.Timber;

public class UserProvidedCredentialsTask extends CheckCredentialsTask {

    private String Email;
    private Context context;

    public UserProvidedCredentialsTask(Context context) {
        this.context = context;
    }

    public UserProvidedCredentialsTask getUserCredentialsTask() {
        UserProvidedCredentialsTask task = new UserProvidedCredentialsTask(context);
        task.setContext(context);
        task.prepareDialog(R.string.dialog_message_logging_in_provided_by_user, R.string.dialog_title_logging_in);
        return task;
    }

    public void setGooglePrefs(String email, String password) {
        Prefs.putBoolean(context, Aurora.SEC_ACCOUNT, true);
        Prefs.putString(context, Aurora.GOOGLE_EMAIL, email);
        Prefs.putString(context, Aurora.GOOGLE_PASSWORD, password);
    }

    public void removeGooglePrefs() {
        Prefs.putBoolean(context, Aurora.SEC_ACCOUNT, false);
        Prefs.putString(context, Aurora.GOOGLE_EMAIL, "");
        Prefs.putString(context, Aurora.GOOGLE_PASSWORD, "");
    }

    public void withSavedGoogle() {
        String email = Prefs.getString(context, Aurora.GOOGLE_EMAIL);
        String password = Prefs.getString(context, Aurora.GOOGLE_PASSWORD);
        getUserCredentialsTask().execute(email, password);
    }

    @Override
    protected Void doInBackground(String[] params) {
        if (params.length < 2
                || params[0] == null
                || params[1] == null
                || TextUtils.isEmpty(params[0])
                || TextUtils.isEmpty(params[1])
        ) {
            exception = new CredentialsEmptyException();
            return null;
        }

        try {
            new PlayStoreApiAuthenticator(context).login(params[0], params[1]);
            addUsedEmail(params[0]);
        } catch (Throwable e) {
            if (e instanceof AuthException && null != ((AuthException) e).getTwoFactorUrl()) {
                addUsedEmail(params[0]);
            }
            exception = e;
        }
        return null;
    }

    private void addUsedEmail(String email) {
        Set<String> emailsSet = Prefs.getStringSet(context, Aurora.USED_EMAILS_SET);
        emailsSet.add(email);
        Prefs.putStringSet(context, Aurora.USED_EMAILS_SET, emailsSet);
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        if (success()) {
            setUser();
        }
    }

    private void setUser() {
        Email = PreferenceFragment.getString(context, Aurora.PREFERENCE_EMAIL);
        getJSON(Email);
    }

    private void getJSON(String ID) {
        RequestQueue mRequestQueue = Volley.newRequestQueue(context);
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                "http://picasaweb.google.com/data/entry/api/user/" + ID + "?alt=json",
                null,
                obj -> parseJSON(obj, true),
                error -> parseJSON(new JSONObject(), false));
        mRequestQueue.add(jsonObjReq);
    }

    private void parseJSON(JSONObject obj, Boolean success) {
        String Username = Email;
        String ImgURL = "https://imgur.com/qgNTGK1.png";
        if (success) {
            try {
                JSONObject mJsonObject = obj.getJSONObject("entry");
                Username = mJsonObject.getJSONObject("gphoto$nickname").getString("$t");
                ImgURL = mJsonObject.getJSONObject("gphoto$thumbnail").getString("$t");
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
        Prefs.putString(context, Aurora.GOOGLE_NAME, Username);
        Prefs.putString(context, Aurora.GOOGLE_URL, ImgURL);
        Prefs.putBoolean(context, Aurora.LOGGED_IN, true);
        Prefs.putBoolean(context, Aurora.GOOGLE_ACC, true);
        Prefs.putBoolean(context, Aurora.DUMMY_ACC, false);

        if (context instanceof AccountsActivity)
            ((AccountsActivity) context).userChanged();
    }
}
