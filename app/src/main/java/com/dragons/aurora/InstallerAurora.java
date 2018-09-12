package com.dragons.aurora;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.aurora.services.IPrivilegedCallback;
import com.aurora.services.IPrivilegedService;
import com.dragons.aurora.model.App;

public class InstallerAurora extends InstallerAbstract {

    public static final String PRIVILEGED_EXTENSION_PACKAGE_NAME = "com.aurora.services";
    public static final int ACTION_INSTALL_REPLACE_EXISTING = 2;
    private static final String PRIVILEGED_EXTENSION_SERVICE_INTENT = "com.aurora.services.IPrivilegedService";

    public InstallerAurora(Context context) {
        super(context);
    }

    private static boolean isExtensionInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(PRIVILEGED_EXTENSION_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isExtensionAvailable(Context context) {
        if (!isExtensionInstalled(context)) {
            return false;
        }
        ServiceConnection serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
            }

            public void onServiceDisconnected(ComponentName name) {
            }
        };
        Intent serviceIntent = new Intent(PRIVILEGED_EXTENSION_SERVICE_INTENT);
        serviceIntent.setPackage(PRIVILEGED_EXTENSION_PACKAGE_NAME);

        try {
            context.getApplicationContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    @Override
    public boolean verify(App app) {
        if (!super.verify(app)) {
            return false;
        }
        return isExtensionAvailable(context);
    }

    @Override
    protected void install(final App app) {
        InstallationState.setInstalling(app.getPackageName());
        ServiceConnection mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                IPrivilegedService service = IPrivilegedService.Stub.asInterface(binder);
                IPrivilegedCallback callback = new IPrivilegedCallback.Stub() {
                    @Override
                    public void handleResult(String packageName, int returnCode) throws RemoteException {
                        Log.i(getClass().getSimpleName(), "Installation of " + packageName + " complete with code " + returnCode);
                        sendBroadcast(packageName, returnCode > 0);
                    }
                };
                try {
                    if (!service.hasPrivilegedPermissions()) {
                        Log.e(getClass().getSimpleName(), "service.hasPrivilegedPermissions() is false");
                        sendBroadcast(app.getPackageName(), false);
                        return;
                    }
                    service.installPackage(
                            Uri.fromFile(Paths.getApkPath(context, app.getPackageName(), app.getVersionCode())),
                            ACTION_INSTALL_REPLACE_EXISTING,
                            BuildConfig.APPLICATION_ID,
                            callback
                    );
                } catch (RemoteException e) {
                    Log.e(getClass().getSimpleName(), "Connecting to privileged service failed");
                    sendBroadcast(app.getPackageName(), false);
                }
            }

            public void onServiceDisconnected(ComponentName name) {
            }
        };

        Intent serviceIntent = new Intent(PRIVILEGED_EXTENSION_SERVICE_INTENT);
        serviceIntent.setPackage(PRIVILEGED_EXTENSION_PACKAGE_NAME);
        context.getApplicationContext().bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }
}
