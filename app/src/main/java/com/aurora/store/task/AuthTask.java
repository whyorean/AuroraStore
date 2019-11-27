package com.aurora.store.task;

import android.content.Context;

import com.aurora.store.adapter.NativeHttpClientAdapter;
import com.aurora.store.model.LoginInfo;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.ApiBuilderUtil;
import com.aurora.store.utility.PrefUtil;
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
        api.setClient(new NativeHttpClientAdapter(context));
        return api.generateAASToken(email, oauth_token);
    }

    public boolean getAuthToken(String email, String aas_token) throws Exception {
        GooglePlayAPI api = new GooglePlayAPI();
        api.setDeviceInfoProvider(ApiBuilderUtil.getDeviceInfoProvider(context));
        api.setLocale(Locale.getDefault());
        api.setClient(new NativeHttpClientAdapter(context));
        String gsfId = api.generateGsfId();
        api.setGsfId(gsfId);
        api.uploadDeviceConfig();
        String token = api.generateToken(email, aas_token);
        api.setToken(token);

        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setAasToken(aas_token);
        loginInfo.setAuthToken(token);
        loginInfo.setEmail(email);
        loginInfo.setGsfId(gsfId);
        loginInfo.setLocale(Locale.getDefault().toString());
        LoginInfo.save(context, loginInfo);

        UserProfile userProfile = api.userProfile().getUserProfile();
        PrefUtil.putString(context, Accountant.EMAIL, email);
        PrefUtil.putString(context, Accountant.PROFILE_NAME, userProfile.getName());
        for (Image image : userProfile.getImageList()) {
            if (image.getImageType() == GooglePlayAPI.IMAGE_TYPE_APP_ICON) {
                PrefUtil.putString(context, Accountant.PROFILE_AVATAR, image.getImageUrl());
            }
        }
        return !loginInfo.isEmpty();
    }
}
