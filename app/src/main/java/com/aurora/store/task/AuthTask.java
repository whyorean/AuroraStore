package com.aurora.store.task;

import android.content.Context;

import com.aurora.store.AuroraApplication;
import com.aurora.store.adapter.OkHttpClientAdapter;
import com.aurora.store.model.LoginInfo;
import com.aurora.store.util.Accountant;
import com.aurora.store.util.ApiBuilderUtil;
import com.aurora.store.util.PrefUtil;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.Image;
import com.dragons.aurora.playstoreapiv2.UserProfile;

import java.util.Locale;

public class AuthTask extends BaseTask {

    public AuthTask(Context context) {
        super(context);
    }

    public String getAASToken(String email, String oauth_token) throws Exception {
        GooglePlayAPI api = new GooglePlayAPI();
        api.setLocale(Locale.getDefault());
        api.setDeviceInfoProvider(ApiBuilderUtil.getDeviceInfoProvider(context));
        api.setClient(new OkHttpClientAdapter(context));
        return api.generateAASToken(email, oauth_token);
    }

    public boolean getAuthToken(String email, String aasToken) throws Exception {
        GooglePlayAPI api = new GooglePlayAPI();
        api.setDeviceInfoProvider(ApiBuilderUtil.getDeviceInfoProvider(context));
        api.setLocale(Locale.getDefault());
        api.setClient(new OkHttpClientAdapter(context));

        String gsfId = api.generateGsfId();
        api.setGsfId(gsfId);
        api.uploadDeviceConfig();

        String token = api.generateToken(email, aasToken);
        api.setToken(token);

        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setAasToken(aasToken);
        loginInfo.setAuthToken(token);
        loginInfo.setEmail(email);
        loginInfo.setGsfId(gsfId);
        loginInfo.setLocale(Locale.getDefault().toString());
        loginInfo.setDeviceCheckinConsistencyToken(api.getDeviceCheckinConsistencyToken());
        loginInfo.setDeviceConfigToken(api.getDeviceConfigToken());
        loginInfo.setDfeCookie(api.getDfeCookie());

        LoginInfo.save(context, loginInfo);

        UserProfile userProfile = api.userProfile().getUserProfile();
        PrefUtil.putString(context, Accountant.EMAIL, email);
        PrefUtil.putString(context, Accountant.PROFILE_NAME, userProfile.getName());
        for (Image image : userProfile.getImageList()) {
            if (image.getImageType() == GooglePlayAPI.IMAGE_TYPE_APP_ICON) {
                PrefUtil.putString(context, Accountant.PROFILE_AVATAR, image.getImageUrl());
            }

            if (image.getImageType() == GooglePlayAPI.IMAGE_TYPE_GOOGLE_PLUS_BACKGROUND) {
                PrefUtil.putString(context, Accountant.PROFILE_BACKGROUND, image.getImageUrl());
            }
        }

        AuroraApplication.api = api;
        return !loginInfo.isEmpty();
    }
}
