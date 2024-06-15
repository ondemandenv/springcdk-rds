package dev.odmd.platform.springcdk.common

enum class CreditCardNetwork {
    AMEX,
    VISA,
    MASTERCARD,
    DISCOVER,
    UNKNOWN;

    companion object {
        fun fromFirstDigit(firstDigit: Int): CreditCardNetwork {
            return when (firstDigit) {
                2 -> MASTERCARD
                3 -> AMEX
                4 -> VISA
                5 -> MASTERCARD
                6 -> DISCOVER
                else -> UNKNOWN
            }
        }
    }
}

interface FirstCreditCardDigitProvider {
    val firstDigit: Int
}

val FirstCreditCardDigitProvider.creditCardNetwork: CreditCardNetwork
    get() = CreditCardNetwork.fromFirstDigit(firstDigit)
