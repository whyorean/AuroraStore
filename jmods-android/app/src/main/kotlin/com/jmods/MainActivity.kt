package com.jmods

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jmods.feature.home.ui.HomeScreen
import com.jmods.feature.details.ui.DetailsScreen
import com.jmods.feature.categories.ui.CategoriesScreen
import com.jmods.feature.categories.ui.CategoryResultsScreen
import com.jmods.feature.search.ui.SearchScreen
import com.jmods.feature.updates.ui.UpdatesScreen
import com.jmods.navigation.AppDestination
import com.jmods.ui.component.JModsPlayer
import com.jmods.ui.theme.JMODSTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JMODSTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var isPlayerVisible by remember { mutableStateOf(false) }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Text(
                                "J MODS",
                                modifier = Modifier.padding(24.dp),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                            )
                            HorizontalDivider()
                            NavigationDrawerItem(
                                label = { Text("Settings") },
                                selected = false,
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = { scope.launch { drawerState.close() } }
                            )
                            NavigationDrawerItem(
                                label = { Text("About") },
                                selected = false,
                                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = { scope.launch { drawerState.close() } }
                            )
                        }
                    }
                ) {
                    Scaffold(
                        bottomBar = {
                            Column {
                                if (isPlayerVisible) {
                                    JModsPlayer(
                                        title = "Community Radio",
                                        onClose = { isPlayerVisible = false }
                                    )
                                }
                                NavigationBar {
                                    val items = listOf(
                                        NavigationItem("Home", AppDestination.Home, Icons.Default.Home),
                                        NavigationItem("Categories", AppDestination.Categories, Icons.AutoMirrored.Filled.List),
                                        NavigationItem("Updates", AppDestination.Updates, Icons.Default.Refresh),
                                        NavigationItem("Search", AppDestination.Search, Icons.Default.Search)
                                    )
                                    items.forEach { item ->
                                        NavigationBarItem(
                                            icon = { Icon(item.icon, contentDescription = item.name) },
                                            label = { Text(item.name) },
                                            selected = currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true,
                                            onClick = {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = AppDestination.Home,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable<AppDestination.Home> {
                                HomeScreen(
                                    viewModel = hiltViewModel(),
                                    onAppClick = { packageName ->
                                        navController.navigate(AppDestination.Details(packageName))
                                    },
                                    onSearchClick = {
                                        navController.navigate(AppDestination.Search)
                                    },
                                    onMenuClick = {
                                        scope.launch { drawerState.open() }
                                    }
                                )
                            }
                            composable<AppDestination.Categories> {
                                CategoriesScreen(
                                    onCategoryClick = { category ->
                                        navController.navigate(AppDestination.CategoryResults(category))
                                    }
                                )
                            }
                            composable<AppDestination.CategoryResults> {
                                CategoryResultsScreen(
                                    viewModel = hiltViewModel(),
                                    onAppClick = { packageName ->
                                        navController.navigate(AppDestination.Details(packageName))
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable<AppDestination.Updates> {
                                UpdatesScreen(
                                    viewModel = hiltViewModel(),
                                    onAppClick = { packageName ->
                                        navController.navigate(AppDestination.Details(packageName))
                                    }
                                )
                            }
                            composable<AppDestination.Search> {
                                SearchScreen(
                                    viewModel = hiltViewModel(),
                                    onAppClick = { packageName ->
                                        navController.navigate(AppDestination.Details(packageName))
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable<AppDestination.Details> {
                                DetailsScreen(
                                    viewModel = hiltViewModel(),
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class NavigationItem(val name: String, val route: Any, val icon: androidx.compose.ui.graphics.vector.ImageVector)
