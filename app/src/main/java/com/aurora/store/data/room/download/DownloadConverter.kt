package com.aurora.store.data.room.download

import androidx.room.TypeConverter
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
}
