/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.providers

import android.content.Context
import com.aurora.Constants
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.store.data.model.AccountType
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTH_DATA

object AccountProvider {

    fun getAccountType(context: Context): AccountType {
        val rawType = Preferences.getString(context, Constants.ACCOUNT_TYPE)
        return when (rawType) {
            "GOOGLE" -> AccountType.GOOGLE
            else -> AccountType.ANONYMOUS
        }
    }

    fun isLoggedIn(context: Context): Boolean =
        Preferences.getBoolean(context, Constants.ACCOUNT_SIGNED_IN, false)

    fun getLoginEmail(context: Context): String? {
        val email = Preferences.getString(context, Constants.ACCOUNT_EMAIL_PLAIN)
        return email.ifBlank { null }
    }

    fun getLoginToken(context: Context): Pair<String, AuthHelper.Token>? {
        val email = Preferences.getString(context, Constants.ACCOUNT_EMAIL_PLAIN)
        val aasToken = Preferences.getString(context, Constants.ACCOUNT_AAS_PLAIN)
        val authToken = Preferences.getString(context, Constants.ACCOUNT_AUTH_PLAIN)

        if (email.isBlank() && (aasToken.isBlank() || authToken.isBlank())) return null

        val tokenType = if (aasToken.isBlank()) AuthHelper.Token.AUTH else AuthHelper.Token.AAS
        return Pair(aasToken.ifBlank { authToken }, tokenType)
    }

    fun login(
        context: Context,
        email: String,
        token: String,
        tokenType: AuthHelper.Token,
        accountType: AccountType
    ) {
        Preferences.putBoolean(context, Constants.ACCOUNT_SIGNED_IN, true)
        Preferences.putString(context, Constants.ACCOUNT_EMAIL_PLAIN, email)
        Preferences.putString(context, Constants.ACCOUNT_TYPE, accountType.name)

        if (tokenType == AuthHelper.Token.AAS) {
            Preferences.putString(context, Constants.ACCOUNT_AAS_PLAIN, token)
        } else {
            Preferences.putString(context, Constants.ACCOUNT_AUTH_PLAIN, token)
        }
    }

    fun logout(context: Context) {
        Preferences.remove(context, PREFERENCE_AUTH_DATA)
        Preferences.remove(context, Constants.ACCOUNT_SIGNED_IN)
        Preferences.remove(context, Constants.ACCOUNT_TYPE)
        Preferences.remove(context, Constants.ACCOUNT_EMAIL_PLAIN)
        Preferences.remove(context, Constants.ACCOUNT_AAS_PLAIN)
        Preferences.remove(context, Constants.ACCOUNT_AUTH_PLAIN)
        Preferences.remove(context, Preferences.PREFERENCE_AUTH_VIA_MICROG)
    }
}
