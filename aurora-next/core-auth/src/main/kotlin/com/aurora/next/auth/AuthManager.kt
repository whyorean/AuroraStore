package com.aurora.next.auth

import kotlinx.coroutines.flow.StateFlow

interface AuthManager {
    val authState: StateFlow<AuthState>
    suspend fun loginAnonymous()
    suspend fun loginGoogle(email: String, token: String)
    suspend fun logout()
}

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(val account: Account) : AuthState()
}

data class Account(val email: String, val token: String, val type: AccountType)
enum class AccountType { GOOGLE, ANONYMOUS }
