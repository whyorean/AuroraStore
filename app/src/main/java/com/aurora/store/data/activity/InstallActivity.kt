package com.aurora.store.data.activity

import android.content.pm.PackageInstaller
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import com.aurora.Constants
import com.aurora.store.data.installer.SessionInstaller
import com.aurora.store.data.room.download.Download
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InstallActivity : AppCompatActivity() {

    private val TAG = InstallActivity::class.java.simpleName

    private lateinit var sessionInstaller: SessionInstaller

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val download =
            IntentCompat.getParcelableExtra(intent, Constants.PARCEL_DOWNLOAD, Download::class.java)

        if (download != null) {
            val callback = object : PackageInstaller.SessionCallback() {
                override fun onCreated(sessionId: Int) {}

                override fun onBadgingChanged(sessionId: Int) {}

                override fun onActiveChanged(sessionId: Int, active: Boolean) {}

                override fun onProgressChanged(sessionId: Int, progress: Float) {}

                override fun onFinished(sessionId: Int, success: Boolean) {
                    if (sessionInstaller.parentSessionId == sessionId) {
                        Log.i(TAG, "Install finished with status code: $success")
                        finish()
                    }
                }
            }
            packageManager.packageInstaller.registerSessionCallback(callback)
            install(download)
        }
    }

    private fun install(download: Download) {
        sessionInstaller = SessionInstaller(this)
        try {
            sessionInstaller.install(download)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to install $packageName")
            finish()
        }
    }
}
