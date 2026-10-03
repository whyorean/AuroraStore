/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.providers

import android.content.Context
import android.util.Log
import com.aurora.extensions.TAG
import com.aurora.gplayapi.device.DeviceRegistry
import com.aurora.store.util.PathUtil
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties
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
            propertiesList.addAll(spoofDevicesFromLibrary)
            propertiesList.addAll(spoofDevicesFromUser)
            propertiesList.sortBy { it.getProperty("UserReadableName") }
            return propertiesList.distinctBy { it.getProperty("Build.PRODUCT") }
        }

    private val spoofDevicesFromLibrary: List<Properties>
        get() = DeviceRegistry.available().map { it.toProperties() }

    /**
     * A user-imported `.properties` file predates gplayapi v4's validation (33 required keys,
     * 11 numeric) - v3 never checked at all, so an already-imported config can fail it now.
     * Skipped and logged rather than surfaced, so one bad file cannot take down the whole
     * device list; the live import path (`SpoofViewModel.importDeviceSpoof`) is where a user
     * importing a config for the first time hears about it.
     */
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
                    getValidatedProperties(file)?.let { deviceNames.add(it) }
                }
            }
            return deviceNames
        }

    private fun getValidatedProperties(file: File): Properties? {
        val raw = Properties()
        return try {
            BufferedInputStream(FileInputStream(file)).use { raw.load(it) }
            val profile = DeviceRegistry.parse(file.name, raw)
            profile.toProperties().apply { setProperty("CONFIG_NAME", file.name) }
        } catch (exception: IOException) {
            Log.e(TAG, "Could not read ${file.name}", exception)
            null
        } catch (exception: IllegalArgumentException) {
            Log.e(TAG, "Ignoring invalid imported device config ${file.name}", exception)
            null
        }
    }

    private fun filenameValid(filename: String): Boolean = filename.endsWith(SUFFIX)
}
