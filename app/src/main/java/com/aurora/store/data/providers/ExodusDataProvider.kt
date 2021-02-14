/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.data.providers

import android.content.Context
import com.aurora.store.data.SingletonHolder
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class ExodusDataProvider private constructor(val context: Context) {

    companion object : SingletonHolder<ExodusDataProvider, Context>(::ExodusDataProvider)

    private val exodusTrackers: JSONObject

    init {
        exodusTrackers = loadLocalTrackers()
    }

    fun getLocalTrackers(): JSONObject {
        return exodusTrackers
    }

    fun getFilteredTrackers(trackerIds: List<Int>): List<JSONObject> {
        return trackerIds.map {
            exodusTrackers.getJSONObject(
                it.toString()
            )
        }.toList()
    }

    private fun loadLocalTrackers(): JSONObject {
        val inputStream = context.assets.open("exodus_trackers.json")
        val bytes = ByteArray(inputStream.available())
        inputStream.read(bytes)
        inputStream.close()

        val json = String(bytes, StandardCharsets.UTF_8)
        val jsonArray = JSONArray(json)
        return jsonArray.getJSONObject(0)
    }
}