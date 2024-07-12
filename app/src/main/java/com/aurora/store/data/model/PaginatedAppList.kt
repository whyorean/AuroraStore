package com.aurora.store.data.model

import com.aurora.gplayapi.data.models.App

data class PaginatedAppList(
    val appList: MutableList<App> = mutableListOf(),
    var hasMore: Boolean
)