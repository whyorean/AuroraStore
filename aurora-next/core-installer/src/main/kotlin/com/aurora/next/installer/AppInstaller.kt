package com.aurora.next.installer

import kotlinx.coroutines.flow.Flow

interface AppInstaller {
    fun install(packagePath: String): Flow<InstallStatus>
    fun uninstall(packageName: String): Flow<InstallStatus>
}

sealed class InstallStatus {
    object Idle : InstallStatus()
    data class Progress(val percentage: Float) : InstallStatus()
    object Success : InstallStatus()
    data class Failure(val error: String) : InstallStatus()
}
