package com.aurora.next.network.di

import com.aurora.next.network.api.PlayApiService
import com.aurora.next.network.api.PlayApiServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds
    @Singleton
    abstract fun bindPlayApiService(impl: PlayApiServiceImpl): PlayApiService
}
