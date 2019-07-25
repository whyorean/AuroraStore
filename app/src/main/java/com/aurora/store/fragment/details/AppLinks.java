package com.aurora.store.fragment.details;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.LinearLayout;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.model.App;
import com.aurora.store.sheet.PermissionBottomSheet;
import com.aurora.store.utility.Log;
import com.aurora.store.view.DetailsLinkView;

import butterknife.BindView;

public class AppLinks extends AbstractHelper {

    @BindView(R.id.layout_link)
    LinearLayout linkLayout;

    public AppLinks(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        linkLayout.removeAllViews();
        setupShare();
        setupDevApps();
        setupPermissions();
        setupAppPreferences();
        setupPlayLink();
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
            PermissionBottomSheet profileFragment = new PermissionBottomSheet();
            profileFragment.setApp(app);
            profileFragment.show(fragment.getChildFragmentManager(), "PERMISSION");
        });
        permLinkView.build();
        linkLayout.addView(permLinkView);
    }

    private void setupShare() {
        DetailsLinkView shareLinkView = new DetailsLinkView(context);
        shareLinkView.setLinkText(context.getString(R.string.link_share));
        shareLinkView.setLinkImageId(R.drawable.app_share);
        shareLinkView.setColor(R.color.colorPurple);
        shareLinkView.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, app.getDisplayName());
            i.putExtra(Intent.EXTRA_TEXT, Constants.APP_DETAIL_URL + app.getPackageName());
            context.startActivity(Intent.createChooser(i, fragment.getActivity().getString(R.string.details_share)));
        });
        shareLinkView.build();
        linkLayout.addView(shareLinkView);
    }

    private void setupAppPreferences() {
        if (!app.isInstalled()) {
            return;
        }
        DetailsLinkView prefLinkView = new DetailsLinkView(context);
        prefLinkView.setLinkText(context.getString(R.string.link_setting));
        prefLinkView.setLinkImageId(R.drawable.app_settings);
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

    private void setupPlayLink() {
        if (!isPlayStoreInstalled() || !app.isInPlayStore()) {
            return;
        }
        DetailsLinkView playLinkView = new DetailsLinkView(context);
        playLinkView.setLinkText(context.getString(R.string.link_playstore));
        playLinkView.setLinkImageId(R.drawable.app_playstore);
        playLinkView.setColor(R.color.colorGreen);
        playLinkView.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(Constants.APP_DETAIL_URL + app.getPackageName()));
            context.startActivity(i);
        });
        playLinkView.build();
        linkLayout.addView(playLinkView);
    }
}
