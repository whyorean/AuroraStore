package com.aurora.next.domain.usecase

import com.aurora.next.domain.model.App
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppsUseCase @Inject constructor(
    private val repository: AppRepository
) {
    operator fun invoke(category: String): Flow<List<App>> = repository.getApps(category)
}
