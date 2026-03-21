package com.jmods.data.repository

import com.jmods.database.dao.AppDao
import com.jmods.domain.model.App
import com.jmods.network.api.AppDto
import com.jmods.network.api.PlayApiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class AppRepositoryTest {

    private val api: PlayApiService = mock()
    private val appDao: AppDao = mock()
    private lateinit var repository: AppRepositoryImpl

    @Before
    fun setup() {
        repository = AppRepositoryImpl(api, appDao)
    }

    @Test
    fun getApps_shouldEmitCachedDataThenNetworkData() = runTest {
        val category = "Games"
        val cachedApps = emptyList<com.jmods.database.entity.AppEntity>()
        val networkApps = listOf(
            AppDto(
                id = "1", name = "Game", packageName = "com.game",
                description = "Desc", iconUrl = "", version = "1.0", versionCode = 1, size = 100
            )
        )

        whenever(appDao.getAppsByCategory(category)).thenReturn(flowOf(cachedApps))
        whenever(api.getApps(category)).thenReturn(flowOf(networkApps))

        val result = repository.getApps(category).first()

        verify(api).getApps(category)
        verify(appDao).insertApps(any())
    }
}
