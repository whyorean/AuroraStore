package com.aurora.next.data.repository

import com.aurora.next.data.mapper.toDomain
import com.aurora.next.domain.model.App
import com.aurora.next.domain.usecase.AppRepository
import com.aurora.next.network.api.PlayApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppRepositoryImpl @Inject constructor(
    private val api: PlayApiService
) : AppRepository {
    override fun getApps(category: String): Flow<List<App>> = api.getApps(category).map { list ->
        list.map { it.toDomain() }
    }

    override fun getAppDetails(packageName: String): Flow<App> = api.getAppDetails(packageName).map {
        it.toDomain()
    }
}
