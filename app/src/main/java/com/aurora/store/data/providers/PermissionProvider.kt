package com.aurora.store.data.providers

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.aurora.extensions.checkManifestPermission
import com.aurora.extensions.isDomainVerified
import com.aurora.extensions.isExternalStorageAccessible
import com.aurora.extensions.isIgnoringBatteryOptimizations
import com.aurora.extensions.isMAndAbove
import com.aurora.extensions.isTAndAbove
import com.aurora.extensions.toast
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.model.PermissionType
import com.aurora.store.util.PackageUtil

class PermissionProvider(private val fragment: Fragment) :
    ActivityResultCallback<ActivityResult> {

    private val TAG = PermissionProvider::class.java.simpleName

    private val context: Context
        get() = fragment.requireContext()

    private var permissionRequested: PermissionType? = null
    private var permissionCallback: ((Boolean) -> Unit) = {}

    private val intentLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        this
    )

    private val permissionLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionCallback(it)
        }

    override fun onActivityResult(result: ActivityResult) {
        permissionRequested?.let { permissionCallback(isGranted(it)) }
    }

    fun request(permissionType: PermissionType, callback: (Boolean) -> Unit = {}) {
        permissionRequested = permissionType
        permissionCallback = callback

        try {
            when (permissionType) {
                PermissionType.EXTERNAL_STORAGE -> {
                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }

                PermissionType.POST_NOTIFICATIONS -> {
                    if (isTAndAbove) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                else -> {
                    val intent = knownPermissions()[permissionType] ?: return

                    if (permissionType == PermissionType.STORAGE_MANAGER) {
                        if (!isGranted(PermissionType.INSTALL_UNKNOWN_APPS)) {
                            context.toast(R.string.toast_permission_installer_required)
                        } else {
                            /**
                             * I don't know why, but for storage manager permission on Android 11 & 12,
                             * we need to request both permissions otherwise the permission is not granted,
                             * even though OS says it is granted.
                             */
                            ActivityCompat.requestPermissions(
                                fragment.requireActivity(),
                                arrayOf(
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ),
                                1
                            )

                            // TODO: Verify & remove this after testing
                            context.toast(R.string.toast_permission_esm_restart)

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
            PermissionType.STORAGE_MANAGER -> {
                context.isExternalStorageAccessible()
            }

            PermissionType.INSTALL_UNKNOWN_APPS -> PackageUtil.canRequestPackageInstalls(context)

            PermissionType.POST_NOTIFICATIONS -> {
                if (isTAndAbove) {
                    context.checkManifestPermission(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    true
                }
            }

            PermissionType.DOZE_WHITELIST -> {
                if (isMAndAbove) context.isIgnoringBatteryOptimizations() else true
            }

            PermissionType.APP_LINKS -> context.isDomainVerified("play.google.com") &&
                    context.isDomainVerified("market.android.com")
        }
    }

    fun unregister() {
        intentLauncher.unregister()
        permissionLauncher.unregister()
    }

    @SuppressLint("InlinedApi")
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
}
