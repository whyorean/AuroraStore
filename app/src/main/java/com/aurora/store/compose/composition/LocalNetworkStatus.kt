/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composition

import androidx.compose.runtime.compositionLocalOf
import com.aurora.store.data.model.NetworkStatus

/**
 * CompositionLocal carrying the current device network status. Provided once at the
 * activity root from a single [com.aurora.store.data.providers.NetworkProvider] subscription,
 * so any screen can read `LocalNetworkStatus.current` without injecting the provider
 * or duplicating the flow collection.
 *
 * Uses [compositionLocalOf] (not static) so only readers recompose on change.
 */
val LocalNetworkStatus = compositionLocalOf { NetworkStatus.AVAILABLE }
