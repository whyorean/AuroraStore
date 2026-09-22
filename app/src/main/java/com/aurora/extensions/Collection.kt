/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.extensions

import com.aurora.gplayapi.data.models.PlayFile

fun List<PlayFile>.requiresObbDir(): Boolean = this.any { it.type == PlayFile.Type.OBB }
