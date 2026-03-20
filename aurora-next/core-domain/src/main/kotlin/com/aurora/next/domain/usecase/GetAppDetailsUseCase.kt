package com.aurora.next.domain.usecase

import com.aurora.next.domain.model.App
import kotlinx.coroutines.flow.Flow

interface GetAppDetailsUseCase {
    operator fun invoke(packageName: String): Flow<App>
}
