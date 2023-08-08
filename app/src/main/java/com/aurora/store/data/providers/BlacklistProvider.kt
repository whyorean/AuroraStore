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
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.aurora.extensions.isNAndAbove
import com.aurora.store.data.SingletonHolder
import com.aurora.store.util.Preferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.reflect.Modifier

class BlacklistProvider private constructor(var context: Context) {

    companion object : SingletonHolder<BlacklistProvider, Context>(::BlacklistProvider) {
        const val PREFERENCE_BLACKLIST = "PREFERENCE_BLACKLIST"
    }

    private var gson: Gson = GsonBuilder()
        .excludeFieldsWithModifiers(Modifier.TRANSIENT)
        .create()

    fun getBlackList(): MutableSet<String> {
        return try {
            val rawBlacklist = if (isNAndAbove()) {
                val refMethod = Context::class.java.getDeclaredMethod(
                    "getSharedPreferences",
                    File::class.java,
                    Int::class.java
                )
                val refSharedPreferences = refMethod.invoke(
                    context,
                    File("/product/etc/com.aurora.store/blacklist.xml"),
                    Context.MODE_PRIVATE
                ) as SharedPreferences

                PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(
                        PREFERENCE_BLACKLIST,
                        refSharedPreferences.getString(PREFERENCE_BLACKLIST, "")
                    )
            } else {
                Preferences.getString(context, PREFERENCE_BLACKLIST)
            }
            if (rawBlacklist!!.isEmpty())
                mutableSetOf()
            else
                gson.fromJson(rawBlacklist, object : TypeToken<Set<String?>?>() {}.type)
        } catch (e: Exception) {
            mutableSetOf()
        }
    }

    fun isBlacklisted(packageName: String): Boolean {
        return getBlackList().contains(packageName)
    }

    fun blacklist(packageName: String) {
        blacklist(setOf(packageName))
    }

    fun whitelist(packageName: String) {
        whitelist(setOf(packageName))
    }

    fun blacklist(packageNames: Set<String>) {
        val oldBlackList: MutableSet<String> = getBlackList()
        oldBlackList.addAll(packageNames)
        save(oldBlackList)
    }

    fun whitelist(packageNames: Set<String>) {
        val oldBlackList: MutableSet<String> = getBlackList()
        oldBlackList.removeAll(packageNames)
        save(oldBlackList)
    }

    @Synchronized
    fun save(blacklist: Set<String>) {
        Preferences.putString(context, PREFERENCE_BLACKLIST, gson.toJson(blacklist))
    }
}
