package com.jmods.installer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidPackageInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) : AppInstaller {

    override fun install(packageName: String, apkUri: String): Flow<InstallStatus> = callbackFlow {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(packageName)

        val sessionId = installer.createSession(params)
        val session = installer.openSession(sessionId)

        val apkFile = File(apkUri)
        val inputStream = FileInputStream(apkFile)
        val outputStream = session.openWrite("package_install", 0, apkFile.length())

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        session.fsync(outputStream)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val status = intent?.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                when (status) {
                    PackageInstaller.STATUS_SUCCESS -> {
                        trySend(InstallStatus.Success)
                        close()
                    }
                    else -> {
                        val msg = intent?.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Unknown error"
                        trySend(InstallStatus.Failure(msg))
                        close()
                    }
                }
            }
        }

        val action = "com.jmods.INSTALL_COMPLETE"
        context.registerReceiver(receiver, IntentFilter(action))

        val intent = Intent(action)
        val pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        session.commit(pendingIntent.intentSender)
        session.close()

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {}
        }
    }

    override fun uninstall(packageName: String): Flow<InstallStatus> = callbackFlow {
        val installer = context.packageManager.packageInstaller

        val action = "com.jmods.UNINSTALL_COMPLETE"
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val status = intent?.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                if (status == PackageInstaller.STATUS_SUCCESS) {
                    trySend(InstallStatus.Success)
                } else {
                    trySend(InstallStatus.Failure("Uninstall failed"))
                }
                close()
            }
        }

        context.registerReceiver(receiver, IntentFilter(action))
        val intent = Intent(action)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        installer.uninstall(packageName, pendingIntent.intentSender)

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {}
        }
    }
}
