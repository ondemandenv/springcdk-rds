package dev.odmd.platform.springcdk.domain.entities

import dev.odmd.platform.springcdk.common.BillingInformation
import dev.odmd.platform.springcdk.common.FirstCreditCardDigitProvider
import dev.odmd.platform.springcdk.domain.cryptography.JsonEncrypted

/**
 * Do not rename this class. It will break deserialization of existing objects. See comment on
 * [PaymentMethodInformation] for more information.
 */
data class CreditCardPaymentMethodInformation(
    @dev.odmd.platform.springcdk.domain.cryptography.JsonEncrypted val billingInformation: BillingInformation?,
    val creditCardInfo: dev.odmd.platform.springcdk.domain.entities.CreditCardPaymentMethodInformation.CreditCardInfo
) : dev.odmd.platform.springcdk.domain.entities.PaymentMethodInformation {
    data class CreditCardInfo(
        @dev.odmd.platform.springcdk.domain.cryptography.JsonEncrypted val gatewayToken: String,
        override val firstDigit: Int,
        val lastFourDigits: String,
        val expMonth: Int,
        val expYear: Int
    ) : FirstCreditCardDigitProvider {
        /**
         * Intentionally omit [gatewayToken] to avoid inadvertent information leaks in logs
         */
        override fun toString(): String {
            return "CreditCardInfo(firstDigit=$firstDigit, lastFourDigits=$lastFourDigits, expMonth=$expMonth, expYear=$expYear)"
        }
    }
}
