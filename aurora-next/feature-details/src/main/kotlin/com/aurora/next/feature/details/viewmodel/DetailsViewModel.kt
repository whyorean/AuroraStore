package com.aurora.next.feature.details.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aurora.next.domain.error.AppResult
import com.aurora.next.domain.model.App
import com.aurora.next.domain.usecase.GetAppDetailsUseCase
import com.aurora.next.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val getAppDetailsUseCase: GetAppDetailsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val details = savedStateHandle.toRoute<AppDestination.Details>()
    private val packageName = details.packageName

    private val _uiState = MutableStateFlow<AppResult<App>>(AppResult.Loading)
    val uiState: StateFlow<AppResult<App>> = _uiState.asStateFlow()

    init {
        fetchDetails()
    }

    fun fetchDetails() {
        viewModelScope.launch {
            getAppDetailsUseCase(packageName)
                .onStart { _uiState.value = AppResult.Loading }
                .catch { _uiState.value = AppResult.Error(com.aurora.next.domain.error.AppException.Unknown(it.message ?: "Unknown error")) }
                .collect { app ->
                    _uiState.value = AppResult.Success(app)
                }
        }
    }
}
