package com.jmods.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.jmods.data.mapper.toDomain
import com.jmods.data.mapper.toEntity
import com.jmods.database.dao.AppDao
import com.jmods.domain.model.App
import com.jmods.domain.model.InstalledAppInfo
import com.jmods.domain.usecase.AppRepository
import com.jmods.network.api.PlayApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class AppRepositoryImpl @Inject constructor(
    private val api: PlayApiService,
    private val appDao: AppDao,
    @ApplicationContext private val context: Context
) : AppRepository {

    override fun getApps(category: String): Flow<List<App>> = flow {
        val cached = appDao.getAppsByCategory(category).first()
        if (cached.isNotEmpty()) {
            emit(cached.map { it.toDomain() })
        }

        api.getApps(category).collect { dtos ->
            val entities = dtos.map { it.toEntity(category) }
            appDao.insertApps(entities)
            emit(dtos.map { it.toDomain() })
        }
    }

    override fun getAppDetails(packageName: String): Flow<App> = flow {
        val cached = appDao.getAppByPackageName(packageName).first()
        if (cached != null) {
            emit(cached.toDomain())
        }

        api.getAppDetails(packageName).collect { dto ->
            appDao.insertApp(dto.toEntity("unknown"))
            emit(dto.toDomain())
        }
    }

    override fun searchApps(query: String): Flow<List<App>> = flow {
        api.searchApps(query).collect { dtos ->
            emit(dtos.map { it.toDomain() })
        }
    }

    override fun getInstalledApps(): Flow<List<InstalledAppInfo>> = flow {
        val pm = context.packageManager
        val apps = pm.getInstalledPackages(0).map { pkg ->
            InstalledAppInfo(
                packageName = pkg.packageName,
                versionName = pkg.versionName ?: "",
                versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    pkg.longVersionCode
                } else {
                    pkg.versionCode.toLong()
                },
                isSystemApp = (pkg.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )
        }
        emit(apps)
    }

    override fun getAppsWithUpdates(): Flow<List<App>> = flow {
        val installed = getInstalledApps().first()
        val updateable = mutableListOf<App>()

        // This is a simplified implementation.
        // In a production app, we would perform a bulk fetch for these package names.
        installed.filter { !it.isSystemApp }.take(10).forEach { info ->
            try {
                // Fetch latest details for each app to check for updates
                // We wrap in try-catch because not all installed apps might be in the store
                api.getAppDetails(info.packageName).collect { dto ->
                    if (dto.versionCode > info.versionCode) {
                        updateable.add(dto.toDomain().copy(hasUpdate = true))
                    }
                }
            } catch (e: Exception) {
                // App not found or other error, skip
            }
        }
        emit(updateable)
    }
}
