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
import com.aurora.extensions.isNAndAbove
import com.aurora.store.util.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class BlacklistProvider @Inject constructor(
    private val json: Json,
    @ApplicationContext val context: Context
) {

    companion object {
        private const val PREFERENCE_BLACKLIST = "PREFERENCE_BLACKLIST"
    }

    var blacklist: MutableSet<String>
        set(value) = Preferences.putString(
            context,
            PREFERENCE_BLACKLIST,
            json.encodeToString(value)
        )
        get() {
            return try {
                val rawBlacklist = if (isNAndAbove) {
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

                    Preferences.getPrefs(context)
                        .getString(
                            PREFERENCE_BLACKLIST,
                            refSharedPreferences.getString(PREFERENCE_BLACKLIST, "")
                        )
                } else {
                    Preferences.getString(context, PREFERENCE_BLACKLIST)
                }
                if (rawBlacklist!!.isEmpty()) {
                    mutableSetOf()
                } else {
                    json.decodeFromString<MutableSet<String>>(rawBlacklist)
                }
            } catch (e: Exception) {
                mutableSetOf()
            }
        }

    fun isBlacklisted(packageName: String): Boolean = blacklist.contains(packageName)

    fun blacklist(packageName: String) {
        blacklist = blacklist.apply {
            add(packageName)
        }
    }

    fun whitelist(packageName: String) {
        blacklist = blacklist.apply {
            remove(packageName)
        }
    }
}
