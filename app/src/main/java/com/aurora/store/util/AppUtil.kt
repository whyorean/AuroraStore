package com.aurora.store.util

import android.content.Context
import android.content.pm.PackageInfo
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.store.AuroraApp
import com.aurora.store.BuildConfig
import com.aurora.store.data.model.SelfUpdate
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.data.room.update.Update
import com.aurora.store.data.room.update.UpdateDao
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AppUtil @Inject constructor(
    private val gson: Gson,
    private val authProvider: AuthProvider,
    private val updateDao: UpdateDao,
    @ApplicationContext private val context: Context
) {

    private val TAG = AppUtil::class.java.simpleName
    private val RELEASE = "release"

    private val isExtendedUpdateEnabled get() = Preferences.getBoolean(
        context, Preferences.PREFERENCE_UPDATES_EXTENDED
    )
    val updates = updateDao.updates()
        .map { list -> list.filter { it.isInstalled(context) } }
        .map { list -> if (!isExtendedUpdateEnabled) list.filter { it.hasValidCert } else list }
        .stateIn(AuroraApp.scope, SharingStarted.WhileSubscribed(), null)

    suspend fun checkUpdates(): List<Update> {
        Log.i(TAG, "Checking for updates")
        val packageInfoMap = PackageUtil.getPackageInfoMap(context)
        val appUpdatesList = getFilteredInstalledApps(packageInfoMap).filter {
            val packageInfo = packageInfoMap[it.packageName]
            if (packageInfo != null) {
                it.versionCode.toLong() > PackageInfoCompat.getLongVersionCode(packageInfo)
            } else {
                false
            }
        }.toMutableList()

        if (canSelfUpdate(context)) {
            getSelfUpdate(context, gson)?.let { appUpdatesList.add(it) }
        }

        return appUpdatesList.map { Update.fromApp(context, it) }.also {
            // Cache the updates into the database
            updateDao.insertUpdates(it)
        }
    }

    suspend fun deleteUpdate(packageName: String) {
        updateDao.delete(packageName)
    }

    suspend fun getFilteredInstalledApps(
        packageInfoMap: MutableMap<String, PackageInfo>? = null
    ): List<App> {
        return withContext(Dispatchers.IO) {
            val blackList = BlacklistProvider.with(context).getBlackList()
            val appDetailsHelper = AppDetailsHelper(authProvider.authData!!)
                .using(HttpClient.getPreferredClient(context))

            (packageInfoMap ?: PackageUtil.getPackageInfoMap(context)).keys.let { packages ->
                val filtersPackages = packages.filter { !blackList.contains(it) }

                appDetailsHelper.getAppByPackageName(filtersPackages)
                    .filter { it.displayName.isNotEmpty() }
                    .map { it.isInstalled = true; it }
            }
        }
    }

    private fun canSelfUpdate(context: Context): Boolean {
        return !CertUtil.isFDroidApp(context, BuildConfig.APPLICATION_ID) &&
                !CertUtil.isAppGalleryApp(context, BuildConfig.APPLICATION_ID)
    }

    private suspend fun getSelfUpdate(context: Context, gson: Gson): App? {
        return withContext(Dispatchers.IO) {
            @Suppress("KotlinConstantConditions") // False-positive for build type always not being release
            if (BuildConfig.BUILD_TYPE != RELEASE) {
                Log.i(TAG, "Self-updates are not available for this build!")
                return@withContext null
            }

            try {
                val response =
                    HttpClient.getPreferredClient(context).get(Constants.UPDATE_URL, mapOf())
                val selfUpdate =
                    gson.fromJson(String(response.responseBytes), SelfUpdate::class.java)

                if (selfUpdate.versionCode > BuildConfig.VERSION_CODE) {
                    if (CertUtil.isFDroidApp(context, BuildConfig.APPLICATION_ID)) {
                        if (selfUpdate.fdroidBuild.isNotEmpty()) {
                            return@withContext SelfUpdate.toApp(selfUpdate, context)
                        }
                    } else if (selfUpdate.auroraBuild.isNotEmpty()) {
                        return@withContext SelfUpdate.toApp(selfUpdate, context)
                    } else {
                        Log.e(TAG, "Update file is missing!")
                        return@withContext null
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to check self-updates", exception)
                return@withContext null
            }

            Log.i(TAG, "No self-updates found!")
            return@withContext null
        }
    }
}
