package com.dragons.aurora.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.dragons.aurora.CategoryManager;
import com.dragons.aurora.CircleTransform;
import com.dragons.aurora.R;
import com.dragons.aurora.activities.AccountsActivity;
import com.dragons.aurora.activities.CategoryAppsActivity;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.task.CategoryTaskHelper;
import com.dragons.aurora.task.FeaturedTaskHelper;
import com.dragons.aurora.view.AdaptiveToolbar;
import com.dragons.aurora.view.MoreAppsCard;
import com.dragons.custom.TagView;
import com.squareup.picasso.Picasso;

public class HomeFragment extends UtilFragment {

    private AdaptiveToolbar adtb;
    private View view;
    private LinearLayout topLinks;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view != null) {
            if ((ViewGroup) view.getParent() != null)
                ((ViewGroup) view.getParent()).removeView(view);
            return view;
        }
        view = inflater.inflate(R.layout.fragment_home, container, false);

        adtb = view.findViewById(R.id.adtb);
        adtb.getAvatar_icon().setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AccountsActivity.class);
            intent.putExtra("account_profile_animate", true);
            startActivity(intent);
        });

        initTags();

        topLinks = view.findViewById(R.id.top_links);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isLoggedIn()) {
            if (isConnected(getContext())) {
                setUser();
                if (topLinks.getVisibility() == View.GONE) {
                    setupTopFeatured();
                    drawCategories();
                }
            }
        } else {
            resetUser();
            LoginFirst();
        }
    }

    protected void setUser() {
        if (isGoogle()) {
            Picasso.with(getActivity())
                    .load(PreferenceFragment.getString(getActivity(), "GOOGLE_URL"))
                    .placeholder(R.drawable.ic_user_placeholder)
                    .transform(new CircleTransform())
                    .into(adtb.getAvatar_icon());
        } else {
            (adtb.getAvatar_icon()).setImageDrawable(getResources()
                    .getDrawable(R.drawable.ic_dummy_avatar));
        }
    }

    protected void resetUser() {
        (adtb.getAvatar_icon()).setImageDrawable(getResources()
                .getDrawable(R.drawable.ic_user_placeholder));
    }

    protected void initTags() {
        setupTag(view, R.id.tag_gamesAction, "GAME_ACTION");
        setupTag(view, R.id.tag_family, "FAMILY");
        setupTag(view, R.id.tag_gamesRacing, "GAME_RACING");
        setupTag(view, R.id.tag_travel, "TRAVEL_AND_LOCAL");
        setupTag(view, R.id.tag_social, "SOCIAL");
    }

    protected void setupTag(View v, int viewID, String Category) {
        TagView tagView = v.findViewById(viewID);
        if (tagView.getStyle() == 0)
            tagView.setMono_title(new CategoryManager(getContext()).getCategoryName(Category));
        else {
            if (Category.contains("GAME_"))
                tagView.setDual_title0(getString(R.string.tagview_games));
            else
                tagView.setDual_title0(getString(R.string.tagview_family));
            tagView.setDual_title1(new CategoryManager(getContext()).getCategoryName(Category));
        }
        tagView.setOnClickListener(click -> getActivity()
                .startActivity(CategoryAppsActivity.start(v.getContext(), Category)));
    }

    protected void setupTopFeatured() {
        RecyclerView topGrossingGames = view.findViewById(R.id.top_featured_games);
        RecyclerView topGrossingApps = view.findViewById(R.id.top_featured_apps);

        new FeaturedTaskHelper(getContext(), topGrossingGames).getCategoryApps("GAME",
                GooglePlayAPI.SUBCATEGORY.TOP_GROSSING);
        new FeaturedTaskHelper(getContext(), topGrossingApps).getCategoryApps("APPLICATION",
                GooglePlayAPI.SUBCATEGORY.TOP_GROSSING);
    }

    protected void drawCategories() {
        GooglePlayAPI.SUBCATEGORY subcategory = GooglePlayAPI.SUBCATEGORY.TOP_GROSSING;
        LinearLayout topLinksLayout = view.findViewById(R.id.top_links);
        topLinksLayout.setVisibility(View.VISIBLE);
        topLinksLayout.addView(buildAppsCard("TOOLS", subcategory,
                new CategoryManager(getContext()).getCategoryName("TOOLS")));
        topLinksLayout.addView(buildAppsCard("COMMUNICATION", subcategory,
                new CategoryManager(getContext()).getCategoryName("COMMUNICATION")));
        topLinksLayout.addView(buildAppsCard("MUSIC_AND_AUDIO", subcategory,
                new CategoryManager(getContext()).getCategoryName("MUSIC_AND_AUDIO")));
        topLinksLayout.addView(buildAppsCard("PERSONALIZATION", subcategory,
                new CategoryManager(getContext()).getCategoryName("PERSONALIZATION")));
    }

    private MoreAppsCard buildAppsCard(String categoryId, GooglePlayAPI.SUBCATEGORY subcategory, String label) {
        MoreAppsCard moreAppsCard = new MoreAppsCard(getContext(), categoryId, label);

        RecyclerView recyclerView = moreAppsCard.findViewById(R.id.m_apps_recycler);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        moreAppsCard.setLayoutParams(params);
        moreAppsCard.setGravity(Gravity.CENTER_VERTICAL);

        new CategoryTaskHelper(getActivity(), recyclerView).getCategoryApps(categoryId, subcategory);
        return moreAppsCard;
    }
}
