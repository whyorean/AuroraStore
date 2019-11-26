package com.aurora.store.task;

import android.content.Context;

import com.aurora.store.adapter.NativeHttpClientAdapter;
import com.aurora.store.model.LoginInfo;
import com.aurora.store.utility.Accountant;
import com.aurora.store.utility.ApiBuilderUtil;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

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

    public String getAuthToken(String email, String aas_token) throws Exception {
        GooglePlayAPI api = new GooglePlayAPI();
        api.setDeviceInfoProvider(ApiBuilderUtil.getDeviceInfoProvider(context));
        api.setLocale(Locale.getDefault());
        api.setClient(new NativeHttpClientAdapter(context));
        String gsfId = api.generateGsfId();
        api.setGsfId(gsfId);
        api.uploadDeviceConfig();
        String token = api.generateToken(email, aas_token);

        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setAasToken(aas_token);
        loginInfo.setAuthToken(token);
        loginInfo.setEmail(email);
        loginInfo.setGsfId(gsfId);
        loginInfo.setLocale(Locale.getDefault().toString());
        LoginInfo.save(context, loginInfo);

        return token;
    }
}
