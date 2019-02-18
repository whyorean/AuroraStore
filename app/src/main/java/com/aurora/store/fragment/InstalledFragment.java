package com.aurora.store.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.aurora.store.Constants;
import com.aurora.store.ErrorType;
import com.aurora.store.R;
import com.aurora.store.activity.AuroraActivity;
import com.aurora.store.adapter.InstalledAppsAdapter;
import com.aurora.store.model.App;
import com.aurora.store.task.InstalledApps;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.ViewUtil;
import com.aurora.store.view.CustomSwipeToRefresh;
import com.aurora.store.view.ErrorView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class InstalledFragment extends BaseFragment implements BaseFragment.EventListenerImpl {

    @BindView(R.id.view_switcher)
    ViewSwitcher mViewSwitcher;
    @BindView(R.id.content_view)
    LinearLayout layoutContent;
    @BindView(R.id.err_view)
    LinearLayout layoutError;
    @BindView(R.id.swipe_layout)
    CustomSwipeToRefresh mSwipeRefreshLayout;
    @BindView(R.id.recycler)
    RecyclerView mRecyclerView;
    @BindView(R.id.switch_system)
    SwitchMaterial switchSystem;

    private Context context;
    private BottomNavigationView mBottomNavigationView;
    private View view;
    private List<App> mInstalledApps = new ArrayList<>(new HashSet<>());
    private InstalledAppsAdapter mAdapter;

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_installed, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setErrorView(ErrorType.UNKNOWN);
        fetchData();

        switchSystem.setChecked(PrefUtil.getBoolean(context, Constants.PREFERENCE_INCLUDE_SYSTEM));
        switchSystem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                PrefUtil.putBoolean(context, Constants.PREFERENCE_INCLUDE_SYSTEM, true);
            else
                PrefUtil.putBoolean(context, Constants.PREFERENCE_INCLUDE_SYSTEM, false);
            fetchData();
        });

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            //TODO:Block Tokenizer to single task
            fetchData();
        });

        if (getActivity() instanceof AuroraActivity)
            mBottomNavigationView = ((AuroraActivity) getActivity()).getBottomNavigation();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter == null && mInstalledApps.isEmpty())
            fetchData();

    }

    @Override
    public void onPause() {
        super.onPause();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void fetchData() {
        InstalledApps mTaskHelper = new InstalledApps(context);
        mDisposable.add(Observable.fromCallable(() -> mTaskHelper.getInstalledApps(switchSystem.isChecked()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscription -> mSwipeRefreshLayout.setRefreshing(true))
                .subscribe((mApps) -> {
                    if (view != null) {
                        mInstalledApps = mApps;
                        if (mApps.isEmpty()) {
                            setErrorView(ErrorType.NO_APPS);
                            switchViews(true);
                        } else {
                            switchViews(false);
                            setupRecycler(mApps);
                        }
                    }
                }, err -> {
                    Log.e(err.getMessage());
                    processException(err);
                }));
    }

    private void setupRecycler(List<App> mApps) {
        mSwipeRefreshLayout.setRefreshing(false);
        mAdapter = new InstalledAppsAdapter(context, mApps);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        mRecyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(context, R.anim.anim_falldown));
        mRecyclerView.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                if (velocityY < 0) {
                    if (mBottomNavigationView != null)
                        ViewUtil.showBottomNav(mBottomNavigationView, true);
                } else if (velocityY > 0) {
                    if (mBottomNavigationView != null)
                        ViewUtil.hideBottomNav(mBottomNavigationView, true);
                }
                return false;
            }
        });
    }

    private void setErrorView(ErrorType errorType) {
        layoutError.removeAllViews();
        layoutError.addView(new ErrorView(context, errorType, retry()));
    }

    private View.OnClickListener retry() {
        return v -> {
            fetchData();
            ((Button) v).setText(getString(R.string.action_retry_ing));
            ((Button) v).setEnabled(false);
        };
    }

    private void switchViews(boolean showError) {
        if (mViewSwitcher.getCurrentView() == layoutContent && showError)
            mViewSwitcher.showNext();
        else if (mViewSwitcher.getCurrentView() == layoutError && !showError)
            mViewSwitcher.showPrevious();
    }

    @Override
    public void onLoggedIn() {
        fetchData();
    }

    @Override
    public void onLoginFailed() {
        setErrorView(ErrorType.UNKNOWN);
        switchViews(true);
    }

    @Override
    public void onNetworkFailed() {
        setErrorView(ErrorType.NO_NETWORK);
        switchViews(true);
    }
}
