package com.jmods.feature.search.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jmods.domain.error.AppResult
import com.jmods.feature.search.viewmodel.SearchViewModel
import com.jmods.ui.component.AppCard
import com.jmods.ui.component.JModsTopBar

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onAppClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val query by viewModel.query.collectAsState()
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            JModsTopBar(
                title = "Search",
                onBackClick = onBack
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search apps...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Box(modifier = Modifier.weight(1f)) {
                when (val result = state) {
                    is AppResult.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is AppResult.Success -> {
                        if (result.data.isEmpty() && query.isNotEmpty()) {
                            Text(
                                text = "No results found for '$query'",
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
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
}
