package com.aurora.store.util

import android.content.Context
import android.content.pm.PackageInfo
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.aurora.Constants
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.store.BuildConfig
import com.aurora.store.data.model.SelfUpdate
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.BlacklistProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppUtil {

    private const val TAG = "AppUtil"
    private const val RELEASE = "release"

    suspend fun getUpdatableApps(context: Context, gson: Gson, verifyCert: Boolean): List<App> {
        val packageInfoMap = PackageUtil.getPackageInfoMap(context)
        val appUpdatesList = getFilteredInstalledApps(context, packageInfoMap).filter {
            val packageInfo = packageInfoMap[it.packageName]
            if (packageInfo != null) {
                it.versionCode.toLong() > PackageInfoCompat.getLongVersionCode(packageInfo)
            } else {
                false
            }
        }.toMutableList()

        val verifiedUpdatesList = if (verifyCert) {
            appUpdatesList.filter { app ->
                app.certificateSetList.any {
                    it.certificateSet in CertUtil.getEncodedCertificateHashes(
                        context, app.packageName
                    )
                }
            }.toMutableList()
        } else {
            appUpdatesList
        }

        if (!CertUtil.isFDroidApp(context, BuildConfig.APPLICATION_ID)) {
            getSelfUpdate(context, gson)?.let { verifiedUpdatesList.add(it) }
        }

        return verifiedUpdatesList
    }

    suspend fun getFilteredInstalledApps(
        context: Context,
        packageInfoMap: MutableMap<String, PackageInfo>? = null
    ): List<App> {
        return withContext(Dispatchers.IO) {
            val authData = AuthProvider.with(context).getAuthData()
            val blackList = BlacklistProvider.with(context).getBlackList()
            val appDetailsHelper =
                AppDetailsHelper(authData).using(HttpClient.getPreferredClient(context))

            (packageInfoMap ?: PackageUtil.getPackageInfoMap(context)).keys.let { packages ->
                val filtersPackages = packages.filter { !blackList.contains(it) }

                appDetailsHelper.getAppByPackageName(filtersPackages)
                    .filter { it.displayName.isNotEmpty() }
                    .map { it.isInstalled = true; it }
            }
        }
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
