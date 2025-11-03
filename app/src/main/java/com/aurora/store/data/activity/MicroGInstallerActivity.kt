package com.aurora.store.data.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.content.FileProvider
import com.aurora.Constants.PACKAGE_NAME_PLAY_STORE
import com.aurora.extensions.isTAndAbove
import com.aurora.store.BuildConfig
import com.aurora.store.data.installer.MicroGInstaller.Companion.buildMicroGInstallIntent
import java.io.File

class MicroGInstallerActivity : Activity() {

    companion object {
        private val TAG = MicroGInstallerActivity::class.java.simpleName
        private const val REQUEST_CODE = 1001
        const val EXTRA_FILES = "extra_files"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"

        fun launch(context: Context, packageName: String, files: List<File>) {
            val uris = files.map { file ->
                val uri = FileProvider.getUriForFile(
                    context,
                    "${BuildConfig.APPLICATION_ID}.fileProvider",
                    file
                )

                context.grantUriPermission(
                    PACKAGE_NAME_PLAY_STORE,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                uri
            }

            val intent = Intent(context, MicroGInstallerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_FILES, ArrayList(uris))
            }

            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val files: ArrayList<Uri>? = if (isTAndAbove) {
            intent.getParcelableArrayListExtra(EXTRA_FILES, Uri::class.java)
        } else {
            intent.getParcelableArrayListExtra(EXTRA_FILES)
        }

        if (files.isNullOrEmpty()) {
            Log.e(TAG, "No files provided, cannot proceed with MicroG installation")
            return finish()
        }

        startActivityForResult(
            buildMicroGInstallIntent(files),
            REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // TODO: Handle result if needed
        finish()
    }
}