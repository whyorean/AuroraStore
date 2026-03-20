package com.aurora.next.domain.usecase

import com.aurora.next.domain.model.App
import com.aurora.next.domain.usecase.AppRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppDetailsUseCase @Inject constructor(
    private val repository: AppRepository
) {
    operator fun invoke(packageName: String): Flow<App> = repository.getAppDetails(packageName)
}
