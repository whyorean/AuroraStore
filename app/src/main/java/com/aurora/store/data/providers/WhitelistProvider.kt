package com.aurora.store.data.providers

import android.content.Context
import com.aurora.gplayapi.data.models.App
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.File
import java.io.IOException

@Singleton
class WhitelistProvider @Inject constructor(
    private val json: Json,
    @ApplicationContext private val context: Context
) {
    private var cachedWhitelist: Set<String>? = null

    private fun getWhitelist(): Set<String> {
        cachedWhitelist?.let { return it }

        val remoteFile = File(context.filesDir, "whitelist.json")
        val whitelist = if (remoteFile.exists()) {
            try {
                val jsonString = remoteFile.readText()
                json.decodeFromString<Set<String>>(jsonString)
            } catch (e: Exception) {
                loadFromAssets()
            }
        } else {
            loadFromAssets()
        }
        cachedWhitelist = whitelist
        return whitelist
    }

    private fun loadFromAssets(): Set<String> {
        return try {
            val jsonString = context.assets.open("whitelist.json").bufferedReader().use { it.readText() }
            json.decodeFromString<Set<String>>(jsonString)
        } catch (e: IOException) {
            emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun isWhitelisted(packageName: String): Boolean {
        val whitelist = getWhitelist()
        if (whitelist.isEmpty()) return true
        return whitelist.contains(packageName)
    }

    fun filterApps(apps: List<App>): List<App> {
        val whitelist = getWhitelist()
        if (whitelist.isEmpty()) return apps
        return apps.filter { isWhitelisted(it.packageName) }
    }

    fun refresh() {
        cachedWhitelist = null
    }
}
