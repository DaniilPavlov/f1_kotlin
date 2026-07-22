package com.example.f1_kotlin.domain

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Unit-тесты [ApiCallHandler] / [Throwable.toAppError] — тексты ошибок и автоповтор. */
class ApiCallHandlerTest {

    @Test
    fun safeCall_success_returnsValue() = runTest {
        val result = ApiCallHandler.safeCall { "ok" }
        assertEquals("ok", result.getOrNull())
    }

    @Test
    fun safeCall_retriesIOException_thenSucceeds() = runTest {
        var attempts = 0
        val result = ApiCallHandler.safeCall {
            attempts++
            if (attempts == 1) throw SocketTimeoutException()
            "ok"
        }
        assertEquals("ok", result.getOrNull())
        assertEquals(2, attempts)
    }

    @Test
    fun safeCall_timeoutAfterRetries_returnsServerSlowMessage() = runTest {
        val result = ApiCallHandler.safeCall(retries = 1) { throw SocketTimeoutException() }
        val error = result.exceptionOrNull()!!.toAppError()
        assertEquals("Сервер долго не отвечает", error.title)
    }

    @Test
    fun safeCall_unknownHost_returnsNoConnectionMessage() = runTest {
        val result = ApiCallHandler.safeCall { throw UnknownHostException() }
        val error = result.exceptionOrNull()!!.toAppError()
        assertEquals("Соединение отсутствует", error.title)
    }

    @Test
    fun safeCall_genericException_returnsParseError() = runTest {
        val result = ApiCallHandler.safeCall { throw IllegalStateException("bad json") }
        val error = result.exceptionOrNull()!!.toAppError()
        assertTrue(error.title.contains("Ошибка при обработке"))
    }

    @Test
    fun safeCall_rateLimit_returnsLocalizedRateLimitMessageWithoutRetry() = runTest {
        var attempts = 0
        val result = ApiCallHandler.safeCall {
            attempts++
            throw HttpException(Response.error<Any>(429, "{}".toResponseBody("application/json".toMediaType())))
        }
        val error = result.exceptionOrNull()!!.toAppError()
        assertEquals(ErrorStrings.tooManyRequests, error.title)
        assertEquals(1, attempts)
    }

    @Test
    fun toAppError_mapsRawThrowableWithoutAppException() {
        val error = SocketTimeoutException().toAppError()
        assertEquals(ErrorStrings.serverSlow, error.title)
    }
}
