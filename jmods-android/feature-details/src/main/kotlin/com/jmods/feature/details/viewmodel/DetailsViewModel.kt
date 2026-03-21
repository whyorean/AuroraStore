package com.jmods.feature.details.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.jmods.data.download.DownloadState
import com.jmods.data.download.JModsDownloadManager
import com.jmods.domain.error.AppResult
import com.jmods.domain.model.App
import com.jmods.domain.usecase.GetAppDetailsUseCase
import com.jmods.installer.AppInstaller
import com.jmods.installer.InstallStatus
import com.jmods.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val getAppDetailsUseCase: GetAppDetailsUseCase,
    private val downloadManager: JModsDownloadManager,
    private val installer: AppInstaller,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val details = savedStateHandle.toRoute<AppDestination.Details>()
    private val packageName = details.packageName

    private val _uiState = MutableStateFlow<AppResult<App>>(AppResult.Loading)
    val uiState: StateFlow<AppResult<App>> = _uiState.asStateFlow()

    val downloadState = downloadManager.getDownloadStatus(packageName)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DownloadState.Idle)

    private val _installStatus = MutableStateFlow<InstallStatus>(InstallStatus.Idle)
    val installStatus: StateFlow<InstallStatus> = _installStatus.asStateFlow()

    init {
        fetchDetails()
        observeDownloadCompletion()
    }

    fun fetchDetails() {
        viewModelScope.launch {
            getAppDetailsUseCase(packageName)
                .onStart { _uiState.value = AppResult.Loading }
                .catch { _uiState.value = AppResult.Error(com.jmods.domain.error.AppException.Unknown(it.message ?: "Unknown error")) }
                .collect { app ->
                    _uiState.value = AppResult.Success(app)
                }
        }
    }

    fun startDownload(app: App) {
        downloadManager.enqueueDownload(app)
    }

    private fun observeDownloadCompletion() {
        viewModelScope.launch {
            downloadState.collect { state ->
                if (state is DownloadState.Completed) {
                    installApp(packageName, state.apkPath)
                }
            }
        }
    }

    private fun installApp(packageName: String, apkPath: String) {
        viewModelScope.launch {
            installer.install(packageName, apkPath).collect { status ->
                _installStatus.value = status
            }
        }
    }
}
