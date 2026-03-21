package com.jmods.domain.usecase

import com.jmods.domain.model.App
import com.jmods.domain.usecase.AppRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppDetailsUseCase @Inject constructor(
    private val repository: AppRepository
) {
    operator fun invoke(packageName: String): Flow<App> = repository.getAppDetails(packageName)
}
