package dev.odmd.platform.springcdk.common

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

internal class BigDecimalIsZeroTest {
    @ParameterizedTest
    @ValueSource(strings = [
        "0",
        "0.0",
        "0.00"
    ])
    fun `true for any number of zeroes in a string`(decimal: BigDecimal) {
        assertTrue(decimal.isZero)
    }

    @ParameterizedTest
    @ValueSource(doubles = [
        0.0,
        0.00
    ])
    fun `true for any number of zeroes in a double`(double: Double) {
        assertTrue(BigDecimal(double).isZero)
    }

    @ParameterizedTest
    @ValueSource(doubles = [
        1.0,
        -1.0,
        0.1,
        -0.1
    ])
    fun `false for any value above or below zero`(doule: Double) {
        assertFalse(BigDecimal(doule).isZero)
    }
}
