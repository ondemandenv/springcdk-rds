package dev.odmd.platform.springcdk.model.v1

import javax.validation.Valid

data class PaymentProfilePaymentMethod(
    val type: dev.odmd.platform.springcdk.model.v1.PaymentType,
    @field:Valid
    val creditCard: dev.odmd.platform.springcdk.model.v1.CreditCardPaymentMethod?
)

enum class PaymentType {
    CREDIT_CARD
}