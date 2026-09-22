/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.extensions

import java.util.Locale

private val isoCountries = Locale.getISOCountries().toSet()

/**
 * Same locale with any region Play cannot resolve dropped.
 *
 * Play answers with an empty result set when the region is a UN M.49 code instead of an
 * ISO 3166-1 one; Android hands out such locales for region-less languages (`eo-001`) and
 * for macro-regions (`es-419`).
 */
fun Locale.withPlayRegion(): Locale = when (country) {
    in isoCountries, "" -> this
    else -> Locale.Builder().setLocale(this).setRegion("").build()
}
