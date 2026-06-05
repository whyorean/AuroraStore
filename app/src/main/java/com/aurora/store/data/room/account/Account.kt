/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.room.account

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.store.data.model.AccountType

@Entity(tableName = "account")
data class Account(
    @PrimaryKey val id: String,
    val type: AccountType,
    val email: String,
    val displayName: String? = null,
    val profilePicUrl: String? = null,
    val aasToken: String? = null,
    val authToken: String? = null,
    val tokenType: AuthHelper.Token = AuthHelper.Token.AAS,
    val authViaMicroG: Boolean = false,
    val authDataJson: String? = null,
    val isDefault: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
) {
    val isAnonymous: Boolean get() = type == AccountType.ANONYMOUS

    companion object {
        const val ANONYMOUS_ID = "anonymous"

        fun idFor(type: AccountType, email: String): String =
            if (type == AccountType.ANONYMOUS) ANONYMOUS_ID else email.lowercase()
    }
}
