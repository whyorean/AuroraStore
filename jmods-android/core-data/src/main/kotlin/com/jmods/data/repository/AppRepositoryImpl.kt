package com.jmods.data.repository

import com.jmods.data.mapper.toDomain
import com.jmods.data.mapper.toEntity
import com.jmods.database.dao.AppDao
import com.jmods.domain.model.App
import com.jmods.domain.usecase.AppRepository
import com.jmods.network.api.PlayApiService
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
