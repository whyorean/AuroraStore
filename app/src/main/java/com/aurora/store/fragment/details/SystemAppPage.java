package com.aurora.store.fragment.details;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageView;

import com.aurora.store.R;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.model.App;
import com.aurora.store.utility.Log;

import butterknife.BindView;

public class SystemAppPage extends AbstractHelper {

    @BindView(R.id.system_app_info)
    ImageView systemAppInfo;

    public SystemAppPage(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        if (!app.isInstalled()) {
            hide(fragment.getView(), R.id.system_app_info);
            return;
        }
        show(fragment.getView(), R.id.system_app_info);
        systemAppInfo.setOnClickListener(v -> startActivity());
    }

    private void startActivity() {
        try {
            context.startActivity(getIntent());
        } catch (ActivityNotFoundException e) {
            Log.e("Could not find system app activity");
        }
    }

    private Intent getIntent() {
        return new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.parse("package:" + app.getPackageName()));
    }
}