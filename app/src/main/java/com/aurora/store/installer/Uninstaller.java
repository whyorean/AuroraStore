package com.aurora.store.installer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import com.aurora.services.IPrivilegedCallback;
import com.aurora.services.IPrivilegedService;
import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.util.Log;
import com.aurora.store.util.PrefUtil;
import com.aurora.store.util.ViewUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class Uninstaller {

    private Context context;

    public Uninstaller(Context context) {
        this.context = context;
    }

    public void uninstall(App app) {
        String prefValue = PrefUtil.getString(context, Constants.PREFERENCE_INSTALLATION_METHOD);
        switch (prefValue) {
            case "0":
            case "2":
                uninstallByPackageManager(app);
                break;
            case "1":
                askUninstall(app);
                break;
            default:
                uninstallByPackageManager(app);
        }
    }

    private void uninstallByServices(App app) {
        ServiceConnection mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                IPrivilegedService service = IPrivilegedService.Stub.asInterface(binder);
                IPrivilegedCallback callback = new IPrivilegedCallback.Stub() {
                    @Override
                    public void handleResult(String packageName, int returnCode) {
                        Log.i("Uninstallation of " + packageName + " complete with code " + returnCode);
                    }
                };
                try {
                    if (!service.hasPrivilegedPermissions()) {
                        Log.d("service.hasPrivilegedPermissions() is false");
                        return;
                    }
                    service.deletePackage(app.getPackageName(), 1, callback);
                } catch (RemoteException e) {
                    Log.e("Connecting to privileged service failed");
                }
            }

            public void onServiceDisconnected(ComponentName name) {
            }
        };
        Intent serviceIntent = new Intent(Constants.PRIVILEGED_EXTENSION_SERVICE_INTENT);
        serviceIntent.setPackage(Constants.PRIVILEGED_EXTENSION_PACKAGE_NAME);
        context.getApplicationContext().bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void uninstallByPackageManager(App app) {
        Uri uri = Uri.fromParts("package", app.getPackageName(), null);
        Intent intent = new Intent();
        intent.setData(uri);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            intent.setAction(Intent.ACTION_DELETE);
        } else {
            intent.setAction(Intent.ACTION_UNINSTALL_PACKAGE);
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private void uninstallByRoot(App app) {
        new AppUninstallerRooted().uninstall(app);
    }

    private void askUninstall(App app) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(app.getDisplayName())
                .setMessage(context.getString(R.string.dialog_uninstall_confirmation))
                .setPositiveButton(context.getString(android.R.string.ok), (dialog, which) -> {
                    uninstallByRoot(app);
                })
                .setNegativeButton(context.getString(android.R.string.cancel), (dialog, which) -> {
                    dialog.dismiss();
                });
        int backGroundColor = ViewUtil.getStyledAttribute(context, android.R.attr.colorBackground);
        builder.setBackground(new ColorDrawable(backGroundColor));
        builder.create();
        builder.show();
    }
}
