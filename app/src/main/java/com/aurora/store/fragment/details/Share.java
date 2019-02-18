package com.aurora.store.fragment.details;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.fragment.DetailsFragment;
import com.aurora.store.model.App;

public class Share extends AbstractHelper {

    public Share(DetailsFragment fragment, App app) {
        super(fragment, app);
    }

    @Override
    public void draw() {
        ImageView share = view.findViewById(R.id.share);
        share.setVisibility(View.VISIBLE);
        share.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, app.getDisplayName());
            i.putExtra(Intent.EXTRA_TEXT, Constants.APP_DETAIL_URL + app.getPackageName());
            context.startActivity(Intent.createChooser(i, fragment.getActivity().getString(R.string.details_share)));
        });
    }
}