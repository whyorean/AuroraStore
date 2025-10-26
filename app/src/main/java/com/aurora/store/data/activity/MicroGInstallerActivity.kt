package com.aurora.store.data.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.aurora.extensions.isTAndAbove
import com.aurora.store.data.installer.MicroGInstaller.Companion.buildMicroGInstallIntent
import java.io.File

class MicroGInstallerActivity : Activity() {

    companion object {
        private val TAG = MicroGInstallerActivity::class.java.simpleName
        private const val REQUEST_INSTALL = 1001
        const val EXTRA_FILES = "extra_files"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"

        fun launch(context: Context, packageName: String, files: List<File>) {
            val intent = Intent(context, MicroGInstallerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_FILES, ArrayList(files))
            }

            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val files = if (isTAndAbove) {
            intent.getSerializableExtra(EXTRA_FILES, ArrayList::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_FILES)
        } as? ArrayList<File>

        if (files.isNullOrEmpty()) {
            Log.e(TAG, "No files provided, cannot proceed with MicroG installation")
            return finish()
        }

        val installIntent = buildMicroGInstallIntent(
            this,
            files
        )

        startActivityForResult(installIntent, REQUEST_INSTALL)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // TODO: Handle result if needed
        finish()
    }
}