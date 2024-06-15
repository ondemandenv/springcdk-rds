package dev.odmd.platform.springcdk.common

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class CreditCardNetworkTest {
    @ParameterizedTest
    @ValueSource(ints = [3, 4, 5, 6])
    fun `maps all first digits correctly`(firstDigit: Int) {
        // This test feels redundant, but might help if there's a typo or something
        val correctMapping = mapOf(
            2 to CreditCardNetwork.MASTERCARD,
            3 to CreditCardNetwork.AMEX,
            4 to CreditCardNetwork.VISA,
            5 to CreditCardNetwork.MASTERCARD,
            6 to CreditCardNetwork.DISCOVER
        )

        val result = CreditCardNetwork.fromFirstDigit(firstDigit)
        Assertions.assertEquals(correctMapping[firstDigit], result)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 7, 8, 9])
    fun `unknown when first digit is invalid`(firstDigit: Int) {
        val result = CreditCardNetwork.fromFirstDigit(firstDigit)
        Assertions.assertEquals(CreditCardNetwork.UNKNOWN, result)
    }
}
