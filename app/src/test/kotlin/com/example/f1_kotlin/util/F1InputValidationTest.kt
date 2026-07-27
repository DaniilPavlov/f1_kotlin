package com.example.f1_kotlin.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit-тесты [F1InputValidation]. */
class F1InputValidationTest {

    @Test
    fun isValidYear_requiresFourDigitsInRange() {
        assertFalse(F1InputValidation.isValidYear("202"))
        assertFalse(F1InputValidation.isValidYear("1949"))
        assertFalse(F1InputValidation.isValidYear("2031"))
        assertFalse(F1InputValidation.isValidYear("20ab"))
        assertTrue(F1InputValidation.isValidYear("1950"))
        assertTrue(F1InputValidation.isValidYear("2026"))
        assertTrue(F1InputValidation.isValidYear("2030"))
    }

    @Test
    fun isValidRound_requiresOneToNinetyNine() {
        assertFalse(F1InputValidation.isValidRound(""))
        assertFalse(F1InputValidation.isValidRound("0"))
        assertFalse(F1InputValidation.isValidRound("100"))
        assertFalse(F1InputValidation.isValidRound("x"))
        assertTrue(F1InputValidation.isValidRound("1"))
        assertTrue(F1InputValidation.isValidRound("99"))
    }
}
