package com.dragons.aurora.database;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import com.dragons.aurora.model.App;
import com.dragons.aurora.model.History;
import com.dragons.aurora.model.ImageSource;
import com.dragons.aurora.model.Rating;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

public class Jessie {

    public static String JSON_INSTALLED = "INSTALLED";
    public static String JSON_UPDATES = "UPDATES";
    public static String JSON_HISTORY = "HISTORY";
    public static String JSON_APP_HISTORY = "APP_HISTORY";

    private String TAG = "JESSIE";

    private Context mContext;

    /*
     *
     * Constructors
     *
     */

    public Jessie(Context mContext) {
        this.mContext = mContext;
    }

    /*
     *
     * Common Json Manipulation Methods
     *
     */

    public JSONArray getJsonArray(List<JSONObject> mJsonObjects) {
        JSONArray mJsonArray = new JSONArray();
        for (JSONObject obj : mJsonObjects)
            mJsonArray.put(obj);
        return mJsonArray;
    }

    public void writeJsonToFile(String filename, JSONArray mJsonArray) {
        if (mJsonArray != null) {
            try {
                FileWriter mFileWriter = new FileWriter(mContext.getFilesDir().getPath() + "/" + filename + ".json");
                mFileWriter.write(mJsonArray.toString());
                mFileWriter.flush();
                mFileWriter.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    public JSONArray readJsonArrayFromFile(String JsonName) {
        try {
            File mFile = new File(mContext.getFilesDir().getPath() + "/" + JsonName + ".json");
            FileInputStream mFileInputStream = new FileInputStream(mFile);
            int size = mFileInputStream.available();
            byte[] buffer = new byte[size];
            mFileInputStream.read(buffer);
            mFileInputStream.close();
            String mResponse = new String(buffer, StandardCharsets.UTF_8);
            return new JSONArray(mResponse);
        } catch (IOException | JSONException e) {
            Log.e(TAG, e.getMessage());
            return new JSONArray();
        }
    }

    public boolean isJsonAvailable(String JsonName) {
        return (new File(mContext.getFilesDir().getPath() + "/" + JsonName + ".json").exists());
    }

    public void removeJson(String JsonName) {
        if (isJsonAvailable(JsonName)) {
            File mFile = new File(mContext.getFilesDir().getPath() + "/" + JsonName + ".json");
            mFile.delete();
        } else Log.i(TAG, "File does not exixts");
    }


    /*
     *
     * Methods to create JSON from AppList
     *
     */

    private JSONObject getAppObject(App mApp) {
        JSONObject mAppObject = new JSONObject();
        if (mApp != null) {
            try {
                mAppObject.put("package_name", mApp.getPackageName());
                mAppObject.put("display_name", mApp.getDisplayName());
                mAppObject.put("version_name", mApp.getVersionName().isEmpty() ? "" : mApp.getVersionName());
                mAppObject.put("version_code", mApp.getVersionCode());
                mAppObject.put("icon_url", mApp.getIconInfo().getUrl());
                mAppObject.put("banner_url", mApp.getPageBackgroundImage().getUrl());
                mAppObject.put("app_changes", mApp.getChanges().isEmpty() ? "" : mApp.getChanges());
                mAppObject.put("app_size", mApp.getSize());
                mAppObject.put("app_updated", mApp.getUpdated().isEmpty() ? "" : mApp.getUpdated());
                mAppObject.put("app_rating", mApp.getRating().getAverage());
                mAppObject.put("app_installed", mApp.isInstalled());
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return mAppObject;
    }

    public List<JSONObject> getJsonObjects(List<App> mAppList) {
        List<JSONObject> mJsonObjects = new ArrayList<>();
        for (App mApp : mAppList) {
            mJsonObjects.add(getAppObject(mApp));
        }
        return mJsonObjects;
    }

    /*
     *
     * Methods to Retrieve AppList from JSON
     *
     */

    public List<App> getAppsFromJsonArray(JSONArray mJsonArray) {
        List<App> mApps = new ArrayList<>();
        try {
            for (int i = 0; i < mJsonArray.length(); i++) {
                JSONObject mJsonObject = mJsonArray.getJSONObject(i);
                App mApp = new App();
                PackageInfo mPackageInfo = new PackageInfo();
                mPackageInfo.packageName = mJsonObject.getString("package_name");
                mApp.setPackageInfo(mPackageInfo);
                mApp.setDisplayName(mJsonObject.getString("display_name"));
                mApp.setVersionName(mJsonObject.getString("version_name"));
                mApp.setVersionCode(mJsonObject.getInt("version_code"));
                mApp.setIconUrl(mJsonObject.getString("icon_url"));
                ImageSource mImageSource = new ImageSource();
                mImageSource.setUrl(mJsonObject.getString("banner_url"));
                mApp.setPageBackgroundImage(mImageSource);
                Rating mRating = mApp.getRating();
                mRating.setAverage(BigDecimal.valueOf(mJsonObject.getDouble("app_rating")).floatValue());
                mApp.setInstalled(mJsonObject.getBoolean("app_installed"));
                mApp.setSize(mJsonObject.getLong("app_size"));
                mApp.setUpdated(mJsonObject.getString("app_updated"));
                mApp.setChanges(mJsonObject.getString("app_changes"));
                mApps.add(mApp);
            }
            return mApps;
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return new ArrayList<>();
        }
    }

    /*
     *
     * Methods to Update AppList from JSON
     *
     */

    public void removeAppFromJson(String JsonName, int position) {
        JSONArray mJsonArray = readJsonArrayFromFile(JsonName);
        mJsonArray.remove(position);
        writeJsonToFile(JsonName, mJsonArray);
    }

    /*
     *
     * Methods to Manage History
     *
     */

    public JSONObject getHistoryObject(History mHistory) {
        JSONObject mJsonObject = new JSONObject();
        if (mHistory != null) {
            try {
                mJsonObject.put("history_query", mHistory.getQuery());
                mJsonObject.put("history_date", mHistory.getDate());
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return mJsonObject;
    }

    public List<JSONObject> getHistoryObjects(List<History> mHistoryList) {
        List<JSONObject> mJsonObjects = new ArrayList<>();
        for (History mHistory : mHistoryList) {
            mJsonObjects.add(getHistoryObject(mHistory));
        }
        return mJsonObjects;
    }

    public List<History> getHistoryFromJsonArray(JSONArray mJsonArray) {
        List<History> mHistoryList = new ArrayList<>();
        try {
            for (int i = 0; i < mJsonArray.length(); i++) {
                JSONObject mJsonObject = mJsonArray.getJSONObject(i);
                History mHistory = new History();
                mHistory.setQuery(mJsonObject.getString("history_query"));
                mHistory.setDate(mJsonObject.getString("history_date"));
                mHistoryList.add(mHistory);
            }
            return mHistoryList;
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            return new ArrayList<>();
        }
    }

    /*
     *
     * Methods to Update History from JSON
     *
     */

    public void removeHistoryFromJson(int position) {
        JSONArray mJsonArray = readJsonArrayFromFile(JSON_HISTORY);
        mJsonArray.remove(position);
        writeJsonToFile(JSON_HISTORY, mJsonArray);
    }

    public void addSingleHistory(History mHistory) {
        JSONObject mJsonObject = getHistoryObject(mHistory);
        JSONArray mJsonArray;
        if (isJsonAvailable(JSON_HISTORY))
            mJsonArray = readJsonArrayFromFile(JSON_HISTORY);
        else
            mJsonArray = new JSONArray();
        mJsonArray.put(mJsonObject);
        writeJsonToFile(JSON_HISTORY, mJsonArray);
    }

    public void addSingleApp(App mApp) {
        JSONObject mJsonObject = getAppObject(mApp);
        JSONArray mJsonArray;
        if (isJsonAvailable(JSON_APP_HISTORY))
            mJsonArray = readJsonArrayFromFile(JSON_APP_HISTORY);
        else
            mJsonArray = new JSONArray();
        mJsonArray.put(mJsonObject);
        writeJsonToFile(JSON_APP_HISTORY, mJsonArray);
    }

}
