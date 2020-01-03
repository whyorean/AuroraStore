package com.aurora.store.ui.preference.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.ui.preference.SettingsActivity;
import com.aurora.store.util.ContextUtil;
import com.aurora.store.util.PackageUtil;
import com.aurora.store.util.PathUtil;
import com.aurora.store.util.PrefUtil;
import com.aurora.store.util.Root;
import com.aurora.store.util.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.scottyab.rootbeer.RootBeer;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class InstallationFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String ROOT = "1";
    private static final String SERVICES = "2";

    private Context context;
    private CompositeDisposable disposable = new CompositeDisposable();

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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SharedPreferences preferences = Util.getPrefs(context);
        preferences.registerOnSharedPreferenceChangeListener(this);

        ListPreference listInstallMethod = findPreference(Constants.PREFERENCE_INSTALLATION_METHOD);
        assert listInstallMethod != null;
        listInstallMethod.setOnPreferenceChangeListener((preference, newValue) -> {
            String installMethod = (String) newValue;
            if (installMethod.equals(ROOT)) {
                RootBeer rootBeer = new RootBeer(context);
                if (rootBeer.isRooted()) {
                    Root.requestRoot();
                    PrefUtil.putString(context, Constants.PREFERENCE_INSTALLATION_METHOD, installMethod);
                    showDownloadDialog();
                    return true;
                } else {
                    showNoRootDialog();
                    return false;
                }
            } else if (installMethod.equals(SERVICES)) {
                if (PrefUtil.getBoolean(context, Constants.PREFERENCE_DOWNLOAD_INTERNAL)) {
                    showInternalDialog();
                    return false;
                }

                if (PackageUtil.isInstalled(context, Constants.SERVICE_PACKAGE)) {
                    PrefUtil.putString(context, Constants.PREFERENCE_INSTALLATION_METHOD, installMethod);
                    PrefUtil.putString(context, Constants.PREFERENCE_DOWNLOAD_DIRECTORY, PathUtil.getExtBaseDirectory(context));
                    return true;
                } else {
                    showNoServicesDialog();
                    return false;
                }
            } else {
                PrefUtil.putString(context, Constants.PREFERENCE_INSTALLATION_METHOD, installMethod);
                return true;
            }
        });

        Preference servicePreference = findPreference(Constants.PREFERENCE_LAUNCH_SERVICES);
        assert servicePreference != null;
        if (PackageUtil.isInstalled(context, Constants.SERVICE_PACKAGE)) {
            servicePreference.setEnabled(true);
            servicePreference.setOnPreferenceClickListener(preference -> {
                try {
                    Intent intent = context.getPackageManager().getLaunchIntentForPackage(Constants.SERVICE_PACKAGE);
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    context.startActivity(intent);
                } catch (Exception e) {
                    ContextUtil.toastLong(context, "Could not launch services");
                }
                return false;
            });
        } else {
            servicePreference.setSummary(getString(R.string.pref_services_desc_alt));
            servicePreference.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://gitlab.com/AuroraOSS/AuroraServices"));
                startActivity(intent);
                return false;
            });
        }

        Preference abandonPreference = findPreference(Constants.PREFERENCE_INSTALLATION_ABANDON_SESSION);
        assert abandonPreference != null;
        abandonPreference.setOnPreferenceClickListener(preference -> {
            Util.clearOldInstallationSessions(context);
            ContextUtil.toastLong(context, getString(R.string.toast_abandon_sessions));
            return false;
        });

        ListPreference userProfilePreference = findPreference(Constants.PREFERENCE_INSTALLATION_PROFILE);
        assert userProfilePreference != null;
        try {
            addUserInfoData(userProfilePreference);
        } catch (Exception e) {
            userProfilePreference.setEnabled(false);
        }

        userProfilePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            String installMethod = (String) newValue;
            PrefUtil.putString(context, Constants.PREFERENCE_INSTALLATION_PROFILE, installMethod);
            return true;
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Constants.PREFERENCE_INSTALLATION_TYPE:
            case Constants.PREFERENCE_INSTALLATION_METHOD:
                SettingsActivity.shouldRestart = true;
                break;
        }
    }

    private void addUserInfoData(ListPreference listPreference) {

        disposable.add(Observable.fromCallable(() -> new Root())
                .map(root -> {
                    if (!root.isAcquired())
                        return root;

                    List<String> rawUserList = getUserInfo(root);
                    List<String> entryList = new ArrayList<>();
                    List<String> entryValueList = new ArrayList<>();

                    for (String rawUser : rawUserList) {
                        String[] rawUserArray = rawUser.split(":");
                        entryValueList.add(rawUserArray[0]);
                        entryList.add(rawUserArray[1]);
                    }

                    CharSequence[] entries = new CharSequence[entryList.size()];
                    CharSequence[] entryValues = new CharSequence[entryValueList.size()];
                    for (int i = 0; i < entryList.size(); i++) {
                        entries[i] = entryList.get(i);
                        entryValues[i] = entryValueList.get(i);
                    }

                    listPreference.setEntries(entries);
                    listPreference.setEntryValues(entryValues);
                    return root;
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe());
    }

    private List<String> getUserInfo(Root root) {
        List<String> rawUserList = new ArrayList<>();
        String result = root.exec("pm list users");
        Scanner scanner = new Scanner(result);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Pattern p = Pattern.compile("\\{(.*):");
            Matcher m = p.matcher(line);
            while (m.find()) {
                String rawUser = m.group(1);
                rawUserList.add(rawUser);
            }
        }
        return rawUserList;
    }

    private void showInternalDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.pref_app_download);
        builder.setMessage(R.string.pref_install_mode_internal_warn);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        builder.create();
        builder.show();
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

    private void showNoServicesDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.action_installations);
        builder.setMessage(R.string.pref_install_mode_no_services);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        builder.create();
        builder.show();
    }
}