package com.aurora.store.data.providers

import android.content.Context
import com.aurora.gplayapi.data.models.App
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import java.io.IOException

@Singleton
class WhitelistProvider @Inject constructor(
    private val json: Json,
    @ApplicationContext private val context: Context
) {
    private val whitelist: Set<String> by lazy {
        try {
            val jsonString = context.assets.open("whitelist.json").bufferedReader().use { it.readText() }
            json.decodeFromString<Set<String>>(jsonString)
        } catch (e: IOException) {
            emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun isWhitelisted(packageName: String): Boolean {
        if (whitelist.isEmpty()) return true
        return whitelist.contains(packageName)
    }

    fun filterApps(apps: List<App>): List<App> {
        if (whitelist.isEmpty()) return apps
        return apps.filter { isWhitelisted(it.packageName) }
    }
}
