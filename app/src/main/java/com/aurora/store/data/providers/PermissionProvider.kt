package com.aurora.store.data.providers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import com.aurora.extensions.checkManifestPermission
import com.aurora.extensions.isDomainVerified
import com.aurora.extensions.isExternalStorageAccessible
import com.aurora.extensions.isMAndAbove
import com.aurora.extensions.toast
import com.aurora.store.BuildConfig
import com.aurora.store.PermissionCallback
import com.aurora.store.PermissionType
import com.aurora.store.R
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.PathUtil

@SuppressLint("NewApi")
class PermissionProvider : ActivityResultCallback<ActivityResult> {

    private val TAG = PermissionProvider::class.java.simpleName

    private var context: Context
    private var intentLauncher: ActivityResultLauncher<Intent>
    private var permissionLauncher: ActivityResultLauncher<String>

    private var permissionCallback: PermissionCallback? = null

    constructor(activity: AppCompatActivity, callback: PermissionCallback? = null) {
        this.context = activity
        permissionCallback = callback
        intentLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            this
        )

        permissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                permissionCallback?.invoke(
                    ActivityResult(
                        if (it) Activity.RESULT_OK else Activity.RESULT_CANCELED,
                        null
                    )
                )
            }
    }

    constructor(fragment: Fragment, callback: PermissionCallback? = null) {
        this.context = fragment.requireContext()
        permissionCallback = callback
        intentLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            this
        )
        permissionLauncher =
            fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                permissionCallback?.invoke(
                    ActivityResult(
                        if (it) Activity.RESULT_OK else Activity.RESULT_CANCELED,
                        null
                    )
                )
            }
    }

    private fun knownPermissions(): Map<PermissionType, Intent> {
        return mapOf(
            PermissionType.STORAGE_MANAGER to PackageUtil.getStorageManagerIntent(context),
            PermissionType.INSTALL_UNKNOWN_APPS to PackageUtil.getInstallUnknownAppsIntent(),
            PermissionType.DOZE_WHITELIST to Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${BuildConfig.APPLICATION_ID}")
            ),
            PermissionType.APP_LINKS to Intent(
                ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                Uri.parse("package:${BuildConfig.APPLICATION_ID}")
            )
        )
    }

    fun request(permissionType: PermissionType) {
        try {
            when (permissionType) {
                PermissionType.EXTERNAL_STORAGE -> permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                PermissionType.POST_NOTIFICATIONS -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else -> {
                    val intent = knownPermissions()[permissionType] ?: return

                    if (permissionType == PermissionType.STORAGE_MANAGER) {
                        if (!isGranted(PermissionType.INSTALL_UNKNOWN_APPS)) {
                            context.toast(R.string.toast_permission_installer_required)
                        } else {
                            context.toast(R.string.toast_permission_esm_caution)
                            intentLauncher.launch(intent)
                        }
                    } else {
                        intentLauncher.launch(intent)
                    }
                }
            }
        } catch (activityNotFoundException: ActivityNotFoundException) {
            Log.e(TAG, "Activity not found for $permissionType", activityNotFoundException)
        } catch (exception: Exception) {
            Log.e(TAG, "Error requesting permission", exception)
        }
    }

    fun isGranted(permissionType: PermissionType): Boolean {
        return when (permissionType) {
            PermissionType.EXTERNAL_STORAGE,
            PermissionType.STORAGE_MANAGER -> context.isExternalStorageAccessible() && PathUtil.canReadWriteOBB()

            PermissionType.POST_NOTIFICATIONS -> context.checkManifestPermission(Manifest.permission.POST_NOTIFICATIONS)
            PermissionType.INSTALL_UNKNOWN_APPS -> PackageUtil.canRequestPackageInstalls(context)
            PermissionType.DOZE_WHITELIST -> {
                if (isMAndAbove()) {
                    val powerManager = context.getSystemService<PowerManager>()
                    powerManager?.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID) ?: false
                } else {
                    true
                }
            }

            PermissionType.APP_LINKS -> context.isDomainVerified("play.google.com") && context.isDomainVerified(
                "market.android.com"
            )
        }
    }

    fun unregister() {
        intentLauncher.unregister()
        permissionLauncher.unregister()
    }

    override fun onActivityResult(result: ActivityResult) {
        permissionCallback?.invoke(result)
    }
}
