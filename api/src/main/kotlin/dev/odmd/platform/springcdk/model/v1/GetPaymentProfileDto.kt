package dev.odmd.platform.springcdk.model.v1

import com.fasterxml.jackson.databind.JsonNode
import dev.odmd.platform.springcdk.common.BillingInformation
import dev.odmd.platform.springcdk.common.CreditCardNetwork
import javax.validation.constraints.Size

data class GetPaymentProfileDto(
    val id: String,
    val customerId: String,
    val reusable: Boolean,
    val default: Boolean,
    val paymentMethod: dev.odmd.platform.springcdk.model.v1.GetPaymentProfileDto.PaymentMethod,
    val metadata: JsonNode? = null
) {
    enum class PaymentMethodType {
        SAVED_CREDIT_CARD
    }

    data class PaymentMethod(
        val type: dev.odmd.platform.springcdk.model.v1.GetPaymentProfileDto.PaymentMethodType,
        val savedCreditCard: dev.odmd.platform.springcdk.model.v1.GetPaymentProfileDto.SavedCreditCard?
    ) {
        constructor(savedCreditCardPaymentMethod: dev.odmd.platform.springcdk.model.v1.GetPaymentProfileDto.SavedCreditCard) : this(
            type = dev.odmd.platform.springcdk.model.v1.GetPaymentProfileDto.PaymentMethodType.SAVED_CREDIT_CARD,
            savedCreditCard = savedCreditCardPaymentMethod
        )

        val methodInformation: dev.odmd.platform.springcdk.model.v1.GetPaymentProfileDto.PaymentMethodInformation
            get() = when (type) {
                dev.odmd.platform.springcdk.model.v1.GetPaymentProfileDto.PaymentMethodType.SAVED_CREDIT_CARD -> savedCreditCard as dev.odmd.platform.springcdk.model.v1.GetPaymentProfileDto.PaymentMethodInformation
            }
    }

    sealed interface PaymentMethodInformation

    data class SavedCreditCard(
        val paymentProfileId: String,
        @Size(min = 4, max = 4)
        val lastFourDigits: String?,
        val expirationMonth: Int?,
        val expirationYear: Int?,
        val type: CreditCardNetwork?,
        val email: String? = null,
        val billingInformation: BillingInformation? = null
    ) : dev.odmd.platform.springcdk.model.v1.GetPaymentProfileDto.PaymentMethodInformation
}