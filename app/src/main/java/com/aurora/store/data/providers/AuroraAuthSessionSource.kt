/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.providers

import com.aurora.gplayapi.session.AuthSessionSource
import com.aurora.gplayapi.session.AuthenticatedSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapts Aurora's account state to gplayapi v4's [AuthSessionSource].
 *
 * A thin shim: [AuthProvider] is the real implementation - it holds the warm,
 * already-decoded account cache and does the v3-to-v4 mapping - this class exists only to
 * give Hilt a type that satisfies the [AuthSessionSource] SPI, since `PlayClientModule`
 * wires it in as such rather than injecting [AuthProvider] under this interface directly.
 */
@Singleton
class AuroraAuthSessionSource @Inject constructor(
    private val authProvider: AuthProvider
) : AuthSessionSource {
    override suspend fun current(): AuthenticatedSession? = authProvider.currentSession()
}
