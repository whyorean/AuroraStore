package com.aurora.next.feature.home.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aurora.next.domain.error.AppResult
import com.aurora.next.domain.model.App
import com.aurora.next.feature.home.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAppClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Aurora Next") })
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val result = state) {
                is AppResult.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is AppResult.Success -> {
                    AppList(apps = result.data, onAppClick = onAppClick)
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

@Composable
fun AppList(apps: List<App>, onAppClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(apps) { app ->
            AppItem(app = app, onClick = { onAppClick(app.packageName) })
        }
    }
}

@Composable
fun AppItem(app: App, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = app.iconUrl,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = app.name, style = MaterialTheme.typography.titleLarge)
            Text(text = app.packageName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${app.version} • ${app.size / 1024 / 1024} MB",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
