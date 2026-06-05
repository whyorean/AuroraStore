/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.room.account

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.store.data.model.AccountType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@ProvidedTypeConverter
class AccountConverter @Inject constructor() {

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter
    fun fromAccountType(type: AccountType): String = type.name

    @TypeConverter
    fun toToken(value: String): AuthHelper.Token = AuthHelper.Token.valueOf(value)

    @TypeConverter
    fun fromToken(token: AuthHelper.Token): String = token.name
}
