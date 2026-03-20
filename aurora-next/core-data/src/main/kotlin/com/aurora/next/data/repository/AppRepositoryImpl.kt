package com.aurora.next.data.repository

import com.aurora.next.data.mapper.toDomain
import com.aurora.next.data.mapper.toEntity
import com.aurora.next.database.dao.AppDao
import com.aurora.next.domain.model.App
import com.aurora.next.domain.usecase.AppRepository
import com.aurora.next.network.api.PlayApiService
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class AppRepositoryImpl @Inject constructor(
    private val api: PlayApiService,
    private val appDao: AppDao
) : AppRepository {

    override fun getApps(category: String): Flow<List<App>> = flow {
        // Emit cached data first
        val cached = appDao.getAppsByCategory(category).first()
        if (cached.isNotEmpty()) {
            emit(cached.map { it.toDomain() })
        }

        // Fetch from network and update cache
        api.getApps(category).collect { dtos ->
            val entities = dtos.map { it.toEntity(category) }
            appDao.insertApps(entities)
            emit(dtos.map { it.toDomain() })
        }
    }

    override fun getAppDetails(packageName: String): Flow<App> = flow {
        // Try cache first
        val cached = appDao.getAppByPackageName(packageName).first()
        if (cached != null) {
            emit(cached.toDomain())
        }

        // Fetch from network and update cache
        api.getAppDetails(packageName).collect { dto ->
            appDao.insertApp(dto.toEntity("unknown")) // category could be improved
            emit(dto.toDomain())
        }
    }
}
