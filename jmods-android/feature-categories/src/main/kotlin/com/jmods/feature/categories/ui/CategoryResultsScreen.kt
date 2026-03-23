package com.jmods.feature.categories.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.jmods.domain.error.AppResult
import com.jmods.domain.model.App
import com.jmods.domain.usecase.GetAppsUseCase
import com.jmods.navigation.AppDestination
import com.jmods.ui.component.AppCard
import com.jmods.ui.component.JModsTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryResultsViewModel @Inject constructor(
    private val getAppsUseCase: GetAppsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<AppDestination.CategoryResults>()
    val categoryName = route.categoryName

    private val _uiState = MutableStateFlow<AppResult<List<App>>>(AppResult.Loading)
    val uiState: StateFlow<AppResult<List<App>>> = _uiState.asStateFlow()

    init {
        fetchApps()
    }

    fun fetchApps() {
        viewModelScope.launch {
            getAppsUseCase(categoryName)
                .onStart { _uiState.value = AppResult.Loading }
                .catch { _uiState.value = AppResult.Error(com.jmods.domain.error.AppException.Unknown(it.message ?: "Unknown error")) }
                .collect { apps ->
                    _uiState.value = AppResult.Success(apps)
                }
        }
    }
}

@Composable
fun CategoryResultsScreen(
    viewModel: CategoryResultsViewModel,
    onAppClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            JModsTopBar(
                title = viewModel.categoryName,
                onBackClick = onBack
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val result = state) {
                is AppResult.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is AppResult.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(result.data) { app ->
                            AppCard(app = app, onClick = { onAppClick(app.packageName) })
                        }
                    }
                }
                is AppResult.Error -> {
                    Text(
                        text = "Error: ${result.exception.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
