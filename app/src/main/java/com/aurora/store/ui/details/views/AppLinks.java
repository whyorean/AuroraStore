package com.aurora.store.ui.details.views;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.sheet.PermissionBottomSheet;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.ui.view.DetailsLinkView;
import com.aurora.store.util.Log;

import butterknife.BindView;

public class AppLinks extends AbstractDetails {

    @BindView(R.id.layout_link)
    LinearLayout linkLayout;

    public AppLinks(DetailsActivity activity, App app) {
        super(activity, app);
    }

    @Override
    public void draw() {
        linkLayout.removeAllViews();
        setupDevApps();
        setupPermissions();
        setupAppPreferences();
    }

    private void setupDevApps() {
        DetailsLinkView devLinkView = new DetailsLinkView(context);
        devLinkView.setLinkText(context.getString(R.string.link_dev));
        devLinkView.setLinkImageId(R.drawable.app_dev);
        devLinkView.setColor(R.color.colorGold);
        devLinkView.setOnClickListener(v -> {
            showDevApps();
        });
        devLinkView.build();
        linkLayout.addView(devLinkView);
    }

    private void setupPermissions() {
        DetailsLinkView permLinkView = new DetailsLinkView(context);
        permLinkView.setLinkText(context.getString(R.string.link_prmission));
        permLinkView.setLinkImageId(R.drawable.app_permission);
        permLinkView.setColor(R.color.colorCyan);
        permLinkView.setOnClickListener(v -> {
            final PermissionBottomSheet permissionBottomSheet = new PermissionBottomSheet();
            final Bundle bundle = new Bundle();
            bundle.putString(Constants.STRING_EXTRA, gson.toJson(app));
            permissionBottomSheet.setArguments(bundle);
            permissionBottomSheet.show(activity.getSupportFragmentManager(), "PERMISSION");
        });
        permLinkView.build();
        linkLayout.addView(permLinkView);
    }

    private void setupAppPreferences() {
        if (!app.isInstalled()) {
            return;
        }
        DetailsLinkView prefLinkView = new DetailsLinkView(context);
        prefLinkView.setLinkText(context.getString(R.string.link_setting));
        prefLinkView.setLinkImageId(R.drawable.ic_menu_settings);
        prefLinkView.setColor(R.color.colorOrange);
        prefLinkView.setOnClickListener(v -> {
            try {
                context.startActivity(new Intent("android.settings.APPLICATION_DETAILS_SETTINGS",
                        Uri.parse("package:" + app.getPackageName())));
            } catch (ActivityNotFoundException e) {
                Log.e("Could not find system app activity");
            }
        });
        prefLinkView.build();
        linkLayout.addView(prefLinkView);
    }
}
