package com.example.f1_kotlin.data.firebase

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Unit-тесты [CrashlyticsReporting] — фильтрация benign network errors. */
class CrashlyticsReportingTest {

    @Test
    fun shouldNotReportBenignNetworkErrors() {
        assertFalse(CrashlyticsReporting.shouldReportUncaughtError(UnknownHostException()))
        assertFalse(CrashlyticsReporting.shouldReportUncaughtError(SocketTimeoutException()))
        assertFalse(
            CrashlyticsReporting.shouldReportUncaughtError(
                HttpException(
                    Response.error<Any>(
                        500,
                        "{}".toResponseBody("application/json".toMediaType()),
                    ),
                ),
            ),
        )
        assertFalse(
            CrashlyticsReporting.shouldReportUncaughtError(
                Exception("wrap", UnknownHostException()),
            ),
        )
    }

    @Test
    fun shouldReportUnexpectedBugs() {
        assertTrue(CrashlyticsReporting.shouldReportUncaughtError(IllegalStateException("bug")))
        assertTrue(CrashlyticsReporting.shouldReportUncaughtError(NullPointerException()))
    }
}
