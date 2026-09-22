/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.providers

import android.content.Context
import android.util.Log
import com.aurora.extensions.TAG
import com.aurora.store.BuildConfig
import com.aurora.store.util.PathUtil
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties
import java.util.jar.JarEntry
import java.util.jar.JarFile
import javax.inject.Singleton

/**
 * Provider class to work with device spoof configs imported by users & shipped by GPlayAPI library
 *
 * Do not use this class directly. Consider using [SpoofProvider] instead.
 */
@Singleton
open class SpoofDeviceProvider(private val context: Context) {

    companion object {
        private const val SUFFIX = ".properties"
    }

    val availableDeviceProperties: List<Properties>
        get() {
            val propertiesList: MutableList<Properties> = ArrayList()
            propertiesList.addAll(spoofDevicesFromApk)
            propertiesList.addAll(spoofDevicesFromUser)
            propertiesList.sortBy { it.getProperty("UserReadableName") }
            return propertiesList.distinctBy { it.getProperty("Build.PRODUCT") }
        }

    private val spoofDevicesFromApk: List<Properties>
        get() {
            val jarFile = apkAsJar
            val propertiesList: MutableList<Properties> = ArrayList()
            if (null == jarFile) {
                return propertiesList
            }
            val entries = jarFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (filenameValid(entry.name)) {
                    val properties = getProperties(jarFile, entry)
                    if (properties.getProperty("UserReadableName") != null) {
                        propertiesList.add(properties)
                    }
                }
            }
            return propertiesList
        }

    private val spoofDevicesFromUser: List<Properties>
        get() {
            val deviceNames: MutableList<Properties> = ArrayList()
            val defaultDir = PathUtil.getSpoofDirectory(context)
            val files = defaultDir.listFiles()
            if (defaultDir.exists() && files != null) {
                for (file in files) {
                    if (!file.isFile || !filenameValid(file.name)) {
                        continue
                    }
                    deviceNames.add(getProperties(file))
                }
            }
            return deviceNames
        }

    private fun getProperties(jarFile: JarFile, entry: JarEntry): Properties {
        val properties = Properties()
        try {
            properties.load(jarFile.getInputStream(entry))
            properties.setProperty("CONFIG_NAME", entry.name)
        } catch (exception: IOException) {
            Log.e(TAG, "Could not read ${entry.name}", exception)
        }
        return properties
    }

    private fun getProperties(file: File): Properties {
        val properties = Properties()
        try {
            properties.load(BufferedInputStream(FileInputStream(file)))
            properties.setProperty("CONFIG_NAME", file.name)
        } catch (exception: IOException) {
            Log.e(TAG, "Could not read ${file.name}", exception)
        }
        return properties
    }

    private val apkAsJar: JarFile?
        get() {
            val file = apkFile
            try {
                if (file != null && file.exists()) {
                    return JarFile(file)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Could not open Aurora Store apk as a jar file")
            }
            return null
        }

    private val apkFile: File?
        get() {
            try {
                val sourceDir: String = context.packageManager.getApplicationInfo(
                    BuildConfig.APPLICATION_ID,
                    0
                ).sourceDir

                if (sourceDir.isNotEmpty()) {
                    return File(sourceDir)
                }
            } catch (ignored: Exception) {
            }
            return null
        }

    private fun filenameValid(filename: String): Boolean = filename.endsWith(SUFFIX)
}
