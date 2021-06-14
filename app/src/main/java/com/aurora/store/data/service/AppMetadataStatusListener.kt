package com.aurora.store.data.service

import com.aurora.gplayapi.data.models.App

interface AppMetadataStatusListener {
    fun onAppMetadataStatusError(reason: String, app: App)
}