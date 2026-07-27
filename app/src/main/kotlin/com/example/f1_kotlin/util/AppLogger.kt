package com.example.f1_kotlin.util

import android.util.Log
import com.example.f1_kotlin.BuildConfig

/**
 * Единый логгер приложения (только в debug).
 *
 * В release ничего не пишет в Logcat — меньше шума и риска утечки данных.
 */
object AppLogger {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (!BuildConfig.DEBUG) return
        if (throwable != null) Log.w(tag, message, throwable) else Log.w(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (!BuildConfig.DEBUG) return
        if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
    }
}
