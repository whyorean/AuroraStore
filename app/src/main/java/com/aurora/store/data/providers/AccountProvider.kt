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
import com.aurora.Constants
import com.aurora.store.data.SingletonHolder
import com.aurora.store.data.model.AccountType
import com.aurora.store.util.Preferences

class AccountProvider private constructor(var context: Context) {

    companion object : SingletonHolder<AccountProvider, Context>(::AccountProvider)

    fun getAccountType(): AccountType {
        val rawType = Preferences.getString(context, Constants.ACCOUNT_TYPE)
        return when (rawType) {
            "GOOGLE" -> AccountType.GOOGLE
            else -> AccountType.ANONYMOUS
        }
    }

    fun logout() {
        Preferences.putBoolean(context, Constants.ACCOUNT_SIGNED_IN, false)
        Preferences.putString(context, Constants.ACCOUNT_EMAIL_PLAIN, "")
        Preferences.putString(context, Constants.ACCOUNT_AAS_PLAIN, "")
    }
}