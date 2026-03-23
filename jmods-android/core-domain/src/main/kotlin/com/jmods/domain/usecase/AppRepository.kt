package com.jmods.domain.usecase

import com.jmods.domain.model.App
import com.jmods.domain.model.InstalledAppInfo
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getApps(category: String): Flow<List<App>>
    fun getAppDetails(packageName: String): Flow<App>
    fun searchApps(query: String): Flow<List<App>>
    fun getInstalledApps(): Flow<List<InstalledAppInfo>>
    fun getAppsWithUpdates(): Flow<List<App>>
}
