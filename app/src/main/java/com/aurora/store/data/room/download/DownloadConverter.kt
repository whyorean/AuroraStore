package com.aurora.store.data.room.download

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.aurora.gplayapi.data.models.PlayFile
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
    fun toGPlayFileList(string: String): List<PlayFile> =
        json.decodeFromString<List<PlayFile>>(string)

    @TypeConverter
    fun fromGPlayFileList(list: List<PlayFile>): String = json.encodeToString(list)
}
