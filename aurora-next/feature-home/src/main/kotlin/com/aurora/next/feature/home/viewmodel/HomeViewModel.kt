package com.aurora.next.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.next.domain.error.AppResult
import com.aurora.next.domain.model.App
import com.aurora.next.domain.usecase.GetAppsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAppsUseCase: GetAppsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppResult<List<App>>>(AppResult.Loading)
    val uiState: StateFlow<AppResult<List<App>>> = _uiState.asStateFlow()

    init {
        fetchApps()
    }

    fun fetchApps() {
        viewModelScope.launch {
            getAppsUseCase("featured")
                .onStart { _uiState.value = AppResult.Loading }
                .catch { _uiState.value = AppResult.Error(com.aurora.next.domain.error.AppException.Unknown(it.message ?: "Unknown error")) }
                .collect { apps ->
                    _uiState.value = AppResult.Success(apps)
                }
        }
    }
}
