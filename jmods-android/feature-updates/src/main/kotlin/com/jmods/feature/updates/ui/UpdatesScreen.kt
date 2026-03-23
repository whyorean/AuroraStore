package com.jmods.feature.updates.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jmods.domain.error.AppResult
import com.jmods.domain.model.App
import com.jmods.feature.updates.viewmodel.UpdatesViewModel
import com.jmods.ui.component.AppCard
import com.jmods.ui.component.JModsTopBar

@Composable
fun UpdatesScreen(
    viewModel: UpdatesViewModel,
    onAppClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            JModsTopBar(
                title = "Updates",
                actions = {
                    IconButton(onClick = { viewModel.fetchUpdates() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val result = state) {
                is AppResult.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is AppResult.Success -> {
                    if (result.data.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Your apps are up to date", style = MaterialTheme.typography.titleMedium)
                            Text("Check back later for new versions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        UpdatesList(
                            updates = result.data,
                            onAppClick = onAppClick,
                            onUpdateAll = { /* TODO */ }
                        )
                    }
                }
                is AppResult.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Error: ${result.exception.message}", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.fetchUpdates() }, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdatesList(
    updates: List<App>,
    onAppClick: (String) -> Unit,
    onUpdateAll: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${updates.size} updates available",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onUpdateAll) {
                    Text("Update All")
                }
            }
        }
        items(updates) { app ->
            AppCard(app = app, onClick = { onAppClick(app.packageName) })
        }
    }
}
