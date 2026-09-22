/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

data class Link(
    var id: Int,
    var title: String,
    var subtitle: String,
    var url: String,
    var icon: Int
) {
    override fun equals(other: Any?): Boolean = when (other) {
        is Link -> other.id == id
        else -> false
    }

    override fun hashCode(): Int = id.hashCode()
}
