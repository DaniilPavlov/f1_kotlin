package com.example.f1_kotlin.domain

import kotlinx.coroutines.delay
import java.io.IOException

/**
 * Единая обёртка для сетевых вызовов в Repository.
 *
 * При [IOException] один повтор через [RETRY_DELAY_MS] — типичный случай «первый запрос упал,
 * повтор сразу прошёл» (холодный DNS/SSL, конкуренция с другими вкладками).
 * Ошибки мапятся через [Throwable.toAppError].
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
            } catch (e: IOException) {
                lastError = e.toAppError().asException()
                if (attempt < retries) {
                    delay(RETRY_DELAY_MS)
                } else {
                    return Result.failure(lastError!!)
                }
            } catch (e: Exception) {
                return Result.failure(e.toAppError().asException())
            }
        }
        return Result.failure(lastError!!)
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
    val responseParseError get() = if (isEnglish) {
        "Error processing the server response"
    } else {
        "Ошибка при обработке ответа от сервера"
    }
    val raceNotFound get() = if (isEnglish) {
        "No races found for your query. Check the entered data and try again."
    } else {
        "По вашему запросу гонок не найдено. Проверьте введенные данные и попробуйте еще раз."
    }
    val circuitNotFound get() = if (isEnglish) "Circuit not found" else "Трасса не найдена"
    val driverNotFound get() = if (isEnglish) "Driver not found" else "Пилот не найден"
    val constructorNotFound get() = if (isEnglish) "Constructor not found" else "Конструктор не найден"
}
