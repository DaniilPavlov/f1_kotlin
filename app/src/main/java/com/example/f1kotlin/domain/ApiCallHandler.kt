package com.example.f1kotlin.domain

import kotlinx.coroutines.delay
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Единая обёртка для сетевых вызовов в Repository.
 *
 * При [IOException] один повтор через [RETRY_DELAY_MS] — типичный случай «первый запрос упал,
 * повтор сразу прошёл» (холодный DNS/SSL, конкуренция с другими вкладками).
 */
object ApiCallHandler {

    private const val DEFAULT_RETRIES = 1
    private const val RETRY_DELAY_MS = 400L

    suspend fun <T> safeCall(
        retries: Int = DEFAULT_RETRIES,
        block: suspend () -> T,
    ): Result<T> {
        var lastError: AppException? = null
        repeat(retries + 1) { attempt ->
            try {
                return Result.success(block())
            } catch (e: Exception) {
                lastError = mapException(e)
                val canRetry = attempt < retries && e is IOException
                if (canRetry) {
                    delay(RETRY_DELAY_MS)
                } else {
                    return Result.failure(lastError!!)
                }
            }
        }
        return Result.failure(lastError!!)
    }

    private fun mapException(e: Exception): AppException = when (e) {
        is SocketTimeoutException -> AppException(
            title = "Сервер долго не отвечает",
            subtitle = "Проверьте соединение и попробуйте обновить позже",
        )
        is UnknownHostException -> AppException(
            title = "Соединение отсутствует",
            subtitle = "Как только соединение восстановится, вы снова сможете пользоваться приложением",
        )
        is IOException -> AppException(
            title = "Соединение отсутствует",
            subtitle = "Как только соединение восстановится, вы снова сможете пользоваться приложением",
        )
        else -> AppException(
            title = "Ошибка при обработке ответа от сервера",
            subtitle = e.message,
        )
    }
}
