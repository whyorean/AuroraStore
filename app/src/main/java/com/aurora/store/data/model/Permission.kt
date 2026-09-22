/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

data class Permission(
    val type: PermissionType,
    val title: String,
    val subtitle: String,
    val optional: Boolean = false,
    val isGranted: Boolean = false
)
