package com.aurora.store.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommonModule {

    @Singleton
    @Provides
    fun providesJsonInstance(): Json {
        return Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }
}
