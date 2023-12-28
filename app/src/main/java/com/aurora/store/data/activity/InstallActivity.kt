package com.aurora.store.data.activity

import android.app.Activity
import android.content.pm.PackageInstaller
import android.os.Bundle
import android.util.Log
import com.aurora.Constants
import com.aurora.store.data.installer.SessionInstaller
import com.aurora.store.util.PathUtil

class InstallActivity : Activity() {

    private val TAG = InstallActivity::class.java.simpleName

    private lateinit var sessionInstaller: SessionInstaller

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.extras?.getString(Constants.STRING_APP) ?: String()
        val versionCode = intent.extras?.getInt(Constants.STRING_VERSION)
        if (packageName.isNotBlank() && versionCode != null) {
            val callback = object : PackageInstaller.SessionCallback() {
                override fun onCreated(sessionId: Int) {}

                override fun onBadgingChanged(sessionId: Int) {}

                override fun onActiveChanged(sessionId: Int, active: Boolean) {}

                override fun onProgressChanged(sessionId: Int, progress: Float) {}

                override fun onFinished(sessionId: Int, success: Boolean) {
                    if (sessionInstaller.sessionId == sessionId) {
                        Log.i(TAG, "Install finished with status code: $success")
                        finish()
                    }
                }
            }
            packageManager.packageInstaller.registerSessionCallback(callback)
            install(packageName, versionCode)
        }
    }

    private fun install(packageName: String, versionCode: Int) {
        sessionInstaller = SessionInstaller(this)
        try {
            val files = PathUtil.getAppDownloadDir(this, packageName, versionCode).listFiles()
            sessionInstaller.install(packageName, files!!.filter { it.path.endsWith(".apk") })
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to install $packageName")
            finish()
        }
    }
}
