package com.aurora.store.data.room.download

import androidx.room.TypeConverter
import com.aurora.gplayapi.data.models.File
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object DownloadConverter {

    @TypeConverter
    fun toSharedLibList(string: String): List<SharedLib> {
        val listType = object : TypeToken<List<SharedLib>>() {}.type
        return Gson().fromJson(string, listType)
    }

    @TypeConverter
    fun fromSharedLibList(list: List<SharedLib>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun toGPlayFileList(string: String): List<File> {
        val listType = object : TypeToken<List<File>>() {}.type
        return Gson().fromJson(string, listType)
    }

    @TypeConverter
    fun fromGPlayFileList(list: List<File>): String {
        return Gson().toJson(list)
    }
}
