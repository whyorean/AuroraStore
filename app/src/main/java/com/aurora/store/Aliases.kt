/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store

import com.aurora.gplayapi.api.TopChartType
import com.aurora.gplayapi.model.Category
import com.aurora.gplayapi.model.ContentType
import com.aurora.gplayapi.model.StreamBundle
import com.aurora.gplayapi.model.StreamCluster

typealias MR = com.google.android.material.R.attr

typealias TopChartStash = MutableMap<ContentType, MutableMap<TopChartType, StreamCluster>>
typealias HomeStash = MutableMap<ContentType, StreamBundle>
typealias CategoryStash = MutableMap<ContentType, List<Category>>
