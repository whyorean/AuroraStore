package com.jmods.feature.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jmods.domain.error.AppResult
import com.jmods.domain.model.App
import com.jmods.domain.usecase.GetAppsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getAppsUseCase: GetAppsUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _uiState = MutableStateFlow<AppResult<List<App>>>(AppResult.Success(emptyList()))
    val uiState: StateFlow<AppResult<List<App>>> = _uiState.asStateFlow()

    init {
        _query
            .debounce(500)
            .filter { it.isNotEmpty() }
            .onEach { performSearch(it) }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isEmpty()) {
            _uiState.value = AppResult.Success(emptyList())
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            getAppsUseCase(query)
                .onStart { _uiState.value = AppResult.Loading }
                .catch { _uiState.value = AppResult.Error(com.jmods.domain.error.AppException.Unknown(it.message ?: "Search failed")) }
                .collect { results ->
                    _uiState.value = AppResult.Success(results)
                }
        }
    }
}
