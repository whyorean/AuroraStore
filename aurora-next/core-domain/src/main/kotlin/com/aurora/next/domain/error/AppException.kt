package com.aurora.next.domain.error

sealed class AppException : Exception() {
    object NetworkUnavailable : AppException()
    data class InstallationFailed(val code: Int, val message: String) : AppException()
    object AuthExpired : AppException()
    data class Unknown(override val message: String) : AppException()
}

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val exception: AppException) : AppResult<Nothing>()
    object Loading : AppResult<Nothing>()
}
