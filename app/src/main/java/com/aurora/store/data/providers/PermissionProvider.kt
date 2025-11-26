package com.aurora.store.data.providers

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.aurora.extensions.checkManifestPermission
import com.aurora.extensions.isDomainVerified
import com.aurora.extensions.isExternalStorageAccessible
import com.aurora.extensions.isIgnoringBatteryOptimizations
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isRAndAbove
import com.aurora.extensions.isSAndAbove
import com.aurora.extensions.isTAndAbove
import com.aurora.extensions.requiresObbDir
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.model.Permission
import com.aurora.store.data.model.PermissionType
import com.aurora.store.util.PackageUtil

class PermissionProvider(private val fragment: Fragment) :
    ActivityResultCallback<ActivityResult> {

    companion object {

        /**
         * Checks if Aurora Store has permissions to install the given app
         */
        fun isPermittedToInstall(context: Context, app: App): Boolean {
            if (!isGranted(context, PermissionType.INSTALL_UNKNOWN_APPS)) return false
            return when {
                app.fileList.requiresObbDir() -> {
                    return isGranted(context, PermissionType.STORAGE_MANAGER)
                }

                else -> true
            }
        }

        /**
         * Checks whether a known permission has been granted
         */
        fun isGranted(context: Context, permissionType: PermissionType): Boolean {
            return when (permissionType) {
                PermissionType.EXTERNAL_STORAGE,
                PermissionType.STORAGE_MANAGER -> {
                    context.isExternalStorageAccessible()
                }

                PermissionType.INSTALL_UNKNOWN_APPS -> {
                    PackageUtil.canRequestPackageInstalls(context)
                }

                PermissionType.POST_NOTIFICATIONS -> {
                    if (isTAndAbove) {
                        context.checkManifestPermission(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        true
                    }
                }

                PermissionType.DOZE_WHITELIST -> context.isIgnoringBatteryOptimizations()

                PermissionType.APP_LINKS -> context.isDomainVerified("play.google.com") &&
                        context.isDomainVerified("market.android.com")
            }
        }

        /**
         * Returns all known permissions that can be requested by Aurora Store
         */
        fun getAllKnownPermissions(context: Context): List<Permission> {
            val permissions = mutableListOf(
                Permission(
                    type = PermissionType.INSTALL_UNKNOWN_APPS,
                    title = context.getString(R.string.onboarding_permission_installer),
                    subtitle = if (isOAndAbove) {
                        context.getString(R.string.onboarding_permission_installer_desc)
                    } else {
                        context.getString(R.string.onboarding_permission_installer_legacy_desc)
                    },
                    optional = false,
                    isGranted = isGranted(context, PermissionType.INSTALL_UNKNOWN_APPS)
                ),
                Permission(
                    type = PermissionType.DOZE_WHITELIST,
                    title = context.getString(R.string.onboarding_permission_doze),
                    subtitle = context.getString(R.string.onboarding_permission_doze_desc),
                    optional = true,
                    isGranted = isGranted(context, PermissionType.DOZE_WHITELIST)
                )
            )

            if (isRAndAbove) {
                permissions.add(
                    Permission(
                        type = PermissionType.STORAGE_MANAGER,
                        title = context.getString(R.string.onboarding_permission_esm),
                        subtitle = context.getString(R.string.onboarding_permission_esa_desc),
                        optional = false,
                        isGranted = isGranted(context, PermissionType.STORAGE_MANAGER)
                    )
                )
            } else {
                permissions.add(
                    Permission(
                        type = PermissionType.EXTERNAL_STORAGE,
                        title = context.getString(R.string.onboarding_permission_esa),
                        subtitle = context.getString(R.string.onboarding_permission_esa_desc),
                        optional = false,
                        isGranted = isGranted(context, PermissionType.EXTERNAL_STORAGE)
                    )
                )
            }

            if (isTAndAbove) {
                permissions.add(
                    Permission(
                        type = PermissionType.POST_NOTIFICATIONS,
                        title = context.getString(R.string.onboarding_permission_notifications),
                        subtitle = context.getString(R.string.onboarding_permission_notifications_desc),
                        optional = true,
                        isGranted = isGranted(context, PermissionType.POST_NOTIFICATIONS)
                    )
                )
            }

            if (isSAndAbove) {
                permissions.add(
                    Permission(
                        type = PermissionType.APP_LINKS,
                        title = context.getString(R.string.app_links_title),
                        subtitle = context.getString(R.string.app_links_desc),
                        optional = true,
                        isGranted = isGranted(context, PermissionType.APP_LINKS)
                    ),
                )
            }

            return permissions
        }
    }

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
        permissionRequested?.let { permissionCallback(isGranted(context, it)) }
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

                PermissionType.STORAGE_MANAGER -> {
                    if (!isGranted(context, PermissionType.INSTALL_UNKNOWN_APPS)) {
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

                        val intent = knownPermissions()[permissionType] ?: return
                        intentLauncher.launch(intent)
                    }
                }

                else -> {
                    val intent = knownPermissions()[permissionType] ?: return
                    intentLauncher.launch(intent)
                }
            }
        } catch (activityNotFoundException: ActivityNotFoundException) {
            Log.e(TAG, "Activity not found for $permissionType", activityNotFoundException)
        } catch (exception: Exception) {
            Log.e(TAG, "Error requesting permission", exception)
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
                "package:${BuildConfig.APPLICATION_ID}".toUri()
            ),
            PermissionType.APP_LINKS to Intent(
                ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                "package:${BuildConfig.APPLICATION_ID}".toUri()
            )
        )
    }
}
