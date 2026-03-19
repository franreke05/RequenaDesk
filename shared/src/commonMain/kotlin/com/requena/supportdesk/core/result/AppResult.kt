package com.requena.supportdesk.core.result

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Error -> this
    is AppResult.Success -> AppResult.Success(transform(data))
}

inline fun <T> AppResult<T>.getOrElse(defaultValue: (AppResult.Error) -> T): T = when (this) {
    is AppResult.Error -> defaultValue(this)
    is AppResult.Success -> data
}
