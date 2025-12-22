/*
 * SPDX-FileCopyrightText: 2026 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.room.update

import androidx.room.Embedded
import androidx.room.Relation
import com.aurora.store.data.room.download.Download

data class UpdateWithDownload(
    @Embedded val update: Update,
    @Relation(parentColumn = "packageName", entityColumn = "packageName") val download: Download?
)
