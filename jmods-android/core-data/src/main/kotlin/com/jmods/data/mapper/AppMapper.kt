package com.jmods.data.mapper

import com.jmods.database.entity.AppEntity
import com.jmods.domain.model.App
import com.jmods.network.api.AppDto

fun AppDto.toDomain(): App = App(
    id = id,
    name = name,
    packageName = packageName,
    description = description,
    iconUrl = iconUrl,
    version = version,
    size = size,
    developer = developer ?: "Unknown Developer",
    rating = rating ?: 0f
)

fun AppDto.toEntity(category: String): AppEntity = AppEntity(
    packageName = packageName,
    id = id,
    name = name,
    description = description,
    iconUrl = iconUrl,
    version = version,
    size = size,
    category = category,
    developer = developer ?: "Unknown Developer",
    rating = rating ?: 0f
)

fun AppEntity.toDomain(): App = App(
    id = id,
    name = name,
    packageName = packageName,
    description = description,
    iconUrl = iconUrl,
    version = version,
    size = size,
    developer = developer,
    rating = rating
)
