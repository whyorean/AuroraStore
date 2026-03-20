package com.aurora.next.network.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class PlayApiServiceImpl @Inject constructor() : PlayApiService {
    override fun getApps(category: String): Flow<List<AppDto>> = flow {
        // Mocking API call
        delay(1000)
        emit(listOf(
            AppDto("1", "Aurora Store", "com.aurora.store", "Open source Google Play client", "https://auroraoss.com/icon.png", "4.1.1", 1000000),
            AppDto("2", "F-Droid", "org.fdroid.fdroid", "FOSS App Store", "https://f-droid.org/icon.png", "1.15.0", 5000000)
        ))
    }

    override fun getAppDetails(packageName: String): Flow<AppDto> = flow {
        delay(500)
        emit(AppDto("1", "Aurora Store", packageName, "Open source Google Play client", "https://auroraoss.com/icon.png", "4.1.1", 1000000))
    }
}
