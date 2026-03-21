package com.jmods.feature.details.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jmods.data.download.DownloadState
import com.jmods.domain.error.AppResult
import com.jmods.domain.model.App
import com.jmods.feature.details.viewmodel.DetailsViewModel
import com.jmods.installer.InstallStatus
import com.jmods.ui.component.JModsTopBar

@Composable
fun DetailsScreen(
    viewModel: DetailsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val installStatus by viewModel.installStatus.collectAsState()

    Scaffold(
        topBar = {
            JModsTopBar(
                title = "App Details",
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
                    AppDetailsContent(
                        app = result.data,
                        downloadState = downloadState,
                        installStatus = installStatus,
                        onDownloadClick = { viewModel.startDownload(result.data) }
                    )
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
fun AppDetailsContent(
    app: App,
    downloadState: DownloadState,
    installStatus: InstallStatus,
    onDownloadClick: () -> Unit
) {
    val sizeMb = app.size / 1024 / 1024
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = app.iconUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = app.name, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black))
                Text(text = app.developer, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = String.format("%.1f", app.rating), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        DownloadInstallButton(
            downloadState = downloadState,
            installStatus = installStatus,
            onDownloadClick = onDownloadClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "About this app", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = app.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Information", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(8.dp))
        InfoRow(label = "Package Name", value = app.packageName)
        InfoRow(label = "Version", value = app.version)
        InfoRow(label = "Size", value = "$sizeMb MB")
    }
}

@Composable
fun DownloadInstallButton(
    downloadState: DownloadState,
    installStatus: InstallStatus,
    onDownloadClick: () -> Unit
) {
    when {
        installStatus is InstallStatus.Success -> {
            Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = false) {
                Text("Installed")
            }
        }
        installStatus is InstallStatus.Progress -> {
            Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = false) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Installing...")
            }
        }
        downloadState is DownloadState.Completed -> {
            Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = false) {
                Text("Verifying...")
            }
        }
        downloadState is DownloadState.Downloading -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { downloadState.progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Downloading ${(downloadState.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
        else -> {
            Button(
                onClick = onDownloadClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Install")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
