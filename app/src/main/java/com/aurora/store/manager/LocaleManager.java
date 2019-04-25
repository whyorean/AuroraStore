package com.aurora.store.manager;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.aurora.store.Constants;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.Util;

import java.util.Locale;

public class LocaleManager {

    private Context context;

    public LocaleManager(Context context) {
        this.context = context;
    }

    public Locale getLocale() {
        return Util.isCustomLocaleEnabled(context) ? getCustomLocale() : Locale.getDefault();
    }

    private Locale getCustomLocale() {
        String language = PrefUtil.getString(context, Constants.PREFERENCE_LOCALE_LANG);
        String country = PrefUtil.getString(context, Constants.PREFERENCE_LOCALE_COUNTRY);
        if (language.equals("b")) {
            return new Locale(country);
        } else return new Locale(language, country);
    }

    public void setLocale() {
        updateResources(getLocale());
    }

    public void setNewLocale(Locale locale, boolean isCustom) {
        if (isCustom)
            saveLocale(locale);
        updateResources(locale);
    }

    private void saveLocale(Locale locale) {
        PrefUtil.putString(context, Constants.PREFERENCE_LOCALE_LANG, locale.getLanguage());
        PrefUtil.putString(context, Constants.PREFERENCE_LOCALE_COUNTRY, locale.getCountry());
        PrefUtil.putBoolean(context, Constants.PREFERENCE_LOCALE_CUSTOM, true);
    }

    private void updateResources(Locale locale) {
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }
}
