package com.aurora.store.fragment.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.scottyab.rootbeer.RootBeer;

public class InstallationFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String ROOT = "1";

    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(Constants.SHARED_PREFERENCES_KEY);
        setPreferencesFromResource(R.xml.preferences_installation, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedPreferences mPrefs = Util.getPrefs(context);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        ListPreference listInstallMethod = findPreference(Constants.PREFERENCE_INSTALLATION_METHOD);
        assert listInstallMethod != null;
        listInstallMethod.setOnPreferenceChangeListener((preference, newValue) -> {
            String installMethod = (String) newValue;
            if (installMethod.equals(ROOT)) {
                RootBeer rootBeer = new RootBeer(context);
                if (rootBeer.isRooted()) {
                    PrefUtil.putString(context, Constants.PREFERENCE_INSTALLATION_METHOD, installMethod);
                    showDownloadDialog();
                    return true;
                } else {
                    showNoRootDialog();
                    return false;
                }
            } else {
                PrefUtil.putString(context, Constants.PREFERENCE_INSTALLATION_METHOD, installMethod);
                return true;
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }

    private void showDownloadDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.pref_app_download);
        builder.setMessage(R.string.pref_install_mode_root_warn);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        builder.create();
        builder.show();
    }

    private void showNoRootDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.action_installations);
        builder.setMessage(R.string.pref_install_mode_no_root);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        builder.create();
        builder.show();
    }
}