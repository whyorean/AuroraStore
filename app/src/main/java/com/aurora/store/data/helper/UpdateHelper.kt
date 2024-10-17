package com.aurora.store.data.helper

import android.content.Context
import android.content.pm.PackageInfo
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.network.IHttpClient
import com.aurora.store.AuroraApp
import com.aurora.store.BuildConfig
import com.aurora.store.data.model.SelfUpdate
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.data.room.update.Update
import com.aurora.store.data.room.update.UpdateDao
import com.aurora.store.util.CertUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class UpdateHelper @Inject constructor(
    private val gson: Gson,
    private val authProvider: AuthProvider,
    private val updateDao: UpdateDao,
    private val blacklistProvider: BlacklistProvider,
    private val httpClient: IHttpClient,
    @ApplicationContext private val context: Context
) {

    private val TAG = UpdateHelper::class.java.simpleName

    private val RELEASE = "release"
    private val NIGHTLY = "nightly"

    private val isExtendedUpdateEnabled get() = Preferences.getBoolean(
        context, Preferences.PREFERENCE_UPDATES_EXTENDED
    )
    val updates = updateDao.updates()
        .map { list -> if (!isExtendedUpdateEnabled) list.filter { it.hasValidCert } else list }
        .stateIn(AuroraApp.scope, SharingStarted.WhileSubscribed(), null)

    init {
        AuroraApp.scope.launch {
            updateDao.updates().firstOrNull()?.forEach { update ->
                if (!update.isInstalled(context) || update.isUpToDate(context)) {
                    deleteUpdate(update.packageName)
                }
            }
        }
    }

    suspend fun checkUpdates(): List<Update> {
        Log.i(TAG, "Checking for updates")
        val packageInfoMap = PackageUtil.getPackageInfoMap(context)
        val appUpdatesList = getFilteredInstalledApps(packageInfoMap)
            .filter {
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

    private suspend fun getFilteredInstalledApps(
        packageInfoMap: MutableMap<String, PackageInfo>? = null
    ): List<App> {
        return withContext(Dispatchers.IO) {
            val appDetailsHelper = AppDetailsHelper(authProvider.authData!!)
                .using(httpClient)

            (packageInfoMap ?: PackageUtil.getPackageInfoMap(context)).keys.let { packages ->
                val filtersPackages = packages.filter { !blacklistProvider.isBlacklisted(it) }

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
            val updateUrl = when (BuildConfig.BUILD_TYPE) {
                RELEASE -> Constants.UPDATE_URL_STABLE
                NIGHTLY -> Constants.UPDATE_URL_NIGHTLY
                else -> {
                    Log.i(TAG, "Self-updates are not available for this build!")
                    return@withContext null
                }
            }

            try {
                val response = httpClient.get(updateUrl, mapOf())
                val selfUpdate =
                    gson.fromJson(String(response.responseBytes), SelfUpdate::class.java)

                val isUpdate = when (BuildConfig.BUILD_TYPE) {
                    RELEASE -> selfUpdate.versionCode > BuildConfig.VERSION_CODE
                    NIGHTLY -> selfUpdate.timestamp > BuildConfig.TIMESTAMP
                    else -> false
                }

                if (isUpdate) {
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
