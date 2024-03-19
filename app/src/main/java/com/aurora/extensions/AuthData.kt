package com.aurora.extensions

import android.content.Context
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.AuthValidator
import com.aurora.store.data.network.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun AuthData.isValid(context: Context): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            AuthValidator(this@isValid)
                .using(HttpClient.getPreferredClient(context))
                .isValid()
        } catch (e: Exception) {
            false
        }
    }
}
