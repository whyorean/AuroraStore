package com.jmods.domain.usecase

import com.jmods.domain.model.App
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppsUseCase @Inject constructor(
    private val repository: AppRepository
) {
    operator fun invoke(category: String): Flow<List<App>> = repository.getApps(category)
}
