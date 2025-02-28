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


sealed class ViewState {
    inline fun <reified T> ViewState.getDataAs(): T {
        return (this as? Success<*>)?.data as T
    }

    data object Loading : ViewState()
    data object Empty : ViewState()
    data class Error(val error: String?) : ViewState()
    data class Status(val status: String?) : ViewState()
    data class Success<T>(val data: T) : ViewState()
}

sealed class AuthState {
    data object Init: AuthState()
    data object Available : AuthState()
    data object Unavailable : AuthState()
    data object SignedIn : AuthState()
    data object SignedOut : AuthState()
    data object Valid : AuthState()
    data object Fetching : AuthState()
    data object Verifying : AuthState()
    data class PendingAccountManager(val email: String, val token: String) : AuthState()
    data class Failed(val status: String) : AuthState()
}
