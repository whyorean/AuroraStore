package com.jmods.feature.updates.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jmods.domain.error.AppResult
import com.jmods.domain.model.App
import com.jmods.domain.usecase.GetUpdatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    private val getUpdatesUseCase: GetUpdatesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppResult<List<App>>>(AppResult.Loading)
    val uiState: StateFlow<AppResult<List<App>>> = _uiState.asStateFlow()

    init {
        fetchUpdates()
    }

    fun fetchUpdates() {
        viewModelScope.launch {
            getUpdatesUseCase()
                .onStart { _uiState.value = AppResult.Loading }
                .catch { _uiState.value = AppResult.Error(com.jmods.domain.error.AppException.Unknown(it.message ?: "Failed to fetch updates")) }
                .collect { updates ->
                    _uiState.value = AppResult.Success(updates)
                }
        }
    }
}
