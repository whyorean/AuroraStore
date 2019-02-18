package com.aurora.store.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aurora.store.utility.Accountant;
import com.aurora.store.Constants;
import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.activity.AuroraActivity;
import com.aurora.store.activity.IntroActivity;
import com.aurora.store.api.PlayStoreApiAuthenticator;
import com.aurora.store.task.UserProfiler;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.PrefUtil;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.Image;
import com.google.android.material.chip.Chip;

import org.jetbrains.annotations.NotNull;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static com.aurora.store.utility.ContextUtil.runOnUiThread;

public class AccountsFragment extends BaseFragment implements BaseFragment.EventListenerImpl {

    @BindView(R.id.view_switcher_top)
    ViewSwitcher mViewSwitcherTop;
    @BindView(R.id.view_switcher_bottom)
    ViewSwitcher mViewSwitcherBottom;
    @BindView(R.id.view_switcher_login)
    ViewSwitcher mViewSwitcherLogin;
    @BindView(R.id.init)
    LinearLayout initLayout;
    @BindView(R.id.info)
    LinearLayout infoLayout;
    @BindView(R.id.login)
    LinearLayout loginLayout;
    @BindView(R.id.logout)
    LinearLayout logoutLayout;
    @BindView(R.id.login_google)
    RelativeLayout loginGoogle;
    @BindView(R.id.login_dummy)
    RelativeLayout loginDummy;
    @BindView(R.id.avatar)
    ImageView imgAvatar;
    @BindView(R.id.user_name)
    TextView txtName;
    @BindView(R.id.user_mail)
    TextView txtMail;
    @BindView(R.id.txt_input_email)
    TextView txtInputEmail;
    @BindView(R.id.txt_input_password)
    TextView txtInputPassword;
    @BindView(R.id.user_account_chip)
    Chip accountSwitch;
    @BindView(R.id.progress_bar)
    ProgressBar mProgressBar;
    @BindView(R.id.btn_positive)
    Button btnPositive;
    @BindView(R.id.btn_positive_alt)
    Button btnPositiveAlt;
    @BindView(R.id.btn_negative)
    Button btnNegative;

