/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.room.download

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.aurora.gplayapi.model.AppFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
@ProvidedTypeConverter
class DownloadConverter @Inject constructor(private val json: Json) {

    @TypeConverter
    fun toSharedLibList(string: String): List<SharedLib> =
        json.decodeFromString<List<SharedLib>>(string)

    @TypeConverter
    fun fromSharedLibList(list: List<SharedLib>): String = json.encodeToString(list)

    @TypeConverter
    fun toGPlayFileList(string: String): List<AppFile> =
        json.decodeFromString<List<AppFile>>(string)

    @TypeConverter
    fun fromGPlayFileList(list: List<AppFile>): String = json.encodeToString(list)

    @TypeConverter
    fun toStringList(string: String): List<String> = json.decodeFromString<List<String>>(string)

    @TypeConverter
    fun fromStringList(list: List<String>): String = json.encodeToString(list)
}
