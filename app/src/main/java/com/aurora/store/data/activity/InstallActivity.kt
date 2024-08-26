package com.aurora.store.data.activity

import android.content.pm.PackageInstaller.SessionCallback
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import com.aurora.Constants
import com.aurora.store.data.installer.SessionInstaller
import com.aurora.store.data.room.download.Download
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InstallActivity : AppCompatActivity() {

    private val TAG = InstallActivity::class.java.simpleName

    @Inject
    lateinit var sessionInstaller: SessionInstaller

    private lateinit var sessionCallback: SessionCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val download =
            IntentCompat.getParcelableExtra(intent, Constants.PARCEL_DOWNLOAD, Download::class.java)

        if (download != null) {
            sessionCallback = object : SessionCallback() {
                override fun onCreated(sessionId: Int) {}

                override fun onBadgingChanged(sessionId: Int) {}

                override fun onActiveChanged(sessionId: Int, active: Boolean) {}

                override fun onProgressChanged(sessionId: Int, progress: Float) {}

                override fun onFinished(sessionId: Int, success: Boolean) {
                    if (sessionInstaller.currentSessionId == sessionId) {
                        Log.i(TAG, "Install finished with status code: $success")
                        finish()
                    }
                }
            }
            packageManager.packageInstaller.registerSessionCallback(sessionCallback)
            install(download)
        } else {
            Log.e(TAG, "InstallActivity triggered without a valid download, bailing out!")
            finish()
        }
    }

    override fun onDestroy() {
        packageManager.packageInstaller.unregisterSessionCallback(sessionCallback)
        super.onDestroy()
    }

    private fun install(download: Download) {
        try {
            sessionInstaller.install(download)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to install $packageName")
            finish()
        }
    }
}
