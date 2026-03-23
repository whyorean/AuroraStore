package com.jmods.network.api

import com.google.gson.annotations.SerializedName

data class AppDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("description") val description: String,
    @SerializedName("iconUrl") val iconUrl: String,
    @SerializedName("version") val version: String,
    @SerializedName("size") val size: Long,
    @SerializedName("developer") val developer: String? = null,
    @SerializedName("rating") val rating: Float? = null,
    @SerializedName("versionCode") val versionCode: Int = 0,
    @SerializedName("screenshots") val screenshots: List<String> = emptyList()
)

data class AppListResponse(
    @SerializedName("apps") val apps: List<AppDto>
)