    private Context context;
    private boolean dummyAcc = false;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accounts, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        dummyAcc = Accountant.isDummy(context);
        init();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.clear();
        mCompositeDisposable.dispose();
    }

    @Override
    public void onLoggedIn() {

    }

    @Override
    public void onLoginFailed() {

    }

    @Override
    public void onNetworkFailed() {

    }

    private void init() {
        setupView();
        setupAccType();
        setupActions();
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    private void setupView() {
        if (Accountant.isLoggedIn(context)) {
            switchTopViews(true);
            switchBottomViews(true);
            if (Accountant.isDummy(context))
                loadDummyData();
            else
                loadGoogleData();
        } else {
            switchTopViews(false);
            switchBottomViews(false);
        }
    }

    private void setupAccType() {
        dummyAcc = Accountant.isDummy(context);
        accountSwitch.setText(dummyAcc ? R.string.account_dummy : R.string.account_google);
    }

    private void setupActions() {
        btnPositive.setOnClickListener(loginListener());
        btnPositiveAlt.setOnClickListener(loginListener());
        btnNegative.setOnClickListener(logoutListener());
        accountSwitch.setOnClickListener(switchAccountListener());
    }

    private void loadDummyData() {
        imgAvatar.setImageDrawable(context.getDrawable(R.drawable.ic_avatar_boy));
        txtName.setText(Accountant.getUserName(context));
        txtMail.setText(Accountant.getEmail(context));
    }

    private void loadGoogleData() {
        GlideApp
                .with(this)
                .load(Accountant.getImageURL(context))
                .circleCrop()
                .into(imgAvatar);
        txtName.setText(Accountant.getUserName(context));
        txtMail.setText(Accountant.getEmail(context));
    }

    private void switchTopViews(boolean showInfo) {
        if (mViewSwitcherTop.getCurrentView() == initLayout && showInfo)
            mViewSwitcherTop.showNext();
        else if (mViewSwitcherTop.getCurrentView() == infoLayout && !showInfo)
            mViewSwitcherTop.showPrevious();
    }

    private void switchBottomViews(boolean showLogout) {
        if (mViewSwitcherBottom.getCurrentView() == loginLayout && showLogout)
            mViewSwitcherBottom.showNext();
        else if (mViewSwitcherBottom.getCurrentView() == logoutLayout && !showLogout)
            mViewSwitcherBottom.showPrevious();
    }

    private void switchLoginViews(boolean showGoogle) {
        if (mViewSwitcherLogin.getCurrentView() == loginGoogle && !showGoogle)
            mViewSwitcherLogin.showNext();
        else if (mViewSwitcherLogin.getCurrentView() == loginDummy && showGoogle)
            mViewSwitcherLogin.showPrevious();
    }

    private void switchButtonState(boolean logging) {
        btnPositive.setText(logging ? R.string.action_logging_in : R.string.action_login);
        btnPositiveAlt.setText(logging ? R.string.action_logging_in : R.string.action_login);
        btnPositive.setEnabled(!logging);
        btnPositiveAlt.setEnabled(!logging);
        mProgressBar.setVisibility(logging ? View.VISIBLE : View.INVISIBLE);
    }

    private View.OnClickListener logoutListener() {
        return v -> {
            Accountant.completeCheckout(context);
            switchTopViews(false);
            switchBottomViews(false);
            switchButtonState(false);
        };
    }

    private View.OnClickListener loginListener() {
        return v -> {
            if (dummyAcc) {
                logInWithDummy();
            } else {
                logInWithGoogle(txtInputEmail.getText().toString(), txtInputPassword.getText().toString());
            }
        };
    }

    private View.OnClickListener switchAccountListener() {
        return v -> {
            if (dummyAcc) {
                dummyAcc = false;
                accountSwitch.setText(R.string.account_google);
                if (!Accountant.isLoggedIn(context))
                    switchLoginViews(true);
            } else {
                dummyAcc = true;
                accountSwitch.setText(R.string.account_dummy);
                if (!Accountant.isLoggedIn(context))
                    switchLoginViews(false);
            }
        };
    }

    private void logInWithDummy() {
        switchButtonState(true);
        mCompositeDisposable.add(Observable.fromCallable(() ->
                new PlayStoreApiAuthenticator(context).login())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe((success) -> {
                    if (success) {
                        Log.i("Dummy Login Successful");
                        runOnUiThread(() -> {
                            Accountant.saveDummy(context);
                            init();
                            finishIntro();
                        });
                    } else {
                        Log.e("Dummy Login Failed Permanently");
                        switchButtonState(false);
                    }
                }, err -> {
                    Log.e("Dummy Login failed %s", err.getMessage());
                    switchButtonState(false);
                }));
    }

    private void logInWithGoogle(String email, String password) {
        switchButtonState(true);
        mCompositeDisposable.add(Observable.fromCallable(() ->
                new PlayStoreApiAuthenticator(context).login(email, password))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((success) -> {
                    if (success) {
                        Log.d("Google Login Successful");
                        runOnUiThread(() -> {
                            Accountant.saveGoogle(context);
                            getUserInfo();
                            finishIntro();
                        });
                    } else {
                        Log.e("Google Login Failed Permanently");
                        switchButtonState(false);
                    }
                }, err -> {
                    Log.e("Google Login failed : %s", err.getMessage());
                    mProgressBar.setVisibility(View.INVISIBLE);
                    txtInputPassword.setError("Check your password");
                    switchButtonState(false);
                }));
    }

    private void getUserInfo() {
        mCompositeDisposable.add(Observable.fromCallable(() ->
                new UserProfiler(context).getUserProfile())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((profile) -> {
                    if (profile != null) {
                        PrefUtil.putString(context, Accountant.GOOGLE_NAME, profile.getName());
                        for (Image image : profile.getImageList()) {
                            if (image.getImageType() == GooglePlayAPI.IMAGE_TYPE_APP_ICON) {
                                PrefUtil.putString(context, Accountant.GOOGLE_URL, image.getImageUrl());
                            }
                        }
                        runOnUiThread(this::init);
                    }
                }, err -> Log.e("Google Login failed : %s", err.getMessage())));
    }

    private void finishIntro() {
        if (getActivity() instanceof IntroActivity) {
            PrefUtil.putBoolean(context, Constants.PREFERENCE_DO_NOT_SHOW_INTRO, true);
            getActivity().startActivity(new Intent(context, AuroraActivity.class));
            getActivity().finish();
        }
    }
}
