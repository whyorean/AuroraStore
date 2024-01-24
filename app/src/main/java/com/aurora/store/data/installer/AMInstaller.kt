package com.aurora.store.data.installer

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.Log
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AMInstaller(context: Context) : InstallerBase(context) {
    companion object {
        const val AM_PACKAGE_NAME = "io.github.muntashirakon.AppManager"
        const val AM_DEBUG_PACKAGE_NAME = "io.github.muntashirakon.AppManager.debug"
    }

    override fun install(download: Download) {
        if (isAlreadyQueued(download.packageName)) {
            Log.i("${download.packageName} already queued")
        } else {
            Log.i("Received AM install request for ${download.packageName}")
            val fileList =
                getFiles(download.packageName, download.versionCode).map { it.absolutePath }
            when {
                fileList.size == 1 -> {
                    xInstall(File(fileList.first()))
                }
                fileList.size > 1 -> {
                    val apks = zipFile(fileList)
                    xInstall(apks)
                }
                else -> {
                    throw Exception("Invalid data, expecting non empty fileList")
                }
            }
        }
    }

    private fun xInstall(file: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(
            FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".fileProvider",
                file
            ), "application/octet-stream"
        );
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        context.startActivity(intent)
    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun zipFile(files: List<String>): File {
        val outPath = File(files.first()).parentFile.absolutePath + "/bundle.apks"
        val out = ZipOutputStream(BufferedOutputStream(FileOutputStream(outPath)))
        for (file in files) {
            val fi = FileInputStream(file)
            val origin = BufferedInputStream(fi)
            val entry = ZipEntry(file.substring(file.lastIndexOf("/")))
            out.putNextEntry(entry)
            origin.copyTo(out, 1024)
            origin.close()
        }
        out.close()
        return File(outPath)
    }
}
