package com.aurora.store.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aurora.store.R;
import com.aurora.store.activity.CategoriesActivity;
import com.aurora.store.view.FeaturedAppsView;
import com.google.android.material.chip.Chip;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ExploreFragment extends BaseFragment implements BaseFragment.EventListenerImpl {

    @BindView(R.id.all_chip)
    Chip allChip;
    @BindView(R.id.bulk_layout)
    LinearLayout bulk_layout;

    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_apps, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        addViews();
        allChip.setOnClickListener(v -> {
            Intent intent = new Intent(context, CategoriesActivity.class);
            intent.putExtra("INTENT_CATEGORY", "APPLICATION");
            context.startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bulk_layout.getChildCount() < 1)
            addViews();
    }

    private void addViews() {
        bulk_layout.addView(new FeaturedAppsView(context, "Personalization", "PERSONALIZATION"));
        bulk_layout.addView(new FeaturedAppsView(context, "Communication", "COMMUNICATION"));
        bulk_layout.addView(new FeaturedAppsView(context, "Tools", "TOOLS"));
    }

    @Override
    public void onLoggedIn() {
        bulk_layout.removeAllViews();
    }

    @Override
    public void onLoginFailed() {

    }

    @Override
    public void onNetworkFailed() {

    }
}
