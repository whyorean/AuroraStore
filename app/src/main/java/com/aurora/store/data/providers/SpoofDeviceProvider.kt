/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
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
