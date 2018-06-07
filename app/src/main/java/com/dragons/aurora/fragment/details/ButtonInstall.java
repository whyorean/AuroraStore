package com.dragons.aurora.fragment.details;

import android.app.NotificationManager;
import android.content.Context;
import android.view.View;

import com.dragons.aurora.InstallationState;
import com.dragons.aurora.Paths;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.downloader.DownloadState;
import com.dragons.aurora.model.App;
import com.dragons.aurora.task.InstallTask;

public class ButtonInstall extends Button {

    public ButtonInstall(AuroraActivity activity, App app) {
        super(activity, app);
    }

    @Override
    public void draw() {
        super.draw();
        ((android.widget.Button) button).setText(R.string.details_install);
        if (InstallationState.isInstalling(app.getPackageName())) {
            disable(R.string.details_installing);
        }
    }

    @Override
    protected android.widget.Button getButton() {
        return (android.widget.Button) activity.findViewById(R.id.install);
    }

    @Override
    protected boolean shouldBeVisible() {
        return Paths.getApkPath(activity, app.getPackageName(), app.getVersionCode()).exists()
                && DownloadState.get(app.getPackageName()).isEverythingSuccessful()
                ;
    }

    @Override
    protected void onButtonClick(View v) {
        disable(R.string.details_installing);
        ((NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(app.getDisplayName().hashCode());
        new InstallTask(activity, app).execute();
    }
}
