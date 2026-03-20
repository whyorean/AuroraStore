package com.aurora.next

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aurora.next.feature.home.ui.HomeScreen
import com.aurora.next.feature.details.ui.DetailsScreen
import com.aurora.next.navigation.AppDestination
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = AppDestination.Home
                    ) {
                        composable<AppDestination.Home> {
                            HomeScreen(
                                viewModel = hiltViewModel(),
                                onAppClick = { packageName ->
                                    navController.navigate(AppDestination.Details(packageName))
                                }
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
