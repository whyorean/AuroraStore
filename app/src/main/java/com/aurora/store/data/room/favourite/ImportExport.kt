package com.aurora.store.data.room.favourite

import com.aurora.store.BuildConfig

data class ImportExport(
    val favourites: List<Favourite>,
    val auroraStoreVersion: Int = BuildConfig.VERSION_CODE,
)
