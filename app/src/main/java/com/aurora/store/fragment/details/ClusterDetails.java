package com.aurora.store.fragment.details;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.fragment.app.FragmentTransaction;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.fragment.DevAppsFragment;
import com.aurora.store.model.App;
import com.aurora.store.utility.Log;
import com.aurora.store.view.ClusterAppsView;

import butterknife.BindView;

public class ClusterDetails extends AbstractHelper {

    @BindView(R.id.apps_by_same_developer)
    ImageView imgDev;
    @BindView(R.id.cluster_links)
    LinearLayout relatedLinksLayout;

    public ClusterDetails(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        /*relatedLinksLayout.setVisibility(View.VISIBLE);
        for (String label : app.getRelatedLinks().keySet()) {
            relatedLinksLayout.addView(new ClusterAppsView(context, app.getRelatedLinks().get(label), label));
        }*/
        addAppsByThisDeveloper();
    }

    private void addAppsByThisDeveloper() {
        imgDev.setVisibility(View.VISIBLE);
        imgDev.setOnClickListener(v -> {
            DevAppsFragment devAppsFragment = new DevAppsFragment();
            Bundle arguments = new Bundle();
            arguments.putString("SearchQuery", Constants.PUB_PREFIX + app.getDeveloperName());
            arguments.putString("SearchTitle", app.getDeveloperName());
            devAppsFragment.setArguments(arguments);
            fragment.getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, devAppsFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(null)
                    .commit();
        });
    }
}
