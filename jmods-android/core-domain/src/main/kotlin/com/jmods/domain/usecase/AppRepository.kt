package com.jmods.domain.usecase

import com.jmods.domain.model.App
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getApps(category: String): Flow<List<App>>
    fun getAppDetails(packageName: String): Flow<App>
}
