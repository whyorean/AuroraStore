package com.aurora.next.navigation

import kotlinx.serialization.Serializable

sealed interface AppDestination {
    @Serializable
    object Home : AppDestination

    @Serializable
    data class Details(val packageName: String) : AppDestination

    @Serializable
    object Search : AppDestination

    @Serializable
    object Updates : AppDestination

    @Serializable
    object Downloads : AppDestination

    @Serializable
    object Settings : AppDestination
}
