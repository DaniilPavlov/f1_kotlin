package com.example.f1_kotlin.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

/**
 * Единая обёртка для сетевых вызовов в Repository.
 *
 * GoF Behavioral Template Method — фиксированный скелет алгоритма
 * (try → retry при [IOException]/429 → map в [AppError]); вызывающий подставляет только `block`.
 */
object ApiCallHandler {

    private const val DEFAULT_RETRIES = 1
    private const val RETRY_DELAY_MS = 400L

    suspend fun <T> safeCall(
        retries: Int = DEFAULT_RETRIES,
        block: suspend () -> T,
    ): Result<T> {
        var lastError: AppException? = null
        var attempt = 0
        var finished = false
        while (attempt <= retries && !finished) {
            try {
                return Result.success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                lastError = e.toAppError().asException()
                // Jolpica rate-limit — повторяем, execute(maxAttempts: 3).
                if (e.code() == 429 && attempt < retries) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                    attempt++
                } else {
                    finished = true
                }
            } catch (e: IOException) {
                lastError = e.toAppError().asException()
                if (attempt < retries) {
                    delay(RETRY_DELAY_MS)
                    attempt++
                } else {
                    finished = true
                }
            } catch (e: Exception) {
                lastError = e.toAppError().asException()
                finished = true
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
    val unexpectedError get() = if (isEnglish) "Unexpected error" else "Неожиданная ошибка"
    val errorRetrySubtitle get() = if (isEnglish) {
        "Try refreshing the screen."
    } else {
        "Попробуйте обновить экран."
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
