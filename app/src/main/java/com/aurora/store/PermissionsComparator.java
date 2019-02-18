package com.aurora.store;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.aurora.store.model.App;
import com.aurora.store.utility.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PermissionsComparator {

    private Context context;

    public PermissionsComparator(Context context) {
        this.context = context;
    }

    public boolean isSame(App app) {
        Log.i("Checking %s", app.getPackageName());
        Set<String> oldPermissions = getOldPermissions(app.getPackageName());
        if (null == oldPermissions) {
            return true;
        }
        Set<String> newPermissions = new HashSet<>(app.getPermissions());
        newPermissions.removeAll(oldPermissions);
        Log.i(newPermissions.isEmpty()
                ? app.getPackageName() + " requests no new permissions"
                : app.getPackageName() + " requests new permissions: " + TextUtils.join(", ", newPermissions));
        return newPermissions.isEmpty();
    }

    private Set<String> getOldPermissions(String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            return new HashSet<>(Arrays.asList(
                    null == pi.requestedPermissions
                            ? new String[0]
                            : pi.requestedPermissions
            ));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Package " + packageName + " doesn't seem to be installed");
        }
        return null;
    }
}
