@file:Suppress("NOTHING_TO_INLINE")

package com.aliucord.manager.network.utils

import io.ktor.http.HttpStatusCode

sealed interface ApiResponse<T> {
    data class Success<T>(val data: T) : ApiResponse<T>
    data class Error<T>(val error: ApiError) : ApiResponse<T>
    data class Failure<T>(val error: ApiFailure) : ApiResponse<T>
}

class ApiError(code: HttpStatusCode, body: String?) : Error("HTTP Code $code, Body: $body")

class ApiFailure(error: Throwable, body: String?) : Error(body, error)

inline fun <T, R> ApiResponse<T>.fold(
    success: (T) -> R,
    error: (ApiError) -> R,
    failure: (ApiFailure) -> R
): R {
    return when (this) {
        is ApiResponse.Success -> success(this.data)
        is ApiResponse.Error -> error(this.error)
        is ApiResponse.Failure -> failure(this.error)
    }
}

inline fun <T, R> ApiResponse<T>.fold(
    success: (T) -> R,
    fail: (Error) -> R,
): R {
    return when (this) {
        is ApiResponse.Success -> success(data)
        is ApiResponse.Error -> fail(error)
        is ApiResponse.Failure -> fail(error)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <T, R> ApiResponse<T>.transform(block: (T) -> R): ApiResponse<R> {
    return if (this !is ApiResponse.Success) {
        // Error and Failure do not use the generic value
        this as ApiResponse<R>
    } else {
        ApiResponse.Success(block(data))
    }
}

inline fun <T> ApiResponse<T>.getOrThrow(): T {
    return fold(
        success = { it },
        fail = { throw it }
    )
}

inline fun <T> ApiResponse<T>.getOrNull(): T? {
    return fold(
        success = { it },
        fail = { null }
    )
}

@Suppress("UNCHECKED_CAST")
inline fun <T, R> ApiResponse<T>.chain(block: (T) -> ApiResponse<R>): ApiResponse<R> {
    return if (this !is ApiResponse.Success) {
        // Error and Failure do not use the generic value
        this as ApiResponse<R>
    } else {
        block(data)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <T, R> ApiResponse<T>.chain(secondary: ApiResponse<R>): ApiResponse<R> {
    return if (secondary is ApiResponse.Success) {
        secondary
    } else {
        // Error and Failure do not use the generic value
        this as ApiResponse<R>
    }
}
