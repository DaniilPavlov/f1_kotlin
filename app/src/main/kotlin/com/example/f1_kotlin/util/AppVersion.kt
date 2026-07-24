package com.example.f1_kotlin.util

/**
 * Сравнение semver (major.minor.patch), без pre-release/build metadata.
 */
object AppVersion {
    /** `true`, если [current] строго меньше [minimum]. */
    fun isLowerThan(current: String, minimum: String): Boolean {
        val a = parts(current)
        val b = parts(minimum)
        for (i in 0 until 3) {
            if (a[i] < b[i]) return true
            if (a[i] > b[i]) return false
        }
        return false
    }

    private fun parts(raw: String): IntArray {
        val core = raw.substringBefore('+').substringBefore('-')
        val chunks = core.split('.')
        return IntArray(3) { i ->
            chunks.getOrNull(i)?.toIntOrNull() ?: 0
        }
    }
}
