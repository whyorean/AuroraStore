package com.dragons.aurora.database;

import android.content.Context;
import android.content.pm.PackageInfo;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.aurora.model.App;
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
import java.util.Date;
import java.util.List;

import timber.log.Timber;

public class Jessie {

    public static String JSON_INSTALLED = "INSTALLED";
    public static String JSON_UPDATES = "UPDATES";

    private String TAG = "JESSIE";
    private String DIR = "/Database/";
    private String EXT = ".json";
    private Context mContext;

    /*
     *
     * Constructors
     *
     */

    public Jessie(Context mContext) {
        this.mContext = mContext;
        File mFile = new File(getPath());
        if (!mFile.exists() && !mFile.isDirectory())
            mFile.mkdirs();
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
                FileWriter mFileWriter = new FileWriter(getPath() + filename + EXT);
                mFileWriter.write(mJsonArray.toString());
                mFileWriter.flush();
                mFileWriter.close();
            } catch (IOException e) {
                Timber.e(e.getMessage());
            }
        }
    }

    public JSONArray readJsonArrayFromFile(String JsonName) {
        try {
            File mFile = new File(getPath() + JsonName + EXT);
            FileInputStream mFileInputStream = new FileInputStream(mFile);
            int size = mFileInputStream.available();
            byte[] buffer = new byte[size];
            mFileInputStream.read(buffer);
            mFileInputStream.close();
            String mResponse = new String(buffer, StandardCharsets.UTF_8);
            return new JSONArray(mResponse);
        } catch (IOException | JSONException e) {
            Timber.e(e.getMessage());
            return new JSONArray();
        }
    }

    public boolean isJsonAvailable(String JsonName) {
        return (new File(getPath() + JsonName + EXT).exists());
    }

    public String getPath() {
        return mContext.getFilesDir().getPath() + DIR;
    }

    public Boolean isJasonValid(String JsonName) {
        Date lastModified = getLastModified(JsonName);
        Date currentDate = new Date();
        long diff = currentDate.getTime() - lastModified.getTime();
        int diffHours = (int) (diff / (1000 * 60 * 60));
        int validHours = Integer.parseInt(Prefs.getString(mContext, Aurora.PREFERENCE_DATABASE_VALIDITY));
        return diffHours <= validHours;
    }

    private Date getLastModified(String JsonName) {
        File mFile = new File(getPath() + JsonName);
        if (mFile.exists())
            return new Date(mFile.lastModified());
        else
            return new Date();
    }

    public void removeJson(String JsonName) {
        if (isJsonAvailable(JsonName)) {
            File mFile = new File(getPath() + JsonName + EXT);
            mFile.delete();
        } else Timber.i("File does not exist");
    }

    public void removeDatabase() {
        File mDir = new File(getPath());
        if (mDir.exists() && mDir.isDirectory()) {
            String[] contents = mDir.list();
            for (String content : contents) {
                if (!content.contains("HISTORY"))
                    new File(mDir, content).delete();
            }
        } else Timber.i("Directory does not exist");
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
                mAppObject.put("app_system", mApp.isSystem());
            } catch (JSONException e) {
                Timber.e(e.getMessage());
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
                mApp.setSystem(mJsonObject.getBoolean("app_system"));
                mApps.add(mApp);
            }
            return mApps;
        } catch (JSONException e) {
            Timber.e(e.getMessage());
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
}
