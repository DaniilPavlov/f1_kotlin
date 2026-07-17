package com.example.f1_kotlin.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Unit-тесты [ApiCallHandler] — тексты ошибок и автоповтор при [IOException]. */
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
        val error = result.exceptionOrNull() as AppException
        assertEquals("Сервер долго не отвечает", error.title)
    }

    @Test
    fun safeCall_unknownHost_returnsNoConnectionMessage() = runTest {
        val result = ApiCallHandler.safeCall { throw UnknownHostException() }
        val error = result.exceptionOrNull() as AppException
        assertEquals("Соединение отсутствует", error.title)
    }

    @Test
    fun safeCall_genericException_returnsParseError() = runTest {
        val result = ApiCallHandler.safeCall { throw IllegalStateException("bad json") }
        val error = result.exceptionOrNull() as AppException
        assertTrue(error.title.contains("Ошибка при обработке"))
    }
}
