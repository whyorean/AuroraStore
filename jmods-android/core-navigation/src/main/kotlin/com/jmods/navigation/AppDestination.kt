package com.jmods.navigation

import kotlinx.serialization.Serializable

sealed interface AppDestination {
    @Serializable
    object Home : AppDestination

    @Serializable
    object Categories : AppDestination

    @Serializable
    object Updates : AppDestination

    @Serializable
    object Search : AppDestination

    @Serializable
    data class Details(val packageName: String) : AppDestination

    @Serializable
    data class CategoryResults(val categoryName: String) : AppDestination
}
