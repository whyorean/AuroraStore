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

package com.aurora.store.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class ExodusReport(
    val creator: String = String(),
    val name: String = String(),
    val reports: List<Report> = listOf()
)

@Serializable
@Parcelize
data class Report(
    val id: Int = -1,
    val downloads: String = String(),
    val version: String = String(),
    val creationDate: String = String(),
    val updatedAt: String = String(),
    val versionCode: String = String(),
    val trackers: List<Int> = listOf()
) : Parcelable

@Serializable
data class ExodusTracker(
    val id: Int = 0,
    val name: String = String(),
    val url: String = String(),
    val signature: String = String(),
    val date: String = String(),
    val description: String = String(),
    val networkSignature: String = String(),
    val documentation: List<String> = emptyList(),
    val categories: List<String> = emptyList()
) {

    override fun hashCode(): Int {
        return id
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is ExodusTracker -> other.id == id
            else -> false
        }
    }
}
