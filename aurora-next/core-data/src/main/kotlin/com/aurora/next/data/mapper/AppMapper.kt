package com.aurora.next.data.mapper

import com.aurora.next.database.entity.AppEntity
import com.aurora.next.domain.model.App
import com.aurora.next.network.api.AppDto

fun AppDto.toDomain(): App = App(
    id = id,
    name = name,
    packageName = packageName,
    description = description,
    iconUrl = iconUrl,
    version = version,
    size = size
)

fun AppDto.toEntity(category: String): AppEntity = AppEntity(
    packageName = packageName,
    id = id,
    name = name,
    description = description,
    iconUrl = iconUrl,
    version = version,
    size = size,
    category = category
)

fun AppEntity.toDomain(): App = App(
    id = id,
    name = name,
    packageName = packageName,
    description = description,
    iconUrl = iconUrl,
    version = version,
    size = size
)
