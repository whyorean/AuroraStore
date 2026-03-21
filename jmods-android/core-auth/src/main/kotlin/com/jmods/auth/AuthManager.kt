package com.jmods.auth

import com.aurora.gplayapi.data.models.AuthData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor() {
    private val _authData = MutableStateFlow<AuthData?>(null)
    val authData: Flow<AuthData?> = _authData.asStateFlow()

    fun setAuthData(data: AuthData) {
        _authData.value = data
    }

    fun getAuthData(): AuthData? = _authData.value
}
