package com.aurora.next.domain.usecase

import com.aurora.next.domain.model.App
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getApps(category: String): Flow<List<App>>
    fun getAppDetails(packageName: String): Flow<App>
}
