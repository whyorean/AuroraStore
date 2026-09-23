/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.event

abstract class Event

sealed class BusEvent : Event() {
    lateinit var extra: String
    lateinit var error: String

    data class Blacklisted(val packageName: String) : BusEvent()
}

sealed class AuthEvent : Event() {
    data class GoogleLogin(
        val success: Boolean,
        val email: String,
        val token: String,
        val error: String? = null
    ) : AuthEvent()
    data class SessionExpired(val packageName: String? = null) : AuthEvent()
}

open class InstallerEvent(open val packageName: String) : Event() {
    data class Installed(override val packageName: String) : InstallerEvent(packageName)
    data class Uninstalled(override val packageName: String) : InstallerEvent(packageName)

    data class Installing(
        override val packageName: String,
        val progress: Float = 0.0F
    ) : InstallerEvent(packageName)

    data class PendingUserAction(override val packageName: String) : InstallerEvent(packageName)

    data class Failed(
        override val packageName: String,
        val error: String? = null,
        val extra: String? = null
    ) : InstallerEvent(packageName)
}
