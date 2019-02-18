package com.aurora.store.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PrefUtil {

    public static void putString(Context context, String key, String value) {
        Util.getPrefs(context.getApplicationContext()).edit().putString(key, value).apply();
    }

    public static void putInteger(Context context, String key, int value) {
        Util.getPrefs(context.getApplicationContext()).edit().putInt(key, value).apply();
    }

    public static void putFloat(Context context, String key, float value) {
        Util.getPrefs(context.getApplicationContext()).edit().putFloat(key, value).apply();
    }

    public static void putBoolean(Context context, String key, boolean value) {
        Util.getPrefs(context.getApplicationContext()).edit().putBoolean(key, value).apply();
    }

    public static void putListString(Context context, String key, ArrayList<String> stringList) {
        String[] myStringList = stringList.toArray(new String[stringList.size()]);
        Util.getPrefs(context.getApplicationContext()).edit().putString(key, TextUtils.join("‚‗‚", myStringList)).apply();
    }

    public static void putStringSet(Context context, String key, Set<String> set) {
        Util.getPrefs(context.getApplicationContext()).edit().putStringSet(key, set).apply();
    }


    public static String getString(Context context, String key) {
        return Util.getPrefs(context.getApplicationContext()).getString(key, "");
    }

    public static int getInteger(Context context, String key) {
        return Util.getPrefs(context.getApplicationContext()).getInt(key, 0);
    }

    public static float getFloat(Context context, String key) {
        return Util.getPrefs(context.getApplicationContext()).getFloat(key, 0.0f);
    }

    public static Boolean getBoolean(Context context, String key) {
        return Util.getPrefs(context.getApplicationContext()).getBoolean(key, false);
    }

    public static ArrayList<String> getListString(Context context, String key) {
        return new ArrayList<String>(Arrays.asList(TextUtils.split(
                Util.getPrefs(context.getApplicationContext()).getString(key, ""), "‚‗‚")));
    }

    public static Set<String> getStringSet(Context context, String key) {
        return Util.getPrefs(context.getApplicationContext()).getStringSet(key, new HashSet<>());
    }

    public static void saveMap(Context context, Map<String, Integer> map, String key) {
        SharedPreferences mPreferences = Util.getPrefs(context);
        if (mPreferences != null) {
            JSONObject jsonObject = new JSONObject(map);
            String jsonString = jsonObject.toString();
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.remove(key).apply();
            editor.putString(key, jsonString);
            editor.commit();
        }
    }

    public static Map<String, Integer> getMap(Context context, String key) {
        Map<String, Integer> outputMap = new HashMap<>();
        SharedPreferences mPreferences = Util.getPrefs(context);
        try {
            if (mPreferences != null) {
                String jsonString = mPreferences.getString(key, (new JSONObject()).toString());
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keysItr = jsonObject.keys();
                while (keysItr.hasNext()) {
                    String k = keysItr.next();
                    Integer value = (Integer) jsonObject.get(k);
                    outputMap.put(k, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outputMap;
    }
}
