package com.dragons.aurora.fragment;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.dragons.aurora.R;
import com.dragons.aurora.model.App;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailsFragmentMore extends BaseFragment {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.content_readMore)
    TextView contentReadMore;
    @BindView(R.id.txt_updated)
    TextView txtUpdated;
    @BindView(R.id.txt_google_dependencies)
    TextView txtDependencies;
    @BindView(R.id.txt_rating)
    TextView txtRating;
    @BindView(R.id.rating_img)
    ImageView ratingImg;

    private App app;

    public DetailsFragmentMore() {
    }

    public void setApp(App app) {
        this.app = app;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_details_readmore, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mToolbar.setNavigationIcon(R.drawable.ic_cancel);
        mToolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
        mToolbar.setTitle(app.getDisplayName());
        contentReadMore.setText(Html.fromHtml(app.getDescription()).toString());

        txtUpdated.setText(app.getUpdated());
        txtDependencies.setText(app.getDependencies().isEmpty()
                ? R.string.list_app_independent_from_gsf
                : R.string.list_app_depends_on_gsf);
        txtRating.setText(app.getLabeledRating());

        Glide.with(getContext())
                .load(app.getRatingURL())
                .apply(new RequestOptions()
                        .placeholder(ContextCompat.getDrawable(getContext(), R.drawable.ic_audience))
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                .into(ratingImg);
    }

}
