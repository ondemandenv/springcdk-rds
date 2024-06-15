package dev.odmd.platform.springcdk.common

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.javamoney.moneta.FastMoney
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import javax.money.CurrencyContext
import javax.money.CurrencyContextBuilder
import javax.money.CurrencyUnit
import javax.money.Monetary

class MonetaryAmountToScaledLongTest {
    @Test
    fun `converts 1 dollar and 0 cents USD to 100`() {
        assertThat(
            FastMoney.of(1.00, "USD").toScaledLong(),
            equalTo(100L)
        )
    }

    @Test
    fun `converts 1 dollar and 1 cent USD to 101`() {
        assertThat(
            FastMoney.of(1.01, "USD").toScaledLong(),
            equalTo(101L)
        )
    }

    @Test
    fun `converts 1 dollar and 11 cents USD to 111`() {
        assertThat(
            FastMoney.of(1.01, "USD").toScaledLong(),
            equalTo(101L)
        )
    }

    @Test
    fun `converts 1 dollar and 11 cents EUR to 111`() {
        assertThat(
            FastMoney.of(1.01, "EUR").toScaledLong(),
            equalTo(101L)
        )
    }

    @Test
    fun `converts 111 yen JPY to 111`() {
        assertThat(
            FastMoney.of(111, "JPY").toScaledLong(),
            equalTo(111L)
        )
    }

    @Test
    fun `converts 11 and 1 fractional unit of test currency to 111`() {
        val testMonetaryAmount = Monetary.getDefaultAmountFactory()
            .setNumber(11.1)
            .setCurrency(TestCurrencyUnit())
            .create()
        assertThat(
            testMonetaryAmount.toScaledLong(),
            equalTo(111L)
        )
    }

    @Test
    fun `scaledLongToMonetaryAmount returns correct number when Currency is USD`() {
        val actual = scaledLongToMonetaryAmount("USD", 101)
        val expected = FastMoney.of(1.01, "USD")
        //actual.equals(expected) returns false
        Assertions.assertTrue(actual.isEqualTo(expected))
    }

    @Test
    fun `scaledLongToMonetaryAmount returns correct number when Currency is EUR`() {
        val actual = scaledLongToMonetaryAmount("EUR", 100)
        val expected = FastMoney.of(1.00, "EUR")
        //actual.equals(expected) returns false
        Assertions.assertTrue(actual.isEqualTo(expected))
    }

    @Test
    fun `scaledLongToMonetaryAmount returns correct number when Currency is GBP`() {
        val actual = scaledLongToMonetaryAmount("GBP", 100)
        val expected = FastMoney.of(1.00, "GBP")
        //actual.equals(expected) returns false
        Assertions.assertTrue(actual.isEqualTo(expected))
    }

    @Test
    fun `scaledLongToMonetaryAmount returns correct number when Currency is JPY`() {
        val actual = scaledLongToMonetaryAmount("JPY", 100)
        val expected = FastMoney.of(100, "JPY")
        //actual.equals(expected) returns false
        Assertions.assertTrue(actual.isEqualTo(expected))

    }

    class TestCurrencyUnit : CurrencyUnit {
        override fun compareTo(other: CurrencyUnit?): Int {
            return if (other is TestCurrencyUnit) {
                0
            } else {
                1
            }
        }

        override fun getCurrencyCode() = "test"

        override fun getNumericCode() = -1

        override fun getDefaultFractionDigits() = 1

        override fun getContext(): CurrencyContext = CurrencyContextBuilder.of("test").build()
    }
}
