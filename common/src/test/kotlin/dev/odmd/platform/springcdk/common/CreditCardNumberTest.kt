package dev.odmd.platform.springcdk.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class CreditCardNumberTest {

    @Test
    fun `getLastFour works as expected`() {
        val pan = "1234123412346789".toCharArray()
        val ccn = CreditCardNumber(pan)

        assertEquals("6789", ccn.lastFour)
    }

    @Test
    fun `getFirstDigit works as expected`() {
        val pan = "1234123412346789".toCharArray()
        val ccn = CreditCardNumber(pan)

        assertEquals("1", ccn.firstDigitAsString)
    }

    @Test
    fun `toString does not leak PAN`() {
        val pan = "1234123412346789"
        val ccn = CreditCardNumber(pan.toCharArray())

        assertFalse(ccn.toString().contains(pan))
    }

    @Test
    fun `clear removes all information`() {
        val pan = "1234123412346789".toCharArray()
        val ccn = CreditCardNumber(pan)
        val last4 = ccn.lastFour
        val first = ccn.firstDigitAsString

        ccn.close()
        assertNotEquals(ccn.lastFour, last4)
        assertNotEquals(ccn.firstDigitAsString, first)
    }

    @Test
    fun `finalize removes all information`() {
        val pan = "1234123412346789".toCharArray()
        val ccn = CreditCardNumber(pan)
        val last4 = ccn.lastFour
        val first = ccn.firstDigitAsString

        val finalizeFunction = CreditCardNumber::class.java.getDeclaredMethod("finalize")
        finalizeFunction.isAccessible = true
        finalizeFunction.invoke(ccn)

        assertNotEquals(ccn.lastFour, last4)
        assertNotEquals(ccn.firstDigitAsString, first)
    }
}
