package com.aurora.store.module

import com.aurora.gplayapi.data.serializers.LocaleSerializer
import com.aurora.gplayapi.data.serializers.PropertiesSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

@Module
@InstallIn(SingletonComponent::class)
object CommonModule {

    @Singleton
    @Provides
    fun providesJsonInstance(): Json {
        val module = SerializersModule {
            contextual(LocaleSerializer)
            contextual(PropertiesSerializer)
        }

        return Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            coerceInputValues = true
            serializersModule = module
            explicitNulls = false
        }
    }
}
