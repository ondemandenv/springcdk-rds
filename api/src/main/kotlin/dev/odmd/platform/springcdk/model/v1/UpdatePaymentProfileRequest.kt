package dev.odmd.platform.springcdk.model.v1

import dev.odmd.platform.springcdk.common.BillingInformation

data class UpdatePaymentProfileRequest(
    val updateType: dev.odmd.platform.springcdk.model.v1.UpdatePaymentProfileRequest.UpdateType,
    val creditCard: dev.odmd.platform.springcdk.model.v1.UpdatePaymentProfileRequest.CreditCardUpdate? = null
) {
    enum class UpdateType {
        CREDIT_CARD
    }

    sealed interface Update

    data class CreditCardUpdate(
        val expirationMonth: Int,
        val expirationYear: Int,
        val billingInformation: BillingInformation
    ) : dev.odmd.platform.springcdk.model.v1.UpdatePaymentProfileRequest.Update
}
