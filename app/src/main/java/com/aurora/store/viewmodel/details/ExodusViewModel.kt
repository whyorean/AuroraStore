/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.store.data.model.ExodusTracker
import com.aurora.store.data.model.Report
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class ExodusViewModel @Inject constructor(
    private val exodusTrackers: JSONObject
) : ViewModel() {

    private val _trackers = MutableStateFlow<List<ExodusTracker>>(emptyList())
    val trackers = _trackers.asStateFlow()

    fun getExodusTrackersFromReport(report: Report) {
        viewModelScope.launch(Dispatchers.IO) {
            val trackerObjects = report.trackers.map {
                exodusTrackers.getJSONObject(it.toString())
            }.toList()

            _trackers.value = trackerObjects.map {
                ExodusTracker(
                    id = it.getInt("id"),
                    name = it.getString("name"),
                    url = it.getString("website"),
                    signature = it.getString("code_signature"),
                    date = it.getString("creation_date"),
                    description = it.getString("description"),
                    networkSignature = it.getString("network_signature"),
                    documentation = listOf(it.getString("documentation")),
                    categories = listOf(it.getString("categories"))
                )
            }.toList()
        }
    }
}
