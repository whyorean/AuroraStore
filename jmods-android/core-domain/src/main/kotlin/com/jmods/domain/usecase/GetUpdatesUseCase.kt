package com.jmods.domain.usecase

import com.jmods.domain.model.App
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUpdatesUseCase @Inject constructor(
    private val repository: AppRepository
) {
    operator fun invoke(): Flow<List<App>> = repository.getAppsWithUpdates()
}
