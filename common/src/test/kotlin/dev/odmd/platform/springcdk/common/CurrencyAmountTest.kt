package dev.odmd.platform.springcdk.common

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

internal class CurrencyAmountTest {
    @ParameterizedTest
    @ValueSource(strings = ["0.005", "0.006", "0.007", "0.008", "0.009"])
    fun `should round up after two decimal places`(amountString: String) {
        val amount = CurrencyAmount(amountString)
        assertEquals(CurrencyAmount("0.01"), amount)
    }

    @ParameterizedTest
    @ValueSource(strings = ["0.000", "0.001", "0.002", "0.003", "0.004"])
    fun `should round down after two decimal places`(amountString: String) {
        val amount = CurrencyAmount(amountString)
        assertEquals(CurrencyAmount("0.00"), amount)
    }

    @ParameterizedTest
    @ValueSource(strings = ["0.", ".0", "1.", ".1", ".10"])
    fun `should accept non-standard input`(amountString: String) {
        assertDoesNotThrow { CurrencyAmount(amountString) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["hello", "null", "zero", "", " "])
    fun `should throw on invalid input`(invalidInput: String) {
        assertThrows(RuntimeException::class.java) { CurrencyAmount(invalidInput) }
    }

    @Test
    fun `should serialize to decimal string`() {
        val objectMapper = ObjectMapper()
        val amount = CurrencyAmount("123.45")
        val serializedString = objectMapper.writeValueAsString(amount)
        assertEquals("\"123.45\"", serializedString)
    }

    @Test
    fun `should serialize to null when null`() {
        val objectMapper = ObjectMapper()
        val nullAmount: CurrencyAmount? = null
        val serializedString = objectMapper.writeValueAsString(nullAmount)
        assertEquals("null", serializedString)
    }

    internal class MonetaryAmountDeserializationTestContainer {
        var currencyAmount: CurrencyAmount? = null
    }

    @Test
    fun `should deserialize from decimal string`() {
        val amountString = "456.78"
        val jsonString = "{\"currencyAmount\": \"${amountString}\"}"

        val objectMapper = ObjectMapper()
        val obj = objectMapper.readValue(jsonString, MonetaryAmountDeserializationTestContainer::class.java)

        assertEquals(CurrencyAmount(amountString), obj.currencyAmount)
    }

    @Test
    fun `should deserialize as null when null`() {
        val nullString = "null"
        val jsonString = "{\"currencyAmount\": ${nullString}}"

        val objectMapper = ObjectMapper()
        val obj = objectMapper.readValue(jsonString, MonetaryAmountDeserializationTestContainer::class.java)

        assertEquals(null, obj.currencyAmount)
    }

    @Test
    fun `should add like BigDecimal`() {
        val m1 = CurrencyAmount("1.00")
        val m2 = CurrencyAmount("2.00")
        assertEquals(CurrencyAmount(m1.amount.plus(m2.amount)), m1 + m2)
    }

    @Test
    fun `should subtract like BigDecimal`() {
        val m1 = CurrencyAmount("1.00")
        val m2 = CurrencyAmount("2.00")
        assertEquals(CurrencyAmount(m1.amount.minus(m2.amount)), m1 - m2)
    }

    @Test
    fun `should be usable in HashMap`() {
        val map = mutableMapOf<CurrencyAmount, String>()
        val one = CurrencyAmount("1.00")
        map[one] = "hello"

        val otherOne = CurrencyAmount("1.00")
        assertEquals("hello", map[otherOne])
    }

    @ParameterizedTest
    @CsvSource(
        "0.00,0",
        "0.00,0.0",
        "0,0"

    )
    fun `should consider multiple forms of zero equivalent`(amount1: String, amount2: String) {
        assertEquals(CurrencyAmount(amount1), CurrencyAmount(amount2))
    }
}
