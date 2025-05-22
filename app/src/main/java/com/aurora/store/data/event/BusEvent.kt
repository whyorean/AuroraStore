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

package com.aurora.store.data.event

abstract class Event

sealed class BusEvent : Event() {
    lateinit var extra: String
    lateinit var error: String

    data class Blacklisted(val packageName: String) : BusEvent()
}

sealed class AuthEvent : Event() {
    data class GoogleLogin(val success: Boolean, val email: String, val token: String) : AuthEvent()
}

open class InstallerEvent(open val packageName: String) : Event() {
    data class Installed(override val packageName: String) : InstallerEvent(packageName)
    data class Uninstalled(override val packageName: String) : InstallerEvent(packageName)

    data class Installing(
        override val packageName: String,
        val progress: Float = 0.0F
    ) : InstallerEvent(packageName)

    data class Failed(
        override val packageName: String,
        val error: String? = null,
        val extra: String? = null,
    ) : InstallerEvent(packageName)
}
