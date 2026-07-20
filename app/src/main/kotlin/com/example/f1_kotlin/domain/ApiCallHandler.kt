package com.example.f1_kotlin.domain

import kotlinx.coroutines.delay
import retrofit2.HttpException
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
            title = ErrorStrings.serverSlow,
            subtitle = ErrorStrings.noConnectionSubtitle,
        )
        is UnknownHostException -> AppException(
            title = ErrorStrings.noConnection,
            subtitle = ErrorStrings.noConnectionSubtitle,
        )
        is IOException -> AppException(
            title = ErrorStrings.noConnection,
            subtitle = ErrorStrings.noConnectionSubtitle,
        )
        is HttpException -> AppException(
            title = if (e.code() == 429) ErrorStrings.tooManyRequests else ErrorStrings.responseParseError,
            subtitle = e.message(),
        )
        else -> AppException(
            title = ErrorStrings.responseParseError,
            subtitle = e.message,
        )
    }
}

/** Localized domain messages are usable from repositories and JVM tests without a Context. */
object ErrorStrings {
    private val isEnglish: Boolean
        get() = LocaleController.language.value == "en"

    val noConnection get() = if (isEnglish) "No connection" else "Соединение отсутствует"
    val noConnectionSubtitle get() = if (isEnglish) {
        "Once the connection is restored, you will be able to use the app again"
    } else {
        "Как только соединение восстановится, вы снова сможете пользоваться приложением"
    }
    val serverSlow get() = if (isEnglish) "Server is taking too long to respond" else "Сервер долго не отвечает"
    val tooManyRequests get() = if (isEnglish) "Too many requests" else "Слишком много запросов"
    val responseParseError get() = if (isEnglish) "Error processing the server response" else "Ошибка при обработке ответа от сервера"
    val raceNotFound get() = if (isEnglish) {
        "No races found for your query. Check the entered data and try again."
    } else {
        "По вашему запросу гонок не найдено. Проверьте введенные данные и попробуйте еще раз."
    }
    val circuitNotFound get() = if (isEnglish) "Circuit not found" else "Трасса не найдена"
    val driverNotFound get() = if (isEnglish) "Driver not found" else "Пилот не найден"
    val constructorNotFound get() = if (isEnglish) "Constructor not found" else "Конструктор не найден"
}
