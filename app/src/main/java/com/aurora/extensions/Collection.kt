/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.extensions

import com.aurora.gplayapi.model.AppFile

fun List<AppFile>.requiresObbDir(): Boolean = this.any { it.type == AppFile.Type.OBB }
