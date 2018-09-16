package com.dragons.aurora.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import com.dragons.aurora.ContextUtil;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.helpers.Accountant;
import com.dragons.aurora.task.UserProvidedCredentialsTask;
import com.dragons.custom.AuroraDialog;
import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginDialog extends AppCompatDialogFragment {

    @BindView(R.id.email_google)
    TextInputEditText email_google;
    @BindView(R.id.password_google)
    TextInputEditText password_google;
    @BindView(R.id.checkboxSave)
    CheckBox checkbox;
    @BindView(R.id.button_login)
    Button button_login;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_credentials, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AuroraDialog(getContext(), Util.isDark(getContext())
                ? R.style.Theme_Aurora_Dialog_Dark
                : R.style.Theme_Aurora_Dialog);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        button_login.setOnClickListener(v -> init());
    }

    private void init() {
        UserProvidedCredentialsTask mCredentialsTask = new UserProvidedCredentialsTask(getContext());
        if (TextUtils.isEmpty(email_google.getText()) || TextUtils.isEmpty(password_google.getText())) {
            ContextUtil.toast(getContext(), R.string.error_credentials_empty);
        } else {
            String email = email_google.getText().toString();
            String password = password_google.getText().toString();

            if (checkbox.isChecked()) {
                mCredentialsTask.setGooglePrefs(email, password);
            }

            Accountant.completeCheckout(getContext());
            mCredentialsTask.getUserCredentialsTask().execute(email, password);
            dismiss();
        }
    }
}
