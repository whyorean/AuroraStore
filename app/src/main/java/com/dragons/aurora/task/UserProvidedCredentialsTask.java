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

import android.app.Dialog;
import android.content.Context;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.CredentialsEmptyException;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.AccountsActivity;
import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.playstoreapiv2.AuthException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.dragons.aurora.helpers.Prefs.DUMMY_ACC;
import static com.dragons.aurora.helpers.Prefs.GOOGLE_ACC;
import static com.dragons.aurora.helpers.Prefs.GOOGLE_EMAIL;
import static com.dragons.aurora.helpers.Prefs.GOOGLE_NAME;
import static com.dragons.aurora.helpers.Prefs.GOOGLE_PASSWORD;
import static com.dragons.aurora.helpers.Prefs.GOOGLE_URL;
import static com.dragons.aurora.helpers.Prefs.LOGGED_IN;
import static com.dragons.aurora.helpers.Prefs.SEC_ACCOUNT;
import static com.dragons.aurora.helpers.Prefs.USED_EMAILS_SET;

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

    private AutoCompleteTextView getEmailInput(Dialog ad) {
        AutoCompleteTextView editEmail = (AutoCompleteTextView) ad.findViewById(R.id.email);
        editEmail.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, getUsedEmails()));
        String previousEmail = "";
        editEmail.setText(PreferenceManager.getDefaultSharedPreferences(context).getString(PlayStoreApiAuthenticator.PREFERENCE_EMAIL, previousEmail));
        return editEmail;
    }

    private List<String> getUsedEmails() {
        List<String> emails = new ArrayList<>(Prefs.getStringSet(context, USED_EMAILS_SET));
        Collections.sort(emails);
        return emails;
    }

    private void setGooglePrefs(String email, String password) {
        Prefs.putBoolean(context, SEC_ACCOUNT, true);
        Prefs.putString(context, GOOGLE_EMAIL, email);
        Prefs.putString(context, GOOGLE_PASSWORD, password);
    }

    public void removeGooglePrefs() {
        Prefs.putBoolean(context, SEC_ACCOUNT, false);
        Prefs.putString(context, GOOGLE_EMAIL, "");
        Prefs.putString(context, GOOGLE_PASSWORD, "");
    }

    public void withSavedGoogle() {
        String email = Prefs.getString(context, GOOGLE_EMAIL);
        String password = Prefs.getString(context, GOOGLE_PASSWORD);
        getUserCredentialsTask().execute(email, password);
    }

    public void logInWithGoogleAccount() {
        Dialog ad = new Dialog(context);
        ad.setContentView(R.layout.dialog_credentials);
        ad.setTitle(context.getString(R.string.credentials_title));
        ad.setCancelable(false);

        AutoCompleteTextView editEmail = getEmailInput(ad);
        EditText editPassword = ad.findViewById(R.id.password);
        final CheckBox checkBox = ad.findViewById(R.id.checkboxSave);

        editEmail.setText("");

        ad.findViewById(R.id.button_exit).setOnClickListener(v -> ad.dismiss());
        ad.findViewById(R.id.button_ok).setOnClickListener(view -> {
            Context c = view.getContext();
            String email = editEmail.getText().toString();
            String password = editPassword.getText().toString();
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                ContextUtil.toast(c.getApplicationContext(), R.string.error_credentials_empty);
                return;
            }
            ad.dismiss();

            if (checkBox.isChecked()) {
                setGooglePrefs(email, password);
            }
            Accountant.completeCheckout(context);
            getUserCredentialsTask().execute(email, password);
        });

        ad.findViewById(R.id.toggle_password_visibility).setOnClickListener(v -> {
            boolean passwordVisible = !TextUtils.isEmpty((String) v.getTag());
            v.setTag(passwordVisible ? null : "tag");
            ((ImageView) v).setImageResource(passwordVisible ? R.drawable.ic_visibility_on : R.drawable.ic_visibility_off);
            editPassword.setInputType(passwordVisible ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT);
        });

        ad.show();
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
        Set<String> emailsSet = Prefs.getStringSet(context, USED_EMAILS_SET);
        emailsSet.add(email);
        Prefs.putStringSet(context, USED_EMAILS_SET, emailsSet);
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        if (success()) {
            setUser();
        }
    }

    private void setUser() {
        Email = PreferenceFragment.getString(context, PlayStoreApiAuthenticator.PREFERENCE_EMAIL);
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
                Log.e(getClass().getSimpleName(), e.getMessage());
            }
        }
        Prefs.putString(context, GOOGLE_NAME, Username);
        Prefs.putString(context, GOOGLE_URL, ImgURL);
        Prefs.putBoolean(context, LOGGED_IN, true);
        Prefs.putBoolean(context, GOOGLE_ACC, true);
        Prefs.putBoolean(context, DUMMY_ACC, false);

        if (context instanceof AccountsActivity)
            ((AccountsActivity) context).userChanged();
    }
}
