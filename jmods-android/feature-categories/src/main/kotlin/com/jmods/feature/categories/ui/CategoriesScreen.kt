package com.jmods.feature.categories.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jmods.ui.component.CategoryCard
import com.jmods.ui.component.JModsTopBar

@Composable
fun CategoriesScreen(
    onCategoryClick: (String) -> Unit
) {
    val categories = listOf(
        "Productivity" to Icons.Default.Edit,
        "Social" to Icons.Default.Email,
        "Games" to Icons.Default.PlayArrow,
        "Tools" to Icons.Default.Build,
        "Media" to Icons.Default.Search,
        "Security" to Icons.Default.Lock,
        "Finance" to Icons.Default.ShoppingCart,
        "Health" to Icons.Default.Favorite
    )

    Scaffold(
        topBar = {
            JModsTopBar(title = "Categories")
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories) { (name, icon) ->
                CategoryCard(
                    category = name,
                    icon = { Icon(icon, contentDescription = name) },
                    onClick = { onCategoryClick(name) }
                )
            }
        }
    }
}
