/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.tv.navigation

/**
 * Resolves the first TV route. First launch (intro not completed) shows onboarding; otherwise an
 * existing account (anonymous or otherwise) skips the login gate. Anonymous-only login means any
 * saved account counts as logged in.
 */
fun resolveStartDestination(introCompleted: Boolean, isLoggedIn: Boolean): Screen = when {
    !introCompleted -> Screen.Onboarding
    !isLoggedIn -> Screen.Login
    else -> Screen.Home
}
