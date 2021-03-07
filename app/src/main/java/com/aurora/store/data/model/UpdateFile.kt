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

import com.aurora.gplayapi.data.models.App
import com.aurora.store.State
import com.tonyodev.fetch2.FetchGroup

data class UpdateFile(val app: App) {

    var group: FetchGroup? = null
    var state: State = State.IDLE

    override fun hashCode(): Int {
        return app.id
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is UpdateFile -> other.app.id == app.id
                    && other.state == state
                    && isGroupSame(group, other.group)
            else -> false
        }
    }

    private fun isGroupSame(oldGroup: FetchGroup?, newGroup: FetchGroup?): Boolean {
        if (oldGroup != null && newGroup == null)
            return true
        return oldGroup?.groupDownloadProgress != newGroup?.groupDownloadProgress
    }
}