package com.dragons.aurora;

import android.content.Context;
import android.util.Log;

import com.dragons.aurora.model.App;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class InstallerRoot extends InstallerBackground {

    public InstallerRoot(Context context) {
        super(context);
    }

    @Override
    protected void install(App app) {
        InstallationState.setInstalling(app.getPackageName());
        boolean success = shellInstall(Paths.getApkPath(context, app.getPackageName(), app.getVersionCode()).toString());
        if (success) {
            InstallationState.setSuccess(app.getPackageName());
        } else {
            InstallationState.setFailure(app.getPackageName());
        }
        sendBroadcast(app.getPackageName(), true);
        postInstallationResult(app, success);
    }

    private boolean shellInstall(String file) {
        List<String> lines = Shell.SU.run("pm install -i \"" + BuildConfig.APPLICATION_ID + "\" -r " + file);
        if (null != lines) {
            for (String line : lines) {
                Log.i(getClass().getSimpleName(), line);
            }
        }
        return null != lines && lines.size() == 1 && lines.get(0).equals("Success");
    }
}