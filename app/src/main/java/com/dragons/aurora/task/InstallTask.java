package com.dragons.aurora.task;

import android.content.Context;
import android.os.AsyncTask;

import com.dragons.aurora.InstallerAbstract;
import com.dragons.aurora.InstallerFactory;
import com.dragons.aurora.model.App;

public class InstallTask extends AsyncTask<Void, Void, Void> {

    private App app;
    private InstallerAbstract installer;

    public InstallTask(Context context, App app) {
        this(InstallerFactory.get(context), app);
    }

    public InstallTask(InstallerAbstract installer, App app) {
        this.installer = installer;
        this.app = app;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        installer.verifyAndInstall(app);
        return null;
    }
}
