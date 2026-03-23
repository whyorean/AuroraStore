package com.jmods.installer.di

import com.jmods.installer.AndroidPackageInstaller
import com.jmods.installer.AppInstaller
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InstallerModule {
    @Binds
    @Singleton
    abstract fun bindAppInstaller(impl: AndroidPackageInstaller): AppInstaller
}
