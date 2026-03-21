package com.jmods.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String,
    val version: String,
    val size: Long,
    val category: String,
    val developer: String = "Unknown",
    val rating: Float = 0f,
    val lastUpdated: Long = System.currentTimeMillis()
)
