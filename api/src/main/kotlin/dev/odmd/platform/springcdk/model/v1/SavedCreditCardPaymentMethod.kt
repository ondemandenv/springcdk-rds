package dev.odmd.platform.springcdk.model.v1

import dev.odmd.platform.springcdk.common.CreditCardNetwork
import javax.validation.constraints.Size

data class SavedCreditCardPaymentMethod(
    val paymentProfileId: String,
    @Size(min = 4, max = 4)
    val lastFourDigits: String?,
    val expirationMonth: Int?,
    val expirationYear: Int?,
    val type: CreditCardNetwork?,
    val email: String? = null
) : dev.odmd.platform.springcdk.model.v1.PaymentMethodData
