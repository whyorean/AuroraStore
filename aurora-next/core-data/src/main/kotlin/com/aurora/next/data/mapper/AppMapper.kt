package com.aurora.next.data.mapper

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
