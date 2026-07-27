package com.example.f1_kotlin.domain

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Пользовательская ошибка для UI / UiState (не Exception).
 *
 * Сетевые и прочие сбои приводятся сюда через [Throwable.toAppError].
 */
data class AppError(
    val title: String,
    val subtitle: String? = null,
) {
    fun asException(): AppException = AppException(this)

    fun toAsyncError(): AsyncValue.Error = AsyncValue.Error(title, subtitle)
}

/**
 * Throwable-обёртка для [Result.failure], чтобы донести [AppError] через стандартный Result.
 */
data class AppException(
    val error: AppError,
) : Exception(error.title) {
    val title: String get() = error.title
    val subtitle: String? get() = error.subtitle

    constructor(title: String, subtitle: String? = null) : this(AppError(title, subtitle))
}

/** Единая точка: любой [Throwable] → понятная [AppError] для экрана. */
fun Throwable.toAppError(): AppError = when (this) {
    is AppException -> error
    is SocketTimeoutException -> AppError(
        title = ErrorStrings.serverSlow,
        subtitle = ErrorStrings.noConnectionSubtitle,
    )
    is UnknownHostException -> AppError(
        title = ErrorStrings.noConnection,
        subtitle = ErrorStrings.noConnectionSubtitle,
    )
    is IOException -> AppError(
        title = ErrorStrings.noConnection,
        subtitle = ErrorStrings.noConnectionSubtitle,
    )
    is HttpException -> AppError(
        title = if (code() == 429) ErrorStrings.tooManyRequests else ErrorStrings.responseParseError,
        subtitle = ErrorStrings.errorRetrySubtitle,
    )
    else -> AppError(
        title = ErrorStrings.unexpectedError,
        subtitle = ErrorStrings.errorRetrySubtitle,
    )
}
