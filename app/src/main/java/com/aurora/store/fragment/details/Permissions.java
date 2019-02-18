package com.aurora.store.fragment.details;

import android.widget.ImageView;

import com.aurora.store.R;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.model.App;
import com.aurora.store.sheet.PermissionBottomSheet;

import butterknife.BindView;
import butterknife.ButterKnife;

public class Permissions extends AbstractHelper {

    @BindView(R.id.app_permissions)
    ImageView app_permissions;

    public Permissions(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        ButterKnife.bind(this, view);
        app_permissions.setOnClickListener(v -> {
            showPermissions();
        });
    }

    private void showPermissions() {
        PermissionBottomSheet profileFragment = new PermissionBottomSheet();
        profileFragment.setApp(app);
        profileFragment.show(fragment.getChildFragmentManager(), "PERMISSION");
    }
}