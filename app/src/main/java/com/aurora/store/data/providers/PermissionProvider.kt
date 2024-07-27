package com.aurora.store.data.providers

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.aurora.extensions.checkManifestPermission
import com.aurora.extensions.isDomainVerified
import com.aurora.extensions.isExternalStorageAccessible
import com.aurora.extensions.isMAndAbove
import com.aurora.extensions.isOAndAbove
import com.aurora.store.BuildConfig
import com.aurora.store.PermissionType
import com.aurora.store.util.Log
import com.aurora.store.util.PackageUtil

@SuppressLint("NewApi")
class PermissionProvider : ActivityResultCallback<ActivityResult> {
    private var context: Context
    private var intentLauncher: ActivityResultLauncher<Intent>
    private var permissionLauncher: ActivityResultLauncher<String>

    constructor(activity: AppCompatActivity) {
        context = activity
        intentLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            this
        )
        permissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    }

    constructor(fragment: Fragment) {
        context = fragment.requireContext()
        intentLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            this
        )
        permissionLauncher =
            fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    }

    fun unregister() {
        intentLauncher.unregister()
        permissionLauncher.unregister()
    }

    private val permissionMap: Map<PermissionType, Intent> = mapOf(
        PermissionType.STORAGE_MANAGER to PackageUtil.getStorageManagerIntent(safe = true),
        PermissionType.INSTALL_UNKNOWN_APPS to Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${BuildConfig.APPLICATION_ID}")
        ),
        PermissionType.DOZE_WHITELIST to Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${BuildConfig.APPLICATION_ID}")
        ),
        PermissionType.APP_LINKS to Intent(
            ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
            Uri.parse("package:${BuildConfig.APPLICATION_ID}")
        )
    )

    fun requestPermission(permissionType: PermissionType) {
        try {
            when (permissionType) {
                PermissionType.EXTERNAL_STORAGE -> permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                PermissionType.POST_NOTIFICATIONS -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else -> {
                    val intent = permissionMap[permissionType] ?: return
                    intentLauncher.launch(intent)
                }
            }
        } catch (e: ActivityNotFoundException) {
            Log.e("PermissionProvider", "Activity not found for $permissionType")
        }
    }

    fun isPermissionGranted(permissionType: PermissionType): Boolean {
        return when (permissionType) {
            PermissionType.EXTERNAL_STORAGE,
            PermissionType.STORAGE_MANAGER -> context.isExternalStorageAccessible()

            PermissionType.POST_NOTIFICATIONS -> context.checkManifestPermission(Manifest.permission.POST_NOTIFICATIONS)
            PermissionType.INSTALL_UNKNOWN_APPS -> {
                if (isOAndAbove())
                    context.packageManager.canRequestPackageInstalls()
                else
                    true
            }

            PermissionType.DOZE_WHITELIST -> {
                if (isMAndAbove()) {
                    val powerManager =
                        context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)
                } else {
                    true
                }
            }

            PermissionType.APP_LINKS -> context.isDomainVerified("play.google.com") || context.isDomainVerified(
                "market.android.com"
            )
        }
    }

    override fun onActivityResult(result: ActivityResult) {
        // We don't need to do anything here, as we anyway do permission re-checks using @isPermissionGranted
    }
}
