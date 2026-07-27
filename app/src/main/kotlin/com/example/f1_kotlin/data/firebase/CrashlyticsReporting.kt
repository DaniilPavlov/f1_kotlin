package com.example.f1_kotlin.data.firebase

import com.example.f1_kotlin.domain.AppException
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Сетевые/временные ошибки не отправляем в Crashlytics — они не баги приложения.
 */
object CrashlyticsReporting {
    fun shouldReportUncaughtError(error: Throwable): Boolean {
        if (isBenignNetworkError(error)) return false
        if (error is AppException) {
            val cause = error.cause
            if (cause != null && isBenignNetworkError(cause)) return false
        }
        val cause = error.cause
        if (cause != null && cause !== error && isBenignNetworkError(cause)) return false
        return true
    }

    private fun isBenignNetworkError(error: Throwable): Boolean =
        error is IOException ||
            error is SocketException ||
            error is SocketTimeoutException ||
            error is UnknownHostException ||
            error is SSLException ||
            error is retrofit2.HttpException
}
