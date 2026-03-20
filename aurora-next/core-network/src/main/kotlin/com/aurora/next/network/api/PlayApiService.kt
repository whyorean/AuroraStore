package com.aurora.next.network.api

import kotlinx.coroutines.flow.Flow

interface PlayApiService {
    fun getApps(category: String): Flow<List<AppDto>>
    fun getAppDetails(packageName: String): Flow<AppDto>
}
