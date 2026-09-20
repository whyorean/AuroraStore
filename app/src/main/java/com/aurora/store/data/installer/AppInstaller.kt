/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-FileCopyrightText: 2023 grrfe <grrfe@420blaze.it>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.installer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import com.aurora.Constants.PACKAGE_NAME_GMS
import com.aurora.extensions.TAG
import com.aurora.extensions.getUpdateOwnerPackageNameCompat
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isPAndAbove
import com.aurora.extensions.isSAndAbove
import com.aurora.extensions.toast
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.installer.base.IInstaller
import com.aurora.store.data.model.Installer
import com.aurora.store.data.model.InstallerInfo
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.PackageUtil.hasMicroGCompanion
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_INSTALLER_ID
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import rikka.sui.Sui

@Singleton
class AppInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionInstaller: SessionInstaller,
    private val nativeInstaller: NativeInstaller,
    private val rootInstaller: RootInstaller,
    private val serviceInstaller: ServiceInstaller,
    private val amInstaller: AMInstaller,
    private val shizukuInstaller: ShizukuInstaller,
    private val microGInstaller: MicroGInstaller
) {

    companion object {
        const val ACTION_INSTALL_STATUS =
            "com.aurora.store.data.installer.AppInstaller.INSTALL_STATUS"

        const val EXTRA_PACKAGE_NAME =
            "com.aurora.store.data.installer.AppInstaller.EXTRA_PACKAGE_NAME"
        const val EXTRA_VERSION_CODE =
            "com.aurora.store.data.installer.AppInstaller.EXTRA_VERSION_CODE"
        const val EXTRA_DISPLAY_NAME =
            "com.aurora.store.data.installer.AppInstaller.EXTRA_DISPLAY_NAME"

        fun getCurrentInstaller(context: Context): Installer =
            Installer.entries[Preferences.getInteger(context, PREFERENCE_INSTALLER_ID)]

        fun getAvailableInstallersInfo(context: Context): List<InstallerInfo> = listOfNotNull(
            SessionInstaller.installerInfo,
            NativeInstaller.installerInfo,
            if (hasRootAccess()) RootInstaller.installerInfo else null,
            if (hasAuroraService(context)) ServiceInstaller.installerInfo else null,
            if (hasAppManager(context)) AMInstaller.installerInfo else null,
            if (hasShizukuOrSui(context)) {
                // Name whichever fork is serving it, rather than crediting them all to Shizuku
                ShizukuInstaller.installerInfo.copy(
                    provider = ShizukuInstaller.getProviderPackage(context)
                        ?.let { PackageUtil.getPackageLabel(context, it) }
                )
            } else {
                null
            },
            if (hasMicroGCompanion(context)) MicroGInstaller.installerInfo else null
        )

        /**
         * Checks if the given package can be silently installed
         * @param context [Context]
         * @param packageName Package to silently install
         */
        fun canInstallSilently(context: Context, packageName: String, targetSdk: Int): Boolean {
            return when (getCurrentInstaller(context)) {
                Installer.SESSION -> {
                    // Silent install cannot be done on initial install and below A12
                    if (!PackageUtil.isInstalled(context, packageName) || !isSAndAbove) return false

                    // We cannot do silent updates if we are not the update owner
                    if (context.packageManager.getUpdateOwnerPackageNameCompat(packageName) !=
                        BuildConfig.APPLICATION_ID
                    ) {
                        return false
                    }

                    // Ensure app being installed satisfies Android's requirement for targetSdk level
                    when (Build.VERSION.SDK_INT) {
                        Build.VERSION_CODES.BAKLAVA -> {
                            targetSdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                        }

                        Build.VERSION_CODES.VANILLA_ICE_CREAM -> {
                            targetSdk >= Build.VERSION_CODES.TIRAMISU
                        }

                        Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> targetSdk >= Build.VERSION_CODES.S

                        Build.VERSION_CODES.TIRAMISU -> targetSdk >= Build.VERSION_CODES.R

                        Build.VERSION_CODES.S -> targetSdk >= Build.VERSION_CODES.Q

                        else -> false // Only Android version above 12 can silently update apps
                    }
                }

                // Native installer requires user interaction
                Installer.NATIVE -> false

                Installer.ROOT -> hasRootAccess()

                Installer.SERVICE -> hasAuroraService(context)

                // We cannot check if AppManager has ability to auto-update
                Installer.AM -> false

                Installer.SHIZUKU ->
                    isOAndAbove && hasShizukuOrSui(context) && hasShizukuPerm(context)

                Installer.MICROG -> false
            }
        }

        fun hasRootAccess(): Boolean = Shell.getShell().isRoot

        fun hasAuroraService(context: Context): Boolean = try {
            val packageInfo = PackageUtil.getPackageInfo(
                context,
                ServiceInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME
            )
            val version = PackageInfoCompat.getLongVersionCode(packageInfo)

            packageInfo.applicationInfo!!.enabled && version >= 9
        } catch (_: Exception) {
            false
        }

        // TODO: Use microG's proposed API instead of relying on a hardcoded metadata flag that can be misleading (e.g. user can enable the installer from UI)
        fun hasMicroGInstaller(context: Context): Boolean {
            if (!PackageUtil.hasMicroGCompanion(context)) return false
            return try {
                val metadata = PackageUtil.getPackageInfo(
                    context,
                    PACKAGE_NAME_GMS,
                    PackageManager.GET_META_DATA
                ).applicationInfo?.metaData

                metadata?.getBoolean("org.microg.gms.settings.vending_apps_install", false) ?: false
            } catch (_: Exception) {
                false
            }
        }

        fun hasAppManager(context: Context): Boolean =
            PackageUtil.isInstalled(context, AMInstaller.AM_PACKAGE_NAME) or
                PackageUtil.isInstalled(context, AMInstaller.AM_DEBUG_PACKAGE_NAME)

        /**
         * Whether anything here can serve the Shizuku installer — the official app, any fork of
         * it, or Sui. Answers "installed", not "running", so a server that is present but not
         * started keeps the installer visible instead of the option quietly disappearing.
         *
         * A live binder counts on its own: forks with a stealth mode reinstall themselves under a
         * random package name and leave a permission-less stub behind, so there may be no package
         * left to find while the server keeps serving.
         */
        fun hasShizukuOrSui(context: Context): Boolean = isOAndAbove &&
            (
                Shizuku.pingBinder() ||
                    ShizukuInstaller.getProviderPackage(context) != null ||
                    Sui.isSui()
                )

        // Shizuku.checkSelfPermission() throws when the binder is not alive (Shizuku
        // disabled/not running), so guard on pingBinder() and swallow any failure to let
        // callers fall back gracefully instead of crashing.
        /**
         * Whether we hold the Shizuku permission, asking the platform as well as Shizuku itself:
         * Shizuku's own answer is per-client state settled at attach time, and a server that
         * grants without pushing the result back leaves it stale for the rest of the process.
         *
         * checkSelfPermission() throws when the binder is not alive, hence the pingBinder guard.
         */
        fun hasShizukuPerm(context: Context): Boolean = try {
            Shizuku.pingBinder() &&
                (
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, ShizukuProvider.PERMISSION) ==
                        PackageManager.PERMISSION_GRANTED
                    )
        } catch (_: Exception) {
            false
        }

        fun uninstall(context: Context, packageName: String) {
            val intent = Intent().apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (isPAndAbove) {
                    action = Intent.ACTION_DELETE
                } else {
                    @Suppress("DEPRECATION")
                    action = Intent.ACTION_UNINSTALL_PACKAGE
                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                }
            }
            context.startActivity(intent)
        }
    }

    private val defaultInstaller: IInstaller
        get() = sessionInstaller

    fun getMicroGInstaller(): IInstaller = microGInstaller

    /**
     * Returns the installer for the user's chosen mode, transparently falling back to the
     * default (session) installer when that mode is currently unavailable, e.g. the chosen
     * Shizuku installer when Shizuku is disabled.
     * @param notifyOnFallback Whether to inform the user via a toast when a fallback happens
     */
    fun getPreferredInstaller(notifyOnFallback: Boolean = false): IInstaller {
        val selected = getCurrentInstaller(context)
        val installer = when (selected) {
            Installer.SESSION -> sessionInstaller

            Installer.NATIVE -> nativeInstaller

            Installer.ROOT -> if (hasRootAccess()) rootInstaller else defaultInstaller

            Installer.SERVICE -> if (hasAuroraService(context)) {
                serviceInstaller
            } else {
                defaultInstaller
            }

            Installer.AM -> if (hasAppManager(context)) amInstaller else defaultInstaller

            Installer.SHIZUKU -> if (hasShizukuOrSui(context) && hasShizukuPerm(context)) {
                shizukuInstaller
            } else {
                defaultInstaller
            }

            Installer.MICROG -> if (hasMicroGCompanion(context)) {
                microGInstaller
            } else {
                defaultInstaller
            }
        }

        if (selected != Installer.SESSION && installer === defaultInstaller) {
            Log.i(TAG, "$selected installer unavailable, falling back to session installer")
            if (notifyOnFallback) context.toast(R.string.installer_fallback_session)
        }

        return installer
    }
}
