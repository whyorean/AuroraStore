package com.aurora.store.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.Toast;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.TokenDispenserMirrors;
import com.aurora.store.adapter.OkHttpClientAdapter;
import com.aurora.store.exception.CredentialsEmptyException;
import com.aurora.store.exception.TokenizerException;
import com.aurora.store.exception.TwoFactorAuthException;
import com.aurora.store.manager.LocaleManager;
import com.aurora.store.manager.SpoofManager;
import com.aurora.store.model.LoginInfo;
import com.aurora.store.provider.NativeDeviceInfoProvider;
import com.dragons.aurora.playstoreapiv2.ApiBuilderException;
import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.DeviceInfoProvider;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.PlayStoreApiBuilder;
import com.dragons.aurora.playstoreapiv2.PropertiesDeviceInfoProvider;
import com.dragons.aurora.playstoreapiv2.TokenDispenserException;

import java.io.IOException;
import java.util.Locale;

public class ApiBuilderUtil {

    private static int RETRIES = 5;
    private static TokenDispenserMirrors tokenDispenserMirrors = new TokenDispenserMirrors();

    public static GooglePlayAPI buildFromPreferences(Context context) throws IOException {
        SharedPreferences prefs = Util.getPrefs(context);
        String email = prefs.getString(Accountant.ACCOUNT_EMAIL, "");
        if (TextUtils.isEmpty(email)) {
            throw new CredentialsEmptyException();
        }
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setEmail(email);
        return build(context, loginInfo);
    }

    public static GooglePlayAPI build(Context context, LoginInfo loginInfo) throws IOException {
        GooglePlayAPI api = build(context, loginInfo, RETRIES);
        if (api != null) {
            loginInfo.setGsfId(api.getGsfId());
            loginInfo.setToken(api.getToken());
            loginInfo.setDfeCookie(api.getDfeCookie());
            loginInfo.setDeviceConfigToken(api.getDeviceConfigToken());
            loginInfo.setDeviceCheckinConsistencyToken(api.getDeviceCheckinConsistencyToken());
            saveLoginInfo(context, loginInfo);
        }
        return api;
    }

    public static GooglePlayAPI build(Context context, LoginInfo loginInfo, int retries) throws IOException {
        int tried = 0;
        while (tried < retries) {
            try {
                PlayStoreApiBuilder builder = getBuilder(context, loginInfo);
                GooglePlayAPI api = builder.build();
                loginInfo.setEmail(builder.getEmail());
                return api;
            } catch (ApiBuilderException e) {
                throw new RuntimeException(e);
            } catch (AuthException | TokenDispenserException e) {
                if (Util.noNetwork(e.getCause())) {
                    ContextUtil.runOnUiThread(() -> Toast.makeText(context,
                            context.getString(R.string.error_no_network),
                            Toast.LENGTH_LONG).show());
                    throw (IOException) e.getCause();
                }
                if (loginInfo.appProvidedEmail()) {
                    loginInfo.setTokenDispenserUrl(tokenDispenserMirrors.get(context));
                    loginInfo.setEmail(null);
                    loginInfo.setGsfId(null);
                }
                tried++;
                if (tried >= retries) {
                    throw new TokenizerException("Exceeded max retries", e.getCause());
                }
                if (e instanceof AuthException && ((AuthException) e).getTwoFactorUrl() != null)
                    throw new TwoFactorAuthException("2FA Enabled");
                Log.i("Anonymous Login Failed @ %s, attempt %d",
                        loginInfo.getTokenDispenserUrl(), tried);
            }
        }
        return null;
    }

    private static PlayStoreApiBuilder getBuilder(Context context, LoginInfo loginInfo) {
        SharedPreferences prefs = Util.getPrefs(context);
        String locale = prefs.getString(Constants.PREFERENCE_REQUESTED_LANGUAGE, "");
        loginInfo.setLocale(TextUtils.isEmpty(locale) ? Locale.getDefault().toString() : locale);
        loginInfo.setGsfId(prefs.getString(Accountant.GSF_ID, ""));
        loginInfo.setToken(prefs.getString(Accountant.AUTH_TOKEN, ""));
        loginInfo.setTokenDispenserUrl(tokenDispenserMirrors.get(context));

        return new PlayStoreApiBuilder()
                .setHttpClient(new OkHttpClientAdapter(context))
                .setDeviceInfoProvider(getDeviceInfoProvider(context))
                .setLocale(loginInfo.getLocale())
                .setEmail(loginInfo.getEmail())
                .setPassword(loginInfo.getPassword())
                .setGsfId(loginInfo.getGsfId())
                .setToken(loginInfo.getToken())
                .setTokenDispenserUrl(loginInfo.getTokenDispenserUrl())
                .setDeviceCheckinConsistencyToken(loginInfo.getDeviceCheckinConsistencyToken())
                .setDeviceConfigToken(loginInfo.getDeviceConfigToken())
                .setDfeCookie(loginInfo.getDfeCookie());
    }

    private static DeviceInfoProvider getDeviceInfoProvider(Context context) {
        DeviceInfoProvider deviceInfoProvider;
        String spoofDevice = PrefUtil.getString(context, Constants.PREFERENCE_DEVICE_TO_PRETEND_TO_BE);
        if (TextUtils.isEmpty(spoofDevice)) {
            deviceInfoProvider = new NativeDeviceInfoProvider();
            ((NativeDeviceInfoProvider) deviceInfoProvider).setContext(context);
            ((NativeDeviceInfoProvider) deviceInfoProvider).setLocaleString(new LocaleManager(context).getLocale().toString());
        } else {
            deviceInfoProvider = new PropertiesDeviceInfoProvider();
            ((PropertiesDeviceInfoProvider) deviceInfoProvider).setProperties(new SpoofManager(context).getProperties(spoofDevice));
            ((PropertiesDeviceInfoProvider) deviceInfoProvider).setLocaleString(new LocaleManager(context).getLocale().toString());
        }
        return deviceInfoProvider;
    }

    private static void saveLoginInfo(Context context, LoginInfo loginInfo) {
        Util.getPrefs(context)
                .edit()
                .putString(Accountant.ACCOUNT_EMAIL, loginInfo.getEmail())
                .putString(Accountant.GSF_ID, loginInfo.getGsfId())
                .putString(Accountant.AUTH_TOKEN, loginInfo.getToken())
                .putString(Accountant.LAST_USED_TOKEN_DISPENSER, loginInfo.getTokenDispenserUrl())
                .apply();
    }
}
