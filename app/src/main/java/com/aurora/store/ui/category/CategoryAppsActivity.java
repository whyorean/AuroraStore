package com.aurora.store.ui.category;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.ColorUtils;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.aurora.store.R;
import com.aurora.store.sheet.FilterBottomSheet;
import com.aurora.store.ui.single.activity.BaseActivity;
import com.aurora.store.util.ViewUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CategoryAppsActivity extends BaseActivity {

    public static String categoryId;
    public static String categoryName;

    @BindView(R.id.search_view)
    TextInputEditText searchView;
    @BindView(R.id.bottom_navigation)
    BottomNavigationView bottomNavigationView;
    @BindView(R.id.filter_fab)
    ExtendedFloatingActionButton filterFab;
    @BindView(R.id.action2)
    AppCompatImageView action2;

    static boolean matchDestination(@NonNull NavDestination destination, @IdRes int destId) {
        NavDestination currentDestination = destination;
        while (currentDestination.getId() != destId && currentDestination.getParent() != null) {
            currentDestination = currentDestination.getParent();
        }
        return currentDestination.getId() == destId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_apps);
        ButterKnife.bind(this);
        action2.setVisibility(View.INVISIBLE);
        onNewIntent(getIntent());
        searchView.setFocusable(false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            categoryId = intent.getStringExtra("CategoryId");
            categoryName = intent.getStringExtra("CategoryName");
            searchView.setText(categoryName);
            setupNavigation();
        }
    }

    @OnClick(R.id.action1)
    public void goBack() {
        onBackPressed();
    }

    @OnClick(R.id.filter_fab)
    public void showFilterDialog() {
        FilterBottomSheet filterSheet = new FilterBottomSheet();
        filterSheet.show(getSupportFragmentManager(), "FILTER");
    }

    private void setupNavigation() {
        int backGroundColor = ViewUtil.getStyledAttribute(this, android.R.attr.colorBackground);
        bottomNavigationView.setBackgroundColor(ColorUtils.setAlphaComponent(backGroundColor, 245));

        NavController navController = Navigation.findNavController(this, R.id.nav_host_category);

        //Avoid Adding same fragment to NavController, if clicked on current BottomNavigation item
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == bottomNavigationView.getSelectedItemId())
                return false;
            NavigationUI.onNavDestinationSelected(item, navController);
            return true;
        });

        //Check correct BottomNavigation item, if navigation_main is done programmatically
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            final Menu menu = bottomNavigationView.getMenu();
            final int size = menu.size();
            for (int i = 0; i < size; i++) {
                MenuItem item = menu.getItem(i);
                if (matchDestination(destination, item.getItemId())) {
                    item.setChecked(true);
                }
            }
        });
    }
}
