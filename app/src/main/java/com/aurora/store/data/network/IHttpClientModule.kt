package com.aurora.store.data.network

import com.aurora.gplayapi.network.IHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IHttpClientModule {

    @Provides
    @Singleton
    fun providesIHttpClientInstance(httpClient: HttpClient): IHttpClient {
        return httpClient
    }
}
