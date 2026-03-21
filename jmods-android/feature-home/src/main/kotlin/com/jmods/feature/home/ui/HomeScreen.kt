package com.jmods.feature.home.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jmods.domain.model.App
import com.jmods.feature.home.viewmodel.HomeViewModel
import com.jmods.feature.home.viewmodel.HomeUiState
import com.jmods.ui.component.AppCard
import com.jmods.ui.component.JModsTopBar

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAppClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            JModsTopBar(
                title = "JMODS",
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val result = state) {
                is HomeUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is HomeUiState.Success -> {
                    HomeContent(
                        featured = result.featured,
                        topCharts = result.topCharts,
                        onAppClick = onAppClick
                    )
                }
                is HomeUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error: ${result.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.fetchHomeData() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    featured: List<App>,
    topCharts: List<App>,
    onAppClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (featured.isNotEmpty()) {
            item {
                Column {
                    SectionHeader("Featured", modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(featured) { app ->
                            FeaturedAppCard(app = app, onClick = { onAppClick(app.packageName) })
                        }
                    }
                }
            }
        }

        if (topCharts.isNotEmpty()) {
            item {
                SectionHeader("Top Charts", modifier = Modifier.padding(horizontal = 16.dp))
            }
            itemsIndexed(topCharts) { index, app ->
                AppCard(
                    app = app,
                    onClick = { onAppClick(app.packageName) },
                    rank = index + 1,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                if (index < topCharts.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun FeaturedAppCard(app: App, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(300.dp).height(180.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = app.iconUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1
                    )
                    Text(
                        text = app.developer,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "FEATURED APP",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}
