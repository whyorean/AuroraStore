package com.aurora.store.data.activity

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.aurora.Constants
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.util.PathUtil
import java.io.File
import kotlin.io.path.pathString

class InstallActivity : Activity() {

    private val TAG = InstallActivity::class.java.simpleName

    private lateinit var packageName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        packageName = intent.extras?.getString(Constants.STRING_APP) ?: String()
        val version = intent.extras?.getInt(Constants.STRING_VERSION)
        if (packageName.isNotBlank() && version != null) {
            try {
                val downloadDir =
                    File(PathUtil.getAppDownloadDir(this, packageName, version).pathString)
                AppInstaller.getInstance(this).getPreferredInstaller()
                    .install(
                        packageName,
                        downloadDir.listFiles()!!.filter { it.path.endsWith(".apk") }
                    )
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to install $packageName")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        finish()
    }
}
