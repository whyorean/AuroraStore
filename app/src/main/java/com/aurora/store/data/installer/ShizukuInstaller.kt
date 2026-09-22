/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.installer

import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import com.aurora.extensions.TAG
import com.aurora.store.R
import com.aurora.store.data.installer.base.InstallerBase
import com.aurora.store.data.model.Installer
import com.aurora.store.data.model.InstallerInfo
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.PackageUtil.isSharedLibraryInstalled
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

/**
 * Installs through `pm`, run by the Shizuku server as its own uid.
 *
 * [android.content.pm.PackageInstaller]'s privileged constructors are hidden members the platform
 * refuses to link at this targetSdk, so a session cannot be rebuilt out of them however willing
 * the server is. `pm` is the platform's own front end to the same calls, and is already how
 * [RootInstaller] installs.
 */
@Singleton
@RequiresApi(Build.VERSION_CODES.O)
class ShizukuInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) : InstallerBase(context) {

    companion object {
        const val PLAY_PACKAGE_NAME = "com.android.vending"

        private const val STREAM_TIMEOUT_MS = 5_000L

        val installerInfo: InstallerInfo
            get() = InstallerInfo(
                id = 5,
                installer = Installer.SHIZUKU,
                installerPackageNames = listOf(PLAY_PACKAGE_NAME),
                title = R.string.pref_install_mode_shizuku,
                subtitle = R.string.shizuku_installer_subtitle,
                description = R.string.shizuku_installer_desc
            )

        /**
         * Package of the app serving Shizuku — the official app, or any fork of it.
         *
         * Resolved from whichever package *defines* [ShizukuProvider.PERMISSION] rather than from
         * known package names, because that permission is the discovery contract: a server finds
         * its clients by scanning for the apps requesting it. Null covers Sui, which ships the
         * server in a Magisk module and so has no manager app to name.
         */
        fun getProviderPackage(context: Context): String? = runCatching {
            context.packageManager.getPermissionInfo(ShizukuProvider.PERMISSION, 0).packageName
        }.getOrNull()

        /** The session id out of `Success: created install session [1234]`. */
        internal fun parseSessionId(output: String): Int? =
            Regex("""\[(\d+)]""").find(output)?.groupValues?.get(1)?.toIntOrNull()
    }

    override fun install(download: Download) {
        super.install(download)

        if (isAlreadyQueued(download.packageName)) {
            Log.i(TAG, "${download.packageName} already queued")
        } else {
            download.sharedLibs.forEach {
                // Shared library packages cannot be updated
                if (!isSharedLibraryInstalled(context, it.packageName, it.versionCode)) {
                    install(download.packageName, download.versionCode, it.packageName)
                }
            }
            install(download.packageName, download.versionCode)
        }
    }

    private fun install(packageName: String, versionCode: Long, sharedLibPkgName: String = "") {
        val target = sharedLibPkgName.ifBlank { packageName }
        Log.i(TAG, "Received session install request for $target")

        val files = getFiles(packageName, versionCode, sharedLibPkgName)
        if (files.isEmpty()) {
            fail(packageName, context.getString(R.string.installer_status_failure_session))
            return
        }

        val created = exec(
            listOf(
                "pm",
                "install-create",
                "-i",
                PLAY_PACKAGE_NAME,
                "--user",
                (Process.myUid() / 100_000).toString(),
                "-r",
                "-S",
                files.sumOf { it.length() }.toString()
            )
        ).getOrElse { error ->
            fail(packageName, error.localizedMessage, error.stackTraceToString())
            return
        }

        val sessionId = parseSessionId(created)
        if (sessionId == null) {
            fail(packageName, context.getString(R.string.installer_status_failure_session), created)
            return
        }

        Log.i(TAG, "Writing splits to session $sessionId for $target")
        files.forEach { file ->
            // Streamed into stdin, not named as a path: a server started from adb runs as shell
            // and cannot read Aurora's own data directory.
            val command = listOf(
                "pm",
                "install-write",
                "-S",
                file.length().toString(),
                sessionId.toString(),
                file.name,
                "-"
            )
            exec(command, input = file).onFailure { error ->
                exec(listOf("pm", "install-abandon", sessionId.toString()))
                fail(packageName, error.localizedMessage, error.stackTraceToString())
                return
            }
        }

        Log.i(TAG, "Committing session $sessionId for $target")
        exec(listOf("pm", "install-commit", sessionId.toString()))
            .onSuccess {
                // Installation is not yet finished if this is a shared library
                if (packageName == download?.packageName) onInstallationSuccess()
            }
            .onFailure { error ->
                fail(packageName, error.localizedMessage, error.stackTraceToString())
            }
    }

    private fun fail(packageName: String, error: String?, extra: String? = null) {
        removeFromInstallQueue(packageName)
        postError(
            packageName,
            error ?: context.getString(R.string.installer_status_failure),
            extra ?: context.getString(R.string.installer_shizuku_unavailable)
        )
    }

    /**
     * Runs [command] as the Shizuku server's own uid. `newProcess` hands the argv straight to
     * `Runtime.exec`, so nothing here needs quoting.
     *
     * @param input streamed into the command's stdin
     */
    private fun exec(command: List<String>, input: File? = null): Result<String> = runCatching {
        val service = IShizukuService.Stub.asInterface(Shizuku.getBinder())
            ?: throw IOException(context.getString(R.string.installer_shizuku_unavailable))
        val process = service.newProcess(command.toTypedArray(), null, null)

        // Written before stdout is drained: `pm` reads the package to its end before it answers,
        // so waiting on its output first deadlocks against it waiting on the input.
        if (input != null) {
            ParcelFileDescriptor.AutoCloseOutputStream(process.outputStream).use { output ->
                input.inputStream().use { it.copyTo(output) }
            }
        }

        // On its own thread: whichever stream is read second can fill its pipe buffer first, and
        // a subprocess blocked writing it never closes the one being read.
        val errText = StringBuilder()
        val reader = Thread {
            runCatching {
                ParcelFileDescriptor.AutoCloseInputStream(process.errorStream)
                    .bufferedReader().use { errText.append(it.readText()) }
            }
        }
        reader.start()

        val out = ParcelFileDescriptor.AutoCloseInputStream(process.inputStream)
            .bufferedReader().use { it.readText() }.trim()
        val exitCode = process.waitFor()
        reader.join(STREAM_TIMEOUT_MS)
        val err = errText.toString().trim()

        if (exitCode != 0) throw IOException(err.ifBlank { out.ifBlank { "pm exited $exitCode" } })
        out.ifBlank { err }
    }
}
