package com.aurora.store.data.downloader

import android.content.Context
import com.aurora.gplayapi.data.models.App
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_UNIQUE_GROUP_IDS
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RequestGroupIdBuilder {

    data class AppIDnVersion(val id: Int, val versionCode: Int)

    companion object {
        fun getGroupIDsForApp(context: Context, appID: Int): MutableList<Int> {
            val data = Preferences.getPrefs(context).getString(PREFERENCE_UNIQUE_GROUP_IDS, "")!!
            val gson = Gson()
            var groupIDMap = HashMap<Int, RequestGroupIdBuilder.AppIDnVersion>()
            if (data.isNotEmpty()) {
                val empMapType = object : TypeToken<Map<Int, RequestGroupIdBuilder.AppIDnVersion>?>() {}.type
                groupIDMap = HashMap(gson.fromJson(data, empMapType) ?: HashMap())
            }
            val out = mutableListOf<Int>()
            for (item in groupIDMap.entries) {
                if (item.value.id == appID) {
                    out.add(item.key)
                }
            }
            return out
        }
    }
}

fun App.getGroupId(context: Context): Int {
    val data = Preferences.getPrefs(context).getString(PREFERENCE_UNIQUE_GROUP_IDS, "")!!
    val gson = Gson()
    var groupIDMap = HashMap<Int, RequestGroupIdBuilder.AppIDnVersion>()
    if (data.isNotEmpty()) {
        val empMapType = object : TypeToken<Map<Int, RequestGroupIdBuilder.AppIDnVersion>?>() {}.type
        groupIDMap = HashMap(gson.fromJson(data, empMapType) ?: HashMap())
    }
    for (item in groupIDMap.entries) {
        if (item.value.id == this.id && item.value.versionCode == this.versionCode) {
            return item.key
        }
    }
    var randomGroupID = (0 until Int.MAX_VALUE).random()
    while (groupIDMap.containsKey(randomGroupID)) {
        randomGroupID = (0 until Int.MAX_VALUE).random()
    }
    groupIDMap[randomGroupID] = RequestGroupIdBuilder.AppIDnVersion(this.id, this.versionCode)
    Preferences.getPrefs(context).edit().putString(PREFERENCE_UNIQUE_GROUP_IDS, gson.toJson(groupIDMap)).apply()
    return randomGroupID
}