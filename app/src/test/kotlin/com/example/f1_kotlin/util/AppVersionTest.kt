package com.example.f1_kotlin.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {
    @Test
    fun equalVersionsAreNotLower() {
        assertFalse(AppVersion.isLowerThan("1.4.3", "1.4.3"))
        assertFalse(AppVersion.isLowerThan("0.0.0", "0.0.0"))
    }

    @Test
    fun lowerPatchMinorMajor() {
        assertTrue(AppVersion.isLowerThan("1.4.2", "1.4.3"))
        assertTrue(AppVersion.isLowerThan("1.3.9", "1.4.0"))
        assertTrue(AppVersion.isLowerThan("0.9.9", "1.0.0"))
    }

    @Test
    fun higherVersionIsNotLower() {
        assertFalse(AppVersion.isLowerThan("1.4.3", "0.0.0"))
        assertFalse(AppVersion.isLowerThan("2.0.0", "1.9.9"))
    }

    @Test
    fun ignoresBuildMetadata() {
        assertTrue(AppVersion.isLowerThan("1.4.3+202607220", "1.4.4"))
        assertFalse(AppVersion.isLowerThan("1.4.3+1", "1.4.3"))
    }
}
